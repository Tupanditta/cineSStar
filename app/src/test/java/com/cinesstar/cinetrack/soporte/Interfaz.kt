package com.cinesstar.cinetrack.soporte

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.cinesstar.cinetrack.AppContainer
import com.cinesstar.cinetrack.CineTrackApplication
import com.cinesstar.cinetrack.data.room.PerfilEntity
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.ui.biblioteca.TagsBiblioteca
import com.cinesstar.cinetrack.ui.components.TagsHoja
import com.cinesstar.cinetrack.ui.components.tag
import com.cinesstar.cinetrack.ui.components.tagPantalla
import com.cinesstar.cinetrack.ui.perfil.TagsDetalle
import com.cinesstar.cinetrack.ui.perfilform.TagsFormulario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Tiempo máximo de espera de las operaciones asíncronas (base de datos) en los tests de interfaz. */
const val ESPERA_MS = 5_000L

val aplicacion: CineTrackApplication get() = ApplicationProvider.getApplicationContext()

/** Ejecuta [bloque] contra las dependencias reales de la app (la misma base de datos que ve la interfaz). */
fun <T> enBd(bloque: suspend AppContainer.() -> T): T = runBlocking { aplicacion.container.bloque() }

fun perfilesGuardados(): List<PerfilEntity> = enBd { database.perfilDao().observarTodos().first() }

fun ComposeTestRule.existe(tag: String): Boolean = onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

