package com.cinesstar.cinetrack.ui.perfil

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.AppContainer
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.room.PerfilDao
import com.cinesstar.cinetrack.data.room.PerfilEntity
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.soporte.RobotDetalle
import com.cinesstar.cinetrack.soporte.aplicacion
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.esperarToast
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.soporte.existeTexto
import com.cinesstar.cinetrack.soporte.textoEditable
import com.cinesstar.cinetrack.ui.components.TagsBarra
import com.cinesstar.cinetrack.ui.components.TagsDialogo
import com.cinesstar.cinetrack.ui.components.TagsHoja
import com.cinesstar.cinetrack.ui.components.tagPantalla
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

/** Tests UI del S3 sobre el detalle: puntuación (CU-03), crítica (CU-04), estado (CU-05) y eliminación (CU-06). */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class DetalleAccionesTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val detalle = RobotDetalle(compose)

    private fun crear(nuevo: NuevoPerfil): Long = enBd { perfiles.crear(nuevo) }

    private fun abrir(nuevo: NuevoPerfil): Long = crear(nuevo).also { detalle.abrir(it) }

    private fun leer(id: Long): PerfilEntity? = enBd { perfiles.obtener(id)?.perfil }

    private fun esperarEnBd(
        id: Long,
        condicion: (PerfilEntity) -> Boolean,
    ) = compose.esperar { leer(id)?.let(condicion) == true }

    private fun nota(): String =
        compose
            .onNodeWithTag(TagsDetalle.NOTA)
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString { it.text }

    private fun esperarNota(texto: String) = compose.esperar { nota() == texto }

    private fun comentario(texto: String) =
        compose
            .onNode(hasText(texto) and hasAnyAncestor(hasTestTag(TagsDetalle.COMENTARIO)), useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()

    /** Sustituye el DAO de perfiles por uno que falla en [fallo] (antes de abrir el detalle). */
    private fun conDaoQueFalla(fallo: (PerfilDao) -> PerfilDao) {
        val real = aplicacion.container
        aplicacion.reemplazarContenedor(
            AppContainer(real.database, perfiles = PerfilRepository(real.database, perfilDao = fallo(real.database.perfilDao()))),
        )
    }

    // ---- CU-03 Puntuar ----

    @Test
    fun t3_09_anadirPuntuacionAUnPerfilSinElla() {
        val id = abrir(NuevoPerfil("Sin nota", TipoPerfil.PELICULA))
        compose.onNodeWithTag(TagsDetalle.NOTA).assertTextContains("—")
        detalle.ajustar()
        assertEquals("", detalle.nodo(TagsDetalle.EDITOR_NOTA_CAMPO).textoEditable())
        detalle.escribirNota("8,5")
        detalle.guardarNota()
        esperarEnBd(id) { it.puntuacionDecimas == 85 }
        esperarNota("8.5")
        compose.onNodeWithTag(TagsDetalle.RANGO).assertTextContains("Muy buena")
    }

    @Test
    fun t3_10_cambiarUnaPuntuacionExistente() {
        val id = abrir(NuevoPerfil("Con nota", TipoPerfil.PELICULA, puntuacion = Puntuacion.of(70)))
        detalle.ajustar()
        assertEquals("7.0", detalle.nodo(TagsDetalle.EDITOR_NOTA_CAMPO).textoEditable())
        detalle.escribirNota("10")
        detalle.guardarNota()
        esperarEnBd(id) { it.puntuacionDecimas == 100 }
        esperarNota("10")
    }

    @Test
    fun t3_10_elSliderCambiaElCampo() {
        abrir(NuevoPerfil("Slider", TipoPerfil.PELICULA, puntuacion = Puntuacion.of(50)))
        detalle.ajustar()
        compose
            .onNodeWithTag(TagsDetalle.EDITOR_NOTA_SLIDER)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(9.4f) }
        compose.esperar { detalle.nodo(TagsDetalle.EDITOR_NOTA_CAMPO).textoEditable() == "9.4" }
    }

    @Test
    fun t3_11_quitarLaPuntuacionConElBotonYVaciandoElCampo() {
        val a = abrir(NuevoPerfil("Quitar con botón", TipoPerfil.PELICULA, puntuacion = Puntuacion.of(64)))
        detalle.ajustar()
        detalle.tocar(TagsDetalle.QUITAR_NOTA)
        esperarEnBd(a) { it.puntuacionDecimas == null }
        compose.esperar { !compose.existe(TagsDetalle.EDITOR_NOTA) }
        compose.onNodeWithTag(TagsDetalle.RANGO).assertTextContains("Sin puntuar")

        compose.onNodeWithTag(TagsBarra.ATRAS).performClick()
        val b = crear(NuevoPerfil("Quitar vaciando", TipoPerfil.SERIE, puntuacion = Puntuacion.of(50)))
        detalle.abrir(b)
        detalle.ajustar()
        detalle.escribirNota("")
        detalle.guardarNota()
        esperarEnBd(b) { it.puntuacionDecimas == null }
        esperarNota("—")
    }

    @Test
    fun t3_12_valoresInvalidosMuestranM03YNoCambianLaNota() {
        val id = abrir(NuevoPerfil("Inválidos", TipoPerfil.PELICULA, puntuacion = Puntuacion.of(70)))
        detalle.ajustar()
        listOf("-0.1", "-1", "10.1", "11", "100", "7.25", "8.55", "abc", "7a", "1e1", "7 .5", ".5", "5.").forEach { valor ->
            detalle.escribirNota(valor)
            compose.esperar { compose.existe(TagsDetalle.EDITOR_NOTA_ERROR) }
            assertTrue(
                valor,
                compose.existeTexto("Puntuación no válida. Introduce un valor entre 0.0 y 10 con un máximo de un decimal."),
            )
            detalle.nodo(TagsDetalle.EDITOR_NOTA_GUARDAR).assertIsNotEnabled()
            detalle.nodo(TagsDetalle.EDITOR_NOTA_CAMPO).performImeAction()
            compose.waitForIdle()
        }
        assertEquals(70, leer(id)!!.puntuacionDecimas)
        assertEquals("7.0", nota())
        detalle.escribirNota("7.5")
        detalle.nodo(TagsDetalle.EDITOR_NOTA_GUARDAR).assertIsEnabled()
        assertFalse(compose.existe(TagsDetalle.EDITOR_NOTA_ERROR))
    }

    @Test
    fun t3_13_sePuedePuntuarEnCualquierEstado() {
        Estado.entries.forEachIndexed { i, estado ->
            val id = abrir(NuevoPerfil("Estado $estado", TipoPerfil.PELICULA, estado))
            detalle.ajustar()
            detalle.escribirNota("${i + 6}")
            detalle.guardarNota()
            esperarEnBd(id) { it.puntuacionDecimas == (i + 6) * 10 && it.estado == estado }
            compose.onNodeWithTag(TagsBarra.ATRAS).performClick()
            compose.esperar { compose.existe(DestinoBarra.BIBLIOTECA.tagPantalla()) }
        }
    }

    @Test
    fun t3_14_unaSerieTieneUnaSolaNotaGlobal() {
        val id = abrir(NuevoPerfil("Serie larga", TipoPerfil.SERIE, puntuacion = Puntuacion.of(80)))
        detalle.ajustar()
        listOf("Temporada", "temporada", "Episodio", "episodio", "Capítulo").forEach {
            assertFalse(it, compose.existeTexto(it, subcadena = true))
        }
        assertEquals(1, compose.onAllNodes(hasTestTag(TagsDetalle.EDITOR_NOTA_CAMPO)).fetchSemanticsNodes().size)
        detalle.escribirNota("9")
        detalle.guardarNota()
        esperarEnBd(id) { it.puntuacionDecimas == 90 }
    }

    // ---- CU-04 Comentario ----

    @Test
    fun t3_15_anadirComentario() {
        val id = abrir(NuevoPerfil("Sin crítica", TipoPerfil.PELICULA))
        comentario("Sin reseña redactada todavía.")
        detalle.abrirCritica()
        detalle.escribirCritica("Primera línea\nSegunda 🎬")
        detalle.guardarCritica()
        esperarEnBd(id) { it.comentario == "Primera línea\nSegunda 🎬" && it.comentarioEditadoEn != null }
        comentario("“Primera línea\nSegunda 🎬”")
        compose.onNode(hasText("Guardado: ", substring = true), useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun t3_16_cambiarComentario() {
        val id = abrir(NuevoPerfil("Con crítica", TipoPerfil.PELICULA, comentario = "Antigua"))
        detalle.abrirCritica()
        assertEquals("Antigua", compose.onNodeWithTag(TagsDetalle.CRITICA_CAMPO).textoEditable())
        detalle.escribirCritica("Nueva")
        detalle.guardarCritica()
        esperarEnBd(id) { it.comentario == "Nueva" }
        comentario("“Nueva”")
    }

    @Test
    fun t3_17_borrarComentarioConElBotonYVaciandoElTexto() {
        val a = abrir(NuevoPerfil("Borrar con botón", TipoPerfil.PELICULA, comentario = "Fuera"))
        detalle.tocar(TagsDetalle.BORRAR_CRITICA)
        compose.onNodeWithTag(TagsDialogo.DIALOGO).assertIsDisplayed()
        compose.onNodeWithTag(TagsDialogo.CONFIRMAR).performClick()
        esperarEnBd(a) { it.comentario == null && it.comentarioEditadoEn == null }
        comentario("Sin reseña redactada todavía.")
        detalle.nodo(TagsDetalle.BORRAR_CRITICA).assertIsNotEnabled()

        compose.onNodeWithTag(TagsBarra.ATRAS).performClick()
        val b = crear(NuevoPerfil("Borrar vaciando", TipoPerfil.PELICULA, comentario = "Fuera"))
        detalle.abrir(b)
        detalle.abrirCritica()
        detalle.escribirCritica("")
        detalle.guardarCritica()
        esperarEnBd(b) { it.comentario == null }
        comentario("Sin reseña redactada todavía.")
    }

    @Test
    fun t3_17_cancelarElBorradoDelComentarioNoLoBorra() {
        val id = abrir(NuevoPerfil("Conservar", TipoPerfil.PELICULA, comentario = "Se queda"))
        detalle.tocar(TagsDetalle.BORRAR_CRITICA)
        compose.onNodeWithTag(TagsDialogo.CANCELAR).performClick()
        compose.waitForIdle()
        assertEquals("Se queda", leer(id)!!.comentario)
    }

    @Test
    fun t3_18_cancelarTextoDe10000CaracteresYLimite() {
        val id = abrir(NuevoPerfil("Largo", TipoPerfil.PELICULA, comentario = "Original"))
        // A2: cancelar descarta lo escrito.
        detalle.abrirCritica()
        detalle.escribirCritica("Descartado")
        compose.onNodeWithTag(TagsHoja.CANCELAR).performClick()
        compose.esperar { !compose.existe(TagsHoja.HOJA) }
        assertEquals("Original", leer(id)!!.comentario)

        // A3: 10 000 caracteres se guardan íntegros.
        // Saltos de línea, tildes y emojis; 10 000 unidades exactas sin partir ningún emoji.
        val largo = "Línea con tilde ñ 🎬\n".repeat(LINEAS_LARGO).take(10_000)
        detalle.abrirCritica()
        detalle.escribirCritica(largo)
        compose.onNodeWithTag(TagsDetalle.CRITICA_CONTADOR, useUnmergedTree = true).assertTextEquals("10000 / 10000")
        detalle.guardarCritica()
        esperarEnBd(id) { it.comentario == largo }

        // E2: no deja escribir más del límite.
        detalle.abrirCritica()
        detalle.escribirCritica(largo + "xyz")
        assertEquals(largo, compose.onNodeWithTag(TagsDetalle.CRITICA_CAMPO).textoEditable())
        detalle.guardarCritica()
        esperarEnBd(id) { it.comentario == largo }
    }

    // ---- CU-05 Estado ----

    @Test
    fun t3_19_lasSeisTransicionesSonPosiblesYPersisten() {
        val id = abrir(NuevoPerfil("Transiciones", TipoPerfil.SERIE))
        // P→Vi, Vi→Vt, Vt→P, P→Vt, Vt→Vi, Vi→P
        listOf(Estado.VIENDO, Estado.VISTO, Estado.PENDIENTE, Estado.VISTO, Estado.VIENDO, Estado.PENDIENTE).forEach { destino ->
            detalle.estado(destino)
            esperarEnBd(id) { it.estado == destino }
            compose.esperar { runCatching { compose.onNodeWithTag(TagsDetalle.estado(destino)).assertIsSelected() }.isSuccess }
            Estado.entries.filter { it != destino }.forEach { compose.onNodeWithTag(TagsDetalle.estado(it)).assertIsNotSelected() }
            compose.onNodeWithTag(TagsDetalle.ESTADO_HERO).assertTextContains(
                when (destino) {
                    Estado.PENDIENTE -> "Pendiente"
                    Estado.VIENDO -> "Viendo"
                    Estado.VISTO -> "Visto"
                },
            )
        }
    }

    @Test
    fun t3_20_tocarElEstadoActivoNoCambiaNada() {
        // Si tocar el estado activo escribiera, este DAO fallaría y saldría M-25.
        conDaoQueFalla { dao ->
            object : PerfilDao by dao {
                override suspend fun actualizarEstado(
                    perfilId: Long,
                    estado: Estado,
                ): Int = throw IOException("No debía escribir")
            }
        }
        val id = abrir(NuevoPerfil("Activo", TipoPerfil.PELICULA))
        detalle.estado(Estado.PENDIENTE)
        compose.mainClock.advanceTimeBy(500)
        assertFalse(compose.existeTexto("Error inesperado. Tus datos no se han modificado."))
        assertEquals(Estado.PENDIENTE, leer(id)!!.estado)
    }

    @Test
    fun t3_20_pendienteAVistoSinPuntuacion() {
        val id = abrir(NuevoPerfil("Visto sin nota", TipoPerfil.PELICULA))
        detalle.estado(Estado.VISTO)
        esperarEnBd(id) { it.estado == Estado.VISTO && it.puntuacionDecimas == null }
    }

    @Test
    fun t3_21_cambiarElEstadoNoModificaNotaNiComentario() {
        val id = abrir(NuevoPerfil("Intacto", TipoPerfil.PELICULA, puntuacion = Puntuacion.of(77), comentario = "Notas"))
        val antes = leer(id)!!
        detalle.estado(Estado.VIENDO)
        esperarEnBd(id) { it.estado == Estado.VIENDO }
        assertEquals(antes.copy(estado = Estado.VIENDO), leer(id))
        esperarNota("7.7")
        comentario("“Notas”")
    }

    @Test
    fun t3_22_unFalloAlCambiarElEstadoMuestraM25YVuelveAlAnterior() {
        conDaoQueFalla { dao ->
            object : PerfilDao by dao {
                override suspend fun actualizarEstado(
                    perfilId: Long,
                    estado: Estado,
                ): Int = throw IOException("Disco lleno")
            }
        }
        val id = abrir(NuevoPerfil("Fallo", TipoPerfil.PELICULA, Estado.VIENDO))
        compose.esperarToast("Error inesperado. Tus datos no se han modificado.") { detalle.estado(Estado.VISTO) }
        compose.onNodeWithTag(TagsDetalle.estado(Estado.VIENDO)).assertIsSelected()
        compose.onNodeWithTag(TagsDetalle.estado(Estado.VISTO)).assertIsNotSelected()
        assertEquals(Estado.VIENDO, leer(id)!!.estado)
    }

    // ---- CU-06 Eliminar ----

    @Test
    fun t3_23_eliminarMuestraM06ConElNombre() {
        abrir(NuevoPerfil("Dune: Parte Dos", TipoPerfil.PELICULA))
        detalle.tocar(TagsDetalle.ELIMINAR)
        compose.onNodeWithTag(TagsDialogo.DIALOGO).assertIsDisplayed()
        compose.onNode(hasText("¿Eliminar del catálogo?"), useUnmergedTree = true).assertIsDisplayed()
        compose
            .onNodeWithTag(TagsDialogo.TEXTO, useUnmergedTree = true)
            .assertTextEquals("¿Eliminar «Dune: Parte Dos»? Se quitará también de todas tus listas. Esta acción no se puede deshacer.")
        compose.onNodeWithTag(TagsDialogo.CONFIRMAR).assertTextContains("Sí, eliminar definitivamente")
        compose.onNodeWithTag(TagsDialogo.CANCELAR).performClick()

        // También desde el menú ⋮.
        detalle.menu(TagsDetalle.MENU_ELIMINAR)
        compose.onNodeWithTag(TagsDialogo.DIALOGO).assertIsDisplayed()
    }

    @Test
    fun t3_24_cancelarLaConfirmacionNoBorra() {
        val id = abrir(NuevoPerfil("Se queda", TipoPerfil.PELICULA))
        detalle.tocar(TagsDetalle.ELIMINAR)
        compose.onNodeWithTag(TagsDialogo.CANCELAR).performClick()
        compose.waitForIdle()
        assertFalse(compose.existe(TagsDialogo.DIALOGO))
        assertTrue(compose.existe(TagsDetalle.PANTALLA))
        assertNotNull(leer(id))
    }

    @Test
    fun cu06_fp_confirmarBorraYVuelveConPerfilEliminado() {
        val (id, lista) =
            enBd {
                val p = perfiles.crear(NuevoPerfil("Adiós", TipoPerfil.PELICULA))
                val l = listas.crear("Mi lista", TipoLista.NORMAL)
                listas.anadir(l, p)
                p to l
            }
        detalle.abrir(id)
        detalle.tocar(TagsDetalle.ELIMINAR)
        compose.esperarToast("Perfil eliminado") { compose.onNodeWithTag(TagsDialogo.CONFIRMAR).performClick() }
        compose.esperar { compose.existe(DestinoBarra.BIBLIOTECA.tagPantalla()) && !compose.existe(TagsDetalle.PANTALLA) }
        assertFalse("No sale el aviso de CU-07 E1", compose.existeTexto("Este título ya no existe."))
        assertNull(leer(id))
        assertTrue(enBd { listas.elementos(lista) }.isEmpty())
    }

    @Test
    fun cu06_e1_unFalloAlBorrarMuestraM25YNoBorra() {
        conDaoQueFalla { dao ->
            object : PerfilDao by dao {
                override suspend fun eliminar(id: Long): Int = throw IOException("Disco lleno")
            }
        }
        val id = abrir(NuevoPerfil("Resiste", TipoPerfil.PELICULA))
        detalle.tocar(TagsDetalle.ELIMINAR)
        compose.esperarToast("Error inesperado. Tus datos no se han modificado.") {
            compose.onNodeWithTag(TagsDialogo.CONFIRMAR).performClick()
        }
        assertTrue(compose.existe(TagsDetalle.PANTALLA))
        assertNotNull(leer(id))
    }

    // ---- CU-07 E1 y RNF-09 ----

    @Test
    fun t3_30_siElPerfilSeBorraMientrasSeVeVuelveAtrasConAviso() {
        val id = abrir(NuevoPerfil("Efímero", TipoPerfil.PELICULA))
        compose.esperarToast("Este título ya no existe.") { enBd { perfiles.eliminar(id) } }
        compose.esperar { compose.existe(DestinoBarra.BIBLIOTECA.tagPantalla()) && !compose.existe(TagsDetalle.PANTALLA) }
    }

    @Test
    fun t3_31_puntuarUnPerfilExistenteEnTresPasos() {
        val id = abrir(NuevoPerfil("Tres pasos", TipoPerfil.PELICULA))
        // 1) Ajustar, 2) escribir la nota, 3) Guardar (RNF-09).
        detalle.ajustar()
        detalle.escribirNota("9.2")
        detalle.guardarNota()
        esperarEnBd(id) { it.puntuacionDecimas == 92 }
    }
}

private const val LINEAS_LARGO = 500
