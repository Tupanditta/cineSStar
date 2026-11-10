package com.cinesstar.cinetrack.ui.datos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.contenedor
import com.cinesstar.cinetrack.data.backup.ArchivosSaf
import com.cinesstar.cinetrack.data.backup.Metricas
import com.cinesstar.cinetrack.domain.DetalleCopia
import com.cinesstar.cinetrack.domain.ResumenFusion
import com.cinesstar.cinetrack.ui.components.EtiquetaSeccion
import com.cinesstar.cinetrack.ui.components.HojaInferior
import com.cinesstar.cinetrack.ui.components.SymbolIcon
import com.cinesstar.cinetrack.ui.components.TextoAjustable
import com.cinesstar.cinetrack.ui.theme.CineAlpha
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.Dimens
import com.cinesstar.cinetrack.ui.theme.DimensComunes
import com.cinesstar.cinetrack.ui.theme.DimensDatos
import com.cinesstar.cinetrack.ui.theme.Sombras
import com.cinesstar.cinetrack.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Tipos de archivo que ofrece el selector al importar (muchos gestores marcan los .json como binarios o texto). */
private val TIPOS_IMPORTAR = arrayOf("application/json", "text/plain", "application/octet-stream")
private const val TIPO_EXPORTAR = "application/json"

/**
 * Estado y acciones de §10, con el selector del sistema para guardar (CU-21, `CreateDocument`) y para abrir (CU-22,
 * `OpenDocument`): la app no necesita permisos de almacenamiento. También muestra M-19 cuando toca confirmar.
 */
@Composable
fun rememberPanelDatos(): PanelDatos {
    val context = LocalContext.current
    val contenedor = context.contenedor
    val vm: DatosViewModel =
        viewModel { DatosViewModel(contenedor.copias, contenedor.archivos ?: ArchivosSaf(context.applicationContext.contentResolver)) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val nombre = stringResource(R.string.datos_archivo, LocalDate.now().toString())
    val guardar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TIPO_EXPORTAR)) { vm.exportar(it) }
    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { vm.abrir(it) }
    (estado.operacion as? Operacion.Confirmar)?.let { ConfirmarImportacion(it.resumen, vm::confirmar, vm::cancelar) }
    return PanelDatos(estado, vm, { guardar.launch(nombre) }, { abrir.launch(TIPOS_IMPORTAR) })
}

/** Elementos de "Datos Locales" antes del aviso en línea: banner, métricas, distribución, exportar e importar. */
const val ELEMENTOS_DATOS = 5

/** Lleva el aviso en línea (o el progreso) a la vista cuando aparece. [primero] es el índice del primer elemento de §10. */
@Composable
fun SeguirAviso(
    panel: PanelDatos,
    lista: LazyListState,
    primero: Int,
) {
    val visible = panel.estado.resultado != null || panel.estado.ocupado
    LaunchedEffect(panel.estado.resultado, panel.estado.ocupado) {
        if (visible && lista.layoutInfo.totalItemsCount > primero + ELEMENTOS_DATOS) lista.animateScrollToItem(primero + ELEMENTOS_DATOS)
    }
}

/** Pantalla Ajustes: el mismo contenido que la pestaña "Datos Locales", sin pestañas (design.md §10). */
@Composable
fun ContenidoAjustes() {
    val panel = rememberPanelDatos()
    val arriba = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Dimens.TopBarContentHeight + Spacing.Sm
    val abajo = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Dimens.ContentBottomPadding
    val lista = rememberLazyListState()
    SeguirAviso(panel, lista, primero = 0)
    LazyColumn(
        state = lista,
        modifier = Modifier.fillMaxSize().testTag(TagsDatos.LISTA_AJUSTES),
        contentPadding = PaddingValues(top = arriba, bottom = abajo),
    ) {
        datosLocales(panel)
    }
}

