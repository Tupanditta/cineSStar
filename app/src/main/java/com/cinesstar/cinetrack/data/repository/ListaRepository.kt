package com.cinesstar.cinetrack.data.repository

import androidx.room.withTransaction
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.data.room.ListaDao
import com.cinesstar.cinetrack.data.room.ListaEntity
import com.cinesstar.cinetrack.data.room.ListaItemEntity
import com.cinesstar.cinetrack.data.room.PerfilDao
import com.cinesstar.cinetrack.domain.DominioException
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Normalizador
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.exigirAdmite

/** Operaciones de posición de los rankings: siempre 1..n contiguo (S-07). Deben llamarse dentro de una transacción. */
internal class RankingOps(
    private val listaDao: ListaDao,
) {
    suspend fun compactar(listaId: Long) {
        listaDao.itemsPorPosicion(listaId).forEachIndexed { indice, item ->
            val esperada = indice + 1
            if (item.posicion != esperada) listaDao.actualizarPosicion(listaId, item.perfilId, esperada)
        }
    }
}

/** Listas y sus elementos (CU-11..CU-19). Cada escritura es una transacción. */
class ListaRepository(
    private val db: CineDatabase,
    private val listaDao: ListaDao = db.listaDao(),
    private val perfilDao: PerfilDao = db.perfilDao(),
    private val reloj: () -> Long = System::currentTimeMillis,
) {
    private val rankings = RankingOps(listaDao)

    /** CU-11. Errores: M-07 (nombre vacío), M-08 (ya existe, comparando nombres normalizados, S-15). */
    suspend fun crear(
        nombre: String,
        tipo: TipoLista,
        restriccion: Restriccion = Restriccion.MIXTO,
    ): Long =
        db.withTransaction {
            val limpio = Normalizador.limpiar(nombre)
            if (limpio.isEmpty()) throw DominioException(ErrorDominio.ListaNombreObligatorio)
            val nombreBusqueda = Normalizador.normalizar(limpio)
            if (listaDao.buscarPorNombre(nombreBusqueda) != null) throw DominioException(ErrorDominio.ListaDuplicada)
            listaDao.insertar(
                ListaEntity(nombre = limpio, nombreBusqueda = nombreBusqueda, tipo = tipo, restriccion = restriccion, creadoEn = reloj()),
            )
        }

    /**
     * CU-15. En un ranking, [posicion] (1..n+1) inserta en medio desplazando el resto; sin ella va al final (RF-39).
     * En una lista de prioridad, sin [prioridad] se usa Media (S-16). Errores: M-09, M-10, M-24.
     */
    suspend fun anadir(
        listaId: Long,
        perfilId: Long,
        posicion: Int? = null,
        prioridad: Prioridad? = null,
    ) = db.withTransaction {
        val lista = listaDao.obtener(listaId) ?: throw DominioException(ErrorDominio.NoEncontrado)
        val perfil = perfilDao.obtener(perfilId) ?: throw DominioException(ErrorDominio.NoEncontrado)
        lista.restriccion.exigirAdmite(perfil.tipo)
        if (listaDao.item(listaId, perfilId) != null) throw DominioException(ErrorDominio.YaEnLista(perfil.nombre))
        val item = ListaItemEntity(listaId = listaId, perfilId = perfilId, posicion = null, prioridad = null, anadidoEn = reloj())
        when (lista.tipo) {
            TipoLista.NORMAL -> listaDao.insertarItem(item)
            TipoLista.PRIORIDAD -> listaDao.insertarItem(item.copy(prioridad = prioridad ?: Prioridad.POR_DEFECTO))
            TipoLista.RANKING -> {
                val total = listaDao.contarItems(listaId)
                val destino = posicion ?: (total + 1)
                if (destino !in 1..total + 1) throw DominioException(ErrorDominio.PosicionInvalida)
                listaDao.desplazarPosiciones(listaId, desde = destino, delta = 1)
                listaDao.insertarItem(item.copy(posicion = destino))
            }
        }
    }

    /** CU-16. Quita un perfil de la lista; en un ranking las posiciones posteriores suben (S-07). Devuelve si la lista quedó vacía. */
    suspend fun quitar(
        listaId: Long,
        perfilId: Long,
    ): Boolean =
        db.withTransaction {
            val lista = listaDao.obtener(listaId) ?: throw DominioException(ErrorDominio.NoEncontrado)
            if (listaDao.eliminarItem(listaId, perfilId) == 0) throw DominioException(ErrorDominio.NoEncontrado)
            if (lista.tipo == TipoLista.RANKING) rankings.compactar(listaId)
            listaDao.contarItems(listaId) == 0
        }

    /** CU-18. Lleva un elemento del ranking a [nuevaPosicion] (1..n). Error: M-24. */
    suspend fun mover(
        listaId: Long,
        perfilId: Long,
        nuevaPosicion: Int,
    ) = db.withTransaction {
        val items = listaDao.itemsPorPosicion(listaId).toMutableList()
        val actual = items.indexOfFirst { it.perfilId == perfilId }
        if (actual < 0) throw DominioException(ErrorDominio.NoEncontrado)
        if (nuevaPosicion !in 1..items.size) throw DominioException(ErrorDominio.PosicionInvalida)
        items.add(nuevaPosicion - 1, items.removeAt(actual))
        items.forEachIndexed { indice, item ->
            if (item.posicion != indice + 1) listaDao.actualizarPosicion(listaId, item.perfilId, indice + 1)
        }
    }

    /** CU-19 / RF-38: borra la lista y sus elementos; los perfiles no se tocan. */
    suspend fun eliminar(listaId: Long) =
        db.withTransaction {
            if (listaDao.eliminar(listaId) == 0) throw DominioException(ErrorDominio.NoEncontrado)
        }

    suspend fun obtener(listaId: Long): ListaEntity? = listaDao.obtener(listaId)

    /** Elementos en el orden de su tipo: posición (ranking), prioridad y antigüedad (prioridad) o antigüedad (normal). */
    suspend fun elementos(listaId: Long): List<ListaItemEntity> {
        val lista = listaDao.obtener(listaId) ?: return emptyList()
        return when (lista.tipo) {
            TipoLista.NORMAL -> listaDao.itemsPorAntiguedad(listaId)
            TipoLista.PRIORIDAD -> listaDao.itemsPorPrioridad(listaId)
            TipoLista.RANKING -> listaDao.itemsPorPosicion(listaId)
        }
    }
}
