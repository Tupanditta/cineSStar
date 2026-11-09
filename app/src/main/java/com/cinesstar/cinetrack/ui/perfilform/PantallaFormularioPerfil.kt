package com.cinesstar.cinetrack.ui.perfilform

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.contenedor
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.PasoPuntuacion
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.ui.components.Aviso
import com.cinesstar.cinetrack.ui.components.CampoTexto
import com.cinesstar.cinetrack.ui.components.CineSlider
import com.cinesstar.cinetrack.ui.components.DialogoConfirmacion
import com.cinesstar.cinetrack.ui.components.EtiquetaSeccion
import com.cinesstar.cinetrack.ui.components.LocalAvisos
import com.cinesstar.cinetrack.ui.components.PantallaSecundaria
import com.cinesstar.cinetrack.ui.components.SymbolIcon
import com.cinesstar.cinetrack.ui.components.etiqueta
import com.cinesstar.cinetrack.ui.components.icono
import com.cinesstar.cinetrack.ui.components.iconoFormulario
import com.cinesstar.cinetrack.ui.components.tagPantalla
import com.cinesstar.cinetrack.ui.components.textoError
import com.cinesstar.cinetrack.ui.theme.CineAlpha
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.Dimens
import com.cinesstar.cinetrack.ui.theme.DimensComunes
import com.cinesstar.cinetrack.ui.theme.DimensFormulario
import com.cinesstar.cinetrack.ui.theme.Sombras
import com.cinesstar.cinetrack.ui.theme.Spacing

/**
 * Pantalla "Añadir nuevo perfil" (CU-01) y, con [perfilId], "Editar Medio" (CU-02), design.md §8. Al editar, el
 * formulario se abre con los datos actuales y el tipo no se puede cambiar (S-03).
 */
@Composable
fun PantallaFormularioPerfil(
    onSalir: () -> Unit,
    onAvatar: () -> Unit,
    perfilId: Long? = null,
) {
    val contenedor = LocalContext.current.contenedor
    val vm: FormularioPerfilViewModel =
        viewModel {
            FormularioPerfilViewModel(createSavedStateHandle(), contenedor.perfiles, contenedor.sagas, contenedor.catalogo, perfilId)
        }
    val estado by vm.estado.collectAsStateWithLifecycle()
    var confirmarDescarte by rememberSaveable { mutableStateOf(false) }
    val avisos = LocalAvisos.current
    val edicion = perfilId != null
    val textoGuardado = stringResource(if (edicion) R.string.toast_cambios_guardados else R.string.toast_perfil_guardado)
    val textoNoExiste = stringResource(R.string.aviso_perfil_no_existe)

    // CU-01 A7 / CU-02 A4: cancelar o atrás con datos (o con cambios, al editar) pide confirmar el descarte.
    val salir = { if (estado.hayCambios && estado.guardadoId == null) confirmarDescarte = true else onSalir() }
    BackHandler(onBack = salir)

    LaunchedEffect(estado.guardadoId) {
        if (estado.guardadoId != null) {
            avisos.mostrar(Aviso(textoGuardado, exito = true))
            onSalir()
        }
    }
    LaunchedEffect(estado.noExiste) {
        if (estado.noExiste) {
            avisos.mostrar(Aviso(textoNoExiste))
            onSalir()
        }
    }

    PantallaSecundaria(
        titulo = stringResource(if (edicion) R.string.titulo_editar_medio else R.string.titulo_anadir_medio),
        tag = if (edicion) TagsFormulario.PANTALLA_EDITAR else DestinoBarra.ANADIR.tagPantalla(),
        onBack = salir,
        onAvatarClick = onAvatar,
    ) {
        Formulario(estado, vm, onCancelar = salir)
    }

    if (confirmarDescarte) {
        DialogoConfirmacion(
            titulo = stringResource(if (edicion) R.string.descartar_cambios_titulo else R.string.descartar_titulo),
            texto = stringResource(if (edicion) R.string.descartar_cambios_texto else R.string.descartar_texto),
            confirmar = stringResource(R.string.descartar_confirmar),
            cancelar = stringResource(R.string.descartar_seguir),
            onConfirmar = {
                confirmarDescarte = false
                onSalir()
            },
            onCancelar = { confirmarDescarte = false },
        )
    }
}

