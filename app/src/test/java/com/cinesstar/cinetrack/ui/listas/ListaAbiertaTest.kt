package com.cinesstar.cinetrack.ui.listas

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.soporte.FixturePequeno
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
import com.cinesstar.cinetrack.ui.components.TagsDialogo
import com.cinesstar.cinetrack.ui.components.TagsHoja
import com.cinesstar.cinetrack.ui.components.tag
import com.cinesstar.cinetrack.ui.lista.TagsLista
import com.cinesstar.cinetrack.ui.perfil.TagsDetalle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests UI del S5 sobre la lista abierta (design.md §6) y el alta desde el perfil: CU-13 (T5.09, T5.18, T5.32),
 * CU-15 (T5.10–T5.17), CU-16 con el aviso único M-11 (T5.21–T5.28) y CU-19 desde la lista (T5.26).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class ListaAbiertaTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val listas = RobotListas(compose)
    private val detalle = RobotDetalle(compose)

    private fun perfil(
        nombre: String,
        tipo: TipoPerfil = TipoPerfil.PELICULA,
    ): Long = enBd { perfiles.crear(NuevoPerfil(nombre, tipo)) }

    private fun lista(
        nombre: String,
        tipo: TipoLista = TipoLista.NORMAL,
        restriccion: Restriccion = Restriccion.MIXTO,
        vararg perfilesIniciales: Long,
    ): Long =
        enBd {
            listas.crear(nombre, tipo, restriccion).also { id -> perfilesIniciales.forEach { listas.anadir(id, it) } }
        }

    /** Abre "Mis Listas" y la lista [id]. */
    private fun abrirLista(id: Long) {
        listas.abrir()
        listas.abrirLista(id)
    }

    private fun textoM11(lista: String) = "Has quitado el último elemento. ¿Quieres eliminar también la lista «$lista»?"

    private fun estado(texto: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, texto)

    /** Desde el detalle del perfil, paso 1: "+ Añadir a lista" abre el selector de listas. */
    private fun abrirSelectorPerfil(listaId: Long) {
        compose.onNodeWithTagScroll(TagsDetalle.ANADIR_A_LISTA).performClick()
        compose.esperar { compose.existe(TagsDetalle.opcionLista(listaId)) }
    }

    /** Paso 2: tocar la lista [listaId]. */
    private fun elegirLista(listaId: Long) {
        compose.onNode(hasTestTag(TagsDetalle.opcionLista(listaId))).performClick()
    }

    /** Pasos 2–3: en un ranking o una lista de prioridad (S6) se despliega y se confirma con "Añadir". */
    private fun anadirDesdePerfil(listaId: Long) {
        abrirSelectorPerfil(listaId)
        elegirLista(listaId)
        compose.waitForIdle()
        if (compose.existe(TagsDetalle.CONFIRMAR_ALTA)) {
            compose.onNode(hasTestTag(TagsDetalle.CONFIRMAR_ALTA)).performClick()
            compose.waitForIdle()
        }
    }

    // ---- CU-13 ----

    @Test
    fun t5_09_listaVaciaMuestraM23YAnadirTitulo() {
        val id = lista("Vacía")
        abrirLista(id)
        listas.nodo(TagsLista.VACIA).assertIsDisplayed()
        assertTrue(compose.existeTexto("Esta lista está vacía."))
        listas.tocar(TagsLista.VACIA_ANADIR)
        compose.esperar { compose.existe(TagsLista.SELECTOR) }
    }

    @Test
    fun t5_18_unaListaDe25MuestraComoMaximo10YSeLlegaAlUltimoConScroll() {
        val ids = (1..25).map { perfil("Título %02d".format(it)) }
        val id = lista("Larga", TipoLista.NORMAL, Restriccion.MIXTO, *ids.toLongArray())
        abrirLista(id)
        compose.esperar { compose.existe(TagsLista.fila(ids.first())) }
        val visibles =
            ids.count {
                compose.existe(TagsLista.fila(it)) &&
                    runCatching { listas.nodo(TagsLista.fila(it)).assertIsDisplayed() }.isSuccess
            }
        assertTrue("Se ven $visibles filas a la vez", visibles in 1..10)
        listas.verEnLista(TagsLista.fila(ids.last())).assertIsDisplayed()
    }

    // ---- CU-15 ----

    @Test
    fun t5_10_anadirUnPerfilDesdeLaLista() {
        val p = perfil("Alien")
        val id = lista("Terror")
        abrirLista(id)
        listas.nodo(TagsLista.TOTAL).assertTextEquals("0 títulos")
        listas.abrirSelector()
        compose.esperarToast("«Alien» añadido a la lista") { listas.tocarCandidato(p) }
        assertEquals(listOf(p), elementosDe(id))
        listas.cerrarSelector()
        compose.esperar { compose.existe(TagsLista.fila(p)) }
        listas.nodo(TagsLista.TOTAL).assertTextEquals("1 título")
    }

    @Test
    fun t5_11_anadirALaListaDesdeElPerfil() {
        val p = perfil("Alien")
        val id = lista("Terror")
        detalle.abrir(p)
        abrirSelectorPerfil(id)
        compose.esperarToast("«Alien» añadido a «Terror»") { elegirLista(id) }
        assertEquals(listOf(p), elementosDe(id))
        compose.esperar { compose.existe(TagsDetalle.lista(id)) }
        assertFalse("El selector se cierra", compose.existe(TagsDetalle.SELECTOR_LISTAS))
    }

    @Test
    fun t5_12_elMismoPerfilDosVecesMuestraM10YNoSeDuplica() {
        val p = perfil("Alien")
        val id = lista("Terror")
        abrirLista(id)
        listas.abrirSelector()
        listas.anadir(id, p)
        compose.esperar { !listas.existeInterno(TagsLista.candidatoAnadir(p)) }
        compose.onNode(hasTestTag(TagsLista.candidato(p)) and estado("Ya está en la lista")).assertExists()
        compose.esperarToast("«Alien» ya está en esta lista.") { listas.tocarCandidato(p) }
        assertEquals(listOf(p), elementosDe(id))

        // Desde el perfil, la lista aparece como no disponible y forzarla da el mismo aviso.
        listas.cerrarSelector()
        listas.tocar(TagsLista.fila(p))
        compose.esperar { compose.existe(TagsDetalle.TITULO) }
        abrirSelectorPerfil(id)
        compose.esperarToast("«Alien» ya está en esta lista.") { elegirLista(id) }
        assertEquals(listOf(p), elementosDe(id))
    }

    @Test
    fun t5_13_unaSerieNoSeOfreceEnUnaListaDeSoloPeliculas() {
        val serie = perfil("Dark", TipoPerfil.SERIE)
        val peli = perfil("Alien")
        val id = lista("Solo pelis", TipoLista.NORMAL, Restriccion.SOLO_PELICULAS)
        abrirLista(id)
        listas.abrirSelector()
        compose.esperar { compose.existe(TagsLista.candidato(serie)) }
        assertFalse("La serie aparece sin icono de añadir", listas.existeInterno(TagsLista.candidatoAnadir(serie)))
        assertTrue(listas.existeInterno(TagsLista.candidatoAnadir(peli)))
        compose.onNode(hasTestTag(TagsLista.candidato(serie)) and estado("No admitido en esta lista")).assertExists()
        compose.esperarToast("Esta lista solo admite películas.") { listas.tocarCandidato(serie) }
        assertEquals(emptyList<Long>(), elementosDe(id))
    }

    @Test
    fun t5_14_elMismoPerfilEnTresListas() {
        val p = perfil("Alien")
        val ids = listOf("Uno" to TipoLista.NORMAL, "Dos" to TipoLista.PRIORIDAD, "Tres" to TipoLista.RANKING).map { (n, t) -> lista(n, t) }
        detalle.abrir(p)
        ids.forEach { id ->
            anadirDesdePerfil(id)
            compose.esperar { compose.existe(TagsDetalle.lista(id)) }
        }
        ids.forEach { assertEquals(listOf(p), elementosDe(it)) }
        assertEquals(setOf("Uno", "Dos", "Tres"), enBd { database.listaDao().listasDe(p) }.map { it.nombre }.toSet())
    }

    @Test
    fun t5_15_anadirVariosSeguidosSinCerrarElSelector() {
        val ids = listOf("A", "B", "C").map { perfil(it) }
        val id = lista("Varios")
        abrirLista(id)
        listas.abrirSelector()
        ids.forEach {
            listas.anadir(id, it)
            assertTrue("El selector sigue abierto", compose.existe(TagsLista.SELECTOR))
        }
        assertEquals(ids.toSet(), elementosDe(id).toSet())
    }

    @Test
    fun t5_16_buscarEnElSelectorSinMayusculasNiTildes() {
        val f = enBd { FixturePequeno.cargar(database) }
        val padrinos =
            setOf(f.perfiles.getValue("Pádrino|${TipoPerfil.PELICULA}"), f.perfiles.getValue("EL padrino|${TipoPerfil.PELICULA}"))
        abrirLista(f.listas.getValue(FixturePequeno.LISTA_RANKING))
        listas.abrirSelector()

        fun encontrados(): Set<Long> =
            f.perfiles.values
                .filter { compose.existe(TagsLista.candidato(it)) }
                .toSet()
        listas.buscar("padrino")
        compose.esperar { encontrados() == padrinos }
        listas.buscar("PÁDRINO")
        compose.esperar { encontrados() == padrinos }
        listas.buscar("zzz")
        compose.esperar { compose.existe(TagsLista.SELECTOR_SIN_RESULTADOS) }
        listas.nodo(TagsLista.SELECTOR_SIN_RESULTADOS).assertTextEquals("No se han encontrado resultados.")
    }

    @Test
    fun t5_17_anadirAUnaListaEnComoMaximoTresPasos() {
        val p = perfil("Alien")
        val q = perfil("Aliens")
        val id = lista("Terror")
        abrirLista(id)
        var pasos = 0
        listas.tocar(TagsLista.ANADIR).also { pasos++ }
        listas.tocarCandidato(p).also { pasos++ }
        compose.esperar { p in elementosDe(id) }
        assertTrue("Desde la lista: $pasos pasos", pasos <= 3)

        // Desde el perfil: "+ Añadir a lista" y la lista.
        listas.cerrarSelector()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNode(hasTestTag(DestinoBarra.BIBLIOTECA.tag())).performClick()
        detalle.abrir(q)
        anadirDesdePerfil(id)
        compose.esperar { q in elementosDe(id) }
    }

    // ---- CU-16 ----

    @Test
    fun t5_21_quitarUnElementoConConfirmacionNoBorraElPerfil() {
        val a = perfil("A")
        val b = perfil("B")
        val id = lista("Dos", TipoLista.NORMAL, Restriccion.MIXTO, a, b)
        abrirLista(id)
        listas.pedirQuitar(a)
        compose.esperarToast("Título quitado de la lista (permanece en biblioteca)") { listas.tocar(TagsHoja.ACCION) }
        compose.esperar { !compose.existe(TagsLista.fila(a)) }
        assertEquals(listOf(b), elementosDe(id))
        assertTrue(perfilesGuardados().any { it.id == a })
    }

    @Test
    fun t5_22_cancelarLaConfirmacionNoCambiaNada() {
        val a = perfil("A")
        val id = lista("Una", TipoLista.NORMAL, Restriccion.MIXTO, a)
        abrirLista(id)
        listas.pedirQuitar(a)
        listas.tocar(TagsHoja.CANCELAR)
        compose.esperar { !compose.existe(TagsHoja.HOJA) }
        assertEquals(listOf(a), elementosDe(id))
        listas.nodo(TagsLista.fila(a)).assertIsDisplayed()
    }

    @Test
    fun t5_23_quitarElUltimoMuestraM11UnaSolaVez() {
        val a = perfil("A")
        val id = lista("Corta", TipoLista.NORMAL, Restriccion.MIXTO, a)
        abrirLista(id)
        listas.quitar(id, a)
        compose.esperar { compose.existeTexto(textoM11("Corta")) }
        assertEquals("Una sola hoja", 1, compose.onAllNodes(hasTestTag(TagsHoja.HOJA)).fetchSemanticsNodes().size)
        listas.nodo(TagsHoja.ACCION).assertTextEquals("Eliminar lista")
        listas.nodo(TagsHoja.CANCELAR).assertTextEquals("Mantener lista")
    }

    @Test
    fun t5_24_aceptarM11BorraLaListaSinSegundaConfirmacion() {
        val a = perfil("A")
        val id = lista("Corta", TipoLista.NORMAL, Restriccion.MIXTO, a)
        abrirLista(id)
        listas.quitar(id, a)
        compose.esperar { compose.existeTexto(textoM11("Corta")) }
        compose.esperarToast("Lista eliminada") { listas.tocar(TagsHoja.ACCION) }
        assertFalse("Sin segunda confirmación (M-12)", compose.existe(TagsDialogo.DIALOGO))
        compose.esperar { compose.existe(TagsListas.BANNER) && !compose.existe(TagsListas.tarjeta(id)) }
        assertNull(enBd { listas.obtener(id) })
        assertTrue(perfilesGuardados().any { it.id == a })
    }

    @Test
    fun t5_25_rechazarM11DejaLaListaVaciaYElAvisoNoSeRepite() {
        val a = perfil("A")
        val id = lista("Corta", TipoLista.NORMAL, Restriccion.MIXTO, a)
        abrirLista(id)
        listas.quitar(id, a)
        compose.esperar { compose.existeTexto(textoM11("Corta")) }
        listas.tocar(TagsHoja.CANCELAR)
        compose.esperar { !compose.existe(TagsHoja.HOJA) }
        listas.nodo(TagsLista.VACIA).assertIsDisplayed()

        // Al volver a abrir la lista (sigue vacía) el aviso no aparece.
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        listas.abrirLista(id)
        compose.waitForIdle()
        assertFalse(compose.existe(TagsHoja.HOJA))
        assertTrue(enBd { listas.obtener(id)!!.avisoVaciaMostrado })
    }

    @Test
    fun t5_26_trasRechazarSePuedeBorrarLaListaAMano() {
        val a = perfil("A")
        val id = lista("Corta", TipoLista.NORMAL, Restriccion.MIXTO, a)
        abrirLista(id)
        listas.quitar(id, a)
        compose.esperar { compose.existeTexto(textoM11("Corta")) }
        listas.tocar(TagsHoja.CANCELAR)
        compose.esperar { !compose.existe(TagsHoja.HOJA) }

        listas.verEnLista(TagsLista.ELIMINAR).performClick()
        compose.esperar { compose.existe(TagsDialogo.DIALOGO) }
        listas.nodo(TagsDialogo.TEXTO).assertTextEquals("¿Eliminar la lista «Corta»? Los títulos no se borrarán.")
        compose.esperarToast("Lista eliminada") { listas.tocar(TagsDialogo.CONFIRMAR) }
        compose.esperar { compose.existe(TagsListas.BANNER) }
        assertNull(enBd { listas.obtener(id) })
    }

    @Test
    fun t5_27_trasRechazarAnadirYQuitarOtraVezVuelveAAvisar() {
        val a = perfil("A")
        val id = lista("Corta", TipoLista.NORMAL, Restriccion.MIXTO, a)
        abrirLista(id)
        listas.quitar(id, a)
        compose.esperar { compose.existeTexto(textoM11("Corta")) }
        listas.tocar(TagsHoja.CANCELAR)
        compose.esperar { !compose.existe(TagsHoja.HOJA) }

        listas.tocar(TagsLista.VACIA_ANADIR)
        compose.esperar { compose.existe(TagsLista.SELECTOR) }
        listas.anadir(id, a)
        listas.cerrarSelector()
        compose.esperar { compose.existe(TagsLista.fila(a)) }
        listas.quitar(id, a)
        compose.esperar { compose.existeTexto(textoM11("Corta")) }
    }

    @Test
    fun t5_28_conDosElementosQuitarUnoNoAvisa() {
        val a = perfil("A")
        val b = perfil("B")
        val id = lista("Dos", TipoLista.NORMAL, Restriccion.MIXTO, a, b)
        abrirLista(id)
        listas.quitar(id, a)
        compose.waitForIdle()
        assertFalse(compose.existe(TagsHoja.HOJA))
        assertFalse(compose.existeTexto(textoM11("Dos")))
    }

    // ---- CU-13 E1 / CU-16 E1 ----

    @Test
    fun t5_32_laListaOElElementoYaNoExisten() {
        val a = perfil("A")
        val b = perfil("B")
        val id = lista("Efímera", TipoLista.NORMAL, Restriccion.MIXTO, a, b)
        abrirLista(id)

        // CU-16 E1: el elemento desaparece con la hoja abierta → aviso y la lista se refresca.
        listas.pedirQuitar(a)
        enBd { listas.quitar(id, a) }
        compose.esperar { !compose.existe(TagsLista.fila(a)) }
        compose.esperarToast("Este título ya no está en la lista.") { listas.tocar(TagsHoja.ACCION) }
        listas.nodo(TagsLista.fila(b)).assertIsDisplayed()

        // CU-13 E1: la lista desaparece mientras está abierta → vuelve atrás con aviso.
        compose.esperarToast("Esta lista ya no existe.") { enBd { listas.eliminar(id) } }
        compose.esperar { compose.existe(TagsListas.BANNER) && !compose.existe(TagsLista.PANTALLA) }
    }
}
