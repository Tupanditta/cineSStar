package com.cinesstar.cinetrack.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colores de una insignia o chip: fondo y texto. */
@Immutable
data class ParColores(
    val fondo: Color,
    val texto: Color,
)

/** Estilo de un estado de visualización (design.md §2.3). */
@Immutable
data class EstiloEstado(
    /** Insignia de las tarjetas de Biblioteca. */
    val insignia: ParColores,
    /** Chip de los resultados de Buscar. */
    val chip: ParColores,
    /** Color del icono o punto que acompaña a la insignia. */
    val icono: Color,
)

/** Tokens que Material 3 no tiene: §2.2 (Tailwind), §2.3 (estados) y §2.4 (prioridad). */
@Immutable
data class CineColors(
    val sky200: Color,
    val sky300: Color,
    val sky400: Color,
    val sky500: Color,
    val sky950: Color,
    val slate950: Color,
    val slate700: Color,
    val emerald500: Color,
    val black: Color,
    val estadoViendo: EstiloEstado,
    val estadoVisto: EstiloEstado,
    val estadoPendiente: EstiloEstado,
    val prioridadAlta: ParColores,
    val prioridadMedia: ParColores,
    val prioridadBaja: ParColores,
    /** Fondo de la barra superior: surface al 80 % (§3.2). */
    val barraSuperior: Color,
    /** Fondo de la barra inferior: surface al 90 % (§3.4). */
    val barraInferior: Color,
    /** Sombra del botón central "Añadir": negro al 40 % (§3.4, §13). */
    val sombraFab: Color,
)

val DefaultCineColors =
    CineColors(
        sky200 = TailwindPalette.Sky200,
        sky300 = TailwindPalette.Sky300,
        sky400 = TailwindPalette.Sky400,
        sky500 = TailwindPalette.Sky500,
        sky950 = TailwindPalette.Sky950,
        slate950 = TailwindPalette.Slate950,
        slate700 = TailwindPalette.Slate700,
        emerald500 = TailwindPalette.Emerald500,
        black = TailwindPalette.Black,
        estadoViendo =
            EstiloEstado(
                insignia = ParColores(CinePalette.SurfaceContainerHigh, TailwindPalette.Sky400),
                chip = ParColores(CinePalette.PrimaryContainer.copy(alpha = CineAlpha.A15), CinePalette.Primary),
                icono = TailwindPalette.Sky400,
            ),
        estadoVisto =
            EstiloEstado(
                insignia = ParColores(CinePalette.SurfaceContainerHighest, CinePalette.OnSurface),
                chip = ParColores(CinePalette.SurfaceContainerHighest, CinePalette.OnSurfaceVariant),
                icono = TailwindPalette.Sky400,
            ),
        estadoPendiente =
            EstiloEstado(
                insignia = ParColores(CinePalette.SurfaceContainerHighest, CinePalette.Secondary),
                chip = ParColores(CinePalette.SurfaceContainerHighest, CinePalette.Secondary),
                icono = CinePalette.Secondary,
            ),
        prioridadAlta = ParColores(CinePalette.ErrorContainer.copy(alpha = CineAlpha.A30), CinePalette.Error),
        prioridadMedia = ParColores(TailwindPalette.Sky500.copy(alpha = CineAlpha.A20), TailwindPalette.Sky300),
        prioridadBaja = ParColores(CinePalette.SurfaceContainerHighest, CinePalette.OnSurfaceVariant),
        barraSuperior = CinePalette.Surface.copy(alpha = CineAlpha.A80),
        barraInferior = CinePalette.Surface.copy(alpha = CineAlpha.A90),
        sombraFab = TailwindPalette.Black.copy(alpha = CineAlpha.A40),
    )

val LocalCineColors = staticCompositionLocalOf { DefaultCineColors }
