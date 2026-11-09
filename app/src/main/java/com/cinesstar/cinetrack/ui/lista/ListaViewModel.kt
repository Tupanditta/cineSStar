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
import com.cinesstar.cinetrack.domain.PosicionAlta
import com.cinesstar.cinetrack.domain.Prioridad
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
    /** Campo "Posición" del selector en un ranking (CU-15 A1); vacío = al final (RF-39). */
    val posicion: String = "",
    /** Prioridad con la que se añade en una lista de prioridad (CU-15 A2); Media por defecto (S-16). */
    val prioridad: Prioridad = Prioridad.POR_DEFECTO,
    /** Modo "Reordenar puestos" del ranking: muestra el banner con "Listo" (design.md §6.1). */
    val reordenando: Boolean = false,
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

    /** CU-18: un elemento del ranking cambió de puesto. */
    data object PosicionActualizada : EventoLista

    /** "Listo" del modo "Reordenar puestos" (design.md §6.1). */
    data object RankingGuardado : EventoLista

    /** CU-17: un elemento cambió de prioridad. */
    data object PrioridadActualizada : EventoLista

    /** CU-16 E1: el elemento ya no existe; la lista se refresca sola. */
    data object ElementoNoExiste : EventoLista

    /** Error de negocio (M-09, M-10, M-24) o, con null, fallo de escritura (M-25, sin cambios). */
    data class Error(
        val error: ErrorDominio?,
    ) : EventoLista
}

/**
 * CU-13 Ver contenido de una lista y, desde ella, CU-15 Añadir (selector "Añadir título", con posición o prioridad),
 * CU-16 Quitar (con el aviso único M-11), CU-17 Cambiar prioridad, CU-18 Reordenar el ranking, CU-14 Renombrar y
 * CU-19 Eliminar. La lista se vuelve a leer sola cuando cambian sus datos.
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

    private val restaurarInterno = MutableStateFlow(0)

    /**
     * Cambia cada vez que un reordenado no se guarda (CU-18 A2 y E1): la interfaz descarta el orden provisional del
     * arrastre y vuelve a pintar el guardado.
     */
    val restaurarOrden: StateFlow<Int> get() = restaurarInterno

    private val contenido: ContenidoLista? get() = (estado.value as? EstadoLista.Lista)?.contenido

    // ---- CU-15 ----

    fun abrirSelector() = edicionInterna.update { it.copy(busqueda = "", posicion = "", prioridad = Prioridad.POR_DEFECTO) }

    fun cerrarSelector() = edicionInterna.update { it.copy(busqueda = null) }

    fun buscar(texto: String) = edicionInterna.update { if (it.busqueda == null) it else it.copy(busqueda = texto) }

    /** CU-15 A1: texto del campo "Posición" (se valida al añadir: M-24). */
    fun cambiarPosicion(texto: String) = edicionInterna.update { it.copy(posicion = texto.take(MAX_CIFRAS)) }

    /** Botones − / + del campo "Posición" (1..n+1). */
    fun pasoPosicion(delta: Int) {
        val total = contenido?.lista?.total ?: return
        edicionInterna.update { it.copy(posicion = PosicionAlta.paso(it.posicion, delta, total)) }
    }

    /** CU-15 A2: Alta, Media o Baja para los siguientes que se añadan. */
    fun elegirPrioridad(prioridad: Prioridad) = edicionInterna.update { it.copy(prioridad = prioridad) }

    /**
     * CU-15 pasos 2–3. El selector no se cierra (A3). En un ranking se añade en la posición del campo (vacío = al final;
     * inválida: M-24 y no se añade, E3) y el campo vuelve a "al final"; en una lista de prioridad, con la elegida. Un
     * perfil no disponible también se intenta: el repositorio responde con M-10 o M-09 y no añade nada (E1, E2).
     */
    fun anadir(candidato: Candidato) {
        val lista = contenido?.lista ?: return
        val edicion = edicionInterna.value
        when (lista.tipo) {
            TipoLista.RANKING -> {
                val destino = PosicionAlta.destino(edicion.posicion, lista.total)
                if (destino == null && candidato.disponible) {
                    viewModelScope.launch { eventosInternos.send(EventoLista.Error(ErrorDominio.PosicionInvalida)) }
                    return
                }
                escribir {
                    listas.anadir(listaId, candidato.perfilId, posicion = destino)
                    edicionInterna.update { it.copy(posicion = "") }
                    eventosInternos.send(EventoLista.Anadido(candidato.nombre, destino))
                }
            }
            TipoLista.PRIORIDAD ->
                escribir {
                    listas.anadir(listaId, candidato.perfilId, prioridad = edicion.prioridad)
                    eventosInternos.send(EventoLista.Anadido(candidato.nombre, null))
                }
            TipoLista.NORMAL ->
                escribir {
                    listas.anadir(listaId, candidato.perfilId)
                    eventosInternos.send(EventoLista.Anadido(candidato.nombre, null))
                }
        }
    }

    // ---- CU-17 ----

    /** CU-17 pasos 2–3: la lista se reordena sola al leerse de nuevo. El mismo nivel no cambia nada (A1). */
    fun cambiarPrioridad(
        elemento: ElementoLista,
        prioridad: Prioridad,
    ) {
        if (elemento.prioridad == prioridad) return
        escribir {
            if (listas.cambiarPrioridad(listaId, elemento.perfilId, prioridad)) {
                eventosInternos.send(EventoLista.PrioridadActualizada)
            }
        }
    }

    // ---- CU-18 ----

    fun activarReordenar() = edicionInterna.update { it.copy(reordenando = true) }

    /** "Listo": cada movimiento ya se ha guardado; se cierra el modo con el aviso "Ranking guardado". */
    fun terminarReordenar() {
        edicionInterna.update { it.copy(reordenando = false) }
        viewModelScope.launch { eventosInternos.send(EventoLista.RankingGuardado) }
    }

    /** ▲: sube un puesto. En el #1 no tiene efecto (A1). */
    fun subir(elemento: ElementoLista) {
        val puesto = elemento.posicion ?: return
        if (puesto > 1) mover(elemento.perfilId, puesto - 1)
    }

    /** ▼: baja un puesto. En el último no tiene efecto (A1). */
    fun bajar(elemento: ElementoLista) {
        val puesto = elemento.posicion ?: return
        val total = contenido?.lista?.total ?: return
        if (puesto < total) mover(elemento.perfilId, puesto + 1)
    }

    /**
     * CU-18 pasos 1–3 (flechas o fin del arrastre). Soltar en el mismo puesto no escribe (A2). Si falla, M-25 y se
     * vuelve al orden guardado (E1).
     */
    fun mover(
        perfilId: Long,
        nuevaPosicion: Int,
    ) = escribir(alFallar = { restaurarInterno.update { it + 1 } }) {
        if (listas.mover(listaId, perfilId, nuevaPosicion)) {
            eventosInternos.send(EventoLista.PosicionActualizada)
        } else {
            restaurarInterno.update { it + 1 }
        }
    }

    /** CU-18 A2: arrastre soltado donde estaba; solo se descarta el orden provisional. */
    fun cancelarArrastre() = restaurarInterno.update { it + 1 }

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
        // Una escritura a la vez: la pulsación que llega mientras tanto se descarta como si hubiera fallado.
        if (guardando) {
            alFallar()
            return
        }
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

        /** El campo "Posición" no necesita más cifras (RF-41 no fija tamaño máximo; con 6 sobra). */
        const val MAX_CIFRAS = 6
    }
}
