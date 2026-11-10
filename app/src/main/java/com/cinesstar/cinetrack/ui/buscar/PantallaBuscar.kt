package com.cinesstar.cinetrack.ui.buscar

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.contenedor
import com.cinesstar.cinetrack.domain.Ambito
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Etiqueta
import com.cinesstar.cinetrack.domain.PerfilBiblioteca
import com.cinesstar.cinetrack.ui.components.CineRangeSlider
import com.cinesstar.cinetrack.ui.components.EtiquetaSeccion
import com.cinesstar.cinetrack.ui.components.SymbolIcon
import com.cinesstar.cinetrack.ui.components.TextoAjustable
import com.cinesstar.cinetrack.ui.components.etiqueta
import com.cinesstar.cinetrack.ui.components.icono
import com.cinesstar.cinetrack.ui.components.textoError
import com.cinesstar.cinetrack.ui.datos.SeguirAviso
import com.cinesstar.cinetrack.ui.datos.datosLocales
import com.cinesstar.cinetrack.ui.datos.rememberPanelDatos
import com.cinesstar.cinetrack.ui.theme.CineAlpha
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.Dimens
import com.cinesstar.cinetrack.ui.theme.DimensBuscar
import com.cinesstar.cinetrack.ui.theme.Sombras
import com.cinesstar.cinetrack.ui.theme.Spacing
import kotlinx.coroutines.launch

/** Pestañas de Buscar (design.md §9.1): "Filtros Cine" (CU-20) y "Datos Locales" (copia de seguridad, §10). */
private enum class PestanaBuscar { FILTROS, DATOS }

/** Acciones de la pantalla. */
private class AccionesBuscar(
    val vm: BuscarViewModel,
    val onPerfil: (Long) -> Unit,
    val onAplicar: () -> Unit,
)

/** Clave del primer elemento de resultados (cabecera "Resultados" o aviso de error). */
private const val CLAVE_RESULTADOS = "resultados"

/**
 * Pantalla Buscar (design.md §9), pestaña "Filtros Cine": CU-20 Buscar con filtros. Los resultados se actualizan al
 * cambiar cualquier criterio; "Aplicar Filtros ({n})" cierra el teclado y desplaza la pantalla hasta ellos.
 */
