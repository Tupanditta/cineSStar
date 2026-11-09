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
import com.cinesstar.cinetrack.data.room.PertenenciaLista
import com.cinesstar.cinetrack.data.room.SagaDao
import com.cinesstar.cinetrack.data.room.SagaEntity
import com.cinesstar.cinetrack.domain.DominioException
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Normalizador
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

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
    /**
     * Saga creada en el propio formulario (CU-10 A1). Se registra al guardar el perfil, en la misma transacción, y no
     * antes (CU-10 E4). Si ya existe una con el mismo nombre normalizado, se reutiliza (RF-10). Tiene prioridad sobre
     * [sagaId].
     */
    val sagaNueva: String? = null,
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

/** Todo lo que muestra el detalle de un perfil (CU-07, design.md §7). */
data class FichaPerfil(
    val detalle: PerfilDetalle,
    /** Perfiles de la saga por tipo (incluido este); vacío si no tiene saga. */
    val perfilesSaga: Map<TipoPerfil, Int>,
    val listas: List<PertenenciaLista>,
) {
    /** Mejor puesto del perfil en sus rankings, para el hero (design.md §7.1); null si no está en ninguno. */
    val mejorRanking: PertenenciaLista?
        get() = listas.filter { it.tipo == TipoLista.RANKING && it.posicion != null }.minByOrNull { it.posicion!! }
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

    private companion object {
        val TABLAS_FICHA = arrayOf("perfil", "saga", "perfil_genero", "perfil_idioma", "lista", "lista_item")
    }

    /** CU-01. Devuelve el id del perfil creado. Errores: M-01, M-02. */
    suspend fun crear(nuevo: NuevoPerfil): Long =
        db.withTransaction {
            val nombre = Normalizador.limpiar(nuevo.nombre)
            if (nombre.isEmpty()) throw DominioException(ErrorDominio.NombreObligatorio)
            val nombreBusqueda = Normalizador.normalizar(nombre)
            if (perfilDao.buscarPorNombre(nombreBusqueda, nuevo.tipo) != null) {
                throw DominioException(ErrorDominio.PerfilDuplicado(nuevo.tipo))
            }
            val sagaId = nuevo.sagaNueva?.let { registrarSaga(it) } ?: nuevo.sagaId
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
                        sagaId = sagaId,
                        caratulaUri = nuevo.caratulaUri,
                        creadoEn = ahora,
                    ),
                )
            if (nuevo.generoIds.isNotEmpty()) perfilDao.insertarGeneros(nuevo.generoIds.map { PerfilGeneroEntity(id, it) })
            if (nuevo.idiomaIds.isNotEmpty()) perfilDao.insertarIdiomas(nuevo.idiomaIds.map { PerfilIdiomaEntity(id, it) })
            id
        }

    /** Saga del formulario: la existente con ese nombre normalizado o una nueva. Dentro de la transacción de [crear]. */
    private suspend fun registrarSaga(nombre: String): Long {
        val limpio = Normalizador.limpiar(nombre)
        if (limpio.isEmpty()) throw DominioException(ErrorDominio.SagaNombreObligatorio)
        val nombreBusqueda = Normalizador.normalizar(limpio)
        return sagaDao.buscarPorNombre(nombreBusqueda)?.id ?: sagaDao.insertar(SagaEntity(nombre = limpio, nombreBusqueda = nombreBusqueda))
    }

    /** CU-01 paso 4 / E5: ¿ya hay un perfil con ese nombre (normalizado) y tipo? Un nombre vacío no existe. */
    suspend fun existe(
        nombre: String,
        tipo: TipoPerfil,
    ): Boolean {
        val nombreBusqueda = Normalizador.normalizar(nombre)
        return nombreBusqueda.isNotEmpty() && perfilDao.existe(nombreBusqueda, tipo)
    }

    /** Todos los perfiles, más recientes primero (S-10). */
    fun observarTodos(): Flow<List<PerfilEntity>> = perfilDao.observarTodos()

    /**
     * CU-07: ficha del perfil, que se vuelve a leer cada vez que cambia cualquiera de las tablas que la forman. Emite
     * null si el perfil no existe (o se borra mientras se ve, CU-07 E1).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observarFicha(perfilId: Long): Flow<FichaPerfil?> =
        db.invalidationTracker
            .createFlow(*TABLAS_FICHA)
            .mapLatest { obtenerFicha(perfilId) }

    suspend fun obtenerFicha(perfilId: Long): FichaPerfil? =
        db.withTransaction {
            val detalle = obtener(perfilId) ?: return@withTransaction null
            val sagaId = detalle.perfil.sagaId
            FichaPerfil(
                detalle = detalle,
                perfilesSaga =
                    if (sagaId == null) {
                        emptyMap()
                    } else {
                        TipoPerfil.entries.associateWith { sagaDao.contarPerfiles(sagaId, it) }
                    },
                listas = listaDao.listasDe(perfilId),
            )
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
