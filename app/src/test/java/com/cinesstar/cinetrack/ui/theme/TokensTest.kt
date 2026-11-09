package com.cinesstar.cinetrack.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.cinesstar.cinetrack.soporte.Proyecto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** T0.01 y T0.02: los tokens del tema coinciden con design.md §2.1–§2.6, leídos del propio documento. */
class TokensTest {
    private fun hex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

    private val paleta: Map<String, Color> =
        mapOf(
            "background" to CinePalette.Background,
            "surface" to CinePalette.Surface,
            "surface-dim" to CinePalette.SurfaceDim,
            "surface-container-lowest" to CinePalette.SurfaceContainerLowest,
            "surface-container-low" to CinePalette.SurfaceContainerLow,
            "surface-container" to CinePalette.SurfaceContainer,
            "surface-container-high" to CinePalette.SurfaceContainerHigh,
            "surface-container-highest" to CinePalette.SurfaceContainerHighest,
            "surface-bright" to CinePalette.SurfaceBright,
            "surface-variant" to CinePalette.SurfaceVariant,
            "on-surface" to CinePalette.OnSurface,
            "on-background" to CinePalette.OnBackground,
            "on-surface-variant" to CinePalette.OnSurfaceVariant,
            "outline" to CinePalette.Outline,
            "outline-variant" to CinePalette.OutlineVariant,
            "primary" to CinePalette.Primary,
            "on-primary" to CinePalette.OnPrimary,
            "primary-container" to CinePalette.PrimaryContainer,
            "on-primary-container" to CinePalette.OnPrimaryContainer,
            "secondary" to CinePalette.Secondary,
            "on-secondary" to CinePalette.OnSecondary,
            "secondary-container" to CinePalette.SecondaryContainer,
            "on-secondary-container" to CinePalette.OnSecondaryContainer,
            "tertiary" to CinePalette.Tertiary,
            "error" to CinePalette.Error,
            "on-error" to CinePalette.OnError,
            "error-container" to CinePalette.ErrorContainer,
            "on-error-container" to CinePalette.OnErrorContainer,
        )

    private val tailwind: Map<String, Color> =
        mapOf(
            "sky-200" to TailwindPalette.Sky200,
            "sky-300" to TailwindPalette.Sky300,
            "sky-400" to TailwindPalette.Sky400,
            "sky-500" to TailwindPalette.Sky500,
            "sky-950" to TailwindPalette.Sky950,
            "slate-950" to TailwindPalette.Slate950,
            "slate-700" to TailwindPalette.Slate700,
            "emerald-500" to TailwindPalette.Emerald500,
            "black" to TailwindPalette.Black,
        )

    /** Extrae `token` y `#HEX` de cada fila: la primera celda puede agrupar varios tokens ("`background` / `surface`"). */
    private fun tokensDeTabla(
        seccion: String,
        columnaHex: Int,
    ): Map<String, String> =
        Proyecto
            .filasTabla(Proyecto.seccionDesign(seccion))
            .flatMap { celdas ->
                val hexCanonico = Regex("#[0-9A-Fa-f]{6}").find(celdas[columnaHex])!!.value.uppercase()
                Regex("`([a-z0-9-]+)`").findAll(celdas[0]).map { it.groupValues[1] to hexCanonico }.toList()
            }.toMap()

    @Test
    @DisplayName("T0.01 §2.1: cada token de la paleta existe y coincide con su hex canónico")
    fun paletaDelTema() {
        val esperado = tokensDeTabla("### 2.1", columnaHex = 1)
        assertEquals(esperado.keys, paleta.keys, "Tokens de §2.1 sin definir o sobrantes")
        esperado.forEach { (token, valor) -> assertEquals(valor, hex(paleta.getValue(token)), "Token $token") }
    }

    @Test
    @DisplayName("T0.01 §2.2: colores de Tailwind usados directamente")
    fun coloresTailwind() {
        val esperado = tokensDeTabla("### 2.2", columnaHex = 1)
        assertEquals(esperado.keys, tailwind.keys)
        esperado.forEach { (token, valor) -> assertEquals(valor, hex(tailwind.getValue(token)), "Token $token") }
        val c = DefaultCineColors
        assertEquals(
            listOf(c.sky200, c.sky300, c.sky400, c.sky500, c.sky950, c.slate950, c.slate700, c.emerald500, c.black),
            tailwind.values.toList(),
        )
        assertEquals(hex(CinePalette.PrimaryContainer), hex(c.sky400), "sky-400 es igual que primary-container")
    }

    @Test
    @DisplayName("T0.01 §2.3: estados de visualización")
    fun estados() {
        val c = DefaultCineColors
        // Viendo: insignia surface-container-high + sky-400; chip primary-container 15 % + primary.
        assertEquals(ParColores(CinePalette.SurfaceContainerHigh, TailwindPalette.Sky400), c.estadoViendo.insignia)
        assertEquals(ParColores(CinePalette.PrimaryContainer.copy(alpha = 0.15f), CinePalette.Primary), c.estadoViendo.chip)
        assertEquals(TailwindPalette.Sky400, c.estadoViendo.icono)
        // Visto: insignia surface-container-highest + on-surface, icono sky-400; chip surface-container-highest + on-surface-variant.
        assertEquals(ParColores(CinePalette.SurfaceContainerHighest, CinePalette.OnSurface), c.estadoVisto.insignia)
        assertEquals(ParColores(CinePalette.SurfaceContainerHighest, CinePalette.OnSurfaceVariant), c.estadoVisto.chip)
        assertEquals(TailwindPalette.Sky400, c.estadoVisto.icono)
        // Pendiente: surface-container-highest + secondary en insignia y chip.
        assertEquals(ParColores(CinePalette.SurfaceContainerHighest, CinePalette.Secondary), c.estadoPendiente.insignia)
        assertEquals(ParColores(CinePalette.SurfaceContainerHighest, CinePalette.Secondary), c.estadoPendiente.chip)
    }

