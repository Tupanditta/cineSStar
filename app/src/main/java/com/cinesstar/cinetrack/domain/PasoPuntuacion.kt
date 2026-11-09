package com.cinesstar.cinetrack.domain

import kotlin.math.roundToInt

/**
 * Botones − / + y slider de la puntuación (CU-03 A3, S-05). Trabajan en décimas para no acumular errores de coma
 * flotante (RNF-05).
 */
object PasoPuntuacion {
    /** Con el campo vacío (o inválido) los botones parten de 5.0, como en el prototipo (S-05). */
    const val BASE_SIN_VALOR = 50

    /** Cada pulsación cambia 0.1. */
    const val PASO = 1

    private const val DECIMAS_POR_UNIDAD = 10f

    private fun base(texto: String): Int = (Puntuacion.leer(texto) as? Puntuacion.Lectura.Valida)?.puntuacion?.decimas ?: BASE_SIN_VALOR

    /** Texto tras pulsar "−" ([delta] = −[PASO]) o "+" ([delta] = [PASO]), limitado a 0.0..10. */
    fun aplicar(
        texto: String,
        delta: Int,
    ): String = Puntuacion.of((base(texto) + delta).coerceIn(Puntuacion.MIN_DECIMAS, Puntuacion.MAX_DECIMAS)).formatear()

    /** "−" se deshabilita en 0.0. */
    fun puedeBajar(texto: String): Boolean = base(texto) > Puntuacion.MIN_DECIMAS

    /** "+" se deshabilita en 10. */
    fun puedeSubir(texto: String): Boolean = base(texto) < Puntuacion.MAX_DECIMAS

    /** Posición del slider (0f..10f) para el texto del campo; 0 si está vacío o es inválido. */
    fun valorSlider(texto: String): Float =
        ((Puntuacion.leer(texto) as? Puntuacion.Lectura.Valida)?.puntuacion?.decimas ?: 0) / DECIMAS_POR_UNIDAD

    /** Texto del campo para una posición del slider, redondeada a la décima más cercana. */
    fun desdeSlider(valor: Float): String =
        Puntuacion.of((valor * DECIMAS_POR_UNIDAD).roundToInt().coerceIn(Puntuacion.MIN_DECIMAS, Puntuacion.MAX_DECIMAS)).formatear()
}
