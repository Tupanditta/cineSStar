package com.cinesstar.cinetrack.ui.biblioteca

import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.contenedor
import com.cinesstar.cinetrack.domain.Agrupador
import com.cinesstar.cinetrack.domain.Ambito
import com.cinesstar.cinetrack.domain.ClaveGrupo
import com.cinesstar.cinetrack.domain.CriterioAgrupacion
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.PerfilBiblioteca
import com.cinesstar.cinetrack.domain.RangoPuntuacion
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.ui.components.SymbolIcon
import com.cinesstar.cinetrack.ui.components.etiqueta
import com.cinesstar.cinetrack.ui.components.icono
import com.cinesstar.cinetrack.ui.theme.CineAlpha
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.Dimens
import com.cinesstar.cinetrack.ui.theme.DimensBiblioteca
import com.cinesstar.cinetrack.ui.theme.LeadingTight
import com.cinesstar.cinetrack.ui.theme.Sombras
import com.cinesstar.cinetrack.ui.theme.Spacing
import com.cinesstar.cinetrack.ui.theme.TrackingTight
import com.cinesstar.cinetrack.ui.theme.TrackingWider

/** Acciones de la Biblioteca. */
private class AccionesBiblioteca(
    val vm: BibliotecaViewModel,
    val onPerfil: (Long) -> Unit,
)

/**
 * Pantalla Biblioteca (design.md §4): CU-08 Ver biblioteca (contadores, filtros por tipo, estado y escala, tarjetas
 * de medio con scroll, vacío y error) y CU-09 Agrupar (§4.2). Todo el contenido se desplaza bajo las barras.
 */
@Composable
fun ContenidoBiblioteca(
    onPerfil: (Long) -> Unit,
    onAnadir: () -> Unit,
) {
    val contenedor = LocalContext.current.contenedor
    val vm: BibliotecaViewModel = viewModel { BibliotecaViewModel(contenedor.biblioteca) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val arriba = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Dimens.TopBarContentHeight
    val abajo = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Dimens.ContentBottomPadding
    val acciones = remember(vm, onPerfil) { AccionesBiblioteca(vm, onPerfil) }
    when (val e = estado) {
        EstadoBiblioteca.Cargando -> Unit
        EstadoBiblioteca.Vacia ->
            Aviso(
                icono = "video_library",
                texto = R.string.m22_biblioteca_vacia,
                boton = R.string.biblioteca_anadir,
                iconoBoton = "add",
                tag = TagsBiblioteca.VACIA,
                tagBoton = TagsBiblioteca.ANADIR,
                onBoton = onAnadir,
                modifier = Modifier.padding(top = arriba, bottom = abajo),
            )
        EstadoBiblioteca.Error ->
            Aviso(
                icono = "error",
                texto = R.string.m25_error_inesperado,
                boton = R.string.biblioteca_reintentar,
                iconoBoton = "refresh",
                tag = TagsBiblioteca.ERROR,
                tagBoton = TagsBiblioteca.REINTENTAR,
                onBoton = vm::reintentar,
                modifier = Modifier.padding(top = arriba, bottom = abajo),
            )
        is EstadoBiblioteca.Lista ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag(TagsBiblioteca.LISTA),
                contentPadding = PaddingValues(top = arriba + Spacing.Sm, bottom = abajo),
            ) {
                cabecera(e.datos, acciones)
                catalogo(e.datos, acciones)
            }
    }
}

private fun LazyListScope.cabecera(
    datos: DatosBiblioteca,
    acciones: AccionesBiblioteca,
) {
    item(key = "tipos") { ChipsTipo(datos, acciones.vm::elegirAmbito) }
    item(key = "contadores") { Contadores(datos, acciones.vm::alternarEstado) }
    item(key = "escala") { Escala(datos.filtros.rango, acciones.vm::alternarRango) }
    item(key = "agrupar") { Agrupar(datos.filtros.criterio, acciones.vm::agrupar) }
    item(key = "catalogo") { CabeceraCatalogo(datos.perfiles.size, datos.total) }
}

