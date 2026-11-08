package com.cinesstar.cinetrack.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.cinesstar.cinetrack.ui.theme.SymbolsFamily

/**
 * Icono de Material Symbols Outlined dibujado por ligadura con la fuente empaquetada.
 * El nombre del icono nunca llega a la semántica: solo [contentDescription] (si es nulo, el icono es decorativo).
 * El tamaño no escala con la fuente del sistema, como un icono normal.
 */
@Composable
fun SymbolIcon(
    nombre: String,
    contentDescription: String?,
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    val density = LocalDensity.current
    // sp que, multiplicados por la escala de fuente del sistema, vuelven a dar exactamente `size`.
    val tamano = (size.value / density.fontScale).sp
    val estilo =
        TextStyle(
            fontFamily = if (filled) SymbolsFamily.Filled else SymbolsFamily.Outlined,
            fontSize = tamano,
            lineHeight = tamano,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            textAlign = TextAlign.Center,
            color = tint,
        )
    val semantica =
        if (contentDescription != null) {
            Modifier.clearAndSetSemantics {
                this.contentDescription = contentDescription
                role = Role.Image
            }
        } else {
            Modifier.clearAndSetSemantics { }
        }
    Box(modifier = modifier.size(size).then(semantica), contentAlignment = Alignment.Center) {
        // El nombre de la ligadura no es texto para el usuario: se quita también del árbol sin fusionar.
        Text(text = nombre, style = estilo, maxLines = 1, softWrap = false, modifier = Modifier.clearAndSetSemantics { })
    }
}
