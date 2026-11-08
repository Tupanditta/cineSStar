package com.cinesstar.cinetrack.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.soporte.Proyecto
import com.cinesstar.cinetrack.ui.components.tag
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * T0.08: capturas de las barras superior e inferior comparadas con `gesti_n_de_listas/screen.png` (pantalla Listas,
 * que tiene las dos barras sin contenido superpuesto en esas zonas).
 *
 * Método y tolerancia:
 * - Las dos imágenes se llevan a la misma escala (1 px = 1 dp, 411 dp de ancho) y se dividen en celdas de 4×4 dp.
 * - Se compara el color medio de cada celda; la diferencia es la media de |ΔR|+|ΔG|+|ΔB| / 3 en 0..255.
 * - Pasa si la diferencia media es ≤ [TOLERANCIA_MEDIA] y no más del [MAX_CELDAS_DISTINTAS] de las celdas difiere
 *   más de [UMBRAL_CELDA]. La captura del prototipo está reescalada y tiene otro motor de texto: las diferencias de
 *   suavizado del texto son normales; un color, tamaño o posición equivocados no.
 * - La barra de estado falsa del prototipo (28 dp, §3.1) se descarta.
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
        const val CELDA_DP = 4
        const val TOLERANCIA_MEDIA = 12.0
        const val UMBRAL_CELDA = 40.0
        const val MAX_CELDAS_DISTINTAS = 0.10
    }

    private data class Resultado(
        val media: Double,
        val fraccionDistintas: Double,
    )

    @Test
    fun t0_08_barrasComoEnElPrototipo() {
        compose.onNodeWithTag(DestinoBarra.LISTAS.tag()).performClick()
        compose.waitForIdle()
        val captura = compose.onRoot().captureToImage().asAndroidBitmap()
        val app = aBuffered(captura).escalada(ANCHO_DP)
        val referencia = ImageIO.read(Proyecto.captura("gesti_n_de_listas")).escalada(ANCHO_DP)

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
            assertTrue("Barra $nombre: diferencia media ${r.media} > $TOLERANCIA_MEDIA", r.media <= TOLERANCIA_MEDIA)
            assertTrue(
                "Barra $nombre: ${r.fraccionDistintas} de celdas distintas > $MAX_CELDAS_DISTINTAS",
                r.fraccionDistintas <= MAX_CELDAS_DISTINTAS,
            )
        }
    }

    private fun aBuffered(bitmap: Bitmap): BufferedImage {
        val pixeles = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixeles, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return BufferedImage(bitmap.width, bitmap.height, BufferedImage.TYPE_INT_RGB).apply {
            setRGB(0, 0, bitmap.width, bitmap.height, pixeles, 0, bitmap.width)
        }
    }

    private fun BufferedImage.escalada(ancho: Int): BufferedImage {
        val alto = (height * ancho.toDouble() / width).roundToInt()
        val destino = BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB)
        val g = destino.createGraphics()
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(getScaledInstance(ancho, alto, java.awt.Image.SCALE_AREA_AVERAGING), 0, 0, null)
        g.dispose()
        return destino
    }

    private fun BufferedImage.recorte(
        y: Int,
        alto: Int,
    ): BufferedImage = getSubimage(0, y, width, alto)

    private fun comparar(
        a: BufferedImage,
        b: BufferedImage,
    ): Resultado {
        val columnas = a.width / CELDA_DP
        val filas = a.height / CELDA_DP
        val diferencias =
            (0 until filas).flatMap { f ->
                (0 until columnas).map { c ->
                    val ma = a.mediaCelda(c * CELDA_DP, f * CELDA_DP)
                    val mb = b.mediaCelda(c * CELDA_DP, f * CELDA_DP)
                    (abs(ma[0] - mb[0]) + abs(ma[1] - mb[1]) + abs(ma[2] - mb[2])) / 3
                }
            }
        return Resultado(diferencias.average(), diferencias.count { it > UMBRAL_CELDA }.toDouble() / diferencias.size)
    }

    private fun BufferedImage.mediaCelda(
        x0: Int,
        y0: Int,
    ): DoubleArray {
        val suma = DoubleArray(3)
        var n = 0
        for (y in y0 until y0 + CELDA_DP) {
            for (x in x0 until x0 + CELDA_DP) {
                val rgb = getRGB(x, y)
                suma[0] += (rgb shr 16 and 0xFF).toDouble()
                suma[1] += (rgb shr 8 and 0xFF).toDouble()
                suma[2] += (rgb and 0xFF).toDouble()
                n++
            }
        }
        return DoubleArray(3) { suma[it] / n }
    }

    private fun guardar(
        nombre: String,
        imagen: BufferedImage,
    ) {
        ImageIO.write(imagen, "png", File(Proyecto.capturas, "$nombre.png"))
    }
}
