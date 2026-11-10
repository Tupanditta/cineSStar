package com.cinesstar.cinetrack.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * T9.11: contraste AA (WCAG 2.1, 4.5:1 para texto normal) de cada combinación de texto y fondo que usan las pantallas
 * (design.md §2–§10). Los fondos translúcidos se componen sobre la superficie en la que se pintan.
 */
class ContrasteTest {
    private data class Par(
        val uso: String,
        val texto: Color,
        val fondo: Color,
    ) {
        val ratio: Double
            get() {
                val a = texto.luminance() + CLARO
                val b = fondo.luminance() + CLARO
                return maxOf(a, b).toDouble() / minOf(a, b)
            }
    }

    private val p = CinePalette
    private val c = DefaultCineColors
    private val tarjeta = p.SurfaceContainer

    private val pares =
        buildList {
            listOf(
                p.Background,
                p.SurfaceContainerLow,
                p.SurfaceContainer,
                p.SurfaceContainerHigh,
                p.SurfaceContainerHighest,
                p.SurfaceBright,
            ).forEach { fondo ->
                add(Par("on-surface", p.OnSurface, fondo))
                add(Par("on-surface-variant", p.OnSurfaceVariant, fondo))
                add(Par("primary", p.Primary, fondo))
                add(Par("tertiary", p.Tertiary, fondo))
                add(Par("error", p.Error, fondo))
                add(Par("secondary", p.Secondary, fondo))
                add(Par("sky-400", c.sky400, fondo))
            }
            add(Par("outline (sufijos de etiqueta)", p.Outline, p.Background))
            add(Par("on-primary-container / primary-container", p.OnPrimaryContainer, p.PrimaryContainer))
            add(Par("slate-950 / sky-400 (botones de Listas y Biblioteca)", c.slate950, c.sky400))
            add(Par("on-primary / primary", p.OnPrimary, p.Primary))
            add(Par("botón Quitar (§3.6-3)", p.OnSecondaryContainer, p.SecondaryContainer))
            add(Par("on-error / error", p.OnError, p.Error))
            add(Par("on-error-container / error-container", p.OnErrorContainer, p.ErrorContainer))
            add(
                Par(
                    "nota de resultado (primary sobre primary-container 20 %)",
                    p.Primary,
                    p.PrimaryContainer.copy(alpha = CineAlpha.A20).compositeOver(tarjeta),
                ),
            )
            listOf(c.estadoViendo, c.estadoVisto, c.estadoPendiente).forEach { e ->
                add(Par("insignia de estado", e.insignia.texto, e.insignia.fondo.compositeOver(tarjeta)))
                add(Par("chip de estado", e.chip.texto, e.chip.fondo.compositeOver(tarjeta)))
            }
            listOf(c.prioridadAlta, c.prioridadMedia, c.prioridadBaja).forEach {
                add(Par("chip de prioridad", it.texto, it.fondo.compositeOver(tarjeta)))
            }
        }

    @Test
    fun t9_11_todosLosTextosCumplenAA() {
        val fallan = pares.filter { it.ratio < AA }.map { "${it.uso}: ${"%.2f".format(it.ratio)}:1" }
        assertEquals(emptyList<String>(), fallan, "Combinaciones por debajo de $AA:1")
    }

    private companion object {
        const val AA = 4.5
        const val CLARO = 0.05f
    }
}
