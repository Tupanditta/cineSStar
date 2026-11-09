package com.cinesstar.cinetrack.ui.lista

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinesstar.cinetrack.data.repository.ListaRepository
import com.cinesstar.cinetrack.domain.Candidato
import com.cinesstar.cinetrack.domain.ContenidoLista
import com.cinesstar.cinetrack.domain.DominioException
import com.cinesstar.cinetrack.domain.ElementoLista
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.OrdenLista
import com.cinesstar.cinetrack.domain.TipoLista
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la lista abierta: cargando, sus datos, que ya no existe (CU-13 E1) o que la acaba de borrar el usuario (CU-19). */
sealed interface EstadoLista {
    data object Cargando : EstadoLista

    data class Lista(
        val contenido: ContenidoLista,
    ) : EstadoLista

    data object NoExiste : EstadoLista

    /** CU-19 paso 4: se vuelve a "Mis Listas" con el aviso "Lista eliminada". */
    data object Eliminada : EstadoLista
}

/** Diálogos y hojas de la lista abierta. Cada uno es null / false mientras está cerrado. */
data class EdicionLista(
    /** Texto de búsqueda del selector "Añadir título"; null = cerrado (CU-15). */
    val busqueda: String? = null,
    /** Elemento cuya retirada se está confirmando (CU-16 paso 1). */
    val quitar: ElementoLista? = null,
    /** Aviso único M-11 tras quitar el último elemento (CU-16 A1). */
    val avisoVacia: Boolean = false,
    /** Nombre del diálogo "Renombrar" (CU-14); null = cerrado. */
    val renombrar: String? = null,
    val errorRenombrar: ErrorDominio? = null,
    /** Confirmación M-12 (CU-19). */
    val eliminar: Boolean = false,
)

/** Avisos de un solo uso (toasts). */
sealed interface EventoLista {
    /** CU-15 paso 3; [puesto] solo en rankings. */
    data class Anadido(
        val nombre: String,
        val puesto: Int?,
    ) : EventoLista

    data object Quitado : EventoLista

    data object Renombrada : EventoLista

    /** CU-16 E1: el elemento ya no existe; la lista se refresca sola. */
    data object ElementoNoExiste : EventoLista

    /** Error de negocio (M-09, M-10, M-24) o, con null, fallo de escritura (M-25, sin cambios). */
    data class Error(
        val error: ErrorDominio?,
    ) : EventoLista
}

/**
 * CU-13 Ver contenido de una lista y, desde ella, CU-15 Añadir (selector "Añadir título"), CU-16 Quitar (con el aviso
 * único M-11), CU-14 Renombrar y CU-19 Eliminar. La lista se vuelve a leer sola cuando cambian sus datos.
 */
