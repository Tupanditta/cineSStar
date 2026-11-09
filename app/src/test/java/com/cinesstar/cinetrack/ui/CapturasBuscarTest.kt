package com.cinesstar.cinetrack.ui

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.navigation.DestinoBarra
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
import com.cinesstar.cinetrack.ui.buscar.TagsBuscar
import com.cinesstar.cinetrack.ui.components.tag
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.awt.image.BufferedImage

/**
 * T7.23 (G): pantalla Buscar vacía, con filtros, con resultados y sin resultados frente a
 * `b_squeda_y_copia_de_seguridad/screen.png`. Método en [Capturas]. Como en T4.20 y T5.35, el contenido sigue el estilo
 * del prototipo con la escala tipográfica mayor, sin carátulas, con dos pulgares en el slider y sin director ni año
 * (design.md §12), así que la comparación automática se limita a las barras; las capturas quedan en
 * `app/build/capturas` para revisarlas a mano. Ventana: la del prototipo (390 dp) menos su barra de estado falsa.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h1138dp-xhdpi")
class CapturasBuscarTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private companion object {
        const val BARRA_ESTADO_FALSA_DP = 28
        const val ANCHO_DP = 390
        const val BARRA_SUPERIOR_DP = 52

        /** Cuerpo de la barra inferior, sin el botón "Añadir" que sobresale (como en T5.35). */
        const val BARRA_INFERIOR_DP = 64
    }

    private fun captura(): BufferedImage {
        compose.waitForIdle()
        return aBuffered(compose.onRoot().captureToImage().asAndroidBitmap()).escalada(ANCHO_DP)
    }

    private fun ver(tag: String) {
        compose.onNodeWithTag(TagsBuscar.LISTA).performScrollToNode(hasTestTag(tag))
        compose.waitForIdle()
    }

    /** Títulos del prototipo con datos que existen en la app (sin director ni año, §12). */
    private fun crearDatosDelPrototipo() =
        enBd {
            val generos = catalogo.generos().associate { it.nombre to it.id }

            data class P(
                val nombre: String,
                val tipo: TipoPerfil,
                val nota: Int?,
                val estado: Estado,
                val saga: String?,
                val generos: List<String>,
            )
            listOf(
                P("Severance", TipoPerfil.SERIE, 87, Estado.VIENDO, null, listOf("Suspense", "Ciencia Ficción")),
                P("Oppenheimer", TipoPerfil.PELICULA, 84, Estado.VISTO, null, listOf("Historia", "Drama")),
                P("Blade Runner 2049", TipoPerfil.PELICULA, 85, Estado.VISTO, "Blade Runner", listOf("Ciencia Ficción", "Thriller")),
                P("Dune: Parte Dos", TipoPerfil.PELICULA, 88, Estado.VIENDO, "Dune", listOf("Ciencia Ficción", "Aventura")),
                P("La Mesías", TipoPerfil.SERIE, null, Estado.PENDIENTE, null, listOf("Suspense", "Drama")),
            ).forEach { p ->
                perfiles.crear(
                    NuevoPerfil(
                        nombre = p.nombre,
                        tipo = p.tipo,
                        estado = p.estado,
                        puntuacion = p.nota?.let { Puntuacion.of(it) },
                        sagaNueva = p.saga,
                        generoIds = p.generos.map { generos.getValue(it) }.toSet(),
                    ),
                )
            }
            generos
        }

    @Test
    fun t7_23_buscarComoEnElPrototipo() {
        val generos = crearDatosDelPrototipo()
        compose.onNodeWithTag(DestinoBarra.BUSCAR.tag()).performClick()
        compose.esperar { compose.existeTexto("5 coincidencias") }

        // Vacía: sin filtros (tipo = Todas), todos los títulos.
        val app = captura()
        val ref = prototipo("b_squeda_y_copia_de_seguridad").escalada(ANCHO_DP).recorte(BARRA_ESTADO_FALSA_DP, app.height)
        guardar("buscar_app", app)
        guardar("buscar_prototipo", ref)
        val superior =
            comparar(sinTextosBarraSuperior(app.recorte(0, BARRA_SUPERIOR_DP)), sinTextosBarraSuperior(ref.recorte(0, BARRA_SUPERIOR_DP)))
        val inferior =
            comparar(
                app.recorte(app.height - BARRA_INFERIOR_DP, BARRA_INFERIOR_DP),
                ref.recorte(ref.height - BARRA_INFERIOR_DP, BARRA_INFERIOR_DP),
            )
        println("T7.23 Buscar: barra superior $superior; barra inferior $inferior")

        // Con filtros, como el prototipo: mínimo 7.0 y "Thriller" (más Ciencia Ficción para que salgan dos).
        compose.onNodeWithTag(TagsBuscar.MINIMO).performTextReplacement("7.0")
        ver(TagsBuscar.genero(generos.getValue("Thriller")))
        compose.onNodeWithTag(TagsBuscar.genero(generos.getValue("Thriller"))).performClick()
        compose.esperar { compose.existeTexto("1 coincidencia") }
        compose.onNodeWithTag(TagsBuscar.LISTA).performScrollToNode(hasTestTag(TagsBuscar.RANGO))
        guardar("buscar_filtros_app", captura())

        // Con resultados: "Aplicar Filtros" lleva a ellos.
        compose.onNodeWithTag(TagsBuscar.genero(generos.getValue("Ciencia Ficción"))).performClick()
        compose.esperar { compose.existeTexto("3 coincidencias") }
        ver(TagsBuscar.APLICAR)
        compose.onNodeWithTag(TagsBuscar.APLICAR).performClick()
        guardar("buscar_resultados_app", captura())

        // Sin resultados (M-13).
        compose.onNodeWithTag(TagsBuscar.LISTA).performScrollToNode(hasTestTag(TagsBuscar.NOMBRE))
        compose.onNodeWithTag(TagsBuscar.NOMBRE).performTextReplacement("zzz")
        compose.esperar { compose.existe(TagsBuscar.SIN_RESULTADOS) }
        ver(TagsBuscar.SIN_RESULTADOS)
        guardar("buscar_sin_resultados_app", captura())

        assertTrue("Barra superior: $superior fuera de ${Capturas.BARRAS}", superior.cumple(Capturas.BARRAS))
        assertTrue("Barra inferior: $inferior fuera de ${Capturas.BARRAS}", inferior.cumple(Capturas.BARRAS))
    }
}
