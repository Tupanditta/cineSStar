package com.cinesstar.cinetrack.data

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.repository.SagaRepository
import com.cinesstar.cinetrack.domain.CopiaSeguridad
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.FormatoCopia
import com.cinesstar.cinetrack.domain.ListaCopia
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.soporte.CopiasReferencia
import com.cinesstar.cinetrack.soporte.FixtureGrande
import com.cinesstar.cinetrack.soporte.FixturePequeno
import com.cinesstar.cinetrack.soporte.Instantanea
import com.cinesstar.cinetrack.soporte.comoCopia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests D del S8: reglas de fusión de la importación (CU-22, S-20), idempotencia y postcondiciones. */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class FusionS8Test : PruebaCopia() {
    // ---- CU-22 reglas de fusión (S-20) ----

    @Test
    fun t8_22_perfilesExistentesSeOmitenYLosNuevosSeAnaden() =
        prueba {
            val a = crear("A")
            val b = crear("B")
            val antes = Instantanea.filas(db)
            val r = importar(copia(listOf(perfil("B"), perfil("C"), perfil("D"))))
            assertEquals(resumen(2, 1, 0, 0), r)
            val despues = Instantanea.filas(db)
            assertEquals(antes["perfil:$a"], despues["perfil:$a"])
            assertEquals(antes["perfil:$b"], despues["perfil:$b"])
            assertEquals(
                listOf("A", "B", "C", "D"),
                db
                    .copiaDao()
                    .perfiles()
                    .map { it.nombre }
                    .sorted(),
            )
        }

    @Test
    fun t8_23_prevalecenLosDatosLocalesDelPerfilExistente() =
        prueba {
            val id = crear("Dune", estado = Estado.VIENDO, decimas = 50, comentario = "Local", saga = "Dune")
            perfiles.actualizar(
                id,
                NuevoPerfil(
                    "Dune",
                    TipoPerfil.PELICULA,
                    Estado.VIENDO,
                    Puntuacion.of(50),
                    "Local",
                    sagaId = db.perfilDao().obtener(id)!!.sagaId,
                    generoIds =
                        setOf(
                            db
                                .catalogoDao()
                                .generos()
                                .first { it.nombre == "Drama" }
                                .id,
                        ),
                ),
            )
            val antes = Instantanea.filas(db)
            val r =
                importar(
                    copia(
                        listOf(
                            perfil("Dune", puntuacion = 9.0, saga = "Otra saga", estado = Estado.VISTO, comentario = "De la copia")
                                .copy(generos = listOf("Ciencia Ficción"), idiomas = listOf("Inglés")),
                        ),
                    ),
                )
            assertEquals(resumen(0, 1, 0, 0), r)
            val despues = Instantanea.filas(db)
            assertEquals(antes["perfil:$id"], despues["perfil:$id"])
            val p = db.perfilDao().obtener(id)!!
            assertEquals(50, p.puntuacionDecimas)
            assertEquals(Estado.VIENDO, p.estado)
            assertEquals("Local", p.comentario)
            assertEquals(listOf("Drama"), db.perfilDao().generosDe(id).map { it.nombre })
            assertTrue(db.perfilDao().idiomasDe(id).isEmpty())
        }

    @Test
    fun t8_24_mismoNombreConDistintoTipoCreaElPerfilSerie() =
        prueba {
            val pelicula = crear("Dune", decimas = 80)
            val antes = Instantanea.filas(db)["perfil:$pelicula"]
            assertEquals(resumen(1, 0, 0, 0), importar(copia(listOf(perfil("Dune", TipoPerfil.SERIE, puntuacion = 6.0)))))
            assertEquals(antes, Instantanea.filas(db)["perfil:$pelicula"])
            assertEquals(60, db.perfilDao().buscarPorNombre("dune", TipoPerfil.SERIE)!!.puntuacionDecimas)
        }

    @Test
    fun t8_25_nombreIgualSalvoMayusculasYTildesEsElMismoPerfil() =
        prueba {
            crear("El Padrino")
            assertEquals(resumen(0, 1, 0, 0), importar(copia(listOf(perfil("EL PÁDRINO")))))
            assertEquals(1, db.perfilDao().contar())
        }

    @Test
    fun t8_26_laSagaExistenteSeReutilizaYLaNuevaSeCrea() =
        prueba {
            val existente = SagaRepository(db).crear("El Señor de los Anillos")
            importar(
                copia(
                    listOf(perfil("La Comunidad del Anillo", saga = "el señor de los ANILLOS"), perfil("Dune", saga = "Dune")),
                    sagas = listOf("EL SEÑOR DE LOS ANILLOS", "Dune"),
                ),
            )
            val sagas = db.sagaDao().todas()
            assertEquals(2, sagas.size)
            assertEquals("El Señor de los Anillos", db.sagaDao().obtener(existente)!!.nombre)
            assertEquals(existente, db.perfilDao().buscarPorNombre("la comunidad del anillo", TipoPerfil.PELICULA)!!.sagaId)
            val dune = db.sagaDao().buscarPorNombre("dune")!!
            assertEquals(dune.id, db.perfilDao().buscarPorNombre("dune", TipoPerfil.PELICULA)!!.sagaId)
        }

    @Test
    fun t8_27_listaNormalCompatibleSeAmpliaSinDuplicar() =
        prueba {
            val a = crear("A")
            crear("B")
            val lista = listas.crear("Finde", TipoLista.NORMAL)
            listas.anadir(lista, a)
            val antes = Instantanea.filas(db)
            val r =
                importar(
                    copia(
                        listOf(perfil("A"), perfil("B"), perfil("C")),
                        listOf(
                            ListaCopia(
                                "FINDE",
                                TipoLista.NORMAL,
                                Restriccion.MIXTO,
                                1L,
                                listOf(elemento("A"), elemento("B"), elemento("C")),
                            ),
                        ),
                    ),
                )
            assertEquals(resumen(1, 2, 0, 1), r)
            assertEquals(listOf("A", "B", "C"), nombresEn(lista))
            assertEquals(antes["item:$lista:$a"], Instantanea.filas(db)["item:$lista:$a"])
            assertEquals(1, db.copiaDao().listas().size)
        }

    @Test
    fun t8_28_rankingAmpliadoConservaLasPosicionesYAnadeAlFinalEnElOrdenDeLaCopia() =
        prueba {
            val ids = listOf("R1", "R2", "R3").map { crear(it) }
            val ranking = listas.crear("Top", TipoLista.RANKING)
            ids.forEach { listas.anadir(ranking, it) }
            val c =
                copia(
                    listOf("R1", "R2", "R3", "N1", "N2").map { perfil(it) },
                    listOf(
                        ListaCopia(
                            "Top",
                            TipoLista.RANKING,
                            Restriccion.MIXTO,
                            1L,
                            listOf(
                                elemento("N2", posicion = 1),
                                elemento("R3", posicion = 2),
                                elemento("R1", posicion = 3),
                                elemento("N1", posicion = 4),
                                elemento("R2", posicion = 5),
                            ),
                        ),
                    ),
                )
            assertEquals(resumen(2, 3, 0, 1), importar(c))
            val items = listas.elementos(ranking)
            assertEquals(listOf("R1", "R2", "R3", "N2", "N1"), nombresEn(ranking))
            assertEquals((1..5).toList(), items.map { it.posicion })
        }

    @Test
    fun t8_29_listaDePrioridadAmpliadaConservaLosNivelesDeLaCopia() =
        prueba {
            val a = crear("A", TipoPerfil.SERIE)
            val lista = listas.crear("Ver", TipoLista.PRIORIDAD)
            listas.anadir(lista, a, prioridad = Prioridad.BAJA)
            importar(
                copia(
                    listOf(perfil("A", TipoPerfil.SERIE), perfil("B", TipoPerfil.SERIE)),
                    listOf(
                        ListaCopia(
                            "Ver",
                            TipoLista.PRIORIDAD,
                            Restriccion.MIXTO,
                            1L,
                            listOf(
                                elemento("A", TipoPerfil.SERIE, prioridad = Prioridad.ALTA),
                                elemento("B", TipoPerfil.SERIE, prioridad = Prioridad.ALTA),
                            ),
                        ),
                    ),
                ),
            )
            val porPerfil = listas.elementos(lista).associate { it.perfilId to it.prioridad }
            assertEquals(Prioridad.BAJA, porPerfil[a])
            assertEquals(Prioridad.ALTA, porPerfil[db.perfilDao().buscarPorNombre("b", TipoPerfil.SERIE)!!.id])
        }

    @Test
    fun t8_30_mismoNombreConDistintoTipoORestriccionSeImportaComoImportada() =
        prueba {
            val a = crear("A")
            val local = listas.crear("Favoritas", TipoLista.NORMAL)
            listas.anadir(local, a)
            val antes = Instantanea.filas(db)
            val ranking = ListaCopia("Favoritas", TipoLista.RANKING, Restriccion.MIXTO, 1L, listOf(elemento("A", posicion = 1)))
            assertEquals(resumen(0, 1, 1, 0), importar(copia(listOf(perfil("A")), listOf(ranking))))
            val importada = listaId("Favoritas (importada)")
            assertEquals(TipoLista.RANKING, db.listaDao().obtener(importada)!!.tipo)
            assertEquals(listOf("A"), nombresEn(importada))
            assertEquals(antes["lista:$local"], Instantanea.filas(db)["lista:$local"])

            // Si "(importada)" también existe y no es compatible: "(importada 2)". Con otra restricción, tampoco coincide.
            val otraRestriccion =
                ListaCopia("Favoritas", TipoLista.RANKING, Restriccion.SOLO_PELICULAS, 1L, listOf(elemento("A", posicion = 1)))
            assertEquals(resumen(0, 1, 1, 0), importar(copia(listOf(perfil("A")), listOf(otraRestriccion))))
            assertEquals(Restriccion.SOLO_PELICULAS, db.listaDao().obtener(listaId("Favoritas (importada 2)"))!!.restriccion)
            assertEquals(3, db.copiaDao().listas().size)
        }

    @Test
    fun t8_31_listaSinCoincidenciaSeCreaConSusElementosPosicionesYNiveles() =
        prueba {
            val c =
                copia(
                    listOf(perfil("A"), perfil("B"), perfil("C")),
                    listOf(
                        ListaCopia(
                            "Top",
                            TipoLista.RANKING,
                            Restriccion.SOLO_PELICULAS,
                            1L,
                            listOf(elemento("C", posicion = 2), elemento("A", posicion = 1)),
                        ),
                        ListaCopia(
                            "Ver",
                            TipoLista.PRIORIDAD,
                            Restriccion.MIXTO,
                            1L,
                            listOf(elemento("B", prioridad = Prioridad.BAJA), elemento("C", prioridad = Prioridad.ALTA)),
                        ),
                    ),
                )
            assertEquals(resumen(3, 0, 2, 0), importar(c))
            val top = listaId("Top")
            assertEquals(Restriccion.SOLO_PELICULAS, db.listaDao().obtener(top)!!.restriccion)
            assertEquals(listOf("A", "C"), nombresEn(top))
            assertEquals(listOf(1, 2), listas.elementos(top).map { it.posicion })
            assertEquals(listOf("C", "B"), nombresEn(listaId("Ver")))
            assertEquals(listOf(Prioridad.ALTA, Prioridad.BAJA), listas.elementos(listaId("Ver")).map { it.prioridad })
        }

    @Test
    fun t8_32_elementoDeUnPerfilOmitidoUsaElPerfilLocal() =
        prueba {
            val local = crear("Dune", decimas = 70)
            importar(
                copia(
                    listOf(perfil("DUNE", puntuacion = 9.0)),
                    listOf(ListaCopia("Nueva", TipoLista.NORMAL, Restriccion.MIXTO, 1L, listOf(elemento("DUNE")))),
                ),
            )
            assertEquals(listOf(local), listas.elementos(listaId("Nueva")).map { it.perfilId })
            assertEquals(1, db.perfilDao().contar())
        }

    @Test
    fun t8_33_importarDosVecesElMismoArchivoNoCambiaNadaLaSegunda() =
        prueba {
            crear("A")
            val local = listas.crear("Favoritas", TipoLista.NORMAL)
            val c =
                copia(
                    listOf(perfil("A"), perfil("B")),
                    listOf(
                        ListaCopia("Favoritas", TipoLista.RANKING, Restriccion.MIXTO, 1L, listOf(elemento("B", posicion = 1))),
                        ListaCopia("Otra", TipoLista.NORMAL, Restriccion.MIXTO, 1L, listOf(elemento("A"), elemento("B"))),
                    ),
                    sagas = listOf("Saga"),
                )
            assertEquals(resumen(1, 1, 2, 0), importar(c))
            val tras1 = Instantanea.filas(db)
            assertEquals(resumen(0, 2, 0, 0), importar(c))
            assertEquals(tras1, Instantanea.filas(db))
            assertNotNull(db.listaDao().obtener(local))
        }

    @Test
    fun t8_34_importarLaExportacionDeLaPropiaBaseNoCambiaNada() =
        prueba {
            FixturePequeno.cargar(db, reloj)
            val antes = Instantanea.filas(db)
            assertEquals(resumen(0, 12, 0, 0), copias.importar(exportarYLeer()))
            assertEquals(antes, Instantanea.filas(db))
        }

    @Test
    fun t8_35_elRecuentoPrevioCoincideConElResultadoFinal() =
        prueba {
            FixturePequeno.cargar(db, reloj)
            val c =
                FormatoCopia.leer(CopiasReferencia.texto(2)).let {
                    // Dune, Dune: Parte Dos, Dark y Cosmos ya existen; "Top Histórico" y "Para el finde" coinciden en tipo y
                    // restricción y reciben un perfil cada una; "Próximas a ver" ya los tiene todos.
                    it.copy(listas = it.listas + ListaCopia("Sin coincidencia", TipoLista.NORMAL, Restriccion.MIXTO, 1L))
                }
            val previo = copias.planificar(c).resumen
            assertEquals(resumen(1, 4, 1, 2), previo)
            assertEquals(previo, copias.importar(c))
        }

    @Test
    fun t8_36_ningunDatoPrevioSeBorraNiModificaTrasLaFusion() =
        prueba {
            FixturePequeno.cargar(db, reloj)
            val antes = Instantanea.filas(db)
            copias.importar(FormatoCopia.leer(CopiasReferencia.texto(2)))
            copias.importar(FixtureGrande().comoCopiaPequena())
            val despues = Instantanea.filas(db)
            antes.forEach { (clave, fila) -> assertEquals("$clave cambió", fila, despues[clave]) }
            assertTrue(despues.size > antes.size)
        }

    /** Las 50 primeras filas del fixture grande (con listas que solo apuntan a ellas), para no alargar el test. */
    private fun FixtureGrande.comoCopiaPequena(): CopiaSeguridad {
        val completa = comoCopia()
        val nombres =
            completa.perfiles
                .take(MUESTRA)
                .map { it.nombre }
                .toSet()
        return completa.copy(
            perfiles = completa.perfiles.take(MUESTRA),
            listas =
                completa.listas.map { l ->
                    l.copy(
                        elementos =
                            l.elementos.filter { it.nombre in nombres }.mapIndexed { i, e ->
                                if (e.posicion !=
                                    null
                                ) {
                                    e.copy(posicion = i + 1)
                                } else {
                                    e
                                }
                            },
                    )
                },
        )
    }

    private companion object {
        const val MUESTRA = 50
    }
}
