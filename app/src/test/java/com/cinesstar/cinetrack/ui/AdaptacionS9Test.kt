package com.cinesstar.cinetrack.ui

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.soporte.Capturas
import com.cinesstar.cinetrack.soporte.FixturePequeno
import com.cinesstar.cinetrack.soporte.Revision
import com.cinesstar.cinetrack.soporte.enBd
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests UI del S9 (RNF-11, design.md §11): todas las pantallas con datos en varios anchos (T9.07), con la fuente del
 * sistema al 130 % y al 200 % (T9.08) y con las comprobaciones automáticas de accesibilidad (T9.10). El recorrido y las
 * comprobaciones están en [Revision].
 */
@RunWith(AndroidJUnit4::class)
class AdaptacionS9Test {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** Revisa todas las pantallas; con [capturas] guarda además cada una en `app/build/capturas` para revisarla a mano. */
    private fun revisar(
        capturas: String? = null,
        comprobar: Revision.(String) -> Unit,
    ): List<String> {
        val datos = enBd { FixturePequeno.cargar(database) }
        val revision = Revision(compose, compose.activity.resources.displayMetrics.density)
        revision.recorrer(
            datos,
            comprobar = { pantalla ->
                revision.comprobar(pantalla)
                capturas?.let {
                    Capturas.guardar(
                        "${it}_$pantalla",
                        Capturas.aBuffered(compose.onRoot().captureToImage().asAndroidBitmap()),
                    )
                }
            },
            atras = {
                compose.activity.onBackPressedDispatcher.onBackPressed()
                compose.waitForIdle()
            },
        )
        return revision.problemas
    }

    private fun sinProblemas(
        capturas: String? = null,
        comprobar: Revision.(String) -> Unit,
    ) = assertEquals(emptyList<String>(), revisar(capturas, comprobar))

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun t9_07_ancho320dp() = sinProblemas("ancho320") { sinDesbordes(it) }

    @Test
    @Config(qualifiers = "w360dp-h740dp")
    fun t9_07_ancho360dp() = sinProblemas { sinDesbordes(it) }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun t9_07_ancho411dp() = sinProblemas { sinDesbordes(it) }

    @Test
    @Config(qualifiers = "w600dp-h960dp")
    fun t9_07_ancho600dp() = sinProblemas { sinDesbordes(it) }

    @Test
    @Config(qualifiers = "w360dp-h740dp", fontScale = 1.3f)
    fun t9_08_fuenteAl130() =
        sinProblemas("fuente130") {
            sinDesbordes(it)
            botonesEnteros(it)
        }

    @Test
    @Config(qualifiers = "w360dp-h740dp", fontScale = 2.0f)
    fun t9_08_fuenteAl200() =
        sinProblemas("fuente200") {
            sinDesbordes(it)
            botonesEnteros(it)
        }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun t9_10_todoLoPulsableTieneNombreYZonaTactilDe48dp() = sinProblemas { accesible(it) }
}
