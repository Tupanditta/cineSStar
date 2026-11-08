package com.cinesstar.cinetrack.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.cinesstar.cinetrack.R

/**
 * Material Symbols Outlined variable empaquetada (design.md §15). Los iconos se dibujan por ligadura
 * (el nombre del icono, p. ej. "video_library"), con el eje FILL a 0 (por defecto) o a 1.
 */
@OptIn(ExperimentalTextApi::class)
private fun familiaSymbols(fill: Float) =
    FontFamily(
        Font(
            resId = R.font.material_symbols_outlined,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.Setting("FILL", fill),
                    FontVariation.weight(400),
                    FontVariation.Setting("GRAD", 0f),
                    FontVariation.Setting("opsz", 24f),
                ),
        ),
    )

object SymbolsFamily {
    val Outlined = familiaSymbols(fill = 0f)
    val Filled = familiaSymbols(fill = 1f)
}
