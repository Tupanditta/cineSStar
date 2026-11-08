package com.cinesstar.cinetrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.Dimens
import dev.chrisbanes.haze.HazeState

/** Etiqueta de test de cada destino: "nav_biblioteca", "nav_anadir"… */
fun DestinoBarra.tag(): String = "nav_" + name.lowercase()

/**
 * Barra de navegación inferior con los 5 destinos y el botón central elevado "Añadir" (design.md §3.4).
 * [activo] es la pestaña principal que se está mostrando.
 */
@Composable
fun CineBottomBar(
    activo: DestinoBarra,
    hazeState: HazeState,
    onDestino: (DestinoBarra) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(Dimens.ShadowBar, clip = false, ambientColor = CineTheme.colors.black, spotColor = CineTheme.colors.black)
                .fondoBarra(hazeState, CineTheme.colors.barraInferior)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(Dimens.BottomBarHeight)
                .padding(horizontal = Dimens.BottomBarPaddingH)
                .testTag(TagsBarra.INFERIOR),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DestinoBarra.entries.forEach { destino ->
                if (destino == DestinoBarra.ANADIR) {
                    BotonAnadir(onClick = { onDestino(destino) })
                } else {
                    ElementoBarra(destino, seleccionado = destino == activo, onClick = { onDestino(destino) })
                }
            }
        }
    }
}

@Composable
private fun ElementoBarra(
    destino: DestinoBarra,
    seleccionado: Boolean,
    onClick: () -> Unit,
) {
    val color = if (seleccionado) CineTheme.colors.sky400 else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier =
            Modifier
                .defaultMinSize(minWidth = Dimens.NavItemMin, minHeight = Dimens.NavItemMin)
                .selectable(selected = seleccionado, role = Role.Tab, onClick = onClick)
                .testTag(destino.tag()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SymbolIcon(destino.icono, contentDescription = null, size = Dimens.NavIcon, tint = color)
        Text(
            text = stringResource(destino.etiqueta),
            style = CineTheme.typography.labelSm,
            fontWeight = if (seleccionado) FontWeight.Bold else CineTheme.typography.labelSm.fontWeight,
            color = color,
            maxLines = 1,
        )
    }
}

/**
 * Círculo de 48 dp con `-mt-5` (20 dp), sombra negra al 40 % y etiqueta debajo. En el prototipo la fila centra la caja
 * de margen del elemento (`items-center`), así que el desplazamiento visible es la mitad del margen: 10 dp.
 */
@Composable
private fun BotonAnadir(onClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .offset(y = -Dimens.NavFabLift / 2)
                .defaultMinSize(minWidth = Dimens.NavItemMin, minHeight = Dimens.NavItemMin)
                .clip(CineShapes.Xl)
                .clickable(role = Role.Button, onClick = onClick)
                .testTag(DestinoBarra.ANADIR.tag()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(Dimens.NavFab)
                    .shadow(
                        Dimens.ShadowLg,
                        CineShapes.Full,
                        ambientColor = CineTheme.colors.sombraFab,
                        spotColor = CineTheme.colors.sombraFab,
                    ).background(CineTheme.colors.sky400, CineShapes.Full),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(DestinoBarra.ANADIR.icono, contentDescription = null, size = Dimens.NavFabIcon, tint = CineTheme.colors.slate950)
        }
        Spacer(Modifier.height(Dimens.NavFabLabelGap))
        Text(
            text = stringResource(DestinoBarra.ANADIR.etiqueta),
            style = CineTheme.typography.labelSm,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