private fun LazyListScope.catalogo(
    datos: DatosBiblioteca,
    acciones: AccionesBiblioteca,
) {
    if (datos.perfiles.isEmpty()) {
        item(key = "sin_resultados") { SinResultados() }
        return
    }
    val grupos = datos.grupos
    if (grupos == null) {
        items(datos.perfiles, key = { it.id }) { perfil ->
            TarjetaMedio(perfil, TagsBiblioteca.fila(perfil.id)) { acciones.onPerfil(perfil.id) }
        }
        return
    }
    grupos.forEach { grupo ->
        val plegado = grupo.clave in datos.filtros.plegados
        val tagGrupo = TagsBiblioteca.grupo(grupo.clave)
        item(key = tagGrupo) { CabeceraGrupo(grupo.clave, grupo.total, plegado) { acciones.vm.alternarGrupo(grupo.clave) } }
        if (!plegado) {
            items(grupo.perfiles, key = { TagsBiblioteca.filaEnGrupo(grupo.clave, it.id) }) { perfil ->
                TarjetaMedio(perfil, TagsBiblioteca.filaEnGrupo(grupo.clave, perfil.id)) { acciones.onPerfil(perfil.id) }
            }
        }
    }
}

/** §4-2: chips de tipo con su recuento (CU-08 A1, ámbito de CU-09). */
@Composable
private fun ChipsTipo(
    datos: DatosBiblioteca,
    onElegir: (Ambito) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = Spacing.Margin, end = Spacing.Margin, bottom = Spacing.Sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
    ) {
        Ambito.entries.forEach { ambito ->
            val (texto, icono) =
                when (ambito) {
                    Ambito.TODO -> stringResource(R.string.biblioteca_todas, datos.total) to "auto_awesome"
                    Ambito.PELICULAS ->
                        stringResource(R.string.biblioteca_solo_peliculas, datos.porTipo[TipoPerfil.PELICULA] ?: 0) to "movie"
                    Ambito.SERIES -> stringResource(R.string.biblioteca_solo_series, datos.porTipo[TipoPerfil.SERIE] ?: 0) to "tv"
                }
            val activo = datos.filtros.ambito == ambito
            val colorTexto = if (activo) CineTheme.colors.slate950 else MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                modifier =
                    Modifier
                        .then(if (activo) Modifier.shadow(Sombras.Sm, CineShapes.Full) else Modifier)
                        .clip(CineShapes.Full)
                        .background(if (activo) CineTheme.colors.sky400 else MaterialTheme.colorScheme.surfaceContainer)
                        .selectable(selected = activo, role = Role.Tab, onClick = { onElegir(ambito) })
                        .padding(horizontal = DimensBiblioteca.TipoPaddingH, vertical = DimensBiblioteca.TipoPaddingV)
                        .testTag(TagsBiblioteca.ambito(ambito)),
                horizontalArrangement = Arrangement.spacedBy(DimensBiblioteca.TipoGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(icono, null, DimensBiblioteca.TipoIcon, colorTexto)
                Text(
                    text = texto,
                    style = CineTheme.typography.labelMd,
                    fontWeight = if (activo) FontWeight.SemiBold else null,
                    color = colorTexto,
                    maxLines = 1,
                )
            }
        }
    }
}

