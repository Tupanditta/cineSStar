package com.cinesstar.cinetrack.domain

/** Una lista con lo que muestra su tarjeta en "Mis Listas" (CU-12, design.md §5.2). */
data class ResumenLista(
    val id: Long,
    val nombre: String,
    val tipo: TipoLista,
    val restriccion: Restriccion,
    /** Elementos de la lista. */
    val total: Int,
    /** Elementos con prioridad Alta (solo listas de prioridad). */
    val altas: Int = 0,
    /** Título en el puesto #1 (solo rankings con elementos). */
    val primero: String? = null,
)

/** Pantalla "Mis Listas": las listas y el total de perfiles de la Biblioteca ("{N} Obras catalogadas"). */
data class PanelListas(
    val listas: List<ResumenLista>,
    val obrasCatalogadas: Int,
)

/** Un elemento de una lista con los datos del perfil que pinta su fila (design.md §6.2). */
data class ElementoLista(
    val perfilId: Long,
    val nombre: String,
    val tipo: TipoPerfil,
    val puntuacion: Puntuacion?,
    val saga: String?,
    val generos: List<String>,
    /** Solo en RANKING (1..n). */
    val posicion: Int?,
    /** Solo en PRIORIDAD. */
    val prioridad: Prioridad?,
    val anadidoEn: Long,
)

/** Una lista abierta (CU-13): sus datos y los elementos en el orden de su tipo. */
data class ContenidoLista(
    val lista: ResumenLista,
    val elementos: List<ElementoLista>,
)

/**
 * Un perfil del selector "Añadir título" (CU-15, design.md §6.6). [detalle] es la saga o, si no tiene, sus géneros.
 * No está [disponible] si ya está en la lista (RES-08) o la restricción no lo admite (RF-32).
 */
data class Candidato(
    val perfilId: Long,
    val nombre: String,
    val nombreBusqueda: String,
    val tipo: TipoPerfil,
    val detalle: String?,
    val enLista: Boolean,
    val admitido: Boolean,
) {
    val disponible: Boolean get() = !enLista && admitido
}

/** Una lista del selector "Añadir a lista" del perfil (CU-15 desde el perfil). */
data class OpcionLista(
    val lista: ResumenLista,
    val contiene: Boolean,
    val admite: Boolean,
) {
    val disponible: Boolean get() = !contiene && admite
}

/** Orden de los elementos según el tipo de lista (CU-13, RF-42). */
object OrdenLista {
    fun ordenar(
        tipo: TipoLista,
        elementos: List<ElementoLista>,
    ): List<ElementoLista> =
        when (tipo) {
            TipoLista.NORMAL -> elementos.sortedWith(compareBy({ it.anadidoEn }, { it.perfilId }))
            // RF-42 / CU-17: Alta → Media → Baja; dentro de un nivel, por antigüedad en la lista.
            TipoLista.PRIORIDAD ->
                elementos.sortedWith(compareBy({ it.prioridad?.ordinal ?: Prioridad.entries.size }, { it.anadidoEn }, { it.perfilId }))
            TipoLista.RANKING -> elementos.sortedWith(compareBy({ it.posicion ?: Int.MAX_VALUE }, { it.anadidoEn }, { it.perfilId }))
        }

    /** CU-15 A5: el selector busca sin distinguir mayúsculas ni tildes. */
    fun filtrar(
        candidatos: List<Candidato>,
        busqueda: String,
    ): List<Candidato> {
        val clave = Normalizador.normalizar(busqueda)
        return if (clave.isEmpty()) candidatos else candidatos.filter { clave in it.nombreBusqueda }
    }
}
