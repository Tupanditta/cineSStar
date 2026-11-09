package com.cinesstar.cinetrack.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.data.repository.ListaRepository
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.repository.SagaRepository
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.data.room.ListaDao
import com.cinesstar.cinetrack.data.room.PerfilDao
import com.cinesstar.cinetrack.data.room.PerfilIdiomaEntity
import com.cinesstar.cinetrack.domain.DominioException
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

/** Tests D del S3 (T3.25–T3.29) y reglas de repositorio de CU-02..CU-05. Room en memoria con Robolectric. */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class PerfilRepositoryS3Test {
    private var ahora = 1_000L
    private val db = CineDatabase.construir(ApplicationProvider.getApplicationContext(), nombre = null)
    private val perfiles = PerfilRepository(db, reloj = { ahora })
    private val sagas = SagaRepository(db)
    private val listas = ListaRepository(db, reloj = { ahora++ })

    @After
    fun cerrar() = db.close()

    private fun prueba(bloque: suspend () -> Unit) = runBlocking { bloque() }

    private suspend fun perfil(
        nombre: String,
        tipo: TipoPerfil = TipoPerfil.PELICULA,
    ) = perfiles.crear(NuevoPerfil(nombre, tipo))

    private fun contar(sql: String): Int =
        db.openHelper.readableDatabase.query(sql).use {
            it.moveToFirst()
            it.getInt(0)
        }

    @Test
    fun t3_25_borrarUnPerfilQueEstaEnTresListasLoQuitaDeTodas() =
        prueba {
            val p = perfil("Dune")
            val otro = perfil("Otro")
            val normal = listas.crear("Normal", TipoLista.NORMAL)
            val prioridad = listas.crear("Prioridad", TipoLista.PRIORIDAD)
            val ranking = listas.crear("Ranking", TipoLista.RANKING)
            listOf(normal, prioridad, ranking).forEach {
                listas.anadir(it, p, prioridad = if (it == prioridad) Prioridad.ALTA else null)
                listas.anadir(it, otro)
            }

            perfiles.eliminar(p)

            assertNull(perfiles.obtener(p))
            listOf(normal, prioridad, ranking).forEach { lista ->
                assertEquals(listOf(otro), listas.elementos(lista).map { it.perfilId })
            }
            assertEquals(0, contar("SELECT COUNT(*) FROM lista_item WHERE perfilId = $p"))
            assertEquals(0, contar("SELECT COUNT(*) FROM perfil WHERE id = $p"))
        }

    @Test
    fun t3_26_borrarDelMedioDeUnRankingDejaPosicionesContiguas() =
        prueba {
            val ids = (1..5).map { perfil("P$it") }
            val r = listas.crear("Top", TipoLista.RANKING)
            ids.forEach { listas.anadir(r, it) }

            perfiles.eliminar(ids[2])

            val elementos = listas.elementos(r)
            assertEquals(listOf(ids[0], ids[1], ids[3], ids[4]), elementos.map { it.perfilId })
            assertEquals(listOf(1, 2, 3, 4), elementos.map { it.posicion })
        }

    @Test
    fun t3_27_borrarElUnicoPerfilDeUnaSagaLaConserva() =
        prueba {
            val saga = sagas.crear("Matrix")
            val p = perfiles.crear(NuevoPerfil("Matrix", TipoPerfil.PELICULA, sagaId = saga))

            perfiles.eliminar(p)

            assertEquals(listOf("Matrix"), sagas.todas().map { it.nombre })
            // Se puede reutilizar (S-08).
            val nuevo = perfiles.crear(NuevoPerfil("Matrix Reloaded", TipoPerfil.PELICULA, sagaId = saga))
            assertEquals(saga, perfiles.obtener(nuevo)!!.perfil.sagaId)
        }

    @Test
    fun t3_28_borrarElUnicoPerfilDeUnaListaLaDejaVaciaYConservada() =
        prueba {
            val p = perfil("Solo")
            val lista = listas.crear("Una", TipoLista.NORMAL)
            listas.anadir(lista, p)

            perfiles.eliminar(p)

            assertNotNull(listas.obtener(lista))
            assertTrue(listas.elementos(lista).isEmpty())
        }

    @Test
    fun t3_29_unFalloDuranteElBorradoNoBorraNada() =
        prueba {
            val a = perfil("A")
            val b = perfil("B")
            val c = perfil("C")
            val r = listas.crear("Top", TipoLista.RANKING)
            listOf(a, b, c).forEach { listas.anadir(r, it) }
            val n = listas.crear("Normal", TipoLista.NORMAL)
            listas.anadir(n, b)
            val real = db.listaDao()
            val conFallo =
                object : ListaDao by real {
                    override suspend fun actualizarPosicion(
                        listaId: Long,
                        perfilId: Long,
                        posicion: Int,
                    ) = throw IOException("Fallo simulado")
                }

            val error = runCatching { PerfilRepository(db, listaDao = conFallo).eliminar(b) }.exceptionOrNull()

            assertTrue(error is IOException)
            assertNotNull(perfiles.obtener(b))
            assertEquals(listOf(a to 1, b to 2, c to 3), listas.elementos(r).map { it.perfilId to it.posicion })
            assertEquals(listOf(b), listas.elementos(n).map { it.perfilId })
        }

    @Test
    fun cu02_actualizarSustituyeLosDatosYConservaTipoListasYFechaDeAlta() =
        prueba {
            val generos = db.catalogoDao().generos()
            val idiomas = db.catalogoDao().idiomas()
            val saga = sagas.crear("Dune")
            val p =
                perfiles.crear(
                    NuevoPerfil(
                        "Dune",
                        TipoPerfil.PELICULA,
                        sagaId = saga,
                        generoIds = setOf(generos[0].id, generos[1].id),
                        idiomaIds = setOf(idiomas[0].id),
                        comentario = "Primera",
                    ),
                )
            val r = listas.crear("Top", TipoLista.RANKING)
            listas.anadir(r, perfil("Antes"))
            listas.anadir(r, p)
            val antes = perfiles.obtener(p)!!.perfil
            ahora = 5_000L

            perfiles.actualizar(
                p,
                NuevoPerfil(
                    "Dune: Parte Uno",
                    TipoPerfil.SERIE,
                    estado = Estado.VISTO,
                    puntuacion = Puntuacion.of(88),
                    comentario = "Primera",
                    sagaNueva = "Arrakis",
                    generoIds = setOf(generos[2].id),
                    idiomaIds = emptySet(),
                ),
            )

            val despues = perfiles.obtener(p)!!
            assertEquals("Dune: Parte Uno", despues.perfil.nombre)
            assertEquals("dune: parte uno", despues.perfil.nombreBusqueda)
            assertEquals("El tipo no cambia (S-03)", TipoPerfil.PELICULA, despues.perfil.tipo)
            assertEquals(Estado.VISTO to 88, despues.perfil.estado to despues.perfil.puntuacionDecimas)
            assertEquals("Arrakis", despues.saga!!.nombre)
            assertEquals(listOf(generos[2].nombre), despues.generos.map { it.nombre })
            assertTrue(despues.idiomas.isEmpty())
            assertEquals(antes.creadoEn, despues.perfil.creadoEn)
            assertEquals("El comentario no cambió: conserva su fecha", antes.comentarioEditadoEn, despues.perfil.comentarioEditadoEn)
            // CU-02 postcondición: sigue en el ranking con la misma posición.
            assertEquals(2, listas.elementos(r).single { it.perfilId == p }.posicion)
        }

    @Test
    fun cu02_e2_duplicadoConOtroPerfilPeroNoConsigoMismo() =
        prueba {
            perfil("El Padrino")
            perfil("Dune", TipoPerfil.SERIE)
            val p = perfil("Dune")

            val error = runCatching { perfiles.actualizar(p, NuevoPerfil("el padrino", TipoPerfil.PELICULA)) }.exceptionOrNull()
            assertEquals(ErrorDominio.PerfilDuplicado(TipoPerfil.PELICULA), (error as DominioException).error)
            assertEquals("Dune", perfiles.obtener(p)!!.perfil.nombre)

            perfiles.actualizar(p, NuevoPerfil("DÚNE", TipoPerfil.PELICULA))
            assertEquals("DÚNE", perfiles.obtener(p)!!.perfil.nombre)
            assertTrue(perfiles.existe("dune", TipoPerfil.PELICULA))
            assertTrue("Excluyendo el propio perfil, el nombre está libre", !perfiles.existe("dune", TipoPerfil.PELICULA, excluirId = p))
        }

    @Test
    fun cu02_e3_unFalloDeEscrituraDejaElPerfilComoEstaba() =
        prueba {
            val idiomas = db.catalogoDao().idiomas()
            val p = perfiles.crear(NuevoPerfil("Intacto", TipoPerfil.PELICULA, idiomaIds = setOf(idiomas[0].id)))
            val real = db.perfilDao()
            val conFallo =
                object : PerfilDao by real {
                    override suspend fun insertarIdiomas(relaciones: List<PerfilIdiomaEntity>): Unit = throw IOException("Disco lleno")
                }
            val error =
                runCatching {
                    PerfilRepository(db, perfilDao = conFallo).actualizar(
                        p,
                        NuevoPerfil("Cambiado", TipoPerfil.PELICULA, sagaNueva = "Nueva", idiomaIds = setOf(idiomas[1].id)),
                    )
                }.exceptionOrNull()
            assertTrue(error is IOException)
            val detalle = perfiles.obtener(p)!!
            assertEquals("Intacto", detalle.perfil.nombre)
            assertEquals(listOf(idiomas[0].id), detalle.idiomas.map { it.id })
            assertTrue(sagas.todas().isEmpty())
        }

    @Test
    fun cu03_cu04_cu05_actualizacionesSueltasNoTocanElRestoDeCampos() =
        prueba {
            val p = perfiles.crear(NuevoPerfil("Campos", TipoPerfil.SERIE, puntuacion = Puntuacion.of(70), comentario = "Hola"))
            val antes = perfiles.obtener(p)!!.perfil

            perfiles.cambiarEstado(p, Estado.VIENDO)
            assertEquals(antes.copy(estado = Estado.VIENDO), perfiles.obtener(p)!!.perfil)

            perfiles.puntuar(p, Puntuacion.of(100))
            assertEquals(100, perfiles.obtener(p)!!.perfil.puntuacionDecimas)
            perfiles.puntuar(p, null)
            assertNull(perfiles.obtener(p)!!.perfil.puntuacionDecimas)

            ahora = 9_000L
            perfiles.comentar(p, "Adiós\n🎬")
            val comentado = perfiles.obtener(p)!!.perfil
            assertEquals("Adiós\n🎬" to 9_000L, comentado.comentario to comentado.comentarioEditadoEn)
            perfiles.comentar(p, "   ")
            val sinComentario = perfiles.obtener(p)!!.perfil
            assertEquals(null to null, sinComentario.comentario to sinComentario.comentarioEditadoEn)
            assertEquals(Estado.VIENDO, sinComentario.estado)

            val noExiste = runCatching { perfiles.cambiarEstado(p + 100, Estado.VISTO) }.exceptionOrNull()
            assertEquals(ErrorDominio.NoEncontrado, (noExiste as DominioException).error)
        }
}
