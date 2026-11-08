package com.cinesstar.cinetrack.domain

/** Película o serie (RF-04). No se puede cambiar después de crear el perfil (S-03). */
enum class TipoPerfil { PELICULA, SERIE }

/**
 * Estado de visualización. `VISTO` es el "sin estado" del requisito (S-01): ni pendiente ni viendo.
 * No hay campos de progreso para las series (RF-12).
 */
enum class Estado { PENDIENTE, VIENDO, VISTO }

/** Tipo de lista (RF-27, RF-28, RF-29). */
enum class TipoLista { NORMAL, PRIORIDAD, RANKING }

/** Restricción de contenido de una lista (RF-30, RF-32). Solo se elige al crear la lista (S-14). */
enum class Restriccion {
    MIXTO,
    SOLO_PELICULAS,
    SOLO_SERIES,
    ;

    fun admite(tipo: TipoPerfil): Boolean =
        when (this) {
            MIXTO -> true
            SOLO_PELICULAS -> tipo == TipoPerfil.PELICULA
            SOLO_SERIES -> tipo == TipoPerfil.SERIE
        }
}

/** Niveles de prioridad (RF-40). El orden de declaración es el de la lista: Alta → Media → Baja. */
enum class Prioridad {
    ALTA,
    MEDIA,
    BAJA,
    ;

    companion object {
        /** Prioridad al añadir sin indicarla (S-16). */
        val POR_DEFECTO = MEDIA
    }
}

/** Géneros predefinidos (design.md §8.7) en el orden en que se muestran. */
val GENEROS_PREDEFINIDOS =
    listOf(
        "Acción",
        "Drama",
        "Comedia",
        "Ciencia Ficción",
        "Terror",
        "Suspense",
        "Animación",
        "Fantasía",
        "Documental",
        "Crimen",
        "Romance",
        "Historia",
        "Thriller",
        "Aventura",
    )

/** Idiomas predefinidos (design.md §8.8) en el orden en que se muestran. */
val IDIOMAS_PREDEFINIDOS =
    listOf(
        "Español",
        "Inglés",
        "Francés",
        "Japonés",
        "Alemán",
        "Coreano",
        "Italiano",
        "Portugués",
        "Chino",
    )
