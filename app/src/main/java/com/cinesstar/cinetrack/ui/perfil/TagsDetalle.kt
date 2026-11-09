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

    fun estado(estado: Estado) = "detalle_estado_" + estado.name.lowercase()
}
