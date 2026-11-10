package com.cinesstar.cinetrack.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.data.repository.BibliotecaRepository
import com.cinesstar.cinetrack.data.repository.BusquedaRepository
import com.cinesstar.cinetrack.data.repository.ListaRepository
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.domain.Ambito
import com.cinesstar.cinetrack.domain.CriterioAgrupacion
import com.cinesstar.cinetrack.domain.CriteriosBusqueda
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.FormatoCopia
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.soporte.DatosFixture
import com.cinesstar.cinetrack.soporte.FixtureGrande
import com.cinesstar.cinetrack.soporte.Proyecto
import com.cinesstar.cinetrack.soporte.comoCopia
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests R del S9 (RNF-08) sobre los repositorios con el fixture grande (1000 perfiles, 30 listas, 5000 elementos):
 * cada lectura que abre una pantalla tarda menos de 1 s. Se mide la segunda lectura: la primera compila las sentencias
 * SQL, como pasa una sola vez al abrir la app.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class RendimientoS9Test {
    private val db = CineDatabase.construir(ApplicationProvider.getApplicationContext(), nombre = null)
    private lateinit var datos: DatosFixture
    private var listaGrande = 0L

    @Before
    fun cargar() =
        runBlocking {
            datos = FixtureGrande().cargar(db)
            val listas = ListaRepository(db)
            listaGrande = listas.crear("Lista de $ELEMENTOS_LISTA", TipoLista.RANKING)
            datos.perfiles.values
                .take(ELEMENTOS_LISTA)
                .forEach { listas.anadir(listaGrande, it) }
        }

    @After
    fun cerrar() = db.close()

    private fun prueba(bloque: suspend () -> Unit) = runBlocking { bloque() }

    /** Tiempo en milisegundos de [bloque], tras una ejecución de calentamiento. */
    private suspend fun <T> medir(
        nombre: String,
        bloque: suspend () -> T,
    ): Pair<T, Long> {
        bloque()
        val inicio = System.nanoTime()
        val resultado = bloque()
        val ms = (System.nanoTime() - inicio) / NANOS_POR_MS
        println("$nombre: $ms ms")
        assertTrue("$nombre tardó $ms ms (máximo $LIMITE_MS)", ms < LIMITE_MS)
        return resultado to ms
    }

    @Test
    fun t9_01_abrirUnPerfilCon1000PerfilesTardaMenosDeUnSegundo() =
        prueba {
            val perfiles = PerfilRepository(db)
            // El perfil con más listas: el caso más caro de la ficha (§7.5).
            val id = datos.perfiles.values.maxBy { db.listaDao().listasDe(it).size }
            val (ficha, _) = medir("T9.01 ficha del perfil") { perfiles.obtenerFicha(id) }
            assertTrue(ficha!!.listas.isNotEmpty())
        }

    @Test
    fun t9_02_abrirUnaListaCon200ElementosTardaMenosDeUnSegundo() =
        prueba {
            val (contenido, _) = medir("T9.02 lista de $ELEMENTOS_LISTA elementos") { ListaRepository(db).leerContenido(listaGrande) }
            assertEquals(ELEMENTOS_LISTA, contenido!!.elementos.size)
        }

    @Test
    fun t9_03_busquedaCombinadaLocalTardaMenosDeUnSegundo() =
        prueba {
            val generos = db.catalogoDao().generos().map { it.id }
            val criterios =
                CriteriosBusqueda(
                    nombre = "o",
                    ambito = Ambito.TODO,
                    minimo = Puntuacion.of(30),
                    maximo = Puntuacion.of(90),
                    generoIds = generos.take(4).toSet(),
                    estados = setOf(Estado.VISTO, Estado.VIENDO),
                )
            medir("T9.03 búsqueda combinada") { BusquedaRepository(db).buscar(criterios) }
        }

    @Test
    fun t9_04_agruparPorGeneroCon1000PerfilesTardaMenosDeUnSegundo() =
        prueba {
            val (grupos, _) = medir("T9.04 agrupar por género") { BibliotecaRepository(db).agrupar(CriterioAgrupacion.GENERO) }
            assertTrue(grupos.isNotEmpty())
            assertTrue(grupos.sumOf { it.perfiles.size } >= FixtureGrande.PERFILES)
        }

    /** Deja la copia del fixture grande en `app/build/capturas` para las medidas manuales en dispositivo (T9.05, T9.06). */
    @Test
    fun copiaDe1000PerfilesParaLasPruebasManuales() {
        val archivo = java.io.File(Proyecto.capturas, "cinetrack_backup_1000_perfiles.json")
        archivo.writeText(FormatoCopia.escribir(FixtureGrande().comoCopia()))
        assertEquals(FixtureGrande.PERFILES, FormatoCopia.leer(archivo.readText()).perfiles.size)
    }

    private companion object {
        const val LIMITE_MS = 1_000L
        const val NANOS_POR_MS = 1_000_000L
        const val ELEMENTOS_LISTA = 200
    }
}
