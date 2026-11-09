package com.cinesstar.cinetrack.ui.biblioteca

import com.cinesstar.cinetrack.domain.Ambito
import com.cinesstar.cinetrack.domain.ClaveGrupo
import com.cinesstar.cinetrack.domain.CriterioAgrupacion
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.RangoPuntuacion

/** Etiquetas de test de la Biblioteca. */
object TagsBiblioteca {
    const val LISTA = "biblioteca_lista"
    const val VACIA = "biblioteca_vacia"
    const val ANADIR = "biblioteca_anadir"
    const val ERROR = "biblioteca_error"
    const val REINTENTAR = "biblioteca_reintentar"
    const val MOSTRANDO = "biblioteca_mostrando"
    const val SIN_RESULTADOS = "biblioteca_sin_resultados"
    const val AGRUPAR = "biblioteca_agrupar"

    /** Tarjeta de un perfil sin agrupar. */
    fun fila(perfilId: Long) = "biblioteca_perfil_$perfilId"

    /** Tarjeta de un perfil dentro de un grupo (un perfil puede estar en varios, RF-26). */
    fun filaEnGrupo(
        clave: ClaveGrupo,
        perfilId: Long,
    ) = "${grupo(clave)}_perfil_$perfilId"

    fun insigniaEstado(perfilId: Long) = "biblioteca_estado_$perfilId"

    fun ambito(ambito: Ambito) = "biblioteca_ambito_${ambito.name.lowercase()}"

    fun contador(estado: Estado) = "biblioteca_contador_${estado.name.lowercase()}"

    fun rango(rango: RangoPuntuacion) = "biblioteca_rango_${rango.name.lowercase()}"

    /** Chip "Agrupar"; null = "Ninguno". */
    fun criterio(criterio: CriterioAgrupacion?) = "biblioteca_agrupar_${criterio?.name?.lowercase() ?: "ninguno"}"

    /** Cabecera de un grupo. */
    fun grupo(clave: ClaveGrupo) =
        "biblioteca_grupo_" +
            when (clave) {
                is ClaveGrupo.Rango -> "rango_${clave.rango.name.lowercase()}"
                is ClaveGrupo.Saga -> "saga_${clave.id}"
                is ClaveGrupo.Genero -> "genero_${clave.id}"
                is ClaveGrupo.Idioma -> "idioma_${clave.id}"
                is ClaveGrupo.DeEstado -> "estado_${clave.estado.name.lowercase()}"
                ClaveGrupo.SinSaga -> "sin_saga"
                ClaveGrupo.SinGenero -> "sin_genero"
                ClaveGrupo.SinIdioma -> "sin_idioma"
            }

    /** Recuento de la cabecera de un grupo. */
    fun totalGrupo(clave: ClaveGrupo) = grupo(clave) + "_total"
}
