package com.cinesstar.cinetrack.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.ui.components.TagsBarra
import com.cinesstar.cinetrack.ui.components.tag
import com.cinesstar.cinetrack.ui.components.tagPantalla
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** T0.04–T0.07: barra inferior y navegación (Robolectric, sin emulador). T0.03 está en [ArranqueTest]. */
@RunWith(AndroidJUnit4::class)
class NavegacionTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun pantalla(destino: DestinoBarra) = compose.onNodeWithTag(destino.tagPantalla())

    private fun atras() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun t0_04_barraInferiorConLosCincoDestinosEnOrden() {
        val etiquetas = listOf("Biblioteca", "Listas", "Añadir", "Buscar", "Ajustes")
        val destinos = DestinoBarra.entries
        assertEquals(etiquetas.size, destinos.size)
        val posiciones =
            destinos.mapIndexed { i, destino ->
                val nodo = compose.onNode(hasTestTag(destino.tag()) and hasText(etiquetas[i]), useUnmergedTree = false)
                nodo.assertIsDisplayed()
                nodo.fetchSemanticsNode().boundsInRoot.left
            }
        assertEquals("Los destinos van de izquierda a derecha en este orden", posiciones.sorted(), posiciones)
        compose.onAllNodesWithTag(TagsBarra.INFERIOR).assertCountEquals(1)
    }

    @Test
    fun t0_05_cadaDestinoCambiaDePantallaYSoloSeResaltaEse() {
        val pestanas = DestinoBarra.entries.filter { it.esPestana }
        for (destino in pestanas.reversed()) {
            compose.onNodeWithTag(destino.tag()).performClick()
            compose.waitForIdle()
            pantalla(destino).assertIsDisplayed()
            pestanas.forEach { otro ->
                val nodo = compose.onNodeWithTag(otro.tag())
                if (otro == destino) nodo.assertIsSelected() else nodo.assertIsNotSelected()
            }
            // El nombre de la pantalla aparece bajo la marca (fila 2 de la barra superior, §3.2).
            compose
                .onNode(hasText(etiqueta(destino)) and hasAnyAncestor(hasTestTag(TagsBarra.SUPERIOR)), useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun t0_06_anadirAbreLaPantallaDeAltaSinBarraInferior() {
        compose.onNodeWithTag(DestinoBarra.ANADIR.tag()).performClick()
        compose.waitForIdle()
        pantalla(DestinoBarra.ANADIR).assertIsDisplayed()
        compose.onNodeWithText("Añadir Medio").assertIsDisplayed()
        compose.onAllNodesWithTag(TagsBarra.INFERIOR).assertCountEquals(0)
        compose.onNodeWithTag(TagsBarra.ATRAS).assertIsDisplayed()
    }

    @Test
    fun t0_07_atrasVuelveALaAnteriorYDesdeBibliotecaSaleDeLaApp() {
        // Listas → Añadir → atrás (sistema) → Listas
        compose.onNodeWithTag(DestinoBarra.LISTAS.tag()).performClick()
        compose.onNodeWithTag(DestinoBarra.ANADIR.tag()).performClick()
        compose.waitForIdle()
        pantalla(DestinoBarra.ANADIR).assertIsDisplayed()
        atras()
        pantalla(DestinoBarra.LISTAS).assertIsDisplayed()

        // Añadir → botón atrás de la barra → Listas
        compose.onNodeWithTag(DestinoBarra.ANADIR.tag()).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(TagsBarra.ATRAS).performClick()
        compose.waitForIdle()
        pantalla(DestinoBarra.LISTAS).assertIsDisplayed()

        // Desde una pestaña, atrás vuelve a Biblioteca (las pestañas no se apilan).
        compose.onNodeWithTag(DestinoBarra.BUSCAR.tag()).performClick()
        compose.waitForIdle()
        atras()
        pantalla(DestinoBarra.BIBLIOTECA).assertIsDisplayed()
        var terminando = false
        compose.activityRule.scenario.onActivity { terminando = it.isFinishing }
        assertFalse(terminando)

        // Desde Biblioteca, atrás sale de la app.
        compose.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
            terminando = it.isFinishing
        }
        assertTrue("Atrás desde Biblioteca cierra la actividad", terminando)
    }

    private fun etiqueta(destino: DestinoBarra): String = compose.activity.getString(destino.etiqueta)
}
