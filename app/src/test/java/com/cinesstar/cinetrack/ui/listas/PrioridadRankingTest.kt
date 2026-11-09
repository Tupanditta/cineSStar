package com.cinesstar.cinetrack.ui.listas

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.soporte.RobotDetalle
import com.cinesstar.cinetrack.soporte.RobotListas
import com.cinesstar.cinetrack.soporte.elementosDe
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.esperarToast
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.soporte.existeTexto
import com.cinesstar.cinetrack.soporte.onNodeWithTagScroll
import com.cinesstar.cinetrack.soporte.perfilesGuardados
import com.cinesstar.cinetrack.soporte.textoEditable
import com.cinesstar.cinetrack.ui.components.TagsHoja
import com.cinesstar.cinetrack.ui.components.tag
import com.cinesstar.cinetrack.ui.lista.TagsLista
import com.cinesstar.cinetrack.ui.perfil.TagsDetalle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests UI del S6 sobre listas de prioridad y rankings (design.md §6.1–§6.3, §6.6): alta con posición o prioridad
 * (T6.01–T6.06, T6.22), orden (T6.07, T6.08), CU-17 (T6.09, T6.10), CU-18 con flechas y arrastre (T6.12–T6.16) y
 * quitar (T6.19, T6.20).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class PrioridadRankingTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val listas = RobotListas(compose)
    private val detalle = RobotDetalle(compose)

    private fun perfil(nombre: String): Long = enBd { perfiles.crear(NuevoPerfil(nombre, TipoPerfil.PELICULA)) }

    private fun perfiles(
        prefijo: String,
        n: Int,
    ): List<Long> = (1..n).map { perfil("$prefijo %02d".format(it)) }

    /** Ranking con [ids] añadidos en orden (#1 = el primero). */
    private fun ranking(
        nombre: String,
        ids: List<Long>,
    ): Long = enBd { listas.crear(nombre, TipoLista.RANKING).also { id -> ids.forEach { listas.anadir(id, it) } } }

    /** Lista de prioridad con cada perfil en su nivel, añadidos en el orden dado. */
    private fun prioridades(
        nombre: String,
        niveles: List<Pair<Long, Prioridad>>,
    ): Long =
        enBd {
            listas.crear(nombre, TipoLista.PRIORIDAD).also { id ->
                niveles.forEach { (p, n) -> listas.anadir(id, p, prioridad = n) }
            }
        }

    private fun abrirLista(id: Long) {
        listas.abrir()
        listas.abrirLista(id)
    }

    private fun posiciones(listaId: Long): List<Int?> = enBd { listas.elementos(listaId).map { it.posicion } }

    private fun prioridadDe(
        listaId: Long,
        perfilId: Long,
    ): Prioridad? = enBd { listas.elementos(listaId).first { it.perfilId == perfilId }.prioridad }

    /** Orden en que se ven las filas de [ids] (de arriba abajo). */
    private fun ordenEnPantalla(ids: List<Long>): List<Long> =
        ids
            .filter { compose.existe(TagsLista.fila(it)) }
            .sortedBy {
                listas
                    .nodo(TagsLista.fila(it))
                    .fetchSemanticsNode()
                    .boundsInRoot.top
            }

    private fun puesto(perfilId: Long) = listas.interno(TagsLista.puesto(perfilId))

    // ---- CU-15 en ranking y prioridad ----

    @Test
    fun t6_01_anadirAUnRankingSinIndicarPosicionQuedaAlFinal() {
        val ids = perfiles("Top", 2)
        val nuevo = perfil("Nuevo")
        val id = ranking("Top", ids)
        abrirLista(id)
        listas.abrirSelector()
        listas.nodo(TagsLista.ALTA_POSICION).assertIsDisplayed()
        assertTrue("Ayuda con el puesto por defecto", compose.existeTexto("Vacío = al final (#3)"))
        compose.esperarToast("«Nuevo» añadido al puesto #3") { listas.tocarCandidato(nuevo) }
        assertEquals(ids + nuevo, elementosDe(id))
        listas.cerrarSelector()
        compose.esperar { compose.existe(TagsLista.fila(nuevo)) }
        puesto(nuevo).assertTextEquals("#3")
    }

    @Test
    fun t6_02_anadirEnLaPosicion2DeUnRankingDe5() {
        val ids = perfiles("Top", 5)
        val nuevo = perfil("Nuevo")
        val id = ranking("Top", ids)
        abrirLista(id)
        listas.abrirSelector()
        listas.escribirPosicion("2")
        compose.esperarToast("«Nuevo» añadido al puesto #2") { listas.tocarCandidato(nuevo) }
        assertEquals(listOf(ids[0], nuevo) + ids.drop(1), elementosDe(id))
        assertEquals((1..6).toList(), posiciones(id))
        assertEquals("El campo vuelve a «al final»", "", listas.nodo(TagsLista.ALTA_POSICION).textoEditable())
        listas.cerrarSelector()
        compose.esperar { compose.existe(TagsLista.fila(nuevo)) }
        puesto(nuevo).assertTextEquals("#2")
        puesto(ids[1]).assertTextEquals("#3")
    }

    @Test
    fun t6_02_desdeElPerfilSeEligeLaPosicionEnElRanking() {
        val ids = perfiles("Top", 3)
        val nuevo = perfil("Nuevo")
        val id = ranking("Top", ids)
        detalle.abrir(nuevo)
        compose.onNodeWithTagScroll(TagsDetalle.ANADIR_A_LISTA).performClick()
        compose.esperar { compose.existe(TagsDetalle.opcionLista(id)) }
        compose.onNode(hasTestTag(TagsDetalle.opcionLista(id))).performClick()
        compose.esperar { compose.existe(TagsDetalle.ALTA_DESPLEGADA) }
        listas.escribirPosicion("1")
        compose.esperarToast("«Nuevo» añadido a «Top»") { listas.tocar(TagsDetalle.CONFIRMAR_ALTA) }
        assertEquals(listOf(nuevo) + ids, elementosDe(id))
        assertFalse("El selector se cierra", compose.existe(TagsDetalle.SELECTOR_LISTAS))
    }

    @Test
    fun t6_03_anadirEnLaPosicionNMasUnoQuedaAlFinal() {
        val ids = perfiles("Top", 3)
        val nuevo = perfil("Nuevo")
        val id = ranking("Top", ids)
        abrirLista(id)
        listas.abrirSelector()
        // Con − y + desde el campo vacío: 3 y otra vez 4 (n+1); + queda deshabilitado en n+1.
        listas.tocar(TagsLista.ALTA_MENOS)
        assertEquals("3", listas.nodo(TagsLista.ALTA_POSICION).textoEditable())
        listas.tocar(TagsLista.ALTA_MAS)
        assertEquals("4", listas.nodo(TagsLista.ALTA_POSICION).textoEditable())
        listas.nodo(TagsLista.ALTA_MAS).assertIsNotEnabled()
        compose.esperarToast("«Nuevo» añadido al puesto #4") { listas.tocarCandidato(nuevo) }
        assertEquals(ids + nuevo, elementosDe(id))
    }

    @Test
    fun t6_04_posicionesNoValidasMuestranM24YNoAnaden() {
        val ids = perfiles("Top", 3)
        val nuevo = perfil("Nuevo")
        val id = ranking("Top", ids)
        abrirLista(id)
        listas.abrirSelector()
        listOf("0", "5", "abc").forEach { texto ->
            listas.escribirPosicion(texto)
            compose.esperarToast("Posición no válida.") { listas.tocarCandidato(nuevo) }
            assertEquals("«$texto» no añade nada", ids, elementosDe(id))
        }
        // Vacío = al final.
        listas.escribirPosicion("")
        compose.esperarToast("«Nuevo» añadido al puesto #4") { listas.tocarCandidato(nuevo) }
        assertEquals(ids + nuevo, elementosDe(id))
    }

    @Test
    fun t6_05_anadirAPrioridadSinElegirEsMedia() {
        val p = perfil("Alien")
        val id = prioridades("Pendientes", emptyList())
        abrirLista(id)
        listas.abrirSelector()
        listas.nodo(TagsLista.altaPrioridad(Prioridad.MEDIA)).assertIsDisplayed()
        compose.esperarToast("«Alien» añadido a la lista") { listas.tocarCandidato(p) }
        assertEquals(Prioridad.MEDIA, prioridadDe(id, p))
        listas.cerrarSelector()
        compose.esperar { compose.existe(TagsLista.prioridad(p)) }
        listas.nodo(TagsLista.prioridad(p)).assertTextEquals("Media")
    }

    @Test
    fun t6_06_anadirConAltaMediaYBaja() {
        val ids = perfiles("P", 3)
        val niveles = listOf(Prioridad.ALTA, Prioridad.MEDIA, Prioridad.BAJA)
        val id = prioridades("Pendientes", emptyList())
        abrirLista(id)
        listas.abrirSelector()
        ids.zip(niveles).forEach { (p, nivel) ->
            listas.prioridadAlta(nivel)
            listas.anadir(id, p)
        }
        ids.zip(niveles).forEach { (p, nivel) -> assertEquals(nivel, prioridadDe(id, p)) }

        // Desde el perfil: la lista se despliega con los segmentos y se añade con el nivel elegido.
        val otro = perfil("Otro")
        listas.cerrarSelector()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNode(hasTestTag(DestinoBarra.BIBLIOTECA.tag())).performClick()
        detalle.abrir(otro)
        compose.onNodeWithTagScroll(TagsDetalle.ANADIR_A_LISTA).performClick()
        compose.esperar { compose.existe(TagsDetalle.opcionLista(id)) }
        compose.onNode(hasTestTag(TagsDetalle.opcionLista(id))).performClick()
        compose.esperar { compose.existe(TagsDetalle.ALTA_DESPLEGADA) }
        listas.prioridadAlta(Prioridad.BAJA)
        listas.tocar(TagsDetalle.CONFIRMAR_ALTA)
        compose.esperar { otro in elementosDe(id) }
        assertEquals(Prioridad.BAJA, prioridadDe(id, otro))
    }

    // ---- CU-13: orden ----

    @Test
    fun t6_07_prioridadOrdenadaAltaMediaBajaYPorAntiguedad() {
        val ids = perfiles("P", 6)
        val niveles = listOf(Prioridad.BAJA, Prioridad.ALTA, Prioridad.MEDIA, Prioridad.ALTA, Prioridad.BAJA, Prioridad.MEDIA)
        val id = prioridades("Mezcla", ids.zip(niveles))
        abrirLista(id)
        compose.esperar { ids.all { compose.existe(TagsLista.fila(it)) } }
        assertEquals(listOf(ids[1], ids[3], ids[2], ids[5], ids[0], ids[4]), ordenEnPantalla(ids))
    }

    @Test
    fun t6_08_rankingDe12OrdenadoPorPosicionConComoMaximo10Visibles() {
        val ids = perfiles("Top", 12)
        val id = ranking("Doce", ids)
        enBd { listas.mover(id, ids[11], 1) }
        val esperado = listOf(ids[11]) + ids.dropLast(1)
        abrirLista(id)
        compose.esperar { compose.existe(TagsLista.fila(ids[11])) }
        val visibles =
            ids.count {
                compose.existe(TagsLista.fila(it)) &&
                    runCatching { listas.nodo(TagsLista.fila(it)).assertIsDisplayed() }.isSuccess
            }
        assertTrue("Se ven $visibles filas a la vez", visibles in 1..10)
        assertEquals(esperado.take(3), ordenEnPantalla(ids).take(3))
        puesto(ids[11]).assertTextEquals("#1")
        listas.verEnLista(TagsLista.fila(ids[10])).assertIsDisplayed()
        puesto(ids[10]).assertTextEquals("#12")
    }

    // ---- CU-17 ----

    @Test
    fun t6_09_cambiarLaPrioridadReordenaLaLista() {
        val ids = perfiles("P", 3)
        val id = prioridades("Pendientes", ids.zip(listOf(Prioridad.ALTA, Prioridad.MEDIA, Prioridad.BAJA)))
        abrirLista(id)
        compose.esperar { compose.existe(TagsLista.prioridad(ids[2])) }
        listas.abrirMenuPrioridad(ids[2])
        compose.esperarToast("Prioridad actualizada") { listas.tocar(TagsLista.opcionPrioridad(ids[2], Prioridad.ALTA)) }
        assertEquals(Prioridad.ALTA, prioridadDe(id, ids[2]))
        // Dentro de Alta manda la antigüedad en la lista: el primero sigue delante.
        compose.esperar { ordenEnPantalla(ids) == listOf(ids[0], ids[2], ids[1]) }
        listas.nodo(TagsLista.prioridad(ids[2])).assertTextEquals("Alta")
    }

    @Test
    fun t6_10_elegirElMismoNivelNoCambiaNada() {
        val ids = perfiles("P", 2)
        val id = prioridades("Pendientes", ids.zip(listOf(Prioridad.ALTA, Prioridad.BAJA)))
        val antes = enBd { listas.elementos(id) }
        abrirLista(id)
        compose.esperar { compose.existe(TagsLista.prioridad(ids[1])) }
        listas.cambiarPrioridad(ids[1], Prioridad.BAJA)
        compose.waitForIdle()
        assertFalse(compose.existeTexto("Prioridad actualizada"))
        assertEquals(antes, enBd { listas.elementos(id) })
    }

    // ---- CU-18 ----

    @Test
    fun t6_12_arrastrarEl5AlPuesto2() {
        val ids = perfiles("Top", 5)
        val id = ranking("Top", ids)
        abrirLista(id)
        compose.esperar { ids.all { compose.existe(TagsLista.fila(it)) } }
        listas.arrastrar(ids[4], sobre = ids[1])
        compose.esperar { elementosDe(id) == listOf(ids[0], ids[4], ids[1], ids[2], ids[3]) }
        assertEquals((1..5).toList(), posiciones(id))
        compose.esperar { runCatching { puesto(ids[4]).assertTextEquals("#2") }.isSuccess }
        puesto(ids[3]).assertTextEquals("#5")
    }

    @Test
    fun t6_13_flechasIntercambianConElVecinoYReordenarPuestos() {
        val ids = perfiles("Top", 3)
        val id = ranking("Top", ids)
        abrirLista(id)
        compose.esperar { ids.all { compose.existe(TagsLista.fila(it)) } }

        // Modo "Reordenar puestos": banner con "Listo".
        listas.tocar(TagsLista.REORDENAR)
        listas.nodo(TagsLista.BANNER_REORDENAR).assertIsDisplayed()
        assertTrue(compose.existeTexto("Usa las flechas para desplazar puestos ordinales."))

        compose.esperarToast("Posición actualizada") { listas.subir(ids[2]) }
        compose.esperar { elementosDe(id) == listOf(ids[0], ids[2], ids[1]) }
        listas.bajar(ids[0])
        compose.esperar { elementosDe(id) == listOf(ids[2], ids[0], ids[1]) }
        assertEquals((1..3).toList(), posiciones(id))
        compose.esperar { runCatching { puesto(ids[2]).assertTextEquals("#1") }.isSuccess }

        compose.esperarToast("Ranking guardado") { listas.tocar(TagsLista.LISTO) }
        assertFalse(compose.existe(TagsLista.BANNER_REORDENAR))
    }

    @Test
    fun t6_14_subirEnEl1YBajarEnElUltimoEstanDeshabilitados() {
        val ids = perfiles("Top", 3)
        val id = ranking("Top", ids)
        abrirLista(id)
        compose.esperar { ids.all { compose.existe(TagsLista.fila(it)) } }
        listas.nodo(TagsLista.subir(ids[0])).assertIsNotEnabled()
        listas.nodo(TagsLista.bajar(ids[2])).assertIsNotEnabled()
        listas.subir(ids[0])
        listas.bajar(ids[2])
        assertEquals(ids, elementosDe(id))
        assertFalse(compose.existeTexto("Posición actualizada"))
    }

    @Test
    fun t6_15_arrastrarYSoltarEnElMismoSitioNoCambiaNada() {
        val ids = perfiles("Top", 4)
        val id = ranking("Top", ids)
        abrirLista(id)
        compose.esperar { ids.all { compose.existe(TagsLista.fila(it)) } }
        listas.arrastrarYDevolver(ids[1])
        assertEquals(ids, elementosDe(id))
        assertEquals(ids, ordenEnPantalla(ids))
        assertFalse(compose.existeTexto("Posición actualizada"))
        assertFalse("Soltar no abre el perfil", compose.existe(TagsDetalle.TITULO))
    }

    @Test
    fun t6_16_arrastrarCon25ElementosHaceScrollAutomatico() {
        val ids = perfiles("Top", 25)
        val id = ranking("Largo", ids)
        abrirLista(id)
        compose.esperar { compose.existe(TagsLista.fila(ids[0])) }
        listas.arrastrarAlBordeSuperior(ids[24], esperaMs = 4_000)
        compose.esperar { elementosDe(id).indexOf(ids[24]) < 10 }
        assertEquals((1..25).toList(), posiciones(id))
        assertEquals(ids - ids[24], elementosDe(id) - ids[24])
    }

    // ---- CU-16 en ranking y prioridad ----

    @Test
    fun t6_19_quitarEl2DeUnRankingDe5SubeLosPosteriores() {
        val ids = perfiles("Top", 5)
        val id = ranking("Top", ids)
        abrirLista(id)
        compose.esperar { ids.all { compose.existe(TagsLista.fila(it)) } }
        listas.quitar(id, ids[1])
        assertEquals(ids - ids[1], elementosDe(id))
        assertEquals((1..4).toList(), posiciones(id))
        compose.esperar { runCatching { puesto(ids[2]).assertTextEquals("#2") }.isSuccess }
        puesto(ids[4]).assertTextEquals("#4")
        assertTrue(perfilesGuardados().any { it.id == ids[1] })
    }

    @Test
    fun t6_20_quitarDePrioridadConservaElNivelDelResto() {
        val ids = perfiles("P", 3)
        val niveles = listOf(Prioridad.ALTA, Prioridad.MEDIA, Prioridad.BAJA)
        val id = prioridades("Pendientes", ids.zip(niveles))
        abrirLista(id)
        compose.esperar { ids.all { compose.existe(TagsLista.fila(it)) } }
        listas.quitar(id, ids[1])
        assertFalse(compose.existe(TagsHoja.HOJA))
        assertEquals(Prioridad.ALTA, prioridadDe(id, ids[0]))
        assertEquals(Prioridad.BAJA, prioridadDe(id, ids[2]))
        listas.nodo(TagsLista.prioridad(ids[0])).assertTextEquals("Alta")
        listas.nodo(TagsLista.prioridad(ids[2])).assertTextEquals("Baja")
    }

    // ---- RNF-09 ----

    @Test
    fun t6_22_anadirAUnRankingAlFinalEnComoMaximoTresPasos() {
        val ids = perfiles("Top", 2)
        val nuevo = perfil("Nuevo")
        val id = ranking("Top", ids)
        abrirLista(id)
        var pasos = 0
        listas.tocar(TagsLista.ANADIR).also { pasos++ }
        compose.esperar { compose.existe(TagsLista.SELECTOR) }
        listas.tocarCandidato(nuevo).also { pasos++ }
        compose.esperar { nuevo in elementosDe(id) }
        assertEquals(ids + nuevo, elementosDe(id))
        assertTrue("$pasos pasos", pasos <= 3)
    }

    @Test
    fun unaListaRestringidaSigueSinAdmitirOtroTipoAunqueSeElijaPosicion() {
        val serie = enBd { perfiles.crear(NuevoPerfil("Dark", TipoPerfil.SERIE)) }
        val id = enBd { listas.crear("Solo pelis", TipoLista.RANKING, Restriccion.SOLO_PELICULAS) }
        abrirLista(id)
        listas.abrirSelector()
        listas.escribirPosicion("9")
        compose.esperarToast("Esta lista solo admite películas.") { listas.tocarCandidato(serie) }
        assertEquals(emptyList<Long>(), elementosDe(id))
    }
}
