package com.cinesstar.cinetrack.ui.perfilform

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.AppContainer
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.room.PerfilDao
import com.cinesstar.cinetrack.data.room.PerfilEntity
import com.cinesstar.cinetrack.data.room.PerfilIdiomaEntity
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.soporte.RobotFormulario
import com.cinesstar.cinetrack.soporte.aplicacion
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.soporte.existeTexto
import com.cinesstar.cinetrack.soporte.perfilesGuardados
import com.cinesstar.cinetrack.soporte.textoEditable
import com.cinesstar.cinetrack.ui.components.TagsBarra
import com.cinesstar.cinetrack.ui.components.TagsDialogo
import com.cinesstar.cinetrack.ui.components.tagPantalla
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

/** Tests UI del S2 sobre el formulario de alta (CU-01, CU-03 en el alta, CU-10). Robolectric, sin emulador. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class FormularioPerfilTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val form = RobotFormulario(compose)

    private fun nodo(tag: String) = form.nodo(tag)

    private fun unico(): PerfilEntity = perfilesGuardados().single()

    private fun crearEnBd(
        nombre: String,
        tipo: TipoPerfil = TipoPerfil.PELICULA,
    ) = enBd { perfiles.crear(NuevoPerfil(nombre, tipo)) }

    private fun errorNombre(): String? =
        if (compose.existe(TagsFormulario.NOMBRE_ERROR)) {
            compose
                .onNodeWithTag(TagsFormulario.NOMBRE_ERROR, useUnmergedTree = true)
                .fetchSemanticsNode()
                .let { n -> textos(n) }
        } else {
            null
        }

    private fun textos(n: SemanticsNode): String =
        n.config
            .getOrNull(SemanticsProperties.Text)
            .orEmpty()
            .joinToString("") { it.text } +
            n.children.joinToString("") { textos(it) }

    private fun esperarErrorNombre(texto: String) = compose.esperar { errorNombre() == texto }

    private fun guardarDeshabilitado() = nodo(TagsFormulario.GUARDAR).assertIsNotEnabled()

    @Test
    fun t2_01_soloNombreSeGuardaComoPeliculaPendienteSinPuntuacion() {
        form.abrir()
        form.nombre("Blade Runner 2049")
        form.guardar()
        compose.onNodeWithTag(DestinoBarra.BIBLIOTECA.tagPantalla()).assertIsDisplayed()
        val p = unico()
        assertEquals("Blade Runner 2049", p.nombre)
        assertEquals(TipoPerfil.PELICULA, p.tipo)
        assertEquals(Estado.PENDIENTE, p.estado)
        assertNull(p.puntuacionDecimas)
        assertNull(p.sagaId)
        assertNull(p.comentario)
        val detalle = enBd { perfiles.obtener(p.id)!! }
        assertTrue(detalle.generos.isEmpty() && detalle.idiomas.isEmpty())
    }

    @Test
    fun t2_02_guardarDeshabilitadoConNombreVacio() {
        form.abrir()
        nodo(TagsFormulario.tipo(TipoPerfil.PELICULA)).assertIsSelected()
        nodo(TagsFormulario.estado(Estado.PENDIENTE)).assertIsSelected()
        guardarDeshabilitado()
        form.nombre("Dune")
        form.esperarHabilitado()
        form.nombre("")
        compose.esperar { runCatching { guardarDeshabilitado() }.isSuccess }
    }

    @Test
    fun t2_03_nombreSoloConEspaciosSigueDeshabilitadoYMuestraM01() {
        form.abrir()
        form.nombre("    ")
        guardarDeshabilitado()
        esperarErrorNombre("El nombre es obligatorio.")
        // Intentar guardar desde el teclado: sigue M-01 y no se guarda nada.
        nodo(TagsFormulario.NOMBRE).performImeAction()
        compose.waitForIdle()
        esperarErrorNombre("El nombre es obligatorio.")
        assertTrue(compose.existe(DestinoBarra.ANADIR.tagPantalla()))
        assertTrue(perfilesGuardados().isEmpty())
    }

    @Test
    fun t2_04_elegirSerieYGuardar() {
        form.abrir()
        form.tipo(TipoPerfil.SERIE)
        nodo(TagsFormulario.tipo(TipoPerfil.SERIE)).assertIsSelected()
        form.nombre("Breaking Bad")
        form.guardar()
        assertEquals(TipoPerfil.SERIE, unico().tipo)
    }

    @Test
    fun t2_05_duplicadoMuestraM02YNoSeGuarda() {
        crearEnBd("Dune")
        form.abrir()
        form.nombre("Dune")
        esperarErrorNombre("Ya existe una película con ese nombre.")
        form.esperarTexto("Ya registrado")
        guardarDeshabilitado()
        assertEquals(1, perfilesGuardados().size)
    }

    @Test
    fun t2_06_mismoNombreConDistintoTipoSePuedeCrear() {
        crearEnBd("Dune")
        form.abrir()
        form.tipo(TipoPerfil.SERIE)
        form.nombre("Dune")
        form.esperarTexto("Disponible")
        form.guardar()
        assertEquals(setOf(TipoPerfil.PELICULA, TipoPerfil.SERIE), perfilesGuardados().map { it.tipo }.toSet())
    }

    @Test
    fun t2_07_duplicadoIgnorandoMayusculasYTildes() {
        crearEnBd("Dune")
        form.abrir()
        form.nombre("  DÚNE ")
        esperarErrorNombre("Ya existe una película con ese nombre.")
        guardarDeshabilitado()
    }

    @Test
    fun t2_08_cambiarElTipoReevaluaElDuplicado() {
        crearEnBd("Dune")
        form.abrir()
        form.nombre("Dune")
        esperarErrorNombre("Ya existe una película con ese nombre.")
        form.tipo(TipoPerfil.SERIE)
        compose.esperar { errorNombre() == null }
        form.esperarTexto("Disponible")
        form.esperarHabilitado()
    }

    @Test
    fun t2_09_cadaEstadoSeGuardaSinPedirProgreso() {
        Estado.entries.forEach { estado ->
            form.abrir()
            assertFalse(compose.existeTexto("Temporada", subcadena = true) || compose.existeTexto("Episodio", subcadena = true))
            form.nombre("Título ${estado.name}")
            form.estado(estado)
            nodo(TagsFormulario.estado(estado)).assertIsSelected()
            form.guardar()
        }
        val estados = perfilesGuardados().associate { it.nombre to it.estado }
        Estado.entries.forEach { assertEquals(it, estados["Título ${it.name}"]) }
    }

    @Test
    fun t2_10_puntuacionValidaEnLosTresEstados() {
        val casos = listOf(Triple(Estado.PENDIENTE, "8.5", 85), Triple(Estado.VIENDO, "7", 70), Triple(Estado.VISTO, "10", 100))
        casos.forEach { (estado, texto, _) ->
            form.abrir()
            form.nombre("Nota ${estado.name}")
            form.estado(estado)
            form.puntuacion(texto)
            form.esperarTexto("Puntuación válida.")
            form.guardar()
        }
        val notas = perfilesGuardados().associate { it.nombre to it.puntuacionDecimas }
        casos.forEach { (estado, _, decimas) -> assertEquals(decimas, notas["Nota ${estado.name}"]) }
    }

    @Test
    fun t2_11_puntuacionInvalidaMuestraM03YNoSeGuarda() {
        form.abrir()
        form.nombre("Inválida")
        form.esperarHabilitado()
        listOf("11", "-1", "7.25", "abc").forEach { texto ->
            form.puntuacion(texto)
            form.esperarTexto("Puntuación no válida. Introduce un valor entre 0.0 y 10 con un máximo de un decimal.")
            guardarDeshabilitado()
        }
        assertTrue(perfilesGuardados().isEmpty())
    }

    @Test
    fun t2_12_vaciarLaPuntuacionGuardaSinPuntuacion() {
        form.abrir()
        form.nombre("Sin nota")
        form.puntuacion("8")
        form.esperarTexto("Puntuación válida.")
        form.puntuacion("")
        form.guardar()
        assertNull(unico().puntuacionDecimas)
    }

    @Test
    fun t2_13_botonesMenosYMasConLimites() {
        form.abrir()
        val campo = nodo(TagsFormulario.PUNTUACION)
        form.tocar(TagsFormulario.MAS)
        assertEquals("5.1", campo.textoEditable())
        form.puntuacion("")
        form.tocar(TagsFormulario.MENOS)
        assertEquals("4.9", campo.textoEditable())
        form.tocar(TagsFormulario.MAS)
        assertEquals("5.0", campo.textoEditable())

        form.puntuacion("0.1")
        form.tocar(TagsFormulario.MENOS)
        assertEquals("0.0", campo.textoEditable())
        nodo(TagsFormulario.MENOS).assertIsNotEnabled()
        nodo(TagsFormulario.MAS).assertIsEnabled()

        form.puntuacion("9.9")
        form.tocar(TagsFormulario.MAS)
        assertEquals("10", campo.textoEditable())
        nodo(TagsFormulario.MAS).assertIsNotEnabled()
        nodo(TagsFormulario.MENOS).assertIsEnabled()
        nodo(TagsFormulario.NOTA_GRANDE).assertTextContains("10")
    }

    @Test
    fun t2_14_sliderYCampoSincronizados() {
        form.abrir()
        nodo(TagsFormulario.SLIDER).performSemanticsAction(SemanticsActions.SetProgress) { it(7.3f) }
        compose.waitForIdle()
        assertEquals("7.3", nodo(TagsFormulario.PUNTUACION).textoEditable())
        nodo(TagsFormulario.NOTA_GRANDE).assertTextContains("7.3")

        form.puntuacion("4,2")
        val progreso = nodo(TagsFormulario.SLIDER).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(4.2f, progreso.current, 0.001f)
        nodo(TagsFormulario.NOTA_GRANDE).assertTextContains("4.2")
    }

    @Test
    fun t2_15_variosGenerosConContador() {
        form.abrir()
        val contador = nodo(TagsFormulario.CONTADOR_GENEROS)
        contador.assertTextContains("0 seleccionados")
        form.chip("Drama")
        contador.assertTextContains("1 seleccionado")
        form.chip("Terror")
        form.chip("Comedia")
        form.chip("Terror")
        contador.assertTextContains("2 seleccionados")
        form.nombre("Con géneros")
        form.guardar()
        val generos =
            enBd {
                perfiles
                    .obtener(unico().id)!!
                    .generos
                    .map { it.nombre }
                    .toSet()
            }
        assertEquals(setOf("Drama", "Comedia"), generos)
    }

    @Test
    fun t2_16_variosIdiomasConContador() {
        form.abrir()
        form.chip("Español")
        form.chip("Japonés")
        form.chip("Chino")
        form.chip("Chino")
        nodo(TagsFormulario.CONTADOR_IDIOMAS).assertTextContains("2 seleccionados")
        form.nombre("Con idiomas")
        form.guardar()
        val idiomas =
            enBd {
                perfiles
                    .obtener(unico().id)!!
                    .idiomas
                    .map { it.nombre }
                    .toSet()
            }
        assertEquals(setOf("Español", "Japonés"), idiomas)
    }

    @Test
    fun t2_17_elegirUnaSagaExistente() {
        val sagaId = enBd { sagas.crear("Dune") }
        form.abrir()
        form.tocar(TagsFormulario.SAGA)
        compose.onNodeWithText("Dune").performClick()
        compose.waitForIdle()
        nodo(TagsFormulario.SAGA).assertTextContains("Dune")
        form.nombre("Dune: Parte Dos")
        form.guardar()
        assertEquals(sagaId, unico().sagaId)
    }

    @Test
    fun t2_18_crearSagaNuevaYReutilizarlaEnOtroPerfil() {
        form.abrir()
        form.tocar(TagsFormulario.CREAR_SAGA)
        form.escribir(TagsFormulario.SAGA_CAMPO, "Star Wars")
        form.tocar(TagsFormulario.SAGA_GUARDAR)
        nodo(TagsFormulario.SAGA).assertTextContains("Star Wars")
        assertTrue("Aún no se registra (CU-10 E4)", enBd { sagas.todas() }.isEmpty())
        form.nombre("Una nueva esperanza")
        form.guardar()

        form.abrir()
        form.tocar(TagsFormulario.SAGA)
        compose.onNodeWithText("Star Wars").performClick()
        compose.waitForIdle()
        form.nombre("El imperio contraataca")
        form.guardar()

        val sagas = enBd { sagas.todas() }
        assertEquals(listOf("Star Wars"), sagas.map { it.nombre })
        assertEquals(setOf(sagas.single().id), perfilesGuardados().map { it.sagaId }.toSet())
    }

    @Test
    fun t2_19_sagaVaciaYSagaDuplicada() {
        val existente = enBd { sagas.crear("Dune") }
        form.abrir()
        form.tocar(TagsFormulario.CREAR_SAGA)
        form.tocar(TagsFormulario.SAGA_GUARDAR)
        form.esperarTexto("El nombre de la saga es obligatorio.")
        form.escribir(TagsFormulario.SAGA_CAMPO, "  DÚNE ")
        form.tocar(TagsFormulario.SAGA_GUARDAR)
        form.esperarTexto("Ya existe una saga con ese nombre.")
        // S-13: se ofrece elegir la existente.
        form.tocar(TagsFormulario.SAGA_EXISTENTE)
        assertFalse(compose.existe(TagsFormulario.SAGA_CAMPO))
        nodo(TagsFormulario.SAGA).assertTextContains("Dune")
        form.nombre("Dune")
        form.guardar()
        assertEquals(existente, unico().sagaId)
        assertEquals(1, enBd { sagas.todas() }.size)
    }

    @Test
    fun t2_20_crearSagaYCancelarElFormularioNoLaRegistra() {
        form.abrir()
        form.tocar(TagsFormulario.CREAR_SAGA)
        form.escribir(TagsFormulario.SAGA_CAMPO, "Matrix")
        form.tocar(TagsFormulario.SAGA_GUARDAR)
        form.tocar(TagsFormulario.CANCELAR)
        compose.onNodeWithTag(TagsDialogo.CONFIRMAR).performClick()
        compose.esperar { compose.existe(DestinoBarra.BIBLIOTECA.tagPantalla()) }
        assertTrue(enBd { sagas.todas() }.isEmpty())
        assertTrue(perfilesGuardados().isEmpty())
    }

    @Test
    fun t2_21_comentarioConSaltosDeLineaYEmojisSeGuardaIntegro() {
        val texto = "Primera línea\nSegunda línea 🎬🍿\n\n«Fin» — 100%"
        form.abrir()
        form.nombre("Con crítica")
        form.comentario(texto)
        form.guardar()
        assertEquals(texto, unico().comentario)
    }

    @Test
    fun t2_22_cancelarConDatosPideConfirmarElDescarte() {
        // Sin datos: vuelve directamente.
        form.abrir()
        form.tocar(TagsFormulario.CANCELAR)
        compose.esperar { compose.existe(DestinoBarra.BIBLIOTECA.tagPantalla()) }
        assertFalse(compose.existe(TagsDialogo.DIALOGO))

        // Con datos: pide confirmación; "Seguir editando" no pierde nada.
        form.abrir()
        form.nombre("A medias")
        form.tocar(TagsFormulario.CANCELAR)
        compose.onNodeWithTag(TagsDialogo.DIALOGO).assertIsDisplayed()
        compose.onNodeWithTag(TagsDialogo.CANCELAR).performClick()
        compose.waitForIdle()
        assertEquals("A medias", nodo(TagsFormulario.NOMBRE).textoEditable())

        // Atrás del sistema y de la barra también piden confirmación; al confirmar no se guarda nada.
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag(TagsDialogo.DIALOGO).assertIsDisplayed()
        compose.onNodeWithTag(TagsDialogo.CANCELAR).performClick()
        compose.onNodeWithTag(TagsBarra.ATRAS).performClick()
        compose.onNodeWithTag(TagsDialogo.CONFIRMAR).performClick()
        compose.esperar { compose.existe(DestinoBarra.BIBLIOTECA.tagPantalla()) }
        assertTrue(perfilesGuardados().isEmpty())
    }

    @Test
    fun t2_23_doblePulsacionCreaUnSoloPerfil() {
        form.abrir()
        form.nombre("Rápido")
        form.esperarHabilitado()
        val guardar = nodo(TagsFormulario.GUARDAR)
        guardar.performClick()
        runCatching { guardar.performClick() }
        compose.esperar { compose.existe(DestinoBarra.BIBLIOTECA.tagPantalla()) }
        assertEquals(1, perfilesGuardados().size)
    }

    @Test
    fun t2_24_falloDeEscrituraMuestraM25SinDatosParciales() {
        // El fallo llega después de insertar el perfil, la saga y los géneros: todo debe deshacerse.
        val real = aplicacion.container
        val dao = real.database.perfilDao()
        val conFallo =
            object : PerfilDao by dao {
                override suspend fun insertarIdiomas(relaciones: List<PerfilIdiomaEntity>): Unit = throw IOException("Disco lleno")
            }
        aplicacion.reemplazarContenedor(AppContainer(real.database, perfiles = PerfilRepository(real.database, perfilDao = conFallo)))

        form.abrir()
        form.nombre("Fallido")
        form.chip("Drama")
        form.chip("Inglés")
        form.tocar(TagsFormulario.CREAR_SAGA)
        form.escribir(TagsFormulario.SAGA_CAMPO, "Saga fallida")
        form.tocar(TagsFormulario.SAGA_GUARDAR)
        form.esperarHabilitado()
        form.tocar(TagsFormulario.GUARDAR)
        compose.esperar { compose.existe(TagsFormulario.ERROR_ESCRITURA) }
        form.esperarTexto("Error inesperado. Tus datos no se han modificado.")
        assertTrue("Sigue en el formulario", compose.existe(DestinoBarra.ANADIR.tagPantalla()))

        val filas =
            enBd {
                val db = database.openHelper.readableDatabase
                listOf("perfil", "saga", "perfil_genero", "perfil_idioma").associateWith { tabla ->
                    db.query("SELECT COUNT(*) FROM $tabla").use {
                        it.moveToFirst()
                        it.getInt(0)
                    }
                }
            }
        assertEquals(mapOf("perfil" to 0, "saga" to 0, "perfil_genero" to 0, "perfil_idioma" to 0), filas)
    }

    @Test
    fun t2_28_crearConNombreYPuntuacionEnTresPasos() {
        // Desde el botón "Añadir": 1) nombre, 2) puntuación, 3) Guardar (RNF-09).
        form.abrir()
        form.nombre("Tres pasos")
        form.puntuacion("9")
        form.guardar()
        assertEquals(90, unico().puntuacionDecimas)
    }

    @Test
    fun t2_29_rotarYPasarASegundoPlanoConservaElFormulario() {
        form.abrir()
        form.tipo(TipoPerfil.SERIE)
        form.nombre("Rotación")
        form.estado(Estado.VIENDO)
        form.puntuacion("7.5")
        form.chip("Drama")
        form.comentario("A medias")

        fun comprobar() {
            compose.esperar { compose.existe(TagsFormulario.NOMBRE) }
            assertEquals("Rotación", nodo(TagsFormulario.NOMBRE).textoEditable())
            assertEquals("7.5", nodo(TagsFormulario.PUNTUACION).textoEditable())
            assertEquals("A medias", nodo(TagsFormulario.COMENTARIO).textoEditable())
            nodo(TagsFormulario.tipo(TipoPerfil.SERIE)).assertIsSelected()
            nodo(TagsFormulario.estado(Estado.VIENDO)).assertIsSelected()
            nodo(TagsFormulario.CONTADOR_GENEROS).assertTextContains("1 seleccionado")
        }

        RuntimeEnvironment.setQualifiers("+land")
        compose.activityRule.scenario.recreate()
        comprobar()

        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        comprobar()

        form.guardar()
        val p = unico()
        assertEquals(TipoPerfil.SERIE to Estado.VIENDO, p.tipo to p.estado)
        assertEquals(75, p.puntuacionDecimas)
    }

    @Test
    fun toastDeExitoAlGuardar() {
        form.abrir()
        form.nombre("Con aviso")
        form.esperarHabilitado()
        // Con el reloj parado el toast no desaparece antes de comprobarlo (dura 2,8 s).
        compose.mainClock.autoAdvance = false
        form.nodo(TagsFormulario.GUARDAR).performClick()
        compose.esperar {
            compose.mainClock.advanceTimeByFrame()
            compose.onAllNodes(hasText("¡Perfil guardado correctamente en CineTrack!")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.mainClock.autoAdvance = true
        compose.esperar { compose.existe(DestinoBarra.BIBLIOTECA.tagPantalla()) && !compose.existe(DestinoBarra.ANADIR.tagPantalla()) }
    }
}
