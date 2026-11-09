package com.cinesstar.cinetrack.ui.buscar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinesstar.cinetrack.data.repository.BusquedaRepository
import com.cinesstar.cinetrack.data.repository.CatalogoRepository
import com.cinesstar.cinetrack.domain.Ambito
import com.cinesstar.cinetrack.domain.CriteriosBusqueda
import com.cinesstar.cinetrack.domain.CriteriosBusqueda.Companion.conRango
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Etiqueta
import com.cinesstar.cinetrack.domain.PerfilBiblioteca
import com.cinesstar.cinetrack.domain.Puntuacion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

/**
 * Lo que el usuario ha escrito o elegido en "Filtros Cine" (design.md §9.2). La puntuación se guarda como texto, tal
 * como se escribe en la píldora del rango, para poder señalar un valor inválido (CU-20 E3).
 */
data class FormularioBusqueda(
    val nombre: String = "",
    val ambito: Ambito = Ambito.TODO,
    val textoMinimo: String = TEXTO_MINIMO,
    val textoMaximo: String = TEXTO_MAXIMO,
    val generoIds: Set<Long> = emptySet(),
    val idiomaId: Long? = null,
    val sagaId: Long? = null,
    val estados: Set<Estado> = emptySet(),
) {
    val lecturaMinimo: Puntuacion.Lectura get() = Puntuacion.leer(textoMinimo)
    val lecturaMaximo: Puntuacion.Lectura get() = Puntuacion.leer(textoMaximo)

    /** M-03 si un extremo no es una puntuación válida; si no, M-14 si el mínimo supera al máximo; null si todo es válido. */
    val error: ErrorDominio?
        get() =
            if (lecturaMinimo is Puntuacion.Lectura.Invalida || lecturaMaximo is Puntuacion.Lectura.Invalida) {
                ErrorDominio.PuntuacionInvalida
            } else {
                criterios().error()
            }

    /** Un extremo vacío es "sin límite" (0.0 o 10, CU-20). Solo tiene sentido si [error] no es M-03. */
    fun criterios(): CriteriosBusqueda =
        CriteriosBusqueda(
            nombre = nombre,
            ambito = ambito,
            generoIds = generoIds,
            idiomaId = idiomaId,
            sagaId = sagaId,
            estados = estados,
        ).conRango(lecturaMinimo.valor(), lecturaMaximo.valor())

    /** El rango excluye los títulos sin puntuar ("★ Excluye títulos sin puntuar", design.md §9.2-3). */
    val rangoActivo: Boolean get() = error != ErrorDominio.PuntuacionInvalida && criterios().rangoActivo

    /** Posición de los pulgares del slider (0..10); un extremo vacío o inválido queda en su tope. */
    val posicionSlider: ClosedFloatingPointRange<Float>
        get() {
            val min = lecturaMinimo.valor()?.let { it.decimas / DECIMAS } ?: 0f
            val max = lecturaMaximo.valor()?.let { it.decimas / DECIMAS } ?: MAX_SLIDER
            return minOf(min, max)..maxOf(min, max)
        }

    companion object {
        val TEXTO_MINIMO: String = Puntuacion.of(Puntuacion.MIN_DECIMAS).formatear()
        val TEXTO_MAXIMO: String = Puntuacion.of(Puntuacion.MAX_DECIMAS).formatear()
        const val MAX_SLIDER = 10f
        const val DECIMAS = 10f
    }
}

private fun Puntuacion.Lectura.valor(): Puntuacion? = (this as? Puntuacion.Lectura.Valida)?.puntuacion

/** Resultado de la búsqueda con los criterios actuales. */
sealed interface Resultados {
    data object Cargando : Resultados

    /** CU-20 E2/E3: los criterios no son válidos (M-03 o M-14) y no se busca. */
    data class Invalidos(
        val error: ErrorDominio,
    ) : Resultados

    /** No se pudo leer la base de datos (M-25). */
    data object Fallo : Resultados

    /** Perfiles que cumplen todos los criterios (vacía = M-13). */
    data class Lista(
        val perfiles: List<PerfilBiblioteca>,
    ) : Resultados
}

/** Opciones de los filtros: catálogo de géneros e idiomas y sagas registradas. */
data class OpcionesBusqueda(
    val generos: List<Etiqueta> = emptyList(),
    val idiomas: List<Etiqueta> = emptyList(),
    val sagas: List<Etiqueta> = emptyList(),
)

