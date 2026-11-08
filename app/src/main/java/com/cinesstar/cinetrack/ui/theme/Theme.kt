package com.cinesstar.cinetrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

/** Esquema de color de Material 3 construido con la paleta §2.1. Tema solo oscuro (§1). */
val CineColorScheme =
    darkColorScheme(
        primary = CinePalette.Primary,
        onPrimary = CinePalette.OnPrimary,
        primaryContainer = CinePalette.PrimaryContainer,
        onPrimaryContainer = CinePalette.OnPrimaryContainer,
        secondary = CinePalette.Secondary,
        onSecondary = CinePalette.OnSecondary,
        secondaryContainer = CinePalette.SecondaryContainer,
        onSecondaryContainer = CinePalette.OnSecondaryContainer,
        tertiary = CinePalette.Tertiary,
        error = CinePalette.Error,
        onError = CinePalette.OnError,
        errorContainer = CinePalette.ErrorContainer,
        onErrorContainer = CinePalette.OnErrorContainer,
        background = CinePalette.Background,
        onBackground = CinePalette.OnBackground,
        surface = CinePalette.Surface,
        onSurface = CinePalette.OnSurface,
        surfaceVariant = CinePalette.SurfaceVariant,
        onSurfaceVariant = CinePalette.OnSurfaceVariant,
        surfaceDim = CinePalette.SurfaceDim,
        surfaceBright = CinePalette.SurfaceBright,
        surfaceContainerLowest = CinePalette.SurfaceContainerLowest,
        surfaceContainerLow = CinePalette.SurfaceContainerLow,
        surfaceContainer = CinePalette.SurfaceContainer,
        surfaceContainerHigh = CinePalette.SurfaceContainerHigh,
        surfaceContainerHighest = CinePalette.SurfaceContainerHighest,
        outline = CinePalette.Outline,
        outlineVariant = CinePalette.OutlineVariant,
    )

@Composable
fun CineTrackTheme(content: @Composable () -> Unit) {
    val tipografia = remember { CineTypography() }
    CompositionLocalProvider(
        LocalCineColors provides DefaultCineColors,
        LocalCineTypography provides tipografia,
    ) {
        MaterialTheme(
            colorScheme = CineColorScheme,
            typography = tipografia.toMaterial(),
            shapes = CineMaterialShapes,
            content = content,
        )
    }
}

/** Acceso corto a los tokens propios: `CineTheme.colors`, `CineTheme.typography`. */
object CineTheme {
    val colors: CineColors
        @Composable @ReadOnlyComposable
        get() = LocalCineColors.current

    val typography: CineTypography
        @Composable @ReadOnlyComposable
        get() = LocalCineTypography.current
}
