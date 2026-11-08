package com.cinesstar.cinetrack.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radios de Tailwind del prototipo (design.md §2.6), en dp. */
object CineRadii {
    val Rounded = 4.dp
    val Md = 6.dp
    val Lg = 8.dp
    val Xl = 12.dp
    val Xxl = 16.dp
}

object CineShapes {
    val Rounded = RoundedCornerShape(CineRadii.Rounded)
    val Md = RoundedCornerShape(CineRadii.Md)
    val Lg = RoundedCornerShape(CineRadii.Lg)
    val Xl = RoundedCornerShape(CineRadii.Xl)
    val Xxl = RoundedCornerShape(CineRadii.Xxl)

    /** `rounded-full`: píldora. */
    val Full = CircleShape
}

val CineMaterialShapes =
    Shapes(
        extraSmall = CineShapes.Rounded,
        small = CineShapes.Lg,
        medium = CineShapes.Xl,
        large = CineShapes.Xxl,
        extraLarge = CineShapes.Xxl,
    )
