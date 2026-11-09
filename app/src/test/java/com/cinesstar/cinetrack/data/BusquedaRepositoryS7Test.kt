package com.cinesstar.cinetrack.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.data.repository.BusquedaRepository
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.domain.Ambito
import com.cinesstar.cinetrack.domain.CriteriosBusqueda
import com.cinesstar.cinetrack.domain.CriteriosBusqueda.Companion.conRango
import com.cinesstar.cinetrack.domain.DominioException
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.soporte.DatosFixture
import com.cinesstar.cinetrack.soporte.FixturePequeno
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests D del S7 (CU-20): búsqueda combinada sobre Room en memoria, con el fixture pequeño cuando aplica. */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class BusquedaRepositoryS7Test {
    private var ahora = 1_000L
    private val db = CineDatabase.construir(ApplicationProvider.getApplicationContext(), nombre = null)
    private val perfiles = PerfilRepository(db, reloj = { ahora++ })
    private val busqueda = BusquedaRepository(db)

    @After
    fun cerrar() = db.close()

    private fun prueba(bloque: suspend () -> Unit) = runBlocking { bloque() }

    private suspend fun fixture(): DatosFixture = FixturePequeno.cargar(db, reloj = { ahora++ })

    private fun DatosFixture.id(
        nombre: String,
        tipo: TipoPerfil = FixturePequeno.perfiles.first { it.nombre == nombre }.tipo,
    ): Long = perfiles.getValue("$nombre|$tipo")

    private fun DatosFixture.ids(vararg nombres: Pair<String, TipoPerfil>): Set<Long> = nombres.map { (n, t) -> id(n, t) }.toSet()

    private suspend fun buscar(criterios: CriteriosBusqueda): Set<Long> = busqueda.buscar(criterios).map { it.id }.toSet()

    private suspend fun crear(
        nombre: String,
        decimas: Int? = null,
    ): Long = perfiles.crear(NuevoPerfil(nombre, TipoPerfil.PELICULA, puntuacion = decimas?.let { Puntuacion.of(it) }))

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

    private fun nota(decimas: Int) = Puntuacion.of(decimas)

    private val p = TipoPerfil.PELICULA
    private val s = TipoPerfil.SERIE

    @Test
    fun t7_01_nombreSinDistinguirMayusculasNiTildes() =
        prueba {
            val padrino = crear("El Padrino")
            crear("Dune")
            listOf("padrino", "PADRINO", "pádrino", "  El   PÁDRINO ").forEach { texto ->
                assertEquals("\"$texto\"", setOf(padrino), buscar(CriteriosBusqueda(nombre = texto)))
            }
        }

    @Test
    fun t7_02_nombreParcial() =
        prueba {
            val f = fixture()
            assertEquals(
                f.ids("Dune" to p, "Dune: Parte Dos" to p, "Dune" to s),
                buscar(CriteriosBusqueda(nombre = "dun")),
            )
        }

    @Test
    fun t7_03_puntuacionMinima7() =
        prueba {
            val f = fixture()
            assertEquals(
                "Solo ≥ 7.0 y sin los perfiles sin nota",
                f.ids("Pádrino" to p, "EL padrino" to p, "Dune" to p, "Amélie" to p),
                buscar(CriteriosBusqueda(minimo = nota(70))),
            )
        }

    @Test
    fun t7_04_puntuacionMaxima5() =
        prueba {
            val f = fixture()
            assertEquals(
                f.ids("Dark" to s, "La casa de papel" to s, "El juego del calamar" to s, "Dune" to s, "Cosmos" to s),
                buscar(CriteriosBusqueda(maximo = nota(50))),
            )
        }

    @Test
    fun t7_05_rangoInclusivoEnLosExtremos() =
        prueba {
            val siete = crear("Siete", 70)
            val diez = crear("Diez", 100)
            crear("Seis nueve", 69)
            crear("Sin nota")
            val esperado = setOf(siete, diez)
            assertEquals(esperado, buscar(CriteriosBusqueda(minimo = nota(70), maximo = nota(100))))
            // Como lo deja el slider: 10 es "sin máximo", el rango sigue activo por el mínimo.
            assertEquals(esperado, buscar(CriteriosBusqueda().conRango(nota(70), nota(100))))
        }

    @Test
    fun t7_06_conRangoActivoLosPerfilesSinNotaQuedanFuera() =
        prueba {
            val f = fixture()
            val sinNota = f.ids("Dune: Parte Dos" to p, "Ñandú y compañía" to s)
            listOf(
                CriteriosBusqueda(minimo = nota(0)),
                CriteriosBusqueda(maximo = nota(99)),
                CriteriosBusqueda(minimo = nota(0), maximo = nota(100)),
                CriteriosBusqueda().conRango(nota(1), null),
            ).forEach { criterios ->
                val encontrados = buscar(criterios)
                assertTrue("$criterios no debe incluir los sin nota", encontrados.none { it in sinNota })
            }
        }

    @Test
    fun t7_07_sinRangoActivoLosPerfilesSinNotaAparecen() =
        prueba {
            val f = fixture()
            val sinNota = f.ids("Dune: Parte Dos" to p, "Ñandú y compañía" to s)
            val criterios = CriteriosBusqueda().conRango(nota(0), nota(100))
            assertTrue("0.0–10 no activa el rango", !criterios.rangoActivo)
            assertTrue(buscar(criterios).containsAll(sinNota))
            assertEquals(12, buscar(criterios).size)
        }

    @Test
    fun t7_08_generosUnoYVariosConOr() =
        prueba {
            val f = fixture()
            val drama = generoId("Drama")
            val comedia = generoId("Comedia")
            assertEquals(
                f.ids("Pádrino" to p, "EL padrino" to p, "El juego del calamar" to s),
                buscar(CriteriosBusqueda(generoIds = setOf(drama))),
            )
            assertEquals(
                "Varios géneros: basta uno (S-19)",
                f.ids("Pádrino" to p, "EL padrino" to p, "El juego del calamar" to s, "Amélie" to p),
                buscar(CriteriosBusqueda(generoIds = setOf(drama, comedia))),
            )
        }

    @Test
    fun t7_09_idiomaYSaga() =
        prueba {
            val f = fixture()
            assertEquals(f.ids("El viaje de Chihiro" to p), buscar(CriteriosBusqueda(idiomaId = idiomaId("Japonés"))))
            assertEquals(
                f.ids("Dune" to p, "El viaje de Chihiro" to p, "La casa de papel" to s, "Cosmos" to s),
                buscar(CriteriosBusqueda(idiomaId = idiomaId("Español"))),
            )
            assertEquals(
                f.ids("Dune" to p, "Dune: Parte Dos" to p),
                buscar(CriteriosBusqueda(sagaId = f.sagas.getValue(FixturePequeno.SAGA_DUNE))),
            )
        }

    @Test
    fun t7_10_criteriosCombinadosConAnd() =
        prueba {
            val f = fixture()
            val criterios =
                CriteriosBusqueda(
                    nombre = "dune",
                    generoIds = setOf(generoId("Ciencia Ficción")),
                    minimo = nota(50),
                    ambito = Ambito.PELICULAS,
                )
            // "Dune: Parte Dos" no tiene nota y la serie "Dune" no es película ni llega a 5.0.
            assertEquals(f.ids("Dune" to p), buscar(criterios))
        }

    @Test
    fun t7_11_tipo() =
        prueba {
            val f = fixture()
            val peliculas =
                FixturePequeno.perfiles
                    .filter { it.tipo == p }
                    .map { f.id(it.nombre, p) }
                    .toSet()
            val series =
                FixturePequeno.perfiles
                    .filter { it.tipo == s }
                    .map { f.id(it.nombre, s) }
                    .toSet()
            assertEquals("Por defecto, ambas", Ambito.TODO, CriteriosBusqueda().ambito)
            assertEquals(peliculas + series, buscar(CriteriosBusqueda()))
            assertEquals(peliculas, buscar(CriteriosBusqueda(ambito = Ambito.PELICULAS)))
            assertEquals(series, buscar(CriteriosBusqueda(ambito = Ambito.SERIES)))
        }

    @Test
    fun t7_12_estadoUnoYVarios() =
        prueba {
            val f = fixture()
            val pendientes = f.ids("Dune: Parte Dos" to p, "El juego del calamar" to s, "Dune" to s)
            val viendo = f.ids("Dune" to p, "El viaje de Chihiro" to p, "La casa de papel" to s, "Ñandú y compañía" to s)
            assertEquals(pendientes, buscar(CriteriosBusqueda(estados = setOf(Estado.PENDIENTE))))
            assertEquals(pendientes + viendo, buscar(CriteriosBusqueda(estados = setOf(Estado.PENDIENTE, Estado.VIENDO))))
        }

    @Test
    fun t7_13_sinCriteriosDevuelveTodosMasRecientesPrimero() =
        prueba {
            fixture()
            val resultado = busqueda.buscar(CriteriosBusqueda())
            assertEquals(12, resultado.size)
            assertEquals(resultado.sortedByDescending { it.creadoEn }, resultado)
            // La tarjeta muestra saga y géneros: vienen con el resultado.
            val padrino = resultado.single { it.nombre == "Pádrino" }
            assertEquals(FixturePequeno.SAGA_PADRINO, padrino.saga?.nombre)
            assertEquals(listOf("Drama", "Crimen"), padrino.generos.map { it.nombre })
        }

    @Test
    fun t7_15_minimoMayorQueMaximoNoBuscaYDaM14() =
        prueba {
            fixture()
            try {
                busqueda.buscar(CriteriosBusqueda(minimo = nota(80), maximo = nota(50)))
                fail("Debía lanzar M-14")
            } catch (e: DominioException) {
                assertEquals(ErrorDominio.RangoInvertido, e.error)
                assertEquals("M-14", e.error.codigo)
            }
        }

    @Test
    fun t7_17_caracteresEspecialesComoTextoLiteral() =
        prueba {
            val porcentaje = crear("100% Real")
            val guionBajo = crear("Mi_casa")
            val apostrofo = crear("L'Avventura")
            val comillas = crear("Dicen \"hola\"")
            val barra = crear("Barra \\ invertida")
            crear("100 Real")
            crear("MiXcasa")
            crear("Comodines")
            assertEquals(setOf(porcentaje), buscar(CriteriosBusqueda(nombre = "%")))
            assertEquals(setOf(porcentaje), buscar(CriteriosBusqueda(nombre = "0% r")))
            assertEquals(setOf(guionBajo), buscar(CriteriosBusqueda(nombre = "_")))
            assertEquals(setOf(apostrofo), buscar(CriteriosBusqueda(nombre = "'")))
            assertEquals(setOf(comillas), buscar(CriteriosBusqueda(nombre = "\"")))
            assertEquals(setOf(barra), buscar(CriteriosBusqueda(nombre = "\\")))
            assertEquals(emptySet<Long>(), buscar(CriteriosBusqueda(nombre = "%%")))
            assertEquals(emptySet<Long>(), buscar(CriteriosBusqueda(nombre = "'; DROP TABLE perfil; --")))
            assertEquals("La tabla sigue intacta", 8, db.perfilDao().contar())
        }
}
