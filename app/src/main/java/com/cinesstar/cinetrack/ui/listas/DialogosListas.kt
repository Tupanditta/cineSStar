package com.cinesstar.cinetrack.ui.listas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.ui.components.BotonesDialogoFormulario
import com.cinesstar.cinetrack.ui.components.CampoTexto
import com.cinesstar.cinetrack.ui.components.DialogoConfirmacion
import com.cinesstar.cinetrack.ui.components.DialogoFormulario
import com.cinesstar.cinetrack.ui.components.ErrorCampo
import com.cinesstar.cinetrack.ui.components.EtiquetaCampo
import com.cinesstar.cinetrack.ui.components.SymbolIcon
import com.cinesstar.cinetrack.ui.components.conNegrita
import com.cinesstar.cinetrack.ui.components.etiqueta
import com.cinesstar.cinetrack.ui.components.iconoMosaico
import com.cinesstar.cinetrack.ui.components.textoError
import com.cinesstar.cinetrack.ui.theme.CineAlpha
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.DimensDialogoFormulario
import com.cinesstar.cinetrack.ui.theme.DimensListas
import com.cinesstar.cinetrack.ui.theme.Spacing

/** Diálogo "Nueva Lista" (design.md §5.3, CU-11). */
@Composable
fun DialogoNuevaLista(
    dialogo: DialogoListas.Nueva,
    onNombre: (String) -> Unit,
    onTipo: (TipoLista) -> Unit,
    onRestriccion: (Restriccion) -> Unit,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit,
) {
    DialogoFormulario(
        titulo = stringResource(R.string.nueva_lista_titulo),
        icono = "create_new_folder",
        onCerrar = onCerrar,
        tag = TagsListas.DIALOGO_NUEVA,
    ) {
        CampoNombre(dialogo.nombre, dialogo.error, onNombre, onGuardar)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
            EtiquetaCampo(stringResource(R.string.nueva_lista_tipo))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
                TipoLista.entries.forEach { tipo ->
                    MosaicoTipo(tipo, dialogo.tipo == tipo, Modifier.weight(1f)) { onTipo(tipo) }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
            EtiquetaCampo(stringResource(R.string.nueva_lista_restriccion))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
                Restriccion.entries.forEach { restriccion ->
                    SegmentoRestriccion(restriccion, dialogo.restriccion == restriccion, Modifier.weight(1f)) { onRestriccion(restriccion) }
                }
            }
        }
        BotonesDialogoFormulario(stringResource(R.string.nueva_lista_guardar), onGuardar, onCerrar)
    }
}

/** Diálogo "Renombrar lista" (CU-14), con el estilo de formulario y el mismo campo que "Nueva Lista". */
@Composable
fun DialogoRenombrarLista(
    nombre: String,
    error: ErrorDominio?,
    onNombre: (String) -> Unit,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit,
) {
    DialogoFormulario(
        titulo = stringResource(R.string.renombrar_titulo),
        icono = "edit",
        onCerrar = onCerrar,
        tag = TagsListas.DIALOGO_RENOMBRAR,
    ) {
        CampoNombre(nombre, error, onNombre, onGuardar)
        BotonesDialogoFormulario(stringResource(R.string.comun_guardar), onGuardar, onCerrar)
    }
}

/** CU-19: confirmación destructiva con M-12 (design.md §6.4). */
@Composable
fun DialogoEliminarLista(
    nombre: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    DialogoConfirmacion(
        titulo = stringResource(R.string.eliminar_lista_titulo),
        texto = conNegrita(stringResource(R.string.m12_eliminar_lista), nombre),
        confirmar = stringResource(R.string.eliminar_lista_confirmar),
        cancelar = stringResource(R.string.comun_cancelar),
        onConfirmar = onConfirmar,
        onCancelar = onCancelar,
    )
}

@Composable
private fun CampoNombre(
    nombre: String,
    error: ErrorDominio?,
    onNombre: (String) -> Unit,
    onGuardar: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
        EtiquetaCampo(stringResource(R.string.nueva_lista_nombre))
        CampoTexto(
            valor = nombre,
            onCambio = onNombre,
            placeholder = stringResource(R.string.nueva_lista_placeholder),
            modifier = Modifier.fillMaxWidth().testTag(TagsListas.NOMBRE),
            forma = CineShapes.Lg,
            fondo = MaterialTheme.colorScheme.surfaceContainer,
            fondoEnfocado = MaterialTheme.colorScheme.surfaceContainerLowest,
            relleno = PaddingValues(DimensDialogoFormulario.CampoPadding),
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            accionesTeclado = KeyboardActions(onDone = { onGuardar() }),
        )
        if (error != null) ErrorCampo(textoError(error), TagsListas.ERROR_NOMBRE)
    }
}

@Composable
private fun MosaicoTipo(
    tipo: TipoLista,
    activo: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val (fondo, texto) =
        when {
            !activo -> MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurfaceVariant
            tipo == TipoLista.PRIORIDAD -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
            else -> CineTheme.colors.sky400 to CineTheme.colors.slate950
        }
    Column(
        modifier =
            modifier
                .clip(CineShapes.Lg)
                .background(fondo)
                .selectable(selected = activo, role = Role.RadioButton, onClick = onClick)
                .padding(DimensListas.MosaicoPadding)
                .testTag(TagsListas.tipo(tipo)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Xs),
    ) {
        SymbolIcon(tipo.iconoMosaico(), null, DimensListas.MosaicoIcon, texto)
        Text(text = stringResource(tipo.etiqueta()), style = CineTheme.typography.labelSm, fontWeight = FontWeight.Bold, color = texto)
    }
}

@Composable
private fun SegmentoRestriccion(
    restriccion: Restriccion,
    activo: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val fondo = if (activo) MaterialTheme.colorScheme.surfaceBright else MaterialTheme.colorScheme.surfaceContainer
    val texto: Color = if (activo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = stringResource(restriccion.etiqueta()),
        style = CineTheme.typography.labelSm,
        color = texto,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier =
            modifier
                .clip(CineShapes.Lg)
                .background(fondo)
                .then(
                    if (activo) {
                        Modifier.border(DimensListas.Anillo, CineTheme.colors.sky400.copy(alpha = CineAlpha.A40), CineShapes.Lg)
                    } else {
                        Modifier
                    },
                ).selectable(selected = activo, role = Role.RadioButton, onClick = onClick)
                .padding(vertical = DimensListas.SegmentoPaddingV)
                .testTag(TagsListas.restriccion(restriccion)),
    )
}
