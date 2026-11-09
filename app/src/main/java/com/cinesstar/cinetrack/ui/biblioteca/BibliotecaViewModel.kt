package com.cinesstar.cinetrack.ui.biblioteca

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinesstar.cinetrack.data.repository.BibliotecaRepository
import com.cinesstar.cinetrack.domain.Agrupador
import com.cinesstar.cinetrack.domain.Ambito
import com.cinesstar.cinetrack.domain.ClaveGrupo
import com.cinesstar.cinetrack.domain.CriterioAgrupacion
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Grupo
import com.cinesstar.cinetrack.domain.PerfilBiblioteca
import com.cinesstar.cinetrack.domain.RangoPuntuacion
import com.cinesstar.cinetrack.domain.TipoPerfil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Filtros de la Biblioteca. Son independientes: cambiar uno conserva los demás (CU-09 A2). [estado] lo eligen los
 * contadores (design.md §4-3); null en [estado], [rango] o [criterio] = sin filtro / sin agrupar.
 */
data class FiltrosBiblioteca(
    val ambito: Ambito = Ambito.TODO,
    val estado: Estado? = null,
    val rango: RangoPuntuacion? = null,
    val criterio: CriterioAgrupacion? = null,
    /** Grupos plegados (CU-09 A1). Se vacía al cambiar de criterio. */
    val plegados: Set<ClaveGrupo> = emptySet(),
)

/** Lo que pinta la Biblioteca con datos. */
data class DatosBiblioteca(
    val filtros: FiltrosBiblioteca,
    /** Perfiles de toda la Biblioteca ("{N} entradas", "Todas ({n})"). */
    val total: Int,
    val porTipo: Map<TipoPerfil, Int>,
    /** Contadores Viendo / Pendientes / Vistas del ámbito elegido. */
    val porEstado: Map<Estado, Int>,
    /** Perfiles que pasan los filtros, más recientes primero (S-10). */
    val perfiles: List<PerfilBiblioteca>,
    /** Grupos de [perfiles] si hay criterio; null si no se agrupa. */
    val grupos: List<Grupo>?,
)

sealed interface EstadoBiblioteca {
    data object Cargando : EstadoBiblioteca

    /** CU-08 A3 / CU-09 E1: no hay ningún perfil (M-22). */
    data object Vacia : EstadoBiblioteca

    /** CU-08 E1: no se pudo leer (M-25 con "Reintentar"). */
    data object Error : EstadoBiblioteca

    data class Lista(
        val datos: DatosBiblioteca,
    ) : EstadoBiblioteca
}

/** CU-08 Ver biblioteca y CU-09 Agrupar perfiles. */
@OptIn(ExperimentalCoroutinesApi::class)
class BibliotecaViewModel(
    private val biblioteca: BibliotecaRepository,
) : ViewModel() {
    private val filtros = MutableStateFlow(FiltrosBiblioteca())
    private val intento = MutableStateFlow(0)

    /** Datos leídos: lista o null tras un error de lectura. */
    private val lectura =
        intento.flatMapLatest {
            biblioteca
                .observar()
                .map<List<PerfilBiblioteca>, List<PerfilBiblioteca>?> { it }
                .catch { e ->
                    if (e is CancellationException) throw e
                    Log.e(TAG, "No se pudo leer la Biblioteca", e)
                    emit(null)
                }
        }

    val estado: StateFlow<EstadoBiblioteca> =
        combine(lectura, filtros) { perfiles, f ->
            when {
                perfiles == null -> EstadoBiblioteca.Error
                perfiles.isEmpty() -> EstadoBiblioteca.Vacia
                else -> EstadoBiblioteca.Lista(calcular(perfiles, f))
            }
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUSCRIPCION_MS), EstadoBiblioteca.Cargando)

    /** CU-08 A1 / CU-09 paso 1. */
    fun elegirAmbito(ambito: Ambito) = filtros.update { it.copy(ambito = ambito) }

    /** Contadores: pulsar el estado activo quita el filtro. */
    fun alternarEstado(estado: Estado) = filtros.update { it.copy(estado = if (it.estado == estado) null else estado) }

    /** CU-08 A2: pulsar el rango seleccionado lo deselecciona (design.md §4-4). */
    fun alternarRango(rango: RangoPuntuacion) = filtros.update { it.copy(rango = if (it.rango == rango) null else rango) }

    /** CU-09 paso 2; null = "Ninguno". */
    fun agrupar(criterio: CriterioAgrupacion?) =
        filtros.update { if (it.criterio == criterio) it else it.copy(criterio = criterio, plegados = emptySet()) }

    /** CU-09 A1. */
    fun alternarGrupo(clave: ClaveGrupo) =
        filtros.update { it.copy(plegados = if (clave in it.plegados) it.plegados - clave else it.plegados + clave) }

    /** CU-08 E1: "Reintentar". */
    fun reintentar() = intento.update { it + 1 }

    private fun calcular(
        perfiles: List<PerfilBiblioteca>,
        f: FiltrosBiblioteca,
    ): DatosBiblioteca {
        val enAmbito = perfiles.filter { f.ambito.admite(it.tipo) }
        val filtrados = enAmbito.filter { (f.estado == null || it.estado == f.estado) && (f.rango == null || it.rango == f.rango) }
        return DatosBiblioteca(
            filtros = f,
            total = perfiles.size,
            porTipo = TipoPerfil.entries.associateWith { tipo -> perfiles.count { it.tipo == tipo } },
            porEstado = Estado.entries.associateWith { estado -> enAmbito.count { it.estado == estado } },
            perfiles = filtrados,
            grupos = f.criterio?.let { Agrupador.agrupar(filtrados, it) },
        )
    }

    private companion object {
        const val SUSCRIPCION_MS = 5_000L
        const val TAG = "Biblioteca"
    }
}