/** Contenido de §10 (padding lateral 16, separación 16). */
fun LazyListScope.datosLocales(panel: PanelDatos) {
    val estado = panel.estado
    item(key = "datos_banner") { Banner() }
    item(key = "datos_metricas") { MetricasBd(estado.metricas) }
    item(key = "datos_distribucion") { Distribucion(estado.metricas) }
    item(key = "datos_exportar") { TarjetaExportar(estado.tamanoEstimado, !estado.ocupado, panel.onExportar) }
    item(key = "datos_importar") { TarjetaImportar(!estado.ocupado, panel.onImportar) }
    when {
        estado.ocupado -> item(key = "datos_aviso") { Progreso(estado.operacion == Operacion.Exportando) }
        estado.resultado != null -> item(key = "datos_aviso") { AvisoEnLinea(estado.resultado) }
    }
}

private val bloque = Modifier.fillMaxWidth().padding(start = Spacing.Margin, end = Spacing.Margin, bottom = DimensDatos.BloquePadding)

/** §10-1: banner de privacidad. */
@Composable
private fun Banner() {
    Row(
        modifier =
            bloque
                .shadow(Sombras.Md, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(Spacing.Md)
                .testTag(TagsDatos.BANNER),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
    ) {
        SymbolIcon(
            "verified_user",
            null,
            DimensDatos.BannerIcon,
            MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = DimensDatos.BannerIconTop),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.datos_privacidad_titulo),
                style = CineTheme.typography.titleSm,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.datos_privacidad_texto),
                style = CineTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = DimensDatos.BannerTextoTop),
            )
        }
    }
}

/** §10-2: títulos, listas y almacenamiento. */
@Composable
private fun MetricasBd(metricas: Metricas?) {
    Column(modifier = bloque, verticalArrangement = Arrangement.spacedBy(DimensDatos.EtiquetaGap)) {
        EtiquetaSeccion(stringResource(R.string.datos_metricas))
        // Las tres tarjetas con la misma altura, aunque una etiqueta ocupe una línea y otra dos.
        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
            val modificador = Modifier.weight(1f).fillMaxHeight()
            Metrica(
                "movie_filter",
                MaterialTheme.colorScheme.primary,
                metricas?.titulos?.toString(),
                R.string.datos_titulos,
                TagsDatos.TITULOS,
                modificador,
            )
            Metrica(
                "playlist_add_check",
                MaterialTheme.colorScheme.tertiary,
                metricas?.listas?.toString(),
                R.string.datos_listas,
                TagsDatos.LISTAS,
                modificador,
            )
            Metrica(
                "sd_card",
                MaterialTheme.colorScheme.primary,
                metricas?.bytes?.let { tamano(it) },
                R.string.datos_almacenamiento,
                TagsDatos.ALMACENAMIENTO,
                modificador,
                lineas = 1,
            )
        }
    }
}

