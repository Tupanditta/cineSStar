package com.cinesstar.cinetrack.ui.lista

/** Etiquetas de test de la lista abierta y del selector "Añadir título" (no son textos visibles). */
object TagsLista {
    const val PANTALLA = "pantalla_lista"
    const val LISTA = "lista_elementos"
    const val TIPO = "lista_tipo"
    const val TOTAL = "lista_total"
    const val TITULO = "lista_titulo"
    const val RENOMBRAR = "lista_renombrar"
    const val MENU = "lista_menu"
    const val MENU_ELIMINAR = "lista_menu_eliminar"
    const val ANADIR = "lista_anadir"
    const val VACIA = "lista_vacia"
    const val VACIA_ANADIR = "lista_vacia_anadir"
    const val ELIMINAR = "lista_eliminar"

    // Selector "Añadir título"
    const val SELECTOR = "lista_selector"
    const val SELECTOR_BUSCAR = "lista_selector_buscar"
    const val SELECTOR_RESULTADOS = "lista_selector_resultados"
    const val SELECTOR_SIN_RESULTADOS = "lista_selector_sin_resultados"
    const val SELECTOR_CERRAR = "lista_selector_cerrar"

    fun fila(perfilId: Long) = "lista_fila_$perfilId"

    fun puesto(perfilId: Long) = "lista_fila_${perfilId}_puesto"

    fun prioridad(perfilId: Long) = "lista_fila_${perfilId}_prioridad"

    fun quitar(perfilId: Long) = "lista_fila_${perfilId}_quitar"

    fun candidato(perfilId: Long) = "lista_candidato_$perfilId"

    /** Icono "añadir" de un candidato disponible (los no disponibles no lo tienen, design.md §6.6). */
    fun candidatoAnadir(perfilId: Long) = "lista_candidato_${perfilId}_anadir"
}