@Composable
fun ContenidoBuscar(onPerfil: (Long) -> Unit) {
    val contenedor = LocalContext.current.contenedor
    val vm: BuscarViewModel = viewModel { BuscarViewModel(contenedor.busqueda, contenedor.catalogo) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    var pestana by rememberSaveable { mutableStateOf(PestanaBuscar.FILTROS) }
    val arriba = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Dimens.TopBarContentHeight
    val abajo = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Dimens.ContentBottomPadding
    val lista = rememberLazyListState()
    val alcance = rememberCoroutineScope()
    val foco = LocalFocusManager.current
    val acciones =
        remember(vm, onPerfil) {
            AccionesBuscar(vm, onPerfil) {
                foco.clearFocus()
                alcance.launch { lista.irAResultados() }
            }
        }
    val datos = if (pestana == PestanaBuscar.DATOS) rememberPanelDatos() else null
    datos?.let { SeguirAviso(it, lista, primero = 1) }
    LazyColumn(
        state = lista,
        modifier = Modifier.fillMaxSize().testTag(TagsBuscar.LISTA),
        contentPadding = PaddingValues(top = arriba, bottom = abajo),
    ) {
        item(key = "pestanas") { Pestanas(pestana) { pestana = it } }
        if (pestana == PestanaBuscar.FILTROS) {
            filtros(estado, acciones)
            resultados(estado.resultados, acciones)
        } else {
            datos?.let { datosLocales(it) }
        }
    }
}

/** Lleva la cabecera de resultados (o el aviso de criterios no válidos) a lo alto de la pantalla. */
private suspend fun LazyListState.irAResultados() {
    if (layoutInfo.totalItemsCount > ELEMENTOS_FILTROS) animateScrollToItem(ELEMENTOS_FILTROS)
}

/** Elementos de la lista antes de los resultados: pestañas + 7 bloques de filtros (§9.2-1 a §9.2-7). */
private const val ELEMENTOS_FILTROS = 8

private fun LazyListScope.filtros(
    estado: EstadoBuscar,
    acciones: AccionesBuscar,
) {
    val f = estado.formulario
    val vm = acciones.vm
    val total = (estado.resultados as? Resultados.Lista)?.perfiles?.size
    item(key = "buscador") { Buscador(f.nombre, total, vm::escribirNombre, acciones.onAplicar) }
    item(key = "tipo") { Tipo(f.ambito, vm::elegirAmbito) }
    item(key = "rango") { Rango(f, vm) }
    item(key = "generos") { Generos(estado.opciones.generos, f.generoIds, vm::alternarGenero, vm::quitarGeneros) }
    item(key = "idioma_saga") { IdiomaYSaga(estado, vm) }
    item(key = "estado") { Estados(f.estados, vm::alternarEstado) }
    item(key = "acciones") { Acciones(total, vm::limpiar, acciones.onAplicar) }
}

private fun LazyListScope.resultados(
    resultados: Resultados,
    acciones: AccionesBuscar,
) {
    when (resultados) {
        Resultados.Cargando -> Unit
        is Resultados.Invalidos -> item(key = CLAVE_RESULTADOS) { ErrorCriterios(textoError(resultados.error)) }
        Resultados.Fallo -> item(key = CLAVE_RESULTADOS) { ErrorCriterios(stringResource(R.string.m25_error_inesperado)) }
        is Resultados.Lista -> {
            item(key = CLAVE_RESULTADOS) { CabeceraResultados(resultados.perfiles.size) }
            if (resultados.perfiles.isEmpty()) {
                item(key = "sin_resultados") { SinResultados() }
            } else {
                items(resultados.perfiles, key = { it.id }) { perfil -> TarjetaResultado(perfil) { acciones.onPerfil(perfil.id) } }
            }
        }
    }
}

/** Bloque de filtros: padding lateral 16 y separación 16 (design.md §9.2). */
private val bloque = Modifier.fillMaxWidth().padding(start = Spacing.Margin, end = Spacing.Margin, bottom = Spacing.Md)

/** §9.1: píldora con "Filtros Cine" y "Datos Locales". */
@Composable
private fun Pestanas(
    actual: PestanaBuscar,
    onElegir: (PestanaBuscar) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Margin, vertical = DimensBuscar.TabsPaddingV)
                .clip(CineShapes.Full)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(DimensBuscar.TabsContainerPadding),
    ) {
        listOf(
            Triple(PestanaBuscar.FILTROS, R.string.buscar_tab_filtros, "filter_alt") to TagsBuscar.TAB_FILTROS,
            Triple(PestanaBuscar.DATOS, R.string.buscar_tab_datos, "storage") to TagsBuscar.TAB_DATOS,
        ).forEach { (datos, tag) ->
            val (pestana, texto, icono) = datos
            val activa = pestana == actual
            val color = if (activa) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .then(if (activa) Modifier.shadow(Sombras.Sm, CineShapes.Full) else Modifier)
                        .clip(CineShapes.Full)
                        .background(if (activa) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                        .selectable(selected = activa, role = Role.Tab, onClick = { onElegir(pestana) })
                        .padding(vertical = DimensBuscar.TabPaddingV)
                        .testTag(tag),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Sm, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(icono, null, DimensBuscar.TabIcon, color)
                TextoAjustable(stringResource(texto), CineTheme.typography.labelMd, color)
            }
        }
    }
}

/** §9.2-1: buscador por nombre con "{n} coincidencias". */
@Composable
private fun Buscador(
    nombre: String,
    total: Int?,
    onCambio: (String) -> Unit,
    onBuscar: () -> Unit,
) {
    Column(modifier = bloque) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(Sombras.Md, CineShapes.Full)
                    .clip(CineShapes.Full)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = DimensBuscar.BuscadorPaddingH, vertical = DimensBuscar.BuscadorPaddingV),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        ) {
            SymbolIcon("search", null, DimensBuscar.BuscadorIcon, MaterialTheme.colorScheme.primary)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                val estilo = CineTheme.typography.bodyMd
                if (nombre.isEmpty()) {
                    Text(
                        text = stringResource(R.string.buscar_placeholder),
                        style = estilo,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = nombre,
                    onValueChange = onCambio,
                    singleLine = true,
                    textStyle = estilo.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onBuscar() }),
                    modifier = Modifier.fillMaxWidth().testTag(TagsBuscar.NOMBRE),
                )
            }
            if (nombre.isNotEmpty()) {
                SymbolIcon(
                    "cancel",
                    stringResource(R.string.cd_borrar_busqueda),
                    DimensBuscar.BorrarIcon,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .clip(CineShapes.Full)
                            .clickable(role = Role.Button) { onCambio("") }
                            .testTag(TagsBuscar.BORRAR_NOMBRE),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = DimensBuscar.AyudaTop).padding(horizontal = DimensBuscar.AyudaPaddingH),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        ) {
            Text(
                text = stringResource(R.string.buscar_insensible),
                style = CineTheme.typography.labelSm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (total != null) {
                Text(
                    text = pluralStringResource(R.plurals.buscar_coincidencias, total, total),
                    style = CineTheme.typography.labelSm,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.testTag(TagsBuscar.COINCIDENCIAS),
                )
            }
        }
    }
}

