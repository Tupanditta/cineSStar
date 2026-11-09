package com.cinesstar.cinetrack.ui.listas

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.AppContainer
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.repository.ListaRepository
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.room.ListaDao
import com.cinesstar.cinetrack.data.room.ListaEntity
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.soporte.RobotListas
import com.cinesstar.cinetrack.soporte.aplicacion
import com.cinesstar.cinetrack.soporte.elementosDe
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.esperarToast
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.soporte.listaGuardada
import com.cinesstar.cinetrack.ui.components.TagsDialogo
import com.cinesstar.cinetrack.ui.components.TagsHoja
import com.cinesstar.cinetrack.ui.lista.TagsLista
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Tests de listas que preparan las dependencias antes de abrir la app: T5.33 (fallos de escritura simulados) y T5.34
 * (persistencia tras reiniciar, E).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class ListasArranqueTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val listas = RobotListas(compose)

    /** Sustituye el DAO de listas por [fallo] (antes de abrir la app). */
    private fun conDaoQueFalla(fallo: (ListaDao) -> ListaDao) {
        val real = aplicacion.container
        aplicacion.reemplazarContenedor(
            AppContainer(real.database, listas = ListaRepository(real.database, listaDao = fallo(real.database.listaDao()))),
        )
    }

    @Test
    fun t5_33_unFalloDeEscrituraMuestraM25YNoCambiaNada() {
        val p = enBd { perfiles.crear(NuevoPerfil("Alien", TipoPerfil.PELICULA)) }
        val id = enBd { listas.crear("Existente", TipoLista.NORMAL).also { listas.anadir(it, p) } }
        conDaoQueFalla { dao ->
            object : ListaDao by dao {
                override suspend fun insertar(lista: ListaEntity): Long = throw IOException("Disco lleno")

                override suspend fun eliminarItem(
                    listaId: Long,
                    perfilId: Long,
                ): Int = throw IOException("Disco lleno")
            }
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            listas.abrir()
            // CU-11 E3: el diálogo sigue abierto y no se crea la lista.
            listas.abrirNueva()
            listas.escribirNombre("Nueva")
            listas.tocar(TagsListas.tipo(TipoLista.RANKING))
            listas.tocar(TagsListas.restriccion(Restriccion.SOLO_SERIES))
            compose.esperarToast("Error inesperado. Tus datos no se han modificado.") { listas.tocar(TagsDialogo.CONFIRMAR) }
            assertTrue(compose.existe(TagsListas.DIALOGO_NUEVA))
            assertNull(listaGuardada("Nueva"))
            listas.tocar(TagsDialogo.CANCELAR)
            compose.esperar { !compose.existe(TagsListas.DIALOGO_NUEVA) }

            // CU-16 E2: el elemento sigue en la lista.
            listas.abrirLista(id)
            listas.pedirQuitar(p)
            compose.esperarToast("Error inesperado. Tus datos no se han modificado.") { listas.tocar(TagsHoja.ACCION) }
            assertEquals(listOf(p), elementosDe(id))
            listas.nodo(TagsLista.fila(p)).assertIsDisplayed()
        }
    }

    @Test
    fun t5_34_laListaYSusElementosSiguenTrasReiniciarLaApp() {
        val ids = listOf("Uno", "Dos").map { enBd { perfiles.crear(NuevoPerfil(it, TipoPerfil.SERIE)) } }
        var id = 0L
        ActivityScenario.launch(MainActivity::class.java).use {
            listas.abrir()
            id = listas.crear("Persistente", TipoLista.RANKING, Restriccion.SOLO_SERIES)
            listas.abrirLista(id)
            listas.abrirSelector()
            ids.forEach { listas.anadir(id, it) }
            listas.cerrarSelector()
        }
        aplicacion.container.database.close()
        aplicacion.reemplazarContenedor(AppContainer(aplicacion))

        ActivityScenario.launch(MainActivity::class.java).use {
            val lista = listaGuardada("Persistente")!!
            assertEquals(Triple(id, TipoLista.RANKING, Restriccion.SOLO_SERIES), Triple(lista.id, lista.tipo, lista.restriccion))
            assertEquals(ids, elementosDe(id))
            listas.abrir()
            listas.abrirLista(id)
            compose.esperar { ids.all { compose.existe(TagsLista.fila(it)) } }
            listas.interno(TagsLista.puesto(ids[0])).assertIsDisplayed()
        }
    }
}
