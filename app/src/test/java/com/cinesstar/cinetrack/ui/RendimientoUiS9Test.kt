package com.cinesstar.cinetrack.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.AppContainer
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.soporte.FixtureGrande
import com.cinesstar.cinetrack.soporte.aplicacion
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.ui.biblioteca.TagsBiblioteca
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests R del S9 sobre la interfaz con 1000 perfiles. En Robolectric la primera actividad de cada proceso carga Compose,
 * las fuentes y Room en frío (segundos que no existen en el dispositivo), así que se abre una vez para calentar y se
 * mide después. Son una aproximación: la medida definitiva de T9.05 y T9.06 es en dispositivo (lista de comprobación
 * de `SOFTWARE/pruebasManuales.md`).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class RendimientoUiS9Test {
    @get:Rule
    val compose = createEmptyComposeRule()

    private fun calentar(primero: Long) {
        ActivityScenario.launch(MainActivity::class.java).use { compose.esperar { compose.existe(TagsBiblioteca.fila(primero)) } }
    }

    /**
     * T9.06: arranque con 1000 perfiles < 2 s. "En frío" respecto a los datos: se descarta el contenedor (y con él la
     * base de datos abierta y sus sentencias compiladas) y se crea otro, como en un proceso nuevo.
     */
    @Test
    fun t9_06_arranqueCon1000PerfilesTardaMenosDeDosSegundos() {
        enBd { FixtureGrande().cargar(database) }
        val primero = enBd { biblioteca.leer().first().id }
        calentar(primero)
        aplicacion.container.database.close()

        val inicio = System.nanoTime()
        aplicacion.reemplazarContenedor(AppContainer(aplicacion))
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(LIMITE_ARRANQUE_MS * 5) { compose.existe(TagsBiblioteca.fila(primero)) }
            val ms = (System.nanoTime() - inicio) / NANOS_POR_MS
            println("T9.06 arranque con base de datos nueva y ${FixtureGrande.PERFILES} perfiles: $ms ms")
            assertTrue("Arranque en $ms ms (máximo $LIMITE_ARRANQUE_MS)", ms < LIMITE_ARRANQUE_MS)
        }
    }

    /**
     * T9.05 (aproximación): la Biblioteca con 1000 perfiles se recorre entera con scroll y no se atasca. Los fps solo se
     * miden en dispositivo; aquí se informa del tiempo medio por tarjeta recorrida.
     */
    @Test
    fun t9_05_laBibliotecaCon1000PerfilesSeRecorreEntera() {
        enBd { FixtureGrande().cargar(database) }
        val perfiles = enBd { biblioteca.leer() }
        calentar(perfiles.first().id)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.esperar { compose.existe(TagsBiblioteca.fila(perfiles.first().id)) }
            val inicio = System.nanoTime()
            compose.onNodeWithTag(TagsBiblioteca.LISTA).performScrollToNode(hasTestTag(TagsBiblioteca.fila(perfiles.last().id)))
            compose.waitForIdle()
            val ms = (System.nanoTime() - inicio) / NANOS_POR_MS
            println("T9.05 scroll de ${perfiles.size} tarjetas: $ms ms (${ms.toDouble() / perfiles.size} ms por tarjeta)")
            assertTrue(compose.existe(TagsBiblioteca.fila(perfiles.last().id)))
        }
    }

    private companion object {
        const val LIMITE_ARRANQUE_MS = 2_000L
        const val NANOS_POR_MS = 1_000_000L
    }
}
