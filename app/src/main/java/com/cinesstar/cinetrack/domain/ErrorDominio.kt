package com.cinesstar.cinetrack.domain

/**
 * Errores de negocio. Cada uno corresponde a un mensaje del catálogo de casosUso.md §1.3; la interfaz lo traduce a su
 * texto de `strings.xml`.
 */
sealed class ErrorDominio(
    val codigo: String,
) {
    /** M-01 */
    data object NombreObligatorio : ErrorDominio("M-01")

    /** M-02 */
    data class PerfilDuplicado(
        val tipo: TipoPerfil,
    ) : ErrorDominio("M-02")

    /** M-03 */
    data object PuntuacionInvalida : ErrorDominio("M-03")

    /** M-04 */
    data object SagaDuplicada : ErrorDominio("M-04")

    /** M-05 */
    data object SagaNombreObligatorio : ErrorDominio("M-05")

    /** M-07 */
    data object ListaNombreObligatorio : ErrorDominio("M-07")

    /** M-08 */
    data object ListaDuplicada : ErrorDominio("M-08")

    /** M-09: la lista solo admite el tipo [admitido]. */
    data class RestriccionLista(
        val admitido: TipoPerfil,
    ) : ErrorDominio("M-09")

    /** M-10 */
    data class YaEnLista(
        val nombrePerfil: String,
    ) : ErrorDominio("M-10")

    /** M-14: en la búsqueda, la puntuación mínima es mayor que la máxima (CU-20 E2). */
    data object RangoInvertido : ErrorDominio("M-14")

    /** M-24 */
    data object PosicionInvalida : ErrorDominio("M-24")

    /** No existe el perfil, la saga o la lista indicados (borrados mientras tanto). Se muestra como M-25. */
    data object NoEncontrado : ErrorDominio("M-25")
}

/** Excepción que transporta un [ErrorDominio]. Las transacciones que la lanzan no dejan cambios (atomicidad). */
class DominioException(
    val error: ErrorDominio,
) : RuntimeException(error.toString())

/** Comprobación de la restricción de una lista (RF-32, M-09). */
fun Restriccion.exigirAdmite(tipo: TipoPerfil) {
    if (!admite(tipo)) {
        val admitido = if (this == Restriccion.SOLO_PELICULAS) TipoPerfil.PELICULA else TipoPerfil.SERIE
        throw DominioException(ErrorDominio.RestriccionLista(admitido))
    }
}
