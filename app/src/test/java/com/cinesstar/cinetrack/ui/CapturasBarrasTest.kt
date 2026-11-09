package com.cinesstar.cinetrack.ui

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.soporte.Capturas
import com.cinesstar.cinetrack.soporte.Capturas.aBuffered
import com.cinesstar.cinetrack.soporte.Capturas.comparar
import com.cinesstar.cinetrack.soporte.Capturas.escalada
import com.cinesstar.cinetrack.soporte.Capturas.guardar
import com.cinesstar.cinetrack.soporte.Capturas.prototipo
import com.cinesstar.cinetrack.soporte.Capturas.recorte
import com.cinesstar.cinetrack.ui.components.tag
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * T0.08: capturas de las barras superior e inferior comparadas con `gesti_n_de_listas/screen.png` (pantalla Listas,
 * que tiene las dos barras sin contenido superpuesto en esas zonas).
 *
 * Método y tolerancia en [Capturas] ([Capturas.BARRAS]): 411 dp de ancho, celdas de 4×4 dp. La barra de estado falsa
 * del prototipo (28 dp, §3.1) se descarta.
 * Las imágenes generadas y las de referencia recortadas quedan en `app/build/capturas` para revisarlas.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class CapturasBarrasTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private companion object {
        const val ANCHO_DP = 411
        const val BARRA_ESTADO_FALSA_DP = 28
        const val BARRA_SUPERIOR_DP = 52
        const val BARRA_INFERIOR_DP = 64
        const val ELEVACION_ANADIR_DP = 20
    }

    @Test
    fun t0_08_barrasComoEnElPrototipo() {
        compose.onNodeWithTag(DestinoBarra.LISTAS.tag()).performClick()
        compose.waitForIdle()
        val captura = compose.onRoot().captureToImage().asAndroidBitmap()
        val app = aBuffered(captura).escalada(ANCHO_DP)
        val referencia = prototipo("gesti_n_de_listas").escalada(ANCHO_DP)

        val superiorApp = app.recorte(0, BARRA_SUPERIOR_DP)
        val superiorRef = referencia.recorte(BARRA_ESTADO_FALSA_DP, BARRA_SUPERIOR_DP)
        val alturaInferior = BARRA_INFERIOR_DP + ELEVACION_ANADIR_DP
        val inferiorApp = app.recorte(app.height - alturaInferior, alturaInferior)
        val inferiorRef = referencia.recorte(referencia.height - alturaInferior, alturaInferior)

        guardar("barra_superior_app", superiorApp)
        guardar("barra_superior_prototipo", superiorRef)
        guardar("barra_inferior_app", inferiorApp)
        guardar("barra_inferior_prototipo", inferiorRef)

        val superior = comparar(superiorApp, superiorRef)
        val inferior = comparar(inferiorApp, inferiorRef)
        println("T0.08 barra superior: $superior · barra inferior: $inferior")
        listOf("superior" to superior, "inferior" to inferior).forEach { (nombre, r) ->
            assertTrue("Barra $nombre: $r fuera de ${Capturas.BARRAS}", r.cumple(Capturas.BARRAS))
        }
    }
}
