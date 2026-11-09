package com.cinesstar.cinetrack.ui.lista

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.domain.PosicionAlta
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.ui.components.CampoTexto
import com.cinesstar.cinetrack.ui.components.EtiquetaCampo
import com.cinesstar.cinetrack.ui.components.SymbolIcon
import com.cinesstar.cinetrack.ui.components.etiqueta
import com.cinesstar.cinetrack.ui.theme.CineAlpha
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.DimensFormulario
import com.cinesstar.cinetrack.ui.theme.DimensLista
import com.cinesstar.cinetrack.ui.theme.Spacing

/** Lo que el usuario elige al añadir a un ranking (posición) o a una lista de prioridad (nivel). */
class OpcionesAltaEstado(
    val tipo: TipoLista,
    /** Elementos actuales de la lista (n): la posición va de 1 a n+1. */
    val total: Int,
    val posicion: String,
    val prioridad: Prioridad,
    val onPosicion: (String) -> Unit,
    val onPaso: (Int) -> Unit,
    val onPrioridad: (Prioridad) -> Unit,
)

/**
 * Opciones al añadir (design.md §6.6, CU-15 A1/A2): en un ranking, la fila "Posición" con − / + (estilo del stepper de
 * §8) y vacía = al final; en una lista de prioridad, los segmentos Alta / Media / Baja (Media por defecto, S-16). Una
 * lista normal no tiene opciones.
 */
@Composable
fun OpcionesAlta(
    estado: OpcionesAltaEstado,
    modifier: Modifier = Modifier,
) {
    when (estado.tipo) {
        TipoLista.RANKING -> FilaPosicion(estado, modifier)
        TipoLista.PRIORIDAD -> SegmentosPrioridad(estado.prioridad, estado.onPrioridad, modifier)
        TipoLista.NORMAL -> Unit
    }
}

@Composable
private fun FilaPosicion(
    estado: OpcionesAltaEstado,
    modifier: Modifier,
) {
    val alFinal = estado.total + 1
    val destino = PosicionAlta.destino(estado.posicion, estado.total)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Xxs)) {
            EtiquetaCampo(stringResource(R.string.alta_posicion))
            Text(
                text = stringResource(R.string.alta_posicion_ayuda, alFinal),
                style = CineTheme.typography.labelSm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier =
                Modifier
                    .clip(CineShapes.Xl)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(DimensFormulario.StepperPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BotonPaso("remove", stringResource(R.string.cd_posicion_menos), (destino ?: alFinal) > 1, TagsLista.ALTA_MENOS) {
                estado.onPaso(-1)
            }
            val descripcion = stringResource(R.string.cd_posicion_alta)
            CampoTexto(
                valor = estado.posicion,
                onCambio = estado.onPosicion,
                placeholder = alFinal.toString(),
                modifier =
                    Modifier
                        .width(DimensFormulario.StepperField)
                        .semantics { contentDescription = descripcion }
                        .testTag(TagsLista.ALTA_POSICION),
                estilo = CineTheme.typography.headlineMd.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                fondo = Color.Transparent,
                fondoEnfocado = Color.Transparent,
                relleno = PaddingValues(),
                teclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                alineacionPlaceholder = Alignment.Center,
            )
            BotonPaso("add", stringResource(R.string.cd_posicion_mas), (destino ?: alFinal) < alFinal, TagsLista.ALTA_MAS) {
                estado.onPaso(1)
            }
        }
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
private fun SegmentosPrioridad(
    elegida: Prioridad,
    onElegir: (Prioridad) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
        EtiquetaCampo(stringResource(R.string.alta_prioridad))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
            Prioridad.entries.forEach { prioridad ->
                val activo = prioridad == elegida
                Text(
                    text = stringResource(prioridad.etiqueta()),
                    style = CineTheme.typography.labelSm,
                    color = if (activo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(CineShapes.Lg)
                            .background(if (activo) MaterialTheme.colorScheme.surfaceBright else MaterialTheme.colorScheme.surfaceContainer)
                            .then(
                                if (activo) {
                                    Modifier.border(DimensLista.Anillo, CineTheme.colors.sky400.copy(alpha = CineAlpha.A40), CineShapes.Lg)
                                } else {
                                    Modifier
                                },
                            ).selectable(selected = activo, role = Role.RadioButton) { onElegir(prioridad) }
                            .padding(vertical = DimensLista.SegmentoPaddingV)
                            .testTag(TagsLista.altaPrioridad(prioridad)),
                )
            }
        }
    }
}
