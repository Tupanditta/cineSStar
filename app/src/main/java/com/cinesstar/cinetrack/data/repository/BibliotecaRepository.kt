package com.cinesstar.cinetrack.data.repository

import androidx.room.withTransaction
import com.cinesstar.cinetrack.data.room.BibliotecaDao
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.data.room.EtiquetaPerfil
import com.cinesstar.cinetrack.domain.Agrupador
import com.cinesstar.cinetrack.domain.Ambito
import com.cinesstar.cinetrack.domain.CriterioAgrupacion
import com.cinesstar.cinetrack.domain.Etiqueta
import com.cinesstar.cinetrack.domain.Grupo
import com.cinesstar.cinetrack.domain.PerfilBiblioteca
import com.cinesstar.cinetrack.domain.Puntuacion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

/**
 * Lectura de la Biblioteca (CU-08) y agrupación (CU-09). Tres consultas para toda la Biblioteca (perfiles con saga,
 * géneros e idiomas), sin una consulta por perfil, para abrirla en menos de 1 s con 1000 perfiles (RNF-08).
 */
class BibliotecaRepository(
    private val db: CineDatabase,
    private val dao: BibliotecaDao = db.bibliotecaDao(),
) {
    private companion object {
        val TABLAS = arrayOf("perfil", "saga", "perfil_genero", "perfil_idioma")
    }

    /** Todos los perfiles, más recientes primero (S-10); se vuelve a leer cuando cambian. Un fallo de lectura se propaga. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observar(): Flow<List<PerfilBiblioteca>> =
        db.invalidationTracker
            .createFlow(*TABLAS)
            .mapLatest { leer() }

    suspend fun leer(): List<PerfilBiblioteca> =
        db.withTransaction {
            val filas = dao.filasBiblioteca()
            val generos = dao.generosDeTodos().porPerfil()
            val idiomas = dao.idiomasDeTodos().porPerfil()
            filas.map { f ->
                PerfilBiblioteca(
                    id = f.id,
                    nombre = f.nombre,
                    tipo = f.tipo,
                    estado = f.estado,
                    puntuacion = f.puntuacionDecimas?.let { Puntuacion.of(it) },
                    saga = f.sagaId?.let { Etiqueta(it, f.sagaNombre.orEmpty()) },
                    generos = generos[f.id].orEmpty(),
                    idiomas = idiomas[f.id].orEmpty(),
                    caratulaUri = f.caratulaUri,
                    creadoEn = f.creadoEn,
                )
            }
        }

    /** CU-09: grupos del [criterio] dentro del [ambito] (RF-23). */
    suspend fun agrupar(
        criterio: CriterioAgrupacion,
        ambito: Ambito = Ambito.TODO,
    ): List<Grupo> = Agrupador.agrupar(leer().filter { ambito.admite(it.tipo) }, criterio)

    /** Agrupa por perfil conservando el orden del catálogo de la consulta. */
    private fun List<EtiquetaPerfil>.porPerfil(): Map<Long, List<Etiqueta>> =
        groupBy({ it.perfilId }, { Etiqueta(it.id, it.nombre, it.orden) })
}
