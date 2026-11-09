package com.cinesstar.cinetrack.ui.components

import androidx.annotation.StringRes
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista

// Textos e iconos de las listas (design.md §5, §6).

@StringRes
fun TipoLista.etiqueta(): Int =
    when (this) {
        TipoLista.NORMAL -> R.string.tipo_lista_normal
        TipoLista.PRIORIDAD -> R.string.tipo_lista_prioridad
        TipoLista.RANKING -> R.string.tipo_lista_ranking
    }

/** Icono del tipo de lista en el diálogo "Nueva Lista" (design.md §5.3). */
fun TipoLista.iconoMosaico(): String =
    when (this) {
        TipoLista.NORMAL -> "view_list"
        TipoLista.PRIORIDAD -> "flag"
        TipoLista.RANKING -> "military_tech"
    }

/** Segmentos "Mixto / Películas / Series" del diálogo "Nueva Lista" (design.md §5.3). */
@StringRes
fun Restriccion.etiqueta(): Int =
    when (this) {
        Restriccion.MIXTO -> R.string.restriccion_mixto
        Restriccion.SOLO_PELICULAS -> R.string.restriccion_peliculas
        Restriccion.SOLO_SERIES -> R.string.restriccion_series
    }

/** "Solo Películas" / "Solo Series"; null si es mixta. */
@StringRes
fun Restriccion.etiquetaSolo(): Int? =
    when (this) {
        Restriccion.MIXTO -> null
        Restriccion.SOLO_PELICULAS -> R.string.restriccion_solo_peliculas
        Restriccion.SOLO_SERIES -> R.string.restriccion_solo_series
    }
