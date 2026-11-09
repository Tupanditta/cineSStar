package com.cinesstar.cinetrack.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Reglas de los botones − / + y del slider (CU-03 A3, S-05), base de T2.13 y T2.14. */
class PasoPuntuacionTest {
    private val mas = PasoPuntuacion.PASO
    private val menos = -PasoPuntuacion.PASO

    @Test
    fun desdeVacioParteDeCincoComoEnElPrototipo() {
        assertEquals("5.1", PasoPuntuacion.aplicar("", mas))
        assertEquals("4.9", PasoPuntuacion.aplicar("", menos))
        assertEquals("5.1", PasoPuntuacion.aplicar("abc", mas), "Un valor inválido se sustituye partiendo de 5.0")
    }

    @Test
    fun cadaPulsacionCambiaUnaDecimaSinErroresDeComaFlotante() {
        var texto = "0.0"
        repeat(100) { texto = PasoPuntuacion.aplicar(texto, mas) }
        assertEquals("10", texto)
        repeat(100) { texto = PasoPuntuacion.aplicar(texto, menos) }
        assertEquals("0.0", texto)
        assertEquals("8.6", PasoPuntuacion.aplicar("8,5", mas))
        assertEquals("9.9", PasoPuntuacion.aplicar("10", menos))
    }

    @Test
    fun limitesEnCeroYDiez() {
        assertFalse(PasoPuntuacion.puedeBajar("0.0"))
        assertTrue(PasoPuntuacion.puedeSubir("0.0"))
        assertFalse(PasoPuntuacion.puedeSubir("10"))
        assertTrue(PasoPuntuacion.puedeBajar("10"))
        assertTrue(PasoPuntuacion.puedeBajar("") && PasoPuntuacion.puedeSubir(""))
        assertEquals("0.0", PasoPuntuacion.aplicar("0.0", menos))
        assertEquals("10", PasoPuntuacion.aplicar("10", mas))
    }

    @Test
    fun sliderYCampoEquivalen() {
        (0..100).forEach { decimas ->
            val texto = Puntuacion.of(decimas).formatear()
            assertEquals(texto, PasoPuntuacion.desdeSlider(PasoPuntuacion.valorSlider(texto)))
        }
        assertEquals("7.3", PasoPuntuacion.desdeSlider(7.2999f))
        assertEquals(0f, PasoPuntuacion.valorSlider(""))
    }
}
