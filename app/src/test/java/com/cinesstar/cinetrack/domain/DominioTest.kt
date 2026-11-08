package com.cinesstar.cinetrack.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class PuntuacionTest {
    companion object {
        /** Tabla de casos límite de sprints.md §1.4: entrada → décimas guardadas (null = sin puntuación). */
        @JvmStatic
        fun validas(): Stream<Arguments> =
            Stream.of(
                Arguments.of("", null),
                Arguments.of("0", 0),
                Arguments.of("0.0", 0),
                Arguments.of("0,0", 0),
                Arguments.of("7", 70),
                Arguments.of("7.0", 70),
                Arguments.of("8.5", 85),
                Arguments.of("8,5", 85),
                Arguments.of("9.9", 99),
                Arguments.of("10", 100),
                Arguments.of("10.0", 100),
            )

        @JvmStatic
        fun invalidas(): Stream<String> =
            Stream.of(
                "-0.1",
                "-1",
                "10.1",
                "11",
                "100",
                "7.25",
                "8.55",
                "abc",
                "7a",
                "1e1",
                "7 .5",
                ".5",
                "5.",
            )
    }

    @ParameterizedTest(name = "T1.01 \"{0}\" es válida → {1} décimas")
    @MethodSource("validas")
    fun t1_01_entradasValidas(
        entrada: String,
        decimas: Int?,
    ) {
        val lectura = Puntuacion.leer(entrada)
        if (decimas == null) {
            assertEquals(Puntuacion.Lectura.Vacia, lectura)
        } else {
            assertEquals(Puntuacion.Lectura.Valida(Puntuacion.of(decimas)), lectura)
        }
    }

    @ParameterizedTest(name = "T1.01 \"{0}\" no es válida")
    @MethodSource("invalidas")
    fun t1_01_entradasInvalidas(entrada: String) {
        assertEquals(Puntuacion.Lectura.Invalida, Puntuacion.leer(entrada))
    }

    @Test
    @DisplayName("T1.02 RNF-05: de 0 a 100 décimas, cada n/10 se acepta y se formatea con un decimal (el 100 como 10)")
    fun t1_02_todosLosValoresDeCeroADiez() {
        var aceptados = 0
        for (n in 0..100) {
            val texto = if (n == 100) "10" else "${n / 10}.${n % 10}"
            val lectura = Puntuacion.leer(texto)
            assertEquals(Puntuacion.Lectura.Valida(Puntuacion.of(n)), lectura, texto)
            assertEquals(texto, Puntuacion.of(n).formatear())
            // También con coma y sin errores de coma flotante al venir de un Double.
            assertEquals(n, (Puntuacion.leer(texto.replace('.', ',')) as Puntuacion.Lectura.Valida).puntuacion.decimas)
            assertEquals(n, (Puntuacion.leer((n / 10.0).toString()) as Puntuacion.Lectura.Valida).puntuacion.decimas)
            aceptados++
        }
        assertEquals(101, aceptados)
        assertEquals("10", Puntuacion.of(100).formatear())
        assertEquals("0.0", Puntuacion.of(0).formatear())
    }

    @Test
    @DisplayName("Puntuación fuera de rango desde la base de datos")
    fun fueraDeRango() {
        assertNull(Puntuacion.deDecimas(-1))
        assertNull(Puntuacion.deDecimas(101))
        assertThrows(IllegalArgumentException::class.java) { Puntuacion.of(101) }
    }

    @Test
    @DisplayName("RF-25: rangos de puntuación en los bordes")
    fun rangos() {
        val esperado =
            mapOf(
                0 to RangoPuntuacion.DE_0_A_2_9,
                29 to RangoPuntuacion.DE_0_A_2_9,
                30 to RangoPuntuacion.DE_3_A_4_9,
                49 to RangoPuntuacion.DE_3_A_4_9,
                50 to RangoPuntuacion.DE_5_A_6_9,
                69 to RangoPuntuacion.DE_5_A_6_9,
                70 to RangoPuntuacion.DE_7_A_8_9,
                89 to RangoPuntuacion.DE_7_A_8_9,
                90 to RangoPuntuacion.DE_9_A_10,
                100 to RangoPuntuacion.DE_9_A_10,
            )
        esperado.forEach { (d, rango) -> assertEquals(rango, Puntuacion.of(d).rango, "$d décimas") }
        assertEquals(RangoPuntuacion.SIN_PUNTUACION, RangoPuntuacion.de(null))
        // Cada valor de 0..100 cae en exactamente un rango.
        (0..100).forEach { d ->
            assertEquals(1, RangoPuntuacion.entries.count { it.minDecimas != null && d in it.minDecimas..it.maxDecimas!! })
        }
    }
}

class NormalizadorTest {
    @Test
    @DisplayName("T1.03 \"  EL  Pádrino \", \"el padrino\" y \"Él Padrino\" tienen la misma forma normalizada")
    fun t1_03_mismaFormaNormalizada() {
        val formas = listOf("  EL  Pádrino ", "el padrino", "Él Padrino").map(Normalizador::normalizar)
        assertEquals(setOf("el padrino"), formas.toSet())
    }

    @Test
    @DisplayName("Normalizador: diacríticos, eñes, tabuladores y nombre limpio para mostrar")
    fun otrosCasos() {
        assertEquals("pinon fijo", Normalizador.normalizar("Piñón\tFijo"))
        assertEquals("amelie", Normalizador.normalizar("Amélie"))
        assertEquals("", Normalizador.normalizar("   "))
        assertEquals("EL Pádrino", Normalizador.limpiar("  EL  Pádrino "))
    }
}

class RestriccionTest {
    @Test
    @DisplayName("T1.20 Una serie en SOLO_PELICULAS y una película en SOLO_SERIES se rechazan con M-09")
    fun t1_20_restriccionDeLista() {
        val e1 = assertThrows(DominioException::class.java) { Restriccion.SOLO_PELICULAS.exigirAdmite(TipoPerfil.SERIE) }
        assertEquals(ErrorDominio.RestriccionLista(TipoPerfil.PELICULA), e1.error)
        assertEquals("M-09", e1.error.codigo)
        val e2 = assertThrows(DominioException::class.java) { Restriccion.SOLO_SERIES.exigirAdmite(TipoPerfil.PELICULA) }
        assertEquals(ErrorDominio.RestriccionLista(TipoPerfil.SERIE), e2.error)

        Restriccion.SOLO_PELICULAS.exigirAdmite(TipoPerfil.PELICULA)
        Restriccion.SOLO_SERIES.exigirAdmite(TipoPerfil.SERIE)
        TipoPerfil.entries.forEach { assertTrue(Restriccion.MIXTO.admite(it)) }
    }
}
