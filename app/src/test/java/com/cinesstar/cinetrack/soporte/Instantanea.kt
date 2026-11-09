package com.cinesstar.cinetrack.soporte

import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.domain.CopiaSeguridad
import com.cinesstar.cinetrack.domain.ElementoCopia
import com.cinesstar.cinetrack.domain.ListaCopia
import com.cinesstar.cinetrack.domain.PerfilCopia
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.ReglasCopia
import com.cinesstar.cinetrack.domain.TipoLista

/** Fotografías de la base de datos para comparar datos antes y después de exportar o importar (S8). */
object Instantanea {
    /**
     * Todos los datos del usuario sin ids internos, una línea por saga, perfil, lista y elemento, ordenadas: dos bases
     * de datos con el mismo contenido dan la misma lista (ida y vuelta, T8.06). No incluye la carátula (no se exporta)
     * ni el indicador interno del aviso de lista vacía.
     */
    suspend fun contenido(db: CineDatabase): List<String> {
        val dao = db.copiaDao()
        val sagas = db.sagaDao().todas().associateBy { it.id }
        val generos = db.catalogoDao().generos().associateBy { it.id }
        val idiomas = db.catalogoDao().idiomas().associateBy { it.id }
        val generosDe =
            dao
                .perfilGeneros()
                .groupBy(
                    { it.perfilId },
                    { generos.getValue(it.generoId).nombre },
                ).mapValues { it.value.sorted() }
        val idiomasDe =
            dao
                .perfilIdiomas()
                .groupBy(
                    { it.perfilId },
                    { idiomas.getValue(it.idiomaId).nombre },
                ).mapValues { it.value.sorted() }
        val perfiles = dao.perfiles().associateBy { it.id }
        val listas = dao.listas().associateBy { it.id }
        val lineas = mutableListOf<String>()
        sagas.values.forEach { lineas += "saga|${it.nombre}" }
        perfiles.values.forEach { p ->
            lineas +=
                listOf(
                    "perfil",
                    p.nombre,
                    p.tipo,
                    p.estado,
                    p.puntuacionDecimas,
                    p.comentario,
                    p.comentarioEditadoEn,
                    p.sagaId?.let { sagas.getValue(it).nombre },
                    generosDe[p.id].orEmpty(),
                    idiomasDe[p.id].orEmpty(),
                    p.creadoEn,
                ).joinToString("|")
        }
        listas.values.forEach { lineas += "lista|${it.nombre}|${it.tipo}|${it.restriccion}|${it.creadoEn}" }
        dao.items().forEach { i ->
            val p = perfiles.getValue(i.perfilId)
            lineas += "item|${listas.getValue(i.listaId).nombre}|${p.nombre}|${p.tipo}|${i.posicion}|${i.prioridad}|${i.anadidoEn}"
        }
        return lineas.sorted()
    }

    /**
     * Cada fila con su id ("perfil:3" → datos de la fila con sus géneros e idiomas): para comprobar que una fusión no
     * borra ni modifica nada de lo que ya había (CU-22 postcondición, T8.36).
     */
    suspend fun filas(db: CineDatabase): Map<String, String> {
        val dao = db.copiaDao()
        val generos = dao.perfilGeneros().groupBy({ it.perfilId }, { it.generoId }).mapValues { it.value.sorted() }
        val idiomas = dao.perfilIdiomas().groupBy({ it.perfilId }, { it.idiomaId }).mapValues { it.value.sorted() }
        return buildMap {
            db.sagaDao().todas().forEach { put("saga:${it.id}", it.toString()) }
            dao.perfiles().forEach { put("perfil:${it.id}", "$it ${generos[it.id]} ${idiomas[it.id]}") }
            dao.listas().forEach { put("lista:${it.id}", it.toString()) }
            dao.items().forEach { put("item:${it.listaId}:${it.perfilId}", it.toString()) }
        }
    }
}

/** Copia con los mismos datos que [FixtureGrande] (1000 perfiles, 30 listas, 5000 elementos), sin pasar por la base de datos. */
fun FixtureGrande.comoCopia(): CopiaSeguridad {
    val porLista = elementos.groupBy({ it.first }, { it.second })
    return CopiaSeguridad(
        fecha = "2026-11-09T12:00:00",
        sagas = perfiles.mapNotNull { it.saga }.distinct(),
        perfiles =
            perfiles.mapIndexed { i, p ->
                PerfilCopia(
                    nombre = p.nombre,
                    tipo = p.tipo,
                    estado = p.estado,
                    puntuacion = p.decimas?.let { ReglasCopia.valor(Puntuacion.of(it)) },
                    saga = p.saga,
                    generos = p.generos,
                    idiomas = p.idiomas,
                    creadoEn = i.toLong(),
                )
            },
        listas =
            listas.mapIndexed { li, (nombre, tipo, restriccion) ->
                ListaCopia(
                    nombre = nombre,
                    tipo = tipo,
                    restriccion = restriccion,
                    creadoEn = li.toLong(),
                    elementos =
                        porLista[li].orEmpty().mapIndexed { i, pi ->
                            ElementoCopia(
                                nombre = perfiles[pi].nombre,
                                tipo = perfiles[pi].tipo,
                                posicion = (i + 1).takeIf { tipo == TipoLista.RANKING },
                                prioridad = Prioridad.entries[pi % Prioridad.entries.size].takeIf { tipo == TipoLista.PRIORIDAD },
                                anadidoEn = i.toLong(),
                            )
                        },
                )
            },
    )
}

/** Archivos de copia de referencia del repositorio (`src/test/resources/copias`), por versión del formato. */
object CopiasReferencia {
    fun texto(version: Int): String =
        requireNotNull(javaClass.classLoader!!.getResource("copias/cinetrack_backup_v$version.json")) { "Falta la copia v$version" }
            .readText(Charsets.UTF_8)
}
