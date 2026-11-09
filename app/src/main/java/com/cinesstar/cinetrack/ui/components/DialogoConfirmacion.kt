package com.cinesstar.cinetrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cinesstar.cinetrack.ui.theme.CineAlpha
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.DimensComunes
import com.cinesstar.cinetrack.ui.theme.Spacing

/**
 * Diálogo de confirmación destructiva (design.md §3.6-2): círculo `error-container` con `warning`, título centrado,
 * texto `body-sm` y dos botones apilados a ancho completo (acción en `error`, "Cancelar" en `surface-container`).
 */
@Composable
fun DialogoConfirmacion(
    titulo: String,
    texto: String,
    confirmar: String,
    cancelar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) = DialogoConfirmacion(titulo, AnnotatedString(texto), confirmar, cancelar, onConfirmar, onCancelar)

/** Variante con partes del texto en negrita (el nombre del perfil en M-06, design.md §3.6-2). */
@Composable
fun DialogoConfirmacion(
    titulo: String,
    texto: AnnotatedString,
    confirmar: String,
    cancelar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    Dialog(onDismissRequest = onCancelar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceDim.copy(alpha = CineAlpha.A80))
                    .padding(Spacing.Margin),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = DimensComunes.DialogMaxWidth)
                        .fillMaxWidth()
                        .clip(CineShapes.Xl)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(DimensComunes.DialogPadding)
                        .testTag(TagsDialogo.DIALOGO),
                verticalArrangement = Arrangement.spacedBy(Spacing.Md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(DimensComunes.DialogIconCircle)
                            .clip(CineShapes.Full)
                            .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon("warning", null, DimensComunes.DialogIcon, MaterialTheme.colorScheme.onErrorContainer)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
                    Text(
                        text = titulo,
                        style = CineTheme.typography.headlineMd,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = texto,
                        style = CineTheme.typography.bodySm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag(TagsDialogo.TEXTO),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
                    BotonDialogo(
                        confirmar,
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.onError,
                        true,
                        TagsDialogo.CONFIRMAR,
                        onConfirmar,
                    )
                    BotonDialogo(
                        cancelar,
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.onSurface,
                        false,
                        TagsDialogo.CANCELAR,
                        onCancelar,
                    )
                }
            }
        }
    }
}

@Composable
private fun BotonDialogo(
    texto: String,
    fondo: Color,
    color: Color,
    negrita: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CineShapes.Lg)
                .background(fondo)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = DimensComunes.DialogButtonPaddingV)
                .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = texto, style = CineTheme.typography.titleSm, fontWeight = if (negrita) FontWeight.Bold else null, color = color)
    }
}

/**
 * [plantilla] (con un único `%1$s`) con [valor] en negrita: "¿Eliminar «**Dune**»? …". Se usa con `stringResource`
 * para no partir los textos del catálogo de mensajes.
 */
fun conNegrita(
    plantilla: String,
    valor: String,
): AnnotatedString {
    val partes = plantilla.split(MARCADOR, limit = 2)
    return buildAnnotatedString {
        append(partes[0])
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(valor) }
        if (partes.size > 1) append(partes[1])
    }
}

private const val MARCADOR = "%1\$s"