/** §9.2-2: "Todas", "Solo Películas", "Solo Series". */
@Composable
private fun Tipo(
    actual: Ambito,
    onElegir: (Ambito) -> Unit,
) {
    Column(modifier = bloque, verticalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
        EtiquetaSeccion(stringResource(R.string.buscar_tipo))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
            Ambito.entries.forEach { ambito ->
                val activo = ambito == actual
                val texto =
                    when (ambito) {
                        Ambito.TODO -> R.string.buscar_tipo_todas
                        Ambito.PELICULAS -> R.string.buscar_tipo_peliculas
                        Ambito.SERIES -> R.string.buscar_tipo_series
                    }
                Text(
                    text = stringResource(texto),
                    style = CineTheme.typography.labelMd,
                    color = if (activo) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .shadow(Sombras.Sm, CineShapes.Full)
                            .clip(CineShapes.Full)
                            .background(
                                if (activo) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            ).selectable(selected = activo, role = Role.Tab, onClick = { onElegir(ambito) })
                            .padding(horizontal = DimensBuscar.TipoPaddingH, vertical = DimensBuscar.TipoPaddingV)
                            .testTag(TagsBuscar.ambito(ambito)),
                )
            }
        }
    }
}

/** §9.2-3: rango de calificación con slider doble; los dos valores de la píldora se pueden escribir (CU-20 E2, E3). */
@Composable
private fun Rango(
    f: FormularioBusqueda,
    vm: BuscarViewModel,
) {
    Column(
        modifier =
            bloque
                .shadow(Sombras.Md, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(DimensBuscar.RangoPadding)
                .testTag(TagsBuscar.RANGO),
        verticalArrangement = Arrangement.spacedBy(Spacing.Sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
            SymbolIcon("star", null, DimensBuscar.RangoIcon, MaterialTheme.colorScheme.primary, filled = true)
            Text(
                text = stringResource(R.string.buscar_rango),
                style = CineTheme.typography.titleSm,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier =
                    Modifier
                        .clip(CineShapes.Full)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = DimensBuscar.ValoresPaddingH, vertical = DimensBuscar.ValoresPaddingV),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
            ) {
                ValorRango(f.textoMinimo, vm::escribirMinimo, stringResource(R.string.cd_puntuacion_minima), TagsBuscar.MINIMO)
                Text(
                    text = stringResource(R.string.buscar_rango_separador),
                    style = CineTheme.typography.labelSm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ValorRango(f.textoMaximo, vm::escribirMaximo, stringResource(R.string.cd_puntuacion_maxima), TagsBuscar.MAXIMO)
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(R.string.buscar_marca_min, R.string.buscar_marca_media, R.string.buscar_marca_max).forEachIndexed { i, marca ->
                Text(
                    text = stringResource(marca),
                    style = CineTheme.typography.labelSm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign =
                        when (i) {
                            0 -> TextAlign.Start
                            1 -> TextAlign.Center
                            else -> TextAlign.End
                        },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        CineRangeSlider(
            valor = f.posicionSlider,
            onCambio = vm::moverRango,
            rango = 0f..FormularioBusqueda.MAX_SLIDER,
            pasos = PASOS_SLIDER,
            color = MaterialTheme.colorScheme.primary,
            colorPista = MaterialTheme.colorScheme.surfaceContainerHighest,
            descripcionMinimo = stringResource(R.string.cd_puntuacion_minima),
            descripcionMaximo = stringResource(R.string.cd_puntuacion_maxima),
            modifier = Modifier.fillMaxWidth().testTag(TagsBuscar.SLIDER),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        ) {
            Text(
                text = stringResource(R.string.buscar_rango_ayuda),
                style = CineTheme.typography.labelSm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (f.rangoActivo) {
                Text(
                    text = stringResource(R.string.buscar_rango_excluye),
                    style = CineTheme.typography.labelSm,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    modifier = Modifier.testTag(TagsBuscar.EXCLUYE),
                )
            }
        }
    }
}

/** 0..10 con paso 0.1: 101 valores, 99 pasos intermedios (RNF-05). */
private const val PASOS_SLIDER = 99

@Composable
private fun ValorRango(
    texto: String,
    onCambio: (String) -> Unit,
    descripcion: String,
    tag: String,
) {
    val estilo = CineTheme.typography.labelMd.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    BasicTextField(
        value = texto,
        onValueChange = onCambio,
        singleLine = true,
        textStyle = estilo.copy(color = MaterialTheme.colorScheme.primary),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        modifier = Modifier.width(DimensBuscar.ValorAncho).semantics { contentDescription = descripcion }.testTag(tag),
    )
}

/** §9.2-4: géneros (varios = basta uno, S-19) y "Deseleccionar". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Generos(
    generos: List<Etiqueta>,
    marcados: Set<Long>,
    onAlternar: (Long) -> Unit,
    onDeseleccionar: () -> Unit,
) {
    Column(modifier = bloque) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EtiquetaSeccion(stringResource(R.string.buscar_generos), modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.buscar_deseleccionar),
                style = CineTheme.typography.labelSm,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .clip(CineShapes.Rounded)
                        .clickable(role = Role.Button, onClick = onDeseleccionar)
                        .testTag(TagsBuscar.DESELECCIONAR),
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.Sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.Sm),
        ) {
            generos.forEach { genero ->
                val marcado = genero.id in marcados
                Text(
                    text = if (marcado) stringResource(R.string.buscar_genero_marcado, genero.nombre) else genero.nombre,
                    style = CineTheme.typography.labelMd,
                    color = if (marcado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .shadow(Sombras.Sm, CineShapes.Lg)
                            .clip(CineShapes.Lg)
                            .background(
                                if (marcado) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                            ).toggleable(value = marcado, role = Role.Checkbox, onValueChange = { onAlternar(genero.id) })
                            .padding(horizontal = DimensBuscar.GeneroPaddingH, vertical = DimensBuscar.GeneroPaddingV)
                            .testTag(TagsBuscar.genero(genero.id)),
                )
            }
        }
    }
}

/** §9.2-5: desplegables de idioma y saga (uno de cada, como en el prototipo). */
@Composable
private fun IdiomaYSaga(
    estado: EstadoBuscar,
    vm: BuscarViewModel,
) {
    val f = estado.formulario
    Row(modifier = bloque, horizontalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
        Desplegable(
            etiqueta = R.string.buscar_idioma,
            icono = "translate",
            todos = R.string.buscar_idioma_todos,
            opciones = estado.opciones.idiomas,
            elegido = f.idiomaId,
            tag = TagsBuscar.IDIOMA,
            tagOpcion = TagsBuscar::opcionIdioma,
            onElegir = vm::elegirIdioma,
            modifier = Modifier.weight(1f),
        )
        Desplegable(
            etiqueta = R.string.buscar_saga,
            icono = "collections_bookmark",
            todos = R.string.buscar_saga_cualquiera,
            opciones = estado.opciones.sagas,
            elegido = f.sagaId,
            tag = TagsBuscar.SAGA,
            tagOpcion = TagsBuscar::opcionSaga,
            onElegir = vm::elegirSaga,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Desplegable(
    @StringRes etiqueta: Int,
    icono: String,
    @StringRes todos: Int,
    opciones: List<Etiqueta>,
    elegido: Long?,
    tag: String,
    tagOpcion: (Long?) -> String,
    onElegir: (Long?) -> Unit,
    modifier: Modifier,
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    val nombre = opciones.firstOrNull { it.id == elegido }?.nombre ?: stringResource(todos)
    Column(
        modifier =
            modifier
                .shadow(Sombras.Sm, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(DimensBuscar.DesplegablePadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.Xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
            SymbolIcon(icono, null, DimensBuscar.DesplegableIcon, MaterialTheme.colorScheme.tertiary)
            Text(
                text = stringResource(etiqueta),
                style = CineTheme.typography.labelSm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(CineShapes.Lg)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(role = Role.DropdownList) { abierto = true }
                        .padding(DimensBuscar.DesplegablePadding)
                        .testTag(tag),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = nombre,
                    style = CineTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                SymbolIcon("expand_more", null, DimensBuscar.DesplegableIcon, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(
                expanded = abierto,
                onDismissRequest = { abierto = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                (listOf<Etiqueta?>(null) + opciones).forEach { opcion ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = opcion?.nombre ?: stringResource(todos),
                                style = CineTheme.typography.bodySm,
                                fontWeight = if (opcion?.id == elegido) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        onClick = {
                            onElegir(opcion?.id)
                            abierto = false
                        },
                        modifier = Modifier.testTag(tagOpcion(opcion?.id)),
                    )
                }
            }
        }
    }
}

/** §9.2-6: estado en tu colección, selección múltiple (S-18). */
@Composable
private fun Estados(
    marcados: Set<Estado>,
    onAlternar: (Estado) -> Unit,
) {
    Column(modifier = bloque, verticalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
        EtiquetaSeccion(stringResource(R.string.buscar_estado))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
            Estado.entries.forEach { estado ->
                val activo = estado in marcados
                val color = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .shadow(Sombras.Sm, CineShapes.Xl)
                            .clip(CineShapes.Xl)
                            .background(
                                if (activo) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                            ).toggleable(value = activo, role = Role.Checkbox, onValueChange = { onAlternar(estado) })
                            .padding(DimensBuscar.EstadoPadding)
                            .testTag(TagsBuscar.estado(estado)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.Xs),
                ) {
                    SymbolIcon(estado.iconoBuscar(), null, DimensBuscar.EstadoIcon, color)
                    Text(
                        text = stringResource(estado.etiqueta()),
                        style = CineTheme.typography.labelSm,
                        fontWeight = if (activo) FontWeight.Bold else null,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Iconos de estado de Buscar (design.md §2.3). */
private fun Estado.iconoBuscar(): String =
    when (this) {
        Estado.PENDIENTE -> "hourglass_top"
        Estado.VIENDO -> "play_circle"
        Estado.VISTO -> "check_circle"
    }

/** §9.2-7: "Limpiar" (CU-20 A1) y "Aplicar Filtros ({n})". */
@Composable
private fun Acciones(
    total: Int?,
    onLimpiar: () -> Unit,
    onAplicar: () -> Unit,
) {
    Row(
        modifier = bloque.padding(top = DimensBuscar.AccionesTop),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .shadow(Sombras.Md, CineShapes.Xl)
                    .clip(CineShapes.Xl)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(role = Role.Button, onClick = onLimpiar)
                    .padding(vertical = DimensBuscar.LimpiarPaddingV)
                    .testTag(TagsBuscar.LIMPIAR),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Xs, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon("restart_alt", null, DimensBuscar.LimpiarIcon, MaterialTheme.colorScheme.onSurface)
            Text(
                text = stringResource(R.string.buscar_limpiar),
                style = CineTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        Row(
            modifier =
                Modifier
                    .weight(2f)
                    .shadow(Sombras.Lg, CineShapes.Xl)
                    .clip(CineShapes.Xl)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(role = Role.Button, onClick = onAplicar)
                    .padding(horizontal = DimensBuscar.AplicarPaddingH, vertical = DimensBuscar.AplicarPaddingV)
                    .testTag(TagsBuscar.APLICAR),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon("manage_search", null, DimensBuscar.AplicarIcon, MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                text = stringResource(R.string.buscar_aplicar, total ?: 0),
                style = CineTheme.typography.labelLg,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
            )
        }
    }
}

/** §9.2-8: cabecera "Resultados" con el recuento. */
@Composable
private fun CabeceraResultados(total: Int) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = Spacing.Margin, end = Spacing.Margin, top = DimensBuscar.ResultadosTop, bottom = Spacing.Sm)
                .testTag(TagsBuscar.RESULTADOS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.buscar_resultados),
            style = CineTheme.typography.titleSm,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = pluralStringResource(R.plurals.buscar_coincidencias, total, total),
            style = CineTheme.typography.labelSm,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag(TagsBuscar.TOTAL_RESULTADOS),
        )
    }
}

/** §9.2-8: tarjeta de resultado; pulsarla abre el detalle (CU-20 A3). */
@Composable
private fun TarjetaResultado(
    perfil: PerfilBiblioteca,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = Spacing.Margin, end = Spacing.Margin, bottom = Spacing.Sm)
                .shadow(Sombras.Md, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.cd_ver_detalle), onClick = onClick)
                .padding(DimensBuscar.ResultadoPadding)
                .testTag(TagsBuscar.resultado(perfil.id)),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(DimensBuscar.PosterAncho, DimensBuscar.PosterAlto)
                    .shadow(Sombras.Sm, CineShapes.Lg)
                    .clip(CineShapes.Lg)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(perfil.tipo.icono(), null, DimensBuscar.PosterIcon, CineTheme.colors.sky400)
        }
        Column(modifier = Modifier.weight(1f)) {
            ChipsResultado(perfil)
            Text(
                text = perfil.nombre,
                style = CineTheme.typography.titleSm,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val detalle = listOfNotNull(perfil.saga?.nombre, perfil.generos.joinToString(", ") { it.nombre }.ifEmpty { null })
            if (detalle.isNotEmpty()) {
                Text(
                    text = detalle.joinToString(SEPARADOR),
                    style = CineTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ChipEstado(perfil.estado)
        }
        SymbolIcon(
            "chevron_right",
            null,
            DimensBuscar.ChevronIcon,
            MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(DimensBuscar.ChevronPadding),
        )
    }
}

/** Separador de "{saga} • {géneros}" (el mismo de la tarjeta del prototipo). */
private const val SEPARADOR = " • "

@Composable
private fun ChipsResultado(perfil: PerfilBiblioteca) {
    Row(
        modifier = Modifier.padding(bottom = DimensBuscar.ChipsBottom),
        horizontalArrangement = Arrangement.spacedBy(DimensBuscar.ChipsGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(perfil.tipo.etiqueta()),
            style = CineTheme.typography.labelSm,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier =
                Modifier
                    .clip(CineShapes.Full)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = DimensBuscar.ChipTipoPaddingH, vertical = DimensBuscar.ChipTipoPaddingV),
        )
        perfil.puntuacion?.let { nota ->
            Text(
                text = stringResource(R.string.buscar_nota, nota.formatear()),
                style = CineTheme.typography.labelSm,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier =
                    Modifier
                        .clip(CineShapes.Full)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = CineAlpha.A20))
                        .padding(horizontal = DimensBuscar.ChipTipoPaddingH, vertical = DimensBuscar.ChipTipoPaddingV),
            )
        }
    }
}

/** Chip de estado de los resultados (design.md §2.3, columna "Buscar"). */
@Composable
private fun ChipEstado(estado: Estado) {
    val estilo =
        when (estado) {
            Estado.VIENDO -> CineTheme.colors.estadoViendo
            Estado.VISTO -> CineTheme.colors.estadoVisto
            Estado.PENDIENTE -> CineTheme.colors.estadoPendiente
        }
    Text(
        text = stringResource(estado.etiqueta()),
        style = CineTheme.typography.labelSm,
        color = estilo.chip.texto,
        maxLines = 1,
        modifier =
            Modifier
                .padding(top = DimensBuscar.EstadoTop)
                .clip(CineShapes.Full)
                .background(estilo.chip.fondo)
                .padding(horizontal = Spacing.Sm, vertical = Spacing.Xxs),
    )
}

/** CU-20 E1: M-13 con `search_off` y una sugerencia. */
@Composable
private fun SinResultados() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(DimensBuscar.SinResultadosPadding).testTag(TagsBuscar.SIN_RESULTADOS),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Sm),
    ) {
        SymbolIcon("search_off", null, DimensBuscar.SinResultadosIcon, MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(R.string.m13_sin_resultados),
            style = CineTheme.typography.titleSm,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.buscar_sin_resultados_ayuda),
            style = CineTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** CU-20 E2 / E3: criterios no válidos (M-14, M-03) o fallo de lectura (M-25); no se muestran resultados. */
@Composable
private fun ErrorCriterios(texto: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = Spacing.Margin, end = Spacing.Margin, top = DimensBuscar.ResultadosTop, bottom = Spacing.Sm)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(Spacing.Md)
                .testTag(TagsBuscar.ERROR),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon("error", null, DimensBuscar.RangoIcon, MaterialTheme.colorScheme.error)
        Text(text = texto, style = CineTheme.typography.bodySm, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
    }
}
