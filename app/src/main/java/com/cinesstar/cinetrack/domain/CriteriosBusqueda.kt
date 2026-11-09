package com.cinesstar.cinetrack.domain

/**
 * Criterios de la búsqueda combinada (CU-20). Un criterio vacío o nulo no filtra; entre criterios distintos se exige
 * que se cumplan todos (AND) y dentro de los géneros basta uno (OR, S-19).
 *
 * Rango de puntuación (RF-44): inclusivo. Si solo hay [minimo], el máximo es 10; si solo hay [maximo], el mínimo es
 * 0.0. Con el rango activo (alguno de los dos extremos), los perfiles sin puntuación quedan fuera. 0.0–10 equivale a
 * no tener rango (design.md §9.2-3): ver [conRango].
 */
data class CriteriosBusqueda(
    /** Texto que debe contener el nombre, sin distinguir mayúsculas ni tildes (RF-46). */
    val nombre: String = "",
    /** Tipo: por defecto ambos (RF-47). */
    val ambito: Ambito = Ambito.TODO,
    val minimo: Puntuacion? = null,
    val maximo: Puntuacion? = null,
    val generoIds: Set<Long> = emptySet(),
    val idiomaId: Long? = null,
    val sagaId: Long? = null,
    /** Extensión del prototipo (S-18): uno o varios estados (OR). */
    val estados: Set<Estado> = emptySet(),
) {
    /** Hay rango de puntuación: los perfiles sin nota no aparecen. */
    val rangoActivo: Boolean get() = minimo != null || maximo != null

    /** Texto del nombre tal como se compara con `nombreBusqueda`; vacío = sin criterio de nombre. */
    val nombreNormalizado: String get() = Normalizador.normalizar(nombre)

    /** CU-20 E2: M-14 si el mínimo es mayor que el máximo; null si los criterios son válidos. */
    fun error(): ErrorDominio? {
        val min = minimo
        val max = maximo
        return if (min != null && max != null && min > max) ErrorDominio.RangoInvertido else null
    }

    /** Ningún criterio activo (CU-20 A2: devuelve todos los perfiles). */
    val vacios: Boolean get() = this == CriteriosBusqueda()

    companion object {
        /**
         * Criterios con el rango [minimo]..[maximo] tal como lo deja el slider: los extremos 0.0 y 10 son "sin límite",
         * de modo que 0.0–10 no activa el filtro (design.md §9.2-3).
         */
        fun CriteriosBusqueda.conRango(
            minimo: Puntuacion?,
            maximo: Puntuacion?,
        ): CriteriosBusqueda =
            copy(
                minimo = minimo?.takeIf { it.decimas > Puntuacion.MIN_DECIMAS },
                maximo = maximo?.takeIf { it.decimas < Puntuacion.MAX_DECIMAS },
            )
    }
}

/**
 * Patrón `LIKE` que busca [texto] como subcadena literal: `%`, `_` y el propio carácter de escape `\` se escapan
 * (CU-20 E4). Se usa con `ESCAPE '\'`. Las comillas no necesitan escape porque el valor va como parámetro.
 */
fun patronContiene(texto: String): String {
    val escapado = buildString { texto.forEach { c -> if (c == '\\' || c == '%' || c == '_') append('\\').append(c) else append(c) } }
    return "%$escapado%"
}
