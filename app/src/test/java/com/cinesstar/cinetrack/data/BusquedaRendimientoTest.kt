package com.cinesstar.cinetrack.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.data.repository.BusquedaRepository
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.domain.Ambito
import com.cinesstar.cinetrack.domain.CriteriosBusqueda
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.soporte.FixtureGrande
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** T7.22 (R, RNF-08): cada búsqueda combinada con el fixture grande (1000 perfiles) tarda menos de 1 s. */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class BusquedaRendimientoTest {
    private val db = CineDatabase.construir(ApplicationProvider.getApplicationContext(), nombre = null)
    private val busqueda = BusquedaRepository(db)

    @After
    fun cerrar() = db.close()

    @Test
    fun t7_22_cadaBusquedaCombinadaTardaMenosDeUnSegundo() =
        runBlocking {
            val datos = FixtureGrande().cargar(db)
            val generos = db.catalogoDao().generos().map { it.id }
            val idiomas = db.catalogoDao().idiomas().map { it.id }
            val saga = datos.sagas.values.first()
            val casos =
                listOf(
                    CriteriosBusqueda(),
                    CriteriosBusqueda(nombre = "noche"),
                    CriteriosBusqueda(nombre = "título 05", ambito = Ambito.SERIES),
                    CriteriosBusqueda(minimo = Puntuacion.of(70)),
                    CriteriosBusqueda(minimo = Puntuacion.of(30), maximo = Puntuacion.of(80), generoIds = generos.take(3).toSet()),
                    CriteriosBusqueda(idiomaId = idiomas.first(), estados = setOf(Estado.VIENDO, Estado.PENDIENTE)),
                    CriteriosBusqueda(sagaId = saga, ambito = Ambito.PELICULAS),
                    CriteriosBusqueda(
                        nombre = "o",
                        ambito = Ambito.PELICULAS,
                        minimo = Puntuacion.of(50),
                        generoIds = generos.toSet(),
                        idiomaId = idiomas.first(),
                        estados = Estado.entries.toSet(),
                    ),
                )
            busqueda.buscar(CriteriosBusqueda()) // calentamiento (primera compilación de las sentencias)
            casos.forEach { criterios ->
                val inicio = System.nanoTime()
                val n = busqueda.buscar(criterios).size
                val ms = (System.nanoTime() - inicio) / 1_000_000
                println("T7.22 $criterios: $n resultados en $ms ms")
                assertTrue("$criterios tardó $ms ms (máximo $LIMITE_MS)", ms < LIMITE_MS)
            }
        }

    private companion object {
        const val LIMITE_MS = 1_000L
    }
}
