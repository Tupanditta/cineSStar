package com.cinesstar.cinetrack.ui.perfilform

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinesstar.cinetrack.data.repository.CatalogoRepository
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.repository.PerfilDetalle
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.repository.SagaRepository
import com.cinesstar.cinetrack.data.room.GeneroEntity
import com.cinesstar.cinetrack.data.room.IdiomaEntity
import com.cinesstar.cinetrack.data.room.SagaEntity
import com.cinesstar.cinetrack.domain.DominioException
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Normalizador
import com.cinesstar.cinetrack.domain.PasoPuntuacion
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.TipoPerfil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lo que el usuario ha escrito en el formulario. Se guarda en el [SavedStateHandle] para sobrevivir a la rotación y al
 * cierre del proceso en segundo plano (CU-23 A2, S-21).
 */
@Serializable
data class Borrador(
    val tipo: TipoPerfil = TipoPerfil.PELICULA,
    val nombre: String = "",
    val estado: Estado = Estado.PENDIENTE,
    val puntuacion: String = "",
    /** Saga registrada elegida; null = "Ninguna / Obra individual" (o la saga nueva, si [sagaNuevaElegida]). */
    val sagaId: Long? = null,
    /** Saga creada en este formulario, aún sin registrar (CU-10 A1, E4). */
    val sagaNueva: String? = null,
    val sagaNuevaElegida: Boolean = false,
    val generoIds: Set<Long> = emptySet(),
    val idiomaIds: Set<Long> = emptySet(),
    val comentario: String = "",
    /** Caja en línea "Nueva saga local" (design.md §8.6). */
    val editorSagaAbierto: Boolean = false,
    val editorSagaTexto: String = "",
    /** El usuario ya intentó guardar: se muestran los errores pendientes (M-01). */
    val intentoGuardar: Boolean = false,
) {
    /** Solo los datos del perfil, sin el estado de la interfaz (caja de saga, intento de guardar). */
    fun datos(): Borrador = copy(editorSagaAbierto = false, editorSagaTexto = "", intentoGuardar = false)

    /** Hay datos introducidos: cancelar pide confirmación (CU-01 A7). */
    val tieneDatos: Boolean get() = datos() != Borrador()

    companion object {
        /** Borrador con los datos actuales de un perfil, para editarlo (CU-02 paso 1). */
        fun de(detalle: PerfilDetalle): Borrador =
            Borrador(
                tipo = detalle.perfil.tipo,
                nombre = detalle.perfil.nombre,
                estado = detalle.perfil.estado,
                puntuacion = detalle.puntuacion?.formatear().orEmpty(),
                sagaId = detalle.perfil.sagaId,
                generoIds = detalle.generos.map { it.id }.toSet(),
                idiomaIds = detalle.idiomas.map { it.id }.toSet(),
                comentario = detalle.perfil.comentario.orEmpty(),
            )
    }
}

/** Resultado de la comprobación en línea del nombre (design.md §8.3). */
enum class Disponibilidad { VACIO, COMPROBANDO, DISPONIBLE, YA_REGISTRADO }

/** Error de la caja "Nueva saga local" (CU-10 E1/E2). */
sealed interface ErrorSaga {
    /** M-05 */
    data object Vacia : ErrorSaga

    /** M-04, con la saga existente para ofrecer seleccionarla (S-13). */
    data class Duplicada(
        val existente: SagaEntity,
    ) : ErrorSaga
}

/** Opción del desplegable de saga. */
data class OpcionSaga(
    val nombre: String,
    /** null = saga nueva sin registrar. */
    val sagaId: Long?,
)

