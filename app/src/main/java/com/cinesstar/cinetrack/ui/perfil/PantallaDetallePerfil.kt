package com.cinesstar.cinetrack.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.contenedor
import com.cinesstar.cinetrack.data.repository.FichaPerfil
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.room.PertenenciaLista
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.PasoPuntuacion
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.ui.components.Aviso
import com.cinesstar.cinetrack.ui.components.CampoTexto
import com.cinesstar.cinetrack.ui.components.CineSlider
import com.cinesstar.cinetrack.ui.components.DialogoConfirmacion
import com.cinesstar.cinetrack.ui.components.EtiquetaSeccion
import com.cinesstar.cinetrack.ui.components.HojaInferior
import com.cinesstar.cinetrack.ui.components.LocalAvisos
import com.cinesstar.cinetrack.ui.components.LocaleEs
import com.cinesstar.cinetrack.ui.components.PantallaSecundaria
import com.cinesstar.cinetrack.ui.components.SymbolIcon
import com.cinesstar.cinetrack.ui.components.conNegrita
import com.cinesstar.cinetrack.ui.components.etiqueta
import com.cinesstar.cinetrack.ui.components.icono
import com.cinesstar.cinetrack.ui.components.iconoDetalle
import com.cinesstar.cinetrack.ui.components.iconoGenero
import com.cinesstar.cinetrack.ui.components.textoError
import com.cinesstar.cinetrack.ui.theme.CineAlpha
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.Dimens
import com.cinesstar.cinetrack.ui.theme.DimensComunes
import com.cinesstar.cinetrack.ui.theme.DimensDetalle
import com.cinesstar.cinetrack.ui.theme.LeadingRelaxed
import com.cinesstar.cinetrack.ui.theme.LeadingTight
import com.cinesstar.cinetrack.ui.theme.Sombras
import com.cinesstar.cinetrack.ui.theme.Spacing
import com.cinesstar.cinetrack.ui.theme.TrackingWide
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Pantalla "Detalle del perfil" (CU-07, design.md §7). Desde ella (CU-07 A4) se cambia el estado (CU-05), la nota
 * (CU-03) y la crítica (CU-04), se abre la edición (CU-02) y se elimina el perfil (CU-06).
 */
