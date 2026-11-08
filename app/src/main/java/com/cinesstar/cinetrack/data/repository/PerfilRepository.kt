package com.cinesstar.cinetrack.data.repository

import androidx.room.withTransaction
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.data.room.GeneroEntity
import com.cinesstar.cinetrack.data.room.IdiomaEntity
import com.cinesstar.cinetrack.data.room.ListaDao
import com.cinesstar.cinetrack.data.room.PerfilDao
import com.cinesstar.cinetrack.data.room.PerfilEntity
import com.cinesstar.cinetrack.data.room.PerfilGeneroEntity
import com.cinesstar.cinetrack.data.room.PerfilIdiomaEntity
import com.cinesstar.cinetrack.data.room.SagaDao
import com.cinesstar.cinetrack.data.room.SagaEntity
import com.cinesstar.cinetrack.domain.DominioException
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Normalizador
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.TipoPerfil

/** Datos para crear un perfil. Solo nombre y tipo son obligatorios (RF-03). */
data class NuevoPerfil(
    val nombre: String,
    val tipo: TipoPerfil,
    val estado: Estado = Estado.PENDIENTE,
    val puntuacion: Puntuacion? = null,
    val comentario: String? = null,
    val sagaId: Long? = null,
    val generoIds: Set<Long> = emptySet(),
    val idiomaIds: Set<Long> = emptySet(),
    val caratulaUri: String? = null,
)

/** Perfil con sus relaciones, para la vista de detalle. */
data class PerfilDetalle(
    val perfil: PerfilEntity,
    val saga: SagaEntity?,
    val generos: List<GeneroEntity>,
    val idiomas: List<IdiomaEntity>,
) {
    val puntuacion: Puntuacion? get() = perfil.puntuacionDecimas?.let { Puntuacion.of(it) }
}

/**
 * Operaciones sobre perfiles. Cada operación de escritura es una única transacción: o se aplica entera o no deja
 * ningún dato parcial (CU-01 E4, CU-06 E1).
 */
class PerfilRepository(
    private val db: CineDatabase,
    private val perfilDao: PerfilDao = db.perfilDao(),
    private val sagaDao: SagaDao = db.sagaDao(),
    private val listaDao: ListaDao = db.listaDao(),
    private val reloj: () -> Long = System::currentTimeMillis,
) {
    private val rankings = RankingOps(listaDao)

    /** CU-01. Devuelve el id del perfil creado. Errores: M-01, M-02. */
    suspend fun crear(nuevo: NuevoPerfil): Long =
        db.withTransaction {
            val nombre = Normalizador.limpiar(nuevo.nombre)
            if (nombre.isEmpty()) throw DominioException(ErrorDominio.NombreObligatorio)
            val nombreBusqueda = Normalizador.normalizar(nombre)
            if (perfilDao.buscarPorNombre(nombreBusqueda, nuevo.tipo) != null) {
                throw DominioException(ErrorDominio.PerfilDuplicado(nuevo.tipo))
            }
            val ahora = reloj()
            val comentario = nuevo.comentario?.takeIf { it.isNotBlank() }
            val id =
                perfilDao.insertar(
                    PerfilEntity(
                        nombre = nombre,
                        nombreBusqueda = nombreBusqueda,
                        tipo = nuevo.tipo,
                        estado = nuevo.estado,
                        puntuacionDecimas = nuevo.puntuacion?.decimas,
                        comentario = comentario,
                        comentarioEditadoEn = comentario?.let { ahora },
                        sagaId = nuevo.sagaId,
                        caratulaUri = nuevo.caratulaUri,
                        creadoEn = ahora,
                    ),
                )
            if (nuevo.generoIds.isNotEmpty()) perfilDao.insertarGeneros(nuevo.generoIds.map { PerfilGeneroEntity(id, it) })
            if (nuevo.idiomaIds.isNotEmpty()) perfilDao.insertarIdiomas(nuevo.idiomaIds.map { PerfilIdiomaEntity(id, it) })
            id
        }

    suspend fun obtener(perfilId: Long): PerfilDetalle? =
        db.withTransaction {
            val perfil = perfilDao.obtener(perfilId) ?: return@withTransaction null
            PerfilDetalle(
                perfil = perfil,
                saga = perfil.sagaId?.let { sagaDao.obtener(it) },
                generos = perfilDao.generosDe(perfilId),
                idiomas = perfilDao.idiomasDe(perfilId),
            )
        }

    /** RF-09: un perfil tiene como máximo una saga; asignar otra reemplaza la anterior y `null` la quita. */
    suspend fun asignarSaga(
        perfilId: Long,
        sagaId: Long?,
    ) = db.withTransaction {
        if (sagaId != null && sagaDao.obtener(sagaId) == null) throw DominioException(ErrorDominio.NoEncontrado)
        if (perfilDao.asignarSaga(perfilId, sagaId) == 0) throw DominioException(ErrorDominio.NoEncontrado)
    }

    /**
     * CU-06 / RF-18, RF-20: borra el perfil, sus géneros e idiomas y sus elementos en todas las listas (`ON DELETE
     * CASCADE`), y recompacta las posiciones de los rankings afectados (S-07). Todo en una transacción.
     */
    suspend fun eliminar(perfilId: Long) =
        db.withTransaction {
            val afectados = listaDao.rankingsCon(perfilId)
            if (perfilDao.eliminar(perfilId) == 0) throw DominioException(ErrorDominio.NoEncontrado)
            afectados.forEach { rankings.compactar(it) }
        }
}
