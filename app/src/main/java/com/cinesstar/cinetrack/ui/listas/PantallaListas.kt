package com.cinesstar.cinetrack.ui.listas

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.contenedor
import com.cinesstar.cinetrack.domain.PanelListas
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.ResumenLista
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.ui.components.Aviso
import com.cinesstar.cinetrack.ui.components.LocalAvisos
import com.cinesstar.cinetrack.ui.components.LocaleEs
import com.cinesstar.cinetrack.ui.components.SymbolIcon
import com.cinesstar.cinetrack.ui.components.etiqueta
import com.cinesstar.cinetrack.ui.theme.CineAlpha
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.Dimens
import com.cinesstar.cinetrack.ui.theme.DimensBiblioteca
import com.cinesstar.cinetrack.ui.theme.DimensListas
import com.cinesstar.cinetrack.ui.theme.Motion
import com.cinesstar.cinetrack.ui.theme.Sombras
import com.cinesstar.cinetrack.ui.theme.Spacing
import com.cinesstar.cinetrack.ui.theme.TrackingWider

/**
 * Pantalla Listas (design.md §5): CU-12 Ver todas las listas y, desde ella, CU-11 Crear (diálogo "Nueva Lista"),
 * CU-14 Renombrar y CU-19 Eliminar (menú ⋮ de cada tarjeta). Pulsar una tarjeta abre la lista (CU-12 A2 → CU-13).
 */
@Composable
fun ContenidoListas(onLista: (Long) -> Unit) {
    val contenedor = LocalContext.current.contenedor
    val vm: ListasViewModel = viewModel { ListasViewModel(contenedor.listas) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val dialogo by vm.dialogo.collectAsStateWithLifecycle()
    val avisos = LocalAvisos.current
    val textos =
        mapOf(
            EventoListas.Creada to stringResource(R.string.toast_lista_creada),
            EventoListas.Renombrada to stringResource(R.string.toast_lista_renombrada),
            EventoListas.Eliminada to stringResource(R.string.toast_lista_eliminada),
            EventoListas.NoExiste to stringResource(R.string.aviso_lista_no_existe),
            EventoListas.ErrorEscritura to stringResource(R.string.m25_error_inesperado),
        )
    val sinRanking = stringResource(R.string.listas_sin_ranking)
    LaunchedEffect(vm) { vm.eventos.collect { avisos.mostrar(Aviso(textos.getValue(it))) } }

    val arriba = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Dimens.TopBarContentHeight
    val abajo = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Dimens.ContentBottomPadding
    val panel = (estado as? EstadoListas.Datos)?.panel
    Column(modifier = Modifier.fillMaxSize().padding(top = arriba)) {
        Pestanas(panel?.listas?.size ?: 0) {
            val ranking = panel?.listas?.firstOrNull { it.tipo == TipoLista.RANKING }
            if (ranking != null) onLista(ranking.id) else avisos.mostrar(Aviso(sinRanking))
        }
        when (val e = estado) {
            EstadoListas.Cargando -> Unit
            EstadoListas.Error -> ErrorLectura(vm::reintentar)
            is EstadoListas.Datos ->
                MisListas(
                    panel = e.panel,
                    abajo = abajo,
                    acciones = AccionesListas(onLista, vm),
                )
        }
    }
    Dialogos(dialogo, vm)
}

/** Acciones de las tarjetas. */
private class AccionesListas(
    val onLista: (Long) -> Unit,
    val vm: ListasViewModel,
)

@Composable
private fun Dialogos(
    dialogo: DialogoListas?,
    vm: ListasViewModel,
) {
    when (val d = dialogo) {
        null -> Unit
        is DialogoListas.Nueva ->
            DialogoNuevaLista(d, vm::cambiarNombre, vm::elegirTipo, vm::elegirRestriccion, vm::guardarNueva, vm::cerrarDialogo)
        is DialogoListas.Renombrar -> DialogoRenombrarLista(d.nombre, d.error, vm::cambiarNombre, vm::guardarRenombrar, vm::cerrarDialogo)
        is DialogoListas.Eliminar -> DialogoEliminarLista(d.nombre, vm::confirmarEliminar, vm::cerrarDialogo)
    }
}

/** §5.1: "Mis Listas" (activa) y "Top Ranking", que abre el primer ranking (§13). Fijas: la lista se desplaza debajo. */
@Composable
private fun Pestanas(
    total: Int,
    onTopRanking: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = CineAlpha.A95))
                .padding(
                    start = Spacing.Margin,
                    end = Spacing.Margin,
                    top = DimensListas.TabsPaddingTop,
                    bottom = DimensListas.TabsPaddingBottom,
                ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(CineShapes.Xl)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(DimensListas.TabsContainerPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
        ) {
            Pestana(
                texto = stringResource(R.string.listas_mis_listas),
                icono = "collections_bookmark",
                activa = true,
                tag = TagsListas.PESTANA_MIS_LISTAS,
                modifier = Modifier.weight(1f),
                onClick = {},
            ) {
                Text(
                    text = stringResource(R.string.listas_contador, total),
                    style = CineTheme.typography.labelSm,
                    color = CineTheme.colors.slate950,
                    modifier =
                        Modifier
                            .clip(CineShapes.Full)
                            .background(CineTheme.colors.slate950.copy(alpha = CineAlpha.A20))
                            .padding(horizontal = DimensListas.TabContadorPaddingH),
                )
            }
            Pestana(
                texto = stringResource(R.string.listas_top_ranking),
                icono = "hotel_class",
                activa = false,
                tag = TagsListas.PESTANA_TOP,
                modifier = Modifier.weight(1f),
                onClick = onTopRanking,
            ) {
                Box(Modifier.size(DimensListas.TabPunto).clip(CineShapes.Full).background(CineTheme.colors.sky400))
            }
        }
    }
}

