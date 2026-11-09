package com.cinesstar.cinetrack.soporte

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import com.cinesstar.cinetrack.data.room.ListaEntity
import com.cinesstar.cinetrack.domain.Normalizador
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.ui.components.TagsDialogo
import com.cinesstar.cinetrack.ui.components.TagsHoja
import com.cinesstar.cinetrack.ui.components.tag
import com.cinesstar.cinetrack.ui.lista.TagsLista
import com.cinesstar.cinetrack.ui.listas.TagsListas

/** Lista guardada con ese nombre (comparando nombres normalizados), o null. */
fun listaGuardada(nombre: String): ListaEntity? = enBd { database.listaDao().buscarPorNombre(Normalizador.normalizar(nombre)) }

/** Ids de los perfiles de una lista, en el orden de su tipo. */
fun elementosDe(listaId: Long): List<Long> = enBd { listas.elementos(listaId).map { it.perfilId } }

private const val LARGA_MS = 800L
private const val PASOS_ARRASTRE = 24
private const val PEQUENO_PX = 30f
private const val PASO_RELOJ_MS = 100L

/** Acciones sobre "Mis Listas" (design.md §5) y la lista abierta (§6). */
class RobotListas(
    private val c: ComposeTestRule,
) {
    /** Abre la pestaña Listas desde la barra inferior y espera a que se lean. */
    fun abrir(): RobotListas {
        c.onNode(hasTestTag(DestinoBarra.LISTAS.tag())).performClick()
        c.esperar { c.existe(TagsListas.BANNER) }
        return this
    }

    fun nodo(tag: String): SemanticsNodeInteraction = c.onNode(hasTestTag(tag))

    /** Nodo dentro de un elemento pulsable (tarjeta, fila), que fusiona la semántica de sus hijos. */
    fun interno(tag: String): SemanticsNodeInteraction = c.onNode(hasTestTag(tag), useUnmergedTree = true)

    fun existeInterno(tag: String): Boolean = c.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    fun tocar(tag: String) {
        nodo(tag).performClick()
        c.waitForIdle()
    }

    /** Desplaza "Mis Listas" hasta [tag]. */
    fun verEnListas(tag: String): SemanticsNodeInteraction {
        c.onNode(hasTestTag(TagsListas.LISTA)).performScrollToNode(hasTestTag(tag))
        c.waitForIdle()
        return nodo(tag)
    }

    /** Desplaza la lista abierta hasta [tag]. */
    fun verEnLista(tag: String): SemanticsNodeInteraction {
        c.onNode(hasTestTag(TagsLista.LISTA)).performScrollToNode(hasTestTag(tag))
        c.waitForIdle()
        return nodo(tag)
    }

    // ---- CU-11 ----

    fun abrirNueva() {
        verEnListas(TagsListas.NUEVA).performClick()
        c.esperar { c.existe(TagsListas.DIALOGO_NUEVA) }
    }

    fun escribirNombre(nombre: String) {
        nodo(TagsListas.NOMBRE).performTextReplacement(nombre)
        c.waitForIdle()
    }

    /** Rellena "Nueva Lista" y pulsa "Guardar Lista" sin esperar el resultado. */
    fun rellenarNueva(
        nombre: String,
        tipo: TipoLista = TipoLista.NORMAL,
        restriccion: Restriccion = Restriccion.MIXTO,
    ) {
        abrirNueva()
        escribirNombre(nombre)
        tocar(TagsListas.tipo(tipo))
        tocar(TagsListas.restriccion(restriccion))
        tocar(TagsDialogo.CONFIRMAR)
    }

    /** CU-11 completo: crea la lista desde la interfaz y devuelve su id. */
    fun crear(
        nombre: String,
        tipo: TipoLista = TipoLista.NORMAL,
        restriccion: Restriccion = Restriccion.MIXTO,
    ): Long {
        rellenarNueva(nombre, tipo, restriccion)
        c.esperar { !c.existe(TagsListas.DIALOGO_NUEVA) && listaGuardada(nombre) != null }
        val id = listaGuardada(nombre)!!.id
        c.esperar { c.existe(TagsListas.tarjeta(id)) }
        return id
    }

    // ---- Lista abierta ----

    fun abrirLista(listaId: Long) {
        c.esperar { c.existe(TagsListas.tarjeta(listaId)) }
        verEnListas(TagsListas.tarjeta(listaId)).performClick()
        c.esperar { c.existe(TagsLista.TITULO) }
    }

    fun abrirSelector() {
        tocar(TagsLista.ANADIR)
        c.esperar { c.existe(TagsLista.SELECTOR) }
    }

    fun buscar(texto: String) {
        nodo(TagsLista.SELECTOR_BUSCAR).performTextReplacement(texto)
        c.waitForIdle()
    }

    /** Toca un perfil del selector (desplazando los resultados hasta él). */
    fun tocarCandidato(perfilId: Long) {
        c.esperar { c.existe(TagsLista.SELECTOR_RESULTADOS) }
        c.onNode(hasTestTag(TagsLista.SELECTOR_RESULTADOS)).performScrollToNode(hasTestTag(TagsLista.candidato(perfilId)))
        tocar(TagsLista.candidato(perfilId))
    }

    /** CU-15 desde la lista: toca el perfil y espera a que esté en la lista. */
    fun anadir(
        listaId: Long,
        perfilId: Long,
    ) {
        tocarCandidato(perfilId)
        c.esperar { perfilId in elementosDe(listaId) }
    }

    fun cerrarSelector() {
        tocar(TagsLista.SELECTOR_CERRAR)
        c.esperar { !c.existe(TagsLista.SELECTOR) }
    }

    // ---- S6: posición, prioridad y reordenar ----

    /** CU-15 A1: escribe en el campo "Posición" del selector. */
    fun escribirPosicion(texto: String) {
        nodo(TagsLista.ALTA_POSICION).performTextReplacement(texto)
        c.waitForIdle()
    }

    /** CU-15 A2: segmento Alta/Media/Baja del selector. */
    fun prioridadAlta(prioridad: Prioridad) = tocar(TagsLista.altaPrioridad(prioridad))

    /** CU-17 paso 1: toca la píldora de prioridad del elemento y espera al menú Alta/Media/Baja. */
    fun abrirMenuPrioridad(perfilId: Long) {
        verEnLista(TagsLista.prioridad(perfilId)).performClick()
        c.esperar { c.existe(TagsLista.opcionPrioridad(perfilId, Prioridad.ALTA)) }
    }

    /** CU-17 pasos 1–2. */
    fun cambiarPrioridad(
        perfilId: Long,
        prioridad: Prioridad,
    ) {
        abrirMenuPrioridad(perfilId)
        tocar(TagsLista.opcionPrioridad(perfilId, prioridad))
    }

    fun subir(perfilId: Long) = verEnLista(TagsLista.subir(perfilId)).performClick().also { c.waitForIdle() }

    fun bajar(perfilId: Long) = verEnLista(TagsLista.bajar(perfilId)).performClick().also { c.waitForIdle() }

    /** Mantiene pulsada la fila de [perfilId] hasta que empieza el arrastre (§6.2). */
    private fun empezarArrastre(perfilId: Long) {
        verEnLista(TagsLista.fila(perfilId)).performTouchInput { down(center) }
        // La pulsación larga se detecta con el reloj de la interfaz: hay que hacerlo avanzar con el dedo quieto.
        c.mainClock.advanceTimeBy(LARGA_MS)
        c.waitForIdle()
    }

    /** Desplaza el dedo [dy] px en [pasos] movimientos, dejando que la lista se recoloque entre uno y otro. */
    private fun moverDedo(
        dy: Float,
        pasos: Int = PASOS_ARRASTRE,
    ) = repeat(pasos) {
        nodo(TagsLista.LISTA).performTouchInput { moveBy(Offset(0f, dy / pasos)) }
        c.waitForIdle()
    }

    private fun soltar() {
        nodo(TagsLista.LISTA).performTouchInput { up() }
        c.waitForIdle()
    }

    private fun centroY(tag: String): Float =
        nodo(tag)
            .fetchSemanticsNode()
            .boundsInRoot.center.y

    /** CU-18: mantiene pulsada la fila de [perfilId] y la arrastra hasta el sitio de la de [sobre]. */
    fun arrastrar(
        perfilId: Long,
        sobre: Long,
    ) {
        verEnLista(TagsLista.fila(sobre))
        val distancia = centroY(TagsLista.fila(sobre)) - centroY(TagsLista.fila(perfilId))
        empezarArrastre(perfilId)
        moverDedo(distancia)
        soltar()
    }

    /** CU-18 A2: arrastra un poco y vuelve a soltar en el mismo sitio. */
    fun arrastrarYDevolver(perfilId: Long) {
        empezarArrastre(perfilId)
        moverDedo(-PEQUENO_PX)
        moverDedo(PEQUENO_PX)
        soltar()
    }

    /** CU-18 A3: arrastra la fila de [perfilId] por encima del borde superior y la mantiene ahí [esperaMs] (scroll automático). */
    fun arrastrarAlBordeSuperior(
        perfilId: Long,
        esperaMs: Long,
    ) {
        verEnLista(TagsLista.fila(perfilId))
        val arriba = nodo(TagsLista.LISTA).fetchSemanticsNode().boundsInRoot.top
        val distancia = arriba - centroY(TagsLista.fila(perfilId))
        empezarArrastre(perfilId)
        moverDedo(distancia)
        var espera = 0L
        while (espera < esperaMs) {
            c.mainClock.advanceTimeBy(PASO_RELOJ_MS)
            nodo(TagsLista.LISTA).performTouchInput { moveBy(Offset(0f, -1f)) }
            c.waitForIdle()
            espera += PASO_RELOJ_MS
        }
        soltar()
    }

    /** CU-16 paso 1: abre la hoja "Quitar" de un elemento. */
    fun pedirQuitar(perfilId: Long) {
        verEnLista(TagsLista.quitar(perfilId)).performClick()
        c.esperar { c.existe(TagsHoja.HOJA) }
    }

    /** CU-16 completo: confirma y espera a que el elemento ya no esté. */
    fun quitar(
        listaId: Long,
        perfilId: Long,
    ) {
        pedirQuitar(perfilId)
        tocar(TagsHoja.ACCION)
        c.esperar { perfilId !in elementosDe(listaId) && !c.existe(TagsLista.fila(perfilId)) }
    }
}
