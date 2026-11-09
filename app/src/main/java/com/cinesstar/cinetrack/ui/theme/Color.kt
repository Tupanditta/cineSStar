package com.cinesstar.cinetrack.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta del tema (design.md §2.1, valores canónicos; §13 descarta la variante de Biblioteca/Listas).
 * Único lugar de la app con colores escritos a mano.
 */
object CinePalette {
    val Background = Color(0xFF121316)
    val Surface = Color(0xFF121316)
    val SurfaceDim = Color(0xFF121316)
    val SurfaceContainerLowest = Color(0xFF0D0E11)
    val SurfaceContainerLow = Color(0xFF1B1B1F)
    val SurfaceContainer = Color(0xFF1F1F23)
    val SurfaceContainerHigh = Color(0xFF292A2D)
    val SurfaceContainerHighest = Color(0xFF323742)
    val SurfaceBright = Color(0xFF38393C)
    val SurfaceVariant = Color(0xFF343538)
    val OnSurface = Color(0xFFE3E2E6)
    val OnBackground = Color(0xFFE3E2E6)
    val OnSurfaceVariant = Color(0xFFBDC8D1)
    val Outline = Color(0xFF87929A)
    val OutlineVariant = Color(0xFF3E484F)
    val Primary = Color(0xFF8ED5FF)
    val OnPrimary = Color(0xFF00354A)
    val PrimaryContainer = Color(0xFF38BDF8)
    val OnPrimaryContainer = Color(0xFF004965)
    val Secondary = Color(0xFFFFB3B6)
    val OnSecondary = Color(0xFF68001A)
    val SecondaryContainer = Color(0xFFCC003C)
    val OnSecondaryContainer = Color(0xFFFFDCDC)
    val Tertiary = Color(0xFF83D7FF)
    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)
    val ErrorContainer = Color(0xFF93000A)
    val OnErrorContainer = Color(0xFFFFDAD6)
}

/** Colores de Tailwind que el prototipo usa directamente (design.md §2.2). */
object TailwindPalette {
    val Sky200 = Color(0xFFBAE6FD)
    val Sky300 = Color(0xFF7DD3FC)
    val Sky400 = Color(0xFF38BDF8)
    val Sky500 = Color(0xFF0EA5E9)
    val Sky950 = Color(0xFF082F49)
    val Slate950 = Color(0xFF020617)
    val Slate700 = Color(0xFF334155)
    val Emerald500 = Color(0xFF10B981)
    val Black = Color(0xFF000000)
}

/** Opacidades del prototipo (sufijos de Tailwind como /15, /20 o /80). */
object CineAlpha {
    const val A10 = 0.10f
    const val A15 = 0.15f
    const val A20 = 0.20f
    const val A30 = 0.30f
    const val A40 = 0.40f
    const val A50 = 0.50f
    const val A60 = 0.60f
    const val A75 = 0.75f
    const val A80 = 0.80f
    const val A90 = 0.90f
    const val A95 = 0.95f
}
