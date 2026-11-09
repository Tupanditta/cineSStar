package com.cinesstar.cinetrack.soporte

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
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.cinesstar.cinetrack.AppContainer
import com.cinesstar.cinetrack.CineTrackApplication
import com.cinesstar.cinetrack.data.room.PerfilEntity
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.ui.components.tag
import com.cinesstar.cinetrack.ui.components.tagPantalla
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

    /** Pulsa "Guardar Perfil" y espera a volver a la pantalla anterior. */
    fun guardar() {
        esperarHabilitado()
        tocar(TagsFormulario.GUARDAR)
        c.esperar { !c.existe(DestinoBarra.ANADIR.tagPantalla()) }
    }
}

/** Nodo por etiqueta, desplazando el formulario hasta él si hace falta. */
fun ComposeTestRule.onNodeWithTagScroll(tag: String): SemanticsNodeInteraction {
    val nodo = onNode(hasTestTag(tag))
    runCatching { nodo.performScrollTo() }
    return nodo
}
