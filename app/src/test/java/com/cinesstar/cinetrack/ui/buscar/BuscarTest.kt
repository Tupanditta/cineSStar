package com.cinesstar.cinetrack.ui.buscar

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.domain.Ambito
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.soporte.DatosFixture
import com.cinesstar.cinetrack.soporte.FixturePequeno
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.soporte.existeTexto
import com.cinesstar.cinetrack.soporte.textoEditable
import com.cinesstar.cinetrack.ui.components.tag
import com.cinesstar.cinetrack.ui.perfil.TagsDetalle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests UI del S7 sobre la pantalla Buscar (design.md §9, CU-20): T7.14–T7.16, T7.18–T7.21. Los que cuentan tarjetas
 * usan una ventana alta ([ALTA]) en la que caben los filtros y todos los resultados.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class BuscarTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private companion object {
        const val ALTA = "w411dp-h4000dp"
        const val PREFIJO_RESULTADO = "buscar_resultado_"
    }

    /** Carga el fixture pequeño y abre Buscar con todos los perfiles como resultado. */
    private fun abrir(): DatosFixture {
        val f = enBd { FixturePequeno.cargar(database) }
        compose.onNodeWithTag(DestinoBarra.BUSCAR.tag()).performClick()
        coincidencias(f.perfiles.size)
        return f
    }

    private fun coincidencias(n: Int) = compose.esperar { compose.existeTexto(if (n == 1) "1 coincidencia" else "$n coincidencias") }

    private fun nodo(tag: String): SemanticsNodeInteraction = compose.onNodeWithTag(tag)

    private fun ver(tag: String): SemanticsNodeInteraction {
        nodo(TagsBuscar.LISTA).performScrollToNode(hasTestTag(tag))
        compose.waitForIdle()
        return nodo(tag)
    }

    private fun tocar(tag: String) {
        ver(tag).performClick()
        compose.waitForIdle()
    }

    private fun escribir(
        tag: String,
        texto: String,
    ) {
        ver(tag).performTextReplacement(texto)
        compose.waitForIdle()
    }

    /** Ids de los resultados compuestos (en una ventana [ALTA], todos). */
    private fun resultados(): Set<Long> =
        compose
            .onAllNodes(hasTestTagPrefijo())
            .fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.TestTag].removePrefix(PREFIJO_RESULTADO).toLong() }
            .toSet()

    private fun hasTestTagPrefijo() =
        SemanticsMatcher("tag empieza por $PREFIJO_RESULTADO") { n ->
            n.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(PREFIJO_RESULTADO) == true
        }

    private fun DatosFixture.id(
        nombre: String,
        tipo: TipoPerfil = FixturePequeno.perfiles.first { it.nombre == nombre }.tipo,
    ): Long = perfiles.getValue("$nombre|$tipo")

    @Test
    fun t7_14_sinCoincidenciasMuestraM13() {
        abrir()
        escribir(TagsBuscar.NOMBRE, "zzz no existe")
        coincidencias(0)
        ver(TagsBuscar.SIN_RESULTADOS).assertIsDisplayed()
        compose.onNode(hasText("No se han encontrado resultados."), useUnmergedTree = true).assertIsDisplayed()
        assertTrue(resultados().isEmpty())
    }

    @Test
    fun t7_15_minimoMayorQueMaximoMuestraM14YNoBusca() {
        abrir()
        escribir(TagsBuscar.MINIMO, "8")
        escribir(TagsBuscar.MAXIMO, "5")
        compose.esperar { compose.existe(TagsBuscar.ERROR) }
        compose.onNode(hasText("La puntuación mínima no puede ser mayor que la máxima."), useUnmergedTree = true).assertExists()
        assertFalse("No se busca: no hay cabecera de resultados", compose.existe(TagsBuscar.RESULTADOS))
        assertFalse("Ni recuento", compose.existe(TagsBuscar.COINCIDENCIAS))
        assertTrue(resultados().isEmpty())
        // Al corregirlo vuelve a buscar.
        escribir(TagsBuscar.MAXIMO, "10")
        coincidencias(3) // 10, 9.0 y 8.9
    }

    @Test
    fun t7_16_valorDePuntuacionInvalidoMuestraM03() {
        abrir()
        listOf("abc", "7.25", "11", "-1").forEach { texto ->
            escribir(TagsBuscar.MINIMO, texto)
            compose.esperar { compose.existe(TagsBuscar.ERROR) }
            compose
                .onNode(
                    hasText("Puntuación no válida. Introduce un valor entre 0.0 y 10 con un máximo de un decimal."),
                    useUnmergedTree = true,
                ).assertExists()
            assertTrue("\"$texto\" no busca", resultados().isEmpty())
        }
        escribir(TagsBuscar.MINIMO, "7")
        coincidencias(4)
    }

    @Test
    fun t7_18_limpiarRestableceTodo() {
        val f = abrir()
        escribir(TagsBuscar.NOMBRE, "a")
        tocar(TagsBuscar.ambito(Ambito.SERIES))
        escribir(TagsBuscar.MINIMO, "3")
        val drama = enBd { catalogo.generos().single { it.nombre == "Drama" }.id }
        tocar(TagsBuscar.genero(drama))
        tocar(TagsBuscar.estado(Estado.PENDIENTE))
        coincidencias(1) // "El juego del calamar"
        compose.esperar { compose.existe(TagsBuscar.EXCLUYE) }

        tocar(TagsBuscar.LIMPIAR)
        coincidencias(f.perfiles.size)
        assertEquals("", ver(TagsBuscar.NOMBRE).textoEditable())
        assertEquals("0.0", ver(TagsBuscar.MINIMO).textoEditable())
        assertEquals("10", ver(TagsBuscar.MAXIMO).textoEditable())
        ver(TagsBuscar.ambito(Ambito.TODO)).assertIsSelected()
        ver(TagsBuscar.genero(drama)).assertIsOff()
        ver(TagsBuscar.estado(Estado.PENDIENTE)).assertIsOff()
        assertFalse("Sin rango activo", compose.existe(TagsBuscar.EXCLUYE))
    }

    @Test
    fun t7_19_pulsarUnResultadoAbreElDetalle() {
        val f = abrir()
        escribir(TagsBuscar.NOMBRE, "chihiro")
        coincidencias(1)
        // "Aplicar Filtros" lleva los resultados a lo alto de la pantalla.
        tocar(TagsBuscar.APLICAR)
        nodo(TagsBuscar.resultado(f.id("El viaje de Chihiro"))).assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.esperar { compose.existe(TagsDetalle.TITULO) }
        compose.onNode(hasText("El viaje de Chihiro"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun t7_20_masDeDiezResultadosConScroll() {
        val f = abrir()
        tocar(TagsBuscar.APLICAR)
        val visibles =
            f.perfiles.values.count { id ->
                compose.existe(TagsBuscar.resultado(id)) &&
                    nodo(TagsBuscar.resultado(id)).isDisplayed()
            }
        assertTrue("Como mucho 10 visibles a la vez: $visibles", visibles in 1..10)
        // El primero creado es el más antiguo y va el último (S-10): se llega a él desplazando.
        ver(TagsBuscar.resultado(f.id("Pádrino"))).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = ALTA)
    fun t7_21_elRecuentoCoincideConLasFilas() {
        val f = abrir()
        assertEquals(f.perfiles.values.toSet(), resultados())
        val drama = enBd { catalogo.generos().single { it.nombre == "Drama" }.id }
        tocar(TagsBuscar.genero(drama))
        ver(TagsBuscar.genero(drama)).assertIsOn()
        coincidencias(3)
        compose.waitForIdle()
        assertEquals(3, resultados().size)
        nodo(TagsBuscar.TOTAL_RESULTADOS).assertIsDisplayed()
        nodo(TagsBuscar.APLICAR).assertIsDisplayed()
        compose.onNode(hasText("Aplicar Filtros (3)"), useUnmergedTree = true).assertExists()
    }
}
