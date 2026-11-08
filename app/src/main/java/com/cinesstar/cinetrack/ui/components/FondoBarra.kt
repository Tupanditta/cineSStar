package com.cinesstar.cinetrack.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cinesstar.cinetrack.ui.theme.CineAlpha
import com.cinesstar.cinetrack.ui.theme.Dimens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Fondo de barra con desenfoque del contenido que pasa por debajo (`backdrop-blur-xl`, §3.2 y §3.4).
 * En Android < 12 Haze no desenfoca y usa la superficie sólida al 95 % (sprints.md §6).
 */
@Composable
fun Modifier.fondoBarra(
    hazeState: HazeState,
    tinte: Color,
): Modifier {
    val superficie = MaterialTheme.colorScheme.surface
    return this.hazeEffect(
        state = hazeState,
        style =
            HazeStyle(
                backgroundColor = superficie,
                tint = HazeTint(tinte),
                blurRadius = Dimens.BarBlur,
                noiseFactor = 0f,
                fallbackTint = HazeTint(superficie.copy(alpha = CineAlpha.A95)),
            ),
    )
}
