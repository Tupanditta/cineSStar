package com.cinesstar.cinetrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.cinesstar.cinetrack.navigation.DestinoBarra
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * Esqueleto de una pantalla principal: contenido a pantalla completa (pasa por debajo de las barras, que lo desenfocan),
 * barra superior de marca y barra inferior. El contenido debe dejar 96 dp abajo (`Dimens.ContentBottomPadding`).
 */
@Composable
fun PantallaPrincipal(
    destino: DestinoBarra,
    onDestino: (DestinoBarra) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val hazeState = rememberHazeState()
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag(destino.tagPantalla())) {
        Box(modifier = Modifier.fillMaxSize().hazeSource(hazeState), content = content)
        CineTopBar(
            titulo = stringResource(destino.etiqueta),
            hazeState = hazeState,
            onAvatarClick = { onDestino(DestinoBarra.AJUSTES) },
            modifier = Modifier.align(Alignment.TopCenter),
        )
        CineBottomBar(
            activo = destino,
            hazeState = hazeState,
            onDestino = onDestino,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Esqueleto de una pantalla secundaria: barra superior con "atrás" y sin barra inferior (§3.3). */
@Composable
fun PantallaSecundaria(
    titulo: String,
    tag: String,
    onBack: () -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val hazeState = rememberHazeState()
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag(tag)) {
        Box(modifier = Modifier.fillMaxSize().hazeSource(hazeState), content = content)
        CineTopBarSecondary(
            titulo = titulo,
            hazeState = hazeState,
            onBack = onBack,
            onAvatarClick = onAvatarClick,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/** Etiqueta de test de la pantalla de cada destino: "pantalla_biblioteca"… */
fun DestinoBarra.tagPantalla(): String = "pantalla_" + name.lowercase()
