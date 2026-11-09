package com.cinesstar.cinetrack.ui.perfil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.soporte.RobotFormulario
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.soporte.perfilesGuardados
import com.cinesstar.cinetrack.ui.biblioteca.TagsBiblioteca
import com.cinesstar.cinetrack.ui.components.tagPantalla
import com.cinesstar.cinetrack.ui.perfilform.TagsFormulario
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** T2.25–T2.27 y CU-07: detalle del perfil, al que se llega desde la Biblioteca. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class DetallePerfilTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun abrirDetalle(perfilId: Long) {
        compose.esperar { compose.existe(TagsBiblioteca.fila(perfilId)) }
        compose.onNodeWithTag(TagsBiblioteca.fila(perfilId)).performClick()
        compose.esperar { compose.existe(TagsDetalle.TITULO) }
    }

    /** Texto [texto] dentro de la sección con etiqueta [seccion]. */
    private fun enSeccion(
        seccion: String,
        texto: String,
    ) = compose
        .onNode(hasText(texto) and hasAnyAncestor(hasTestTag(seccion)), useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()

    private fun texto(
        texto: String,
        subcadena: Boolean = false,
    ) = compose.onNode(hasText(texto, substring = subcadena), useUnmergedTree = true).performScrollTo().assertIsDisplayed()

    @Test
    fun t2_25_elDetalleDelPerfilCreadoMuestraTodosSusDatos() {
        val form = RobotFormulario(compose)
        enBd { sagas.crear("Dune") }
        form.abrir()
        form.nombre("Dune: Parte Dos")
        form.estado(Estado.VISTO)
        form.puntuacion("9.4")
        form.tocar(TagsFormulario.SAGA)
        compose.onNode(hasText("Dune")).performClick()
        form.chip("Ciencia Ficción")
        form.chip("Aventura")
        form.chip("Español")
        form.chip("Inglés")
        form.comentario("Experiencia audiovisual abrumadora.")
        form.guardar()

        abrirDetalle(perfilesGuardados().single().id)
        compose.onNode(hasText("Detalle Película"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(TagsDetalle.TIPO).assertTextContains("Película")
        compose.onNodeWithTag(TagsDetalle.TITULO).assertTextContains("Dune: Parte Dos")
        compose.onNodeWithTag(TagsDetalle.estado(Estado.VISTO)).assertIsSelected()
        compose.onNodeWithTag(TagsDetalle.NOTA).assertTextContains("9.4")
        compose.onNodeWithTag(TagsDetalle.RANGO).assertTextContains("Imprescindible")
        enSeccion(TagsDetalle.SAGA, "Saga Dune")
        enSeccion(TagsDetalle.SAGA, "1 película guardada en tu archivo")
        enSeccion(TagsDetalle.GENEROS, "Ciencia Ficción")
        enSeccion(TagsDetalle.GENEROS, "Aventura")
        enSeccion(TagsDetalle.IDIOMAS, "Español")
        enSeccion(TagsDetalle.IDIOMAS, "Inglés")
        enSeccion(TagsDetalle.COMENTARIO, "“Experiencia audiovisual abrumadora.”")
        texto("Guardado: ", subcadena = true)
        texto("Aún no está en ninguna lista")
    }

    @Test
    fun t2_26_perfilSoloConNombreNoRompeElDiseno() {
        val id = enBd { perfiles.crear(NuevoPerfil("Solo nombre", TipoPerfil.SERIE)) }
        abrirDetalle(id)
        compose.onNode(hasText("Detalle Serie"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(TagsDetalle.NOTA).assertTextContains("—")
        compose.onNodeWithTag(TagsDetalle.RANGO).assertTextContains("Sin puntuar")
        enSeccion(TagsDetalle.SAGA, "Sin saga asociada")
        enSeccion(TagsDetalle.GENEROS, "—")
        enSeccion(TagsDetalle.IDIOMAS, "—")
        enSeccion(TagsDetalle.COMENTARIO, "Sin reseña redactada todavía.")
        texto("Aún no está en ninguna lista")
    }

    @Test
    fun t2_27_elEstadoSeVeEnElDetalle() {
        val id = enBd { perfiles.crear(NuevoPerfil("En curso", TipoPerfil.SERIE, Estado.VIENDO)) }
        abrirDetalle(id)
        compose.onNodeWithTag(TagsDetalle.estado(Estado.VIENDO)).assertIsSelected()
        compose.onNodeWithTag(TagsDetalle.estado(Estado.PENDIENTE)).assertIsNotSelected()
        compose.onNodeWithTag(TagsDetalle.estado(Estado.VISTO)).assertIsNotSelected()
        compose.onNodeWithTag(TagsDetalle.ESTADO_HERO).assertTextContains("Viendo")
    }

    @Test
    fun cu07_a2_muestraLasListasConPosicionYPrioridad() {
        val id =
            enBd {
                val p = perfiles.crear(NuevoPerfil("Dune: Parte Dos", TipoPerfil.PELICULA, puntuacion = Puntuacion.of(92)))
                val otro = perfiles.crear(NuevoPerfil("Oppenheimer", TipoPerfil.PELICULA))
                val ranking = listas.crear("Favoritas 2024", TipoLista.RANKING)
                listas.anadir(ranking, otro)
                listas.anadir(ranking, p, posicion = 1)
                listas.anadir(listas.crear("Maratón Otoño", TipoLista.PRIORIDAD), p, prioridad = Prioridad.ALTA)
                listas.anadir(listas.crear("Para el finde", TipoLista.NORMAL), p)
                p
            }
        abrirDetalle(id)
        texto("• #1 en Favoritas 2024")
        enSeccion(TagsDetalle.LISTAS, "Posición actual: #1 de 2 títulos")
        enSeccion(TagsDetalle.LISTAS, "Ranking #1")
        enSeccion(TagsDetalle.LISTAS, "Prioridad Alta en esta lista")
        enSeccion(TagsDetalle.LISTAS, "Prioridad Alta")
        enSeccion(TagsDetalle.LISTAS, "1 título")
        enSeccion(TagsDetalle.LISTAS, "Normal")
    }

    @Test
    fun cu07_e1_siElPerfilSeBorraMientrasSeVeVuelveAtras() {
        val id = enBd { perfiles.crear(NuevoPerfil("Efímero", TipoPerfil.PELICULA)) }
        abrirDetalle(id)
        enBd { perfiles.eliminar(id) }
        compose.esperar { compose.existe(DestinoBarra.BIBLIOTECA.tagPantalla()) && !compose.existe(TagsDetalle.PANTALLA) }
    }
}
