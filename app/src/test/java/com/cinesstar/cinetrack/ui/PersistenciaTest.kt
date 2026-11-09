package com.cinesstar.cinetrack.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.AppContainer
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.soporte.RobotFormulario
import com.cinesstar.cinetrack.soporte.aplicacion
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.soporte.perfilesGuardados
import com.cinesstar.cinetrack.ui.biblioteca.TagsBiblioteca
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T2.30 (E, RNF-07): crear un perfil, "matar" la app (se cierra la actividad y la base de datos y se crean unas
 * dependencias nuevas sobre el mismo archivo) y volver a abrirla: el perfil sigue ahí.
 */
@RunWith(AndroidJUnit4::class)
class PersistenciaTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun t2_30_elPerfilSigueTrasReiniciarLaApp() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val form = RobotFormulario(compose)
            form.abrir()
            form.nombre("Persistente")
            form.puntuacion("6.5")
            form.guardar()
        }
        aplicacion.container.database.close()
        aplicacion.reemplazarContenedor(AppContainer(aplicacion))

        ActivityScenario.launch(MainActivity::class.java).use {
            val perfil = perfilesGuardados().single()
            assertEquals("Persistente" to 65, perfil.nombre to perfil.puntuacionDecimas)
            compose.esperar { compose.existe(TagsBiblioteca.fila(perfil.id)) }
            compose.onNodeWithTag(TagsBiblioteca.fila(perfil.id)).assertIsDisplayed()
            compose.onNode(hasText("Persistente"), useUnmergedTree = true).assertIsDisplayed()
        }
    }
}
