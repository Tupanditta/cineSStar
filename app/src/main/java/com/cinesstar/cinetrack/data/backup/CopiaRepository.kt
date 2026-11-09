package com.cinesstar.cinetrack.data.backup

import androidx.room.withTransaction
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.data.room.ListaEntity
import com.cinesstar.cinetrack.data.room.ListaItemEntity
import com.cinesstar.cinetrack.data.room.PerfilEntity
import com.cinesstar.cinetrack.data.room.PerfilGeneroEntity
import com.cinesstar.cinetrack.data.room.PerfilIdiomaEntity
import com.cinesstar.cinetrack.data.room.SagaEntity
import com.cinesstar.cinetrack.domain.ClavePerfil
import com.cinesstar.cinetrack.domain.CopiaSeguridad
import com.cinesstar.cinetrack.domain.ElementoCopia
import com.cinesstar.cinetrack.domain.EstadoLocal
import com.cinesstar.cinetrack.domain.Fusion
import com.cinesstar.cinetrack.domain.ListaCopia
import com.cinesstar.cinetrack.domain.ListaLocal
import com.cinesstar.cinetrack.domain.Normalizador
import com.cinesstar.cinetrack.domain.PerfilCopia
import com.cinesstar.cinetrack.domain.PlanFusion
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.ReglasCopia
import com.cinesstar.cinetrack.domain.ResumenFusion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.domain.clave
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Datos de "Métricas de la base de datos" y "Distribución de Registros" (design.md §10). */
data class Metricas(
    val titulos: Int,
    val listas: Int,
    val peliculas: Int,
    val series: Int,
    /** Tamaño de la base de datos en disco; null si no está en un archivo (tests en memoria). */
    val bytes: Long?,
    /** Última escritura en la base de datos (milisegundos); null si no se conoce. */
    val ultimaModificacion: Long?,
)

/**
 * Copia de seguridad (CU-21) e importación por fusión (CU-22, S-20).
 *
 * [ficheroBd] es el archivo de la base de datos, para las métricas de almacenamiento. [interrupcion] se llama tras
 * crear cada perfil importado con el número de perfiles creados; solo sirve para simular un fallo a mitad de la fusión
 * en los tests (CU-22 E4).
 */
