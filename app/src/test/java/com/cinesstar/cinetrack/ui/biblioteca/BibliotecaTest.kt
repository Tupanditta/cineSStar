package com.cinesstar.cinetrack.ui.biblioteca

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.domain.Ambito
import com.cinesstar.cinetrack.domain.ClaveGrupo
import com.cinesstar.cinetrack.domain.CriterioAgrupacion
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.RangoPuntuacion
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.soporte.DatosFixture
import com.cinesstar.cinetrack.soporte.FixturePequeno
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.soporte.existeTexto
import com.cinesstar.cinetrack.ui.components.tagPantalla
import com.cinesstar.cinetrack.ui.perfil.TagsDetalle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests UI del S4 sobre la Biblioteca (design.md §4): CU-08 (T4.01–T4.08) y la parte de interfaz de CU-09 (T4.16,
 * T4.17). Los que necesitan ver todas las tarjetas a la vez usan una ventana alta ([ALTA]).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class BibliotecaTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private companion object {
        /** Cabecera de la Biblioteca + 12 tarjetas de 176 dp: caben todas sin desplazar. */
        const val ALTA = "w411dp-h3000dp"
    }

    private fun fixture(): DatosFixture = enBd { FixturePequeno.cargar(database) }.also { esperarTarjetas(it.perfiles.size) }

    private fun esperarTarjetas(n: Int) = compose.esperar { compose.existeTexto("Todas ($n)") }

    private fun DatosFixture.id(
        nombre: String,
        tipo: TipoPerfil = FixturePequeno.perfiles.first { it.nombre == nombre }.tipo,
    ): Long = perfiles.getValue("$nombre|$tipo")

    private fun nodo(tag: String): SemanticsNodeInteraction = compose.onNodeWithTag(tag)

    /** Recuento de un grupo: está dentro de la cabecera pulsable, que fusiona sus textos. */
    private fun total(tag: String): SemanticsNodeInteraction = compose.onNodeWithTag(tag, useUnmergedTree = true)

    /** Desplaza la lista hasta el nodo [tag]. */
    private fun verEnLista(tag: String): SemanticsNodeInteraction {
        compose.onNodeWithTag(TagsBiblioteca.LISTA).performScrollToNode(hasTestTag(tag))
        compose.waitForIdle()
        return nodo(tag)
    }

    private fun tocar(tag: String) {
        verEnLista(tag).performClick()
        compose.waitForIdle()
    }

    private fun mostrando(
        x: Int,
        n: Int,
    ) = compose.esperar { compose.existeTexto("Mostrando $x de $n") }

    /** Perfiles con tarjeta en pantalla (en una ventana [ALTA] están todas compuestas). */
    private fun visibles(f: DatosFixture): Set<Long> =
        f.perfiles.values
            .filter { compose.existe(TagsBiblioteca.fila(it)) }
            .toSet()

    // ---- CU-08 ----

    @Test
    @Config(qualifiers = ALTA)
    fun t4_01_cadaTarjetaMuestraLaInsigniaDeSuEstado() {
        val f = fixture()
        FixturePequeno.perfiles.forEach { p ->
            val id = f.perfiles.getValue(FixturePequeno.clave(p))
            val texto = mapOf(Estado.PENDIENTE to "Pendiente", Estado.VIENDO to "Viendo", Estado.VISTO to "Visto").getValue(p.estado)
            verEnLista(TagsBiblioteca.fila(id))
            compose
                .onNode(hasText(texto) and hasAnyAncestor(hasTestTag(TagsBiblioteca.insigniaEstado(id))), useUnmergedTree = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun t4_02_contadoresViendoPendientesVistas() {
        fixture()
        val esperado = FixturePequeno.perfiles.groupingBy { it.estado }.eachCount()
        assertEquals(mapOf(Estado.VIENDO to 4, Estado.PENDIENTE to 3, Estado.VISTO to 5), esperado)
        mapOf(Estado.VIENDO to "VIENDO", Estado.PENDIENTE to "PENDIENTES", Estado.VISTO to "VISTAS").forEach { (estado, etiqueta) ->
            nodo(TagsBiblioteca.contador(estado)).assertTextContains(esperado.getValue(estado).toString()).assertTextContains(etiqueta)
        }
    }

    @Test
    fun t4_02_pulsarUnContadorFiltraPorEseEstado() {
        fixture()
        tocar(TagsBiblioteca.contador(Estado.VIENDO))
        nodo(TagsBiblioteca.contador(Estado.VIENDO)).assertIsSelected()
        mostrando(4, 12)
        tocar(TagsBiblioteca.contador(Estado.VIENDO))
        mostrando(12, 12)
    }

    @Test
    @Config(qualifiers = ALTA)
    fun t4_03_ordenPorDefectoMasRecientesPrimero() {
        val ids =
            listOf("Primero", "Segundo", "Tercero").map { nombre ->
                enBd { perfiles.crear(NuevoPerfil(nombre, TipoPerfil.PELICULA)) }
            }
        esperarTarjetas(3)
        val arriba = ids.map { nodo(TagsBiblioteca.fila(it)).getBoundsInRoot().top }
        assertTrue("El último creado va arriba: $arriba", arriba[2] < arriba[1] && arriba[1] < arriba[0])
    }

    @Test
    fun t4_04_bibliotecaVaciaMuestraM22YAnadir() {
        compose.esperar { compose.existe(TagsBiblioteca.VACIA) }
        compose.onNode(hasText("Todavía no hay títulos. Añade el primero.")).assertIsDisplayed()
        nodo(TagsBiblioteca.ANADIR).assertIsDisplayed().performClick()
        compose.esperar { compose.existe(DestinoBarra.ANADIR.tagPantalla()) }
    }

    @Test
    @Config(qualifiers = ALTA)
    fun t4_05_filtroTodasSoloPeliculasSoloSeries() {
        val f = fixture()
        compose.onNode(hasText("Todas (12)")).assertIsDisplayed()
        compose.onNode(hasText("Solo Películas (6)")).assertIsDisplayed()
        compose.onNode(hasText("Solo Series (6)")).assertIsDisplayed()
        nodo(TagsBiblioteca.ambito(Ambito.TODO)).assertIsSelected()
        val porTipo = FixturePequeno.perfiles.groupBy({ it.tipo }, { f.perfiles.getValue(FixturePequeno.clave(it)) })

        tocar(TagsBiblioteca.ambito(Ambito.PELICULAS))
        mostrando(6, 12)
        compose.esperar { visibles(f) == porTipo.getValue(TipoPerfil.PELICULA).toSet() }
        tocar(TagsBiblioteca.ambito(Ambito.SERIES))
        mostrando(6, 12)
        compose.esperar { visibles(f) == porTipo.getValue(TipoPerfil.SERIE).toSet() }
        // Los contadores siguen el ámbito: series en Viendo = 2.
        nodo(TagsBiblioteca.contador(Estado.VIENDO)).assertTextContains("2")
        tocar(TagsBiblioteca.ambito(Ambito.TODO))
        mostrando(12, 12)
        compose.esperar { visibles(f) == f.perfiles.values.toSet() }
    }

    @Test
    @Config(qualifiers = ALTA)
    fun t4_06_filtroPorEscalaDePuntuacion() {
        val f = fixture()
        tocar(TagsBiblioteca.rango(RangoPuntuacion.DE_9_A_10))
        nodo(TagsBiblioteca.rango(RangoPuntuacion.DE_9_A_10)).assertIsSelected()
        compose.esperar { visibles(f) == setOf(f.id("Pádrino"), f.id("EL padrino")) }
        mostrando(2, 12)

        tocar(TagsBiblioteca.rango(RangoPuntuacion.DE_0_A_2_9))
        compose.esperar { visibles(f) == setOf(f.id("Dune", TipoPerfil.SERIE), f.id("Cosmos")) }

        // "Sin puntuar" incluye solo los perfiles sin nota (no el de 0.0).
        tocar(TagsBiblioteca.rango(RangoPuntuacion.SIN_PUNTUACION))
        compose.esperar { visibles(f) == setOf(f.id("Dune: Parte Dos"), f.id("Ñandú y compañía")) }

        // Pulsar el chip seleccionado lo deselecciona.
        tocar(TagsBiblioteca.rango(RangoPuntuacion.SIN_PUNTUACION))
        nodo(TagsBiblioteca.rango(RangoPuntuacion.SIN_PUNTUACION)).assertIsNotSelected()
        mostrando(12, 12)

        // Filtros combinados sin resultados: M-13.
        tocar(TagsBiblioteca.ambito(Ambito.PELICULAS))
        tocar(TagsBiblioteca.rango(RangoPuntuacion.DE_3_A_4_9))
        compose.esperar { compose.existe(TagsBiblioteca.SIN_RESULTADOS) }
        mostrando(0, 12)
    }

    @Test
    fun t4_07_con25PerfilesSeVenComoMucho10YElRestoConScroll() {
        val ids = (1..25).map { i -> enBd { perfiles.crear(NuevoPerfil("Título $i", TipoPerfil.SERIE)) } }
        esperarTarjetas(25)
        val enPantalla =
            ids.count { id ->
                compose.onAllNodes(hasTestTag(TagsBiblioteca.fila(id))).fetchSemanticsNodes().isNotEmpty() &&
                    nodo(TagsBiblioteca.fila(id)).isDisplayed()
            }
        assertTrue("Tarjetas a la vez: $enPantalla", enPantalla in 1..10)
        // El más antiguo es el último: se llega con scroll.
        verEnLista(TagsBiblioteca.fila(ids.first())).assertIsDisplayed()
    }

    @Test
    fun t4_08_tocarUnPerfilAbreSuDetalle() {
        val f = fixture()
        val id = f.id("Amélie")
        tocar(TagsBiblioteca.fila(id))
        compose.esperar { compose.existe(TagsDetalle.TITULO) }
        nodo(TagsDetalle.TITULO).assertTextContains("Amélie")
    }

    // ---- CU-09 (interfaz) ----

    @Test
    fun t4_16_plegarUnGrupoYCambiarDeCriterioConservaElAmbito() {
        val f = fixture()
        tocar(TagsBiblioteca.criterio(CriterioAgrupacion.ESTADO))
        nodo(TagsBiblioteca.criterio(CriterioAgrupacion.ESTADO)).assertIsSelected()
        val viendo = ClaveGrupo.DeEstado(Estado.VIENDO)
        val pendiente = ClaveGrupo.DeEstado(Estado.PENDIENTE)
        compose.esperar { compose.existe(TagsBiblioteca.grupo(pendiente)) }
        total(TagsBiblioteca.totalGrupo(pendiente)).assertTextContains("3")
        val dunePendiente = TagsBiblioteca.filaEnGrupo(pendiente, f.id("Dune: Parte Dos"))
        verEnLista(dunePendiente).assertIsDisplayed()

        // A1: plegar y desplegar el grupo "Pendiente".
        tocar(TagsBiblioteca.grupo(pendiente))
        compose.esperar { !compose.existe(dunePendiente) }
        nodo(TagsBiblioteca.grupo(pendiente)).assertIsDisplayed()
        verEnLista(TagsBiblioteca.grupo(viendo)).assertIsDisplayed()
        tocar(TagsBiblioteca.grupo(pendiente))
        compose.esperar { compose.existe(dunePendiente) }

        // A2: cambiar el ámbito mantiene el criterio…
        tocar(TagsBiblioteca.ambito(Ambito.SERIES))
        nodo(TagsBiblioteca.criterio(CriterioAgrupacion.ESTADO)).assertIsSelected()
        compose.esperar { runCatching { total(TagsBiblioteca.totalGrupo(pendiente)).assertTextContains("2") }.isSuccess }
        // …y cambiar el criterio mantiene el ámbito.
        tocar(TagsBiblioteca.criterio(CriterioAgrupacion.SAGA))
        nodo(TagsBiblioteca.ambito(Ambito.SERIES)).assertIsSelected()
        compose.esperar { compose.existe(TagsBiblioteca.grupo(ClaveGrupo.SinSaga)) }
        total(TagsBiblioteca.totalGrupo(ClaveGrupo.SinSaga)).assertTextContains("6")
        assertFalse(
            "Las series del fixture no tienen saga",
            compose.existe(TagsBiblioteca.grupo(ClaveGrupo.Saga(f.sagas.getValue("Dune"), "Dune"))),
        )

        // "Ninguno" vuelve a la lista sin grupos.
        tocar(TagsBiblioteca.criterio(null))
        compose.esperar { !compose.existe(TagsBiblioteca.grupo(ClaveGrupo.SinSaga)) }
    }

    @Test
    fun t4_17_agruparConLaBibliotecaVaciaMuestraM22() {
        fixture()
        tocar(TagsBiblioteca.criterio(CriterioAgrupacion.GENERO))
        enBd { biblioteca.leer().forEach { perfiles.eliminar(it.id) } }
        compose.esperar { compose.existe(TagsBiblioteca.VACIA) }
        compose.onNode(hasText("Todavía no hay títulos. Añade el primero.")).assertIsDisplayed()
    }
}
