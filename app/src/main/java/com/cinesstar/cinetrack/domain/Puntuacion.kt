package com.cinesstar.cinetrack.domain

/**
 * Puntuación de un perfil (S-04, RF-06, RNF-04, RNF-05). Se guarda en décimas (0..100) para evitar errores de coma
 * flotante: 8.5 = 85, 10 = 100.
 */
@JvmInline
value class Puntuacion private constructor(
    val decimas: Int,
) : Comparable<Puntuacion> {
    /** Formato de la interfaz: un decimal con punto, salvo el 10 que va sin decimales (RNF-04). */
    fun formatear(): String = if (decimas == MAX_DECIMAS) "10" else "${decimas / DECIMAS_POR_UNIDAD}.${decimas % DECIMAS_POR_UNIDAD}"

    /** Rango de agrupación (RF-25). */
    val rango: RangoPuntuacion get() = RangoPuntuacion.de(this)

    override fun compareTo(other: Puntuacion): Int = decimas.compareTo(other.decimas)

    override fun toString(): String = formatear()

    /** Resultado de interpretar el texto que escribe el usuario. */
    sealed interface Lectura {
        /** Campo vacío: es válido y significa "sin puntuación" (RF-03, RF-06). */
        data object Vacia : Lectura

        data class Valida(
            val puntuacion: Puntuacion,
        ) : Lectura

        /** No se registra; la interfaz muestra M-03. */
        data object Invalida : Lectura
    }

    companion object {
        const val MIN_DECIMAS = 0
        const val MAX_DECIMAS = 100
        private const val DECIMAS_POR_UNIDAD = 10

        /** Uno o dos dígitos, y opcionalmente `.` o `,` seguido de exactamente un dígito. */
        private val FORMATO = Regex("""^(\d{1,2})(?:[.,](\d))?$""")

        /**
         * Interpreta la entrada del usuario (tabla de sprints.md §1.4). Se ignoran los espacios de los extremos;
         * cualquier otro carácter, un signo, más de un decimal o un valor fuera de 0..10 la hacen inválida.
         */
        fun leer(texto: String): Lectura {
            val limpio = texto.trim()
            if (limpio.isEmpty()) return Lectura.Vacia
            val grupos = FORMATO.matchEntire(limpio)?.groupValues ?: return Lectura.Invalida
            val decimas = grupos[1].toInt() * DECIMAS_POR_UNIDAD + (grupos[2].takeIf { it.isNotEmpty() }?.toInt() ?: 0)
            return deDecimas(decimas)?.let { Lectura.Valida(it) } ?: Lectura.Invalida
        }

        /** Puntuación a partir de décimas guardadas; `null` si está fuera de 0..100. */
        fun deDecimas(decimas: Int): Puntuacion? = if (decimas in MIN_DECIMAS..MAX_DECIMAS) Puntuacion(decimas) else null

        /** Para valores que ya se sabe que son válidos (base de datos, constantes). */
        fun of(decimas: Int): Puntuacion = requireNotNull(deDecimas(decimas)) { "Puntuación fuera de rango: $decimas décimas" }
    }
}

/** Cinco rangos de puntuación y "Sin puntuación" (RF-25). Los límites están en décimas e incluyen ambos extremos. */
enum class RangoPuntuacion(
    val minDecimas: Int?,
    val maxDecimas: Int?,
) {
    DE_0_A_2_9(0, 29),
    DE_3_A_4_9(30, 49),
    DE_5_A_6_9(50, 69),
    DE_7_A_8_9(70, 89),
    DE_9_A_10(90, 100),
    SIN_PUNTUACION(null, null),
    ;

    companion object {
        fun de(puntuacion: Puntuacion?): RangoPuntuacion {
            if (puntuacion == null) return SIN_PUNTUACION
            return entries.first { it.minDecimas != null && puntuacion.decimas in it.minDecimas..it.maxDecimas!! }
        }
    }
}
