package com.cinesstar.cinetrack.domain

/** Saga, género o idioma de un perfil de la Biblioteca. [orden] es el del catálogo (géneros e idiomas). */
data class Etiqueta(
    val id: Long,
    val nombre: String,
    val orden: Int = 0,
)

/** Lo que muestra la tarjeta de medio de la Biblioteca (CU-08, design.md §4.1). */
data class PerfilBiblioteca(
    val id: Long,
    val nombre: String,
    val tipo: TipoPerfil,
    val estado: Estado,
    val puntuacion: Puntuacion?,
    val saga: Etiqueta?,
    /** En el orden del catálogo. */
    val generos: List<Etiqueta>,
    /** En el orden del catálogo. */
    val idiomas: List<Etiqueta>,
    val caratulaUri: String?,
    val creadoEn: Long,
) {
    val rango: RangoPuntuacion get() = RangoPuntuacion.de(puntuacion)
}

/** Ámbito de la Biblioteca y de la agrupación (CU-08 A1, CU-09 paso 1, RF-23). */
enum class Ambito {
    TODO,
    PELICULAS,
    SERIES,
    ;

    fun admite(tipo: TipoPerfil): Boolean =
        when (this) {
            TODO -> true
            PELICULAS -> tipo == TipoPerfil.PELICULA
            SERIES -> tipo == TipoPerfil.SERIE
        }
}

/** Criterios de agrupación (CU-09 paso 2, RF-24..RF-26). */
enum class CriterioAgrupacion { PUNTUACION, SAGA, IDIOMA, GENERO, ESTADO }

/** Grupo al que pertenece un perfil. Los "Sin ..." son los de S-11. */
sealed interface ClaveGrupo {
    data class Rango(
        val rango: RangoPuntuacion,
    ) : ClaveGrupo

    data class Saga(
        val id: Long,
        val nombre: String,
    ) : ClaveGrupo

    /** [orden]: posición en el catálogo (design.md §8.7). */
    data class Genero(
        val id: Long,
        val nombre: String,
        val orden: Int,
    ) : ClaveGrupo

    /** [orden]: posición en el catálogo (design.md §8.8). */
    data class Idioma(
        val id: Long,
        val nombre: String,
        val orden: Int,
    ) : ClaveGrupo

    data class DeEstado(
        val estado: Estado,
    ) : ClaveGrupo

    data object SinSaga : ClaveGrupo

    data object SinGenero : ClaveGrupo

    data object SinIdioma : ClaveGrupo
}

/** Un grupo con su nombre (clave) y sus perfiles, en el orden de la Biblioteca (S-10). */
data class Grupo(
    val clave: ClaveGrupo,
    val perfiles: List<PerfilBiblioteca>,
) {
    val total: Int get() = perfiles.size
}

/**
 * Agrupación de CU-09. Reglas:
 * - Puntuación: los cinco rangos de RF-25 y "Sin puntuación", de mayor a menor como los chips de escala (design.md §4).
 * - Saga: por nombre (sin tildes ni mayúsculas); "Sin saga" al final.
 * - Género e idioma: en el orden del catálogo; un perfil con varios aparece en cada grupo (RF-26); "Sin ..." al final.
 * - Estado: Pendiente, Viendo, Visto.
 * - Los grupos vacíos no aparecen (S-12): solo se crean grupos con algún perfil.
 * Dentro de cada grupo se conserva el orden de entrada (más recientes primero, S-10).
 */
object Agrupador {
    /** Orden de los rangos en los chips de escala y en los grupos. */
    val ORDEN_RANGOS =
        listOf(
            RangoPuntuacion.DE_9_A_10,
            RangoPuntuacion.DE_7_A_8_9,
            RangoPuntuacion.DE_5_A_6_9,
            RangoPuntuacion.DE_3_A_4_9,
            RangoPuntuacion.DE_0_A_2_9,
            RangoPuntuacion.SIN_PUNTUACION,
        )

    fun agrupar(
        perfiles: List<PerfilBiblioteca>,
        criterio: CriterioAgrupacion,
    ): List<Grupo> {
        val grupos = LinkedHashMap<ClaveGrupo, MutableList<PerfilBiblioteca>>()
        perfiles.forEach { perfil -> claves(perfil, criterio).forEach { grupos.getOrPut(it) { mutableListOf() }.add(perfil) } }
        return grupos.entries
            .sortedWith(compareBy<Map.Entry<ClaveGrupo, *>> { posicion(it.key) }.thenBy { textoOrden(it.key) })
            .map { Grupo(it.key, it.value) }
    }

    private fun claves(
        perfil: PerfilBiblioteca,
        criterio: CriterioAgrupacion,
    ): List<ClaveGrupo> =
        when (criterio) {
            CriterioAgrupacion.PUNTUACION -> listOf(ClaveGrupo.Rango(perfil.rango))
            CriterioAgrupacion.SAGA -> listOf(perfil.saga?.let { ClaveGrupo.Saga(it.id, it.nombre) } ?: ClaveGrupo.SinSaga)
            CriterioAgrupacion.GENERO ->
                perfil.generos.map { ClaveGrupo.Genero(it.id, it.nombre, it.orden) }.ifEmpty { listOf(ClaveGrupo.SinGenero) }
            CriterioAgrupacion.IDIOMA ->
                perfil.idiomas.map { ClaveGrupo.Idioma(it.id, it.nombre, it.orden) }.ifEmpty { listOf(ClaveGrupo.SinIdioma) }
            CriterioAgrupacion.ESTADO -> listOf(ClaveGrupo.DeEstado(perfil.estado))
        }.distinct()

    /** Posición numérica del grupo; los "Sin ..." van detrás de todos. */
    private fun posicion(clave: ClaveGrupo): Int =
        when (clave) {
            is ClaveGrupo.Rango -> ORDEN_RANGOS.indexOf(clave.rango)
            is ClaveGrupo.DeEstado -> clave.estado.ordinal
            is ClaveGrupo.Saga -> 0
            is ClaveGrupo.Genero -> clave.orden
            is ClaveGrupo.Idioma -> clave.orden
            ClaveGrupo.SinSaga, ClaveGrupo.SinGenero, ClaveGrupo.SinIdioma -> Int.MAX_VALUE
        }

    private fun textoOrden(clave: ClaveGrupo): String =
        when (clave) {
            is ClaveGrupo.Saga -> Normalizador.normalizar(clave.nombre)
            else -> ""
        }
}
