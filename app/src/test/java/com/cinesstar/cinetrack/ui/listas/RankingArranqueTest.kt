package com.cinesstar.cinetrack.ui.listas

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.AppContainer
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.repository.ListaRepository
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.room.ListaDao
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.soporte.RobotListas
import com.cinesstar.cinetrack.soporte.aplicacion
import com.cinesstar.cinetrack.soporte.elementosDe
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.esperarToast
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.ui.lista.TagsLista
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Tests de prioridad y ranking que preparan las dependencias antes de abrir la app: T6.21 (fallo de escritura simulado
 * al reordenar y al cambiar la prioridad) y T6.23 (el orden persiste tras reiniciar, E).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class RankingArranqueTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val listas = RobotListas(compose)

    private val m25 = "Error inesperado. Tus datos no se han modificado."

    private fun perfiles(n: Int): List<Long> = (1..n).map { enBd { perfiles.crear(NuevoPerfil("Título $it", TipoPerfil.PELICULA)) } }

    private fun ranking(ids: List<Long>): Long =
        enBd { listas.crear("Top", TipoLista.RANKING).also { id -> ids.forEach { listas.anadir(id, it) } } }

    private fun puestoEs(
        perfilId: Long,
        texto: String,
    ): Boolean = runCatching { listas.interno(TagsLista.puesto(perfilId)).assertTextEquals(texto) }.isSuccess

    /** Sustituye el DAO de listas por uno que falla al escribir posiciones y prioridades (antes de abrir la app). */
    private fun conDaoQueFalla() {
        val real = aplicacion.container
        val dao =
            object : ListaDao by real.database.listaDao() {
                override suspend fun actualizarPosicion(
                    listaId: Long,
                    perfilId: Long,
                    posicion: Int,
                ) = throw IOException("Disco lleno")

                override suspend fun actualizarPrioridad(
                    listaId: Long,
                    perfilId: Long,
                    prioridad: Prioridad,
                ): Int = throw IOException("Disco lleno")
            }
        aplicacion.reemplazarContenedor(AppContainer(real.database, listas = ListaRepository(real.database, listaDao = dao)))
    }

    @Test
    fun t6_21_unFalloAlReordenarMuestraM25YRestauraElOrden() {
        val ids = perfiles(3)
        val top = ranking(ids)
        val prio = enBd { listas.crear("Pendientes", TipoLista.PRIORIDAD).also { listas.anadir(it, ids[0], prioridad = Prioridad.BAJA) } }
        conDaoQueFalla()

        ActivityScenario.launch(MainActivity::class.java).use {
            listas.abrir()
            listas.abrirLista(top)
            compose.esperar { ids.all { compose.existe(TagsLista.fila(it)) } }

            // Flechas: M-25 y nada cambia.
            compose.esperarToast(m25) { listas.subir(ids[2]) }
            assertEquals(ids, elementosDe(top))
            assertEquals(true, puestoEs(ids[2], "#3"))

            // Arrastre: el orden provisional se descarta y vuelve el guardado.
            listas.arrastrar(ids[2], sobre = ids[0])
            compose.esperar { puestoEs(ids[2], "#3") && puestoEs(ids[0], "#1") }
            assertEquals(ids, elementosDe(top))

            // CU-17 E1: la prioridad no cambia.
            it.onActivity { actividad -> actividad.onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()
            listas.abrirLista(prio)
            compose.esperar { compose.existe(TagsLista.prioridad(ids[0])) }
            listas.abrirMenuPrioridad(ids[0])
            compose.esperarToast(m25) { listas.tocar(TagsLista.opcionPrioridad(ids[0], Prioridad.ALTA)) }
            assertEquals(Prioridad.BAJA, enBd { listas.elementos(prio).single().prioridad })
            listas.nodo(TagsLista.prioridad(ids[0])).assertTextEquals("Baja")
        }
    }

    @Test
    fun t6_23_elOrdenDelRankingPersisteTrasReiniciar() {
        val ids = perfiles(4)
        val top = ranking(ids)
        val esperado = listOf(ids[3], ids[0], ids[1], ids[2])
        ActivityScenario.launch(MainActivity::class.java).use {
            listas.abrir()
            listas.abrirLista(top)
            compose.esperar { ids.all { compose.existe(TagsLista.fila(it)) } }
            listas.arrastrar(ids[3], sobre = ids[0])
            compose.esperar { elementosDe(top) == esperado }
        }
        aplicacion.container.database.close()
        aplicacion.reemplazarContenedor(AppContainer(aplicacion))

        ActivityScenario.launch(MainActivity::class.java).use {
            assertEquals(esperado, elementosDe(top))
            listas.abrir()
            listas.abrirLista(top)
            compose.esperar { ids.all { compose.existe(TagsLista.fila(it)) } }
            esperado.forEachIndexed { i, p -> assertEquals("Puesto de $p", true, puestoEs(p, "#${i + 1}")) }
        }
    }
}
