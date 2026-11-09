package com.cinesstar.cinetrack.ui.listas

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinesstar.cinetrack.data.repository.ListaRepository
import com.cinesstar.cinetrack.domain.DominioException
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.PanelListas
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface EstadoListas {
    data object Cargando : EstadoListas

    /** CU-12 E1: no se pudo leer (M-25 con "Reintentar"). */
    data object Error : EstadoListas

    data class Datos(
        val panel: PanelListas,
    ) : EstadoListas
}

/** Diálogos de "Mis Listas". Los textos que escribe el usuario viven aquí para no perderse al girar (S-21). */
sealed interface DialogoListas {
    /** CU-11: "Nueva Lista" (tipo Normal y restricción Mixto por defecto). */
    data class Nueva(
        val nombre: String = "",
        val tipo: TipoLista = TipoLista.NORMAL,
        val restriccion: Restriccion = Restriccion.MIXTO,
        val error: ErrorDominio? = null,
    ) : DialogoListas

    /** CU-14. */
    data class Renombrar(
        val listaId: Long,
        val nombre: String,
        val error: ErrorDominio? = null,
    ) : DialogoListas

    /** CU-19: confirmación M-12. */
    data class Eliminar(
        val listaId: Long,
        val nombre: String,
    ) : DialogoListas
}

/** Avisos de un solo uso (toasts). */
sealed interface EventoListas {
    data object Creada : EventoListas

    data object Renombrada : EventoListas

    data object Eliminada : EventoListas

    /** La lista se borró mientras tanto. */
    data object NoExiste : EventoListas

    /** M-25: falló una escritura; no ha cambiado nada (CU-11 E3, CU-14 E3, CU-19 E1). */
    data object ErrorEscritura : EventoListas
}

/** CU-12 Ver todas las listas y, desde ella, CU-11 Crear, CU-14 Renombrar y CU-19 Eliminar. */
@Suppress("TooManyFunctions") // Una función por evento de la pantalla (UDF).
@OptIn(ExperimentalCoroutinesApi::class)
class ListasViewModel(
    private val listas: ListaRepository,
) : ViewModel() {
    private val intento = MutableStateFlow(0)

    val estado: StateFlow<EstadoListas> =
        intento
            .flatMapLatest {
                listas
                    .observarPanel()
                    .map<PanelListas, EstadoListas> { EstadoListas.Datos(it) }
                    .catch { e ->
                        if (e is CancellationException) throw e
                        Log.e(TAG, "No se pudieron leer las listas", e)
                        emit(EstadoListas.Error)
                    }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUSCRIPCION_MS), EstadoListas.Cargando)

    private val dialogoInterno = MutableStateFlow<DialogoListas?>(null)
    val dialogo: StateFlow<DialogoListas?> get() = dialogoInterno

    private val eventosInternos = Channel<EventoListas>(Channel.BUFFERED)
    val eventos: Flow<EventoListas> = eventosInternos.receiveAsFlow()

    private var guardando = false

    fun reintentar() = intento.update { it + 1 }

    /** CU-11 paso 1. */
    fun abrirNueva() {
        dialogoInterno.value = DialogoListas.Nueva()
    }

    fun abrirRenombrar(
        listaId: Long,
        nombre: String,
    ) {
        dialogoInterno.value = DialogoListas.Renombrar(listaId, nombre)
    }

    fun abrirEliminar(
        listaId: Long,
        nombre: String,
    ) {
        dialogoInterno.value = DialogoListas.Eliminar(listaId, nombre)
    }

    /** CU-11 A1, CU-14 A1, CU-19 A1: cancelar no cambia nada. */
    fun cerrarDialogo() {
        dialogoInterno.value = null
    }

    /** Escribir quita el error anterior: se vuelve a validar al guardar. */
    fun cambiarNombre(nombre: String) =
        dialogoInterno.update {
            when (it) {
                is DialogoListas.Nueva -> it.copy(nombre = nombre, error = null)
                is DialogoListas.Renombrar -> it.copy(nombre = nombre, error = null)
                else -> it
            }
        }

    fun elegirTipo(tipo: TipoLista) = dialogoInterno.update { (it as? DialogoListas.Nueva)?.copy(tipo = tipo) ?: it }

    fun elegirRestriccion(restriccion: Restriccion) =
        dialogoInterno.update { (it as? DialogoListas.Nueva)?.copy(restriccion = restriccion) ?: it }

    /** CU-11 pasos 5–6. Errores M-07 / M-08 en el diálogo; un fallo de escritura, M-25 con el diálogo abierto. */
    fun guardarNueva() {
        val d = dialogoInterno.value as? DialogoListas.Nueva ?: return
        escribir(alErrorDominio = { e -> dialogoInterno.update { (it as? DialogoListas.Nueva)?.copy(error = e) ?: it } }) {
            listas.crear(d.nombre, d.tipo, d.restriccion)
            dialogoInterno.value = null
            eventosInternos.send(EventoListas.Creada)
        }
    }

    /** CU-14 pasos 3–4; el mismo nombre cierra sin cambios (A2). */
    fun guardarRenombrar() {
        val d = dialogoInterno.value as? DialogoListas.Renombrar ?: return
        escribir(alErrorDominio = { e -> dialogoInterno.update { (it as? DialogoListas.Renombrar)?.copy(error = e) ?: it } }) {
            val cambiada = listas.renombrar(d.listaId, d.nombre)
            dialogoInterno.value = null
            if (cambiada) eventosInternos.send(EventoListas.Renombrada)
        }
    }

    /** CU-19 pasos 2–4 (ya confirmado con M-12). */
    fun confirmarEliminar() {
        val d = dialogoInterno.value as? DialogoListas.Eliminar ?: return
        dialogoInterno.value = null
        escribir {
            listas.eliminar(d.listaId)
            eventosInternos.send(EventoListas.Eliminada)
        }
    }

    @Suppress("TooGenericExceptionCaught") // Cualquier fallo de escritura se muestra como M-25.
    private fun escribir(
        alErrorDominio: (ErrorDominio) -> Unit = {},
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
                if (e.error == ErrorDominio.NoEncontrado) {
                    dialogoInterno.value = null
                    eventosInternos.send(EventoListas.NoExiste)
                } else {
                    alErrorDominio(e.error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo guardar el cambio de las listas", e)
                eventosInternos.send(EventoListas.ErrorEscritura)
            } finally {
                guardando = false
            }
        }
    }

    private companion object {
        const val SUSCRIPCION_MS = 5_000L
        const val TAG = "Listas"
    }
}
