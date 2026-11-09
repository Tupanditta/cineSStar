package com.cinesstar.cinetrack.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.data.repository.BibliotecaRepository
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.domain.Ambito
import com.cinesstar.cinetrack.domain.ClaveGrupo
import com.cinesstar.cinetrack.domain.CriterioAgrupacion
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Grupo
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.RangoPuntuacion
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.soporte.DatosFixture
import com.cinesstar.cinetrack.soporte.FixturePequeno
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests D del S4 (T4.09–T4.15): agrupación de CU-09 sobre Room en memoria con el fixture pequeño. */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class BibliotecaRepositoryS4Test {
    private var ahora = 1_000L
    private val db = CineDatabase.construir(ApplicationProvider.getApplicationContext(), nombre = null)
    private val perfiles = PerfilRepository(db, reloj = { ahora++ })
    private val biblioteca = BibliotecaRepository(db)

    @After
    fun cerrar() = db.close()

    private fun prueba(bloque: suspend () -> Unit) = runBlocking { bloque() }

    private suspend fun fixture(): DatosFixture = FixturePequeno.cargar(db, reloj = { ahora++ })

    /** Ids del fixture por nombre; "Dune" es la película y "Dune (serie)" la serie. */
    private fun DatosFixture.id(
        nombre: String,
        tipo: TipoPerfil = FixturePequeno.perfiles.first { it.nombre == nombre }.tipo,
    ): Long = perfiles.getValue("$nombre|$tipo")

    private fun List<Grupo>.ids(clave: ClaveGrupo): Set<Long> = single { it.clave == clave }.perfiles.map { it.id }.toSet()

    private suspend fun generoId(nombre: String) =
        db
            .catalogoDao()
            .generos()
            .single { it.nombre == nombre }
            .id

    private suspend fun idiomaId(nombre: String) =
        db
            .catalogoDao()
            .idiomas()
            .single { it.nombre == nombre }
            .id

    @Test
    fun t4_09_agruparPorPuntuacionConLosValoresLimite() =
        prueba {
            val ids =
                listOf(0, 29, 30, 49, 50, 69, 70, 89, 90, 100, null).associateWith { decimas ->
                    perfiles.crear(NuevoPerfil("Nota $decimas", TipoPerfil.PELICULA, puntuacion = decimas?.let { Puntuacion.of(it) }))
                }

            val grupos = biblioteca.agrupar(CriterioAgrupacion.PUNTUACION)

            assertEquals(
                "Seis grupos como máximo, de mayor a menor y 'Sin puntuación' al final",
                listOf(
                    RangoPuntuacion.DE_9_A_10,
                    RangoPuntuacion.DE_7_A_8_9,
                    RangoPuntuacion.DE_5_A_6_9,
                    RangoPuntuacion.DE_3_A_4_9,
                    RangoPuntuacion.DE_0_A_2_9,
                    RangoPuntuacion.SIN_PUNTUACION,
                ).map { ClaveGrupo.Rango(it) },
                grupos.map { it.clave },
            )

            fun rango(r: RangoPuntuacion) = grupos.ids(ClaveGrupo.Rango(r))
            assertEquals(setOf(ids[0], ids[29]), rango(RangoPuntuacion.DE_0_A_2_9))
            assertEquals(setOf(ids[30], ids[49]), rango(RangoPuntuacion.DE_3_A_4_9))
            assertEquals(setOf(ids[50], ids[69]), rango(RangoPuntuacion.DE_5_A_6_9))
            assertEquals(setOf(ids[70], ids[89]), rango(RangoPuntuacion.DE_7_A_8_9))
            assertEquals(setOf(ids[90], ids[100]), rango(RangoPuntuacion.DE_9_A_10))
            assertEquals(setOf(ids[null]), rango(RangoPuntuacion.SIN_PUNTUACION))
        }

    @Test
    fun t4_09_agruparElFixturePorPuntuacion() =
        prueba {
            val f = fixture()
            val grupos = biblioteca.agrupar(CriterioAgrupacion.PUNTUACION)
            assertEquals(6, grupos.size)
            assertEquals(setOf(f.id("Pádrino"), f.id("EL padrino")), grupos.ids(ClaveGrupo.Rango(RangoPuntuacion.DE_9_A_10)))
            assertEquals(
                setOf(f.id("Dune", TipoPerfil.SERIE), f.id("Cosmos")),
                grupos.ids(ClaveGrupo.Rango(RangoPuntuacion.DE_0_A_2_9)),
            )
            assertEquals(
                setOf(f.id("Dune: Parte Dos"), f.id("Ñandú y compañía")),
                grupos.ids(ClaveGrupo.Rango(RangoPuntuacion.SIN_PUNTUACION)),
            )
            // Dentro de cada grupo, más recientes primero (S-10).
            grupos.forEach { g -> assertEquals(g.perfiles.sortedByDescending { it.creadoEn }, g.perfiles) }
        }

    @Test
    fun t4_10_unPerfilConTresGenerosApareceEnLosTres() =
        prueba {
            val generos = listOf("Drama", "Terror", "Comedia")
            val id = perfiles.crear(NuevoPerfil("Tres géneros", TipoPerfil.PELICULA, generoIds = generos.map { generoId(it) }.toSet()))

            val grupos = biblioteca.agrupar(CriterioAgrupacion.GENERO)

            assertEquals(
                "En el orden del catálogo",
                listOf("Drama", "Comedia", "Terror"),
                grupos.map { (it.clave as ClaveGrupo.Genero).nombre },
            )
            grupos.forEach { assertEquals(listOf(id), it.perfiles.map { p -> p.id }) }
        }

    @Test
    fun t4_11_unPerfilConDosIdiomasApareceEnLosDos() =
        prueba {
            val f = fixture()
            val dune = f.id("Dune", TipoPerfil.PELICULA)
            val grupos = biblioteca.agrupar(CriterioAgrupacion.IDIOMA)
            val conDune = grupos.filter { g -> g.perfiles.any { it.id == dune } }.map { (it.clave as ClaveGrupo.Idioma).nombre }
            assertEquals(listOf("Español", "Inglés"), conDune)
            assertEquals(
                setOf(f.id("Cosmos"), f.id("Dune", TipoPerfil.PELICULA), f.id("El viaje de Chihiro"), f.id("La casa de papel")),
                grupos.ids(ClaveGrupo.Idioma(idiomaId("Español"), "Español", 0)),
            )
        }

    @Test
    fun t4_12_perfilesSinGeneroIdiomaOSagaVanAlGrupoSin() =
        prueba {
            val f = fixture()
            val nandu = f.id("Ñandú y compañía")
            val porGenero = biblioteca.agrupar(CriterioAgrupacion.GENERO)
            val porIdioma = biblioteca.agrupar(CriterioAgrupacion.IDIOMA)
            val porSaga = biblioteca.agrupar(CriterioAgrupacion.SAGA)

            assertEquals(ClaveGrupo.SinGenero, porGenero.last().clave)
            assertEquals(setOf(nandu), porGenero.ids(ClaveGrupo.SinGenero))
            assertEquals(ClaveGrupo.SinIdioma, porIdioma.last().clave)
            assertEquals(setOf(nandu), porIdioma.ids(ClaveGrupo.SinIdioma))
            assertEquals(ClaveGrupo.SinSaga, porSaga.last().clave)
            assertEquals(FixturePequeno.perfiles.count { it.saga == null }, porSaga.last().total)
            assertTrue(nandu in porSaga.ids(ClaveGrupo.SinSaga))
            assertEquals(
                "Sagas por nombre",
                listOf(FixturePequeno.SAGA_DUNE, FixturePequeno.SAGA_PADRINO),
                porSaga.dropLast(1).map { (it.clave as ClaveGrupo.Saga).nombre },
            )
        }

    @Test
    fun t4_13_agruparPorEstado() =
        prueba {
            val f = fixture()
            val grupos = biblioteca.agrupar(CriterioAgrupacion.ESTADO)
            assertEquals(Estado.entries.map { ClaveGrupo.DeEstado(it) }, grupos.map { it.clave })
            Estado.entries.forEach { estado ->
                assertEquals(
                    FixturePequeno.perfiles
                        .filter { it.estado == estado }
                        .map { f.perfiles.getValue(FixturePequeno.clave(it)) }
                        .toSet(),
                    grupos.ids(ClaveGrupo.DeEstado(estado)),
                )
            }
        }

    @Test
    fun t4_14_cadaCriterioConCadaAmbito() =
        prueba {
            val f = fixture()
            Ambito.entries.forEach { ambito ->
                val esperados =
                    FixturePequeno.perfiles
                        .filter { ambito.admite(it.tipo) }
                        .map { f.perfiles.getValue(FixturePequeno.clave(it)) }
                        .toSet()
                CriterioAgrupacion.entries.forEach { criterio ->
                    val grupos = biblioteca.agrupar(criterio, ambito)
                    val vistos = grupos.flatMap { g -> g.perfiles.map { it.id } }.toSet()
                    assertEquals("$criterio / $ambito", esperados, vistos)
                    grupos.forEach { g -> g.perfiles.forEach { assertTrue("$criterio / $ambito", ambito.admite(it.tipo)) } }
                }
            }
        }

    @Test
    fun t4_15_losGruposVaciosNoSeMuestran() =
        prueba {
            fixture()
            CriterioAgrupacion.entries.forEach { criterio ->
                Ambito.entries.forEach { ambito ->
                    biblioteca.agrupar(criterio, ambito).forEach { assertTrue("$criterio / $ambito: ${it.clave}", it.total > 0) }
                }
            }
            // Las películas del fixture no tienen notas entre 0 y 4.9: esos dos rangos no aparecen.
            assertEquals(
                listOf(RangoPuntuacion.DE_9_A_10, RangoPuntuacion.DE_7_A_8_9, RangoPuntuacion.DE_5_A_6_9, RangoPuntuacion.SIN_PUNTUACION)
                    .map { ClaveGrupo.Rango(it) },
                biblioteca.agrupar(CriterioAgrupacion.PUNTUACION, Ambito.PELICULAS).map { it.clave },
            )
            // Géneros del catálogo que nadie usa ("Acción", "Terror") no forman grupo.
            val generos = biblioteca.agrupar(CriterioAgrupacion.GENERO).map { it.clave }
            assertTrue(generos.none { it is ClaveGrupo.Genero && it.nombre in setOf("Acción", "Terror") })
        }

    @Test
    fun cu08_laBibliotecaLeeSagaGenerosEIdiomasMasRecientesPrimero() =
        prueba {
            val f = fixture()
            val todos = biblioteca.leer()
            assertEquals(FixturePequeno.perfiles.size, todos.size)
            assertEquals("Más recientes primero (S-10)", todos.sortedByDescending { it.creadoEn }, todos)
            val padrino = todos.single { it.id == f.id("Pádrino") }
            assertEquals(FixturePequeno.SAGA_PADRINO, padrino.saga?.nombre)
            assertEquals(listOf("Drama", "Crimen"), padrino.generos.map { it.nombre })
            assertEquals(listOf("Inglés", "Italiano"), padrino.idiomas.map { it.nombre })
        }
}
