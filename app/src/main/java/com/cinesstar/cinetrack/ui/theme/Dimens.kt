package com.cinesstar.cinetrack.ui.theme

import androidx.compose.ui.unit.dp

/** Espaciado base 4 dp (design.md §2.7). */
object Spacing {
    val Xxs = 2.dp
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 16.dp
    val Lg = 24.dp
    val Xl = 32.dp

    /** Margen lateral de pantalla. */
    val Margin = 16.dp
}

/** Medidas de componentes concretos (design.md §3). Ninguna medida fija vive fuera de `ui/theme`. */
object Dimens {
    // §3.2 / §3.3 Barra superior
    val TopBarContentHeight = 52.dp
    val TopBarBottomPadding = 8.dp
    val LogoHeight = 32.dp
    val AvatarSize = 32.dp
    val AvatarIcon = 18.dp
    val BackButton = 44.dp
    val BackIcon = 24.dp
    val PillPaddingH = 4.dp
    val PillPaddingV = 2.dp

    // §3.4 Barra inferior
    val BottomBarHeight = 64.dp
    val BottomBarPaddingH = 4.dp
    val NavItemMin = 44.dp
    val NavIcon = 22.dp
    val NavFab = 48.dp
    val NavFabIcon = 26.dp
    val NavFabLift = 20.dp
    val NavFabLabelGap = 2.dp

    /** `pb-24`: margen inferior del contenido para no quedar bajo la barra. */
    val ContentBottomPadding = 96.dp

    // §2.8 Sombras (radio de difuminado aproximado de shadow-lg y de la sombra de barra)
    val ShadowBar = 8.dp
    val ShadowLg = 15.dp

    /** §3.2 Desenfoque de las barras (`backdrop-blur-xl`). */
    val BarBlur = 24.dp

    /** §11 Tablet: ancho máximo del contenido. */
    val MaxContentWidth = 600.dp
}

/** Movimiento (design.md §2.9). */
object Motion {
    const val PRESSED_SCALE_BUTTON = 0.95f
    const val PRESSED_SCALE_LIST_CARD = 0.99f
    const val PRESSED_SCALE_DESTRUCTIVE = 0.98f
    const val COLOR_TRANSITION_MS = 150
    const val TOAST_FADE_MS = 300
}