@Composable
private fun Formulario(
    estado: EstadoFormulario,
    vm: FormularioPerfilViewModel,
    onCancelar: () -> Unit,
) {
    val arriba = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Dimens.TopBarContentHeight
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .testTag(TagsFormulario.SCROLL)
                .padding(top = arriba)
                .navigationBarsPadding()
                .padding(start = Spacing.Margin, end = Spacing.Margin, bottom = DimensFormulario.ContentBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.Lg),
    ) {
        Cabecera(estado.edicion, onCancelar)
        SeccionTipo(estado.borrador.tipo, habilitado = !estado.edicion, onElegir = vm::elegirTipo)
        SeccionNombre(estado, vm)
        SeccionEstado(estado.borrador.estado, vm::elegirEstado)
        SeccionPuntuacion(estado.borrador.puntuacion, vm)
        SeccionSaga(estado, vm)
        SeccionChips(
            titulo = stringResource(R.string.form_generos),
            opciones = estado.generos.map { it.id to it.nombre },
            seleccionados = estado.borrador.generoIds,
            tagContador = TagsFormulario.CONTADOR_GENEROS,
            onAlternar = vm::alternarGenero,
        )
        SeccionChips(
            titulo = stringResource(R.string.form_idiomas),
            opciones = estado.idiomas.map { it.id to it.nombre },
            seleccionados = estado.borrador.idiomaIds,
            tagContador = TagsFormulario.CONTADOR_IDIOMAS,
            onAlternar = vm::alternarIdioma,
        )
        SeccionComentario(estado.borrador.comentario, vm::cambiarComentario)
        BotonGuardar(estado, vm::guardar)
    }
}

@Composable
private fun Cabecera(
    edicion: Boolean,
    onCancelar: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(if (edicion) R.string.form_titulo_editar else R.string.form_titulo),
                style = CineTheme.typography.headlineMd,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(if (edicion) R.string.form_subtitulo_editar else R.string.form_subtitulo),
                style = CineTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.form_cancelar),
            style = CineTheme.typography.labelMd,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .clip(CineShapes.Full)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(role = Role.Button, onClick = onCancelar)
                    .padding(horizontal = DimensFormulario.CancelPaddingH, vertical = DimensFormulario.CancelPaddingV)
                    .testTag(TagsFormulario.CANCELAR),
        )
    }
}

@Composable
private fun SeccionTipo(
    tipo: TipoPerfil,
    habilitado: Boolean,
    onElegir: (TipoPerfil) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
        EtiquetaSeccion(stringResource(R.string.form_tipo), obligatorio = stringResource(R.string.form_obligatorio))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(CineShapes.Xl)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    // Al editar está deshabilitado (S-03): se ve el tipo actual, atenuado.
                    .alpha(if (habilitado) 1f else CineAlpha.A50)
                    .padding(DimensFormulario.SegmentContainerPadding),
        ) {
            TipoPerfil.entries.forEach { opcion ->
                val activo = opcion == tipo
                val color = if (activo) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .then(if (activo) Modifier.shadow(Sombras.Md, CineShapes.Lg) else Modifier)
                            .clip(CineShapes.Lg)
                            .background(if (activo) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .selectable(selected = activo, enabled = habilitado, role = Role.Tab, onClick = { onElegir(opcion) })
                            .padding(vertical = DimensFormulario.SegmentButtonPaddingV)
                            .testTag(TagsFormulario.tipo(opcion)),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Xs, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SymbolIcon(opcion.icono(), null, DimensFormulario.SegmentIcon, color)
                    Text(
                        text = stringResource(opcion.etiqueta()),
                        style = CineTheme.typography.labelLg,
                        fontWeight = if (activo) FontWeight.Bold else null,
                        color = color,
                    )
                }
            }
        }
    }
}