@Composable
private fun Pestana(
    texto: String,
    icono: String,
    activa: Boolean,
    tag: String,
    modifier: Modifier,
    onClick: () -> Unit,
    extra: @Composable () -> Unit,
) {
    val color = if (activa) CineTheme.colors.slate950 else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier =
            modifier
                .then(if (activa) Modifier.shadow(Sombras.Md, CineShapes.Lg) else Modifier)
                .clip(CineShapes.Lg)
                .background(if (activa) CineTheme.colors.sky400 else Color.Transparent)
                .selectable(selected = activa, role = Role.Tab, onClick = onClick)
                .padding(horizontal = DimensListas.TabPaddingH, vertical = DimensListas.TabPaddingV)
                .testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(icono, null, DimensListas.TabIcon, color)
        Text(
            text = texto,
            style = CineTheme.typography.labelMd,
            fontWeight = if (activa) FontWeight.SemiBold else null,
            color = color,
            maxLines = 1,
        )
        extra()
    }
}

@Composable
private fun MisListas(
    panel: PanelListas,
    abajo: androidx.compose.ui.unit.Dp,
    acciones: AccionesListas,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(TagsListas.LISTA),
        contentPadding =
            PaddingValues(start = Spacing.Margin, end = Spacing.Margin, top = Spacing.Xs, bottom = abajo + DimensListas.ContentBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.Md),
    ) {
        item(key = "banner") { Banner(panel) }
        if (panel.listas.isEmpty()) {
            item(key = "vacia") { Vacia(acciones.vm::abrirNueva) }
            return@LazyColumn
        }
        item(key = "cabecera") { Cabecera(panel.listas.size) }
        items(panel.listas, key = { it.id }) { lista -> TarjetaLista(lista, acciones) }
        item(key = "nueva") {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { BotonNueva(acciones.vm::abrirNueva) }
        }
    }
}

