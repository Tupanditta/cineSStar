package com.cinesstar.cinetrack.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.ui.components.PantallaPrincipal
import com.cinesstar.cinetrack.ui.components.PantallaSecundaria
import com.cinesstar.cinetrack.ui.components.tagPantalla

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
    NavHost(navController = navController, startDestination = Ruta.Biblioteca, modifier = modifier) {
        composable<Ruta.Biblioteca> { PantallaPrincipal(DestinoBarra.BIBLIOTECA, irA) }
        composable<Ruta.Listas> { PantallaPrincipal(DestinoBarra.LISTAS, irA) }
        composable<Ruta.Buscar> { PantallaPrincipal(DestinoBarra.BUSCAR, irA) }
        composable<Ruta.Ajustes> { PantallaPrincipal(DestinoBarra.AJUSTES, irA) }
        composable<Ruta.Anadir> {
            PantallaSecundaria(
                titulo = stringResource(R.string.titulo_anadir_medio),
                tag = DestinoBarra.ANADIR.tagPantalla(),
                onBack = { navController.popBackStack() },
                onAvatarClick = { navController.irA(DestinoBarra.AJUSTES) },
            )
        }
    }
}
