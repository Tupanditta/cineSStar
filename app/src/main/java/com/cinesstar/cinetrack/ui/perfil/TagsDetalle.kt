package com.cinesstar.cinetrack.ui.perfil

import com.cinesstar.cinetrack.domain.Estado

/** Etiquetas de test del detalle (no son textos visibles). */
object TagsDetalle {
    const val PANTALLA = "pantalla_detalle"
    const val SCROLL = "detalle_scroll"
    const val TIPO = "detalle_tipo"
    const val ESTADO_HERO = "detalle_estado_hero"
    const val TITULO = "detalle_titulo"
    const val NOTA = "detalle_nota"
    const val RANGO = "detalle_rango"
    const val SAGA = "detalle_saga"
    const val GENEROS = "detalle_generos"
    const val IDIOMAS = "detalle_idiomas"
    const val LISTAS = "detalle_listas"
    const val COMENTARIO = "detalle_comentario"
    const val MENU = "detalle_menu"
    const val MENU_EDITAR = "detalle_menu_editar"
    const val MENU_ELIMINAR = "detalle_menu_eliminar"
    const val MENU_ANADIR_LISTA = "detalle_menu_anadir_lista"
    const val ANADIR_A_LISTA = "detalle_anadir_a_lista"
    const val SELECTOR_LISTAS = "detalle_selector_listas"

    /** Ranking o lista de prioridad desplegada en el selector, con su botón "Añadir" (CU-15 paso 3). */
    const val ALTA_DESPLEGADA = "detalle_alta_desplegada"
    const val CONFIRMAR_ALTA = "detalle_confirmar_alta"
    const val AJUSTAR = "detalle_ajustar"
    const val EDITOR_NOTA = "detalle_editor_nota"
    const val EDITOR_NOTA_CAMPO = "detalle_editor_nota_campo"
    const val EDITOR_NOTA_SLIDER = "detalle_editor_nota_slider"
    const val EDITOR_NOTA_ERROR = "detalle_editor_nota_error"
    const val EDITOR_NOTA_GUARDAR = "detalle_editor_nota_guardar"
    const val QUITAR_NOTA = "detalle_quitar_nota"
    const val EDITAR_CRITICA = "detalle_editar_critica"
    const val BORRAR_CRITICA = "detalle_borrar_critica"
    const val CRITICA_CAMPO = "detalle_critica_campo"
    const val CRITICA_CONTADOR = "detalle_critica_contador"
    const val ELIMINAR = "detalle_eliminar"

    fun estado(estado: Estado) = "detalle_estado_" + estado.name.lowercase()

    /** Fila de una lista en "En tus Listas". */
    fun lista(listaId: Long) = "detalle_lista_$listaId"

    /** Opción de una lista en el selector "Añadir a lista". */
    fun opcionLista(listaId: Long) = "detalle_opcion_lista_$listaId"
}
