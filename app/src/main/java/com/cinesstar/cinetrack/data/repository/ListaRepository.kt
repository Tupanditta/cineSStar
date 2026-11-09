package com.cinesstar.cinetrack.data.repository

import androidx.room.withTransaction
import com.cinesstar.cinetrack.data.room.BibliotecaDao
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.data.room.FilaResumenLista
import com.cinesstar.cinetrack.data.room.ListaDao
import com.cinesstar.cinetrack.data.room.ListaEntity
import com.cinesstar.cinetrack.data.room.ListaItemEntity
import com.cinesstar.cinetrack.data.room.PerfilDao
import com.cinesstar.cinetrack.domain.Candidato
import com.cinesstar.cinetrack.domain.ContenidoLista
import com.cinesstar.cinetrack.domain.DominioException
import com.cinesstar.cinetrack.domain.ElementoLista
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Normalizador
import com.cinesstar.cinetrack.domain.OpcionLista
import com.cinesstar.cinetrack.domain.OrdenLista
import com.cinesstar.cinetrack.domain.PanelListas
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.ResumenLista
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.exigirAdmite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

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
@Suppress("TooManyFunctions") // Una operación por caso de uso de las listas (CU-11..CU-19).
class ListaRepository(
    private val db: CineDatabase,
    private val listaDao: ListaDao = db.listaDao(),
    private val perfilDao: PerfilDao = db.perfilDao(),
    private val bibliotecaDao: BibliotecaDao = db.bibliotecaDao(),
    private val reloj: () -> Long = System::currentTimeMillis,
) {
    private val rankings = RankingOps(listaDao)

    private companion object {
        val TABLAS = arrayOf("lista", "lista_item", "perfil", "saga", "perfil_genero")
        const val SEPARADOR_GENEROS = " / "
    }

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
        // S-17: la lista vuelve a tener elementos; si se vacía otra vez, el aviso se mostrará de nuevo.
        if (lista.avisoVaciaMostrado) listaDao.marcarAvisoVacia(listaId, false)
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

    /**
     * CU-16. Quita un perfil de la lista; en un ranking las posiciones posteriores suben (S-07). Devuelve si hay que
     * mostrar el aviso único M-11 (CU-16 A1): la lista acaba de quedar vacía y el aviso no se ha mostrado en este
     * vaciado (S-17). Lo deja marcado como mostrado.
     */
    suspend fun quitar(
        listaId: Long,
        perfilId: Long,
    ): Boolean =
        db.withTransaction {
            val lista = listaDao.obtener(listaId) ?: throw DominioException(ErrorDominio.NoEncontrado)
            if (listaDao.eliminarItem(listaId, perfilId) == 0) throw DominioException(ErrorDominio.NoEncontrado)
            if (lista.tipo == TipoLista.RANKING) rankings.compactar(listaId)
            val avisar = listaDao.contarItems(listaId) == 0 && !lista.avisoVaciaMostrado
            if (avisar) listaDao.marcarAvisoVacia(listaId, true)
            avisar
        }

    /**
     * CU-14. Cambia solo el nombre. Devuelve false si es el mismo (A2: sin cambios). Errores: M-07, M-08 (otra lista
     * con el mismo nombre normalizado, S-15); cambiar solo mayúsculas o tildes es válido.
     */
    suspend fun renombrar(
        listaId: Long,
        nombre: String,
    ): Boolean =
        db.withTransaction {
            val lista = listaDao.obtener(listaId) ?: throw DominioException(ErrorDominio.NoEncontrado)
            val limpio = Normalizador.limpiar(nombre)
            if (limpio.isEmpty()) throw DominioException(ErrorDominio.ListaNombreObligatorio)
            if (limpio == lista.nombre) return@withTransaction false
            val nombreBusqueda = Normalizador.normalizar(limpio)
            val otra = listaDao.buscarPorNombre(nombreBusqueda)
            if (otra != null && otra.id != listaId) throw DominioException(ErrorDominio.ListaDuplicada)
            if (listaDao.renombrar(listaId, limpio, nombreBusqueda) == 0) throw DominioException(ErrorDominio.NoEncontrado)
            true
        }

    /**
     * CU-18. Lleva un elemento del ranking a [nuevaPosicion] (1..n); los intermedios se desplazan y las posiciones quedan
     * contiguas 1..n. Devuelve false si ya estaba en esa posición (A2: sin cambios). Solo cambia esta lista (RES-07).
     * Errores: M-24 (posición fuera de 1..n o la lista no es un ranking).
     */
    suspend fun mover(
        listaId: Long,
        perfilId: Long,
        nuevaPosicion: Int,
    ): Boolean =
        db.withTransaction {
            val lista = listaDao.obtener(listaId) ?: throw DominioException(ErrorDominio.NoEncontrado)
            if (lista.tipo != TipoLista.RANKING) throw DominioException(ErrorDominio.PosicionInvalida)
            val items = listaDao.itemsPorPosicion(listaId).toMutableList()
            val actual = items.indexOfFirst { it.perfilId == perfilId }
            if (actual < 0) throw DominioException(ErrorDominio.NoEncontrado)
            if (nuevaPosicion !in 1..items.size) throw DominioException(ErrorDominio.PosicionInvalida)
            if (actual == nuevaPosicion - 1) return@withTransaction false
            items.add(nuevaPosicion - 1, items.removeAt(actual))
            items.forEachIndexed { indice, item ->
                if (item.posicion != indice + 1) listaDao.actualizarPosicion(listaId, item.perfilId, indice + 1)
            }
            true
        }

    /**
     * CU-17. Cambia la prioridad de un elemento solo en esta lista (A2, RES-07); el perfil no cambia. Devuelve false si
     * ya tenía ese nivel (A1: sin cambios). Dentro de un nivel se sigue ordenando por antigüedad en la lista.
     */
    suspend fun cambiarPrioridad(
        listaId: Long,
        perfilId: Long,
        prioridad: Prioridad,
    ): Boolean =
        db.withTransaction {
            val lista = listaDao.obtener(listaId) ?: throw DominioException(ErrorDominio.NoEncontrado)
            val item = listaDao.item(listaId, perfilId) ?: throw DominioException(ErrorDominio.NoEncontrado)
            if (lista.tipo != TipoLista.PRIORIDAD) throw DominioException(ErrorDominio.NoEncontrado)
            if (item.prioridad == prioridad) return@withTransaction false
            if (listaDao.actualizarPrioridad(listaId, perfilId, prioridad) == 0) throw DominioException(ErrorDominio.NoEncontrado)
            true
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

    /** CU-12: todas las listas y el total de perfiles; se vuelve a leer cuando cambian. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observarPanel(): Flow<PanelListas> =
        db.invalidationTracker.createFlow(*TABLAS).mapLatest {
            db.withTransaction { PanelListas(listaDao.resumenes().map { it.aDominio() }, perfilDao.contar()) }
        }

    /** CU-13: la lista y sus elementos en el orden de su tipo; null si la lista ya no existe (E1). */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observarContenido(listaId: Long): Flow<ContenidoLista?> =
        db.invalidationTracker.createFlow(*TABLAS).mapLatest { leerContenido(listaId) }

    suspend fun leerContenido(listaId: Long): ContenidoLista? =
        db.withTransaction {
            val resumen = listaDao.resumenes().firstOrNull { it.id == listaId }?.aDominio() ?: return@withTransaction null
            val generos = listaDao.generosDeLista(listaId).groupBy({ it.perfilId }, { it.nombre })
            val elementos =
                listaDao.filasElementos(listaId).map { f ->
                    ElementoLista(
                        perfilId = f.perfilId,
                        nombre = f.nombre,
                        tipo = f.tipo,
                        puntuacion = f.puntuacionDecimas?.let { Puntuacion.of(it) },
                        saga = f.sagaNombre,
                        generos = generos[f.perfilId].orEmpty(),
                        posicion = f.posicion,
                        prioridad = f.prioridad,
                        anadidoEn = f.anadidoEn,
                    )
                }
            ContenidoLista(resumen, OrdenLista.ordenar(resumen.tipo, elementos))
        }

    /** CU-15: perfiles para el selector "Añadir título", con su disponibilidad en la lista; se actualiza al añadir. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observarCandidatos(listaId: Long): Flow<List<Candidato>> =
        db.invalidationTracker.createFlow(*TABLAS).mapLatest {
            db.withTransaction {
                val lista = listaDao.obtener(listaId) ?: return@withTransaction emptyList()
                val generos = bibliotecaDao.generosDeTodos().groupBy({ it.perfilId }, { it.nombre })
                listaDao.candidatos(listaId).map { c ->
                    Candidato(
                        perfilId = c.id,
                        nombre = c.nombre,
                        nombreBusqueda = c.nombreBusqueda,
                        tipo = c.tipo,
                        detalle = c.sagaNombre ?: generos[c.id]?.joinToString(SEPARADOR_GENEROS),
                        enLista = c.enLista,
                        admitido = lista.restriccion.admite(c.tipo),
                    )
                }
            }
        }

    /** CU-15 desde el perfil: todas las listas, indicando si ya lo contienen o si su restricción no lo admite. */
    suspend fun opcionesPara(perfilId: Long): List<OpcionLista> =
        db.withTransaction {
            val perfil = perfilDao.obtener(perfilId) ?: throw DominioException(ErrorDominio.NoEncontrado)
            val contienen = listaDao.listasDe(perfilId).map { it.listaId }.toSet()
            listaDao.resumenes().map { f ->
                OpcionLista(f.aDominio(), contiene = f.id in contienen, admite = f.restriccion.admite(perfil.tipo))
            }
        }

    private fun FilaResumenLista.aDominio() = ResumenLista(id, nombre, tipo, restriccion, total, altas, primero)
}
