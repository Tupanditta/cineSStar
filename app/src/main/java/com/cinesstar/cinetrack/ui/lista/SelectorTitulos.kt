package com.cinesstar.cinetrack.ui.lista

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.domain.Candidato
import com.cinesstar.cinetrack.ui.components.CampoTexto
import com.cinesstar.cinetrack.ui.components.DialogoFormulario
import com.cinesstar.cinetrack.ui.components.EtiquetaCampo
import com.cinesstar.cinetrack.ui.components.SymbolIcon
import com.cinesstar.cinetrack.ui.components.etiqueta
import com.cinesstar.cinetrack.ui.components.icono
import com.cinesstar.cinetrack.ui.theme.CineAlpha
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.DimensDialogoFormulario
import com.cinesstar.cinetrack.ui.theme.DimensLista
import com.cinesstar.cinetrack.ui.theme.Spacing

/**
 * Diálogo "Añadir título" (design.md §6.6, CU-15): búsqueda en la Biblioteca sin distinguir mayúsculas ni tildes (A5),
 * y se pueden añadir varios seguidos sin cerrarlo (A3). Los perfiles que ya están o que la restricción no admite se ven
 * al 40 % y sin icono; si se tocan, se explica por qué (M-10, M-09) y no se añaden.
 */
@Composable
fun SelectorTitulos(
    nombreLista: String,
    busqueda: String,
    candidatos: List<Candidato>,
    vm: ListaViewModel,
) {
    DialogoFormulario(
        titulo = stringResource(R.string.selector_titulo, nombreLista),
        icono = "playlist_add",
        onCerrar = vm::cerrarSelector,
        tag = TagsLista.SELECTOR,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
            EtiquetaCampo(stringResource(R.string.selector_buscar))
            CampoTexto(
                valor = busqueda,
                onCambio = vm::buscar,
                placeholder = stringResource(R.string.selector_placeholder),
                modifier = Modifier.fillMaxWidth().testTag(TagsLista.SELECTOR_BUSCAR),
                estilo = CineTheme.typography.bodySm,
                forma = CineShapes.Lg,
                fondo = MaterialTheme.colorScheme.surfaceContainer,
                fondoEnfocado = MaterialTheme.colorScheme.surfaceContainerLowest,
                relleno =
                    PaddingValues(
                        start = DimensLista.BuscarPaddingStart,
                        end = DimensDialogoFormulario.CampoPadding,
                        top = DimensDialogoFormulario.CampoPadding,
                        bottom = DimensDialogoFormulario.CampoPadding,
                    ),
                teclado = KeyboardOptions(imeAction = ImeAction.Search),
                principio = {
                    SymbolIcon(
                        "search",
                        null,
                        DimensLista.BuscarIcon,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        Modifier.align(Alignment.CenterStart).padding(start = DimensLista.BuscarIconStart),
                    )
                },
            )
        }
        if (candidatos.isEmpty()) {
            Text(
                text = stringResource(R.string.m13_sin_resultados),
                style = CineTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(TagsLista.SELECTOR_SIN_RESULTADOS),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = DimensLista.ResultadosMaxAlto).testTag(TagsLista.SELECTOR_RESULTADOS),
                verticalArrangement = Arrangement.spacedBy(Spacing.Xs),
            ) {
                items(candidatos, key = { it.perfilId }) { candidato -> FilaCandidato(candidato) { vm.anadir(candidato) } }
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
                        .clickable(role = Role.Button, onClick = vm::cerrarSelector)
                        .padding(horizontal = DimensDialogoFormulario.BotonPaddingH, vertical = DimensDialogoFormulario.BotonPaddingV)
                        .testTag(TagsLista.SELECTOR_CERRAR),
            )
        }
    }
}

@Composable
private fun FilaCandidato(
    candidato: Candidato,
    onClick: () -> Unit,
) {
    val noDisponible =
        when {
            candidato.enLista -> stringResource(R.string.selector_ya_en_lista)
            !candidato.admitido -> stringResource(R.string.selector_no_admitido)
            else -> null
        }
    val tipo = stringResource(candidato.tipo.etiqueta())
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CineShapes.Lg)
                .clickable(role = Role.Button, onClick = onClick)
                .then(if (noDisponible != null) Modifier.semantics { stateDescription = noDisponible } else Modifier)
                .alpha(if (noDisponible != null) CineAlpha.A40 else 1f)
                .padding(DimensLista.ResultadoPadding)
                .testTag(TagsLista.candidato(candidato.perfilId)),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(DimensLista.MiniaturaAncho, DimensLista.MiniaturaAlto)
                    .clip(CineShapes.Rounded)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(candidato.tipo.icono(), null, DimensLista.MiniaturaIcon, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidato.nombre,
                style = CineTheme.typography.titleSm,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = candidato.detalle?.let { stringResource(R.string.selector_detalle, tipo, it) } ?: tipo,
                style = CineTheme.typography.labelSm,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (noDisponible == null) {
            SymbolIcon(
                "add_circle",
                stringResource(R.string.cd_anadir),
                DimensLista.ResultadoIcon,
                CineTheme.colors.sky400,
                Modifier.testTag(TagsLista.candidatoAnadir(candidato.perfilId)),
            )
        }
    }
}