data class EstadoFormulario(
    val borrador: Borrador = Borrador(),
    /** Al editar (CU-02), los datos guardados del perfil; null al crear o mientras se cargan. */
    val original: Borrador? = null,
    /** Se está editando un perfil existente: el tipo no se puede cambiar (S-03). */
    val edicion: Boolean = false,
    /** El perfil que se editaba ya no existe: la pantalla vuelve atrás con un aviso. */
    val noExiste: Boolean = false,
    val generos: List<GeneroEntity> = emptyList(),
    val idiomas: List<IdiomaEntity> = emptyList(),
    val sagas: List<SagaEntity> = emptyList(),
    val disponibilidad: Disponibilidad = Disponibilidad.VACIO,
    val errorSaga: ErrorSaga? = null,
    val guardando: Boolean = false,
    /** M-25: falló la escritura (CU-01 E4). */
    val errorEscritura: Boolean = false,
    /** Id del perfil recién guardado: la pantalla vuelve atrás (CU-01 paso 6). */
    val guardadoId: Long? = null,
) {
    val lecturaPuntuacion: Puntuacion.Lectura get() = Puntuacion.leer(borrador.puntuacion)

    /** E1: nombre vacío o solo espacios. */
    val nombreVacio: Boolean get() = borrador.nombre.isBlank()

    /** M-01: se ve si el nombre tiene solo espacios, si se intentó guardar sin nombre o si se vació al editar (CU-02 E1). */
    val mostrarNombreObligatorio: Boolean
        get() = nombreVacio && (borrador.nombre.isNotEmpty() || borrador.intentoGuardar || (edicion && original != null))

    /** Cancelar pide confirmación: al crear, si hay datos (CU-01 A7); al editar, si hay cambios (CU-02 A4). */
    val hayCambios: Boolean
        get() = if (edicion) original != null && borrador.datos() != original.datos() else borrador.tieneDatos

    /** "Guardar perfil" habilitado (design.md §8.10). */
    val puedeGuardar: Boolean
        get() =
            !nombreVacio &&
                disponibilidad == Disponibilidad.DISPONIBLE &&
                lecturaPuntuacion != Puntuacion.Lectura.Invalida &&
                !guardando &&
                guardadoId == null

    val opcionesSaga: List<OpcionSaga>
        get() = sagas.map { OpcionSaga(it.nombre, it.id) } + listOfNotNull(borrador.sagaNueva?.let { OpcionSaga(it, null) })

    /** Nombre de la saga elegida; null = ninguna. */
    val sagaElegida: String?
        get() =
            if (borrador.sagaNuevaElegida) {
                borrador.sagaNueva
            } else {
                sagas.firstOrNull { it.id == borrador.sagaId }?.nombre
            }
}

/**
 * CU-01 Crear perfil y, con [perfilId], CU-02 Editar datos del perfil. Incluye CU-03 (puntuación), CU-04 (comentario),
 * CU-05 (estado) y CU-10 (saga). Una función por evento del formulario (UDF).
 */