@Composable
private fun Metrica(
    icono: String,
    color: Color,
    valor: String?,
    etiqueta: Int,
    tag: String,
    modifier: Modifier,
    lineas: Int = 2,
) {
    Column(
        modifier =
            modifier
                .shadow(Sombras.Md, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(DimensDatos.MetricaPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SymbolIcon(icono, null, DimensDatos.MetricaIcon, color, modifier = Modifier.padding(bottom = DimensDatos.MetricaIconBottom))
        Text(
            text = valor ?: stringResource(R.string.datos_sin_valor),
            style = CineTheme.typography.headlineMd,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.testTag(tag),
        )
        // Con la escala tipográfica mayor (§2.5) "Almacenamiento" no cabe en una tarjeta de un tercio: la etiqueta reduce
        // su tamaño en vez de partir la palabra.
        TextoAjustable(stringResource(etiqueta), CineTheme.typography.labelSm, MaterialTheme.colorScheme.onSurfaceVariant, lineas = lineas)
    }
}

/** §10-3: películas y series con su porcentaje, y la última modificación de la base de datos. */
@Composable
private fun Distribucion(metricas: Metricas?) {
    val total = metricas?.titulos ?: 0
    val peliculas = metricas?.peliculas ?: 0
    val series = metricas?.series ?: 0
    val pctPeliculas = if (total == 0) 0 else (peliculas * CIEN / total.toFloat()).roundToInt()
    val pctSeries = if (total == 0) 0 else CIEN - pctPeliculas
    Column(
        modifier =
            bloque
                .shadow(Sombras.Md, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(Spacing.Md),
        verticalArrangement = Arrangement.spacedBy(Spacing.Sm),
    ) {
        Text(
            text = stringResource(R.string.datos_distribucion),
            style = CineTheme.typography.titleSm,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
            Barra(
                pluralStringResource(R.plurals.datos_peliculas, peliculas, peliculas),
                pctPeliculas,
                MaterialTheme.colorScheme.primary,
                TagsDatos.PELICULAS,
                TagsDatos.PORCENTAJE_PELICULAS,
            )
            Barra(
                pluralStringResource(R.plurals.datos_series, series, series),
                pctSeries,
                MaterialTheme.colorScheme.tertiary,
                TagsDatos.SERIES,
                TagsDatos.PORCENTAJE_SERIES,
            )
        }
        metricas?.ultimaModificacion?.let { UltimaModificacion(it) }
    }
}

private const val CIEN = 100

@Composable
private fun Barra(
    texto: String,
    porcentaje: Int,
    color: Color,
    tagTexto: String,
    tagPorcentaje: String,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = DimensDatos.BarraTextoBottom),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = texto,
                style = CineTheme.typography.labelSm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).testTag(tagTexto),
            )
            Text(
                text = stringResource(R.string.datos_porcentaje, porcentaje),
                style = CineTheme.typography.labelSm,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.testTag(tagPorcentaje),
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(DimensDatos.BarraAlto)
                    .clip(CineShapes.Full)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            if (porcentaje > 0) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(porcentaje / CIEN.toFloat())
                            .height(DimensDatos.BarraAlto)
                            .clip(CineShapes.Full)
                            .background(color),
                )
            }
        }
    }
}

@Composable
private fun UltimaModificacion(milisegundos: Long) {
    val momento = Instant.ofEpochMilli(milisegundos).atZone(ZoneId.systemDefault())
    val dia =
        if (momento.toLocalDate() == LocalDate.now()) {
            stringResource(R.string.datos_hoy)
        } else {
            momento.format(DateTimeFormatter.ofPattern(FORMATO_FECHA, Locale.ROOT))
        }
    Row(
        modifier = Modifier.padding(top = DimensDatos.PieTop),
        horizontalArrangement = Arrangement.spacedBy(DimensDatos.PieGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon("sync_saved_locally", null, DimensDatos.PieIcon, MaterialTheme.colorScheme.primary)
        Text(
            text =
                stringResource(
                    R.string.datos_ultima_modificacion,
                    dia,
                    momento.format(DateTimeFormatter.ofPattern(FORMATO_HORA, Locale.ROOT)),
                ),
            style = CineTheme.typography.labelSm,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TagsDatos.ULTIMA_MODIFICACION),
        )
    }
}

private const val FORMATO_FECHA = "dd/MM/yyyy"
private const val FORMATO_HORA = "HH:mm"

/** §10-4: "Exportar copia de seguridad" (CU-21). */
@Composable
private fun TarjetaExportar(
    tamanoEstimado: Long?,
    habilitada: Boolean,
    onExportar: () -> Unit,
) {
    Column(modifier = bloque, verticalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
        EtiquetaSeccion(stringResource(R.string.datos_acciones))
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(Sombras.Md, CineShapes.Xl)
                    .clip(CineShapes.Xl)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(Spacing.Md),
            verticalArrangement = Arrangement.spacedBy(Spacing.Sm),
        ) {
            CabeceraAccion(
                "file_download",
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
                R.string.datos_exportar_titulo,
                R.string.datos_exportar_texto,
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(CineShapes.Lg)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = DimensDatos.ArchivoPaddingH, vertical = DimensDatos.ArchivoPaddingV),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.datos_archivo, LocalDate.now().toString()),
                    style = CineTheme.typography.labelSm,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).testTag(TagsDatos.ARCHIVO),
                )
                tamanoEstimado?.let {
                    Text(
                        text = stringResource(R.string.datos_tamano_estimado, tamano(it)),
                        style = CineTheme.typography.labelSm,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        modifier = Modifier.testTag(TagsDatos.TAMANO),
                    )
                }
            }
            BotonAccion(
                "download_for_offline",
                R.string.datos_exportar_boton,
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
                Sombras.Md,
                habilitada,
                TagsDatos.EXPORTAR,
                onExportar,
            )
        }
    }
}

