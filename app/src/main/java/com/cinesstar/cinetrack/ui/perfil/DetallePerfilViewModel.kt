package com.cinesstar.cinetrack.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinesstar.cinetrack.data.repository.FichaPerfil
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Estado del detalle: cargando, el perfil, o que ya no existe (CU-07 E1). */
sealed interface EstadoDetalle {
    data object Cargando : EstadoDetalle

    data class Listo(
        val ficha: FichaPerfil,
    ) : EstadoDetalle

    data object NoExiste : EstadoDetalle
}

/** CU-07 Ver perfil. Se actualiza solo cuando cambian los datos del perfil, su saga o sus listas. */
class DetallePerfilViewModel(
    perfilId: Long,
    perfiles: PerfilRepository,
) : ViewModel() {
    val estado: StateFlow<EstadoDetalle> =
        perfiles
            .observarFicha(perfilId)
            .map { ficha -> ficha?.let { EstadoDetalle.Listo(it) } ?: EstadoDetalle.NoExiste }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUSCRIPCION_MS), EstadoDetalle.Cargando)

    private companion object {
        const val SUSCRIPCION_MS = 5_000L
    }
}
