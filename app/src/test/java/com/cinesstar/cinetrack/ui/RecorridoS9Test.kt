package com.cinesstar.cinetrack.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.room.PrecargaCatalogo
import com.cinesstar.cinetrack.domain.ClaveGrupo
import com.cinesstar.cinetrack.domain.CriterioAgrupacion
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.soporte.Instantanea
import com.cinesstar.cinetrack.soporte.RobotDetalle
import com.cinesstar.cinetrack.soporte.RobotFormulario
import com.cinesstar.cinetrack.soporte.RobotListas
import com.cinesstar.cinetrack.soporte.aplicacion
import com.cinesstar.cinetrack.soporte.elementosDe
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.soporte.existeTexto
import com.cinesstar.cinetrack.soporte.perfilesGuardados
import com.cinesstar.cinetrack.ui.biblioteca.TagsBiblioteca
import com.cinesstar.cinetrack.ui.buscar.TagsBuscar
import com.cinesstar.cinetrack.ui.components.TagsHoja
import com.cinesstar.cinetrack.ui.components.tag
import com.cinesstar.cinetrack.ui.datos.TagsDatos
import com.cinesstar.cinetrack.ui.lista.TagsLista
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * T9.13 (E): recorrido completo por la interfaz con la base de datos real de la app: crear 3 perfiles → puntuar →
 * comentar → crear una lista de cada tipo → añadir → reordenar → buscar → agrupar → exportar → vaciar → importar →
 * comprobar que los datos son idénticos. "Vaciar" borra todas las tablas y vuelve a precargar el catálogo, como una
 * instalación nueva.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class RecorridoS9Test {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val formulario by lazy { RobotFormulario(compose) }
    private val detalle by lazy { RobotDetalle(compose) }
    private val listas by lazy { RobotListas(compose) }

    private fun ir(destino: DestinoBarra) {
        compose.onNodeWithTag(destino.tag()).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    private fun atras() {
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
    }

    private fun tocarEnBuscar(tag: String) {
        compose.onNodeWithTag(TagsBuscar.LISTA).performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    /** Responde al selector de archivos del sistema con [archivo]. */
    private fun elegir(archivo: File) {
        val sombra = shadowOf(compose.activity)
        val pedido = requireNotNull(sombra.nextStartedActivityForResult) { "No se ha abierto el selector de archivos" }
        sombra.receiveResult(pedido.intent, Activity.RESULT_OK, Intent().setData(Uri.fromFile(archivo)))
        compose.waitForIdle()
    }

    private fun crearPerfil(
        nombre: String,
        tipo: TipoPerfil,
        genero: String,
    ): Long {
        formulario.abrir()
        formulario.nombre(nombre)
        formulario.tipo(tipo)
        formulario.chip(genero)
        formulario.guardar()
        return perfilesGuardados().first { it.nombre == nombre }.id
    }

    @Test
    fun t9_13_recorridoCompletoSinErroresYConDatosIdenticos() {
        val (dune, amelie, dark) = crearPuntuarYComentar()
        crearListas(dune, amelie, dark)
        buscarYAgrupar(dune)
        val antes = enBd { Instantanea.contenido(database) }
        val copia = exportar()
        vaciar()
        importar(copia)
        // Los mismos datos que antes de exportar.
        assertEquals(antes, enBd { Instantanea.contenido(database) })
        ir(DestinoBarra.BIBLIOTECA)
        compose.esperar { compose.existeTexto("Amélie") }
        assertTrue(compose.existeTexto("Dark"))
    }

    /** 1. Tres perfiles; uno puntuado y comentado. */
    private fun crearPuntuarYComentar(): Triple<Long, Long, Long> {
        val dune = crearPerfil("Dune", TipoPerfil.PELICULA, "Ciencia Ficción")
        val amelie = crearPerfil("Amélie", TipoPerfil.PELICULA, "Comedia")
        val dark = crearPerfil("Dark", TipoPerfil.SERIE, "Suspense")
        ir(DestinoBarra.BIBLIOTECA)
        detalle.abrir(dune)
        detalle.ajustar()
        detalle.escribirNota("8,5")
        detalle.guardarNota()
        detalle.abrirCritica()
        detalle.escribirCritica("Arena y especias.\nPara volver a verla.")
        detalle.guardarCritica()
        atras()
        assertEquals(85, enBd { database.perfilDao().obtener(dune)!!.puntuacionDecimas })
        return Triple(dune, amelie, dark)
    }

    /** 2. Una lista de cada tipo, con elementos; el ranking se reordena con las flechas. */
    private fun crearListas(
        dune: Long,
        amelie: Long,
        dark: Long,
    ) {
        listas.abrir()
        val normal = listas.crear("Para el finde", TipoLista.NORMAL)
        val prioridad = listas.crear("Próximas", TipoLista.PRIORIDAD)
        val ranking = listas.crear("Top", TipoLista.RANKING)
        anadirEn(ranking, dune, amelie)
        listas.tocar(TagsLista.REORDENAR)
        listas.subir(amelie)
        listas.tocar(TagsLista.LISTO)
        compose.esperar { elementosDe(ranking) == listOf(amelie, dune) }
        atras()
        listas.abrirLista(prioridad)
        listas.abrirSelector()
        listas.prioridadAlta(Prioridad.ALTA)
        listas.anadir(prioridad, dark)
        listas.cerrarSelector()
        atras()
        anadirEn(normal, dune, dark)
        atras()
    }

    private fun anadirEn(
        lista: Long,
        vararg perfiles: Long,
    ) {
        listas.abrirLista(lista)
        listas.abrirSelector()
        perfiles.forEach { listas.anadir(lista, it) }
        listas.cerrarSelector()
    }

    /** 3. Buscar y agrupar por género. */
    private fun buscarYAgrupar(dune: Long) {
        ir(DestinoBarra.BUSCAR)
        compose.onNodeWithTag(TagsBuscar.NOMBRE).performTextReplacement("DUN")
        compose.esperar { compose.existeTexto("1 coincidencia") && compose.existe(TagsBuscar.resultado(dune)) }
        ir(DestinoBarra.BIBLIOTECA)
        val genero = TagsBiblioteca.criterio(CriterioAgrupacion.GENERO)
        compose.onNodeWithTag(TagsBiblioteca.LISTA).performScrollToNode(hasTestTag(genero))
        compose.onNodeWithTag(genero).performSemanticsAction(SemanticsActions.OnClick)
        val ficcion = enBd { catalogo.generos().first { it.nombre == "Ciencia Ficción" } }
        compose.esperar { compose.existe(TagsBiblioteca.grupo(ClaveGrupo.Genero(ficcion.id, ficcion.nombre, ficcion.orden))) }
    }

    /** 4. Exportar desde Ajustes; devuelve el archivo. */
    private fun exportar(): File {
        val copia = File(File(aplicacion.cacheDir, "recorrido").apply { mkdirs() }, "copia.json")
        ir(DestinoBarra.AJUSTES)
        compose.esperar { compose.existe(TagsDatos.LISTA_AJUSTES) }
        tocarEnAjustes(TagsDatos.EXPORTAR)
        elegir(copia)
        compose.esperar { compose.existeTexto("Copia guardada correctamente.") }
        return copia
    }

    /** 5a. Vaciar como una instalación nueva: sin datos y con el catálogo precargado. */
    private fun vaciar() {
        enBd {
            withContext(Dispatchers.IO) {
                database.clearAllTables()
                PrecargaCatalogo.onOpen(database.openHelper.writableDatabase)
            }
        }
        compose.esperar { titulosGuardados() == "0" }
    }

    private fun titulosGuardados(): String =
        compose
            .onNodeWithTag(TagsDatos.TITULOS)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            ?.joinToString { it.text }
            .orEmpty()

    /** 5b. Importar la copia confirmando M-19. */
    private fun importar(copia: File) {
        tocarEnAjustes(TagsDatos.IMPORTAR)
        elegir(copia)
        compose.esperar { compose.existe(TagsHoja.HOJA) }
        compose.onNodeWithTag(TagsHoja.ACCION).performSemanticsAction(SemanticsActions.OnClick)
        compose.esperar {
            compose.existeTexto("Importación completada: 3 perfiles nuevos, 0 omitidos, 3 listas nuevas, 0 listas ampliadas.")
        }
        assertEquals("3", titulosGuardados())
    }

    private fun tocarEnAjustes(tag: String) {
        compose.onNodeWithTag(TagsDatos.LISTA_AJUSTES).performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }
}
