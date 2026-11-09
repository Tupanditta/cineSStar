package com.cinesstar.cinetrack.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Campo "Posición" al añadir a un ranking (CU-15 A1/E3), base de T6.01–T6.04. */
class PosicionAltaTest {
    @Test
    fun vacioEsAlFinal() {
        assertEquals(6, PosicionAlta.destino("", 5))
        assertEquals(6, PosicionAlta.destino("  ", 5))
        assertEquals(1, PosicionAlta.destino("", 0))
    }

    @Test
    fun deUnoANMasUnoSonValidas() {
        assertEquals(1, PosicionAlta.destino("1", 5))
        assertEquals(2, PosicionAlta.destino("2", 5))
        assertEquals(6, PosicionAlta.destino("6", 5))
        assertEquals(3, PosicionAlta.destino(" 3 ", 5))
    }

    @Test
    fun ceroMasAllaDeNMasUnoYTextoNoSonValidas() {
        listOf("0", "7", "-1", "abc", "2a", "1.5", "+2", "99999999999999").forEach {
            assertNull(PosicionAlta.destino(it, 5), "\"$it\" no es una posición válida")
        }
    }

    @Test
    fun losBotonesNoSalenDeUnoANMasUno() {
        assertEquals("5", PosicionAlta.paso("", -1, 5))
        assertEquals("6", PosicionAlta.paso("", 1, 5))
        assertEquals("1", PosicionAlta.paso("1", -1, 5))
        assertEquals("3", PosicionAlta.paso("2", 1, 5))
        assertEquals("5", PosicionAlta.paso("abc", -1, 5))
    }
}
