package com.cinesstar.cinetrack.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.cinesstar.cinetrack.ui.theme.DimensComunes

/**
 * Texto de una línea para etiquetas en un hueco fijo (barra inferior, pestañas, métricas): usa el tamaño de [estilo]
 * (que ya respeta la fuente del sistema) y solo lo reduce, hasta `DimensComunes.TextoMinimo` reales, si no cabe. Así con la
 * fuente al 200 % no se corta ni desaparece (T9.08).
 */
@Composable
fun TextoAjustable(
    texto: String,
    estilo: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Center,
    lineas: Int = 1,
) {
    // El mínimo es de tamaño real en pantalla: no crece con la fuente del sistema (si no, al 200 % no cabría nunca).
    val minimo = (DimensComunes.TextoMinimo.value / LocalDensity.current.fontScale).sp
    BasicText(
        text = texto,
        modifier = modifier,
        style = estilo.copy(color = color, fontWeight = fontWeight ?: estilo.fontWeight, textAlign = textAlign),
        maxLines = lineas,
        autoSize = TextAutoSize.StepBased(minFontSize = minimo, maxFontSize = estilo.fontSize),
    )
}