@Composable
private fun SeccionNombre(
    estado: EstadoFormulario,
    vm: FormularioPerfilViewModel,
) {
    val tipo = estado.borrador.tipo
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
                Text(
                    text = stringResource(R.string.form_nombre),
                    style = CineTheme.typography.labelMd,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.form_obligatorio),
                    style = CineTheme.typography.labelMd,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            IndicadorDisponibilidad(estado.disponibilidad)
        }
        CampoTexto(
            valor = estado.borrador.nombre,
            onCambio = vm::cambiarNombre,
            placeholder = stringResource(R.string.form_nombre_placeholder),
            modifier = Modifier.fillMaxWidth().testTag(TagsFormulario.NOMBRE),
            relleno =
                PaddingValues(
                    start = DimensFormulario.NamePaddingStart,
                    end = DimensComunes.FieldPaddingH,
                    top = DimensComunes.FieldPaddingV,
                    bottom = DimensComunes.FieldPaddingV,
                ),
            teclado = KeyboardOptions(imeAction = ImeAction.Done),
            // "Intentar guardar" desde el teclado muestra M-01 si el nombre está vacío (CU-01 E1).
            accionesTeclado = KeyboardActions(onDone = { vm.guardar() }),
            principio = {
                SymbolIcon(
                    "title",
                    null,
                    DimensFormulario.NameIcon,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = DimensFormulario.NameIconStart),
                )
            },
        )
        val error =
            when {
                estado.mostrarNombreObligatorio -> stringResource(R.string.m01_nombre_obligatorio)
                estado.disponibilidad == Disponibilidad.YA_REGISTRADO -> textoError(ErrorDominio.PerfilDuplicado(tipo))
                else -> null
            }
        if (error != null) {
            Mensaje("error", error, MaterialTheme.colorScheme.secondary, Modifier.testTag(TagsFormulario.NOMBRE_ERROR))
        }
        Mensaje(
            "info",
            stringResource(R.string.form_nombre_ayuda),
            MaterialTheme.colorScheme.onSurfaceVariant,
            iconoColor = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun IndicadorDisponibilidad(disponibilidad: Disponibilidad) {
    val (icono, texto, color) =
        when (disponibilidad) {
            Disponibilidad.DISPONIBLE -> Triple("check_circle", R.string.form_disponible, MaterialTheme.colorScheme.primary)
            Disponibilidad.YA_REGISTRADO -> Triple("cancel", R.string.form_ya_registrado, MaterialTheme.colorScheme.secondary)
            else -> return
        }
    Row(
        modifier = Modifier.testTag(TagsFormulario.DISPONIBILIDAD),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(icono, null, DimensComunes.SmallIcon, color)
        Text(text = stringResource(texto), style = CineTheme.typography.labelSm, color = color)
    }
}

/** Texto de ayuda o error con icono de 14 dp. */
@Composable
private fun Mensaje(
    icono: String,
    texto: String,
    color: Color,
    modifier: Modifier = Modifier,
    iconoColor: Color = color,
    negrita: Boolean = false,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.Xs), verticalAlignment = Alignment.CenterVertically) {
        SymbolIcon(icono, null, DimensFormulario.HelpIcon, iconoColor)
        Text(text = texto, style = CineTheme.typography.bodySm, fontWeight = if (negrita) FontWeight.SemiBold else null, color = color)
    }
}

@Composable
private fun SeccionEstado(
    actual: Estado,
    onElegir: (Estado) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
        EtiquetaSeccion(stringResource(R.string.form_estado), obligatorio = stringResource(R.string.form_obligatorio))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
            Estado.entries.forEach { opcion ->
                val activo = opcion == actual
                val color = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                val fondo = if (activo) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .then(if (activo) Modifier.shadow(Sombras.Sm, CineShapes.Xl) else Modifier)
                            .clip(CineShapes.Xl)
                            .background(fondo)
                            .selectable(selected = activo, role = Role.Tab, onClick = { onElegir(opcion) })
                            .padding(vertical = DimensFormulario.EstadoTilePaddingV, horizontal = DimensFormulario.EstadoTilePaddingH)
                            .testTag(TagsFormulario.estado(opcion)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.Xs),
                ) {
                    SymbolIcon(opcion.iconoFormulario(), null, DimensFormulario.EstadoIcon, color)
                    Text(text = stringResource(opcion.etiqueta()), style = CineTheme.typography.labelMd, color = color)
                }
            }
        }
    }
}

