package com.cinesstar.cinetrack.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.data.repository.ListaRepository
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.domain.DominioException
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.soporte.FixturePequeno
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests D del S5: T5.31 y las reglas de repositorio en las que se apoyan los tests de interfaz de CU-11..CU-19
 * (renombrar, aviso único S-17, lectura de "Mis Listas", contenido, selector). Room en memoria con Robolectric.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class ListaRepositoryS5Test {
    private var ahora = 1_000L
    private val db = CineDatabase.construir(ApplicationProvider.getApplicationContext(), nombre = null)
    private val perfiles = PerfilRepository(db, reloj = { ahora++ })
    private val listas = ListaRepository(db, reloj = { ahora++ })

    @After
    fun cerrar() = db.close()

    private fun prueba(bloque: suspend () -> Unit) = runBlocking { bloque() }

    private suspend fun perfil(
        nombre: String,
        tipo: TipoPerfil = TipoPerfil.PELICULA,
    ) = perfiles.crear(NuevoPerfil(nombre, tipo))

    private suspend fun error(bloque: suspend () -> Unit): ErrorDominio? =
        try {
            bloque()
            null
        } catch (e: DominioException) {
            e.error
        }

    @Test
    fun t5_31_borrarUnPerfilQueEstaEnListasLoQuitaDeTodasYCompactaLosRankings() =
        prueba {
            val f = FixturePequeno.cargar(db, reloj = { ahora++ })
            val padrino = f.perfiles.getValue("Pádrino|${TipoPerfil.PELICULA}")
            val normal = f.listas.getValue(FixturePequeno.LISTA_NORMAL)
            val ranking = f.listas.getValue(FixturePequeno.LISTA_RANKING)
            assertTrue(listas.elementos(normal).any { it.perfilId == padrino })
            assertEquals(1, listas.elementos(ranking).single { it.perfilId == padrino }.posicion)

            perfiles.eliminar(padrino)

            assertTrue(listas.elementos(normal).none { it.perfilId == padrino })
            assertEquals((1..5).toList(), listas.elementos(ranking).map { it.posicion })
            // Las listas siguen existiendo con el resto de sus elementos (S-09: sin aviso de lista vacía).
            assertEquals(3, listas.elementos(normal).size)
        }

    @Test
    fun cu14_renombrarSoloCambiaElNombre() =
        prueba {
            val p = perfil("Alien")
            val l = listas.crear("Terror", TipoLista.RANKING, Restriccion.SOLO_PELICULAS)
            listas.anadir(l, p)

            assertTrue(listas.renombrar(l, "  Miedo   total "))

            val lista = listas.obtener(l)!!
            assertEquals("Miedo total", lista.nombre)
            assertEquals(TipoLista.RANKING to Restriccion.SOLO_PELICULAS, lista.tipo to lista.restriccion)
            assertEquals(listOf(p), listas.elementos(l).map { it.perfilId })
        }

    @Test
    fun cu14_a2_e1_e2_mismoNombreVacioYDuplicado() =
        prueba {
            val l = listas.crear("Terror", TipoLista.NORMAL)
            listas.crear("Comedia", TipoLista.NORMAL)

            assertFalse("Mismo nombre: sin cambios", listas.renombrar(l, "Terror"))
            assertTrue("Cambiar solo mayúsculas o tildes es válido", listas.renombrar(l, "TÉRROR"))
            assertEquals(ErrorDominio.ListaNombreObligatorio, error { listas.renombrar(l, "   ") })
            assertEquals(ErrorDominio.ListaDuplicada, error { listas.renombrar(l, "cómedia") })
            assertEquals("TÉRROR", listas.obtener(l)!!.nombre)
            assertEquals(ErrorDominio.NoEncontrado, error { listas.renombrar(9_999, "Otra") })
        }

    @Test
    fun s17_elAvisoDeListaVaciaSaleUnaVezPorVaciado() =
        prueba {
            val a = perfil("A")
            val b = perfil("B")
            val l = listas.crear("Lista", TipoLista.NORMAL)
            listas.anadir(l, a)
            listas.anadir(l, b)

            assertFalse("Con 2 elementos, quitar uno no avisa (T5.28)", listas.quitar(l, a))
            assertTrue("Quitar el último avisa (T5.23)", listas.quitar(l, b))
            assertTrue(listas.obtener(l)!!.avisoVaciaMostrado)

            // Vuelve a tener elementos y a vaciarse: el aviso sale otra vez (T5.27).
            listas.anadir(l, a)
            assertFalse(listas.obtener(l)!!.avisoVaciaMostrado)
            assertTrue(listas.quitar(l, a))
        }

    @Test
    fun cu12_panelConTotalesAltasYPrimeroDelRanking() =
        prueba {
            val f = FixturePequeno.cargar(db, reloj = { ahora++ })
            val panel = listas.observarPanel().first()

            assertEquals(12, panel.obrasCatalogadas)
            assertEquals(
                "Más recientes primero",
                listOf(FixturePequeno.LISTA_RANKING, FixturePequeno.LISTA_PRIORIDAD, FixturePequeno.LISTA_NORMAL),
                panel.listas.map { it.nombre },
            )
            val porNombre = panel.listas.associateBy { it.nombre }
            assertEquals(6, porNombre.getValue(FixturePequeno.LISTA_RANKING).total)
            assertEquals("Pádrino", porNombre.getValue(FixturePequeno.LISTA_RANKING).primero)
            assertEquals(2, porNombre.getValue(FixturePequeno.LISTA_PRIORIDAD).altas)
            assertEquals(4, porNombre.getValue(FixturePequeno.LISTA_NORMAL).total)
            assertEquals(f.listas.getValue(FixturePequeno.LISTA_NORMAL), porNombre.getValue(FixturePequeno.LISTA_NORMAL).id)
        }

    @Test
    fun cu13_contenidoEnElOrdenDeSuTipoYNullSiNoExiste() =
        prueba {
            val ids = listOf("Uno", "Dos", "Tres").map { perfil(it) }
            val prioridad = listas.crear("Prioridad", TipoLista.PRIORIDAD)
            listas.anadir(prioridad, ids[0], prioridad = Prioridad.BAJA)
            listas.anadir(prioridad, ids[1])
            listas.anadir(prioridad, ids[2], prioridad = Prioridad.ALTA)

            val contenido = listas.leerContenido(prioridad)!!
            assertEquals(listOf("Tres", "Dos", "Uno"), contenido.elementos.map { it.nombre })
            assertEquals(listOf(Prioridad.ALTA, Prioridad.MEDIA, Prioridad.BAJA), contenido.elementos.map { it.prioridad })

            listas.eliminar(prioridad)
            assertNull(listas.leerContenido(prioridad))
        }

    @Test
    fun cu15_candidatosYOpcionesIndicanDisponibilidad() =
        prueba {
            val peli = perfil("Película")
            val serie = perfil("Serie", TipoPerfil.SERIE)
            val soloPeliculas = listas.crear("Solo pelis", TipoLista.NORMAL, Restriccion.SOLO_PELICULAS)
            listas.anadir(soloPeliculas, peli)

            val candidatos = listas.observarCandidatos(soloPeliculas).first().associateBy { it.perfilId }
            assertTrue(candidatos.getValue(peli).enLista)
            assertFalse(candidatos.getValue(serie).admitido)
            assertFalse(candidatos.getValue(peli).disponible || candidatos.getValue(serie).disponible)

            val opciones = listas.opcionesPara(serie).single()
            assertFalse(opciones.admite)
            assertEquals(ErrorDominio.RestriccionLista(TipoPerfil.PELICULA), error { listas.anadir(soloPeliculas, serie) })
        }
}
