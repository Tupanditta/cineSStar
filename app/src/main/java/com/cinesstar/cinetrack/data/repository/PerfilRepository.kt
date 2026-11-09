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

/**
 * Datos para crear un perfil (CU-01) o para sustituir los de uno existente (CU-02). Solo nombre y tipo son
 * obligatorios (RF-03). Al editar se ignoran [tipo] (no se puede cambiar, S-03) y [caratulaUri].
 */
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
@Suppress("TooManyFunctions") // Una operación por caso de uso del perfil (CU-01..CU-07, CU-10).
class PerfilRepository(
    private val db: CineDatabase,
    private val perfilDao: PerfilDao = db.perfilDao(),
    private val sagaDao: SagaDao = db.sagaDao(),
    private val listaDao: ListaDao = db.listaDao(),
    private val reloj: () -> Long = System::currentTimeMillis,
) {
    private val rankings = RankingOps(listaDao)

    companion object {
        /** Longitud máxima del comentario (S-06). */
        const val MAX_COMENTARIO = 10_000

        private val TABLAS_FICHA = arrayOf("perfil", "saga", "perfil_genero", "perfil_idioma", "lista", "lista_item")

        /** Ningún perfil tiene id 0 (autoincremental desde 1). */
        private const val SIN_ID = 0L
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

    /**
     * CU-02 (con CU-03, CU-04, CU-05 y CU-10 desde el formulario): sustituye nombre, estado, puntuación, comentario,
     * saga, géneros e idiomas. El tipo no cambia (S-03) y el perfil conserva sus listas con la misma posición y
     * prioridad. Errores: M-01, M-02 (otro perfil del mismo tipo; cambiar solo mayúsculas o tildes del propio nombre es
     * válido), NoEncontrado. Todo o nada (E3).
     */
    suspend fun actualizar(
        perfilId: Long,
        datos: NuevoPerfil,
    ) = db.withTransaction {
        val actual = perfilDao.obtener(perfilId) ?: throw DominioException(ErrorDominio.NoEncontrado)
        val nombre = Normalizador.limpiar(datos.nombre)
        if (nombre.isEmpty()) throw DominioException(ErrorDominio.NombreObligatorio)
        val nombreBusqueda = Normalizador.normalizar(nombre)
        if (perfilDao.existe(nombreBusqueda, actual.tipo, excluirId = perfilId)) {
            throw DominioException(ErrorDominio.PerfilDuplicado(actual.tipo))
        }
        val sagaId = datos.sagaNueva?.let { registrarSaga(it) } ?: datos.sagaId
        val comentario = datos.comentario?.takeIf { it.isNotBlank() }
        perfilDao.actualizar(
            actual.copy(
                nombre = nombre,
                nombreBusqueda = nombreBusqueda,
                estado = datos.estado,
                puntuacionDecimas = datos.puntuacion?.decimas,
                comentario = comentario,
                comentarioEditadoEn = if (comentario == actual.comentario) actual.comentarioEditadoEn else comentario?.let { reloj() },
                sagaId = sagaId,
            ),
        )
        perfilDao.borrarGeneros(perfilId)
        perfilDao.borrarIdiomas(perfilId)
        if (datos.generoIds.isNotEmpty()) perfilDao.insertarGeneros(datos.generoIds.map { PerfilGeneroEntity(perfilId, it) })
        if (datos.idiomaIds.isNotEmpty()) perfilDao.insertarIdiomas(datos.idiomaIds.map { PerfilIdiomaEntity(perfilId, it) })
    }

    /** CU-05 / RF-15, RF-17: cualquier transición es válida y no toca la puntuación ni el comentario. */
    suspend fun cambiarEstado(
        perfilId: Long,
        estado: Estado,
    ) {
        if (perfilDao.actualizarEstado(perfilId, estado) == 0) throw DominioException(ErrorDominio.NoEncontrado)
    }

    /** CU-03 / RF-13: añade, cambia o (con null) quita la puntuación. Válido en cualquier estado (A5). */
    suspend fun puntuar(
        perfilId: Long,
        puntuacion: Puntuacion?,
    ) {
        if (perfilDao.actualizarPuntuacion(perfilId, puntuacion?.decimas) == 0) throw DominioException(ErrorDominio.NoEncontrado)
    }

    /**
     * CU-04 / RF-14: guarda el comentario íntegro (saltos de línea y emojis incluidos) con la fecha de edición. Un texto
     * vacío o solo con espacios lo borra (A1). Más largo que [MAX_COMENTARIO] no se admite (S-06).
     */
    suspend fun comentar(
        perfilId: Long,
        comentario: String?,
    ) {
        val texto = comentario?.takeIf { it.isNotBlank() }
        require((texto?.length ?: 0) <= MAX_COMENTARIO) { "Comentario de más de $MAX_COMENTARIO caracteres" }
        if (perfilDao.actualizarComentario(perfilId, texto, texto?.let { reloj() }) == 0) {
            throw DominioException(ErrorDominio.NoEncontrado)
        }
    }

    /** Saga del formulario: la existente con ese nombre normalizado o una nueva. Dentro de la transacción de [crear]/[actualizar]. */
    private suspend fun registrarSaga(nombre: String): Long {
        val limpio = Normalizador.limpiar(nombre)
        if (limpio.isEmpty()) throw DominioException(ErrorDominio.SagaNombreObligatorio)
        val nombreBusqueda = Normalizador.normalizar(limpio)
        return sagaDao.buscarPorNombre(nombreBusqueda)?.id ?: sagaDao.insertar(SagaEntity(nombre = limpio, nombreBusqueda = nombreBusqueda))
    }

    /**
     * CU-01 paso 4 / E5: ¿ya hay un perfil con ese nombre (normalizado) y tipo? Un nombre vacío no existe. Al editar,
     * [excluirId] es el propio perfil, que no cuenta como duplicado (CU-02 E2).
     */
    suspend fun existe(
        nombre: String,
        tipo: TipoPerfil,
        excluirId: Long? = null,
    ): Boolean {
        val nombreBusqueda = Normalizador.normalizar(nombre)
        return nombreBusqueda.isNotEmpty() && perfilDao.existe(nombreBusqueda, tipo, excluirId ?: SIN_ID)
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
