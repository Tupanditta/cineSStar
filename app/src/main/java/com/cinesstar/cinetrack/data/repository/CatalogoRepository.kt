package com.cinesstar.cinetrack.data.repository

import com.cinesstar.cinetrack.data.room.CatalogoDao
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.data.room.GeneroEntity
import com.cinesstar.cinetrack.data.room.IdiomaEntity

/** Géneros e idiomas predefinidos (RF-07, RF-08), en el orden en que se muestran. */
class CatalogoRepository(
    db: CineDatabase,
    private val catalogoDao: CatalogoDao = db.catalogoDao(),
) {
    suspend fun generos(): List<GeneroEntity> = catalogoDao.generos()

    suspend fun idiomas(): List<IdiomaEntity> = catalogoDao.idiomas()
}