    @Test
    @DisplayName("T0.01 §2.4: prioridad")
    fun prioridad() {
        val c = DefaultCineColors
        assertEquals(ParColores(CinePalette.ErrorContainer.copy(alpha = 0.30f), CinePalette.Error), c.prioridadAlta)
        assertEquals(ParColores(TailwindPalette.Sky500.copy(alpha = 0.20f), TailwindPalette.Sky300), c.prioridadMedia)
        assertEquals(ParColores(CinePalette.SurfaceContainerHighest, CinePalette.OnSurfaceVariant), c.prioridadBaja)
    }

    @Test
    @DisplayName("T0.01: el esquema de Material 3 usa la paleta")
    fun esquemaMaterial() {
        val s = CineColorScheme
        assertEquals(CinePalette.Background, s.background)
        assertEquals(CinePalette.SurfaceContainerLowest, s.surfaceContainerLowest)
        assertEquals(CinePalette.SurfaceContainerHighest, s.surfaceContainerHighest)
        assertEquals(CinePalette.PrimaryContainer, s.primaryContainer)
        assertEquals(CinePalette.OnSurfaceVariant, s.onSurfaceVariant)
        assertEquals(CinePalette.ErrorContainer, s.errorContainer)
        assertEquals(CinePalette.Outline, s.outline)
    }

    private val tipografia = CineTypography()

    private val estilos: Map<String, TextStyle> =
        mapOf(
            "display-lg" to tipografia.displayLg,
            "headline-lg-mobile" to tipografia.headlineLgMobile,
            "headline-md" to tipografia.headlineMd,
            "title-md" to tipografia.titleMd,
            "title-sm" to tipografia.titleSm,
            "body-md" to tipografia.bodyMd,
            "body-sm" to tipografia.bodySm,
            "label-lg" to tipografia.labelLg,
            "label-md" to tipografia.labelMd,
            "label-sm" to tipografia.labelSm,
        )

    @Test
    @DisplayName("T0.02 §2.5: tamaño, interlínea, peso y tracking de cada estilo")
    fun tipografia() {
        val filas = Proyecto.filasTabla(Proyecto.seccionDesign("### 2.5"))
        assertEquals(estilos.keys, filas.map { it[0] }.toSet())
        filas.forEach { celdas ->
            val estilo = estilos.getValue(celdas[0])
            val (tamano, interlinea) = celdas[1].split("/").map { it.trim().toInt() }
            val peso = Regex("\\d{3}").find(celdas[2])!!.value.toInt()
            val tracking = celdas[3].replace("−", "-").removeSuffix("em").toDoubleOrNull() ?: 0.0
            assertEquals(tamano.sp, estilo.fontSize, "${celdas[0]} tamaño")
            assertEquals(interlinea.sp, estilo.lineHeight, "${celdas[0]} interlínea")
            assertEquals(FontWeight(peso), estilo.fontWeight, "${celdas[0]} peso")
            assertEquals(tracking.em, estilo.letterSpacing, "${celdas[0]} tracking")
            assertEquals(InterFamily, estilo.fontFamily, "${celdas[0]} usa Inter")
        }
        // Notas de §2.5: etiquetas de sección con tracking-wider y headline-lg (30/36) para tablet.
        assertEquals(0.05.em, tipografia.sectionLabel.letterSpacing)
        assertEquals(13.sp, tipografia.sectionLabel.fontSize)
        assertEquals(30.sp, tipografia.headlineLg.fontSize)
        assertEquals(36.sp, tipografia.headlineLg.lineHeight)
    }

    @Test
    @DisplayName("T0.02 §2.6: radios")
    fun radios() {
        val radios: Map<String, Dp> =
            mapOf(
                "rounded" to CineRadii.Rounded,
                "rounded-md" to CineRadii.Md,
                "rounded-lg" to CineRadii.Lg,
                "rounded-xl" to CineRadii.Xl,
                "rounded-2xl" to CineRadii.Xxl,
            )
        val filas = Proyecto.filasTabla(Proyecto.seccionDesign("### 2.6"))
        filas.forEach { celdas ->
            val clase = celdas[0].trim('`')
            if (clase == "rounded-full") {
                assertEquals(CircleShape, CineShapes.Full)
            } else {
                assertEquals(celdas[1].toFloat(), radios.getValue(clase).value, clase)
            }
        }
        assertEquals(radios.size + 1, filas.size, "Todas las clases de §2.6 tienen radio")
    }

    @Test
    @DisplayName("T0.02 §2.7: espaciado base 4 dp y margen 16")
    fun espaciado() {
        assertEquals(listOf(4f, 8f, 16f, 24f, 32f), listOf(Spacing.Xs, Spacing.Sm, Spacing.Md, Spacing.Lg, Spacing.Xl).map { it.value })
        assertEquals(16f, Spacing.Margin.value)
    }
}
