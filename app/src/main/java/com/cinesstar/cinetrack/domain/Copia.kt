package com.cinesstar.cinetrack.domain

import kotlinx.serialization.Serializable

/*
 * Formato de la copia de seguridad (CU-21, CU-22, RF-49). Es un JSON legible y versionado: los perfiles y las listas
 * se identifican por nombre (y tipo), no por los ids internos de la base de datos, para poder fusionar la copia con
 * otros datos (S-20). La carátula no se exporta: es un archivo del almacenamiento interno, no un dato portable.
 */

/** Copia de seguridad completa (CU-21 paso 3). */
@Serializable
data class CopiaSeguridad(
    /** Marca de la app: un JSON sin ella no es una copia de CineTrack (CU-22 E1). */
    val app: String = APP,
    val version: Int = VERSION_ACTUAL,
    /** Fecha y hora de la exportación (ISO 8601, hora local). */
    val fecha: String = "",
    /** Todas las sagas registradas, también las que no tienen perfiles (S-08). */
    val sagas: List<String> = emptyList(),
    val perfiles: List<PerfilCopia> = emptyList(),
    val listas: List<ListaCopia> = emptyList(),
) {
    companion object {
        const val APP = "CineTrack"

        /**
         * Versión del formato. Historial:
         * - 1: la puntuación se guardaba en décimas (`puntuacionDecimas`: 85).
         * - 2: la puntuación se guarda como en la interfaz (`puntuacion`: 8.5), legible a mano (RNF-04).
         */
        const val VERSION_ACTUAL = 2
    }
}

/** Un perfil con todos sus datos (RF-49). Fechas en milisegundos desde 1970 (UTC), como en la base de datos. */
@Serializable
data class PerfilCopia(
    val nombre: String,
    val tipo: TipoPerfil,
    val estado: Estado = Estado.PENDIENTE,
    /** 0.0..10 con un decimal como máximo, o null si no tiene (S-04). */
    val puntuacion: Double? = null,
    val comentario: String? = null,
    val comentarioEditadoEn: Long? = null,
    /** Nombre de su saga (RF-09). */
    val saga: String? = null,
    val generos: List<String> = emptyList(),
    val idiomas: List<String> = emptyList(),
    val creadoEn: Long = 0,
)

/** Una lista con sus elementos en el orden de su tipo. */
@Serializable
data class ListaCopia(
    val nombre: String,
    val tipo: TipoLista,
    val restriccion: Restriccion = Restriccion.MIXTO,
    val creadoEn: Long = 0,
    val elementos: List<ElementoCopia> = emptyList(),
)

/** Un elemento de lista: el perfil por nombre y tipo, con su posición (ranking) o prioridad (lista de prioridad). */
@Serializable
data class ElementoCopia(
    val nombre: String,
    val tipo: TipoPerfil,
    val posicion: Int? = null,
    val prioridad: Prioridad? = null,
    val anadidoEn: Long = 0,
)

/** Identidad de un perfil al fusionar: nombre normalizado y tipo (RES-06, S-20). */
data class ClavePerfil(
    val nombre: String,
    val tipo: TipoPerfil,
) {
    companion object {
        fun de(
            nombre: String,
            tipo: TipoPerfil,
        ) = ClavePerfil(Normalizador.normalizar(nombre), tipo)
    }
}

val PerfilCopia.clave: ClavePerfil get() = ClavePerfil.de(nombre, tipo)
val ElementoCopia.clave: ClavePerfil get() = ClavePerfil.de(nombre, tipo)

/** Por qué una copia no se puede importar. */
sealed interface ProblemaCopia {
    /** M-17. [detalle] indica el dato inconsistente (CU-22 E3), o null si el archivo no es una copia (E1). */
    data class NoValida(
        val detalle: DetalleCopia? = null,
    ) : ProblemaCopia

    /** M-18 (CU-22 E2). */
    data object VersionFutura : ProblemaCopia
}

/** Dato inconsistente de una copia (CU-22 E3). [perfil] y [lista] son los nombres tal como vienen en el archivo. */
sealed interface DetalleCopia {
    data object NombreVacio : DetalleCopia

    data class PuntuacionInvalida(
        val perfil: String,
    ) : DetalleCopia

    data class ComentarioLargo(
        val perfil: String,
    ) : DetalleCopia

    data class PerfilDuplicado(
        val perfil: String,
    ) : DetalleCopia

    data class GeneroDesconocido(
        val perfil: String,
        val genero: String,
    ) : DetalleCopia

    data class IdiomaDesconocido(
        val perfil: String,
        val idioma: String,
    ) : DetalleCopia

    data class ListaDuplicada(
        val lista: String,
    ) : DetalleCopia

    /** El elemento apunta a un perfil que no está en la copia. */
    data class ElementoSinPerfil(
        val lista: String,
        val perfil: String,
    ) : DetalleCopia

    /** El mismo perfil dos veces en una lista (RES-08). */
    data class ElementoRepetido(
        val lista: String,
        val perfil: String,
    ) : DetalleCopia

    /** El tipo del perfil viola la restricción de la lista (RF-32). */
    data class RestriccionLista(
        val lista: String,
        val perfil: String,
    ) : DetalleCopia

    /** Falta la posición (ranking) o la prioridad (lista de prioridad), o la posición se repite o no es positiva. */
    data class ElementoIncompleto(
        val lista: String,
        val perfil: String,
    ) : DetalleCopia
}

class CopiaException(
    val problema: ProblemaCopia,
) : RuntimeException(problema.toString())
