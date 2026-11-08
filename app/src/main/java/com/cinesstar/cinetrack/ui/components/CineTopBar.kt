package com.cinesstar.cinetrack.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.CineTheme
import com.cinesstar.cinetrack.ui.theme.Dimens
import com.cinesstar.cinetrack.ui.theme.Spacing
import com.cinesstar.cinetrack.ui.theme.TrackingTight
import dev.chrisbanes.haze.HazeState

/** Contenedor común de las barras superiores: fondo translúcido con desenfoque, inset de estado y 52 dp de contenido. */
@Composable
private fun ContenedorBarraSuperior(
    hazeState: HazeState,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(Dimens.ShadowBar, clip = false, ambientColor = CineTheme.colors.black, spotColor = CineTheme.colors.black)
                .fondoBarra(hazeState, CineTheme.colors.barraSuperior)
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(Dimens.TopBarContentHeight)
                .padding(start = Spacing.Margin, end = Spacing.Margin, bottom = Dimens.TopBarBottomPadding)
                .testTag(TagsBarra.SUPERIOR),
        // Como en el prototipo: la fila (48 dp de texto) empieza arriba y puede invadir el padding inferior.
        contentAlignment = Alignment.TopCenter,
    ) { content() }
}

@Composable
private fun Logo() {
    Image(
        painter = painterResource(R.drawable.cinetrack_logo),
        contentDescription = stringResource(R.string.cd_logo),
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(Dimens.LogoHeight),
    )
}

/** Avatar decorativo (no hay cuentas): da acceso a Ajustes (design.md §3.2, §12, §13). */
@Composable
private fun Avatar(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .size(Dimens.AvatarSize)
                .clip(CineShapes.Full)
                .background(CineTheme.colors.sky400)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.cd_avatar), onClick = onClick)
                .testTag(TagsBarra.AVATAR),
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon("person", stringResource(R.string.cd_avatar), Dimens.AvatarIcon, CineTheme.colors.slate950)
    }
}

/** Barra superior de las pantallas principales: logo, marca, píldora "100% Local", nombre de pantalla y avatar (§3.2). */
@Composable
fun CineTopBar(
    titulo: String,
    hazeState: HazeState,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ContenedorBarraSuperior(hazeState, modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
            ) {
                Logo()
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
                        Text(
                            text = stringResource(R.string.marca),
                            style = CineTheme.typography.headlineMd,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = TrackingTight,
                            color = CineTheme.colors.sky400,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(R.string.pildora_local),
                            style = CineTheme.typography.labelSm,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier =
                                Modifier
                                    .clip(CineShapes.Full)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = Dimens.PillPaddingH, vertical = Dimens.PillPaddingV),
                        )
                    }
                    Text(
                        text = titulo,
                        style = CineTheme.typography.titleSm,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Avatar(onAvatarClick)
        }
    }
}

/** Barra superior de las pantallas secundarias: atrás, logo, título y avatar; sin barra inferior (§3.3). */
@Composable
fun CineTopBarSecondary(
    titulo: String,
    hazeState: HazeState,
    onBack: () -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ContenedorBarraSuperior(hazeState, modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(Dimens.BackButton)
                            .clip(CineShapes.Full)
                            .clickable(role = Role.Button, onClick = onBack)
                            .testTag(TagsBarra.ATRAS),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon("arrow_back", stringResource(R.string.cd_atras), Dimens.BackIcon, MaterialTheme.colorScheme.onSurface)
                }
                Logo()
                Text(
                    text = titulo,
                    style = CineTheme.typography.titleMd,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Avatar(onAvatarClick)
        }
    }
}
