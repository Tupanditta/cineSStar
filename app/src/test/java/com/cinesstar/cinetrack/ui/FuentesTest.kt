package com.cinesstar.cinetrack.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.soporte.Proyecto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * T0.09: Inter y Material Symbols se cargan desde los recursos de la app, sin red. El manifiesto no tiene INTERNET
 * (TX.01), así que cualquier fuente descargable fallaría: aquí se comprueba que no hay ninguna.
 */
@RunWith(AndroidJUnit4::class)
class FuentesTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun t0_09_lasFuentesSonRecursosLocales() {
        val inter = ResourcesCompat.getFont(context, R.font.inter)
        val symbols = ResourcesCompat.getFont(context, R.font.material_symbols_outlined)
        assertNotNull(inter)
        assertNotNull(symbols)

        val fuentes = File(Proyecto.recursosMain, "font").listFiles()!!.map { it.name }.sorted()
        assertEquals(
            "Solo archivos de fuente empaquetados, sin descriptores XML de fuentes descargables",
            listOf("inter.ttf", "material_symbols_outlined.ttf"),
            fuentes,
        )

        val fuentesKotlin =
            Proyecto.fuentesMain
                .walkTopDown()
                .filter { it.extension == "kt" }
                .map { it.readText() }
                .toList()
        assertFalse("No se usan Google Fonts descargables", fuentesKotlin.any { "GoogleFont" in it || "FontProvider" in it })
    }

    @Test
    fun t0_09_lasLigadurasDeSymbolsDibujanUnSoloIcono() {
        val symbols = ResourcesCompat.getFont(context, R.font.material_symbols_outlined)!!
        val tamano = 48f
        val conSymbols =
            Paint().apply {
                typeface = symbols
                textSize = tamano
            }
        val sinSymbols =
            Paint().apply {
                typeface = Typeface.DEFAULT
                textSize = tamano
            }
        listOf("video_library", "bookmarks", "add", "search", "database", "person", "arrow_back").forEach { nombre ->
            val ancho = conSymbols.measureText(nombre)
            // Con ligadura, el nombre se convierte en un único glifo cuadrado del tamaño de la fuente.
            assertTrue("\"$nombre\" mide $ancho px: no se ha formado la ligadura", ancho <= tamano * 1.05f)
            assertTrue(sinSymbols.measureText(nombre) > ancho)
        }
    }
}
