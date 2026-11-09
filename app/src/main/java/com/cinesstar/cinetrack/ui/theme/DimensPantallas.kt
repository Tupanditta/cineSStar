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

    // §3.6-3 Hoja inferior
    val SheetPadding = 20.dp
    val SheetIconCircle = 40.dp
    val SheetIcon = 20.dp
    val SheetButtonPaddingV = 10.dp

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
    val MenuButton = 36.dp
    val MenuIcon = 20.dp

    // 7.2 Estado
    val EstadoPaddingV = 10.dp
    val EstadoPaddingH = 8.dp
    val EstadoGap = 6.dp
    val EstadoIcon = 18.dp

    // 7.3 Nota
    val ScoreBox = 56.dp
    val ScoreBoxIcon = 24.dp
    val AjustarPaddingH = 16.dp
    val AjustarPaddingV = 8.dp
    val AjustarIcon = 18.dp
    val EditorNotaCampo = 64.dp
    val EditorBotonPaddingH = 16.dp
    val EditorBotonPaddingV = 6.dp

    // 7.4–7.6 Tarjetas
    val CardPadding = 16.dp
    val CardHeaderIcon = 20.dp
    val SagaRowPadding = 8.dp
    val SagaIcon = 20.dp
    val ChipGap = 6.dp
    val ListDot = 10.dp

    // 7.6 Crítica
    val CriticaIconPadding = 4.dp
    val CriticaIcon = 18.dp
    val CriticaCampoMinHeight = 160.dp
    val CriticaCampoMaxHeight = 320.dp

    // 7.7 Eliminar
    val EliminarTop = 8.dp
    val EliminarPaddingH = 16.dp
    val EliminarPaddingV = 14.dp
    val EliminarIcon = 20.dp
}

/** Pantalla Biblioteca (design.md §4). */
object DimensBiblioteca {
    // Vacío (§11)
    val EmptyIcon = 48.dp
    val EmptyButtonPaddingH = 16.dp
    val EmptyButtonPaddingV = 12.dp

    // 2. Chips de tipo
    val TipoPaddingH = 16.dp
    val TipoPaddingV = 8.dp
    val TipoGap = 6.dp
    val TipoIcon = 18.dp

    // 3. Contadores
    val ContadoresPadding = 4.dp
    val ContadorPadding = 8.dp
    val ContadorIcon = 16.dp

    // 4. Escala y agrupar
    val EscalaHeaderIcon = 20.dp
    val EscalaHeaderGap = 6.dp
    val EscalaChipPaddingH = 10.dp
    val EscalaChipPaddingV = 6.dp
    val EscalaChipBorde = 1.dp
    val GrupoIcon = 22.dp

    // 5. Catálogo y §4.1 Tarjeta de medio
    val MostrandoPaddingH = 10.dp
    val MostrandoPaddingV = 3.dp
    val CardPadding = 8.dp

    /** Alto del contenido de la tarjeta: el del póster del prototipo (144), así la tarjeta no crece. */
    val CardContentHeight = 144.dp
    val IconoTipoColumna = 56.dp
    val IconoTipoCaja = 48.dp
    val IconoTipo = 28.dp
    val CardIcon = 16.dp
    val ColumnaPaddingV = 2.dp
    val NotaPaddingH = 8.dp
    val NotaPaddingV = 3.dp
    val NotaGap = 2.dp
    val SagaTop = 2.dp
    val GenerosTop = 6.dp
    val GeneroGap = 6.dp
    val GeneroPaddingH = 8.dp
    val GeneroPaddingV = 3.dp
    val FilaInferiorTop = 8.dp
    val EstadoIcon = 15.dp
    val EstadoPunto = 7.dp
    val InsigniaPaddingH = 10.dp
    val InsigniaPaddingV = 3.dp
}

/** Diálogo de formulario (design.md §3.6-1): Nueva Lista, Añadir título, Añadir a lista, Renombrar. */
object DimensDialogoFormulario {
    val Padding = 16.dp
    val HeaderIcon = 22.dp
    val CloseIcon = 20.dp
    val Borde = 1.dp
    val CampoPadding = 8.dp
    val BotonPaddingH = 16.dp
    val BotonPaddingV = 8.dp
    val ErrorIcon = 14.dp
}

/** Pantalla Listas (design.md §5). */
object DimensListas {
    // 5.1 Pestañas
    val TabsPaddingTop = 4.dp
    val TabsPaddingBottom = 8.dp
    val TabsContainerPadding = 4.dp
    val TabPaddingH = 8.dp
    val TabPaddingV = 4.dp
    val TabIcon = 18.dp
    val TabPunto = 8.dp
    val TabContadorPaddingH = 6.dp

    // 5.2-1 Banner
    val BannerPadding = 16.dp
    val BannerIcon = 18.dp
    val BannerCaja = 48.dp
    val BannerCajaIcon = 28.dp
    val BannerBorde = 1.dp
    val BannerPunto = 8.dp
    val BannerPieTop = 16.dp

    /** El texto del banner ocupa como máximo el 70 % del ancho (`max-w-[70%]`). */
    const val BANNER_FRACCION_TEXTO = 0.7f

    // 5.2-3 Tarjeta de lista
    val CardPadding = 16.dp
    val CajaIcono = 40.dp
    val CajaIconoIcon = 22.dp
    val InsigniaPaddingH = 8.dp
    val InsigniaPaddingV = 2.dp
    val RestriccionPaddingH = 6.dp
    val RestriccionPaddingV = 1.dp
    val RestriccionTop = 2.dp
    val PieTop = 16.dp
    val PieIcon = 16.dp
    val PuntoSeparador = 4.dp
    val MasIcon = 18.dp
    val AltaPaddingH = 6.dp
    val AltaPaddingV = 1.dp

