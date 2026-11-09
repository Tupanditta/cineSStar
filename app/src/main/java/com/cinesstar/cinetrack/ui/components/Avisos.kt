package com.cinesstar.cinetrack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.DimensComunes
import com.cinesstar.cinetrack.ui.theme.Motion
import com.cinesstar.cinetrack.ui.theme.Sombras
import com.cinesstar.cinetrack.ui.theme.Spacing
import kotlinx.coroutines.delay

/** Un toast (design.md §3.5). [exito] = variante de "perfil guardado". */
data class Aviso(
    val texto: String,
    val exito: Boolean = false,
    private val id: Long = System.nanoTime(),
)

/** Toasts de la app. Viven por encima de la navegación para que se vean tras volver atrás (CU-01 paso 6). */
@Stable
class Avisos {
    var actual: Aviso? by mutableStateOf(null)
        private set

    fun mostrar(aviso: Aviso) {
        actual = aviso
    }

    internal fun ocultar(aviso: Aviso) {
        if (actual == aviso) actual = null
    }

    companion object {
        const val DURACION_MS = 2_200L
        const val DURACION_EXITO_MS = 2_800L
        const val TAG = "toast"
    }
}

val LocalAvisos = staticCompositionLocalOf { Avisos() }

/** Píldora centrada abajo: 80 dp sobre el borde si hay barra inferior y 24 dp si no (§3.5). */
@Composable
fun CineToastHost(
    avisos: Avisos,
    conBarraInferior: Boolean,
    modifier: Modifier = Modifier,
) {
    val aviso = avisos.actual
    LaunchedEffect(aviso) {
        if (aviso != null) {
            delay(if (aviso.exito) Avisos.DURACION_EXITO_MS else Avisos.DURACION_MS)
            avisos.ocultar(aviso)
        }
    }
    Box(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = aviso != null,
            enter = fadeIn(tween(Motion.TOAST_FADE_MS)),
            exit = fadeOut(tween(Motion.TOAST_FADE_MS)),
            modifier =
                Modifier.padding(
                    bottom = if (conBarraInferior) DimensComunes.ToastBottomConBarra else DimensComunes.ToastBottomSinBarra,
                ),
        ) {
            aviso?.let { Toast(it) }
        }
    }
}

@Composable
private fun Toast(aviso: Aviso) {
    val fondo = if (aviso.exito) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceBright
    val texto = if (aviso.exito) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val icono = if (aviso.exito) MaterialTheme.colorScheme.onPrimaryContainer else CineTheme.colors.sky400
    Row(
        modifier =
            Modifier
                .shadow(Sombras.Xl, CineShapes.Full)
                .clip(CineShapes.Full)
                .background(fondo)
                .padding(horizontal = DimensComunes.ToastPaddingH, vertical = DimensComunes.ToastPaddingV)
                .testTag(Avisos.TAG),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon("check", contentDescription = null, size = DimensComunes.ToastIcon, tint = icono)
        Text(
            text = aviso.texto,
            style = CineTheme.typography.labelMd,
            fontWeight = if (aviso.exito) FontWeight.Bold else null,
            color = texto,
        )
    }
}
