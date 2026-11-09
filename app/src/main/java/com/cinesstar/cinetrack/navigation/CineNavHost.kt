package com.cinesstar.cinetrack.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cinesstar.cinetrack.ui.biblioteca.ContenidoBiblioteca
import com.cinesstar.cinetrack.ui.buscar.ContenidoBuscar
import com.cinesstar.cinetrack.ui.components.Avisos
import com.cinesstar.cinetrack.ui.components.CineToastHost
import com.cinesstar.cinetrack.ui.components.LocalAvisos
import com.cinesstar.cinetrack.ui.components.PantallaPrincipal
import com.cinesstar.cinetrack.ui.lista.PantallaLista
import com.cinesstar.cinetrack.ui.listas.ContenidoListas
import com.cinesstar.cinetrack.ui.perfil.PantallaDetallePerfil
import com.cinesstar.cinetrack.ui.perfilform.PantallaFormularioPerfil

/**
 * Navega a un destino de la barra inferior. Las pestañas no se apilan: siempre queda Biblioteca debajo,
 * así "atrás" desde una pestaña vuelve a Biblioteca y desde Biblioteca sale de la app (T0.07).
 * "Añadir" se apila como pantalla secundaria.
 */
fun NavHostController.irA(destino: DestinoBarra) {
    if (!destino.esPestana) {
        navigate(destino.ruta) { launchSingleTop = true }
        return
    }
    navigate(destino.ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun CineNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val irA: (DestinoBarra) -> Unit = { navController.irA(it) }
    val atras: () -> Unit = { navController.popBackStack() }
    val ajustes: () -> Unit = { navController.irA(DestinoBarra.AJUSTES) }
    val avisos = remember { Avisos() }
    val entrada by navController.currentBackStackEntryAsState()
    val conBarraInferior =
        entrada?.destination?.let { destino -> DestinoBarra.entries.any { it.esPestana && destino.hasRoute(it.ruta::class) } } ?: true

    CompositionLocalProvider(LocalAvisos provides avisos) {
        Box(modifier = modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = Ruta.Biblioteca) {
                composable<Ruta.Biblioteca> {
                    PantallaPrincipal(DestinoBarra.BIBLIOTECA, irA) {
                        ContenidoBiblioteca(
                            onPerfil = { navController.navigate(Ruta.Detalle(it)) },
                            onAnadir = { irA(DestinoBarra.ANADIR) },
                        )
                    }
                }
                composable<Ruta.Listas> {
                    PantallaPrincipal(DestinoBarra.LISTAS, irA) { ContenidoListas(onLista = { navController.navigate(Ruta.Lista(it)) }) }
                }
                composable<Ruta.Lista> { entradaLista ->
                    PantallaLista(
                        listaId = entradaLista.toRoute<Ruta.Lista>().listaId,
                        onBack = atras,
                        onAvatar = ajustes,
                        onPerfil = { navController.navigate(Ruta.Detalle(it)) },
                    )
                }
                composable<Ruta.Buscar> {
                    PantallaPrincipal(DestinoBarra.BUSCAR, irA) { ContenidoBuscar(onPerfil = { navController.navigate(Ruta.Detalle(it)) }) }
                }
                composable<Ruta.Ajustes> { PantallaPrincipal(DestinoBarra.AJUSTES, irA) }
                composable<Ruta.Anadir> { PantallaFormularioPerfil(onSalir = atras, onAvatar = ajustes) }
                composable<Ruta.Detalle> { entradaDetalle ->
                    val perfilId = entradaDetalle.toRoute<Ruta.Detalle>().perfilId
                    PantallaDetallePerfil(
                        perfilId = perfilId,
                        onBack = atras,
                        onAvatar = ajustes,
                        onEditar = { navController.navigate(Ruta.Editar(perfilId)) { launchSingleTop = true } },
                        onLista = { navController.navigate(Ruta.Lista(it)) },
                    )
                }
                composable<Ruta.Editar> { entradaEditar ->
                    PantallaFormularioPerfil(onSalir = atras, onAvatar = ajustes, perfilId = entradaEditar.toRoute<Ruta.Editar>().perfilId)
                }
            }
            CineToastHost(avisos, conBarraInferior)
        }
    }
}
