package com.cinesstar.cinetrack.domain

import java.text.Normalizer
import java.util.Locale

/**
 * Forma normalizada de un nombre (casosUso.md §1.1): minúsculas, sin tildes ni diacríticos y sin espacios sobrantes.
 * Se usa para detectar duplicados (RES-06, RF-10, S-15) y para buscar sin distinguir mayúsculas ni acentos (RF-46).
 */
object Normalizador {
    private val DIACRITICOS = Regex("""\p{Mn}+""")
    private val ESPACIOS = Regex("""\s+""")

    fun normalizar(nombre: String): String {
        val sinDiacriticos = DIACRITICOS.replace(Normalizer.normalize(nombre, Normalizer.Form.NFD), "")
        return ESPACIOS.replace(sinDiacriticos.trim(), " ").lowercase(Locale.ROOT)
    }

    /** Nombre tal como se guarda para mostrarlo: sin espacios en los extremos ni repetidos, pero con mayúsculas y tildes. */
    fun limpiar(nombre: String): String = ESPACIOS.replace(nombre.trim(), " ")
}
