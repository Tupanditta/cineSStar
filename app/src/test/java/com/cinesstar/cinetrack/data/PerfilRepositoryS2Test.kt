package com.cinesstar.cinetrack.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.data.repository.ListaRepository
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.repository.SagaRepository
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.domain.DominioException
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import kotlinx.coroutines.async
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

/** Operaciones de repositorio que usa el S2: saga creada en el alta (CU-10 A1/E4), duplicados en línea y ficha (CU-07). */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class PerfilRepositoryS2Test {
    private val db = CineDatabase.construir(ApplicationProvider.getApplicationContext(), nombre = null)
    private val perfiles = PerfilRepository(db)
    private val sagas = SagaRepository(db)
    private val listas = ListaRepository(db)

    @After
    fun cerrar() = db.close()

    private fun prueba(bloque: suspend () -> Unit) = runBlocking { bloque() }

    @Test
    fun laSagaNuevaSeRegistraAlGuardarYSeReutilizaPorNombreNormalizado() =
        prueba {
            val a = perfiles.crear(NuevoPerfil("Una nueva esperanza", TipoPerfil.PELICULA, sagaNueva = "  Star  Wars "))
            val b = perfiles.crear(NuevoPerfil("The Mandalorian", TipoPerfil.SERIE, sagaNueva = "STAR WARS"))
            val registradas = sagas.todas()
            assertEquals(listOf("Star Wars"), registradas.map { it.nombre })
            assertEquals(registradas.single().id, perfiles.obtener(a)!!.perfil.sagaId)
            assertEquals(registradas.single().id, perfiles.obtener(b)!!.perfil.sagaId)
        }

    @Test
    fun unaSagaNuevaVaciaFallaSinCrearNada() =
        prueba {
            val error = runCatching { perfiles.crear(NuevoPerfil("X", TipoPerfil.PELICULA, sagaNueva = "   ")) }.exceptionOrNull()
            assertEquals(ErrorDominio.SagaNombreObligatorio, (error as DominioException).error)
            assertTrue(sagas.todas().isEmpty())
            assertFalse(perfiles.existe("X", TipoPerfil.PELICULA))
        }

    @Test
    fun existeComparaNombreNormalizadoYTipo() =
        prueba {
            perfiles.crear(NuevoPerfil("El Padrino", TipoPerfil.PELICULA))
            assertTrue(perfiles.existe("  el  PÁDRINO ", TipoPerfil.PELICULA))
            assertFalse(perfiles.existe("El Padrino", TipoPerfil.SERIE))
            assertFalse(perfiles.existe("   ", TipoPerfil.PELICULA))
        }

    @Test
    fun laFichaReuneSagaListasYMejorRanking() =
        prueba {
            val saga = sagas.crear("Dune")
            val p = perfiles.crear(NuevoPerfil("Dune", TipoPerfil.PELICULA, sagaId = saga))
            perfiles.crear(NuevoPerfil("Dune: Parte Dos", TipoPerfil.PELICULA, sagaId = saga))
            perfiles.crear(NuevoPerfil("Dune: La profecía", TipoPerfil.SERIE, sagaId = saga))
            val otro = perfiles.crear(NuevoPerfil("Otro", TipoPerfil.PELICULA))
            val r1 = listas.crear("A ranking", TipoLista.RANKING)
            val r2 = listas.crear("B ranking", TipoLista.RANKING)
            listas.anadir(r1, otro)
            listas.anadir(r1, p)
            listas.anadir(r2, p)
            listas.anadir(listas.crear("C prioridad", TipoLista.PRIORIDAD), p, prioridad = Prioridad.BAJA)

            val ficha = perfiles.obtenerFicha(p)!!
            assertEquals(mapOf(TipoPerfil.PELICULA to 2, TipoPerfil.SERIE to 1), ficha.perfilesSaga)
            assertEquals(listOf("A ranking", "B ranking", "C prioridad"), ficha.listas.map { it.nombre })
            assertEquals(2 to 2, ficha.listas[0].posicion to ficha.listas[0].total)
            assertEquals(Prioridad.BAJA, ficha.listas[2].prioridad)
            assertEquals("B ranking", ficha.mejorRanking!!.nombre)
            assertNull(perfiles.obtenerFicha(otro + 100))
        }

    @Test
    fun laFichaObservadaSeActualizaAlCambiarSusListasYAlBorrarse() =
        runBlocking {
            val p = perfiles.crear(NuevoPerfil("Observada", TipoPerfil.PELICULA))
            val flujo = perfiles.observarFicha(p)
            assertTrue(flujo.first()!!.listas.isEmpty())
            val siguiente = async { flujo.first { it?.listas?.isNotEmpty() == true } }
            listas.anadir(listas.crear("Nueva", TipoLista.NORMAL), p)
            assertEquals(
                "Nueva",
                siguiente
                    .await()!!
                    .listas
                    .single()
                    .nombre,
            )
            val borrado = async { flujo.first { it == null } }
            perfiles.eliminar(p)
            assertNull(borrado.await())
        }
}
