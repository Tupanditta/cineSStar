package com.cinesstar.cinetrack.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.contenedor
import com.cinesstar.cinetrack.data.repository.FichaPerfil
import com.cinesstar.cinetrack.data.room.PertenenciaLista
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.ui.components.Aviso
import com.cinesstar.cinetrack.ui.components.EtiquetaSeccion
import com.cinesstar.cinetrack.ui.components.LocalAvisos
import com.cinesstar.cinetrack.ui.components.LocaleEs
import com.cinesstar.cinetrack.ui.components.PantallaSecundaria
import com.cinesstar.cinetrack.ui.components.SymbolIcon
import com.cinesstar.cinetrack.ui.components.etiqueta
import com.cinesstar.cinetrack.ui.components.icono
import com.cinesstar.cinetrack.ui.components.iconoDetalle
import com.cinesstar.cinetrack.ui.components.iconoGenero
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

/** Pantalla "Detalle del perfil" (CU-07, design.md §7). En el S2 es de solo lectura; la edición llega en el S3. */
@Composable
fun PantallaDetallePerfil(
    perfilId: Long,
    onBack: () -> Unit,
    onAvatar: () -> Unit,
) {
    val contenedor = LocalContext.current.contenedor
    val vm: DetallePerfilViewModel = viewModel(key = "detalle_$perfilId") { DetallePerfilViewModel(perfilId, contenedor.perfiles) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val avisos = LocalAvisos.current
    val textoNoExiste = stringResource(R.string.aviso_perfil_no_existe)

    // CU-07 E1: el perfil ya no existe → vuelve atrás con un aviso.
    LaunchedEffect(estado) {
        if (estado == EstadoDetalle.NoExiste) {
            avisos.mostrar(Aviso(textoNoExiste))
            onBack()
        }
    }

    val ficha = (estado as? EstadoDetalle.Listo)?.ficha
    val tipo = ficha?.detalle?.perfil?.tipo ?: TipoPerfil.PELICULA
    PantallaSecundaria(
        titulo = stringResource(if (tipo == TipoPerfil.PELICULA) R.string.titulo_detalle_pelicula else R.string.titulo_detalle_serie),
        tag = TagsDetalle.PANTALLA,
        onBack = onBack,
        onAvatarClick = onAvatar,
    ) {
        if (ficha != null) Contenido(ficha)
    }
}

@Composable
private fun Contenido(ficha: FichaPerfil) {
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
        Hero(ficha)
        SelectorEstado(ficha.detalle.perfil.estado)
        TarjetaNota(ficha)
        FichaTecnica(ficha)
        EnTusListas(ficha.listas)
        Critica(ficha.detalle.perfil.comentario, ficha.detalle.perfil.comentarioEditadoEn)
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
private fun Hero(ficha: FichaPerfil) {
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

/** RF-22: estado del perfil (design.md §7.2). En el S2 solo lo muestra; cambiarlo es CU-05 (S3). */
@Composable
private fun SelectorEstado(actual: Estado) {
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
                            .semantics(mergeDescendants = true) {
                                selected = activo
                                role = Role.Tab
                            }.padding(vertical = DimensDetalle.EstadoPaddingV, horizontal = DimensDetalle.EstadoPaddingH)
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
private fun TarjetaNota(ficha: FichaPerfil) {
    val nota = ficha.detalle.puntuacion
    Tarjeta {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Md), verticalAlignment = Alignment.CenterVertically) {
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

/** design.md §7.6. Editar y borrar el comentario llegan en el S3 (CU-04). */
@Composable
private fun Critica(
    comentario: String?,
    editadoEn: Long?,
) {
    Tarjeta(separacion = Spacing.Sm) {
        CabeceraTarjeta("rate_review", stringResource(R.string.detalle_critica))
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

private const val SEPARADOR = " • "

private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", LocaleEs)

/** "14 de marzo, 2024" (design.md §7.6). */
private fun formatearFecha(milis: Long): String = FORMATO_FECHA.format(Instant.ofEpochMilli(milis).atZone(ZoneId.systemDefault()))
