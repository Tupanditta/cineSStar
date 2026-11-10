package com.cinesstar.cinetrack.soporte

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.ui.biblioteca.TagsBiblioteca
import com.cinesstar.cinetrack.ui.buscar.TagsBuscar
import com.cinesstar.cinetrack.ui.components.tag
import com.cinesstar.cinetrack.ui.datos.TagsDatos
import com.cinesstar.cinetrack.ui.lista.TagsLista
import com.cinesstar.cinetrack.ui.listas.TagsListas
import com.cinesstar.cinetrack.ui.perfil.TagsDetalle

/**
 * Revisión automática de todas las pantallas (S9): recorre la app con el fixture pequeño y anota los problemas de
 * adaptación (T9.07, T9.08) y de accesibilidad (T9.10) que encuentra en cada una.
 */
class Revision(
    private val c: ComposeTestRule,
    private val densidad: Float,
) {
    val problemas = mutableListOf<String>()

    /** Recorre Biblioteca, detalle, Listas, lista abierta, Buscar (las dos pestañas), Ajustes y Añadir. */
    fun recorrer(
        datos: DatosFixture,
        comprobar: (String) -> Unit,
        atras: () -> Unit,
    ) {
        // El último en crearse: la Biblioteca lo muestra el primero (S-10).
        val dune = datos.perfiles.getValue("Ñandú y compañía|SERIE")
        val ranking = datos.listas.getValue(FixturePequeno.LISTA_RANKING)
        ir(DestinoBarra.BIBLIOTECA)
        verEn(TagsBiblioteca.LISTA, TagsBiblioteca.fila(dune))
        comprobar("Biblioteca")
        c.onNode(hasTestTag(TagsBiblioteca.fila(dune))).performSemanticsAction(SemanticsActions.OnClick)
        c.esperar { c.existe(TagsDetalle.TITULO) }
        comprobar("Detalle")
        atras()
        ir(DestinoBarra.LISTAS)
        verEn(TagsListas.LISTA, TagsListas.tarjeta(ranking))
        comprobar("Listas")
        c.onNode(hasTestTag(TagsListas.tarjeta(ranking))).performSemanticsAction(SemanticsActions.OnClick)
        c.esperar { c.existe(TagsLista.TITULO) && c.existeTexto("Pádrino") }
        comprobar("Lista abierta")
        atras()
        ir(DestinoBarra.BUSCAR)
        c.esperar { c.existe(TagsBuscar.COINCIDENCIAS) }
        comprobar("Buscar")
        c.onNode(hasTestTag(TagsBuscar.TAB_DATOS)).performClick()
        c.esperar { c.existe(TagsDatos.BANNER) }
        comprobar("Datos Locales")
        ir(DestinoBarra.AJUSTES)
        c.esperar { c.existe(TagsDatos.LISTA_AJUSTES) }
        comprobar("Ajustes")
        ir(DestinoBarra.ANADIR)
        c.esperar { c.existeTexto("Drama") }
        comprobar("Añadir")
    }

    /** Espera a la lista [lista] y la desplaza hasta [tag] (con la fuente grande puede quedar fuera de la pantalla). */
    private fun verEn(
        lista: String,
        tag: String,
    ) {
        c.esperar { c.existe(lista) }
        c.onNode(hasTestTag(lista)).performScrollToNode(hasTestTag(tag))
        c.waitForIdle()
    }

    private fun ir(destino: DestinoBarra) {
        c.onNode(hasTestTag(destino.tag())).performSemanticsAction(SemanticsActions.OnClick)
        c.waitForIdle()
    }

    private fun anchoPx(): Float = raices(unmerged = true).maxOf { it.boundsInRoot.width }

    private fun raices(unmerged: Boolean): List<SemanticsNode> = c.onAllNodes(isRoot(), useUnmergedTree = unmerged).fetchSemanticsNodes()

    private fun SemanticsNode.todos(): List<SemanticsNode> = listOf(this) + children.flatMap { it.todos() }

    private fun SemanticsNode.dentroDeScrollHorizontal(): Boolean =
        generateSequence(parent) { it.parent }.any { it.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange) != null }

    private fun SemanticsNode.texto(): String =
        config
            .getOrNull(SemanticsProperties.Text)
            .orEmpty()
            .joinToString { it.text }

    private fun Rect.describir() = "[${(left / densidad).toInt()}..${(right / densidad).toInt()} dp]"

    /** T9.07: ningún elemento se sale por los lados (sin scroll horizontal), salvo dentro de una fila desplazable. */
    fun sinDesbordes(pantalla: String) {
        raices(unmerged = true).flatMap { it.todos() }.forEach { n ->
            val b = n.boundsInRoot
            if (b.width <= 0f || n.dentroDeScrollHorizontal()) return@forEach
            if (b.left < -TOLERANCIA_PX || b.right > anchoPx() + TOLERANCIA_PX) {
                problemas +=
                    "$pantalla: se sale por los lados ${b.describir()} «${n.texto().take(LARGO_TEXTO)}» ${n.config}".take(LARGO_PROBLEMA)
            }
        }
    }

    /** T9.08: los textos de los botones clave se ven enteros (sin cortar ni puntos suspensivos). */
    fun botonesEnteros(pantalla: String) {
        raices(unmerged = true).flatMap { it.todos() }.forEach { n ->
            val texto = n.texto()
            if (texto !in CLAVE && !texto.startsWith(APLICAR)) return@forEach
            val resultados = mutableListOf<TextLayoutResult>()
            n.config
                .getOrNull(SemanticsActions.GetTextLayoutResult)
                ?.action
                ?.invoke(resultados)
            resultados.filter { it.cortado() }.forEach {
                problemas +=
                    "$pantalla: «$texto» se corta (${it.multiParagraph.width.toInt()}x${it.multiParagraph.height.toInt()} px en ${it.size})"
            }
        }
    }

    /**
     * T9.10: todo lo que se puede pulsar tiene un nombre accesible (texto o `contentDescription`) y una zona táctil de
     * 48×48 dp como mínimo (§11).
     */
    fun accesible(pantalla: String) {
        raices(unmerged = false).flatMap { it.todos() }.forEach { n ->
            if (n.config.getOrNull(SemanticsActions.OnClick) == null) return@forEach
            val nombre =
                n.texto().isNotBlank() ||
                    n.config
                        .getOrNull(SemanticsProperties.ContentDescription)
                        .orEmpty()
                        .any { it.isNotBlank() } ||
                    n.config.getOrNull(SemanticsProperties.EditableText) != null
            val id = n.config.getOrNull(SemanticsProperties.TestTag) ?: n.texto().take(LARGO_TEXTO)
            if (!nombre) problemas += "$pantalla: «$id» se puede pulsar pero no tiene nombre accesible"
            val t = n.touchBoundsInRoot
            // Fuera de la parte visible (debajo de una lista recortada): no se puede tocar ahora mismo.
            if (t.width <= 0f || t.height <= 0f) return@forEach
            val minimo = MINIMO_TACTIL_DP * densidad - TOLERANCIA_PX
            if (t.width < minimo || t.height < minimo) {
                problemas += "$pantalla: «$id» zona táctil ${(t.width / densidad).toInt()}×${(t.height / densidad).toInt()} dp"
            }
        }
    }

    /**
     * Texto que no se ve entero: más líneas de las permitidas, puntos suspensivos, o líneas más anchas o altas que la caja
     * en la que se pinta. (`hasVisualOverflow` compara con el ancho de maquetación y marca textos que sí caben.)
     */
    private fun TextLayoutResult.cortado(): Boolean {
        val lineas = 0 until lineCount
        val ancho = lineas.maxOfOrNull { getLineRight(it) - getLineLeft(it) } ?: 0f
        return multiParagraph.didExceedMaxLines ||
            lineas.any { isLineEllipsized(it) } ||
            multiParagraph.height > size.height + TOLERANCIA_PX ||
            ancho > size.width + TOLERANCIA_PX
    }

    private companion object {
        const val TOLERANCIA_PX = 1f
        const val MINIMO_TACTIL_DP = 48
        const val LARGO_TEXTO = 40
        const val LARGO_PROBLEMA = 240
        const val APLICAR = "Aplicar Filtros ("

        /** Textos de los botones y pestañas clave (barra inferior, guardar, filtros, copia de seguridad). */
        val CLAVE =
            listOf(
                "Biblioteca",
                "Listas",
                "Añadir",
                "Buscar",
                "Ajustes",
                "Guardar Perfil",
                "+ Nueva Lista",
                "Limpiar",
                "Filtros Cine",
                "Datos Locales",
                "Generar y Guardar Archivo Local",
                "Seleccionar archivo .JSON",
            )
    }
}
