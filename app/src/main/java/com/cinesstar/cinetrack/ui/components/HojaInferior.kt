package com.cinesstar.cinetrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cinesstar.cinetrack.ui.theme.CineAlpha
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.Dimens
import com.cinesstar.cinetrack.ui.theme.DimensComunes
import com.cinesstar.cinetrack.ui.theme.Sombras
import com.cinesstar.cinetrack.ui.theme.Spacing

/**
 * Hoja inferior (design.md §3.6-3): pegada abajo, `rounded-2xl`, padding 20, fondo `surface-container-high`, scrim
 * `background` 80 %. El [contenido] va encima de la fila de botones ("Cancelar" y la acción, a partes iguales).
 */
@Composable
fun HojaInferior(
    accion: String,
    cancelar: String,
    onAccion: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    accionHabilitada: Boolean = true,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onCancelar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = CineAlpha.A80))
                    // Tocar el scrim cierra la hoja, como "Cancelar" (sin semántica: el cierre accesible es "Cancelar").
                    .pointerInput(onCancelar) { detectTapGestures { onCancelar() } }
                    .imePadding()
                    .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier =
                    modifier
                        .widthIn(max = Dimens.MaxContentWidth)
                        .fillMaxWidth()
                        .shadow(Sombras.Xl, CineShapes.Xxl)
                        .clip(CineShapes.Xxl)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        // El toque dentro de la hoja no llega al scrim.
                        .pointerInput(Unit) { detectTapGestures { } }
                        .padding(DimensComunes.SheetPadding)
                        .testTag(TagsHoja.HOJA),
                verticalArrangement = Arrangement.spacedBy(Spacing.Md),
            ) {
                contenido()
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
                    BotonHoja(
                        cancelar,
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialTheme.colorScheme.onSurface,
                        true,
                        TagsHoja.CANCELAR,
                        onCancelar,
                        Modifier.weight(1f),
                    )
                    BotonHoja(
                        accion,
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer,
                        accionHabilitada,
                        TagsHoja.ACCION,
                        onAccion,
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BotonHoja(
    texto: String,
    fondo: Color,
    color: Color,
    habilitado: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(CineShapes.Xl)
                .background(fondo)
                .clickable(enabled = habilitado, role = Role.Button, onClick = onClick)
                .alpha(if (habilitado) 1f else CineAlpha.A50)
                .padding(vertical = DimensComunes.SheetButtonPaddingV)
                .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = texto, style = CineTheme.typography.labelMd, fontWeight = FontWeight.SemiBold, color = color)
    }
}

/** Etiquetas de test de la hoja inferior. */
object TagsHoja {
    const val HOJA = "hoja_inferior"
    const val ACCION = "hoja_accion"
    const val CANCELAR = "hoja_cancelar"
}
