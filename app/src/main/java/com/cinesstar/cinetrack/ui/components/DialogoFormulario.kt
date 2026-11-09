package com.cinesstar.cinetrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.ui.theme.CineAlpha
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.DimensComunes
import com.cinesstar.cinetrack.ui.theme.DimensDialogoFormulario
import com.cinesstar.cinetrack.ui.theme.Sombras
import com.cinesstar.cinetrack.ui.theme.Spacing

/**
 * Diálogo de formulario (design.md §3.6-1): centrado, ancho máx. 384 dp, `rounded-xl`, fondo `surface-container-high`,
 * padding 16, separación 16, borde 1 dp `slate-700` 60 %, scrim negro 75 %. Cabecera con icono 22 dp `#38BDF8`, título
 * `headline-md` y botón `close`.
 */
@Composable
fun DialogoFormulario(
    titulo: String,
    icono: String,
    onCerrar: () -> Unit,
    tag: String,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(CineTheme.colors.black.copy(alpha = CineAlpha.A75))
                    .systemBarsPadding()
                    .imePadding()
                    .padding(Spacing.Margin),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = DimensComunes.DialogMaxWidth)
                        .fillMaxWidth()
                        .shadow(Sombras.Xl, CineShapes.Xl)
                        .clip(CineShapes.Xl)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(DimensDialogoFormulario.Borde, CineTheme.colors.slate700.copy(alpha = CineAlpha.A60), CineShapes.Xl)
                        .padding(DimensDialogoFormulario.Padding)
                        .testTag(tag),
                verticalArrangement = Arrangement.spacedBy(Spacing.Md),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
                    SymbolIcon(icono, null, DimensDialogoFormulario.HeaderIcon, CineTheme.colors.sky400)
                    Text(
                        text = titulo,
                        style = CineTheme.typography.headlineMd,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(DimensComunes.TouchTarget)
                                .clip(CineShapes.Full)
                                .clickable(role = Role.Button, onClick = onCerrar)
                                .testTag(TagsDialogo.CERRAR),
                        contentAlignment = Alignment.Center,
                    ) {
                        SymbolIcon(
                            "close",
                            stringResource(R.string.cd_cerrar),
                            DimensDialogoFormulario.CloseIcon,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                contenido()
            }
        }
    }
}

/** Botones del pie de un diálogo de formulario: "Cancelar" (`surface-container`) y la acción (`#38BDF8`), a la derecha. */
@Composable
fun BotonesDialogoFormulario(
    accion: String,
    onAccion: () -> Unit,
    onCancelar: () -> Unit,
    cancelar: String = stringResource(R.string.comun_cancelar),
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.Sm, Alignment.End)) {
        Text(
            text = cancelar,
            style = CineTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .clip(CineShapes.Lg)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(role = Role.Button, onClick = onCancelar)
                    .padding(horizontal = DimensDialogoFormulario.BotonPaddingH, vertical = DimensDialogoFormulario.BotonPaddingV)
                    .testTag(TagsDialogo.CANCELAR),
        )
        Text(
            text = accion,
            style = CineTheme.typography.labelMd,
            fontWeight = FontWeight.Bold,
            color = CineTheme.colors.slate950,
            modifier =
                Modifier
                    .shadow(Sombras.Sm, CineShapes.Lg)
                    .clip(CineShapes.Lg)
                    .background(CineTheme.colors.sky400)
                    .clickable(role = Role.Button, onClick = onAccion)
                    .padding(horizontal = DimensDialogoFormulario.BotonPaddingH, vertical = DimensDialogoFormulario.BotonPaddingV)
                    .testTag(TagsDialogo.CONFIRMAR),
        )
    }
}

/** Error de validación bajo un campo (M-07, M-08): `body-sm` `secondary` con icono `error`. */
@Composable
fun ErrorCampo(
    texto: String,
    tag: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Xs), verticalAlignment = Alignment.CenterVertically) {
        SymbolIcon("error", null, DimensDialogoFormulario.ErrorIcon, MaterialTheme.colorScheme.secondary)
        Text(
            text = texto,
            style = CineTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.testTag(tag),
        )
    }
}

/** Etiqueta de campo de los diálogos de Listas (`label-sm` `on-surface-variant`, sin mayúsculas). */
@Composable
fun EtiquetaCampo(texto: String) {
    Text(text = texto, style = CineTheme.typography.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