@Suppress("TooManyFunctions")
class FormularioPerfilViewModel(
    private val handle: SavedStateHandle,
    private val perfiles: PerfilRepository,
    private val sagas: SagaRepository,
    private val catalogo: CatalogoRepository,
    private val perfilId: Long? = null,
) : ViewModel() {
    private val estadoInterno =
        MutableStateFlow(
            EstadoFormulario(
                borrador = handle.get<String>(CLAVE_BORRADOR)?.let(::leerBorrador) ?: Borrador(),
                original = handle.get<String>(CLAVE_ORIGINAL)?.let(::leerBorrador),
                edicion = perfilId != null,
            ),
        )
    val estado: StateFlow<EstadoFormulario> get() = estadoInterno

    init {
        viewModelScope.launch {
            val generos = catalogo.generos()
            val idiomas = catalogo.idiomas()
            val registradas = sagas.todas()
            estadoInterno.update { it.copy(generos = generos, idiomas = idiomas, sagas = registradas) }
        }
        if (perfilId != null && estadoInterno.value.original == null) cargar(perfilId)
        comprobarDisponibilidad()
        // Cada cambio del borrador se guarda para restaurarlo (S-21).
        viewModelScope.launch {
            estadoInterno.map { it.borrador }.distinctUntilChanged().collect { handle[CLAVE_BORRADOR] = Json.encodeToString(it) }
        }
    }

    /** CU-02 paso 1: el formulario se abre con los datos actuales del perfil. */
    private fun cargar(id: Long) {
        viewModelScope.launch {
            val detalle = perfiles.obtener(id)
            if (detalle == null) {
                estadoInterno.update { it.copy(noExiste = true) }
                return@launch
            }
            val original = Borrador.de(detalle)
            handle[CLAVE_ORIGINAL] = Json.encodeToString(original)
            estadoInterno.update { it.copy(borrador = original, original = original) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun comprobarDisponibilidad() {
        // Comprobación en línea del duplicado (CU-01 paso 4); se repite al cambiar el tipo (E5).
        estadoInterno
            .map { Normalizador.normalizar(it.borrador.nombre) to it.borrador.tipo }
            .distinctUntilChanged()
            .transformLatest { (nombre, tipo) ->
                if (nombre.isEmpty()) {
                    emit(Disponibilidad.VACIO)
                } else {
                    emit(Disponibilidad.COMPROBANDO)
                    // Al editar, el propio nombre (o el mismo con otras mayúsculas o tildes) está disponible (CU-02 E2).
                    val ocupado = perfiles.existe(nombre, tipo, excluirId = perfilId)
                    emit(if (ocupado) Disponibilidad.YA_REGISTRADO else Disponibilidad.DISPONIBLE)
                }
            }.onEach { d -> estadoInterno.update { it.copy(disponibilidad = d) } }
            .launchIn(viewModelScope)
    }

    private fun editar(cambio: (Borrador) -> Borrador) {
        estadoInterno.update { it.copy(borrador = cambio(it.borrador), errorEscritura = false) }
    }

    /** El tipo solo se elige al crear (S-03). */
    fun elegirTipo(tipo: TipoPerfil) {
        if (!estadoInterno.value.edicion) editar { it.copy(tipo = tipo) }
    }

    fun cambiarNombre(nombre: String) = editar { it.copy(nombre = nombre) }

    fun elegirEstado(estado: Estado) = editar { it.copy(estado = estado) }

    fun cambiarPuntuacion(texto: String) = editar { it.copy(puntuacion = texto) }

    fun pasoPuntuacion(delta: Int) = editar { it.copy(puntuacion = PasoPuntuacion.aplicar(it.puntuacion, delta)) }

    fun moverSlider(valor: Float) = editar { it.copy(puntuacion = PasoPuntuacion.desdeSlider(valor)) }

    fun alternarGenero(id: Long) = editar { it.copy(generoIds = it.generoIds alternar id) }

    fun alternarIdioma(id: Long) = editar { it.copy(idiomaIds = it.idiomaIds alternar id) }

    fun cambiarComentario(texto: String) = editar { it.copy(comentario = texto.take(MAX_COMENTARIO)) }

    /** CU-10 FP: elige una saga registrada, o ninguna con null. */
    fun elegirSaga(sagaId: Long?) = editar { it.copy(sagaId = sagaId, sagaNuevaElegida = false) }

    fun elegirSagaNueva() = editar { it.copy(sagaId = null, sagaNuevaElegida = it.sagaNueva != null) }

    fun abrirEditorSaga() {
        estadoInterno.update { it.copy(errorSaga = null) }
        editar { it.copy(editorSagaAbierto = true, editorSagaTexto = "") }
    }

    /** CU-10 E3: cerrar la caja no registra nada. */
    fun cerrarEditorSaga() {
        estadoInterno.update { it.copy(errorSaga = null) }
        editar { it.copy(editorSagaAbierto = false, editorSagaTexto = "") }
    }

    fun cambiarTextoSaga(texto: String) {
        estadoInterno.update { it.copy(errorSaga = null) }
        editar { it.copy(editorSagaTexto = texto) }
    }

    /**
     * CU-10 A1: la saga queda elegida pero no se registra hasta guardar el perfil (E4). Errores: M-05 (vacía) y M-04
     * (ya registrada, con opción de elegir la existente, S-13).
     */
    fun confirmarSagaNueva() {
        val actual = estadoInterno.value
        val nombre = Normalizador.limpiar(actual.borrador.editorSagaTexto)
        val existente = actual.sagas.firstOrNull { it.nombreBusqueda == Normalizador.normalizar(nombre) }
        when {
            nombre.isEmpty() -> estadoInterno.update { it.copy(errorSaga = ErrorSaga.Vacia) }
            existente != null -> estadoInterno.update { it.copy(errorSaga = ErrorSaga.Duplicada(existente)) }
            else ->
                editar {
                    it.copy(sagaNueva = nombre, sagaNuevaElegida = true, sagaId = null, editorSagaAbierto = false, editorSagaTexto = "")
                }
        }
    }

    /** S-13: elegir la saga existente que se intentaba duplicar. */
    fun elegirSagaExistente(saga: SagaEntity) {
        estadoInterno.update { it.copy(errorSaga = null) }
        editar { it.copy(sagaId = saga.id, sagaNuevaElegida = false, editorSagaAbierto = false, editorSagaTexto = "") }
    }

    /**
     * CU-01 pasos 5–6 y CU-02 pasos 3–4. Una doble pulsación guarda una sola vez (CU-01 E6). Al editar, guardar sin
     * cambios no escribe nada (CU-02 A3).
     */
    @Suppress("TooGenericExceptionCaught") // Cualquier fallo de escritura se muestra como M-25 (CU-01 E4, CU-02 E3).
    fun guardar() {
        val actual = estadoInterno.value
        if (actual.guardando || actual.guardadoId != null) return
        editar { it.copy(intentoGuardar = true) }
        if (!actual.puedeGuardar) return
        if (perfilId != null && !actual.hayCambios) {
            estadoInterno.update { it.copy(guardadoId = perfilId) }
            return
        }
        estadoInterno.update { it.copy(guardando = true) }
        val b = actual.borrador
        val datos =
            NuevoPerfil(
                nombre = b.nombre,
                tipo = b.tipo,
                estado = b.estado,
                puntuacion = (actual.lecturaPuntuacion as? Puntuacion.Lectura.Valida)?.puntuacion,
                comentario = b.comentario,
                sagaId = b.sagaId.takeUnless { b.sagaNuevaElegida },
                sagaNueva = b.sagaNueva.takeIf { b.sagaNuevaElegida },
                generoIds = b.generoIds,
                idiomaIds = b.idiomaIds,
            )
        viewModelScope.launch {
            try {
                val id = perfilId?.also { perfiles.actualizar(it, datos) } ?: perfiles.crear(datos)
                estadoInterno.update { it.copy(guardando = false, guardadoId = id) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: DominioException) {
                val disponibilidad =
                    if (e.error is ErrorDominio.PerfilDuplicado) Disponibilidad.YA_REGISTRADO else estadoInterno.value.disponibilidad
                estadoInterno.update {
                    it.copy(guardando = false, disponibilidad = disponibilidad, errorEscritura = e.error !is ErrorDominio.PerfilDuplicado)
                }
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo guardar el perfil", e)
                estadoInterno.update { it.copy(guardando = false, errorEscritura = true) }
            }
        }
    }

    companion object {
        const val CLAVE_BORRADOR = "borrador"
        const val CLAVE_ORIGINAL = "original"
        private const val TAG = "FormularioPerfil"

        /** Longitud máxima del comentario (S-06). */
        const val MAX_COMENTARIO = PerfilRepository.MAX_COMENTARIO

        private val json = Json { ignoreUnknownKeys = true }

        private fun leerBorrador(texto: String): Borrador? = runCatching { json.decodeFromString<Borrador>(texto) }.getOrNull()

        private infix fun Set<Long>.alternar(id: Long): Set<Long> = if (id in this) this - id else this + id
    }
}
