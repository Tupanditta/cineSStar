package com.cinesstar.cinetrack.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.domain.OpcionLista
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.ui.components.DialogoFormulario
import com.cinesstar.cinetrack.ui.components.SymbolIcon
import com.cinesstar.cinetrack.ui.components.etiqueta
import com.cinesstar.cinetrack.ui.lista.OpcionesAlta
import com.cinesstar.cinetrack.ui.lista.OpcionesAltaEstado
import com.cinesstar.cinetrack.ui.listas.textoTotal
import com.cinesstar.cinetrack.ui.theme.CineAlpha
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.DimensDetalle
import com.cinesstar.cinetrack.ui.theme.DimensDialogoFormulario
import com.cinesstar.cinetrack.ui.theme.DimensLista
import com.cinesstar.cinetrack.ui.theme.Spacing

/**
 * "Añadir a lista" desde el perfil (CU-15, design.md §7.5), con el estilo de formulario (§3.6-1). Tocar una lista normal
 * añade el perfil y cierra: 2 pasos (RNF-09). Un ranking o una lista de prioridad se despliega con la posición o el
 * nivel (por defecto al final y Media) y el botón "Añadir": 3 pasos. Las listas que ya lo contienen o que no lo admiten
 * se ven al 40 % y sin icono; si se tocan, se explica por qué (M-10, M-09).
 */
@Composable
fun SelectorListas(
    opciones: List<OpcionLista>,
    onElegir: (OpcionLista) -> Unit,
    onCerrar: () -> Unit,
    /** Lista desplegada y sus opciones de alta (CU-15 paso 3); null = ninguna. */
    alta: Pair<Long, OpcionesAltaEstado>? = null,
) {
    DialogoFormulario(
        titulo = stringResource(R.string.anadir_a_lista_titulo),
        icono = "playlist_add",
        onCerrar = onCerrar,
        tag = TagsDetalle.SELECTOR_LISTAS,
    ) {
        if (opciones.isEmpty()) {
            Text(
                text = stringResource(R.string.listas_vacia),
                style = CineTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = DimensLista.ResultadosMaxAlto),
                verticalArrangement = Arrangement.spacedBy(Spacing.Xs),
            ) {
                items(opciones, key = { it.lista.id }) { opcion ->
                    val desplegada = alta?.takeIf { it.first == opcion.lista.id }?.second
                    if (desplegada == null) {
                        FilaOpcion(opcion) { onElegir(opcion) }
                    } else {
                        OpcionDesplegada(opcion, desplegada) { onElegir(opcion) }
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = stringResource(R.string.comun_cerrar),
                style = CineTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .clip(CineShapes.Lg)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(role = Role.Button, onClick = onCerrar)
                        .padding(horizontal = DimensDialogoFormulario.BotonPaddingH, vertical = DimensDialogoFormulario.BotonPaddingV),
            )
        }
    }
}

/** Ranking o lista de prioridad elegida: su fila, la posición o el nivel y "Añadir" (CU-15 paso 3). */
@Composable
private fun OpcionDesplegada(
    opcion: OpcionLista,
    estado: OpcionesAltaEstado,
    onAnadir: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CineShapes.Lg)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(bottom = DimensLista.ResultadoPadding)
                .testTag(TagsDetalle.ALTA_DESPLEGADA),
        verticalArrangement = Arrangement.spacedBy(Spacing.Sm),
    ) {
        FilaOpcion(opcion, onAnadir)
        OpcionesAlta(estado, Modifier.padding(horizontal = DimensLista.ResultadoPadding))
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = DimensLista.ResultadoPadding), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = stringResource(R.string.alta_anadir),
                style = CineTheme.typography.labelMd,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier =
                    Modifier
                        .clip(CineShapes.Lg)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(role = Role.Button, onClick = onAnadir)
                        .padding(horizontal = DimensDialogoFormulario.BotonPaddingH, vertical = DimensDialogoFormulario.BotonPaddingV)
                        .testTag(TagsDetalle.CONFIRMAR_ALTA),
            )
        }
    }
}

@Composable
private fun FilaOpcion(
    opcion: OpcionLista,
    onClick: () -> Unit,
) {
    val noDisponible =
        when {
            opcion.contiene -> stringResource(R.string.selector_ya_en_lista)
            !opcion.admite -> stringResource(R.string.selector_no_admitido)
            else -> null
        }
    val lista = opcion.lista
    val punto =
        when (lista.tipo) {
            TipoLista.RANKING -> MaterialTheme.colorScheme.secondaryContainer
            TipoLista.PRIORIDAD -> MaterialTheme.colorScheme.primary
            TipoLista.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CineShapes.Lg)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(role = Role.Button, onClick = onClick)
                .then(if (noDisponible != null) Modifier.semantics { stateDescription = noDisponible } else Modifier)
                .alpha(if (noDisponible != null) CineAlpha.A40 else 1f)
                .padding(DimensLista.ResultadoPadding)
                .testTag(TagsDetalle.opcionLista(lista.id)),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(DimensDetalle.ListDot).clip(CineShapes.Full).background(punto))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lista.nombre,
                style = CineTheme.typography.titleSm,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    stringResource(
                        R.string.selector_detalle,
                        stringResource(lista.tipo.etiqueta()),
                        textoTotal(lista.total, lista.restriccion),
                    ),
                style = CineTheme.typography.labelSm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (noDisponible == null) {
            SymbolIcon("add_circle", stringResource(R.string.cd_anadir), DimensLista.ResultadoIcon, CineTheme.colors.sky400)
        }
    }
}
