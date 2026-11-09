package com.cinesstar.cinetrack.ui.biblioteca

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.contenedor
import com.cinesstar.cinetrack.data.room.PerfilEntity
import com.cinesstar.cinetrack.ui.components.SymbolIcon
import com.cinesstar.cinetrack.ui.components.etiqueta
import com.cinesstar.cinetrack.ui.components.icono
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.Dimens
import com.cinesstar.cinetrack.ui.theme.DimensBiblioteca
import com.cinesstar.cinetrack.ui.theme.Spacing

/**
 * Biblioteca provisional del S2: lista simple de perfiles (más recientes primero, S-10) para poder abrir su detalle, y
 * el estado vacío de design.md §11 (M-22). El S4 la sustituye por la pantalla completa de design.md §4.
 */
@Composable
fun ContenidoBiblioteca(
    onPerfil: (Long) -> Unit,
    onAnadir: () -> Unit,
) {
    val contenedor = LocalContext.current.contenedor
    val flujo = remember(contenedor) { contenedor.perfiles.observarTodos() }
    val perfiles by flujo.collectAsStateWithLifecycle(initialValue = null)
    val arriba = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Dimens.TopBarContentHeight
    val abajo = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Dimens.ContentBottomPadding
    when {
        perfiles == null -> Unit
        perfiles.isNullOrEmpty() -> Vacia(onAnadir, Modifier.padding(top = arriba, bottom = abajo))
        else ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag(TagsBiblioteca.LISTA),
                contentPadding = PaddingValues(start = Spacing.Margin, end = Spacing.Margin, top = arriba + Spacing.Sm, bottom = abajo),
                verticalArrangement = Arrangement.spacedBy(Spacing.Sm),
            ) {
                items(perfiles.orEmpty(), key = { it.id }) { perfil -> Fila(perfil) { onPerfil(perfil.id) } }
            }
    }
}

@Composable
private fun Fila(
    perfil: PerfilEntity,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CineShapes.Xl)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(DimensBiblioteca.RowPadding)
                .testTag(TagsBiblioteca.fila(perfil.id)),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(perfil.tipo.icono(), null, Dimens.NavIcon, CineTheme.colors.sky400)
        Text(
            text = perfil.nombre,
            style = CineTheme.typography.titleMd,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(perfil.tipo.etiqueta()),
            style = CineTheme.typography.labelSm,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** design.md §11: icono `video_library`, M-22 y botón "Añadir". */
@Composable
private fun Vacia(
    onAnadir: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.Margin).testTag(TagsBiblioteca.VACIA),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Md, Alignment.CenterVertically),
    ) {
        SymbolIcon("video_library", null, DimensBiblioteca.EmptyIcon, MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(R.string.m22_biblioteca_vacia),
            style = CineTheme.typography.titleSm,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier =
                Modifier
                    .clip(CineShapes.Full)
                    .background(CineTheme.colors.sky400)
                    .clickable(role = Role.Button, onClick = onAnadir)
                    .padding(horizontal = DimensBiblioteca.EmptyButtonPaddingH, vertical = DimensBiblioteca.EmptyButtonPaddingV)
                    .testTag(TagsBiblioteca.ANADIR),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon("add", null, Dimens.NavIcon, CineTheme.colors.slate950)
            Text(text = stringResource(R.string.biblioteca_anadir), style = CineTheme.typography.labelLg, color = CineTheme.colors.slate950)
        }
    }
}