class CopiaRepository(
    private val db: CineDatabase,
    private val ficheroBd: File? = null,
    private val reloj: () -> Long = System::currentTimeMillis,
    private val interrupcion: suspend (Int) -> Unit = {},
) {
    private val dao = db.copiaDao()
    private val perfilDao = db.perfilDao()
    private val sagaDao = db.sagaDao()
    private val listaDao = db.listaDao()
    private val catalogoDao = db.catalogoDao()

    private companion object {
        val TABLAS = arrayOf("perfil", "saga", "perfil_genero", "perfil_idioma", "lista", "lista_item")
        val SUFIJOS_BD = listOf("", "-wal", "-shm")
    }

    /** CU-21 paso 3: todos los datos, leídos en una transacción para que la copia sea coherente. */
    suspend fun exportar(): CopiaSeguridad =
        db.withTransaction {
            val generos = catalogoDao.generos().associateBy { it.id }
            val idiomas = catalogoDao.idiomas().associateBy { it.id }
            val sagas = sagaDao.todas()
            val sagaPorId = sagas.associateBy { it.id }
            val generosDe =
                dao.perfilGeneros().groupBy({ it.perfilId }, { generos.getValue(it.generoId) }).mapValues { (_, g) ->
                    g.sortedBy { it.orden }.map { it.nombre }
                }
            val idiomasDe =
                dao.perfilIdiomas().groupBy({ it.perfilId }, { idiomas.getValue(it.idiomaId) }).mapValues { (_, i) ->
                    i.sortedBy { it.orden }.map { it.nombre }
                }
            val perfiles = dao.perfiles()
            val perfilPorId = perfiles.associateBy { it.id }
            val items = dao.items().groupBy { it.listaId }
            CopiaSeguridad(
                fecha = LocalDateTime.ofInstant(Instant.ofEpochMilli(reloj()), ZoneId.systemDefault()).toString(),
                sagas = sagas.map { it.nombre },
                perfiles =
                    perfiles.map { p ->
                        PerfilCopia(
                            nombre = p.nombre,
                            tipo = p.tipo,
                            estado = p.estado,
                            puntuacion = p.puntuacionDecimas?.let { ReglasCopia.valor(Puntuacion.of(it)) },
                            comentario = p.comentario,
                            comentarioEditadoEn = p.comentarioEditadoEn,
                            saga = p.sagaId?.let { sagaPorId[it]?.nombre },
                            generos = generosDe[p.id].orEmpty(),
                            idiomas = idiomasDe[p.id].orEmpty(),
                            creadoEn = p.creadoEn,
                        )
                    },
                listas =
                    dao.listas().map { l ->
                        ListaCopia(
                            nombre = l.nombre,
                            tipo = l.tipo,
                            restriccion = l.restriccion,
                            creadoEn = l.creadoEn,
                            elementos =
                                ordenar(l.tipo, items[l.id].orEmpty()).map { item ->
                                    val perfil = perfilPorId.getValue(item.perfilId)
                                    ElementoCopia(
                                        nombre = perfil.nombre,
                                        tipo = perfil.tipo,
                                        posicion = item.posicion.takeIf { l.tipo == TipoLista.RANKING },
                                        prioridad = item.prioridad.takeIf { l.tipo == TipoLista.PRIORIDAD },
                                        anadidoEn = item.anadidoEn,
                                    )
                                },
                        )
                    },
            )
        }

    /** Elementos en el orden de su lista: posición (ranking), prioridad y antigüedad, o antigüedad. */
    private fun ordenar(
        tipo: TipoLista,
        items: List<ListaItemEntity>,
    ): List<ListaItemEntity> =
        when (tipo) {
            TipoLista.RANKING -> items.sortedWith(compareBy({ it.posicion }, { it.anadidoEn }, { it.perfilId }))
            TipoLista.PRIORIDAD -> items.sortedWith(compareBy({ it.prioridad?.ordinal }, { it.anadidoEn }, { it.perfilId }))
            TipoLista.NORMAL -> items.sortedWith(compareBy({ it.anadidoEn }, { it.perfilId }))
        }

    /** CU-22 paso 3: resultado de la fusión sin tocar la base de datos. [copia] ya está validada (FormatoCopia.leer). */
    suspend fun planificar(copia: CopiaSeguridad): PlanFusion = db.withTransaction { Fusion.planificar(copia, estadoLocal()) }

    /**
     * CU-22 paso 5: aplica la fusión en una sola transacción. Si algo falla, no queda nada a medias y los datos previos
     * siguen intactos (E4). El plan se recalcula dentro de la transacción, así el resultado es el de los datos actuales.
     */
    suspend fun importar(copia: CopiaSeguridad): ResumenFusion =
        db.withTransaction {
            val local = estadoLocal()
            val plan = Fusion.planificar(copia, local)
            aplicar(plan, local)
            plan.resumen
        }

    private suspend fun estadoLocal(): EstadoLocal {
        val miembros = dao.items().groupBy({ it.listaId }, { it.perfilId })
        return EstadoLocal(
            perfiles = dao.perfiles().associate { ClavePerfil(it.nombreBusqueda, it.tipo) to it.id },
            sagas = sagaDao.todas().map { it.nombreBusqueda }.toSet(),
            listas =
                dao.listas().associate { l ->
                    l.nombreBusqueda to ListaLocal(l.id, l.nombre, l.tipo, l.restriccion, miembros[l.id].orEmpty().toSet())
                },
        )
    }

    private suspend fun aplicar(
        plan: PlanFusion,
        local: EstadoLocal,
    ) {
        plan.sagasNuevas.forEach { sagaDao.insertar(SagaEntity(nombre = it, nombreBusqueda = Normalizador.normalizar(it))) }
        val ids = HashMap(local.perfiles)
        crearPerfiles(plan.perfilesNuevos, ids)
        plan.listasNuevas.forEach { nueva ->
            val lista = nueva.lista
            val listaId =
                listaDao.insertar(
                    ListaEntity(
                        nombre = nueva.nombre,
                        nombreBusqueda = Normalizador.normalizar(nueva.nombre),
                        tipo = lista.tipo,
                        restriccion = lista.restriccion,
                        creadoEn = lista.creadoEn,
                    ),
                )
            Fusion.ordenCopia(lista).forEachIndexed { i, e ->
                listaDao.insertarItem(
                    item(
                        listaId,
                        lista.tipo,
                        ids.getValue(e.clave),
                        e,
                        i + 1,
                        e.anadidoEn,
                    ),
                )
            }
        }
        plan.ampliaciones.forEach { ampliacion ->
            val lista = ampliacion.lista
            val total = listaDao.contarItems(lista.id)
            // S-17: la lista vuelve a tener elementos; si se vacía otra vez, el aviso se mostrará de nuevo.
            if (total == 0 && listaDao.obtener(lista.id)?.avisoVaciaMostrado == true) listaDao.marcarAvisoVacia(lista.id, false)
            // Los ampliados van detrás de los que ya había: al final del ranking y como los más recientes del resto.
            val ahora = reloj()
            ampliacion.elementos.forEachIndexed { i, e ->
                listaDao.insertarItem(item(lista.id, lista.tipo, ids.getValue(e.clave), e, total + i + 1, ahora + i))
            }
        }
    }

    private suspend fun crearPerfiles(
        perfiles: List<PerfilCopia>,
        ids: MutableMap<ClavePerfil, Long>,
    ) {
        val generos = catalogoDao.generos().associate { Normalizador.normalizar(it.nombre) to it.id }
        val idiomas = catalogoDao.idiomas().associate { Normalizador.normalizar(it.nombre) to it.id }
        val sagas = sagaDao.todas().associate { it.nombreBusqueda to it.id }
        perfiles.forEachIndexed { i, p ->
            val nombre = Normalizador.limpiar(p.nombre)
            val comentario = p.comentario?.takeIf { it.isNotBlank() }
            val id =
                perfilDao.insertar(
                    PerfilEntity(
                        nombre = nombre,
                        nombreBusqueda = Normalizador.normalizar(nombre),
                        tipo = p.tipo,
                        estado = p.estado,
                        puntuacionDecimas = ReglasCopia.puntuacion(p)?.decimas,
                        comentario = comentario,
                        comentarioEditadoEn = comentario?.let { p.comentarioEditadoEn ?: reloj() },
                        sagaId = p.saga?.let { sagas[Normalizador.normalizar(it)] },
                        caratulaUri = null,
                        creadoEn = p.creadoEn,
                    ),
                )
            val generoIds = p.generos.mapNotNull { generos[Normalizador.normalizar(it)] }.distinct()
            val idiomaIds = p.idiomas.mapNotNull { idiomas[Normalizador.normalizar(it)] }.distinct()
            if (generoIds.isNotEmpty()) perfilDao.insertarGeneros(generoIds.map { PerfilGeneroEntity(id, it) })
            if (idiomaIds.isNotEmpty()) perfilDao.insertarIdiomas(idiomaIds.map { PerfilIdiomaEntity(id, it) })
            ids[p.clave] = id
            interrupcion(i + 1)
        }
    }

    /** Elemento de [tipo]: con [posicion] en un ranking y con la prioridad de la copia en una lista de prioridad. */
    @Suppress("LongParameterList")
    private fun item(
        listaId: Long,
        tipo: TipoLista,
        perfilId: Long,
        elemento: ElementoCopia,
        posicion: Int,
        anadidoEn: Long,
    ) = ListaItemEntity(
        listaId = listaId,
        perfilId = perfilId,
        posicion = posicion.takeIf { tipo == TipoLista.RANKING },
        prioridad = if (tipo == TipoLista.PRIORIDAD) elemento.prioridad ?: Prioridad.POR_DEFECTO else null,
        anadidoEn = anadidoEn,
    )

    /** Métricas de §10, que se vuelven a leer cada vez que cambian los datos. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observarMetricas(): Flow<Metricas> = db.invalidationTracker.createFlow(*TABLAS).mapLatest { leerMetricas() }

    suspend fun leerMetricas(): Metricas {
        val (totales, listas) = db.withTransaction { dao.totalesPorTipo().associate { it.tipo to it.total } to dao.contarListas() }
        val archivos = ficheroBd?.let { bd -> SUFIJOS_BD.map { File(bd.path + it) }.filter { it.exists() } }.orEmpty()
        val peliculas = totales[TipoPerfil.PELICULA] ?: 0
        val series = totales[TipoPerfil.SERIE] ?: 0
        return Metricas(
            titulos = peliculas + series,
            listas = listas,
            peliculas = peliculas,
            series = series,
            bytes = archivos.takeIf { it.isNotEmpty() }?.sumOf { it.length() },
            ultimaModificacion = archivos.maxOfOrNull { it.lastModified() }?.takeIf { it > 0 },
        )
    }
}
