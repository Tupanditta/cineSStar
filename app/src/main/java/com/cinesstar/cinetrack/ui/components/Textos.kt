package com.cinesstar.cinetrack.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cinesstar.cinetrack.R
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.RangoPuntuacion
import com.cinesstar.cinetrack.domain.TipoPerfil

/** Texto del catálogo de mensajes (casosUso.md §1.3) para un error de dominio. */
@Suppress("CyclomaticComplexMethod") // Un caso por mensaje del catálogo.
@Composable
fun textoError(error: ErrorDominio): String =
    when (error) {
        ErrorDominio.NombreObligatorio -> stringResource(R.string.m01_nombre_obligatorio)
        is ErrorDominio.PerfilDuplicado ->
            stringResource(
                if (error.tipo == TipoPerfil.PELICULA) R.string.m02_perfil_duplicado_pelicula else R.string.m02_perfil_duplicado_serie,
            )
        ErrorDominio.PuntuacionInvalida -> stringResource(R.string.m03_puntuacion_invalida)
        ErrorDominio.SagaDuplicada -> stringResource(R.string.m04_saga_duplicada)
        ErrorDominio.SagaNombreObligatorio -> stringResource(R.string.m05_saga_nombre_obligatorio)
        ErrorDominio.ListaNombreObligatorio -> stringResource(R.string.m07_lista_nombre_obligatorio)
        ErrorDominio.ListaDuplicada -> stringResource(R.string.m08_lista_duplicada)
        is ErrorDominio.RestriccionLista ->
            stringResource(if (error.admitido == TipoPerfil.PELICULA) R.string.m09_lista_solo_peliculas else R.string.m09_lista_solo_series)
        is ErrorDominio.YaEnLista -> stringResource(R.string.m10_ya_en_lista, error.nombrePerfil)
        ErrorDominio.RangoInvertido -> stringResource(R.string.m14_minimo_mayor_que_maximo)
        ErrorDominio.PosicionInvalida -> stringResource(R.string.m24_posicion_invalida)
        ErrorDominio.NoEncontrado -> stringResource(R.string.m25_error_inesperado)
    }

@StringRes
fun TipoPerfil.etiqueta(): Int = if (this == TipoPerfil.PELICULA) R.string.tipo_pelicula else R.string.tipo_serie

fun TipoPerfil.icono(): String = if (this == TipoPerfil.PELICULA) "movie" else "tv"

@StringRes
fun Estado.etiqueta(): Int =
    when (this) {
        Estado.PENDIENTE -> R.string.estado_pendiente
        Estado.VIENDO -> R.string.estado_viendo
        Estado.VISTO -> R.string.estado_visto
    }

/** Iconos de estado del formulario (design.md §8.4). */
fun Estado.iconoFormulario(): String =
    when (this) {
        Estado.PENDIENTE -> "schedule"
        Estado.VIENDO -> "play_circle"
        Estado.VISTO -> "check_circle"
    }

/** Iconos de estado del detalle (design.md §7.2). */
fun Estado.iconoDetalle(): String =
    when (this) {
        Estado.PENDIENTE -> "bookmark_add"
        Estado.VIENDO -> "play_circle"
        Estado.VISTO -> "check_circle"
    }

@StringRes
fun Prioridad.etiqueta(): Int =
    when (this) {
        Prioridad.ALTA -> R.string.prioridad_alta
        Prioridad.MEDIA -> R.string.prioridad_media
        Prioridad.BAJA -> R.string.prioridad_baja
    }

/** Etiqueta del rango de la nota en el detalle (design.md §7.3); null = sin puntuación. */
@StringRes
fun RangoPuntuacion.etiqueta(): Int? =
    when (this) {
        RangoPuntuacion.DE_9_A_10 -> R.string.rango_9_10
        RangoPuntuacion.DE_7_A_8_9 -> R.string.rango_7_8
        RangoPuntuacion.DE_5_A_6_9 -> R.string.rango_5_6
        RangoPuntuacion.DE_3_A_4_9 -> R.string.rango_3_4
        RangoPuntuacion.DE_0_A_2_9 -> R.string.rango_0_2
        RangoPuntuacion.SIN_PUNTUACION -> null
    }

/** Icono de cada género predefinido (design.md §7.8); `movie` para los que no tienen uno asignado. */
fun iconoGenero(nombre: String): String = ICONOS_GENERO[nombre] ?: "movie"

private val ICONOS_GENERO =
    mapOf(
        "Ciencia Ficción" to "rocket_launch",
        "Aventura" to "explore",
        "Drama" to "theater_comedy",
        "Acción" to "sports_martial_arts",
        "Comedia" to "sentiment_very_satisfied",
        "Terror" to "skull",
        "Suspense" to "visibility",
        "Animación" to "animation",
        "Fantasía" to "auto_fix_high",
        "Documental" to "videocam",
        "Crimen" to "local_police",
        "Romance" to "favorite",
        "Historia" to "history_edu",
        "Thriller" to "psychology",
    )