/** §10-4: "Importar copia de seguridad" (CU-22, fusiona). */
@Composable
private fun TarjetaImportar(
    habilitada: Boolean,
    onImportar: () -> Unit,
) {
    Column(
        modifier =
            bloque
                .shadow(Sombras.Md, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(Spacing.Md),
        verticalArrangement = Arrangement.spacedBy(Spacing.Sm),
    ) {
        CabeceraAccion(
            "file_upload",
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurface,
            R.string.datos_importar_titulo,
            R.string.datos_importar_texto,
        )
        BotonAccion(
            "folder_open",
            R.string.datos_importar_boton,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurface,
            Sombras.Sm,
            habilitada,
            TagsDatos.IMPORTAR,
            onImportar,
        )
    }
}

@Composable
private fun CabeceraAccion(
    icono: String,
    fondo: Color,
    color: Color,
    titulo: Int,
    texto: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Sm), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.clip(CineShapes.Full).background(fondo).padding(DimensDatos.CirculoPadding)) {
            SymbolIcon(icono, null, DimensDatos.CirculoIcon, color)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(titulo), style = CineTheme.typography.titleSm, color = MaterialTheme.colorScheme.onSurface)
            Text(text = stringResource(texto), style = CineTheme.typography.bodySm, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun BotonAccion(
    icono: String,
    texto: Int,
    fondo: Color,
    color: Color,
    sombra: Dp,
    habilitado: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(sombra, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(fondo)
                .clickable(enabled = habilitado, role = Role.Button, onClick = onClick)
                .alpha(if (habilitado) 1f else CineAlpha.A50)
                .padding(vertical = DimensDatos.BotonPaddingV)
                .testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(icono, null, DimensDatos.BotonIcon, color)
        // Sin límite de líneas: con la fuente del sistema grande el texto pasa a dos líneas en vez de cortarse (T9.08).
        Text(text = stringResource(texto), style = CineTheme.typography.labelLg, color = color, textAlign = TextAlign.Center)
    }
}

/** CU-22 E5 (y CU-21): indicador de progreso mientras se trabaja; la interfaz sigue respondiendo. */
@Composable
private fun Progreso(exportando: Boolean) {
    Column(
        modifier =
            bloque
                .shadow(Sombras.Xl, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(DimensDatos.AvisoPadding)
                .testTag(TagsDatos.PROGRESO),
        verticalArrangement = Arrangement.spacedBy(Spacing.Sm),
    ) {
        Text(
            text = stringResource(if (exportando) R.string.datos_exportando else R.string.datos_importando),
            style = CineTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.primary,
        )
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(DimensDatos.ProgresoAlto).clip(CineShapes.Full),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
}

/** §10-5: aviso en línea tras exportar o importar: éxito en `primary` con `check_circle`, error en `error`. */
@Composable
private fun AvisoEnLinea(resultado: Resultado) {
    val color = if (resultado.exito) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(
        modifier =
            bloque
                .shadow(Sombras.Xl, CineShapes.Xl)
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(DimensDatos.AvisoPadding)
                .testTag(TagsDatos.AVISO),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(if (resultado.exito) "check_circle" else "error", null, DimensDatos.AvisoIcon, color)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
            Text(text = textoResultado(resultado), style = CineTheme.typography.bodySm, color = color)
            (resultado as? Resultado.NoValida)?.detalle?.let {
                Text(
                    text = textoDetalle(it),
                    style = CineTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(TagsDatos.AVISO_DETALLE),
                )
            }
        }
    }
}

@Composable
private fun textoResultado(resultado: Resultado): String =
    when (resultado) {
        Resultado.Guardada -> stringResource(R.string.m15_copia_guardada)
        Resultado.NoGuardada -> stringResource(R.string.m16_copia_no_guardada)
        is Resultado.NoValida -> stringResource(R.string.m17_copia_no_valida)
        Resultado.VersionFutura -> stringResource(R.string.m18_copia_version_futura)
        is Resultado.Importada ->
            resultado.resumen.let {
                stringResource(
                    R.string.m20_importacion_completada,
                    it.perfilesNuevos,
                    it.perfilesOmitidos,
                    it.listasNuevas,
                    it.listasAmpliadas,
                )
            }
        Resultado.NoImportada -> stringResource(R.string.m21_importacion_fallida)
    }

/** Detalle de M-17 (CU-22 E3). */
@Composable
fun textoDetalle(detalle: DetalleCopia): String =
    when (detalle) {
        DetalleCopia.NombreVacio -> stringResource(R.string.copia_detalle_nombre_vacio)
        is DetalleCopia.PuntuacionInvalida -> stringResource(R.string.copia_detalle_puntuacion, detalle.perfil)
        is DetalleCopia.ComentarioLargo -> stringResource(R.string.copia_detalle_comentario, detalle.perfil)
        is DetalleCopia.PerfilDuplicado -> stringResource(R.string.copia_detalle_perfil_duplicado, detalle.perfil)
        is DetalleCopia.GeneroDesconocido -> stringResource(R.string.copia_detalle_genero, detalle.perfil, detalle.genero)
        is DetalleCopia.IdiomaDesconocido -> stringResource(R.string.copia_detalle_idioma, detalle.perfil, detalle.idioma)
        is DetalleCopia.ListaDuplicada -> stringResource(R.string.copia_detalle_lista_duplicada, detalle.lista)
        is DetalleCopia.ElementoSinPerfil -> stringResource(R.string.copia_detalle_sin_perfil, detalle.lista, detalle.perfil)
        is DetalleCopia.ElementoRepetido -> stringResource(R.string.copia_detalle_repetido, detalle.lista, detalle.perfil)
        is DetalleCopia.RestriccionLista -> stringResource(R.string.copia_detalle_restriccion, detalle.lista, detalle.perfil)
        is DetalleCopia.ElementoIncompleto -> stringResource(R.string.copia_detalle_incompleto, detalle.lista, detalle.perfil)
    }

/** CU-22 paso 3: M-19 con los recuentos de la fusión; confirmar es obligatorio (S-20). */
@Composable
private fun ConfirmarImportacion(
    resumen: ResumenFusion,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    HojaInferior(
        accion = stringResource(R.string.datos_confirmar_accion),
        cancelar = stringResource(R.string.comun_cancelar),
        onAccion = onConfirmar,
        onCancelar = onCancelar,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Sm), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(DimensComunes.SheetIconCircle)
                        .clip(CineShapes.Full)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = CineAlpha.A20)),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon("file_upload", null, DimensComunes.SheetIcon, MaterialTheme.colorScheme.primary)
            }
            Text(
                text = stringResource(R.string.datos_importar_titulo),
                style = CineTheme.typography.titleMd,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text =
                stringResource(
                    R.string.m19_confirmar_importacion,
                    resumen.perfilesNuevos,
                    resumen.listasNuevas,
                    resumen.listasAmpliadas,
                    resumen.perfilesOmitidos,
                ),
            style = CineTheme.typography.bodyMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TagsDatos.CONFIRMAR_TEXTO),
        )
    }
}

/** Tamaño legible: "512 B", "420 KB", "1.4 MB" (con punto, como las puntuaciones). */
@Composable
private fun tamano(bytes: Long): String =
    when {
        bytes < KB -> stringResource(R.string.tamano_bytes, bytes.toInt())
        bytes < KB * KB -> stringResource(R.string.tamano_kb, ((bytes + KB - 1) / KB).toInt())
        else -> stringResource(R.string.tamano_mb, String.format(Locale.ROOT, "%.1f", bytes / (KB * KB).toDouble()))
    }

private const val KB = 1024L
