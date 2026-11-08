package com.cinesstar.cinetrack.soporte

import com.cinesstar.cinetrack.data.repository.ListaRepository
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.repository.SagaRepository
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import kotlin.random.Random

/** Ids creados por un fixture, por nombre. */
data class DatosFixture(
    val perfiles: Map<String, Long>,
    val sagas: Map<String, Long>,
    val listas: Map<String, Long>,
)

/** Descripción de un perfil del fixture, independiente de la base de datos. */
data class PerfilFixture(
    val nombre: String,
    val tipo: TipoPerfil,
    val estado: Estado,
    val decimas: Int?,
    val generos: List<String>,
    val idiomas: List<String>,
    val saga: String? = null,
)

/**
 * Fixture pequeño (sprints.md §1.4): 12 perfiles (6 películas, 6 series) con todas las combinaciones de estado, las
 * notas límite 0.0, 2.9, 3.0, 4.9, 5.0, 6.9, 7.0, 8.9, 9.0, 10 y sin nota, géneros e idiomas múltiples, 2 sagas,
 * nombres con tildes y mayúsculas mezcladas, y 3 listas (una de cada tipo).
 */
object FixturePequeno {
    const val SAGA_PADRINO = "El Padrino"
    const val SAGA_DUNE = "Dune"
    const val LISTA_NORMAL = "Para el finde"
    const val LISTA_PRIORIDAD = "Próximas a ver"
    const val LISTA_RANKING = "Top Histórico"

    val perfiles =
        listOf(
            PerfilFixture(
                "Pádrino",
                TipoPerfil.PELICULA,
                Estado.VISTO,
                100,
                listOf("Drama", "Crimen"),
                listOf("Inglés", "Italiano"),
                SAGA_PADRINO,
            ),
            PerfilFixture("EL padrino", TipoPerfil.PELICULA, Estado.VISTO, 90, listOf("Drama"), listOf("Inglés"), SAGA_PADRINO),
            PerfilFixture(
                "Dune",
                TipoPerfil.PELICULA,
                Estado.VIENDO,
                89,
                listOf("Ciencia Ficción", "Aventura"),
                listOf("Inglés", "Español"),
                SAGA_DUNE,
            ),
            PerfilFixture(
                "Dune: Parte Dos",
                TipoPerfil.PELICULA,
                Estado.PENDIENTE,
                null,
                listOf("Ciencia Ficción"),
                listOf("Inglés"),
                SAGA_DUNE,
            ),
            PerfilFixture("Amélie", TipoPerfil.PELICULA, Estado.VISTO, 70, listOf("Comedia", "Romance"), listOf("Francés")),
            PerfilFixture(
                "El viaje de Chihiro",
                TipoPerfil.PELICULA,
                Estado.VIENDO,
                69,
                listOf("Animación", "Fantasía"),
                listOf("Japonés", "Español"),
            ),
            PerfilFixture("Dark", TipoPerfil.SERIE, Estado.VISTO, 50, listOf("Ciencia Ficción", "Suspense"), listOf("Alemán")),
            PerfilFixture("La casa de papel", TipoPerfil.SERIE, Estado.VIENDO, 49, listOf("Crimen", "Thriller"), listOf("Español")),
            PerfilFixture(
                "El juego del calamar",
                TipoPerfil.SERIE,
                Estado.PENDIENTE,
                30,
                listOf("Thriller", "Drama"),
                listOf("Coreano", "Inglés"),
            ),
            PerfilFixture("Dune", TipoPerfil.SERIE, Estado.PENDIENTE, 29, listOf("Ciencia Ficción"), listOf("Inglés")),
            PerfilFixture(
                "Cosmos",
                TipoPerfil.SERIE,
                Estado.VISTO,
                0,
                listOf("Documental", "Historia"),
                listOf("Inglés", "Español", "Portugués"),
            ),
            PerfilFixture("Ñandú y compañía", TipoPerfil.SERIE, Estado.VIENDO, null, emptyList(), emptyList()),
        )

    /** Clave única de un perfil del fixture: hay una película y una serie llamadas "Dune". */
    fun clave(p: PerfilFixture) = "${p.nombre}|${p.tipo}"

    suspend fun cargar(
        db: CineDatabase,
        reloj: () -> Long = System::currentTimeMillis,
    ): DatosFixture {
        val perfilesRepo = PerfilRepository(db, reloj = reloj)
        val sagasRepo = SagaRepository(db)
        val listasRepo = ListaRepository(db, reloj = reloj)
        val generos = db.catalogoDao().generos().associate { it.nombre to it.id }
        val idiomas = db.catalogoDao().idiomas().associate { it.nombre to it.id }
        val sagas = listOf(SAGA_PADRINO, SAGA_DUNE).associateWith { sagasRepo.crear(it) }
        val ids =
            perfiles.associate { p ->
                clave(p) to
                    perfilesRepo.crear(
                        NuevoPerfil(
                            nombre = p.nombre,
                            tipo = p.tipo,
                            estado = p.estado,
                            puntuacion = p.decimas?.let { Puntuacion.of(it) },
                            sagaId = p.saga?.let { sagas.getValue(it) },
                            generoIds = p.generos.map { generos.getValue(it) }.toSet(),
                            idiomaIds = p.idiomas.map { idiomas.getValue(it) }.toSet(),
                        ),
                    )
            }
        val listas =
            mapOf(
                LISTA_NORMAL to listasRepo.crear(LISTA_NORMAL, TipoLista.NORMAL),
                LISTA_PRIORIDAD to listasRepo.crear(LISTA_PRIORIDAD, TipoLista.PRIORIDAD, Restriccion.SOLO_SERIES),
                LISTA_RANKING to listasRepo.crear(LISTA_RANKING, TipoLista.RANKING, Restriccion.SOLO_PELICULAS),
            )
        perfiles.filter { it.tipo == TipoPerfil.PELICULA }.forEach {
            listasRepo.anadir(
                listas.getValue(LISTA_RANKING),
                ids.getValue(clave(it)),
            )
        }
        perfiles.filter { it.tipo == TipoPerfil.SERIE }.forEachIndexed { i, p ->
            listasRepo.anadir(listas.getValue(LISTA_PRIORIDAD), ids.getValue(clave(p)), prioridad = Prioridad.entries[i % 3])
        }
        perfiles.take(4).forEach { listasRepo.anadir(listas.getValue(LISTA_NORMAL), ids.getValue(clave(it))) }
        return DatosFixture(ids, sagas, listas)
    }
}

