package com.cinesstar.cinetrack.ui.datos

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinesstar.cinetrack.data.backup.ArchivosCopia
import com.cinesstar.cinetrack.data.backup.CopiaRepository
import com.cinesstar.cinetrack.data.backup.Metricas
import com.cinesstar.cinetrack.domain.CopiaException
import com.cinesstar.cinetrack.domain.CopiaSeguridad
import com.cinesstar.cinetrack.domain.DetalleCopia
import com.cinesstar.cinetrack.domain.FormatoCopia
import com.cinesstar.cinetrack.domain.ProblemaCopia
import com.cinesstar.cinetrack.domain.ResumenFusion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Operación de copia de seguridad en curso. */
sealed interface Operacion {
    data object Ninguna : Operacion

    /** CU-21: generando y guardando el archivo. */
    data object Exportando : Operacion

    /** CU-22: leyendo y validando el archivo, o aplicando la fusión (E5: indicador de progreso). */
    data object Importando : Operacion

    /** CU-22 paso 3: M-19 con los recuentos de la fusión en seco, a la espera de que el usuario confirme. */
    data class Confirmar(
        val copia: CopiaSeguridad,
        val resumen: ResumenFusion,
    ) : Operacion
}

/** Resultado que se muestra en el aviso en línea (design.md §10-5). */
sealed interface Resultado {
    val exito: Boolean get() = false

    /** M-15 */
    data object Guardada : Resultado {
        override val exito = true
    }

    /** M-16 */
    data object NoGuardada : Resultado

    /** M-17, con el dato inconsistente si lo hay (CU-22 E3). */
    data class NoValida(
        val detalle: DetalleCopia?,
    ) : Resultado

    /** M-18 */
    data object VersionFutura : Resultado

    /** M-20 */
    data class Importada(
        val resumen: ResumenFusion,
    ) : Resultado {
        override val exito = true
    }

    /** M-21 */
    data object NoImportada : Resultado
}

data class EstadoDatos(
    val metricas: Metricas? = null,
    /** Tamaño aproximado del archivo que se generaría ahora ("~420 KB"). */
    val tamanoEstimado: Long? = null,
    val operacion: Operacion = Operacion.Ninguna,
    val resultado: Resultado? = null,
) {
    val ocupado: Boolean get() = operacion == Operacion.Exportando || operacion == Operacion.Importando
}

/**
 * Pestaña "Datos Locales" y pantalla Ajustes (design.md §10): métricas, CU-21 Exportar y CU-22 Importar. Las
 * operaciones de archivo y base de datos se hacen fuera del hilo principal: la interfaz no se bloquea (CU-22 E5).
 */
class DatosViewModel(
    private val copias: CopiaRepository,
    private val archivos: ArchivosCopia,
) : ViewModel() {
    private val operacion = MutableStateFlow<Operacion>(Operacion.Ninguna)
    private val resultado = MutableStateFlow<Resultado?>(null)

    /** Métricas y tamaño estimado; (null, null) mientras se leen o si no se pueden leer. */
    private val metricas: Flow<Pair<Metricas?, Long?>> =
        copias
            .observarMetricas()
            .map<Metricas, Pair<Metricas?, Long?>> { m -> m to tamanoEstimado() }
            .onStart { emit(null to null) }
            .catch { e ->
                if (e is CancellationException) throw e
                Log.e(TAG, "No se pudieron leer las métricas", e)
                emit(null to null)
            }.flowOn(Dispatchers.Default)

    val estado: StateFlow<EstadoDatos> =
        combine(metricas, operacion, resultado) { (m, tamano), o, r -> EstadoDatos(m, tamano, o, r) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUSCRIPCION_MS), EstadoDatos())

    private val ocupado: Boolean get() = operacion.value == Operacion.Exportando || operacion.value == Operacion.Importando

    /** Bytes del archivo que se generaría ahora. */
    private suspend fun tamanoEstimado(): Long? =
        try {
            FormatoCopia
                .escribir(copias.exportar())
                .toByteArray()
                .size
                .toLong()
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Log.e(TAG, "No se pudo estimar el tamaño de la copia", e)
            null
        }

    /** CU-21 pasos 2–4 con el destino elegido; null = el usuario canceló el selector (A1): no se hace nada. */
    fun exportar(destino: Uri?) {
        if (destino == null || ocupado) return
        resultado.value = null
        operacion.value = Operacion.Exportando
        viewModelScope.launch {
            resultado.value =
                try {
                    val contenido = withContext(Dispatchers.Default) { FormatoCopia.escribir(copias.exportar()).toByteArray() }
                    archivos.escribir(destino, contenido)
                    Resultado.Guardada
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    // E1: sin espacio o ubicación no escribible. No queda un archivo corrupto a medias.
                    Log.e(TAG, "No se pudo guardar la copia", e)
                    archivos.descartar(destino)
                    Resultado.NoGuardada
                }
            operacion.value = Operacion.Ninguna
        }
    }

    /** CU-22 pasos 1–3 con el archivo elegido; null = el usuario canceló el selector (A1). */
    fun abrir(origen: Uri?) {
        if (origen == null || ocupado) return
        resultado.value = null
        operacion.value = Operacion.Importando
        viewModelScope.launch {
            try {
                val copia = withContext(Dispatchers.Default) { FormatoCopia.leer(archivos.leer(origen).decodeToString()) }
                operacion.value = Operacion.Confirmar(copia, copias.planificar(copia).resumen)
            } catch (e: CopiaException) {
                resultado.value =
                    when (val p = e.problema) {
                        is ProblemaCopia.NoValida -> Resultado.NoValida(p.detalle)
                        ProblemaCopia.VersionFutura -> Resultado.VersionFutura
                    }
                operacion.value = Operacion.Ninguna
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Log.e(TAG, "No se pudo leer la copia", e)
                resultado.value = Resultado.NoImportada
                operacion.value = Operacion.Ninguna
            }
        }
    }

    /** CU-22 pasos 4–5: aplica la fusión en una transacción y muestra M-20 (o M-21 si falla, E4). */
    fun confirmar() {
        val pendiente = operacion.value as? Operacion.Confirmar ?: return
        operacion.value = Operacion.Importando
        viewModelScope.launch {
            resultado.value =
                try {
                    Resultado.Importada(copias.importar(pendiente.copia))
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    Log.e(TAG, "No se pudo importar la copia", e)
                    Resultado.NoImportada
                }
            operacion.value = Operacion.Ninguna
        }
    }

    /** CU-22 A1: cancelar en M-19; no cambia nada. */
    fun cancelar() {
        if (operacion.value is Operacion.Confirmar) operacion.value = Operacion.Ninguna
    }

    private companion object {
        const val SUSCRIPCION_MS = 5_000L
        const val TAG = "Datos"
    }
}
