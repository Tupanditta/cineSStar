package com.cinesstar.cinetrack.data.repository

import androidx.room.withTransaction
import com.cinesstar.cinetrack.data.room.BibliotecaDao
import com.cinesstar.cinetrack.data.room.BusquedaDao
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.data.room.EtiquetaPerfil
import com.cinesstar.cinetrack.domain.Ambito
import com.cinesstar.cinetrack.domain.CriteriosBusqueda
import com.cinesstar.cinetrack.domain.DominioException
import com.cinesstar.cinetrack.domain.Etiqueta
import com.cinesstar.cinetrack.domain.PerfilBiblioteca
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.domain.patronContiene
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

/**
 * Búsqueda combinada (CU-20). Los criterios se aplican en SQL en una sola consulta; los géneros e idiomas de los
 * resultados se leen con una consulta más para todos (sin una por perfil), para que cada búsqueda tarde menos de 1 s con
 * 1000 perfiles (RNF-08).
 */
class BusquedaRepository(
    private val db: CineDatabase,
    private val dao: BusquedaDao = db.busquedaDao(),
    private val bibliotecaDao: BibliotecaDao = db.bibliotecaDao(),
) {
    private companion object {
        val TABLAS = arrayOf("perfil", "saga", "perfil_genero", "perfil_idioma")
    }

    /**
     * Perfiles que cumplen todos los [criterios], más recientes primero (S-10). Errores: M-14 si el mínimo es mayor que
     * el máximo (CU-20 E2): no se ejecuta la búsqueda.
     */
    suspend fun buscar(criterios: CriteriosBusqueda): List<PerfilBiblioteca> {
        criterios.error()?.let { throw DominioException(it) }
        return db.withTransaction {
            val nombre = criterios.nombreNormalizado
            val filas =
                dao.buscar(
                    patron = nombre.takeIf { it.isNotEmpty() }?.let(::patronContiene),
                    tipo =
                        when (criterios.ambito) {
                            Ambito.TODO -> null
                            Ambito.PELICULAS -> TipoPerfil.PELICULA
                            Ambito.SERIES -> TipoPerfil.SERIE
                        },
                    minimo = criterios.minimo?.decimas,
                    maximo = criterios.maximo?.decimas,
                    conGeneros = criterios.generoIds.isNotEmpty(),
                    generoIds = criterios.generoIds.toList(),
                    idiomaId = criterios.idiomaId,
                    sagaId = criterios.sagaId,
                    conEstados = criterios.estados.isNotEmpty(),
                    estados = criterios.estados.toList(),
                )
            if (filas.isEmpty()) return@withTransaction emptyList()
            val generos = bibliotecaDao.generosDeTodos().porPerfil()
            val idiomas = bibliotecaDao.idiomasDeTodos().porPerfil()
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
    }

    /** [buscar] de nuevo cada vez que cambian los perfiles, sus sagas, géneros o idiomas. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observar(criterios: CriteriosBusqueda): Flow<List<PerfilBiblioteca>> =
        db.invalidationTracker
            .createFlow(*TABLAS)
            .mapLatest { buscar(criterios) }

    /** Sagas registradas para el desplegable "Saga / Franquicia", por nombre; se vuelven a leer al cambiar. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observarSagas(): Flow<List<Etiqueta>> =
        db.invalidationTracker
            .createFlow("saga")
            .mapLatest { db.sagaDao().todas().map { Etiqueta(it.id, it.nombre) } }

    private fun List<EtiquetaPerfil>.porPerfil(): Map<Long, List<Etiqueta>> =
        groupBy({ it.perfilId }, { Etiqueta(it.id, it.nombre, it.orden) })
}
