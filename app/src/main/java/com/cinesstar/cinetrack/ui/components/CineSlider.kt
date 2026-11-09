package com.cinesstar.cinetrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.RangeSliderState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.cinesstar.cinetrack.ui.theme.CineShapes
import com.cinesstar.cinetrack.ui.theme.DimensComunes

/**
 * Slider del prototipo (`<input type="range">` con `accent-color`): pista de 8 dp redondeada, tramo recorrido en
 * [color] y pulgar circular del mismo color (design.md §7.3, §8.5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineSlider(
    valor: Float,
    onCambio: (Float) -> Unit,
    rango: ClosedFloatingPointRange<Float>,
    pasos: Int,
    color: Color,
    colorPista: Color,
    modifier: Modifier = Modifier,
) {
    Slider(
        value = valor,
        onValueChange = onCambio,
        valueRange = rango,
        steps = pasos,
        modifier = modifier.alturaVisual(DimensComunes.SliderAlturaVisual),
        thumb = { Box(Modifier.size(DimensComunes.SliderThumb).clip(CineShapes.Full).background(color)) },
        track = { estado -> Pista(estado, color, colorPista) },
    )
}

/**
 * Ocupa [alto] en el diseño (como el `<input type="range">` del prototipo) pero conserva la zona táctil de 48 dp del
 * Slider, que se extiende por encima y por debajo sin cambiar el tamaño visual (design.md §11).
 */
private fun Modifier.alturaVisual(alto: Dp): Modifier =
    layout { medible, restricciones ->
        val colocable = medible.measure(restricciones.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val visual = alto.roundToPx()
        layout(colocable.width, visual) { colocable.place(0, (visual - colocable.height) / 2) }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Pista(
    estado: SliderState,
    color: Color,
    colorPista: Color,
) {
    val inicio = estado.valueRange.start
    val fraccion = ((estado.value - inicio) / (estado.valueRange.endInclusive - inicio)).coerceIn(0f, 1f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(DimensComunes.SliderTrack)
            .clip(CineShapes.Lg)
            .background(colorPista),
    ) {
        Box(Modifier.fillMaxWidth(fraccion).fillMaxHeight().background(color))
    }
}

/**
 * Slider de rango con dos pulgares (design.md §9.2-3; el prototipo tiene uno solo, §12): misma pista de 8 dp que
 * [CineSlider], con el tramo entre los dos pulgares en [color]. [descripcionMinimo] y [descripcionMaximo] nombran cada
 * pulgar para los lectores de pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineRangeSlider(
    valor: ClosedFloatingPointRange<Float>,
    onCambio: (ClosedFloatingPointRange<Float>) -> Unit,
    rango: ClosedFloatingPointRange<Float>,
    pasos: Int,
    color: Color,
    colorPista: Color,
    descripcionMinimo: String,
    descripcionMaximo: String,
    modifier: Modifier = Modifier,
) {
    RangeSlider(
        value = valor,
        onValueChange = onCambio,
        valueRange = rango,
        steps = pasos,
        modifier = modifier.alturaVisual(DimensComunes.SliderAlturaVisual),
        startThumb = { Pulgar(color, descripcionMinimo) },
        endThumb = { Pulgar(color, descripcionMaximo) },
        track = { estado -> PistaRango(estado, color, colorPista) },
    )
}

@Composable
private fun Pulgar(
    color: Color,
    descripcion: String,
) {
    Box(
        Modifier
            .size(DimensComunes.SliderThumb)
            .clip(CineShapes.Full)
            .background(color)
            .semantics { contentDescription = descripcion },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PistaRango(
    estado: RangeSliderState,
    color: Color,
    colorPista: Color,
) {
    val inicio = estado.valueRange.start
    val ancho = estado.valueRange.endInclusive - inicio
    val desde = ((estado.activeRangeStart - inicio) / ancho).coerceIn(0f, 1f)
    val hasta = ((estado.activeRangeEnd - inicio) / ancho).coerceIn(desde, 1f)
    Row(
        Modifier
            .fillMaxWidth()
            .height(DimensComunes.SliderTrack)
            .clip(CineShapes.Lg)
            .background(colorPista),
    ) {
        if (desde > 0f) Spacer(Modifier.weight(desde))
        if (hasta > desde) Box(Modifier.weight(hasta - desde).fillMaxHeight().background(color))
        if (hasta < 1f) Spacer(Modifier.weight(1f - hasta))
    }
}
