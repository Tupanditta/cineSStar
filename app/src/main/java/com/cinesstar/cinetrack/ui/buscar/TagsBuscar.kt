package com.cinesstar.cinetrack.ui.buscar

import com.cinesstar.cinetrack.domain.Ambito
import com.cinesstar.cinetrack.domain.Estado

/** Etiquetas de test de la pantalla Buscar (design.md §9). */
object TagsBuscar {
    const val LISTA = "buscar_lista"
    const val TAB_FILTROS = "buscar_tab_filtros"
    const val TAB_DATOS = "buscar_tab_datos"
    const val NOMBRE = "buscar_nombre"
    const val BORRAR_NOMBRE = "buscar_borrar_nombre"
    const val COINCIDENCIAS = "buscar_coincidencias"
    const val RANGO = "buscar_rango"
    const val MINIMO = "buscar_minimo"
    const val MAXIMO = "buscar_maximo"
    const val SLIDER = "buscar_slider"
    const val EXCLUYE = "buscar_excluye"
    const val DESELECCIONAR = "buscar_deseleccionar"
    const val IDIOMA = "buscar_idioma"
    const val SAGA = "buscar_saga"
    const val LIMPIAR = "buscar_limpiar"
    const val APLICAR = "buscar_aplicar"
    const val RESULTADOS = "buscar_resultados"
    const val TOTAL_RESULTADOS = "buscar_total_resultados"
    const val SIN_RESULTADOS = "buscar_sin_resultados"
    const val ERROR = "buscar_error"

    fun ambito(ambito: Ambito) = "buscar_ambito_${ambito.name.lowercase()}"

    fun genero(id: Long) = "buscar_genero_$id"

    /** Opción del desplegable de idioma; null = "Todos los idiomas". */
    fun opcionIdioma(id: Long?) = "buscar_idioma_${id ?: "todos"}"

    /** Opción del desplegable de saga; null = "Cualquier saga". */
    fun opcionSaga(id: Long?) = "buscar_saga_${id ?: "cualquiera"}"

    fun estado(estado: Estado) = "buscar_estado_${estado.name.lowercase()}"

    /** Tarjeta de un resultado. */
    fun resultado(perfilId: Long) = "buscar_resultado_$perfilId"
}
