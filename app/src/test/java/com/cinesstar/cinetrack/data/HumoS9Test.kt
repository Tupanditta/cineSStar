package com.cinesstar.cinetrack.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.AppContainer
import com.cinesstar.cinetrack.data.backup.CopiaRepository
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.domain.CriterioAgrupacion
import com.cinesstar.cinetrack.domain.CriteriosBusqueda
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.FormatoCopia
import com.cinesstar.cinetrack.domain.GENEROS_PREDEFINIDOS
import com.cinesstar.cinetrack.domain.IDIOMAS_PREDEFINIDOS
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.ResumenFusion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.soporte.Instantanea
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * T9.14 (suite de humo): el flujo principal de cada caso de uso, CU-23 → CU-01 … CU-22, encadenado sobre el mismo
 * contenedor de dependencias que usa la app. Comprueba que las piezas encajan; los flujos alternativos y la interfaz de
 * cada caso los cubren los tests de su sprint y el recorrido T9.13.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class HumoS9Test {
    private val contexto: Application = ApplicationProvider.getApplicationContext()
    private val app = AppContainer(CineDatabase.construir(contexto, nombre = null))
    private val otra = CineDatabase.construir(contexto, nombre = null)

    @After
    fun cerrar() {
        app.database.close()
        otra.close()
    }

    @Test
    fun t9_14_cadaCasoDeUsoEnCadena() =
        runBlocking {
            with(app) {
                // CU-23: la base de datos se abre con el catálogo precargado.
                assertEquals(GENEROS_PREDEFINIDOS, catalogo.generos().map { it.nombre })
                assertEquals(IDIOMAS_PREDEFINIDOS, catalogo.idiomas().map { it.nombre })
                val drama = catalogo.generos().first { it.nombre == "Drama" }.id

                // CU-01 + CU-10 (saga nueva desde el formulario).
                val padrino =
                    perfiles.crear(
                        NuevoPerfil("El Padrino", TipoPerfil.PELICULA, sagaNueva = "El Padrino", generoIds = setOf(drama)),
                    )
                val dark = perfiles.crear(NuevoPerfil("Dark", TipoPerfil.SERIE))
                // CU-02
                perfiles.actualizar(
                    padrino,
                    NuevoPerfil("El Padrino I", TipoPerfil.PELICULA, sagaId = sagas.todas().single().id, generoIds = setOf(drama)),
                )
                // CU-03, CU-04, CU-05
                perfiles.puntuar(padrino, Puntuacion.of(95))
                perfiles.comentar(padrino, "Obra maestra.")
                perfiles.cambiarEstado(padrino, Estado.VISTO)
                // CU-07
                val ficha = perfiles.obtenerFicha(padrino)!!
                assertEquals("El Padrino I", ficha.detalle.perfil.nombre)
                assertEquals(95, ficha.detalle.perfil.puntuacionDecimas)
                assertEquals(Estado.VISTO, ficha.detalle.perfil.estado)
                // CU-08, CU-09
                assertEquals(2, biblioteca.leer().size)
                assertTrue(biblioteca.agrupar(CriterioAgrupacion.GENERO).isNotEmpty())

                // CU-11, CU-12, CU-14
                val top = listas.crear("Top", TipoLista.RANKING)
                val ver = listas.crear("Ver", TipoLista.PRIORIDAD)
                assertEquals(
                    2,
                    listas
                        .observarPanel()
                        .first()
                        .listas.size,
                )
                assertTrue(listas.renombrar(top, "Top histórico"))
                // CU-15, CU-13, CU-17, CU-18
                listas.anadir(top, padrino)
                listas.anadir(top, dark)
                listas.anadir(ver, dark)
                assertTrue(listas.cambiarPrioridad(ver, dark, Prioridad.ALTA))
                assertTrue(listas.mover(top, dark, 1))
                assertEquals(listOf("Dark", "El Padrino I"), listas.leerContenido(top)!!.elementos.map { it.nombre })
                // CU-16, CU-19
                listas.quitar(ver, dark)
                listas.eliminar(ver)
                assertNull(listas.obtener(ver))

                // CU-20
                assertEquals(listOf(padrino), busqueda.buscar(CriteriosBusqueda(nombre = "padrino")).map { it.id })

                // CU-21, CU-22: exportar e importar en una base de datos vacía da los mismos datos.
                val copia = FormatoCopia.leer(FormatoCopia.escribir(copias.exportar()))
                assertEquals(ResumenFusion(2, 0, 1, 0), CopiaRepository(otra).importar(copia))
                assertEquals(Instantanea.contenido(database), Instantanea.contenido(otra))

                // CU-06: borrar un perfil lo quita de sus listas y el ranking queda contiguo.
                perfiles.eliminar(dark)
                assertEquals(listOf(1), listas.elementos(top).map { it.posicion })
            }
        }
}