/** §5.2-1: banner "Colección Personal". */
@Composable
private fun Banner(panel: PanelListas) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(Sombras.Lg, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(DimensListas.BannerBorde, CineTheme.colors.sky400.copy(alpha = CineAlpha.A10), CineShapes.Xl)
                .padding(DimensListas.BannerPadding)
                .testTag(TagsListas.BANNER),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.fillMaxWidth(DimensListas.BANNER_FRACCION_TEXTO)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
                    SymbolIcon("auto_awesome", null, DimensListas.BannerIcon, CineTheme.colors.sky400)
                    Text(
                        text = stringResource(R.string.listas_coleccion).uppercase(LocaleEs),
                        style = CineTheme.typography.labelSm,
                        letterSpacing = TrackingWider,
                        color = CineTheme.colors.sky400,
                    )
                }
                Text(
                    text = stringResource(R.string.listas_banner_titulo),
                    style = CineTheme.typography.headlineMd,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.listas_banner_texto),
                    style = CineTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier =
                    Modifier
                        .size(DimensListas.BannerCaja)
                        .shadow(Sombras.Md, CineShapes.Xl)
                        .clip(CineShapes.Xl)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon("movie_filter", null, DimensListas.BannerCajaIcon, CineTheme.colors.sky400)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = DimensListas.BannerPieTop), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(DimensListas.BannerPunto).clip(CineShapes.Full).background(CineTheme.colors.emerald500))
            Spacer(Modifier.size(Spacing.Sm))
            Text(
                text = pluralStringResource(R.plurals.listas_n_listas, panel.listas.size, panel.listas.size),
                style = CineTheme.typography.labelSm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = pluralStringResource(R.plurals.listas_obras, panel.obrasCatalogadas, panel.obrasCatalogadas),
                style = CineTheme.typography.labelSm,
                fontWeight = FontWeight.SemiBold,
                color = CineTheme.colors.sky300,
            )
        }
    }
}

/** §5.2-2: "Colecciones Activas" y "{x} de {n} mostradas". */
@Composable
private fun Cabecera(total: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.listas_colecciones_activas),
            style = CineTheme.typography.titleMd,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.listas_mostradas, total, total),
            style = CineTheme.typography.labelSm,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TagsListas.MOSTRADAS),
        )
    }
}

/** §5.2-3: tarjeta de lista. */
@Composable
private fun TarjetaLista(
    lista: ResumenLista,
    acciones: AccionesListas,
) {
    val interaccion = androidx.compose.runtime.remember { MutableInteractionSource() }
    val pulsada by interaccion.collectIsPressedAsState()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .scale(if (pulsada) Motion.PRESSED_SCALE_LIST_CARD else 1f)
                .shadow(Sombras.Md, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(if (pulsada) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer)
                .clickable(interactionSource = interaccion, indication = null, role = Role.Button) { acciones.onLista(lista.id) }
                .padding(DimensListas.CardPadding)
                .testTag(TagsListas.tarjeta(lista.id)),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Sm), verticalAlignment = Alignment.Top) {
            val (icono, colorIcono) = iconoLista(lista)
            Box(
                modifier =
                    Modifier
                        .size(DimensListas.CajaIcono)
                        .clip(CineShapes.Lg)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) { SymbolIcon(icono, null, DimensListas.CajaIconoIcon, colorIcono) }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lista.nombre,
                    style = CineTheme.typography.titleSm,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ChipRestriccion(lista)
            }
            InsigniaTipo(lista.tipo, Modifier.testTag(TagsListas.tipoTarjeta(lista.id)))
        }
        PieTarjeta(lista, acciones)
    }
}

/** Icono de la caja: ranking `format_list_numbered`, prioridad `flag`, normal según la restricción (§5.2-3). */
@Composable
private fun iconoLista(lista: ResumenLista): Pair<String, Color> =
    when (lista.tipo) {
        TipoLista.RANKING -> "format_list_numbered" to CineTheme.colors.sky400
        TipoLista.PRIORIDAD -> "flag" to MaterialTheme.colorScheme.secondary
        TipoLista.NORMAL ->
            when (lista.restriccion) {
                Restriccion.SOLO_SERIES -> "tv" to CineTheme.colors.sky300
                Restriccion.SOLO_PELICULAS -> "theaters" to CineTheme.colors.sky400
                Restriccion.MIXTO -> "view_list" to CineTheme.colors.sky400
            }
    }