/** §4-3: contadores Viendo / Pendientes / Vistas; pulsar uno filtra por ese estado. */
@Composable
private fun Contadores(
    datos: DatosBiblioteca,
    onElegir: (Estado) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = Spacing.Margin, end = Spacing.Margin, bottom = Spacing.Md)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(DimensBiblioteca.ContadoresPadding),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
    ) {
        listOf(
            Triple(Estado.VIENDO, R.string.biblioteca_contador_viendo, "play_circle"),
            Triple(Estado.PENDIENTE, R.string.biblioteca_contador_pendientes, "schedule"),
            Triple(Estado.VISTO, R.string.biblioteca_contador_vistas, "check_circle"),
        ).forEach { (estado, etiqueta, icono) ->
            val color =
                when (estado) {
                    Estado.VIENDO -> CineTheme.colors.sky400
                    Estado.PENDIENTE -> MaterialTheme.colorScheme.secondary
                    Estado.VISTO -> MaterialTheme.colorScheme.onSurface
                }
            val activo = datos.filtros.estado == estado
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(CineShapes.Lg)
                        .background(
                            if (activo) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                        ).selectable(selected = activo, role = Role.Tab, onClick = { onElegir(estado) })
                        .padding(DimensBiblioteca.ContadorPadding)
                        .testTag(TagsBiblioteca.contador(estado)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
                    SymbolIcon(icono, null, DimensBiblioteca.ContadorIcon, color, filled = estado == Estado.VIENDO)
                    Text(
                        text = (datos.porEstado[estado] ?: 0).toString(),
                        style = CineTheme.typography.headlineMd,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                }
                Text(
                    text = stringResource(etiqueta).uppercase(),
                    style = CineTheme.typography.etiquetaContador,
                    letterSpacing = TrackingWider,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** §4-4: filtro por escala (CU-08 A2). */
@Composable
private fun Escala(
    seleccionado: RangoPuntuacion?,
    onElegir: (RangoPuntuacion) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Md)) {
        CabeceraFiltro("star", R.string.biblioteca_escala, stringResource(R.string.biblioteca_escala_rangos))
        FilaChips {
            Agrupador.ORDEN_RANGOS.forEach { rango ->
                ChipFiltro(
                    texto = stringResource(rango.textoChip()),
                    activo = seleccionado == rango,
                    tag = TagsBiblioteca.rango(rango),
                    onClick = { onElegir(rango) },
                )
            }
        }
    }
}

/** §4.2: "Agrupar: Ninguno · Puntuación · Saga · Idioma · Género · Estado" con el estilo de los chips de escala. */
@Composable
private fun Agrupar(
    seleccionado: CriterioAgrupacion?,
    onElegir: (CriterioAgrupacion?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Md).testTag(TagsBiblioteca.AGRUPAR)) {
        CabeceraFiltro("category", R.string.biblioteca_agrupar, null)
        FilaChips {
            (listOf(null) + CriterioAgrupacion.entries).forEach { criterio ->
                ChipFiltro(
                    texto = stringResource(criterio.etiqueta()),
                    activo = seleccionado == criterio,
                    tag = TagsBiblioteca.criterio(criterio),
                    onClick = { onElegir(criterio) },
                )
            }
        }
    }
}

@Composable
private fun CabeceraFiltro(
    icono: String,
    @StringRes titulo: Int,
    detalle: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Margin).padding(bottom = Spacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(icono, null, DimensBiblioteca.EscalaHeaderIcon, CineTheme.colors.sky400)
        Spacer(Modifier.size(DimensBiblioteca.EscalaHeaderGap))
        Text(
            text = stringResource(titulo),
            style = CineTheme.typography.titleSm,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (detalle != null) {
            Text(text = detalle, style = CineTheme.typography.labelSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FilaChips(content: @Composable () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.Margin, vertical = Spacing.Xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun ChipFiltro(
    texto: String,
    activo: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    val colores = CineTheme.colors
    Text(
        text = texto,
        style = CineTheme.typography.labelSm,
        color = if (activo) colores.sky300 else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier =
            Modifier
                .then(if (activo) Modifier.shadow(Sombras.Sm, CineShapes.Lg) else Modifier)
                .clip(CineShapes.Lg)
                .background(if (activo) colores.sky950.copy(alpha = CineAlpha.A60) else MaterialTheme.colorScheme.surfaceContainer)
                .then(
                    if (activo) {
                        Modifier.border(DimensBiblioteca.EscalaChipBorde, colores.sky500.copy(alpha = CineAlpha.A30), CineShapes.Lg)
                    } else {
                        Modifier
                    },
                ).selectable(selected = activo, role = Role.Tab, onClick = onClick)
                .padding(horizontal = DimensBiblioteca.EscalaChipPaddingH, vertical = DimensBiblioteca.EscalaChipPaddingV)
                .testTag(tag),
    )
}

/** §4-5: cabecera "Catálogo de Medios" con "Mostrando {x} de {N}". */
@Composable
private fun CabeceraCatalogo(
    mostrados: Int,
    total: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.Margin, end = Spacing.Margin, bottom = Spacing.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.biblioteca_catalogo),
            style = CineTheme.typography.titleMd,
            fontWeight = FontWeight.Bold,
            letterSpacing = TrackingTight,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        PildoraRecuento(stringResource(R.string.biblioteca_mostrando, mostrados, total), Modifier.testTag(TagsBiblioteca.MOSTRANDO))
    }
}

@Composable
private fun PildoraRecuento(
    texto: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = texto,
        style = CineTheme.typography.labelSm,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier =
            modifier
                .clip(CineShapes.Full)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = DimensBiblioteca.MostrandoPaddingH, vertical = DimensBiblioteca.MostrandoPaddingV),
    )
}

/** §4.2: cabecera de grupo `title-md` bold + recuento; pulsarla pliega o despliega el grupo (CU-09 A1). */
@Composable
private fun CabeceraGrupo(
    clave: ClaveGrupo,
    total: Int,
    plegado: Boolean,
    onClick: () -> Unit,
) {
    val cd = stringResource(if (plegado) R.string.cd_desplegar_grupo else R.string.cd_plegar_grupo)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Margin)
                .padding(bottom = Spacing.Sm)
                .clip(CineShapes.Lg)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { stateDescription = cd }
                .padding(vertical = Spacing.Xs)
                .testTag(TagsBiblioteca.grupo(clave)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
    ) {
        Text(
            text = nombreGrupo(clave),
            style = CineTheme.typography.titleMd,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        PildoraRecuento(total.toString(), Modifier.testTag(TagsBiblioteca.totalGrupo(clave)))
        Spacer(Modifier.weight(1f))
        SymbolIcon(
            if (plegado) "expand_more" else "expand_less",
            cd,
            DimensBiblioteca.GrupoIcon,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun nombreGrupo(clave: ClaveGrupo): String =
    when (clave) {
        is ClaveGrupo.Rango -> stringResource(clave.rango.textoGrupo())
        is ClaveGrupo.Saga -> clave.nombre
        is ClaveGrupo.Genero -> clave.nombre
        is ClaveGrupo.Idioma -> clave.nombre
        is ClaveGrupo.DeEstado -> stringResource(clave.estado.etiqueta())
        ClaveGrupo.SinSaga -> stringResource(R.string.grupo_sin_saga)
        ClaveGrupo.SinGenero -> stringResource(R.string.grupo_sin_genero)
        ClaveGrupo.SinIdioma -> stringResource(R.string.grupo_sin_idioma)
    }

/** §4.1: tarjeta de medio. */
@Composable
private fun TarjetaMedio(
    perfil: PerfilBiblioteca,
    tag: String,
    onClick: () -> Unit,
) {
    val interaccion = remember { MutableInteractionSource() }
    val pulsada by interaccion.collectIsPressedAsState()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = Spacing.Margin, end = Spacing.Margin, bottom = Spacing.Md)
                .shadow(Sombras.Sm, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(if (pulsada) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable(interactionSource = interaccion, indication = null, role = Role.Button, onClick = onClick)
                .padding(DimensBiblioteca.CardPadding)
                .testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
    ) {
        IconoTipo(perfil.tipo)
        Column(
            modifier = Modifier.weight(1f).height(DimensBiblioteca.CardContentHeight).padding(vertical = DimensBiblioteca.ColumnaPaddingV),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            DatosTarjeta(perfil)
            FilaInferior(perfil)
        }
    }
}

/** Icono del tipo (sustituye al póster, design.md §4.1): `movie` para películas y `tv` para series, con su nombre debajo. */
@Composable
private fun IconoTipo(tipo: TipoPerfil) {
    val nombre = stringResource(tipo.etiqueta())
    Column(
        modifier = Modifier.width(DimensBiblioteca.IconoTipoColumna),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Xs),
    ) {
        Box(
            modifier =
                Modifier
                    .size(DimensBiblioteca.IconoTipoCaja)
                    .clip(CineShapes.Lg)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(tipo.icono(), nombre, DimensBiblioteca.IconoTipo, CineTheme.colors.sky400)
        }
        Text(
            text = nombre,
            style = CineTheme.typography.labelSm,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DatosTarjeta(perfil: PerfilBiblioteca) {
    Column {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
            Text(
                text = perfil.nombre,
                style = CineTheme.typography.tituloTarjeta,
                lineHeight = LeadingTight,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ChipNota(perfil)
        }
        val saga = perfil.saga
        Text(
            text = if (saga != null) stringResource(R.string.biblioteca_saga, saga.nombre) else stringResource(R.string.detalle_sin_saga),
            style = CineTheme.typography.labelSm,
            fontWeight = FontWeight.Medium,
            color = if (saga != null) CineTheme.colors.sky300 else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = DimensBiblioteca.SagaTop),
        )
        if (perfil.generos.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = DimensBiblioteca.GenerosTop),
                horizontalArrangement = Arrangement.spacedBy(DimensBiblioteca.GeneroGap),
                verticalArrangement = Arrangement.spacedBy(DimensBiblioteca.GeneroGap),
            ) {
                perfil.generos.forEach { genero ->
                    Text(
                        text = genero.nombre,
                        style = CineTheme.typography.labelSm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier =
                            Modifier
                                .clip(CineShapes.Rounded)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = DimensBiblioteca.GeneroPaddingH, vertical = DimensBiblioteca.GeneroPaddingV),
                    )
                }
            }
        }
    }
}

/** Chip de nota: ★ + valor, o "Sin puntuar". */
@Composable
private fun ChipNota(perfil: PerfilBiblioteca) {
    val puntuacion = perfil.puntuacion
    if (puntuacion == null) {
        PildoraRecuento(stringResource(R.string.sin_puntuar))
        return
    }
    Row(
        modifier =
            Modifier
                .clip(CineShapes.Full)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = DimensBiblioteca.NotaPaddingH, vertical = DimensBiblioteca.NotaPaddingV),
        horizontalArrangement = Arrangement.spacedBy(DimensBiblioteca.NotaGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon("star", null, DimensBiblioteca.CardIcon, CineTheme.colors.sky400, filled = true)
        Text(
            text = puntuacion.formatear(),
            style = CineTheme.typography.labelSm,
            fontWeight = FontWeight.Bold,
            color = CineTheme.colors.sky200,
        )
    }
}

/** Idioma (el primero y "+n") a la izquierda e insignia de estado a la derecha. */
@Composable
private fun FilaInferior(perfil: PerfilBiblioteca) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = DimensBiblioteca.FilaInferiorTop),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val idiomas = perfil.idiomas
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
        ) {
            if (idiomas.isNotEmpty()) {
                SymbolIcon("translate", null, DimensBiblioteca.CardIcon, MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text =
                        if (idiomas.size == 1) {
                            idiomas.first().nombre
                        } else {
                            stringResource(R.string.biblioteca_mas_idiomas, idiomas.first().nombre, idiomas.size - 1)
                        },
                    style = CineTheme.typography.labelSm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        InsigniaEstado(perfil.estado, Modifier.testTag(TagsBiblioteca.insigniaEstado(perfil.id)))
    }
}

/** Insignia de estado de las tarjetas (design.md §2.3): siempre icono o punto + texto. */
@Composable
private fun InsigniaEstado(
    estado: Estado,
    modifier: Modifier = Modifier,
) {
    val estilo =
        when (estado) {
            Estado.VIENDO -> CineTheme.colors.estadoViendo
            Estado.VISTO -> CineTheme.colors.estadoVisto
            Estado.PENDIENTE -> CineTheme.colors.estadoPendiente
        }
    Row(
        modifier =
            modifier
                .clip(CineShapes.Full)
                .background(estilo.insignia.fondo)
                .padding(horizontal = DimensBiblioteca.InsigniaPaddingH, vertical = DimensBiblioteca.InsigniaPaddingV),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (estado) {
            Estado.VIENDO -> PuntoPulso(DimensBiblioteca.EstadoPunto, estilo.icono)
            Estado.VISTO -> SymbolIcon("check", null, DimensBiblioteca.EstadoIcon, estilo.icono)
            Estado.PENDIENTE -> SymbolIcon("hourglass_empty", null, DimensBiblioteca.EstadoIcon, estilo.icono)
        }
        Text(
            text = stringResource(estado.etiqueta()),
            style = CineTheme.typography.labelSm,
            fontWeight = if (estado == Estado.VIENDO) FontWeight.SemiBold else FontWeight.Medium,
            color = estilo.insignia.texto,
        )
    }
}

/** M-13 cuando los filtros no dejan ningún perfil. */
@Composable
private fun SinResultados() {
    Text(
        text = stringResource(R.string.m13_sin_resultados),
        style = CineTheme.typography.bodyMd,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Spacing.Margin,
                    vertical = Spacing.Lg,
                ).testTag(TagsBiblioteca.SIN_RESULTADOS),
    )
}

/** Vacío (design.md §11, M-22 y "Añadir") o error de lectura (M-25 y "Reintentar"). */
@Composable
private fun Aviso(
    icono: String,
    @StringRes texto: Int,
    @StringRes boton: Int,
    iconoBoton: String,
    tag: String,
    tagBoton: String,
    onBoton: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.Margin).testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Md, Alignment.CenterVertically),
    ) {
        SymbolIcon(icono, null, DimensBiblioteca.EmptyIcon, MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(texto),
            style = CineTheme.typography.titleSm,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier =
                Modifier
                    .clip(CineShapes.Full)
                    .background(CineTheme.colors.sky400)
                    .clickable(role = Role.Button, onClick = onBoton)
                    .padding(horizontal = DimensBiblioteca.EmptyButtonPaddingH, vertical = DimensBiblioteca.EmptyButtonPaddingV)
                    .testTag(tagBoton),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon(iconoBoton, null, Dimens.NavIcon, CineTheme.colors.slate950)
            Text(text = stringResource(boton), style = CineTheme.typography.labelLg, color = CineTheme.colors.slate950)
        }
    }
}

