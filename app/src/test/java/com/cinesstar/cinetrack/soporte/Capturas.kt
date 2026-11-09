package com.cinesstar.cinetrack.soporte

import android.graphics.Bitmap
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Comparación de capturas con los `screen.png` del prototipo (tests G).
 *
 * Método: las dos imágenes se llevan a la misma escala (1 px = 1 dp) y se dividen en celdas de [CELDA_DP]×[CELDA_DP];
 * se compara el color medio de cada celda (media de |ΔR|+|ΔG|+|ΔB| / 3, en 0..255). Una comparación pasa si la
 * diferencia media no supera [Tolerancia.media] y la fracción de celdas que difieren más de [UMBRAL_CELDA]
 * no supera [Tolerancia.maxCeldasDistintas]. El texto del prototipo tiene otro motor y suavizado: esas diferencias son
 * normales; un color, tamaño o posición equivocados no.
 */
object Capturas {
    const val CELDA_DP = 4

    /** Diferencia a partir de la cual una celda cuenta como distinta. */
    const val UMBRAL_CELDA = 40.0

    data class Tolerancia(
        val media: Double,
        val maxCeldasDistintas: Double,
    )

    /** Tolerancia de las barras (T0.08): zonas pequeñas y sin datos. */
    val BARRAS = Tolerancia(media = 12.0, maxCeldasDistintas = 0.10)

    data class Resultado(
        val media: Double,
        val fraccionDistintas: Double,
    ) {
        fun cumple(t: Tolerancia): Boolean = media <= t.media && fraccionDistintas <= t.maxCeldasDistintas
    }

    fun aBuffered(bitmap: Bitmap): BufferedImage {
        val pixeles = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixeles, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return BufferedImage(bitmap.width, bitmap.height, BufferedImage.TYPE_INT_RGB).apply {
            setRGB(0, 0, bitmap.width, bitmap.height, pixeles, 0, bitmap.width)
        }
    }

    fun prototipo(pantalla: String): BufferedImage = ImageIO.read(Proyecto.captura(pantalla))

    fun BufferedImage.escalada(ancho: Int): BufferedImage {
        val alto = (height * ancho.toDouble() / width).roundToInt()
        val destino = BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB)
        val g = destino.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(getScaledInstance(ancho, alto, Image.SCALE_AREA_AVERAGING), 0, 0, null)
        g.dispose()
        return destino
    }

    fun BufferedImage.recorte(
        y: Int,
        alto: Int,
    ): BufferedImage = getSubimage(0, y, width, alto.coerceAtMost(height - y))

    /**
     * Compara [a] con [b] celda a celda en la zona común. Las filas de [excluirFilasDp] (rangos verticales en dp) no
     * cuentan: son zonas que el prototipo rellena con fotos o datos que no existen en la app (design.md §12).
     */
    fun comparar(
        a: BufferedImage,
        b: BufferedImage,
        excluirFilasDp: List<IntRange> = emptyList(),
    ): Resultado {
        val columnas = minOf(a.width, b.width) / CELDA_DP
        val filas = minOf(a.height, b.height) / CELDA_DP
        val diferencias =
            (0 until filas)
                .filter { f -> excluirFilasDp.none { (f * CELDA_DP) in it } }
                .flatMap { f ->
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

    fun guardar(
        nombre: String,
        imagen: BufferedImage,
    ) {
        ImageIO.write(imagen, "png", File(Proyecto.capturas, "$nombre.png"))
    }
}
