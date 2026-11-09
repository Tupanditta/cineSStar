package com.cinesstar.cinetrack.ui

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.RangoPuntuacion
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.soporte.Capturas
import com.cinesstar.cinetrack.soporte.Capturas.aBuffered
import com.cinesstar.cinetrack.soporte.Capturas.comparar
import com.cinesstar.cinetrack.soporte.Capturas.escalada
import com.cinesstar.cinetrack.soporte.Capturas.guardar
import com.cinesstar.cinetrack.soporte.Capturas.prototipo
import com.cinesstar.cinetrack.soporte.Capturas.recorte
import com.cinesstar.cinetrack.soporte.Capturas.sinTextosBarraSuperior
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.soporte.existeTexto
import com.cinesstar.cinetrack.ui.biblioteca.TagsBiblioteca
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

/**
 * T4.20 (G): Biblioteca con datos, filtrada y vacía frente a `biblioteca_de_medios/screen.png`. Método en [Capturas].
 * El contenido sigue el estilo del prototipo con otras proporciones a petición del usuario (design.md §12: textos
 * mayores, sin franja ni pie, icono de tipo en lugar de póster), así que la comparación automática se limita a las
 * barras y al alto de la tarjeta; las capturas quedan en `app/build/capturas` para revisarlas a mano.
 * La ventana es la del prototipo (390 dp, viewport de Stitch) menos su barra de estado falsa (28 dp, §3.1).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h2241dp-xhdpi")
class CapturasBibliotecaTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private companion object {
        const val BARRA_ESTADO_FALSA_DP = 28
        const val ANCHO_DP = 390
        const val BARRA_SUPERIOR_DP = 52
        const val BARRA_INFERIOR_DP = 84
        const val ALTO_TARJETA_DP = 160
    }

    private fun captura(): BufferedImage {
        compose.waitForIdle()
        return aBuffered(compose.onRoot().captureToImage().asAndroidBitmap()).escalada(ANCHO_DP)
    }

    private fun referencia(altoDp: Int): BufferedImage =
        prototipo("biblioteca_de_medios").escalada(ANCHO_DP).recorte(BARRA_ESTADO_FALSA_DP, altoDp)

    /** Bordes superior e inferior (dp) de un nodo. */
    private fun limites(tag: String): Pair<Int, Int> {
        val b = compose.onNodeWithTag(tag).getBoundsInRoot()
        return b.top.value.roundToInt() to b.bottom.value.roundToInt()
    }

    /** Los diez títulos del prototipo, con los géneros que no existen en el catálogo sustituidos por uno parecido. */
    private fun crearDatosDelPrototipo(): List<Long> =
        enBd {
            val generos = catalogo.generos().associate { it.nombre to it.id }
            val idiomas = catalogo.idiomas().associate { it.nombre to it.id }

            data class P(
                val nombre: String,
                val tipo: TipoPerfil,
                val nota: Int?,
                val estado: Estado,
                val saga: String?,
                val generos: List<String>,
                val idioma: String,
            )
            listOf(
                P("La Mesías", TipoPerfil.SERIE, null, Estado.PENDIENTE, null, listOf("Suspense", "Drama"), "Español"),
                P(
                    "Blade Runner 2049",
                    TipoPerfil.PELICULA,
                    null,
                    Estado.PENDIENTE,
                    "Blade Runner",
                    listOf("Ciencia Ficción", "Thriller"),
                    "Inglés",
                ),
                P("Shogun", TipoPerfil.SERIE, 88, Estado.VIENDO, null, listOf("Acción", "Historia"), "Japonés"),
                P("Oppenheimer", TipoPerfil.PELICULA, 84, Estado.VISTO, null, listOf("Historia", "Drama"), "Inglés"),
                P("Succession", TipoPerfil.SERIE, 89, Estado.VISTO, null, listOf("Drama", "Comedia"), "Inglés"),
                P("El Secreto de sus Ojos", TipoPerfil.PELICULA, 82, Estado.VISTO, null, listOf("Suspense", "Romance"), "Español"),
                P("Breaking Bad", TipoPerfil.SERIE, 95, Estado.VISTO, "Breaking Bad", listOf("Thriller", "Crimen"), "Inglés"),
                P("El Padrino", TipoPerfil.PELICULA, 100, Estado.VISTO, "The Godfather", listOf("Crimen", "Drama"), "Inglés"),
                P("Severance", TipoPerfil.SERIE, 87, Estado.VIENDO, null, listOf("Suspense", "Ciencia Ficción"), "Inglés"),
                P("Dune: Parte Dos", TipoPerfil.PELICULA, 94, Estado.VIENDO, "Dune", listOf("Ciencia Ficción", "Aventura"), "Inglés"),
            ).map { p ->
                perfiles.crear(
                    NuevoPerfil(
                        nombre = p.nombre,
                        tipo = p.tipo,
                        estado = p.estado,
                        puntuacion = p.nota?.let { Puntuacion.of(it) },
                        sagaNueva = p.saga,
                        generoIds = p.generos.map { generos.getValue(it) }.toSet(),
                        idiomaIds = setOf(idiomas.getValue(p.idioma)),
                    ),
                )
            }
        }

    @Test
    fun t4_20_bibliotecaComoEnElPrototipo() {
        val ids = crearDatosDelPrototipo()
        compose.esperar { compose.existeTexto("Todas (10)") && compose.existe(TagsBiblioteca.fila(ids.last())) }

        val app = captura()
        val ref = referencia(app.height)
        guardar("biblioteca_app", app)
        guardar("biblioteca_prototipo", ref)
        // Barras: iguales al prototipo salvo los textos quitados de la superior (§12).
        val superior =
            comparar(sinTextosBarraSuperior(app.recorte(0, BARRA_SUPERIOR_DP)), sinTextosBarraSuperior(ref.recorte(0, BARRA_SUPERIOR_DP)))
        val inferior =
            comparar(
                app.recorte(app.height - BARRA_INFERIOR_DP, BARRA_INFERIOR_DP),
                ref.recorte(ref.height - BARRA_INFERIOR_DP, BARRA_INFERIOR_DP),
            )
        println("T4.20 Biblioteca: barra superior $superior; barra inferior $inferior")
        // La tarjeta conserva el alto del prototipo (póster 144 + padding 8 × 2) aunque el contenido sea mayor.
        val alto = limites(TagsBiblioteca.fila(ids.last())).let { (arriba, abajo) -> abajo - arriba }

        // Variantes para revisión manual: filtrada por "9 - 10" y vacía.
        compose.onNodeWithTag(TagsBiblioteca.rango(RangoPuntuacion.DE_9_A_10)).performClick()
        compose.esperar { compose.existeTexto("Mostrando 3 de 10") }
        guardar("biblioteca_filtrada_app", captura())
        enBd { biblioteca.leer().forEach { perfiles.eliminar(it.id) } }
        compose.esperar { compose.existe(TagsBiblioteca.VACIA) }
        guardar("biblioteca_vacia_app", captura())

        assertEquals("Alto de la tarjeta de medio (dp)", ALTO_TARJETA_DP, alto)
        assertTrue("Barra superior: $superior fuera de ${Capturas.BARRAS}", superior.cumple(Capturas.BARRAS))
        assertTrue("Barra inferior: $inferior fuera de ${Capturas.BARRAS}", inferior.cumple(Capturas.BARRAS))
    }
}