/** Punto con pulso (`animate-pulse`, §2.3). */
@Composable
private fun PuntoPulso(
    tamano: Dp,
    color: Color,
) {
    val transicion = rememberInfiniteTransition()
    val alfa by transicion.animateFloat(
        initialValue = 1f,
        targetValue = CineAlpha.A50,
        animationSpec = infiniteRepeatable(tween(PULSO_MS), RepeatMode.Reverse),
    )
    Box(
        Modifier
            .size(tamano)
            .graphicsLayer { alpha = alfa }
            .clip(CineShapes.Full)
            .background(color),
    )
}

private const val PULSO_MS = 1_000

@StringRes
private fun RangoPuntuacion.textoChip(): Int =
    when (this) {
        RangoPuntuacion.DE_9_A_10 -> R.string.biblioteca_escala_9_10
        RangoPuntuacion.DE_7_A_8_9 -> R.string.biblioteca_escala_7_8
        RangoPuntuacion.DE_5_A_6_9 -> R.string.biblioteca_escala_5_6
        RangoPuntuacion.DE_3_A_4_9 -> R.string.biblioteca_escala_3_4
        RangoPuntuacion.DE_0_A_2_9 -> R.string.biblioteca_escala_0_2
        RangoPuntuacion.SIN_PUNTUACION -> R.string.sin_puntuar
    }

