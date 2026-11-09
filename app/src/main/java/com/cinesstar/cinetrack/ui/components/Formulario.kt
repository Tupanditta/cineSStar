package com.cinesstar.cinetrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.DimensComunes
import com.cinesstar.cinetrack.ui.theme.Spacing
import java.util.Locale

/**
 * Etiqueta de sección (design.md §2.5): label-md en MAYÚSCULAS, `tracking-wider`, `on-surface-variant`. Con
 * [obligatorio] añade el asterisco en `secondary`; con [sufijo] un texto en `outline` de peso normal ("(Opcional)").
 */
@Composable
fun EtiquetaSeccion(
    texto: String,
    modifier: Modifier = Modifier,
    obligatorio: String? = null,
    sufijo: String? = null,
) {
    val estilo = CineTheme.typography.sectionLabel
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.Xs), verticalAlignment = Alignment.CenterVertically) {
        Text(text = texto.uppercase(LocaleEs), style = estilo, color = MaterialTheme.colorScheme.onSurfaceVariant)
        obligatorio?.let { Text(text = it, style = estilo, color = MaterialTheme.colorScheme.secondary) }
        sufijo?.let {
            Text(text = it.uppercase(LocaleEs), style = estilo, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.outline)
        }
    }
}

/** Español: mayúsculas de las etiquetas y fechas. */
val LocaleEs: Locale = Locale.forLanguageTag("es")

/**
 * Campo de texto del prototipo (design.md §3.7): fondo `surface-container-low` que pasa a `surface-container` al
 * enfocar, sin borde ni anillo; placeholder en `outline`.
 */
@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    estilo: TextStyle = CineTheme.typography.bodyMd,
    forma: Shape = CineShapes.Xl,
    fondo: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    fondoEnfocado: Color = MaterialTheme.colorScheme.surfaceContainer,
    relleno: PaddingValues = PaddingValues(horizontal = DimensComunes.FieldPaddingH, vertical = DimensComunes.FieldPaddingV),
    unaLinea: Boolean = true,
    lineasMinimas: Int = 1,
    teclado: KeyboardOptions = KeyboardOptions.Default,
    accionesTeclado: KeyboardActions = KeyboardActions.Default,
    alineacionPlaceholder: Alignment = Alignment.CenterStart,
    principio: (@Composable BoxScope.() -> Unit)? = null,
) {
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    BasicTextField(
        value = valor,
        onValueChange = onCambio,
        modifier = modifier,
        textStyle = estilo.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = unaLinea,
        minLines = lineasMinimas,
        keyboardOptions = teclado,
        keyboardActions = accionesTeclado,
        interactionSource = interaccion,
        decorationBox = { campo ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(forma)
                        .background(if (enfocado) fondoEnfocado else fondo),
            ) {
                principio?.invoke(this)
                Box(modifier = Modifier.fillMaxWidth().padding(relleno), contentAlignment = alineacionPlaceholder) {
                    if (valor.isEmpty()) {
                        Text(text = placeholder, style = estilo, color = MaterialTheme.colorScheme.outline, maxLines = lineasMinimas)
                    }
                    campo()
                }
            }
        },
    )
}
