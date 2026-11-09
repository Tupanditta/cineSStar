package com.cinesstar.cinetrack.ui.theme

import androidx.compose.ui.unit.dp

/** Sombras de Tailwind (design.md §2.8), como elevación aproximada. En tema oscuro apenas se ven. */
object Sombras {
    val Sm = 2.dp
    val Md = 6.dp
    val Lg = 15.dp
    val Xl = 25.dp
}

/** Componentes compartidos (design.md §3.5–§3.7, §2.3). */
object DimensComunes {
    // §3.5 Toast
    val ToastPaddingH = 16.dp
    val ToastPaddingV = 10.dp
    val ToastIcon = 18.dp
    val ToastBottomConBarra = 80.dp
    val ToastBottomSinBarra = 24.dp

    // §3.6-2 Diálogo de confirmación
    val DialogMaxWidth = 384.dp
    val DialogPadding = 24.dp
    val DialogIconCircle = 48.dp
    val DialogIcon = 28.dp
    val DialogButtonPaddingV = 10.dp

    // §3.7 Campos de formulario
    val FieldPaddingH = 16.dp
    val FieldPaddingV = 8.dp
    val DropdownIcon = 20.dp

    // Insignias de estado y píldoras pequeñas (§2.3)
    val BadgePaddingH = 8.dp
    val BadgePaddingV = 2.dp
    val SmallIcon = 14.dp

    // Slider (§7.3, §8.5)
    val SliderTrack = 8.dp
    val SliderThumb = 18.dp

    /** Alto que ocupa el slider en el diseño; la zona táctil sigue siendo de 48 dp. */
    val SliderAlturaVisual = 20.dp

    /** Objetivo táctil mínimo (§11). */
    val TouchTarget = 48.dp
}

/** Pantalla Añadir / editar perfil (design.md §8). */
object DimensFormulario {
    val ContentBottom = 40.dp
    val CancelPaddingH = 16.dp
    val CancelPaddingV = 4.dp

    // 2. Tipo de producción
    val SegmentContainerPadding = 4.dp
    val SegmentButtonPaddingV = 8.dp
    val SegmentIcon = 18.dp

    // 3. Nombre
    val NameIcon = 20.dp
    val NameIconStart = 16.dp
    val NamePaddingStart = 44.dp
    val HelpIcon = 14.dp

    // 4. Estado
    val EstadoTilePaddingV = 8.dp
    val EstadoTilePaddingH = 4.dp
    val EstadoIcon = 20.dp

    // 5. Puntuación
    val ScoreCardPadding = 16.dp
    val ScoreStarIcon = 22.dp
    val OpcionalPaddingH = 4.dp
    val OpcionalPaddingV = 2.dp
    val AvisoPadding = 4.dp
    val AvisoIcon = 16.dp
    val StepperPadding = 4.dp
    val StepperButton = 40.dp
    val StepperIcon = 18.dp
    val StepperField = 64.dp
    val FeedbackMinHeight = 20.dp
    val FeedbackIcon = 14.dp

    // 6. Saga
    val SagaLinkIcon = 14.dp
    val InlineSagaPadding = 8.dp
    val InlineFieldPaddingH = 8.dp
    val InlineFieldPaddingV = 6.dp
    val InlineButtonPaddingH = 16.dp
    val InlineCloseIcon = 16.dp

    // 7–8. Chips
    val ChipPaddingH = 8.dp
    val ChipPaddingV = 4.dp

    // 9. Comentario: 4 líneas de body-md (20) + padding 16 arriba y abajo
    val TextAreaPadding = 16.dp
    val TextAreaMinHeight = 112.dp

    // 10. Guardar
    val SaveTop = 16.dp
    val SavePaddingV = 16.dp
    val SaveIcon = 20.dp
}

/** Pantalla Detalle del perfil (design.md §7). */
object DimensDetalle {
    val ContentBottom = 32.dp

    // 7.1 Hero
    val HeroHeight = 224.dp
    val HeroPlaceholderIcon = 64.dp
    val HeroInset = 8.dp
    val HeroLift = 64.dp
    val HeroInfoPadding = 16.dp
    val PosterWidth = 112.dp
    val PosterHeight = 160.dp
    val PosterIcon = 40.dp
    val TitleBottom = 4.dp
    val StarIcon = 16.dp

    // 7.2 Estado
    val EstadoPaddingV = 10.dp
    val EstadoPaddingH = 8.dp
    val EstadoGap = 6.dp
    val EstadoIcon = 18.dp

    // 7.3 Nota
    val ScoreBox = 56.dp
    val ScoreBoxIcon = 24.dp

    // 7.4–7.6 Tarjetas
    val CardPadding = 16.dp
    val CardHeaderIcon = 20.dp
    val SagaRowPadding = 8.dp
    val SagaIcon = 20.dp
    val ChipGap = 6.dp
    val ListDot = 10.dp
}

/** Biblioteca provisional del S2 (la definitiva es la del S4, design.md §4). */
object DimensBiblioteca {
    val RowPadding = 16.dp
    val EmptyIcon = 48.dp
    val EmptyButtonPaddingH = 16.dp
    val EmptyButtonPaddingV = 12.dp
}