@Suppress("TooManyFunctions") // Una función por evento de la pantalla (UDF).
@OptIn(ExperimentalCoroutinesApi::class)
class ListaViewModel(
    private val listaId: Long,
    private val listas: ListaRepository,
) : ViewModel() {
    private val eliminadaPorUsuario = MutableStateFlow(false)

    val estado: StateFlow<EstadoLista> =
        combine(listas.observarContenido(listaId), eliminadaPorUsuario) { contenido, eliminada ->
            when {
                contenido != null -> EstadoLista.Lista(contenido)
                eliminada -> EstadoLista.Eliminada
                else -> EstadoLista.NoExiste
            }
        }.catch { e ->
            if (e is CancellationException) throw e
            Log.e(TAG, "No se pudo leer la lista $listaId", e)
            emit(EstadoLista.NoExiste)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUSCRIPCION_MS), EstadoLista.Cargando)

    private val edicionInterna = MutableStateFlow(EdicionLista())
    val edicion: StateFlow<EdicionLista> get() = edicionInterna

    /** Resultados del selector (CU-15 A5): solo se leen con el selector abierto. */
    val candidatos: StateFlow<List<Candidato>> =
        edicionInterna
            .map { it.busqueda }
            .distinctUntilChanged()
            .flatMapLatest { busqueda ->
                if (busqueda == null) flowOf(emptyList()) else listas.observarCandidatos(listaId).map { OrdenLista.filtrar(it, busqueda) }
            }.catch { e ->
                if (e is CancellationException) throw e
                Log.e(TAG, "No se pudo leer la biblioteca para el selector", e)
                emit(emptyList())
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUSCRIPCION_MS), emptyList())

    private val eventosInternos = Channel<EventoLista>(Channel.BUFFERED)
    val eventos: Flow<EventoLista> = eventosInternos.receiveAsFlow()

    private var guardando = false

    private val contenido: ContenidoLista? get() = (estado.value as? EstadoLista.Lista)?.contenido

    // ---- CU-15 ----

    fun abrirSelector() = edicionInterna.update { it.copy(busqueda = "") }

    fun cerrarSelector() = edicionInterna.update { it.copy(busqueda = null) }

    fun buscar(texto: String) = edicionInterna.update { if (it.busqueda == null) it else it.copy(busqueda = texto) }

    /**
     * CU-15 pasos 2–3. El selector no se cierra (A3). Un perfil no disponible también se intenta: el repositorio
     * responde con M-10 o M-09 y no añade nada (E1, E2).
     */
    fun anadir(candidato: Candidato) {
        val lista = contenido?.lista ?: return
        val puesto = if (lista.tipo == TipoLista.RANKING) lista.total + 1 else null
        escribir {
            listas.anadir(listaId, candidato.perfilId)
            eventosInternos.send(EventoLista.Anadido(candidato.nombre, puesto))
        }
    }

    // ---- CU-16 ----

    fun pedirQuitar(elemento: ElementoLista) = edicionInterna.update { it.copy(quitar = elemento) }

    /** CU-16 A2: cancelar no cambia nada. */
    fun cancelarQuitar() = edicionInterna.update { it.copy(quitar = null) }

    /** CU-16 pasos 2–4. Si la lista queda vacía por primera vez en este vaciado, aviso único M-11 (A1, S-17). */
    fun confirmarQuitar() {
        val elemento = edicionInterna.value.quitar ?: return
        edicionInterna.update { it.copy(quitar = null) }
        escribir {
            val avisar = listas.quitar(listaId, elemento.perfilId)
            if (avisar) edicionInterna.update { it.copy(avisoVacia = true) }
            eventosInternos.send(EventoLista.Quitado)
        }
    }

    /** CU-16 A1a: se borra la lista sin una segunda confirmación (CU-19). */
    fun aceptarAvisoVacia() {
        edicionInterna.update { it.copy(avisoVacia = false) }
        eliminarLista()
    }

    /** CU-16 A1b: la lista queda vacía (M-23) y se puede borrar después. */
    fun rechazarAvisoVacia() = edicionInterna.update { it.copy(avisoVacia = false) }

    // ---- CU-14 ----

    fun abrirRenombrar() {
        val nombre = contenido?.lista?.nombre ?: return
        edicionInterna.update { it.copy(renombrar = nombre, errorRenombrar = null) }
    }

    fun cambiarNombre(nombre: String) =
        edicionInterna.update { if (it.renombrar == null) it else it.copy(renombrar = nombre, errorRenombrar = null) }

    fun cerrarRenombrar() = edicionInterna.update { it.copy(renombrar = null, errorRenombrar = null) }

    /** CU-14 pasos 3–4; M-07 / M-08 en el diálogo; el mismo nombre cierra sin cambios (A2). */
    fun guardarRenombrar() {
        val nombre = edicionInterna.value.renombrar ?: return
        escribir(alErrorDominio = { e -> edicionInterna.update { it.copy(errorRenombrar = e) } }) {
            val cambiada = listas.renombrar(listaId, nombre)
            edicionInterna.update { it.copy(renombrar = null) }
            if (cambiada) eventosInternos.send(EventoLista.Renombrada)
        }
    }

    // ---- CU-19 ----

    fun pedirEliminar() = edicionInterna.update { it.copy(eliminar = true) }

    fun cancelarEliminar() = edicionInterna.update { it.copy(eliminar = false) }

    /** CU-19 pasos 2–4 (ya confirmado). Si falla, la lista permanece (E1). */
    fun confirmarEliminar() {
        edicionInterna.update { it.copy(eliminar = false) }
        eliminarLista()
    }

    private fun eliminarLista() {
        eliminadaPorUsuario.value = true
        escribir(alFallar = { eliminadaPorUsuario.value = false }) { listas.eliminar(listaId) }
    }

    @Suppress("TooGenericExceptionCaught") // Cualquier fallo de escritura se muestra como M-25.
    private fun escribir(
        alFallar: () -> Unit = {},
        alErrorDominio: ((ErrorDominio) -> Unit)? = null,
        operacion: suspend () -> Unit,
    ) {
        if (guardando) return
        guardando = true
        viewModelScope.launch {
            try {
                operacion()
            } catch (e: CancellationException) {
                throw e
            } catch (e: DominioException) {
                alFallar()
                when {
                    e.error == ErrorDominio.NoEncontrado -> eventosInternos.send(EventoLista.ElementoNoExiste)
                    alErrorDominio != null -> alErrorDominio(e.error)
                    else -> eventosInternos.send(EventoLista.Error(e.error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo guardar el cambio de la lista $listaId", e)
                alFallar()
                eventosInternos.send(EventoLista.Error(null))
            } finally {
                guardando = false
            }
        }
    }

    private companion object {
        const val SUSCRIPCION_MS = 5_000L
        const val TAG = "Lista"
    }
}
