package com.cinesstar.cinetrack.ui.listas

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.soporte.FixturePequeno
import com.cinesstar.cinetrack.soporte.RobotListas
import com.cinesstar.cinetrack.soporte.elementosDe
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.esperarToast
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.soporte.existeTexto
import com.cinesstar.cinetrack.soporte.listaGuardada
import com.cinesstar.cinetrack.soporte.perfilesGuardados
import com.cinesstar.cinetrack.ui.components.TagsDialogo
import com.cinesstar.cinetrack.ui.lista.TagsLista
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests UI del S5 sobre "Mis Listas" (design.md §5): CU-11 Crear (T5.01–T5.06), CU-12 Ver todas (T5.07, T5.08),
 * CU-14 Renombrar (T5.19, T5.20) y CU-19 Eliminar desde el menú de la tarjeta (T5.29, T5.30).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class ListasTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val listas = RobotListas(compose)

    private fun perfil(
        nombre: String,
        tipo: TipoPerfil = TipoPerfil.PELICULA,
    ): Long = enBd { perfiles.crear(NuevoPerfil(nombre, tipo)) }

    // ---- CU-11 ----

    @Test
    fun t5_01_crearUnaListaNormal() {
        listas.abrir()
        listas.abrirNueva()
        listas.escribirNombre("Joyas Ocultas")
        compose.esperarToast("Lista creada con éxito") { listas.tocar(TagsDialogo.CONFIRMAR) }
        val lista = listaGuardada("Joyas Ocultas")!!
        assertEquals(TipoLista.NORMAL to Restriccion.MIXTO, lista.tipo to lista.restriccion)
        compose.esperar { compose.existe(TagsListas.tarjeta(lista.id)) }
        listas.interno(TagsListas.tipoTarjeta(lista.id)).assertTextEquals("[NORMAL]")
        listas.interno(TagsListas.totalTarjeta(lista.id)).assertTextEquals("0 títulos")
    }

    @Test
    fun t5_02_crearUnaDePrioridadYOtraDeRanking() {
        listas.abrir()
        val prioridad = listas.crear("Próximas", TipoLista.PRIORIDAD)
        val ranking = listas.crear("Top", TipoLista.RANKING)
        assertEquals(TipoLista.PRIORIDAD, listaGuardada("Próximas")!!.tipo)
        assertEquals(TipoLista.RANKING, listaGuardada("Top")!!.tipo)
        listas.verEnListas(TagsListas.tarjeta(prioridad))
        listas.interno(TagsListas.tipoTarjeta(prioridad)).assertTextEquals("[PRIORIDAD]")
        listas.verEnListas(TagsListas.tarjeta(ranking))
        listas.interno(TagsListas.tipoTarjeta(ranking)).assertTextEquals("[RANKING]")
    }

    @Test
    fun t5_03_unaListaMixtaAdmitePeliculaYSerie() {
        val peli = perfil("Alien")
        val serie = perfil("Dark", TipoPerfil.SERIE)
        listas.abrir()
        val id = listas.crear("Mezcla")
        listas.abrirLista(id)
        listas.abrirSelector()
        listas.anadir(id, peli)
        listas.anadir(id, serie)
        assertEquals(setOf(peli, serie), elementosDe(id).toSet())
    }

    @Test
    fun t5_04_listaSoloPeliculasMuestraLaRestriccion() {
        listas.abrir()
        val id = listas.crear("Clásicos", TipoLista.NORMAL, Restriccion.SOLO_PELICULAS)
        assertEquals(Restriccion.SOLO_PELICULAS, listaGuardada("Clásicos")!!.restriccion)
        listas.verEnListas(TagsListas.tarjeta(id))
        listas.interno(TagsListas.restriccionTarjeta(id)).assertTextEquals("Restringida: Solo Películas")
        listas.interno(TagsListas.totalTarjeta(id)).assertTextEquals("0 películas")
    }

    @Test
    fun t5_05_nombreVacioYDuplicado() {
        enBd { listas.crear(FixturePequeno.LISTA_NORMAL, TipoLista.NORMAL) }
        listas.abrir()
        listas.rellenarNueva("   ")
        compose.esperar { compose.existe(TagsListas.ERROR_NOMBRE) }
        listas.nodo(TagsListas.ERROR_NOMBRE).assertTextEquals("El nombre de la lista es obligatorio.")

        listas.escribirNombre("  PARA el FÍNDE ")
        compose.esperar { !compose.existe(TagsListas.ERROR_NOMBRE) }
        listas.tocar(TagsDialogo.CONFIRMAR)
        compose.esperar { compose.existe(TagsListas.ERROR_NOMBRE) }
        listas.nodo(TagsListas.ERROR_NOMBRE).assertTextEquals("Ya existe una lista con ese nombre.")
        assertTrue("El diálogo sigue abierto", compose.existe(TagsListas.DIALOGO_NUEVA))
        assertEquals("No se ha creado ninguna lista", 1, enBd { database.listaDao().resumenes() }.size)
    }

    @Test
    fun t5_06_cancelarNoCreaNada() {
        listas.abrir()
        listas.abrirNueva()
        listas.escribirNombre("No se guarda")
        listas.tocar(TagsDialogo.CANCELAR)
        compose.esperar { !compose.existe(TagsListas.DIALOGO_NUEVA) }
        assertNull(listaGuardada("No se guarda"))
        listas.nodo(TagsListas.VACIA).assertIsDisplayed()
    }

    // ---- CU-12 ----

    @Test
    fun t5_07_misListasMuestraNombreTipoYElementos() {
        val f = enBd { FixturePequeno.cargar(database) }
        listas.abrir()
        compose.esperar { compose.existeTexto("3 de 3 mostradas") }
        val esperado =
            mapOf(
                FixturePequeno.LISTA_RANKING to ("[RANKING]" to "6 películas"),
                FixturePequeno.LISTA_PRIORIDAD to ("[PRIORIDAD]" to "6 series"),
                FixturePequeno.LISTA_NORMAL to ("[NORMAL]" to "4 títulos"),
            )
        esperado.forEach { (nombre, tipoYTotal) ->
            val id = f.listas.getValue(nombre)
            listas.verEnListas(TagsListas.tarjeta(id)).assertTextContains(nombre)
            listas.interno(TagsListas.tipoTarjeta(id)).assertTextEquals(tipoYTotal.first)
            listas.interno(TagsListas.totalTarjeta(id)).assertTextEquals(tipoYTotal.second)
        }
        // Dato extra de cada tipo (§5.2-3): #1 del ranking y elementos de prioridad Alta.
        compose.onNode(hasText("#1 Pádrino"), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("2 Alta"), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("3 Listas"), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("12 Obras catalogadas"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun t5_08_sinListasMuestraElVacioConNuevaLista() {
        listas.abrir()
        compose.esperar { compose.existe(TagsListas.VACIA) }
        listas.nodo(TagsListas.VACIA).assertIsDisplayed()
        compose.onNode(hasTestTag(TagsListas.NUEVA)).assertIsDisplayed().assertTextContains("+ Nueva Lista")
        listas.abrirNueva()
    }

    // ---- CU-14 ----

    @Test
    fun t5_19_renombrarCambiaElNombreYConservaLosElementos() {
        val p = perfil("Alien")
        val id = enBd { listas.crear("Terror", TipoLista.NORMAL).also { listas.anadir(it, p) } }
        listas.abrir()
        listas.verEnListas(TagsListas.menu(id)).performClick()
        compose.esperar { compose.existe(TagsListas.MENU_RENOMBRAR) }
        listas.tocar(TagsListas.MENU_RENOMBRAR)
        compose.esperar { compose.existe(TagsListas.DIALOGO_RENOMBRAR) }
        listas.escribirNombre("Miedo")
        compose.esperarToast("Lista renombrada") { listas.tocar(TagsDialogo.CONFIRMAR) }
        compose.esperar { compose.existeTexto("Miedo") }
        assertEquals("Miedo", enBd { listas.obtener(id)!!.nombre })
        assertEquals(listOf(p), elementosDe(id))

        // También desde la lista abierta (botón `edit` de la cabecera).
        listas.abrirLista(id)
        listas.tocar(TagsLista.RENOMBRAR)
        compose.esperar { compose.existe(TagsListas.DIALOGO_RENOMBRAR) }
        listas.escribirNombre("Pánico")
        listas.tocar(TagsDialogo.CONFIRMAR)
        compose.esperar { runCatching { listas.nodo(TagsLista.TITULO).assertTextEquals("Pánico") }.isSuccess }
    }

    @Test
    fun t5_20_renombrarConElMismoNombreVacioODuplicado() {
        val id = enBd { listas.crear("Terror", TipoLista.NORMAL) }
        enBd { listas.crear("Comedia", TipoLista.NORMAL) }
        listas.abrir()
        listas.abrirLista(id)
        listas.tocar(TagsLista.RENOMBRAR)
        compose.esperar { compose.existe(TagsListas.DIALOGO_RENOMBRAR) }

        listas.escribirNombre("")
        listas.tocar(TagsDialogo.CONFIRMAR)
        compose.esperar { compose.existe(TagsListas.ERROR_NOMBRE) }
        listas.nodo(TagsListas.ERROR_NOMBRE).assertTextEquals("El nombre de la lista es obligatorio.")

        listas.escribirNombre("COMEDIA")
        listas.tocar(TagsDialogo.CONFIRMAR)
        compose.esperar { compose.existe(TagsListas.ERROR_NOMBRE) }
        listas.nodo(TagsListas.ERROR_NOMBRE).assertTextEquals("Ya existe una lista con ese nombre.")

        // A2: el mismo nombre cierra sin cambios ni aviso.
        listas.escribirNombre("Terror")
        listas.tocar(TagsDialogo.CONFIRMAR)
        compose.esperar { !compose.existe(TagsListas.DIALOGO_RENOMBRAR) }
        assertTrue(!compose.existeTexto("Lista renombrada"))
        assertEquals("Terror", enBd { listas.obtener(id)!!.nombre })
    }

    // ---- CU-19 ----

    @Test
    fun t5_29_eliminarUnaListaConConfirmacionConservaLosPerfiles() {
        val p = perfil("Alien")
        val (borrar, otra) =
            enBd {
                val b = listas.crear("Borrar", TipoLista.RANKING)
                val o = listas.crear("Otra", TipoLista.NORMAL)
                listas.anadir(b, p)
                listas.anadir(o, p)
                b to o
            }
        listas.abrir()
        listas.verEnListas(TagsListas.menu(borrar)).performClick()
        compose.esperar { compose.existe(TagsListas.MENU_ELIMINAR) }
        listas.tocar(TagsListas.MENU_ELIMINAR)
        compose.esperar { compose.existe(TagsDialogo.DIALOGO) }
        listas.nodo(TagsDialogo.TEXTO).assertTextEquals("¿Eliminar la lista «Borrar»? Los títulos no se borrarán.")

        compose.esperarToast("Lista eliminada") { listas.tocar(TagsDialogo.CONFIRMAR) }
        compose.esperar { !compose.existe(TagsListas.tarjeta(borrar)) }
        assertNull(enBd { listas.obtener(borrar) })
        assertEquals(listOf(p), perfilesGuardados().map { it.id })
        assertEquals(listOf(p), elementosDe(otra))
    }

    @Test
    fun t5_30_cancelarNoBorraLaLista() {
        val id = enBd { listas.crear("Seguir", TipoLista.NORMAL) }
        listas.abrir()
        listas.verEnListas(TagsListas.menu(id)).performClick()
        compose.esperar { compose.existe(TagsListas.MENU_ELIMINAR) }
        listas.tocar(TagsListas.MENU_ELIMINAR)
        compose.esperar { compose.existe(TagsDialogo.DIALOGO) }
        listas.tocar(TagsDialogo.CANCELAR)
        compose.esperar { !compose.existe(TagsDialogo.DIALOGO) }
        assertEquals("Seguir", enBd { listas.obtener(id)!!.nombre })
        listas.nodo(TagsListas.tarjeta(id)).assertIsDisplayed()
    }
}
