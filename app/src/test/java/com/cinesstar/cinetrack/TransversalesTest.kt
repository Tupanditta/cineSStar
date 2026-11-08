package com.cinesstar.cinetrack

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.soporte.Proyecto
import com.cinesstar.cinetrack.ui.components.tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Pruebas transversales de sprints.md §1.5 y regla 4 del DoD. Se ejecutan en todos los sprints. */
@RunWith(AndroidJUnit4::class)
class TransversalesTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun permisosPedidos(): List<String> {
        val pm = compose.activity.packageManager
        val info = pm.getPackageInfo(compose.activity.packageName, PackageManager.GET_PERMISSIONS)
        return info.requestedPermissions?.toList().orEmpty()
    }

    @Test
    fun tx_01_sinRed() {
        val red = setOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE)
        val pedidos = permisosPedidos()
        assertTrue("El manifiesto combinado (con las dependencias) no pide red: $pedidos", pedidos.none { it in red })
    }

    @Test
    fun tx_03_sinPermisosPeligrosos() {
        val pm = compose.activity.packageManager
        val peligrosos =
            permisosPedidos().filter { permiso ->
                runCatching { pm.getPermissionInfo(permiso, 0).protection == android.content.pm.PermissionInfo.PROTECTION_DANGEROUS }
                    .getOrDefault(false)
            }
        assertEquals(emptyList<String>(), peligrosos)
        val almacenamiento = permisosPedidos().filter { "STORAGE" in it || "READ_MEDIA" in it }
        assertEquals(
            "Sin permisos de almacenamiento; los archivos se eligen con el selector del sistema",
            emptyList<String>(),
            almacenamiento,
        )
    }

    /** Textos de la interfaz: todos los valores de `res/values/strings.xml`. */
    private fun textosDeRecursos(): Set<String> {
        val xml = File(Proyecto.recursosMain, "values/strings.xml").readText()
        return Regex("<string name=\"[^\"]+\">(.*?)</string>").findAll(xml).map { it.groupValues[1] }.toSet()
    }

    private fun textosVisibles(nodo: SemanticsNode): List<String> {
        val propios =
            nodo.config
                .getOrNull(SemanticsProperties.Text)
                .orEmpty()
                .map { it.text } +
                nodo.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        return propios + nodo.children.flatMap { textosVisibles(it) }
    }

    @Test
    fun tx_02_todosLosTextosVisiblesEstanEnStringsXml() {
        val recursos = textosDeRecursos()
        val vistos = mutableSetOf<String>()

        fun recoger() {
            compose.waitForIdle()
            vistos += textosVisibles(compose.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        }
        recoger()
        DestinoBarra.entries.filter { it.esPestana }.forEach {
            compose.onNodeWithTag(it.tag()).performClick()
            recoger()
        }
        compose.onNodeWithTag(DestinoBarra.ANADIR.tag()).performClick()
        recoger()
        assertTrue("No se ha recogido ningún texto", vistos.isNotEmpty())
        val fuera = vistos - recursos
        assertEquals("Textos visibles que no salen de strings.xml", emptySet<String>(), fuera)
    }

    @Test
    fun tx_02_soloEspanolYSinLiteralesEnLaInterfaz() {
        val carpetasValues =
            Proyecto.recursosMain
                .listFiles()!!
                .filter { it.name.startsWith("values") }
                .map { it.name }
        assertEquals("Sin traducciones: la interfaz solo está en español", listOf("values"), carpetasValues)

        val literales = Regex("""(Text\(\s*(text\s*=\s*)?"|contentDescription\s*=\s*"|label\s*=\s*")""")
        val ofensas =
            Proyecto.fuentesMain
                .walkTopDown()
                .filter { it.extension == "kt" }
                .flatMap { archivo ->
                    archivo.readLines().mapIndexedNotNull {
                        i,
                        linea,
                        ->
                        if (literales.containsMatchIn(linea)) "${archivo.name}:${i + 1}" else null
                    }
                }.toList()
        assertEquals("Textos de interfaz escritos en el código en vez de en strings.xml", emptyList<String>(), ofensas)
    }

    @Test
    fun dod_4_coloresYMedidasSoloEnUiTheme() {
        val tema = File(Proyecto.fuentesMain, "com/cinesstar/cinetrack/ui/theme").canonicalFile
        val prohibido = Regex("""Color\(0x|\b\d+(\.\d+)?\.(dp|sp)\b|Color\.(Red|Blue|Green|White|Black|Gray|Yellow|Cyan|Magenta)""")
        val ofensas =
            Proyecto.fuentesMain
                .walkTopDown()
                .filter { it.extension == "kt" && !it.canonicalPath.startsWith(tema.path) }
                .flatMap { archivo ->
                    archivo.readLines().mapIndexedNotNull {
                        i,
                        linea,
                        ->
                        if (prohibido.containsMatchIn(linea)) "${archivo.name}:${i + 1}: ${linea.trim()}" else null
                    }
                }.toList()
        assertEquals("Colores o medidas fijas fuera de ui/theme", emptyList<String>(), ofensas)
    }
}