@Composable
private fun SeccionPuntuacion(
    texto: String,
    vm: FormularioPerfilViewModel,
) {
    val lectura = Puntuacion.leer(texto)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(DimensFormulario.ScoreCardPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.Sm),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon("star", null, DimensFormulario.ScoreStarIcon, MaterialTheme.colorScheme.primary, filled = true)
                Text(
                    text = stringResource(R.string.form_puntuacion),
                    style = CineTheme.typography.labelLg,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(R.string.form_opcional),
                style = CineTheme.typography.labelSm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .clip(CineShapes.Rounded)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = DimensFormulario.OpcionalPaddingH, vertical = DimensFormulario.OpcionalPaddingV),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(CineShapes.Lg)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(DimensFormulario.AvisoPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon("lightbulb", null, DimensFormulario.AvisoIcon, MaterialTheme.colorScheme.tertiary)
            Text(
                text = stringResource(R.string.form_puntuacion_aviso),
                style = CineTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.Xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Stepper(texto, vm)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Xs, Alignment.End),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = (lectura as? Puntuacion.Lectura.Valida)?.puntuacion?.formatear() ?: stringResource(R.string.sin_valor),
                    style = CineTheme.typography.displayLg,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline().testTag(TagsFormulario.NOTA_GRANDE),
                )
                Text(
                    text = stringResource(R.string.sobre_diez),
                    style = CineTheme.typography.labelMd,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        CineSlider(
            valor = PasoPuntuacion.valorSlider(texto),
            onCambio = vm::moverSlider,
            rango = 0f..PUNTUACION_MAXIMA,
            pasos = PASOS_SLIDER,
            color = CineTheme.colors.sky400,
            colorPista = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().testTag(TagsFormulario.SLIDER),
        )
        MensajePuntuacion(lectura)
    }
}

@Composable
private fun Stepper(
    texto: String,
    vm: FormularioPerfilViewModel,
) {
    Row(
        modifier =
            Modifier
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(DimensFormulario.StepperPadding),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotonPaso(
            "remove",
            stringResource(R.string.cd_bajar_puntuacion),
            PasoPuntuacion.puedeBajar(texto),
            TagsFormulario.MENOS,
        ) { vm.pasoPuntuacion(-PasoPuntuacion.PASO) }
        val descripcion = stringResource(R.string.cd_puntuacion)
        CampoTexto(
            valor = texto,
            onCambio = vm::cambiarPuntuacion,
            placeholder = stringResource(R.string.form_puntuacion_placeholder),
            modifier =
                Modifier
                    .width(DimensFormulario.StepperField)
                    .semantics { contentDescription = descripcion }
                    .testTag(TagsFormulario.PUNTUACION),
            estilo = CineTheme.typography.headlineMd.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            fondo = Color.Transparent,
            fondoEnfocado = Color.Transparent,
            relleno = PaddingValues(),
            teclado = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            alineacionPlaceholder = Alignment.Center,
        )
        BotonPaso(
            "add",
            stringResource(R.string.cd_subir_puntuacion),
            PasoPuntuacion.puedeSubir(texto),
            TagsFormulario.MAS,
        ) { vm.pasoPuntuacion(PasoPuntuacion.PASO) }
    }
}

