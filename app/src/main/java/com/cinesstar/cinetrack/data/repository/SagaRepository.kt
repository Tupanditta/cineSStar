package com.cinesstar.cinetrack.data.repository

import androidx.room.withTransaction
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.data.room.SagaDao
import com.cinesstar.cinetrack.data.room.SagaEntity
import com.cinesstar.cinetrack.domain.DominioException
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Normalizador

/** Sagas registradas (RF-10, CU-10). Una saga sin perfiles sigue registrada (S-08). */
class SagaRepository(
    private val db: CineDatabase,
    private val sagaDao: SagaDao = db.sagaDao(),
) {
    /** Crea una saga nueva. Errores: M-05 (nombre vacío), M-04 (ya existe con el mismo nombre normalizado). */
    suspend fun crear(nombre: String): Long =
        db.withTransaction {
            val limpio = Normalizador.limpiar(nombre)
            if (limpio.isEmpty()) throw DominioException(ErrorDominio.SagaNombreObligatorio)
            val nombreBusqueda = Normalizador.normalizar(limpio)
            if (sagaDao.buscarPorNombre(nombreBusqueda) != null) throw DominioException(ErrorDominio.SagaDuplicada)
            sagaDao.insertar(SagaEntity(nombre = limpio, nombreBusqueda = nombreBusqueda))
        }

    /** Devuelve la saga registrada con ese nombre normalizado o la crea si no existe (sin reescribirla, RF-10). */
    suspend fun obtenerOCrear(nombre: String): Long =
        db.withTransaction {
            val limpio = Normalizador.limpiar(nombre)
            if (limpio.isEmpty()) throw DominioException(ErrorDominio.SagaNombreObligatorio)
            sagaDao.buscarPorNombre(Normalizador.normalizar(limpio))?.id ?: crear(limpio)
        }

    suspend fun todas(): List<SagaEntity> = sagaDao.todas()
}