@StringRes
private fun RangoPuntuacion.textoGrupo(): Int =
    when (this) {
        RangoPuntuacion.DE_9_A_10 -> R.string.grupo_9_10
        RangoPuntuacion.DE_7_A_8_9 -> R.string.grupo_7_8
        RangoPuntuacion.DE_5_A_6_9 -> R.string.grupo_5_6
        RangoPuntuacion.DE_3_A_4_9 -> R.string.grupo_3_4
        RangoPuntuacion.DE_0_A_2_9 -> R.string.grupo_0_2
        RangoPuntuacion.SIN_PUNTUACION -> R.string.grupo_sin_puntuacion
    }

@StringRes
private fun CriterioAgrupacion?.etiqueta(): Int =
    when (this) {
        null -> R.string.biblioteca_agrupar_ninguno
        CriterioAgrupacion.PUNTUACION -> R.string.biblioteca_agrupar_puntuacion
        CriterioAgrupacion.SAGA -> R.string.biblioteca_agrupar_saga
        CriterioAgrupacion.IDIOMA -> R.string.biblioteca_agrupar_idioma
        CriterioAgrupacion.GENERO -> R.string.biblioteca_agrupar_genero
        CriterioAgrupacion.ESTADO -> R.string.biblioteca_agrupar_estado
    }