@Composable
fun PantallaDetallePerfil(
    perfilId: Long,
    onBack: () -> Unit,
    onAvatar: () -> Unit,
    onEditar: () -> Unit = {},
) {
    val contenedor = LocalContext.current.contenedor
    val vm: DetallePerfilViewModel = viewModel(key = "detalle_$perfilId") { DetallePerfilViewModel(perfilId, contenedor.perfiles) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val edicion by vm.edicion.collectAsStateWithLifecycle()
    val avisos = LocalAvisos.current
    val textoNoExiste = stringResource(R.string.aviso_perfil_no_existe)
    val textoEliminado = stringResource(R.string.toast_perfil_eliminado)
    val textoError = stringResource(R.string.m25_error_inesperado)
    var confirmarEliminar by rememberSaveable { mutableStateOf(false) }
    var confirmarBorrarCritica by rememberSaveable { mutableStateOf(false) }

    // CU-07 E1: el perfil ya no existe → vuelve atrás con un aviso. CU-06 paso 5: "Perfil eliminado".
    LaunchedEffect(estado) {
        when (estado) {
            EstadoDetalle.NoExiste -> avisos.mostrar(Aviso(textoNoExiste))
            EstadoDetalle.Eliminado -> avisos.mostrar(Aviso(textoEliminado))
            else -> return@LaunchedEffect
        }
        onBack()
    }
    LaunchedEffect(vm) { vm.eventos.collect { avisos.mostrar(Aviso(textoError)) } }

    val ficha = (estado as? EstadoDetalle.Listo)?.ficha
    val tipo = ficha?.detalle?.perfil?.tipo ?: TipoPerfil.PELICULA
    PantallaSecundaria(
        titulo = stringResource(if (tipo == TipoPerfil.PELICULA) R.string.titulo_detalle_pelicula else R.string.titulo_detalle_serie),
        tag = TagsDetalle.PANTALLA,
        onBack = onBack,
        onAvatarClick = onAvatar,
    ) {
        if (ficha != null) {
            Contenido(
                ficha,
                edicion,
                vm,
                Acciones(
                    onEditar = onEditar,
                    onEliminar = { confirmarEliminar = true },
                    onBorrarCritica = { confirmarBorrarCritica = true },
                ),
            )
        }
    }

    if (ficha != null && edicion.critica != null) HojaCritica(edicion, vm)
    if (ficha != null && confirmarEliminar) {
        DialogoConfirmacion(
            titulo = stringResource(R.string.eliminar_titulo),
            texto = conNegrita(stringResource(R.string.m06_eliminar_perfil), ficha.detalle.perfil.nombre),
            confirmar = stringResource(R.string.eliminar_confirmar),
            cancelar = stringResource(R.string.comun_cancelar),
            onConfirmar = {
                confirmarEliminar = false
                vm.eliminar()
            },
            onCancelar = { confirmarEliminar = false },
        )
    }
    if (ficha != null && confirmarBorrarCritica) {
        DialogoConfirmacion(
            titulo = stringResource(R.string.borrar_critica_titulo),
            texto = stringResource(R.string.borrar_critica_texto),
            confirmar = stringResource(R.string.borrar_critica_confirmar),
            cancelar = stringResource(R.string.comun_cancelar),
            onConfirmar = {
                confirmarBorrarCritica = false
                vm.borrarCritica()
            },
            onCancelar = { confirmarBorrarCritica = false },
        )
    }
}

/** Acciones del detalle que abren otra pantalla o un diálogo. */
private class Acciones(
    val onEditar: () -> Unit,
    val onEliminar: () -> Unit,
    val onBorrarCritica: () -> Unit,
)

@Composable
private fun Contenido(
    ficha: FichaPerfil,
    edicion: EdicionDetalle,
    vm: DetallePerfilViewModel,
    acciones: Acciones,
) {
    val arriba = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Dimens.TopBarContentHeight
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag(TagsDetalle.SCROLL)
                .padding(top = arriba)
                .navigationBarsPadding()
                .padding(start = Spacing.Margin, end = Spacing.Margin, bottom = DimensDetalle.ContentBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.Lg),
    ) {
        Hero(ficha, acciones)
        SelectorEstado(ficha.detalle.perfil.estado, vm::cambiarEstado)
        TarjetaNota(ficha, vm::alternarEditorNota)
        if (edicion.nota != null) EditorNota(edicion, vm)
        FichaTecnica(ficha)
        EnTusListas(ficha.listas)
        Critica(ficha.detalle.perfil.comentario, ficha.detalle.perfil.comentarioEditadoEn, vm::abrirEditorCritica, acciones.onBorrarCritica)
        BloqueEliminar(acciones.onEliminar)
    }
}

/** Sube el contenido [alto] sobre el elemento anterior y descuenta ese alto (`-mt-16`). */
private fun Modifier.subir(alto: Dp): Modifier =
    layout { medible, restricciones ->
        val colocable = medible.measure(restricciones)
        val desplazamiento = alto.roundToPx()
        layout(colocable.width, (colocable.height - desplazamiento).coerceAtLeast(0)) { colocable.place(0, -desplazamiento) }
    }

@Composable
private fun Pildora(
    texto: String,
    color: Color,
    modifier: Modifier = Modifier,
    tracking: Boolean = false,
) {
    Text(
        text = texto,
        style = CineTheme.typography.labelSm,
        letterSpacing = if (tracking) TrackingWide else CineTheme.typography.labelSm.letterSpacing,
        color = color,
        modifier =
            modifier
                .clip(CineShapes.Full)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = CineAlpha.A80))
                .padding(horizontal = DimensComunes.BadgePaddingH, vertical = DimensComunes.BadgePaddingV),
    )
}

