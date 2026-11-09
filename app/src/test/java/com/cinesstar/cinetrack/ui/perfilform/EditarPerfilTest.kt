package com.cinesstar.cinetrack.ui.perfilform

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.cinesstar.cinetrack.data.repository.PerfilDetalle
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.soporte.RobotDetalle
import com.cinesstar.cinetrack.soporte.RobotFormulario
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.soporte.existeTexto
import com.cinesstar.cinetrack.soporte.textoEditable
import com.cinesstar.cinetrack.ui.components.TagsDialogo
import com.cinesstar.cinetrack.ui.perfil.TagsDetalle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests UI del S3 sobre "Editar Medio" (CU-02), que se abre desde el menú ⋮ del detalle. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class EditarPerfilTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val form = RobotFormulario(compose)
    private val detalle = RobotDetalle(compose)

    private fun nodo(tag: String) = form.nodo(tag)

    private fun leer(id: Long): PerfilDetalle = enBd { perfiles.obtener(id)!! }

    private fun ids(
        nombres: Set<String>,
        idiomas: Boolean = false,
    ): Set<Long> =
        enBd {
            (if (idiomas) catalogo.idiomas().map { it.id to it.nombre } else catalogo.generos().map { it.id to it.nombre })
                .filter { it.second in nombres }
                .map { it.first }
                .toSet()
        }

    /** Crea un perfil en la base de datos, abre su detalle y después "Editar". */
    private fun editar(nuevo: NuevoPerfil): Long {
        val id = enBd { perfiles.crear(nuevo) }
        detalle.abrir(id)
        detalle.editar()
        return id
    }

    private fun guardar() {
        form.guardar(TagsFormulario.PANTALLA_EDITAR)
        compose.esperar { compose.existe(TagsDetalle.TITULO) }
    }

    private fun elegirSaga(nombre: String) {
        form.tocar(TagsFormulario.SAGA)
        compose.onNode(hasText(nombre) and !hasTestTag(TagsFormulario.SAGA)).performClick()
        compose.waitForIdle()
    }

    private fun enDetalle(
        seccion: String,
        texto: String,
    ) = compose
        .onNode(hasText(texto) and hasAnyAncestor(hasTestTag(seccion)), useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()

    @Test
    fun t3_01_cambiarNombreGenerosIdiomasYSaga() {
        val (dune, arrakis) = enBd { sagas.crear("Dune") to sagas.crear("Arrakis") }
        val id =
            editar(
                NuevoPerfil(
                    "Dune",
                    TipoPerfil.PELICULA,
                    sagaId = dune,
                    generoIds = ids(setOf("Drama")),
                    idiomaIds = ids(setOf("Español"), idiomas = true),
                ),
            )
        compose.onNode(hasText("Editar Medio"), useUnmergedTree = true).assertIsDisplayed()
        nodo(TagsFormulario.SAGA).assertTextContains("Dune")
        nodo(TagsFormulario.CONTADOR_GENEROS).assertTextContains("1 seleccionado")

        form.nombre("Dune: Parte Uno")
        form.chip("Drama")
        form.chip("Acción")
        form.chip("Aventura")
        form.chip("Inglés")
        elegirSaga("Arrakis")
        guardar()

        val p = leer(id)
        assertEquals("Dune: Parte Uno", p.perfil.nombre)
        assertEquals(arrakis, p.perfil.sagaId)
        assertEquals(listOf("Acción", "Aventura"), p.generos.map { it.nombre })
        assertEquals(listOf("Español", "Inglés"), p.idiomas.map { it.nombre })
        compose.onNodeWithTag(TagsDetalle.TITULO).assertTextContains("Dune: Parte Uno")
        enDetalle(TagsDetalle.SAGA, "Saga Arrakis")
        enDetalle(TagsDetalle.GENEROS, "Acción")
        enDetalle(TagsDetalle.IDIOMAS, "Inglés")
    }

    @Test
    fun t3_01_laSagaSePuedeCrearDesdeLaEdicion() {
        val id = editar(NuevoPerfil("Matrix", TipoPerfil.PELICULA))
        form.tocar(TagsFormulario.CREAR_SAGA)
        form.escribir(TagsFormulario.SAGA_CAMPO, "Matrix")
        form.tocar(TagsFormulario.SAGA_GUARDAR)
        guardar()
        assertEquals("Matrix", leer(id).saga?.nombre)
    }

    @Test
    fun t3_02_quitarLaSaga() {
        val saga = enBd { sagas.crear("Matrix") }
        val id = editar(NuevoPerfil("Matrix", TipoPerfil.PELICULA, sagaId = saga))
        elegirSaga("Ninguna / Obra individual")
        guardar()
        assertNull(leer(id).perfil.sagaId)
        enDetalle(TagsDetalle.SAGA, "Sin saga asociada")
        assertEquals("La saga sigue registrada (S-08)", 1, enBd { sagas.todas() }.size)
    }

    @Test
    fun t3_03_quitarTodosLosGenerosEIdiomas() {
        val id =
            editar(
                NuevoPerfil(
                    "Con todo",
                    TipoPerfil.SERIE,
                    generoIds = ids(setOf("Drama", "Comedia")),
                    idiomaIds = ids(setOf("Japonés"), idiomas = true),
                ),
            )
        form.chip("Drama")
        form.chip("Comedia")
        form.chip("Japonés")
        nodo(TagsFormulario.CONTADOR_GENEROS).assertTextContains("0 seleccionados")
        guardar()
        val p = leer(id)
        assertTrue(p.generos.isEmpty() && p.idiomas.isEmpty())
        enDetalle(TagsDetalle.GENEROS, "—")
        enDetalle(TagsDetalle.IDIOMAS, "—")
    }

    @Test
    fun t3_04_guardarSinCambiosYCancelarConCambiosNoModificanNada() {
        val id =
            editar(
                NuevoPerfil(
                    "Intacto",
                    TipoPerfil.PELICULA,
                    Estado.VIENDO,
                    Puntuacion.of(85),
                    comentario = "Notas",
                    generoIds = ids(setOf("Drama")),
                ),
            )
        val antes = leer(id)
        // A3: guardar sin cambios vuelve al detalle sin escribir nada.
        guardar()
        assertEquals(antes, leer(id))

        // A4: cancelar sin cambios vuelve directamente; con cambios pide confirmación.
        detalle.editar()
        form.tocar(TagsFormulario.CANCELAR)
        compose.esperar { compose.existe(TagsDetalle.TITULO) && !compose.existe(TagsFormulario.PANTALLA_EDITAR) }
        assertFalse(compose.existe(TagsDialogo.DIALOGO))

        detalle.editar()
        form.nombre("Cambiado")
        form.chip("Comedia")
        form.puntuacion("2")
        form.tocar(TagsFormulario.CANCELAR)
        compose.onNodeWithTag(TagsDialogo.DIALOGO).assertIsDisplayed()
        compose.onNode(hasText("¿Descartar los cambios?"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(TagsDialogo.CONFIRMAR).performClick()
        compose.esperar { compose.existe(TagsDetalle.TITULO) && !compose.existe(TagsFormulario.PANTALLA_EDITAR) }
        assertEquals(antes, leer(id))
    }

    @Test
    fun t3_05_elTipoNoSePuedeCambiar() {
        val id = editar(NuevoPerfil("Breaking Bad", TipoPerfil.SERIE))
        nodo(TagsFormulario.tipo(TipoPerfil.SERIE)).assertIsSelected().assertIsNotEnabled()
        nodo(TagsFormulario.tipo(TipoPerfil.PELICULA)).assertIsNotEnabled()
        form.nombre("Breaking Bad 2")
        guardar()
        assertEquals(TipoPerfil.SERIE, leer(id).perfil.tipo)
    }

    @Test
    fun t3_06_nombreVacioMuestraM01() {
        val id = editar(NuevoPerfil("Con nombre", TipoPerfil.PELICULA))
        form.nombre("   ")
        compose.esperar { compose.existe(TagsFormulario.NOMBRE_ERROR) }
        form.esperarTexto("El nombre es obligatorio.")
        nodo(TagsFormulario.GUARDAR).assertIsNotEnabled()
        form.nombre("")
        form.esperarTexto("El nombre es obligatorio.")
        assertEquals("Con nombre", leer(id).perfil.nombre)
    }

    @Test
    fun t3_07_renombrarAlNombreDeOtroPerfilDelMismoTipoMuestraM02() {
        enBd { perfiles.crear(NuevoPerfil("El Padrino", TipoPerfil.PELICULA)) }
        enBd { perfiles.crear(NuevoPerfil("Otro", TipoPerfil.SERIE)) }
        val id = editar(NuevoPerfil("Uno", TipoPerfil.PELICULA))
        form.nombre("el PÁDRINO")
        form.esperarTexto("Ya existe una película con ese nombre.")
        nodo(TagsFormulario.GUARDAR).assertIsNotEnabled()
        // El de una serie no cuenta (RES-06).
        form.nombre("Otro")
        form.esperarHabilitado()
        guardar()
        assertEquals("Otro", leer(id).perfil.nombre)
    }

    @Test
    fun t3_08_cambiarSoloMayusculasOTildesDelPropioNombreEsValido() {
        val id = editar(NuevoPerfil("El padrino", TipoPerfil.PELICULA))
        form.esperarTexto("Disponible")
        form.nombre("EL PÁDRINO")
        form.esperarHabilitado()
        form.esperarTexto("Disponible")
        assertFalse(compose.existeTexto("Ya registrado"))
        guardar()
        assertEquals("EL PÁDRINO", leer(id).perfil.nombre)
    }

    @Test
    fun cu02_postcondicion_sigueEnSusListasConLaMismaPosicion() {
        val (id, otro) =
            enBd {
                val otro = perfiles.crear(NuevoPerfil("Primero", TipoPerfil.PELICULA))
                val id = perfiles.crear(NuevoPerfil("Segundo", TipoPerfil.PELICULA))
                val r = listas.crear("Top", TipoLista.RANKING)
                listas.anadir(r, otro)
                listas.anadir(r, id)
                id to otro
            }
        detalle.abrir(id)
        detalle.editar()
        assertEquals("Segundo", nodo(TagsFormulario.NOMBRE).textoEditable())
        form.nombre("Segundo renombrado")
        guardar()
        enDetalle(TagsDetalle.LISTAS, "Posición actual: #2 de 2 títulos")
        val lista = listasDe(id).single()
        val ranking = enBd { listas.elementos(lista) }
        assertEquals(listOf(otro to 1, id to 2), ranking.map { it.perfilId to it.posicion })
    }

    private fun listasDe(id: Long): List<Long> = enBd { database.listaDao().listasDe(id).map { it.listaId } }
}