fun ComposeTestRule.existeTexto(
    texto: String,
    subcadena: Boolean = false,
): Boolean = onAllNodes(hasText(texto, substring = subcadena), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

fun ComposeTestRule.esperar(condicion: () -> Boolean) = waitUntil(ESPERA_MS, condicion)

/** Texto de un campo editable. */
fun SemanticsNodeInteraction.textoEditable(): String =
    fetchSemanticsNode()
        .config
        .getOrNull(SemanticsProperties.EditableText)
        ?.text
        .orEmpty()

/** Acciones sobre el formulario "Añadir nuevo perfil" (design.md §8). */
class RobotFormulario(
    private val c: ComposeTestRule,
) {
    fun nodo(tag: String) = c.onNodeWithTagScroll(tag)

    /** Abre el alta desde la barra inferior y espera a que se cargue el catálogo. */
    fun abrir(): RobotFormulario {
        c.onNode(hasTestTag(DestinoBarra.ANADIR.tag())).performClick()
        c.esperar { c.onAllNodes(hasText("Drama") and isToggleable()).fetchSemanticsNodes().isNotEmpty() }
        return this
    }

    fun tocar(tag: String) {
        nodo(tag).performClick()
        c.waitForIdle()
    }

    fun nombre(texto: String) = escribir(TagsFormulario.NOMBRE, texto)

    fun puntuacion(texto: String) = escribir(TagsFormulario.PUNTUACION, texto)

    fun comentario(texto: String) = escribir(TagsFormulario.COMENTARIO, texto)

    fun escribir(
        tag: String,
        texto: String,
    ) {
        nodo(tag).performTextReplacement(texto)
        c.waitForIdle()
    }

    fun tipo(tipo: TipoPerfil) = tocar(TagsFormulario.tipo(tipo))

    fun estado(estado: Estado) = tocar(TagsFormulario.estado(estado))

    /** Marca o desmarca un chip de género o idioma. */
    fun chip(texto: String) {
        c.onNode(hasText(texto) and isToggleable()).performScrollTo().performClick()
        c.waitForIdle()
    }

    fun esperarTexto(texto: String) = c.esperar { c.existeTexto(texto) }

    fun esperarHabilitado() = c.esperar { runCatching { nodo(TagsFormulario.GUARDAR).assertIsEnabled() }.isSuccess }

    /** Pulsa "Guardar Perfil" y espera a volver a la pantalla anterior ([pantalla]: la del alta o la de edición). */
    fun guardar(pantalla: String = DestinoBarra.ANADIR.tagPantalla()) {
        esperarHabilitado()
        tocar(TagsFormulario.GUARDAR)
        c.esperar { !c.existe(pantalla) }
    }
}

/** Acciones sobre el detalle del perfil (design.md §7), al que se llega desde la Biblioteca. */
class RobotDetalle(
    private val c: ComposeTestRule,
) {
    fun nodo(tag: String) = c.onNodeWithTagScroll(tag)

    fun abrir(perfilId: Long): RobotDetalle {
        c.esperar { c.existe(TagsBiblioteca.fila(perfilId)) }
        c.onNode(hasTestTag(TagsBiblioteca.fila(perfilId))).performClick()
        c.esperar { c.existe(TagsDetalle.TITULO) }
        return this
    }

    fun tocar(tag: String) {
        nodo(tag).performClick()
        c.waitForIdle()
    }

    /**
     * Menú ⋮ del hero → [opcion] (TagsDetalle.MENU_EDITAR / MENU_ELIMINAR). Se pulsa por semántica: tras desplazar, el
     * botón puede quedar bajo la barra superior translúcida, que se llevaría el toque.
     */
    fun menu(opcion: String) {
        nodo(TagsDetalle.MENU).performSemanticsAction(SemanticsActions.OnClick)
        c.waitForIdle()
        c.esperar { c.existe(opcion) }
        c.onNode(hasTestTag(opcion)).performClick()
        c.waitForIdle()
    }

    /** Abre "Editar Medio" y espera a que el formulario tenga los datos del perfil y el catálogo. */
    fun editar() {
        menu(TagsDetalle.MENU_EDITAR)
        c.esperar { c.existe(TagsFormulario.PANTALLA_EDITAR) && c.existe(TagsFormulario.NOMBRE) }
        c.esperar {
            c.onNodeWithTagScroll(TagsFormulario.NOMBRE).textoEditable().isNotEmpty() &&
                c.onAllNodes(hasText("Drama") and isToggleable()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun ajustar() {
        tocar(TagsDetalle.AJUSTAR)
        c.esperar { c.existe(TagsDetalle.EDITOR_NOTA) }
    }

    fun escribirNota(texto: String) {
        nodo(TagsDetalle.EDITOR_NOTA_CAMPO).performTextReplacement(texto)
        c.waitForIdle()
    }

    /** Pulsa "Guardar" en el editor de nota y espera a que se cierre. */
    fun guardarNota() {
        tocar(TagsDetalle.EDITOR_NOTA_GUARDAR)
        c.esperar { !c.existe(TagsDetalle.EDITOR_NOTA) }
    }

    fun abrirCritica() {
        tocar(TagsDetalle.EDITAR_CRITICA)
        c.esperar { c.existe(TagsHoja.HOJA) }
    }

    fun escribirCritica(texto: String) {
        c.onNode(hasTestTag(TagsDetalle.CRITICA_CAMPO)).performTextReplacement(texto)
        c.waitForIdle()
    }

    fun guardarCritica() {
        c.onNode(hasTestTag(TagsHoja.ACCION)).performClick()
        c.esperar { !c.existe(TagsHoja.HOJA) }
    }

    fun estado(estado: Estado) = tocar(TagsDetalle.estado(estado))
}

/**
 * Ejecuta [accion] con el reloj de Compose parado y espera a que aparezca el toast [texto]: así no desaparece (dura
 * 2,2 s) antes de comprobarlo.
 */
fun ComposeTestRule.esperarToast(
    texto: String,
    accion: () -> Unit,
) {
    mainClock.autoAdvance = false
    try {
        accion()
        esperar {
            mainClock.advanceTimeByFrame()
            onAllNodes(hasText(texto)).fetchSemanticsNodes().isNotEmpty()
        }
    } finally {
        mainClock.autoAdvance = true
    }
}

/** Nodo por etiqueta, desplazando el formulario hasta él si hace falta. */
fun ComposeTestRule.onNodeWithTagScroll(tag: String): SemanticsNodeInteraction {
    val nodo = onNode(hasTestTag(tag))
    runCatching { nodo.performScrollTo() }
    return nodo
}