@Composable
private fun BotonPaso(
    icono: String,
    descripcion: String,
    habilitado: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(DimensFormulario.StepperButton)
                .clip(CineShapes.Lg)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(enabled = habilitado, role = Role.Button, onClick = onClick)
                .alpha(if (habilitado) 1f else CineAlpha.A40)
                .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(icono, descripcion, DimensFormulario.StepperIcon, MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MensajePuntuacion(lectura: Puntuacion.Lectura) {
    Box(modifier = Modifier.heightIn(min = DimensFormulario.FeedbackMinHeight).testTag(TagsFormulario.MENSAJE_PUNTUACION)) {
        when (lectura) {
            Puntuacion.Lectura.Vacia ->
                Mensaje("info", stringResource(R.string.form_puntuacion_neutro), MaterialTheme.colorScheme.onSurfaceVariant)
            is Puntuacion.Lectura.Valida ->
                Mensaje("check_circle", stringResource(R.string.form_puntuacion_valida), MaterialTheme.colorScheme.primary, negrita = true)
            Puntuacion.Lectura.Invalida ->
                Mensaje("error", textoError(ErrorDominio.PuntuacionInvalida), MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun SeccionSaga(
    estado: EstadoFormulario,
    vm: FormularioPerfilViewModel,
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EtiquetaSeccion(stringResource(R.string.form_saga), modifier = Modifier.weight(1f))
            Row(
                modifier =
                    Modifier
                        .clip(CineShapes.Rounded)
                        .clickable(role = Role.Button, onClick = vm::abrirEditorSaga)
                        .testTag(TagsFormulario.CREAR_SAGA),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon("add", null, DimensFormulario.SagaLinkIcon, MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(R.string.form_crear_saga),
                    style = CineTheme.typography.labelSm,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Box {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(CineShapes.Xl)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable(role = Role.DropdownList, onClick = { abierto = true })
                        .padding(horizontal = DimensComunes.FieldPaddingH, vertical = DimensComunes.FieldPaddingV)
                        .testTag(TagsFormulario.SAGA),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = estado.sagaElegida ?: stringResource(R.string.form_saga_ninguna),
                    style = CineTheme.typography.bodyMd,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                SymbolIcon("expand_more", null, DimensComunes.DropdownIcon, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(
                expanded = abierto,
                onDismissRequest = { abierto = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                OpcionMenu(stringResource(R.string.form_saga_ninguna)) {
                    vm.elegirSaga(null)
                    abierto = false
                }
                estado.opcionesSaga.forEach { opcion ->
                    OpcionMenu(opcion.nombre) {
                        if (opcion.sagaId == null) vm.elegirSagaNueva() else vm.elegirSaga(opcion.sagaId)
                        abierto = false
                    }
                }
            }
        }
        if (estado.borrador.editorSagaAbierto) EditorSaga(estado, vm)
    }
}

@Composable
private fun OpcionMenu(
    texto: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = texto, style = CineTheme.typography.bodyMd, color = MaterialTheme.colorScheme.onSurface) },
        onClick = onClick,
    )
}

/** Caja en línea "Nueva saga local" (design.md §8.6, CU-10 A1). */
@Composable
private fun EditorSaga(
    estado: EstadoFormulario,
    vm: FormularioPerfilViewModel,
) {
    Column(
        modifier =
            Modifier
                .padding(top = Spacing.Xs)
                .fillMaxWidth()
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(DimensFormulario.InlineSagaPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.Xs),
    ) {
        Text(
            text = stringResource(R.string.form_saga_nueva_titulo),
            style = CineTheme.typography.labelSm,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Xs), verticalAlignment = Alignment.CenterVertically) {
            CampoTexto(
                valor = estado.borrador.editorSagaTexto,
                onCambio = vm::cambiarTextoSaga,
                placeholder = stringResource(R.string.form_saga_placeholder),
                modifier = Modifier.weight(1f).testTag(TagsFormulario.SAGA_CAMPO),
                estilo = CineTheme.typography.bodySm,
                forma = CineShapes.Lg,
                fondo = MaterialTheme.colorScheme.surfaceContainerLowest,
                fondoEnfocado = MaterialTheme.colorScheme.surfaceContainerLowest,
                relleno = PaddingValues(horizontal = DimensFormulario.InlineFieldPaddingH, vertical = DimensFormulario.InlineFieldPaddingV),
                teclado = KeyboardOptions(imeAction = ImeAction.Done),
                accionesTeclado = KeyboardActions(onDone = { vm.confirmarSagaNueva() }),
            )
            Text(
                text = stringResource(R.string.form_saga_guardar),
                style = CineTheme.typography.labelSm,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier =
                    Modifier
                        .clip(CineShapes.Lg)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(role = Role.Button, onClick = vm::confirmarSagaNueva)
                        .padding(horizontal = DimensFormulario.InlineButtonPaddingH, vertical = DimensFormulario.InlineFieldPaddingV)
                        .testTag(TagsFormulario.SAGA_GUARDAR),
            )
            Box(
                modifier =
                    Modifier
                        .clip(CineShapes.Lg)
                        .clickable(role = Role.Button, onClick = vm::cerrarEditorSaga)
                        .padding(DimensFormulario.InlineFieldPaddingV)
                        .testTag(TagsFormulario.SAGA_CERRAR),
            ) {
                SymbolIcon(
                    "close",
                    stringResource(R.string.cd_cerrar),
                    DimensFormulario.InlineCloseIcon,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when (val error = estado.errorSaga) {
            null -> Unit
            ErrorSaga.Vacia ->
                Mensaje(
                    "error",
                    textoError(ErrorDominio.SagaNombreObligatorio),
                    MaterialTheme.colorScheme.secondary,
                    Modifier.testTag(TagsFormulario.SAGA_ERROR),
                )
            is ErrorSaga.Duplicada -> {
                Mensaje(
                    "error",
                    textoError(ErrorDominio.SagaDuplicada),
                    MaterialTheme.colorScheme.secondary,
                    Modifier.testTag(TagsFormulario.SAGA_ERROR),
                )
                Text(
                    text = stringResource(R.string.form_saga_elegir_existente),
                    style = CineTheme.typography.labelSm,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .clip(CineShapes.Rounded)
                            .clickable(role = Role.Button, onClick = { vm.elegirSagaExistente(error.existente) })
                            .padding(vertical = Spacing.Xs)
                            .testTag(TagsFormulario.SAGA_EXISTENTE),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionChips(
    titulo: String,
    opciones: List<Pair<Long, String>>,
    seleccionados: Set<Long>,
    tagContador: String,
    onAlternar: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EtiquetaSeccion(titulo, modifier = Modifier.weight(1f))
            Text(
                text = pluralStringResource(R.plurals.form_seleccionados, seleccionados.size, seleccionados.size),
                style = CineTheme.typography.labelSm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(tagContador),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Xs), verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
            opciones.forEach { (id, nombre) ->
                val activo = id in seleccionados
                Text(
                    text = nombre,
                    style = CineTheme.typography.labelMd,
                    fontWeight = if (activo) FontWeight.Bold else null,
                    color = if (activo) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .then(if (activo) Modifier.shadow(Sombras.Sm, CineShapes.Full) else Modifier)
                            .clip(CineShapes.Full)
                            .background(
                                if (activo) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            ).toggleable(value = activo, role = Role.Checkbox, onValueChange = { onAlternar(id) })
                            .padding(horizontal = DimensFormulario.ChipPaddingH, vertical = DimensFormulario.ChipPaddingV),
                )
            }
        }
    }
}

@Composable
private fun SeccionComentario(
    texto: String,
    onCambio: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
        EtiquetaSeccion(stringResource(R.string.form_comentario), sufijo = stringResource(R.string.form_comentario_opcional))
        CampoTexto(
            valor = texto,
            onCambio = onCambio,
            placeholder = stringResource(R.string.form_comentario_placeholder),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(
                        minHeight = DimensFormulario.TextAreaMinHeight,
                    ).testTag(TagsFormulario.COMENTARIO),
            relleno = PaddingValues(DimensFormulario.TextAreaPadding),
            unaLinea = false,
            lineasMinimas = LINEAS_COMENTARIO,
            alineacionPlaceholder = Alignment.TopStart,
        )
    }
}

@Composable
private fun BotonGuardar(
    estado: EstadoFormulario,
    onGuardar: () -> Unit,
) {
    val habilitado = estado.puedeGuardar
    Column(modifier = Modifier.padding(top = DimensFormulario.SaveTop), verticalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
        if (estado.errorEscritura) {
            Mensaje(
                "error",
                stringResource(R.string.m25_error_inesperado),
                MaterialTheme.colorScheme.secondary,
                Modifier.testTag(TagsFormulario.ERROR_ESCRITURA),
            )
        }
        val color = if (habilitado) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .alpha(if (habilitado) 1f else CineAlpha.A50)
                    .shadow(if (habilitado) Sombras.Lg else Sombras.Md, CineShapes.Full)
                    .clip(CineShapes.Full)
                    .background(
                        if (habilitado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    ).clickable(enabled = habilitado, role = Role.Button, onClick = onGuardar)
                    .padding(vertical = DimensFormulario.SavePaddingV)
                    .testTag(TagsFormulario.GUARDAR),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Xs, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon("save", null, DimensFormulario.SaveIcon, color)
            Text(
                text = stringResource(R.string.form_guardar),
                style = CineTheme.typography.labelLg,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

private const val PUNTUACION_MAXIMA = 10f

/** 0.0..10 en pasos de 0.1: 99 marcas intermedias. */
private const val PASOS_SLIDER = 99
private const val LINEAS_COMENTARIO = 4