@Composable
private fun ChipRestriccion(lista: ResumenLista) {
    val texto =
        when (lista.restriccion) {
            Restriccion.MIXTO -> return
            Restriccion.SOLO_PELICULAS -> R.string.listas_restringida_peliculas
            Restriccion.SOLO_SERIES -> R.string.listas_restringida_series
        }
    Text(
        text = stringResource(texto),
        style = CineTheme.typography.labelSm,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier =
            Modifier
                .padding(top = DimensListas.RestriccionTop)
                .clip(CineShapes.Rounded)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = DimensListas.RestriccionPaddingH, vertical = DimensListas.RestriccionPaddingV)
                .testTag(TagsListas.restriccionTarjeta(lista.id)),
    )
}

/** Insignia `[RANKING]` / `[PRIORIDAD]` / `[NORMAL]` (§5.2-3). */
@Composable
private fun InsigniaTipo(
    tipo: TipoLista,
    modifier: Modifier = Modifier,
) {
    val (fondo, texto) =
        when (tipo) {
            TipoLista.RANKING -> CineTheme.colors.sky400.copy(alpha = CineAlpha.A15) to CineTheme.colors.sky300
            TipoLista.PRIORIDAD ->
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = CineAlpha.A20) to MaterialTheme.colorScheme.secondary
            TipoLista.NORMAL -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
        }
    Text(
        text = stringResource(R.string.listas_insignia, stringResource(tipo.etiqueta()).uppercase(LocaleEs)),
        style = CineTheme.typography.labelSm,
        color = texto,
        maxLines = 1,
        modifier =
            modifier
                .clip(CineShapes.Full)
                .background(fondo)
                .padding(horizontal = DimensListas.InsigniaPaddingH, vertical = DimensListas.InsigniaPaddingV),
    )
}

/** Fila inferior: "{n} títulos" (o películas/series), el dato extra del tipo y el menú ⋮. */
@Composable
private fun PieTarjeta(
    lista: ResumenLista,
    acciones: AccionesListas,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = DimensListas.PieTop),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
    ) {
        val colorTexto = MaterialTheme.colorScheme.onSurfaceVariant
        SymbolIcon(if (lista.restriccion == Restriccion.SOLO_SERIES) "live_tv" else "theaters", null, DimensListas.PieIcon, colorTexto)
        Text(
            text = textoTotal(lista.total, lista.restriccion),
            style = CineTheme.typography.labelSm,
            color = colorTexto,
            maxLines = 1,
            modifier = Modifier.testTag(TagsListas.totalTarjeta(lista.id)),
        )
        DatoExtra(lista, Modifier.weight(1f))
        MenuTarjeta(lista, acciones.vm)
    }
}

@Composable
private fun DatoExtra(
    lista: ResumenLista,
    modifier: Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
        val primero = lista.primero
        val mostrar =
            (lista.tipo == TipoLista.RANKING && primero != null) || (lista.tipo == TipoLista.PRIORIDAD && lista.altas > 0)
        if (!mostrar) return@Row
        Box(
            Modifier
                .size(DimensListas.PuntoSeparador)
                .clip(CineShapes.Full)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = CineAlpha.A40)),
        )
        if (lista.tipo == TipoLista.RANKING) {
            Text(
                text = stringResource(R.string.listas_primero, primero.orEmpty()),
                style = CineTheme.typography.labelSm,
                fontWeight = FontWeight.SemiBold,
                color = CineTheme.colors.sky400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = stringResource(R.string.listas_altas, lista.altas),
                style = CineTheme.typography.labelSm,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .clip(CineShapes.Rounded)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = CineAlpha.A30))
                        .padding(horizontal = DimensListas.AltaPaddingH, vertical = DimensListas.AltaPaddingV),
            )
        }
    }
}

/** "{n} títulos", o "{n} películas" / "{n} series" si la lista está restringida (§5.2-3). */
@Composable
fun textoTotal(
    total: Int,
    restriccion: Restriccion,
): String =
    pluralStringResource(
        when (restriccion) {
            Restriccion.MIXTO -> R.plurals.detalle_lista_titulos
            Restriccion.SOLO_PELICULAS -> R.plurals.listas_peliculas
            Restriccion.SOLO_SERIES -> R.plurals.listas_series
        },
        total,
        total,
    )

