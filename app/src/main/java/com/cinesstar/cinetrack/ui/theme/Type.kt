package com.cinesstar.cinetrack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.cinesstar.cinetrack.R

/** Pesos de Inter que usa el prototipo. */
val PesosInter = listOf(400, 500, 600, 700, 800)

/** Inter variable empaquetada en `res/font` (sin fuentes descargables, RES-03). */
@OptIn(ExperimentalTextApi::class)
val InterFamily =
    FontFamily(
        PesosInter.map { peso ->
            Font(
                resId = R.font.inter,
                weight = FontWeight(peso),
                variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
            )
        },
    )

private fun estilo(
    tamano: Int,
    interlinea: Int,
    peso: Int,
    trackingEm: Double = 0.0,
) = TextStyle(
    fontFamily = InterFamily,
    fontSize = tamano.sp,
    lineHeight = interlinea.sp,
    fontWeight = FontWeight(peso),
    letterSpacing = trackingEm.em,
)

/** Escala tipográfica (design.md §2.5): la del prototipo, con los estilos de título, cuerpo y etiqueta algo mayores. */
@Immutable
data class CineTypography(
    val displayLg: TextStyle = estilo(36, 44, 800, -0.02),
    /** Solo tablet. */
    val headlineLg: TextStyle = estilo(30, 36, 700, -0.01),
    val headlineLgMobile: TextStyle = estilo(24, 32, 700, -0.01),
    val headlineMd: TextStyle = estilo(20, 28, 600, -0.01),
    val titleMd: TextStyle = estilo(17, 24, 600),
    val titleSm: TextStyle = estilo(15, 22, 600),
    val bodyMd: TextStyle = estilo(15, 22, 400),
    val bodySm: TextStyle = estilo(13, 18, 400),
    val labelLg: TextStyle = estilo(15, 22, 600, 0.01),
    val labelMd: TextStyle = estilo(13, 18, 600, 0.02),
    val labelSm: TextStyle = estilo(12, 16, 700, 0.04),
    /** Etiquetas de sección: label-md en MAYÚSCULAS con `tracking-wider` (0.05em). */
    val sectionLabel: TextStyle = estilo(13, 18, 600, 0.05),
    /** Título de la tarjeta de medio de la Biblioteca (design.md §4.1). */
    val tituloTarjeta: TextStyle = estilo(18, 24, 600),
    /** Etiqueta de los contadores de la Biblioteca: conserva el `label-sm` del prototipo (design.md §4-3). */
    val etiquetaContador: TextStyle = estilo(10, 14, 700, 0.04),
)

/** `tracking-tight` de Tailwind, usado en la marca "CineTrack". */
val TrackingTight = (-0.025).em

/** `tracking-wide` de Tailwind (píldora de tipo del hero, design.md §7.1). */
val TrackingWide = 0.025.em

/** `tracking-wider` de Tailwind (etiquetas de los contadores de la Biblioteca, design.md §4-3). */
val TrackingWider = 0.05.em

/** `leading-relaxed` de Tailwind (texto de la crítica, design.md §7.6). */
val LeadingRelaxed = 1.625.em

/** `leading-tight` de Tailwind (título del hero, design.md §7.1). */
val LeadingTight = 1.25.em

val LocalCineTypography = staticCompositionLocalOf { CineTypography() }

/** Correspondencia con los roles de Material 3 para los componentes de la biblioteca. */
fun CineTypography.toMaterial(): Typography =
    Typography(
        displayLarge = displayLg,
        headlineLarge = headlineLgMobile,
        headlineMedium = headlineMd,
        titleLarge = headlineMd,
        titleMedium = titleMd,
        titleSmall = titleSm,
        bodyLarge = bodyMd,
        bodyMedium = bodyMd,
        bodySmall = bodySm,
        labelLarge = labelLg,
        labelMedium = labelMd,
        labelSmall = labelSm,
    )