    // 5.2-4 Botón "+ Nueva Lista"
    val NuevaPaddingH = 16.dp
    val NuevaPaddingV = 12.dp
    val NuevaIcon = 22.dp

    /** Padding inferior del contenido (32) además del hueco de la barra inferior. */
    val ContentBottom = 32.dp

    // Vacío (CU-12 A1)
    val VacioIcon = 48.dp

    // 5.3 Nueva Lista
    val MosaicoPadding = 8.dp
    val MosaicoIcon = 18.dp
    val SegmentoPaddingV = 6.dp
    val Anillo = 1.dp
}

/** Pantalla Lista abierta (design.md §6). */
object DimensLista {
    val ContentBottom = 40.dp

    // 6.1 Cabecera
    val CabeceraTop = 16.dp
    val CabeceraBottom = 8.dp
    val InsigniaPaddingH = 8.dp
    val InsigniaPaddingV = 2.dp
    val ChipIcon = 13.dp
    val BotonRedondo = 36.dp
    val BotonRedondoIcon = 18.dp
    val AccionPaddingH = 12.dp
    val AccionPaddingV = 8.dp
    val AccionIcon = 18.dp

    // 6.2 Fila
    val FilaPadding = 8.dp
    val PuestoAncho = 32.dp
    val MedallaIcon = 14.dp
    val PosterAncho = 56.dp
    val PosterAlto = 80.dp
    val PosterIcon = 24.dp
    val PosterInsigniaPaddingH = 3.dp
    val PosterInsigniaInset = 2.dp
    val NotaPaddingH = 6.dp
    val NotaPaddingV = 2.dp
    val NotaIcon = 12.dp
    val PrioridadPaddingH = 6.dp
    val PrioridadPaddingV = 2.dp
    val QuitarBoton = 32.dp
    val QuitarIcon = 18.dp

    // 6.3 Vacía
    val VaciaPadding = 16.dp
    val VaciaIcon = 32.dp

    // 6.4 Pie
    val PieTop = 24.dp
    val EliminarPaddingH = 16.dp
    val EliminarPaddingV = 8.dp
    val EliminarIcon = 18.dp

    // 6.6 Añadir título
    val ResultadosMaxAlto = 192.dp
    val ResultadoPadding = 8.dp
    val MiniaturaAncho = 32.dp
    val MiniaturaAlto = 44.dp
    val MiniaturaIcon = 16.dp
    val ResultadoIcon = 18.dp
    val BuscarIcon = 18.dp
    val BuscarIconStart = 10.dp
    val BuscarPaddingStart = 36.dp

    // S6: acciones del ranking, flechas ▲▼ y opciones al añadir
    val BannerPadding = 12.dp
    val BannerIcon = 18.dp
    val ListoPaddingH = 12.dp
    val ListoPaddingV = 4.dp
    val Flecha = 28.dp
    val FlechaIcon = 18.dp
    val SegmentoPaddingV = 6.dp
    val Anillo = 1.dp
}

/** Pantalla Buscar (design.md §9). */
object DimensBuscar {
    // 9.1 Pestañas
    val TabsPaddingV = 8.dp
    val TabsContainerPadding = 4.dp
    val TabPaddingV = 8.dp
    val TabIcon = 18.dp

    // 9.2-1 Buscador
    val BuscadorPaddingH = 16.dp
    val BuscadorPaddingV = 10.dp
    val BuscadorIcon = 22.dp
    val BorrarIcon = 20.dp
    val AyudaTop = 4.dp
    val AyudaPaddingH = 8.dp

    // 9.2-2 Tipo
    val TipoPaddingH = 16.dp
    val TipoPaddingV = 6.dp

    // 9.2-3 Rango
    val RangoPadding = 16.dp
    val RangoIcon = 20.dp
    val ValoresPaddingH = 8.dp
    val ValoresPaddingV = 2.dp

    /** Ancho de cada valor editable de la píldora ("10" o "7.5" en `label-md` bold). */
    val ValorAncho = 30.dp

    // 9.2-4 Géneros
    val GeneroPaddingH = 12.dp
    val GeneroPaddingV = 6.dp

    // 9.2-5 Idioma y saga
    val DesplegablePadding = 8.dp
    val DesplegableIcon = 16.dp

    // 9.2-6 Estado
    val EstadoPadding = 10.dp
    val EstadoIcon = 20.dp

    // 9.2-7 Acciones
    val AccionesTop = 8.dp
    val LimpiarPaddingV = 12.dp
    val LimpiarIcon = 18.dp
    val AplicarPaddingH = 16.dp
    val AplicarPaddingV = 12.dp
    val AplicarIcon = 22.dp

    // 9.2-8 Resultados
    val ResultadosTop = 8.dp
    val ResultadoPadding = 8.dp
    val PosterAncho = 64.dp
    val PosterAlto = 96.dp
    val PosterIcon = 28.dp
    val ChipsBottom = 4.dp
    val ChipsGap = 6.dp
    val ChipTipoPaddingH = 6.dp
    val ChipTipoPaddingV = 2.dp
    val EstadoTop = 8.dp
    val ChevronIcon = 20.dp
    val ChevronPadding = 8.dp
    val SinResultadosIcon = 40.dp
    val SinResultadosPadding = 24.dp
}
