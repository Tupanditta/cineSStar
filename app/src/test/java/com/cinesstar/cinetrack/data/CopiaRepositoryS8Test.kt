package com.cinesstar.cinetrack.data

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.data.backup.CopiaRepository
import com.cinesstar.cinetrack.domain.CopiaException
import com.cinesstar.cinetrack.domain.CopiaSeguridad
import com.cinesstar.cinetrack.domain.DetalleCopia
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.FormatoCopia
import com.cinesstar.cinetrack.domain.ListaCopia
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.ProblemaCopia
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.soporte.CopiasReferencia
import com.cinesstar.cinetrack.soporte.FixtureGrande
import com.cinesstar.cinetrack.soporte.FixturePequeno
import com.cinesstar.cinetrack.soporte.Instantanea
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests U y D del S8: formato de la copia, ida y vuelta, validación previa, fallos y versiones (CU-21, CU-22). */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class CopiaRepositoryS8Test : PruebaCopia() {
    // ---- CU-21 Exportar ----

    /** Fixture pequeño con un comentario de varias líneas y emoji, exportado como texto. */
    private suspend fun exportarFixture(): String {
        FixturePequeno.cargar(db, reloj)
        val id = db.perfilDao().buscarPorNombre("padrino", TipoPerfil.PELICULA)!!.id
        perfiles.comentar(id, "Obra maestra.\nCon salto de línea y emoji 🎬")
        return FormatoCopia.escribir(copias.exportar())
    }

    @Test
    fun t8_02_elJsonIncluyeVersionFechaSagasYTodosLosCamposDeCadaPerfil() =
        prueba {
            val texto = exportarFixture()
            val raiz = Json.parseToJsonElement(texto).jsonObject
            assertEquals("CineTrack", raiz["app"].toString().trim('"'))
            assertEquals(CopiaSeguridad.VERSION_ACTUAL.toString(), raiz["version"].toString())
            assertTrue(raiz["fecha"].toString().length > 2)
            val campos =
                setOf(
                    "nombre",
                    "tipo",
                    "estado",
                    "puntuacion",
                    "comentario",
                    "comentarioEditadoEn",
                    "saga",
                    "generos",
                    "idiomas",
                    "creadoEn",
                )
            raiz["perfiles"]!!.jsonArray.forEach { assertEquals(campos, (it as JsonObject).keys) }
            assertEquals(
                setOf(FixturePequeno.SAGA_PADRINO, FixturePequeno.SAGA_DUNE),
                raiz["sagas"]!!
                    .jsonArray
                    .map {
                        it.toString().trim('"')
                    }.toSet(),
            )
        }

    @Test
    fun t8_02_losPerfilesYListasExportadosConservanSusDatosPosicionesYPrioridades() =
        prueba {
            val texto = exportarFixture()
            val c = FormatoCopia.leer(texto)
            assertEquals(12, c.perfiles.size)
            val p = c.perfiles.first { it.nombre == "Pádrino" }
            assertEquals(10.0, p.puntuacion)
            assertEquals(Estado.VISTO, p.estado)
            assertEquals("Obra maestra.\nCon salto de línea y emoji 🎬", p.comentario)
            assertNotNull(p.comentarioEditadoEn)
            assertEquals(FixturePequeno.SAGA_PADRINO, p.saga)
            assertEquals(listOf("Drama", "Crimen"), p.generos)
            assertEquals(listOf("Inglés", "Italiano"), p.idiomas)
            assertNull(c.perfiles.first { it.nombre == "Dune: Parte Dos" }.puntuacion)

            val ranking = c.listas.first { it.nombre == FixturePequeno.LISTA_RANKING }
            assertEquals(TipoLista.RANKING, ranking.tipo)
            assertEquals(Restriccion.SOLO_PELICULAS, ranking.restriccion)
            assertEquals((1..6).toList(), ranking.elementos.map { it.posicion })
            assertTrue(ranking.elementos.all { it.prioridad == null })
            val prioridad = c.listas.first { it.nombre == FixturePequeno.LISTA_PRIORIDAD }
            assertEquals(
                listOf(
                    Prioridad.ALTA,
                    Prioridad.ALTA,
                    Prioridad.MEDIA,
                    Prioridad.MEDIA,
                    Prioridad.BAJA,
                    Prioridad.BAJA,
                ),
                prioridad.elementos.map {
                    it.prioridad
                },
            )
            assertEquals(
                4,
                c.listas
                    .first { it.nombre == FixturePequeno.LISTA_NORMAL }
                    .elementos.size,
            )
        }

    @Test
    fun t8_03_exportarSinDatosDaUnArchivoValidoYVacio() =
        prueba {
            val c = exportarYLeer()
            assertTrue(c.perfiles.isEmpty() && c.listas.isEmpty() && c.sagas.isEmpty())
            assertEquals(CopiaSeguridad.VERSION_ACTUAL, c.version)
        }

    // ---- CU-22 Importar: ida y vuelta ----

    @Test
    fun t8_06_idaYVueltaConElFixtureDaDatosIdenticos() =
        prueba {
            FixturePequeno.cargar(db, reloj)
            val id = db.perfilDao().buscarPorNombre("dark", TipoPerfil.SERIE)!!.id
            perfiles.comentar(id, "Muy buena.")
            listas.mover(listaId(FixturePequeno.LISTA_RANKING), db.perfilDao().buscarPorNombre("dune", TipoPerfil.PELICULA)!!.id, 1)
            val c = exportarYLeer()

            val vacia = nuevaBase()
            val resumen = CopiaRepository(vacia, reloj = reloj).importar(c)

            assertEquals(resumen(12, 0, 3, 0), resumen)
            assertEquals(Instantanea.contenido(db), Instantanea.contenido(vacia))
        }

    @Test
    fun t8_07_idaYVueltaConElFixtureGrande() =
        prueba {
            FixtureGrande().cargar(db)
            val c = exportarYLeer()
            assertEquals(FixtureGrande.PERFILES, c.perfiles.size)
            assertEquals(FixtureGrande.ELEMENTOS, c.listas.sumOf { it.elementos.size })

            val vacia = nuevaBase()
            CopiaRepository(vacia).importar(c)
            assertEquals(Instantanea.contenido(db), Instantanea.contenido(vacia))
        }

    // ---- CU-22 E3: validación previa ----

    private fun base() =
        copia(
            perfiles = listOf(perfil("Dune", puntuacion = 8.5), perfil("Dark", TipoPerfil.SERIE)),
            listas = listOf(ListaCopia("Pelis", TipoLista.NORMAL, Restriccion.SOLO_PELICULAS, 1L, listOf(elemento("Dune")))),
        )

    private suspend fun esperarNoValida(
        c: CopiaSeguridad,
        detalle: DetalleCopia,
    ) {
        val antes = Instantanea.filas(db)
        try {
            importar(c)
            fail("Se esperaba M-17 por $detalle")
        } catch (e: CopiaException) {
            assertEquals(ProblemaCopia.NoValida(detalle), e.problema)
        }
        assertEquals("No se importa nada", antes, Instantanea.filas(db))
    }

    @Test
    fun t8_12_contenidoInconsistenteDaM17ConDetalleYNoImportaNada() =
        prueba {
            FixturePequeno.cargar(db, reloj)
            val b = base()
            esperarNoValida(b.copy(perfiles = b.perfiles + perfil("Amélie", puntuacion = 7.25)), DetalleCopia.PuntuacionInvalida("Amélie"))
            esperarNoValida(b.copy(perfiles = b.perfiles + perfil("DUNE")), DetalleCopia.PerfilDuplicado("DUNE"))
            esperarNoValida(
                b.copy(
                    listas =
                        listOf(
                            ListaCopia(
                                "Pelis",
                                TipoLista.NORMAL,
                                Restriccion.SOLO_PELICULAS,
                                1L,
                                listOf(elemento("Dark", TipoPerfil.SERIE)),
                            ),
                        ),
                ),
                DetalleCopia.RestriccionLista("Pelis", "Dark"),
            )
            esperarNoValida(
                b.copy(
                    listas =
                        listOf(
                            ListaCopia("Pelis", TipoLista.NORMAL, Restriccion.MIXTO, 1L, listOf(elemento("Dune"), elemento("dune"))),
                        ),
                ),
                DetalleCopia.ElementoRepetido("Pelis", "dune"),
            )
            esperarNoValida(
                b.copy(listas = listOf(ListaCopia("Pelis", TipoLista.NORMAL, Restriccion.MIXTO, 1L, listOf(elemento("Matrix"))))),
                DetalleCopia.ElementoSinPerfil("Pelis", "Matrix"),
            )
            esperarNoValida(
                b.copy(perfiles = b.perfiles + perfil("Matrix").copy(generos = listOf("Neo-Noir"))),
                DetalleCopia.GeneroDesconocido("Matrix", "Neo-Noir"),
            )
            esperarNoValida(
                b.copy(perfiles = b.perfiles + perfil("Matrix").copy(idiomas = listOf("Klingon"))),
                DetalleCopia.IdiomaDesconocido("Matrix", "Klingon"),
            )
        }

    // ---- CU-22 E4: fallo a mitad de la fusión ----

    @Test
    fun t8_13_falloAMitadDeLaFusionNoDejaNadaAMedias() =
        prueba {
            FixturePequeno.cargar(db, reloj)
            val antes = Instantanea.filas(db)
            val conFallo =
                CopiaRepository(db, reloj = reloj, interrupcion = { creados ->
                    if (creados ==
                        3
                    ) {
                        error("Fallo simulado: sin espacio")
                    }
                })
            val c =
                copia(
                    perfiles = (1..5).map { perfil("Nuevo $it") },
                    listas =
                        listOf(
                            ListaCopia(
                                "Nueva",
                                TipoLista.RANKING,
                                Restriccion.MIXTO,
                                1L,
                                (1..5).map { elemento("Nuevo $it", posicion = it) },
                            ),
                        ),
                    sagas = listOf("Saga nueva"),
                )
            try {
                conFallo.importar(FormatoCopia.leer(FormatoCopia.escribir(c)))
                fail("Se esperaba el fallo simulado")
            } catch (e: IllegalStateException) {
                assertTrue(e.message!!.contains("Fallo simulado"))
            }
            assertEquals(antes, Instantanea.filas(db))
        }

    // ---- CU-22 A2, RF-49: versiones del formato ----

    @Test
    fun t8_14_archivoDeVersionAnteriorSeMigraEImporta() =
        prueba {
            val c = FormatoCopia.leer(CopiasReferencia.texto(1))
            assertEquals(CopiaSeguridad.VERSION_ACTUAL, c.version)
            assertEquals(8.5, c.perfiles.first { it.nombre == "Dune" }.puntuacion)
            assertEquals(10.0, c.perfiles.first { it.nombre == "El Señor de los Anillos" }.puntuacion)
            copias.importar(c)
            assertEquals(85, db.perfilDao().buscarPorNombre("dune", TipoPerfil.PELICULA)!!.puntuacionDecimas)
            // La v1 y la v2 de referencia tienen los mismos datos: importarlas da lo mismo.
            val otra = nuevaBase()
            CopiaRepository(otra).importar(FormatoCopia.leer(CopiasReferencia.texto(2)))
            assertEquals(Instantanea.contenido(otra), Instantanea.contenido(db))
        }

    @Test
    fun t8_17_copiaDeReferenciaDelFormatoActualSeImportaCompleta() =
        prueba {
            val resumen = copias.importar(FormatoCopia.leer(CopiasReferencia.texto(2)))
            assertEquals(resumen(5, 0, 3, 0), resumen)
            val dune = db.perfilDao().buscarPorNombre("dune", TipoPerfil.PELICULA)!!
            assertEquals(Estado.VISTO, dune.estado)
            assertEquals(85, dune.puntuacionDecimas)
            assertEquals("Impresionante.\nLa volvería a ver 🎬", dune.comentario)
            assertEquals(1762689600000L, dune.comentarioEditadoEn)
            assertEquals("Dune", db.sagaDao().obtener(dune.sagaId!!)!!.nombre)
            assertEquals(listOf("Ciencia Ficción", "Aventura"), db.perfilDao().generosDe(dune.id).map { it.nombre })
            assertEquals(0, db.perfilDao().buscarPorNombre("cosmos", TipoPerfil.SERIE)!!.puntuacionDecimas)
            // S-08: la saga sin perfiles también se importa.
            assertNotNull(db.sagaDao().buscarPorNombre("star wars"))
            assertEquals(listOf("El Señor de los Anillos", "Dune", "Dune: Parte Dos"), nombresEn(listaId("Top Histórico")))
            val prioridad = listas.elementos(listaId("Próximas a ver")).map { it.prioridad }
            assertEquals(listOf(Prioridad.ALTA, Prioridad.BAJA), prioridad)
            assertEquals(listOf("Dune", "Dark"), nombresEn(listaId("Para el finde")))
        }

    // ---- CU-22 A3 ----

    @Test
    fun t8_15_archivoVacioOSinNadaNuevoNoCambiaNada() =
        prueba {
            FixturePequeno.cargar(db, reloj)
            val antes = Instantanea.filas(db)
            assertEquals(resumen(0, 0, 0, 0), importar(copia(emptyList())))
            assertEquals(antes, Instantanea.filas(db))
            val mismo = exportarYLeer()
            assertEquals(resumen(0, 12, 0, 0), copias.importar(mismo))
            assertEquals(antes, Instantanea.filas(db))
        }
}