/**
 * Fixture grande (sprints.md §1.4): generador determinista de 1000 perfiles, 30 listas y 5000 elementos de lista,
 * para pruebas de rendimiento. Con la misma semilla genera siempre los mismos datos.
 */
class FixtureGrande(
    semilla: Int = 20261108,
) {
    private val azar = Random(semilla)

    val perfiles: List<PerfilFixture>
    val listas: List<Triple<String, TipoLista, Restriccion>>

    /** Elementos (índice de lista, índice de perfil) sin repetidos y respetando la restricción de cada lista. */
    val elementos: List<Pair<Int, Int>>

    init {
        val generos = com.cinesstar.cinetrack.domain.GENEROS_PREDEFINIDOS
        val idiomas = com.cinesstar.cinetrack.domain.IDIOMAS_PREDEFINIDOS
        perfiles =
            (0 until PERFILES).map { i ->
                PerfilFixture(
                    nombre = "Título ${i.toString().padStart(4, '0')} ${PALABRAS[azar.nextInt(PALABRAS.size)]}",
                    tipo = if (azar.nextBoolean()) TipoPerfil.PELICULA else TipoPerfil.SERIE,
                    estado = Estado.entries[azar.nextInt(Estado.entries.size)],
                    decimas = if (azar.nextInt(10) == 0) null else azar.nextInt(101),
                    generos = generos.shuffled(azar).take(1 + azar.nextInt(3)),
                    idiomas = idiomas.shuffled(azar).take(1 + azar.nextInt(2)),
                    saga = if (azar.nextInt(5) == 0) "Saga ${azar.nextInt(SAGAS)}" else null,
                )
            }
        listas =
            (0 until LISTAS).map { i ->
                Triple("Lista $i", TipoLista.entries[i % 3], Restriccion.entries[(i / 3) % 3])
            }
        val porLista = ELEMENTOS / LISTAS
        val sobrante = ELEMENTOS % LISTAS
        elementos =
            listas.flatMapIndexed { li, (_, _, restriccion) ->
                val candidatos = perfiles.indices.filter { restriccion.admite(perfiles[it].tipo) }.shuffled(azar)
                candidatos.take(porLista + if (li < sobrante) 1 else 0).map { li to it }
            }
    }

    suspend fun cargar(db: CineDatabase): DatosFixture {
        val perfilesRepo = PerfilRepository(db)
        val sagasRepo = SagaRepository(db)
        val listasRepo = ListaRepository(db)
        val generos = db.catalogoDao().generos().associate { it.nombre to it.id }
        val idiomas = db.catalogoDao().idiomas().associate { it.nombre to it.id }
        val sagas = perfiles.mapNotNull { it.saga }.distinct().associateWith { sagasRepo.obtenerOCrear(it) }
        val idsPerfil =
            perfiles.map { p ->
                perfilesRepo.crear(
                    NuevoPerfil(
                        nombre = p.nombre,
                        tipo = p.tipo,
                        estado = p.estado,
                        puntuacion = p.decimas?.let { Puntuacion.of(it) },
                        sagaId = p.saga?.let { sagas.getValue(it) },
                        generoIds = p.generos.map { generos.getValue(it) }.toSet(),
                        idiomaIds = p.idiomas.map { idiomas.getValue(it) }.toSet(),
                    ),
                )
            }
        val idsLista = listas.map { (nombre, tipo, restriccion) -> listasRepo.crear(nombre, tipo, restriccion) }
        elementos.forEach { (li, pi) ->
            val prioridad = if (listas[li].second == TipoLista.PRIORIDAD) Prioridad.entries[pi % 3] else null
            listasRepo.anadir(idsLista[li], idsPerfil[pi], prioridad = prioridad)
        }
        return DatosFixture(
            perfiles = perfiles.mapIndexed { i, p -> "${p.nombre}|${p.tipo}" to idsPerfil[i] }.toMap(),
            sagas = sagas,
            listas = listas.mapIndexed { i, l -> l.first to idsLista[i] }.toMap(),
        )
    }

    companion object {
        const val PERFILES = 1000
        const val LISTAS = 30
        const val ELEMENTOS = 5000
        const val SAGAS = 40
        private val PALABRAS = listOf("Noche", "Ciudad", "Océano", "Último", "Sueño", "Fuego", "Camino", "Sombra", "Reino", "Viaje")
    }
}