@Composable
private fun Hero(
    ficha: FichaPerfil,
    acciones: Acciones,
) {
    val perfil = ficha.detalle.perfil
    val contenedor = MaterialTheme.colorScheme.surfaceContainer
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(Sombras.Md, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(contenedor),
    ) {
        // Fondo sin carátula (design.md §7.1, §12): icono del tipo sobre surface-container-high y degradado.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(DimensDetalle.HeroHeight)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(perfil.tipo.icono(), null, DimensDetalle.HeroPlaceholderIcon, MaterialTheme.colorScheme.surfaceContainerHighest)
            Box(
                modifier =
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, contenedor.copy(alpha = CineAlpha.A60), contenedor)),
                    ),
            )
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(DimensDetalle.HeroInset),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
            ) {
                Pildora(
                    stringResource(perfil.tipo.etiqueta()),
                    MaterialTheme.colorScheme.onSurface,
                    Modifier.testTag(TagsDetalle.TIPO),
                    tracking = true,
                )
                Pildora(
                    stringResource(perfil.estado.etiqueta()),
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    Modifier.testTag(TagsDetalle.ESTADO_HERO),
                )
            }
            MenuHero(acciones, Modifier.align(Alignment.TopEnd).padding(DimensDetalle.HeroInset))
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = DimensDetalle.HeroInfoPadding,
                        end = DimensDetalle.HeroInfoPadding,
                        bottom = DimensDetalle.HeroInfoPadding,
                    ).subir(DimensDetalle.HeroLift),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Md),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(DimensDetalle.PosterWidth, DimensDetalle.PosterHeight)
                        .shadow(Sombras.Xl, CineShapes.Xl)
                        .clip(CineShapes.Xl)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(perfil.tipo.icono(), null, DimensDetalle.PosterIcon, MaterialTheme.colorScheme.surfaceContainerHighest)
            }
            Column(modifier = Modifier.weight(1f).padding(bottom = DimensDetalle.TitleBottom)) {
                Text(
                    text = perfil.nombre,
                    style = CineTheme.typography.headlineLgMobile,
                    lineHeight = LeadingTight,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(TagsDetalle.TITULO),
                )
                if (ficha.detalle.generos.isNotEmpty()) {
                    Text(
                        text = ficha.detalle.generos.joinToString(SEPARADOR) { it.nombre },
                        style = CineTheme.typography.bodySm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                LineaNota(ficha)
            }
        }
    }
}

@Composable
private fun LineaNota(ficha: FichaPerfil) {
    Row(
        modifier = Modifier.padding(top = Spacing.Xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val nota = ficha.detalle.puntuacion
        SymbolIcon("star", null, DimensDetalle.StarIcon, MaterialTheme.colorScheme.primary, filled = true)
        Text(
            text = nota?.formatear() ?: stringResource(R.string.sin_puntuar),
            style = CineTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.primary,
        )
        ficha.mejorRanking?.let { ranking ->
            Text(
                text = stringResource(R.string.detalle_en_ranking, ranking.posicion ?: 0, ranking.nombre),
                style = CineTheme.typography.labelSm,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Botón ⋮ del hero (design.md §7.1): menú Editar (CU-02) / Eliminar (CU-06). "Añadir a lista" llega con las listas (S5). */
@Composable
private fun MenuHero(
    acciones: Acciones,
    modifier: Modifier = Modifier,
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .size(DimensDetalle.MenuButton)
                    .clip(CineShapes.Full)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = CineAlpha.A80))
                    .clickable(role = Role.Button, onClick = { abierto = true })
                    .testTag(TagsDetalle.MENU),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon("more_vert", stringResource(R.string.cd_mas_opciones), DimensDetalle.MenuIcon, MaterialTheme.colorScheme.onSurface)
        }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            OpcionMenu(stringResource(R.string.menu_editar), "edit", TagsDetalle.MENU_EDITAR) {
                abierto = false
                acciones.onEditar()
            }
            OpcionMenu(stringResource(R.string.menu_eliminar), "delete", TagsDetalle.MENU_ELIMINAR) {
                abierto = false
                acciones.onEliminar()
            }
        }
    }
}

