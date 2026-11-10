package com.cinesstar.cinetrack.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.AppContainer
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.repository.BibliotecaRepository
import com.cinesstar.cinetrack.data.repository.BusquedaRepository
import com.cinesstar.cinetrack.data.repository.ListaRepository
import com.cinesstar.cinetrack.data.room.BibliotecaDao
import com.cinesstar.cinetrack.data.room.BusquedaDao
import com.cinesstar.cinetrack.data.room.FilaBiblioteca
import com.cinesstar.cinetrack.data.room.FilaResumenLista
import com.cinesstar.cinetrack.data.room.ListaDao
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.soporte.aplicacion
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.soporte.existeTexto
import com.cinesstar.cinetrack.ui.biblioteca.TagsBiblioteca
import com.cinesstar.cinetrack.ui.buscar.TagsBuscar
import com.cinesstar.cinetrack.ui.components.tag
import com.cinesstar.cinetrack.ui.datos.TagsDatos
import com.cinesstar.cinetrack.ui.lista.TagsLista
import com.cinesstar.cinetrack.ui.listas.TagsListas
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

/** T9.12: todas las pantallas en estado vacío (M-22, lista sin listas, M-23, M-13) y en estado de error (M-25). */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class EstadosS9Test {
    @get:Rule
    val compose = createEmptyComposeRule()

    private fun ir(destino: DestinoBarra) {
        compose.onNodeWithTag(destino.tag()).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    private fun esperarTexto(texto: String) = compose.esperar { compose.existeTexto(texto) }

    @Test
    fun t9_12_pantallasVacias() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.esperar { compose.existe(TagsBiblioteca.VACIA) }
            esperarTexto("Todavía no hay títulos. Añade el primero.")

            ir(DestinoBarra.LISTAS)
            compose.esperar { compose.existe(TagsListas.VACIA) }
            esperarTexto("Aún no has creado ninguna lista.")

            ir(DestinoBarra.BUSCAR)
            compose.esperar { compose.existe(TagsBuscar.SIN_RESULTADOS) }
            esperarTexto("No se han encontrado resultados.")

            ir(DestinoBarra.AJUSTES)
            compose.esperar { compose.existe(TagsDatos.TITULOS) && compose.existeTexto("0") }

            // M-23: una lista recién creada, sin elementos.
            val nueva = enBd { listas.crear("Pendientes", TipoLista.PRIORIDAD) }
            ir(DestinoBarra.LISTAS)
            compose.esperar { compose.existe(TagsListas.tarjeta(nueva)) }
            compose.onNodeWithTag(TagsListas.tarjeta(nueva)).performSemanticsAction(SemanticsActions.OnClick)
            compose.esperar { compose.existe(TagsLista.VACIA) }
            esperarTexto("Esta lista está vacía.")
        }
    }

    @Test
    fun t9_12_pantallasConErrorDeLecturaMuestranM25() {
        val real = aplicacion.container.database
        val biblioteca =
            object : BibliotecaDao by real.bibliotecaDao() {
                override suspend fun filasBiblioteca(): List<FilaBiblioteca> = throw IOException("Lectura simulada fallida")
            }
        val listas =
            object : ListaDao by real.listaDao() {
                override suspend fun resumenes(): List<FilaResumenLista> = throw IOException("Lectura simulada fallida")
            }
        val busqueda =
            object : BusquedaDao by real.busquedaDao() {
                @Suppress("LongParameterList")
                override suspend fun buscar(
                    patron: String?,
                    tipo: TipoPerfil?,
                    minimo: Int?,
                    maximo: Int?,
                    conGeneros: Boolean,
                    generoIds: List<Long>,
                    idiomaId: Long?,
                    sagaId: Long?,
                    conEstados: Boolean,
                    estados: List<Estado>,
                ): List<FilaBiblioteca> = throw IOException("Lectura simulada fallida")
            }
        aplicacion.reemplazarContenedor(
            AppContainer(
                real,
                biblioteca = BibliotecaRepository(real, dao = biblioteca),
                listas = ListaRepository(real, listaDao = listas),
                busqueda = BusquedaRepository(real, dao = busqueda),
            ),
        )
        val m25 = "Error inesperado. Tus datos no se han modificado."
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.esperar { compose.existe(TagsBiblioteca.ERROR) }
            esperarTexto(m25)
            ir(DestinoBarra.LISTAS)
            compose.esperar { compose.existe(TagsListas.ERROR) }
            ir(DestinoBarra.BUSCAR)
            compose.esperar { compose.existe(TagsBuscar.ERROR) }
            compose.onNodeWithTag(TagsBuscar.NOMBRE).performTextReplacement("x")
            compose.esperar {
                compose.existe(TagsBuscar.ERROR) &&
                    compose.onAllNodes(hasTestTag(TagsBuscar.ERROR)).fetchSemanticsNodes().size == 1
            }
            esperarTexto(m25)
        }
    }
}
