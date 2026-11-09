package com.cinesstar.cinetrack.ui.biblioteca

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.AppContainer
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.repository.BibliotecaRepository
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.room.BibliotecaDao
import com.cinesstar.cinetrack.data.room.FilaBiblioteca
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.soporte.FixtureGrande
import com.cinesstar.cinetrack.soporte.aplicacion
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.existe
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Tests de la Biblioteca que necesitan preparar las dependencias o los datos antes de abrir la app: T4.18 (error de
 * lectura) y T4.19 (rendimiento con el fixture grande).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class BibliotecaArranqueTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun t4_18_errorDeLecturaMuestraM25YReintentar() {
        val real = aplicacion.container
        val id = enBd { perfiles.crear(NuevoPerfil("Recuperado", TipoPerfil.PELICULA)) }
        var fallar = true
        val dao =
            object : BibliotecaDao by real.database.bibliotecaDao() {
                override suspend fun filasBiblioteca(): List<FilaBiblioteca> {
                    if (fallar) throw IOException("Lectura simulada fallida")
                    return real.database.bibliotecaDao().filasBiblioteca()
                }
            }
        aplicacion.reemplazarContenedor(AppContainer(real.database, biblioteca = BibliotecaRepository(real.database, dao = dao)))

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.esperar { compose.existe(TagsBiblioteca.ERROR) }
            compose.onNode(hasText("Error inesperado. Tus datos no se han modificado.")).assertIsDisplayed()
            compose.onNodeWithTag(TagsBiblioteca.REINTENTAR).assertIsDisplayed()

            // Un reintento que vuelve a fallar sigue mostrando el error.
            compose.onNodeWithTag(TagsBiblioteca.REINTENTAR).performClick()
            compose.waitForIdle()
            compose.esperar { compose.existe(TagsBiblioteca.ERROR) }

            fallar = false
            compose.onNodeWithTag(TagsBiblioteca.REINTENTAR).performClick()
            compose.esperar { compose.existe(TagsBiblioteca.fila(id)) }
            assertTrue(!compose.existe(TagsBiblioteca.ERROR))
        }
    }

    /**
     * RNF-08: con 1000 perfiles, de abrir la app a ver la primera tarjeta en menos de 1 s. En Robolectric la primera
     * actividad de cada proceso carga Compose, las fuentes y Room en frío (varios segundos que no existen en el
     * dispositivo): se abre una vez para calentar y se mide la segunda apertura. La medida en dispositivo es la del S9.
     */
    @Test
    fun t4_19_abrirLaBibliotecaConElFixtureGrandeTardaMenosDeUnSegundo() {
        val datos = enBd { FixtureGrande().cargar(database) }
        val masReciente = enBd { biblioteca.leer().first().id }
        assertTrue(datos.perfiles.size == FixtureGrande.PERFILES)

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.esperar { compose.existe(TagsBiblioteca.fila(masReciente)) }
        }

        val inicio = System.nanoTime()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(LIMITE_MS * 5) { compose.existe(TagsBiblioteca.fila(masReciente)) }
            val ms = (System.nanoTime() - inicio) / 1_000_000
            println("T4.19 Biblioteca con ${FixtureGrande.PERFILES} perfiles: $ms ms")
            assertTrue("Primera pantalla en $ms ms (máximo $LIMITE_MS)", ms < LIMITE_MS)
        }
    }

    private companion object {
        const val LIMITE_MS = 1_000L
    }
}
