package com.cinesstar.cinetrack.ui.perfil

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinesstar.cinetrack.data.repository.FichaPerfil
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.PasoPuntuacion
import com.cinesstar.cinetrack.domain.Puntuacion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del detalle: cargando, el perfil, que ya no existe (CU-07 E1) o que lo acaba de borrar el usuario (CU-06). */
sealed interface EstadoDetalle {
    data object Cargando : EstadoDetalle

    data class Listo(
        val ficha: FichaPerfil,
    ) : EstadoDetalle

    data object NoExiste : EstadoDetalle

    /** CU-06 paso 5: se vuelve atrás con el aviso "Perfil eliminado". */
    data object Eliminado : EstadoDetalle
}

/** Editores del detalle. Cada uno es null mientras está cerrado. */
data class EdicionDetalle(
    /** Texto del editor de nota (CU-03, design.md §7.3). */
    val nota: String? = null,
    /** Texto de la hoja de la crítica (CU-04, design.md §7.6). */
    val critica: String? = null,
    /** Hay una escritura en curso: se ignoran nuevas pulsaciones. */
    val guardando: Boolean = false,
) {
    val lecturaNota: Puntuacion.Lectura get() = Puntuacion.leer(nota.orEmpty())
}

/** Avisos de un solo uso para la interfaz. */
sealed interface EventoDetalle {
    /** M-25: falló una escritura; los datos no han cambiado (CU-03 E2, CU-04 E1, CU-05 E1, CU-06 E1). */
    data object ErrorEscritura : EventoDetalle
}

/**
 * CU-07 Ver perfil y, desde el detalle (A4), CU-03 Puntuar, CU-04 Comentario, CU-05 Estado y CU-06 Eliminar. La ficha
 * se vuelve a leer sola cuando cambian los datos del perfil, su saga o sus listas: la interfaz nunca muestra un valor
 * que no esté guardado (si una escritura falla, el selector sigue en el estado anterior).
 */
@Suppress("TooManyFunctions") // Una función por evento de la pantalla (UDF).
class DetallePerfilViewModel(
    private val perfilId: Long,
    private val perfiles: PerfilRepository,
) : ViewModel() {
    private val eliminadoPorUsuario = MutableStateFlow(false)

    val estado: StateFlow<EstadoDetalle> =
        combine(perfiles.observarFicha(perfilId), eliminadoPorUsuario) { ficha, eliminado ->
            when {
                ficha != null -> EstadoDetalle.Listo(ficha)
                eliminado -> EstadoDetalle.Eliminado
                else -> EstadoDetalle.NoExiste
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUSCRIPCION_MS), EstadoDetalle.Cargando)

    private val edicionInterna = MutableStateFlow(EdicionDetalle())
    val edicion: StateFlow<EdicionDetalle> get() = edicionInterna

    private val eventosInternos = Channel<EventoDetalle>(Channel.BUFFERED)
    val eventos: Flow<EventoDetalle> = eventosInternos.receiveAsFlow()

    private val ficha: FichaPerfil? get() = (estado.value as? EstadoDetalle.Listo)?.ficha

    /** CU-05: tocar el estado ya activo no cambia nada (A1). */
    fun cambiarEstado(nuevo: Estado) {
        val actual = ficha?.detalle?.perfil?.estado ?: return
        if (actual != nuevo) escribir { perfiles.cambiarEstado(perfilId, nuevo) }
    }

    /** CU-03 paso 1: "Ajustar" abre (o cierra) el editor con el valor actual o vacío. */
    fun alternarEditorNota() {
        val actual =
            ficha
                ?.detalle
                ?.puntuacion
                ?.formatear()
                .orEmpty()
        edicionInterna.update { it.copy(nota = if (it.nota == null) actual else null) }
    }

    /*
     * Los cambios de texto solo cuentan con el editor abierto: al cerrarse, el campo puede volver a enviar su último
     * texto y no debe reabrirlo.
     */
    fun cambiarTextoNota(texto: String) = edicionInterna.update { if (it.nota == null) it else it.copy(nota = texto) }

    fun moverSliderNota(valor: Float) =
        edicionInterna.update { if (it.nota == null) it else it.copy(nota = PasoPuntuacion.desdeSlider(valor)) }

    /** CU-03 paso 4. Un valor inválido no se guarda y la nota anterior no cambia (E1); vacío la quita (A2). */
    fun guardarNota() {
        val lectura = edicionInterna.value.lecturaNota
        if (edicionInterna.value.nota == null || lectura == Puntuacion.Lectura.Invalida) return
        val nueva = (lectura as? Puntuacion.Lectura.Valida)?.puntuacion
        escribir(alTerminar = { edicionInterna.update { it.copy(nota = null) } }) { perfiles.puntuar(perfilId, nueva) }
    }

    /** CU-03 A2: "Quitar puntuación". */
    fun quitarNota() = escribir(alTerminar = { edicionInterna.update { it.copy(nota = null) } }) { perfiles.puntuar(perfilId, null) }

    /** CU-04 paso 1: abre la hoja con el comentario actual. */
    fun abrirEditorCritica() =
        edicionInterna.update {
            it.copy(
                critica =
                    ficha
                        ?.detalle
                        ?.perfil
                        ?.comentario
                        .orEmpty(),
            )
        }

    /** CU-04 A2: se descarta lo escrito. */
    fun cerrarEditorCritica() = edicionInterna.update { it.copy(critica = null) }

    /** CU-04 E2: no se puede escribir más allá del límite (S-06). */
    fun cambiarTextoCritica(texto: String) =
        edicionInterna.update { if (it.critica == null) it else it.copy(critica = texto.take(PerfilRepository.MAX_COMENTARIO)) }

    /** CU-04 paso 3; guardar el texto vacío borra el comentario (A1). */
    fun guardarCritica() {
        val texto = edicionInterna.value.critica ?: return
        escribir(alTerminar = { edicionInterna.update { it.copy(critica = null) } }) { perfiles.comentar(perfilId, texto) }
    }

    /** CU-04 A1: borrar con el botón (tras la confirmación breve). */
    fun borrarCritica() = escribir { perfiles.comentar(perfilId, null) }

    /** CU-06 pasos 2–5, ya confirmado (M-06). Si falla no se borra nada (E1). */
    fun eliminar() {
        eliminadoPorUsuario.value = true
        escribir(alFallar = { eliminadoPorUsuario.value = false }) { perfiles.eliminar(perfilId) }
    }

    @Suppress("TooGenericExceptionCaught") // Cualquier fallo de escritura se muestra como M-25.
    private fun escribir(
        alFallar: () -> Unit = {},
        alTerminar: () -> Unit = {},
        operacion: suspend () -> Unit,
    ) {
        if (edicionInterna.value.guardando) return
        edicionInterna.update { it.copy(guardando = true) }
        viewModelScope.launch {
            try {
                operacion()
                alTerminar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo guardar el cambio del perfil $perfilId", e)
                alFallar()
                eventosInternos.send(EventoDetalle.ErrorEscritura)
            } finally {
                edicionInterna.update { it.copy(guardando = false) }
            }
        }
    }

    private companion object {
        const val SUSCRIPCION_MS = 5_000L
        const val TAG = "DetallePerfil"
    }
}