data class EstadoBuscar(
    val formulario: FormularioBusqueda = FormularioBusqueda(),
    val opciones: OpcionesBusqueda = OpcionesBusqueda(),
    val resultados: Resultados = Resultados.Cargando,
)

/**
 * CU-20 Buscar con filtros. Los resultados se actualizan al cambiar cualquier criterio (paso 3) y cuando cambian los
 * datos; "Aplicar Filtros" lleva a ellos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("TooManyFunctions") // Una función por evento de la pantalla (UDF).
class BuscarViewModel(
    private val busqueda: BusquedaRepository,
    private val catalogo: CatalogoRepository,
) : ViewModel() {
    private val formulario = MutableStateFlow(FormularioBusqueda())

    private val resultados: Flow<Resultados> =
        formulario
            .map { f -> f.error to f.criterios() }
            .distinctUntilChanged()
            .flatMapLatest { (error, criterios) ->
                if (error != null) {
                    flowOf(Resultados.Invalidos(error))
                } else {
                    busqueda
                        .observar(criterios)
                        .map<List<PerfilBiblioteca>, Resultados> { Resultados.Lista(it) }
                        .catch { e ->
                            if (e is CancellationException) throw e
                            Log.e(TAG, "No se pudo buscar", e)
                            emit(Resultados.Fallo)
                        }
                }
            }

    private val opciones: Flow<OpcionesBusqueda> =
        flow {
            val generos = catalogo.generos().map { Etiqueta(it.id, it.nombre, it.orden) }
            val idiomas = catalogo.idiomas().map { Etiqueta(it.id, it.nombre, it.orden) }
            emit(OpcionesBusqueda(generos, idiomas))
            busqueda.observarSagas().collect { emit(OpcionesBusqueda(generos, idiomas, it)) }
        }.catch { e ->
            if (e is CancellationException) throw e
            Log.e(TAG, "No se pudo leer el catálogo", e)
            emit(OpcionesBusqueda())
        }

    val estado: StateFlow<EstadoBuscar> =
        combine(formulario, opciones, resultados) { f, o, r -> EstadoBuscar(f, o, r) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUSCRIPCION_MS), EstadoBuscar())

    fun escribirNombre(texto: String) = formulario.update { it.copy(nombre = texto) }

    fun elegirAmbito(ambito: Ambito) = formulario.update { it.copy(ambito = ambito) }

    fun escribirMinimo(texto: String) = formulario.update { it.copy(textoMinimo = texto) }

    fun escribirMaximo(texto: String) = formulario.update { it.copy(textoMaximo = texto) }

    /** El slider fija los dos extremos (paso 0.1, RNF-05). */
    fun moverRango(rango: ClosedFloatingPointRange<Float>) =
        formulario.update {
            it.copy(textoMinimo = formatear(rango.start), textoMaximo = formatear(rango.endInclusive))
        }

    fun alternarGenero(id: Long) =
        formulario.update {
            it.copy(
                generoIds =
                    if (id in
                        it.generoIds
                    ) {
                        it.generoIds - id
                    } else {
                        it.generoIds + id
                    },
            )
        }

    /** "Deseleccionar" (design.md §9.2-4). */
    fun quitarGeneros() = formulario.update { it.copy(generoIds = emptySet()) }

    fun elegirIdioma(id: Long?) = formulario.update { it.copy(idiomaId = id) }

    fun elegirSaga(id: Long?) = formulario.update { it.copy(sagaId = id) }

    fun alternarEstado(estado: Estado) =
        formulario.update { it.copy(estados = if (estado in it.estados) it.estados - estado else it.estados + estado) }

    /** CU-20 A1: restablece todos los criterios (tipo = Todas). */
    fun limpiar() = formulario.update { FormularioBusqueda() }

    private fun formatear(valor: Float): String =
        Puntuacion
            .of(
                (valor * FormularioBusqueda.DECIMAS).roundToInt().coerceIn(Puntuacion.MIN_DECIMAS, Puntuacion.MAX_DECIMAS),
            ).formatear()

    private companion object {
        const val SUSCRIPCION_MS = 5_000L
        const val TAG = "Buscar"
    }
}
