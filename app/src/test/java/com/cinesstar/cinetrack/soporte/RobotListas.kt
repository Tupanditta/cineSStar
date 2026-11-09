package com.cinesstar.cinetrack.soporte

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.cinesstar.cinetrack.data.room.ListaEntity
import com.cinesstar.cinetrack.domain.Normalizador
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