/** Menú ⋮ de la tarjeta: Renombrar (CU-14) / Eliminar (CU-19). */
@Composable
private fun MenuTarjeta(
    lista: ResumenLista,
    vm: ListasViewModel,
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    Box {
        Box(
            modifier =
                Modifier
                    .size(Dimens.NavItemMin)
                    .clip(CineShapes.Full)
                    .clickable(role = Role.Button, onClick = { abierto = true })
                    .testTag(TagsListas.menu(lista.id)),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(
                "more_vert",
                stringResource(R.string.cd_opciones_lista),
                DimensListas.MasIcon,
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            OpcionMenuLista(R.string.menu_renombrar, "edit", TagsListas.MENU_RENOMBRAR) {
                abierto = false
                vm.abrirRenombrar(lista.id, lista.nombre)
            }
            OpcionMenuLista(R.string.menu_eliminar, "delete", TagsListas.MENU_ELIMINAR) {
                abierto = false
                vm.abrirEliminar(lista.id, lista.nombre)
            }
        }
    }
}

/** Opción de un menú desplegable de listas (`body-md` con icono 20 dp, como el menú del detalle). */
@Composable
fun OpcionMenuLista(
    @StringRes texto: Int,
    icono: String,
    tag: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = stringResource(texto), style = CineTheme.typography.bodyMd, color = MaterialTheme.colorScheme.onSurface) },
        leadingIcon = { SymbolIcon(icono, null, DimensListas.TabIcon, MaterialTheme.colorScheme.onSurfaceVariant) },
        onClick = onClick,
        modifier = Modifier.testTag(tag),
    )
}

/** §5.2-4: "+ Nueva Lista", alineado a la derecha al final (no flota). */
@Composable
private fun BotonNueva(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .shadow(Sombras.Xl, CineShapes.Full)
                .clip(CineShapes.Full)
                .background(CineTheme.colors.sky400)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = DimensListas.NuevaPaddingH, vertical = DimensListas.NuevaPaddingV)
                .testTag(TagsListas.NUEVA),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon("playlist_add", null, DimensListas.NuevaIcon, CineTheme.colors.slate950)
        Text(
            text = stringResource(R.string.listas_nueva),
            style = CineTheme.typography.labelLg,
            color = CineTheme.colors.slate950,
        )
    }
}

/** CU-12 A1: sin listas, estado vacío con "+ Nueva Lista". */
@Composable
private fun Vacia(onNueva: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Lg).testTag(TagsListas.VACIA),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Md),
    ) {
        SymbolIcon("bookmarks", null, DimensListas.VacioIcon, MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(R.string.listas_vacia),
            style = CineTheme.typography.titleSm,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        BotonNueva(onNueva)
    }
}

/** CU-12 E1: M-25 con "Reintentar" (mismo estilo que la Biblioteca). */
@Composable
private fun ErrorLectura(onReintentar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.Margin).testTag(TagsListas.ERROR),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Md, Alignment.CenterVertically),
    ) {
        SymbolIcon("error", null, DimensBiblioteca.EmptyIcon, MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(R.string.m25_error_inesperado),
            style = CineTheme.typography.titleSm,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier =
                Modifier
                    .clip(CineShapes.Full)
                    .background(CineTheme.colors.sky400)
                    .clickable(role = Role.Button, onClick = onReintentar)
                    .padding(horizontal = DimensBiblioteca.EmptyButtonPaddingH, vertical = DimensBiblioteca.EmptyButtonPaddingV)
                    .testTag(TagsListas.REINTENTAR),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon("refresh", null, Dimens.NavIcon, CineTheme.colors.slate950)
            Text(
                text = stringResource(R.string.biblioteca_reintentar),
                style = CineTheme.typography.labelLg,
                color = CineTheme.colors.slate950,
            )
        }
    }
}