@Composable
private fun OpcionMenu(
    texto: String,
    icono: String,
    tag: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = texto, style = CineTheme.typography.bodyMd, color = MaterialTheme.colorScheme.onSurface) },
        leadingIcon = { SymbolIcon(icono, null, DimensDetalle.MenuIcon, MaterialTheme.colorScheme.onSurfaceVariant) },
        onClick = onClick,
        modifier = Modifier.testTag(tag),
    )
}

/** RF-22 / CU-05: estado del perfil (design.md §7.2). Tocar otro estado lo guarda; el activo no cambia nada (A1). */
@Composable
private fun SelectorEstado(
    actual: Estado,
    onElegir: (Estado) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
        EtiquetaSeccion(stringResource(R.string.detalle_estado))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(Sombras.Sm, CineShapes.Xl)
                    .clip(CineShapes.Xl)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(Spacing.Xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
        ) {
            Estado.entries.forEach { opcion ->
                val activo = opcion == actual
                val color = if (activo) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(CineShapes.Lg)
                            .background(if (activo) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .selectable(selected = activo, role = Role.Tab, onClick = { onElegir(opcion) })
                            .padding(vertical = DimensDetalle.EstadoPaddingV, horizontal = DimensDetalle.EstadoPaddingH)
                            .testTag(TagsDetalle.estado(opcion)),
                    horizontalArrangement = Arrangement.spacedBy(DimensDetalle.EstadoGap, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SymbolIcon(opcion.iconoDetalle(), null, DimensDetalle.EstadoIcon, color, filled = activo)
                    Text(
                        text = stringResource(opcion.etiqueta()),
                        style = CineTheme.typography.labelMd,
                        fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Normal,
                        color = color,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun Tarjeta(
    modifier: Modifier = Modifier,
    separacion: Dp = Spacing.Md,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(Sombras.Sm, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(DimensDetalle.CardPadding),
        verticalArrangement = Arrangement.spacedBy(separacion),
        content = contenido,
    )
}

@Composable
private fun CabeceraTarjeta(
    icono: String,
    titulo: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Sm), verticalAlignment = Alignment.CenterVertically) {
        SymbolIcon(icono, null, DimensDetalle.CardHeaderIcon, MaterialTheme.colorScheme.primary)
        Text(text = titulo, style = CineTheme.typography.titleMd, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TarjetaNota(
    ficha: FichaPerfil,
    onAjustar: () -> Unit,
) {
    val nota = ficha.detalle.puntuacion
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CajaNota(nota)
            }
            // CU-03 disparador: "Ajustar" abre el editor de nota (design.md §7.3).
            Row(
                modifier =
                    Modifier
                        .clip(CineShapes.Lg)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(role = Role.Button, onClick = onAjustar)
                        .padding(horizontal = DimensDetalle.AjustarPaddingH, vertical = DimensDetalle.AjustarPaddingV)
                        .testTag(TagsDetalle.AJUSTAR),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon("edit", null, DimensDetalle.AjustarIcon, MaterialTheme.colorScheme.onSurface)
                Text(
                    text = stringResource(R.string.detalle_ajustar),
                    style = CineTheme.typography.labelMd,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Se coloca dentro de la fila de la tarjeta: caja "Tu nota" y, a su lado, la nota grande con su rango. */
@Composable
private fun CajaNota(nota: Puntuacion?) {
    Column(
        modifier =
            Modifier
                .size(DimensDetalle.ScoreBox)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SymbolIcon("grade", null, DimensDetalle.ScoreBoxIcon, MaterialTheme.colorScheme.primary, filled = true)
        Text(
            text = stringResource(R.string.detalle_tu_nota),
            style = CineTheme.typography.labelSm,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
            Text(
                text = nota?.formatear() ?: stringResource(R.string.sin_valor),
                style = CineTheme.typography.displayLg,
                lineHeight = CineTheme.typography.displayLg.fontSize,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alignByBaseline().testTag(TagsDetalle.NOTA),
            )
            Text(
                text = stringResource(R.string.sobre_diez),
                style = CineTheme.typography.titleSm,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Text(
            text = nota?.rango?.etiqueta()?.let { stringResource(it) } ?: stringResource(R.string.sin_puntuar),
            style = CineTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TagsDetalle.RANGO),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FichaTecnica(ficha: FichaPerfil) {
    Tarjeta {
        CabeceraTarjeta("movie_filter", stringResource(R.string.detalle_ficha))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
            EtiquetaDato(stringResource(R.string.detalle_saga_etiqueta))
            FilaSaga(ficha)
        }
        Column(verticalArrangement = Arrangement.spacedBy(DimensDetalle.ChipGap), modifier = Modifier.testTag(TagsDetalle.GENEROS)) {
            EtiquetaDato(stringResource(R.string.detalle_generos))
            if (ficha.detalle.generos.isEmpty()) {
                Vacio()
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Xs), verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
                    ficha.detalle.generos.forEach { genero ->
                        Chip(genero.nombre, iconoGenero(genero.nombre), CineShapes.Full, MaterialTheme.colorScheme.surfaceContainerHigh)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(DimensDetalle.ChipGap), modifier = Modifier.testTag(TagsDetalle.IDIOMAS)) {
            EtiquetaDato(stringResource(R.string.detalle_idiomas))
            if (ficha.detalle.idiomas.isEmpty()) {
                Vacio()
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Xs), verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
                    ficha.detalle.idiomas.forEach { idioma ->
                        Chip(
                            idioma.nombre,
                            "volume_up",
                            CineShapes.Md,
                            MaterialTheme.colorScheme.surfaceContainerLowest,
                            iconoPrimario = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EtiquetaDato(texto: String) {
    Text(text = texto, style = CineTheme.typography.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** CU-07 A1: un dato opcional vacío se muestra como "—" sin romper el diseño. */
@Composable
private fun Vacio() {
    Text(text = stringResource(R.string.sin_valor), style = CineTheme.typography.bodySm, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Chip(
    texto: String,
    icono: String,
    forma: androidx.compose.ui.graphics.Shape,
    fondo: Color,
    iconoPrimario: Boolean = true,
) {
    Row(
        modifier =
            Modifier
                .clip(forma)
                .background(fondo)
                .padding(horizontal = DimensComunes.BadgePaddingH, vertical = Spacing.Xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (iconoPrimario) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        SymbolIcon(
            icono,
            null,
            DimensComunes.SmallIcon,
            if (iconoPrimario) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = texto, style = CineTheme.typography.labelSm, color = color)
    }
}

@Composable
private fun FilaSaga(ficha: FichaPerfil) {
    val saga = ficha.detalle.saga
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CineShapes.Lg)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(DimensDetalle.SagaRowPadding)
                .testTag(TagsDetalle.SAGA),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon("auto_stories", null, DimensDetalle.SagaIcon, MaterialTheme.colorScheme.tertiary)
        Column {
            Text(
                text = saga?.let { stringResource(R.string.detalle_saga_nombre, it.nombre) } ?: stringResource(R.string.detalle_sin_saga),
                style = CineTheme.typography.titleSm,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (saga != null) {
                Text(
                    text = textoPerfilesSaga(ficha.perfilesSaga),
                    style = CineTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "{n} películas/series guardadas en tu archivo"; si la saga mezcla tipos, "{n} títulos". */
@Composable
private fun textoPerfilesSaga(porTipo: Map<TipoPerfil, Int>): String {
    val peliculas = porTipo[TipoPerfil.PELICULA] ?: 0
    val series = porTipo[TipoPerfil.SERIE] ?: 0
    return when {
        series == 0 -> pluralStringResource(R.plurals.detalle_saga_peliculas, peliculas, peliculas)
        peliculas == 0 -> pluralStringResource(R.plurals.detalle_saga_series, series, series)
        else -> pluralStringResource(R.plurals.detalle_saga_titulos, peliculas + series, peliculas + series)
    }
}

/** design.md §7.5. "+ Añadir a lista" llega con las listas (S5). */
@Composable
private fun EnTusListas(listas: List<PertenenciaLista>) {
    Tarjeta(modifier = Modifier.testTag(TagsDetalle.LISTAS)) {
        CabeceraTarjeta("folder_special", stringResource(R.string.detalle_listas))
        if (listas.isEmpty()) {
            Text(
                text = stringResource(R.string.detalle_sin_listas),
                style = CineTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) { listas.forEach { FilaLista(it) } }
        }
    }
}

@Composable
private fun FilaLista(lista: PertenenciaLista) {
    val (punto, detalle, insignia) =
        when (lista.tipo) {
            TipoLista.RANKING ->
                Triple(
                    MaterialTheme.colorScheme.secondaryContainer,
                    stringResource(R.string.detalle_lista_posicion, lista.posicion ?: 0, lista.total),
                    stringResource(R.string.detalle_insignia_ranking, lista.posicion ?: 0),
                )
            TipoLista.PRIORIDAD -> {
                val nivel = lista.prioridad?.let { stringResource(it.etiqueta()) }.orEmpty()
                Triple(
                    MaterialTheme.colorScheme.primary,
                    stringResource(R.string.detalle_lista_prioridad, nivel),
                    stringResource(R.string.detalle_insignia_prioridad, nivel),
                )
            }
            TipoLista.NORMAL ->
                Triple(
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    pluralStringResource(R.plurals.detalle_lista_titulos, lista.total, lista.total),
                    stringResource(R.string.detalle_insignia_normal),
                )
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CineShapes.Lg)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(DimensDetalle.ListDot).clip(CineShapes.Full).background(punto))
            Column {
                Text(text = lista.nombre, style = CineTheme.typography.titleSm, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text(text = detalle, style = CineTheme.typography.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        Text(
            text = insignia,
            style = CineTheme.typography.labelSm,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .clip(CineShapes.Rounded)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = DimensComunes.BadgePaddingH, vertical = DimensComunes.BadgePaddingV),
        )
    }
}

/** design.md §7.6 / CU-04: `edit_note` abre la hoja de edición y `delete_outline` borra (con confirmación breve). */
@Composable
private fun Critica(
    comentario: String?,
    editadoEn: Long?,
    onEditar: () -> Unit,
    onBorrar: () -> Unit,
) {
    Tarjeta(separacion = Spacing.Sm) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) { CabeceraTarjeta("rate_review", stringResource(R.string.detalle_critica)) }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
                BotonIcono("edit_note", stringResource(R.string.cd_editar_critica), TagsDetalle.EDITAR_CRITICA, onClick = onEditar)
                BotonIcono(
                    "delete_outline",
                    stringResource(R.string.cd_borrar_critica),
                    TagsDetalle.BORRAR_CRITICA,
                    habilitado = comentario != null,
                    onClick = onBorrar,
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(CineShapes.Xl)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(DimensDetalle.CardPadding)
                    .testTag(TagsDetalle.COMENTARIO),
            verticalArrangement = Arrangement.spacedBy(Spacing.Sm),
        ) {
            if (comentario == null) {
                Text(
                    text = stringResource(R.string.detalle_critica_vacia),
                    style = CineTheme.typography.bodyMd,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.detalle_critica_texto, comentario),
                    style = CineTheme.typography.bodyMd,
                    fontStyle = FontStyle.Italic,
                    lineHeight = LeadingRelaxed,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                editadoEn?.let {
                    Text(
                        text = stringResource(R.string.detalle_critica_guardado, formatearFecha(it)),
                        style = CineTheme.typography.labelSm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Botón solo-icono de 18 dp con padding 4 (`p-1 rounded-md`, design.md §7.6). */
@Composable
private fun BotonIcono(
    icono: String,
    descripcion: String,
    tag: String,
    habilitado: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .clip(CineShapes.Md)
                .clickable(enabled = habilitado, role = Role.Button, onClick = onClick)
                .alpha(if (habilitado) 1f else CineAlpha.A40)
                .padding(DimensDetalle.CriticaIconPadding)
                .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(icono, descripcion, DimensDetalle.CriticaIcon, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Editor desplegable de la nota (design.md §7.3, CU-03): valor editable (teclado numérico), slider 0–10 con paso 0.1
 * y sus marcas, M-03 si el valor no es válido, "Quitar puntuación" y "Guardar". Guardar el campo vacío quita la nota.
 */
@Composable
private fun EditorNota(
    edicion: EdicionDetalle,
    vm: DetallePerfilViewModel,
) {
    val texto = edicion.nota.orEmpty()
    val lectura = edicion.lecturaNota
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(Sombras.Md, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(DimensDetalle.CardPadding)
                .testTag(TagsDetalle.EDITOR_NOTA),
        verticalArrangement = Arrangement.spacedBy(Spacing.Sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.detalle_calificar),
                style = CineTheme.typography.labelMd,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            val descripcion = stringResource(R.string.cd_puntuacion)
            CampoTexto(
                valor = texto,
                onCambio = vm::cambiarTextoNota,
                placeholder = stringResource(R.string.sin_valor),
                modifier =
                    Modifier
                        .width(DimensDetalle.EditorNotaCampo)
                        .semantics { contentDescription = descripcion }
                        .testTag(TagsDetalle.EDITOR_NOTA_CAMPO),
                estilo =
                    CineTheme.typography.titleMd.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                    ),
                fondo = Color.Transparent,
                fondoEnfocado = MaterialTheme.colorScheme.surfaceContainer,
                relleno = PaddingValues(horizontal = Spacing.Xs),
                teclado = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                accionesTeclado = KeyboardActions(onDone = { vm.guardarNota() }),
                alineacionPlaceholder = Alignment.CenterEnd,
            )
        }
        CineSlider(
            valor = PasoPuntuacion.valorSlider(texto),
            onCambio = vm::moverSliderNota,
            rango = 0f..PUNTUACION_MAXIMA,
            pasos = PASOS_SLIDER,
            color = MaterialTheme.colorScheme.primaryContainer,
            colorPista = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().testTag(TagsDetalle.EDITOR_NOTA_SLIDER),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(R.string.detalle_marca_min, R.string.detalle_marca_media, R.string.detalle_marca_max).forEach {
                Text(text = stringResource(it), style = CineTheme.typography.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (lectura == Puntuacion.Lectura.Invalida) {
            Row(
                modifier = Modifier.testTag(TagsDetalle.EDITOR_NOTA_ERROR),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon("error", null, DimensComunes.SmallIcon, MaterialTheme.colorScheme.secondary)
                Text(
                    text = textoError(ErrorDominio.PuntuacionInvalida),
                    style = CineTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Añadido por requisito (RF-13): quitar la puntuación.
            Text(
                text = stringResource(R.string.detalle_quitar_puntuacion),
                style = CineTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .clip(CineShapes.Rounded)
                        .clickable(role = Role.Button, onClick = vm::quitarNota)
                        .padding(vertical = Spacing.Xs)
                        .testTag(TagsDetalle.QUITAR_NOTA),
            )
            Box(modifier = Modifier.weight(1f))
            val habilitado = lectura != Puntuacion.Lectura.Invalida
            Text(
                text = stringResource(R.string.comun_guardar),
                style = CineTheme.typography.labelMd,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier =
                    Modifier
                        .alpha(if (habilitado) 1f else CineAlpha.A50)
                        .clip(CineShapes.Lg)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(enabled = habilitado, role = Role.Button, onClick = vm::guardarNota)
                        .padding(horizontal = DimensDetalle.EditorBotonPaddingH, vertical = DimensDetalle.EditorBotonPaddingV)
                        .testTag(TagsDetalle.EDITOR_NOTA_GUARDAR),
            )
        }
    }
}

/** Hoja de edición de la crítica (design.md §7.6, CU-04): campo multilínea de §8.9 con su límite (S-06). */
@Composable
private fun HojaCritica(
    edicion: EdicionDetalle,
    vm: DetallePerfilViewModel,
) {
    val texto = edicion.critica.orEmpty()
    HojaInferior(
        accion = stringResource(R.string.comun_guardar),
        cancelar = stringResource(R.string.comun_cancelar),
        onAccion = vm::guardarCritica,
        onCancelar = vm::cerrarEditorCritica,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Sm), verticalAlignment = Alignment.CenterVertically) {
            SymbolIcon("rate_review", null, DimensDetalle.CardHeaderIcon, MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(R.string.detalle_critica),
                style = CineTheme.typography.titleMd,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
            CampoTexto(
                valor = texto,
                onCambio = vm::cambiarTextoCritica,
                placeholder = stringResource(R.string.form_comentario_placeholder),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = DimensDetalle.CriticaCampoMinHeight, max = DimensDetalle.CriticaCampoMaxHeight)
                        .testTag(TagsDetalle.CRITICA_CAMPO),
                relleno = PaddingValues(DimensDetalle.CardPadding),
                unaLinea = false,
                lineasMinimas = LINEAS_CRITICA,
                alineacionPlaceholder = Alignment.TopStart,
            )
            Text(
                text = stringResource(R.string.detalle_critica_contador, texto.length, PerfilRepository.MAX_COMENTARIO),
                style = CineTheme.typography.labelSm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().testTag(TagsDetalle.CRITICA_CONTADOR),
            )
        }
    }
}

/** design.md §7.7: "Eliminar de mi catálogo" y su nota (CU-06). */
@Composable
private fun BloqueEliminar(onEliminar: () -> Unit) {
    Column(modifier = Modifier.padding(top = DimensDetalle.EliminarTop), verticalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(Sombras.Sm, CineShapes.Xl)
                    .clip(CineShapes.Xl)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .clickable(role = Role.Button, onClick = onEliminar)
                    .padding(horizontal = DimensDetalle.EliminarPaddingH, vertical = DimensDetalle.EliminarPaddingV)
                    .testTag(TagsDetalle.ELIMINAR),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon("delete_forever", null, DimensDetalle.EliminarIcon, MaterialTheme.colorScheme.onErrorContainer)
            Text(
                text = stringResource(R.string.detalle_eliminar),
                style = CineTheme.typography.titleSm,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        Text(
            text = stringResource(R.string.detalle_eliminar_nota),
            style = CineTheme.typography.labelSm,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Sm),
        )
    }
}

private const val PUNTUACION_MAXIMA = 10f

/** 0.0..10 en pasos de 0.1: 99 marcas intermedias. */
private const val PASOS_SLIDER = 99
private const val LINEAS_CRITICA = 6

private const val SEPARADOR = " • "

private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", LocaleEs)

/** "14 de marzo, 2024" (design.md §7.6). */
private fun formatearFecha(milis: Long): String = FORMATO_FECHA.format(Instant.ofEpochMilli(milis).atZone(ZoneId.systemDefault()))
