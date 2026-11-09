package com.cinesstar.cinetrack.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.data.repository.ListaRepository
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.data.room.PerfilEntity
import com.cinesstar.cinetrack.domain.DominioException
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests D del S6 (CU-17, CU-18 y el alta con posición/prioridad de CU-15): T6.11, T6.17, T6.18 y las reglas del
 * repositorio en las que se apoyan los tests de interfaz (T6.02–T6.04, T6.07, T6.10, T6.15, T6.19, T6.20).
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class ListaRepositoryS6Test {
    private var ahora = 1_000L
    private val db = CineDatabase.construir(ApplicationProvider.getApplicationContext(), nombre = null)
    private val perfiles = PerfilRepository(db, reloj = { ahora++ })
    private val listas = ListaRepository(db, reloj = { ahora++ })

    @After
    fun cerrar() = db.close()

    private fun prueba(bloque: suspend () -> Unit) = runBlocking { bloque() }

    private suspend fun perfil(nombre: String) = perfiles.crear(NuevoPerfil(nombre, TipoPerfil.PELICULA))

    private suspend fun error(bloque: suspend () -> Unit): ErrorDominio? =
        try {
            bloque()
            null
        } catch (e: DominioException) {
            e.error
        }

    /** Ranking con [n] perfiles nuevos añadidos al final; devuelve el id de la lista y los perfiles en orden. */
    private suspend fun ranking(
        nombre: String,
        n: Int,
    ): Pair<Long, List<Long>> {
        val id = listas.crear(nombre, TipoLista.RANKING)
        val ids = (1..n).map { perfil("$nombre $it").also { p -> listas.anadir(id, p) } }
        return id to ids
    }

    private suspend fun orden(listaId: Long) = listas.elementos(listaId).map { it.perfilId }

    private suspend fun posiciones(listaId: Long) = listas.elementos(listaId).map { it.posicion }

    private suspend fun fila(perfilId: Long): PerfilEntity = db.perfilDao().obtener(perfilId)!!

    @Test
    fun t6_02_anadirEnLaPosicion2DeUnRankingDe5DesplazaElResto() =
        prueba {
            val (id, ids) = ranking("Top", 5)
            val nuevo = perfil("Nuevo")
            listas.anadir(id, nuevo, posicion = 2)
            assertEquals(listOf(ids[0], nuevo) + ids.drop(1), orden(id))
            assertEquals((1..6).toList(), posiciones(id))
        }

    @Test
    fun t6_04_posicionesNoValidasNoAnaden() =
        prueba {
            val (id, ids) = ranking("Top", 3)
            val nuevo = perfil("Nuevo")
            listOf(0, 5, -1).forEach { assertEquals(ErrorDominio.PosicionInvalida, error { listas.anadir(id, nuevo, posicion = it) }) }
            assertEquals(ids, orden(id))
            listas.anadir(id, nuevo, posicion = 4)
            assertEquals(ids + nuevo, orden(id))
        }

    @Test
    fun t6_07_prioridadAltaMediaBajaYDentroDelNivelPorAntiguedad() =
        prueba {
            val id = listas.crear("Pendientes", TipoLista.PRIORIDAD)
            val niveles = listOf(Prioridad.BAJA, Prioridad.ALTA, Prioridad.MEDIA, Prioridad.ALTA, Prioridad.BAJA, Prioridad.MEDIA)
            val ids = niveles.mapIndexed { i, p -> perfil("P$i").also { listas.anadir(id, it, prioridad = p) } }
            assertEquals(listOf(ids[1], ids[3], ids[2], ids[5], ids[0], ids[4]), orden(id))
        }

    @Test
    fun t6_10_cambiarAlMismoNivelNoCambiaNada() =
        prueba {
            val id = listas.crear("Pendientes", TipoLista.PRIORIDAD)
            val a = perfil("A").also { listas.anadir(id, it, prioridad = Prioridad.ALTA) }
            assertFalse(listas.cambiarPrioridad(id, a, Prioridad.ALTA))
            assertTrue(listas.cambiarPrioridad(id, a, Prioridad.BAJA))
            assertEquals(Prioridad.BAJA, listas.elementos(id).single().prioridad)
        }

    @Test
    fun t6_11_elMismoPerfilConPrioridadDistintaEnDosListas() =
        prueba {
            val uno = listas.crear("Uno", TipoLista.PRIORIDAD)
            val dos = listas.crear("Dos", TipoLista.PRIORIDAD)
            val p = perfil("Dune")
            val antes = fila(p)
            listas.anadir(uno, p, prioridad = Prioridad.ALTA)
            listas.anadir(dos, p, prioridad = Prioridad.BAJA)
            listas.cambiarPrioridad(uno, p, Prioridad.MEDIA)
            assertEquals(Prioridad.MEDIA, listas.elementos(uno).single().prioridad)
            assertEquals(Prioridad.BAJA, listas.elementos(dos).single().prioridad)
            // RNF-03 / RES-07: la prioridad vive en el elemento de la lista; el perfil no cambia.
            assertEquals(antes, fila(p))
            assertFalse(PerfilEntity::class.java.declaredFields.any { it.name.contains("prioridad", ignoreCase = true) })
        }

    @Test
    fun t6_12_moverEl5AlPuesto2DejaPosicionesContiguas() =
        prueba {
            val (id, ids) = ranking("Top", 5)
            assertTrue(listas.mover(id, ids[4], 2))
            assertEquals(listOf(ids[0], ids[4], ids[1], ids[2], ids[3]), orden(id))
            assertEquals((1..5).toList(), posiciones(id))
            assertTrue(listas.mover(id, ids[0], 5))
            assertEquals(listOf(ids[4], ids[1], ids[2], ids[3], ids[0]), orden(id))
            assertEquals((1..5).toList(), posiciones(id))
        }

    @Test
    fun t6_15_moverAlMismoPuestoNoEscribe() =
        prueba {
            val (id, ids) = ranking("Top", 3)
            assertFalse(listas.mover(id, ids[1], 2))
            assertEquals(ids, orden(id))
            assertEquals(ErrorDominio.PosicionInvalida, error { listas.mover(id, ids[1], 0) })
            assertEquals(ErrorDominio.PosicionInvalida, error { listas.mover(id, ids[1], 4) })
        }

    @Test
    fun t6_17_reordenarUnRankingNoCambiaElPerfilNiOtrosRankings() =
        prueba {
            val (top, ids) = ranking("Top", 4)
            val otro = listas.crear("Otro", TipoLista.RANKING)
            ids.reversed().forEach { listas.anadir(otro, it) }
            val perfilesAntes = ids.map { fila(it) }
            val otroAntes = listas.elementos(otro)

            listas.mover(top, ids[3], 1)
            listas.mover(top, ids[0], 3)

            assertEquals(listOf(ids[3], ids[1], ids[0], ids[2]), orden(top))
            assertEquals(otroAntes, listas.elementos(otro))
            assertEquals(perfilesAntes, ids.map { fila(it) })
        }

    @Test
    fun t6_18_unRankingDe150ElementosSeAceptaSinTamanoMaximo() =
        prueba {
            val (id, ids) = ranking("Enorme", 150)
            assertEquals(ids, orden(id))
            assertEquals((1..150).toList(), posiciones(id))
            val extra = perfil("Extra")
            listas.anadir(id, extra, posicion = 151)
            listas.mover(id, extra, 75)
            assertEquals(151, orden(id).size)
            assertEquals(extra, orden(id)[74])
            assertEquals((1..151).toList(), posiciones(id))
        }

    @Test
    fun t6_19_quitarEl2DeUnRankingDe5SubeLosPosteriores() =
        prueba {
            val (id, ids) = ranking("Top", 5)
            listas.quitar(id, ids[1])
            assertEquals(ids - ids[1], orden(id))
            assertEquals((1..4).toList(), posiciones(id))
        }

    @Test
    fun t6_20_quitarDeUnaListaDePrioridadConservaElNivelDelResto() =
        prueba {
            val id = listas.crear("Pendientes", TipoLista.PRIORIDAD)
            val niveles = listOf(Prioridad.ALTA, Prioridad.MEDIA, Prioridad.BAJA)
            val ids = niveles.mapIndexed { i, p -> perfil("P$i").also { listas.anadir(id, it, prioridad = p) } }
            listas.quitar(id, ids[1])
            assertEquals(listOf(Prioridad.ALTA, Prioridad.BAJA), listas.elementos(id).map { it.prioridad })
        }

    @Test
    fun cambiarPrioridadOMoverAlgoQueYaNoEstaEsNoEncontrado() =
        prueba {
            val (top, ids) = ranking("Top", 2)
            val prio = listas.crear("Pendientes", TipoLista.PRIORIDAD)
            val p = perfil("Suelto")
            assertEquals(ErrorDominio.NoEncontrado, error { listas.cambiarPrioridad(prio, p, Prioridad.ALTA) })
            assertEquals(ErrorDominio.NoEncontrado, error { listas.mover(top, p, 1) })
            // Una lista que no es ranking no tiene posiciones.
            listas.anadir(prio, ids[0])
            assertEquals(ErrorDominio.PosicionInvalida, error { listas.mover(prio, ids[0], 1) })
        }
}
