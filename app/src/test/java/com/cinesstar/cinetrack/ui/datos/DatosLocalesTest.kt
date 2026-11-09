package com.cinesstar.cinetrack.ui.datos

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.AppContainer
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.backup.ArchivosCopia
import com.cinesstar.cinetrack.data.backup.ArchivosSaf
import com.cinesstar.cinetrack.domain.CopiaSeguridad
import com.cinesstar.cinetrack.domain.FormatoCopia
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.soporte.CopiasReferencia
import com.cinesstar.cinetrack.soporte.FixtureGrande
import com.cinesstar.cinetrack.soporte.FixturePequeno
import com.cinesstar.cinetrack.soporte.Instantanea
import com.cinesstar.cinetrack.soporte.aplicacion
import com.cinesstar.cinetrack.soporte.comoCopia
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.soporte.existeTexto
import com.cinesstar.cinetrack.ui.buscar.TagsBuscar
import com.cinesstar.cinetrack.ui.components.TagsHoja
import com.cinesstar.cinetrack.ui.components.tag
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.time.LocalDate

/**
 * Tests UI del S8 sobre "Datos Locales" (pestaña de Buscar) y Ajustes (design.md §10, CU-21, CU-22). El selector de
 * archivos del sistema se simula respondiendo al `Intent` que lanza la app con un archivo `file://` temporal.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class DatosLocalesTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val carpeta: File by lazy { File(aplicacion.cacheDir, "copias").apply { mkdirs() } }

    private fun archivo(
        nombre: String,
        contenido: String? = null,
    ): File = File(carpeta, nombre).apply { contenido?.let { writeText(it) } }

    /** Abre Buscar → "Datos Locales". */
    private fun abrir() {
        compose.onNodeWithTag(DestinoBarra.BUSCAR.tag()).performClick()
        compose.onNodeWithTag(TagsBuscar.TAB_DATOS).performClick()
        compose.esperar { compose.existe(TagsDatos.BANNER) }
    }

    private fun ver(tag: String) {
        compose.onNodeWithTag(TagsBuscar.LISTA).performScrollToNode(hasTestTag(tag))
        compose.waitForIdle()
    }

    /** Pulsa por semántica: tras desplazar, el botón puede quedar bajo la barra inferior translúcida, que se llevaría el toque. */
    private fun tocar(tag: String) {
        ver(tag)
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    /** Responde al selector que acaba de abrir la app: con [archivo] elegido, o cancelado si es null. Devuelve su Intent. */
    private fun responder(archivo: File?): Intent {
        val sombra = shadowOf(compose.activity)
        val pedido = requireNotNull(sombra.nextStartedActivityForResult) { "La app no ha abierto el selector de archivos" }
        val resultado = archivo?.let { Intent().setData(Uri.fromFile(it)) }
        sombra.receiveResult(pedido.intent, if (archivo == null) Activity.RESULT_CANCELED else Activity.RESULT_OK, resultado)
        compose.waitForIdle()
        return pedido.intent
    }

    private fun esperarAviso(texto: String) {
        compose.esperar { compose.existe(TagsDatos.AVISO) }
        ver(TagsDatos.AVISO)
        compose.esperar { compose.existeTexto(texto) }
    }

    /** Elige [contenido] como archivo a importar y espera a M-19 (o al aviso si el archivo no es válido). */
    private fun elegirParaImportar(contenido: String) {
        tocar(TagsDatos.IMPORTAR)
        responder(archivo("elegida.json", contenido))
        compose.esperar { compose.existe(TagsHoja.HOJA) || compose.existe(TagsDatos.AVISO) }
    }

    private fun contenido() = enBd { Instantanea.filas(database) }

    @Test
    fun t8_01_exportarConDatosGuardaElArchivoYMuestraM15() {
        enBd { FixturePequeno.cargar(database) }
        abrir()
        tocar(TagsDatos.EXPORTAR)
        val destino = archivo("destino.json")
        val pedido = responder(destino)
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, pedido.action)
        assertEquals("cinetrack_backup_${LocalDate.now()}.json", pedido.getStringExtra(Intent.EXTRA_TITLE))
        assertEquals("application/json", pedido.type)

        esperarAviso("Copia guardada correctamente.")
        val copia = FormatoCopia.leer(destino.readText())
        assertEquals(FixturePequeno.perfiles.size, copia.perfiles.size)
        assertEquals(3, copia.listas.size)
        compose.onNodeWithTag(TagsDatos.ARCHIVO).assertTextEquals("cinetrack_backup_${LocalDate.now()}.json")
    }

    @Test
    fun t8_04_cancelarElSelectorAlExportarNoHaceNada() {
        abrir()
        tocar(TagsDatos.EXPORTAR)
        responder(null)
        assertFalse(compose.existe(TagsDatos.AVISO))
        assertFalse(compose.existe(TagsDatos.PROGRESO))
        assertTrue(carpeta.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun t8_05_destinoQueFallaAMitadDaM16YNoDejaArchivo() {
        val real = aplicacion.container
        val saf = ArchivosSaf(aplicacion.contentResolver)
        val sinEspacio =
            object : ArchivosCopia by saf {
                override suspend fun escribir(
                    uri: Uri,
                    contenido: ByteArray,
                ) {
                    saf.escribir(uri, contenido.copyOf(contenido.size / 2))
                    throw IOException("No queda espacio en el dispositivo")
                }
            }
        aplicacion.reemplazarContenedor(AppContainer(real.database, archivos = sinEspacio))
        enBd { FixturePequeno.cargar(database) }
        abrir()
        tocar(TagsDatos.EXPORTAR)
        val destino = archivo("destino.json")
        responder(destino)
        esperarAviso("No se pudo guardar la copia. Comprueba el espacio disponible y la ubicación elegida.")
        assertFalse("No queda un archivo corrupto a medias", destino.exists())
    }

    @Test
    fun t8_08_m19MuestraLosRecuentosYCoincidenConElResultado() {
        enBd { FixturePequeno.cargar(database) }
        abrir()
        elegirParaImportar(CopiasReferencia.texto(2))
        // Dune, Dune: Parte Dos, Dark y Cosmos ya existen; Top Histórico y Para el finde se amplían.
        compose.onNodeWithTag(TagsDatos.CONFIRMAR_TEXTO).assertTextEquals(
            "Se añadirán a tus datos: 1 perfiles nuevos, 0 listas nuevas y 2 listas ampliadas. 4 perfiles ya existían y se " +
                "omitirán. Tus datos actuales no se borran. ¿Continuar?",
        )
        compose.onNodeWithTag(TagsHoja.ACCION).performClick()
        esperarAviso("Importación completada: 1 perfiles nuevos, 4 omitidos, 0 listas nuevas, 2 listas ampliadas.")
        assertEquals(13, enBd { database.perfilDao().contar() })
    }

    @Test
    fun t8_09_cancelarEnElSelectorOEnM19NoCambiaNada() {
        enBd { FixturePequeno.cargar(database) }
        val antes = contenido()
        abrir()
        tocar(TagsDatos.IMPORTAR)
        responder(null)
        assertFalse(compose.existe(TagsHoja.HOJA))
        assertFalse(compose.existe(TagsDatos.AVISO))

        elegirParaImportar(CopiasReferencia.texto(2))
        assertTrue(compose.existe(TagsHoja.HOJA))
        compose.onNodeWithTag(TagsHoja.CANCELAR).performClick()
        compose.waitForIdle()
        assertFalse(compose.existe(TagsHoja.HOJA))
        assertFalse(compose.existe(TagsDatos.AVISO))
        assertEquals(antes, contenido())
    }

    @Test
    fun t8_10_archivoQueNoEsJsonOEsDeOtraAppDaM17() {
        val antes = contenido()
        abrir()
        elegirParaImportar("Esto no es un JSON")
        esperarAviso("El archivo no es una copia de seguridad válida de CineTrack.")
        assertFalse(compose.existe(TagsHoja.HOJA))
        elegirParaImportar("{\"tareas\": [\"comprar pan\"], \"version\": 1}")
        esperarAviso("El archivo no es una copia de seguridad válida de CineTrack.")
        assertEquals(antes, contenido())
    }

    @Test
    fun t8_10b_contenidoInconsistenteDaM17ConDetalle() {
        abrir()
        elegirParaImportar(CopiasReferencia.texto(2).replace("\"puntuacion\": 8.5", "\"puntuacion\": 7.25"))
        esperarAviso("El archivo no es una copia de seguridad válida de CineTrack.")
        compose.onNodeWithTag(TagsDatos.AVISO_DETALLE).assertTextEquals("La puntuación de «Dune» no es válida.")
        assertEquals(0, enBd { database.perfilDao().contar() })
    }

    @Test
    fun t8_11_versionFuturaDaM18() {
        abrir()
        elegirParaImportar(CopiasReferencia.texto(2).replace("\"version\": 2", "\"version\": ${CopiaSeguridad.VERSION_ACTUAL + 1}"))
        esperarAviso("La copia es de una versión más reciente de la aplicación y no se puede importar.")
        assertEquals(0, enBd { database.perfilDao().contar() })
    }

    @Test
    fun t8_16_archivoGrandeMuestraProgresoSinBloquearLaInterfaz() {
        val real = aplicacion.container
        val saf = ArchivosSaf(aplicacion.contentResolver)
        val puerta = CompletableDeferred<Unit>()
        val lento =
            object : ArchivosCopia by saf {
                override suspend fun leer(uri: Uri): ByteArray {
                    puerta.await()
                    return saf.leer(uri)
                }
            }
        aplicacion.reemplazarContenedor(AppContainer(real.database, archivos = lento))
        abrir()
        tocar(TagsDatos.IMPORTAR)
        responder(archivo("grande.json", FormatoCopia.escribir(FixtureGrande().comoCopia())))

        // Mientras se lee: indicador de progreso y botones deshabilitados.
        compose.esperar { compose.existe(TagsDatos.PROGRESO) }
        compose.onNodeWithTag(TagsDatos.PROGRESO).assertIsDisplayed()
        ver(TagsDatos.IMPORTAR)
        compose.onNodeWithTag(TagsDatos.IMPORTAR).assertIsNotEnabled()
        // La interfaz responde: se puede cambiar de pestaña y volver.
        compose.onNodeWithTag(TagsBuscar.LISTA).performScrollToNode(hasTestTag(TagsBuscar.TAB_FILTROS))
        compose.onNodeWithTag(TagsBuscar.TAB_FILTROS).performClick()
        compose.esperar { compose.existe(TagsBuscar.NOMBRE) }
        compose.onNodeWithTag(TagsBuscar.TAB_DATOS).performClick()
        compose.esperar { compose.existe(TagsDatos.PROGRESO) }

        puerta.complete(Unit)
        compose.esperar { compose.existe(TagsHoja.HOJA) }
        compose.onNode(hasText("1000 perfiles nuevos", substring = true)).assertIsDisplayed()
        compose.onNodeWithTag(TagsHoja.ACCION).performClick()
        compose.waitUntil(IMPORTACION_GRANDE_MS) { compose.existe(TagsDatos.AVISO) }
        esperarAviso("Importación completada: 1000 perfiles nuevos, 0 omitidos, 30 listas nuevas, 0 listas ampliadas.")
        assertEquals(FixtureGrande.PERFILES, enBd { database.perfilDao().contar() })
    }

    @Test
    fun t8_18_metricasYDistribucionCoincidenConLosDatos() {
        enBd { FixturePequeno.cargar(database) }
        compose.onNodeWithTag(DestinoBarra.AJUSTES.tag()).performClick()
        compose.esperar { compose.existe(TagsDatos.TITULOS) && compose.existeTexto("12") }
        compose.onNodeWithTag(TagsDatos.TITULOS).assertTextEquals("12")
        compose.onNodeWithTag(TagsDatos.LISTAS).assertTextEquals("3")
        compose.onNodeWithTag(TagsDatos.PELICULAS).assertTextEquals("Películas (6 entradas)")
        compose.onNodeWithTag(TagsDatos.SERIES).assertTextEquals("Series (6 entradas)")
        compose.onNodeWithTag(TagsDatos.PORCENTAJE_PELICULAS).assertTextEquals("50%")
        compose.onNodeWithTag(TagsDatos.PORCENTAJE_SERIES).assertTextEquals("50%")
        val almacenamiento = compose.onNodeWithTag(TagsDatos.ALMACENAMIENTO).fetchSemanticsNode().config
        val texto = almacenamiento[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString { it.text }
        assertTrue("Almacenamiento: $texto", texto.matches(Regex("""\d+ (B|KB)|\d+\.\d MB""")))
        compose.esperar { compose.existe(TagsDatos.TAMANO) }

        // Tras borrar un perfil, las métricas se actualizan.
        enBd {
            perfiles.eliminar(
                database
                    .copiaDao()
                    .perfiles()
                    .first { it.tipo == TipoPerfil.SERIE }
                    .id,
            )
        }
        compose.esperar { compose.existeTexto("Series (5 entradas)") }
        compose.onNodeWithTag(TagsDatos.TITULOS).assertTextEquals("11")
        compose.onNodeWithTag(TagsDatos.PORCENTAJE_PELICULAS).assertTextEquals("55%")
        compose.onNodeWithTag(TagsDatos.PORCENTAJE_SERIES).assertTextEquals("45%")
    }

    private companion object {
        const val IMPORTACION_GRANDE_MS = 60_000L
    }
}
