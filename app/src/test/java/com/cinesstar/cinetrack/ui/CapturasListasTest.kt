package com.cinesstar.cinetrack.ui

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.soporte.Capturas
import com.cinesstar.cinetrack.soporte.Capturas.aBuffered
import com.cinesstar.cinetrack.soporte.Capturas.comparar
import com.cinesstar.cinetrack.soporte.Capturas.escalada
import com.cinesstar.cinetrack.soporte.Capturas.guardar
import com.cinesstar.cinetrack.soporte.Capturas.prototipo
import com.cinesstar.cinetrack.soporte.Capturas.recorte
import com.cinesstar.cinetrack.soporte.Capturas.sinTextosBarraSuperior
import com.cinesstar.cinetrack.soporte.RobotListas
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.ui.components.TagsHoja
import com.cinesstar.cinetrack.ui.lista.TagsLista
import com.cinesstar.cinetrack.ui.listas.TagsListas
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.awt.image.BufferedImage

/**
 * T5.35 (G): "Mis Listas", la lista abierta (ranking y normal) y los diálogos frente a `gesti_n_de_listas/screen.png` y
 * `lista_ranking_top_10_hist_rico/screen.png`. Método en [Capturas]. Como en T4.20, el contenido sigue el estilo del
 * prototipo con la escala tipográfica mayor y sin carátulas (design.md §12), así que la comparación automática se
 * limita a las barras; las capturas quedan en `app/build/capturas` para revisarlas a mano.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CapturasListasTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val listas = RobotListas(compose)

    private companion object {
        const val BARRA_ESTADO_FALSA_DP = 28
        const val ANCHO_DP = 390
        const val BARRA_SUPERIOR_DP = 52

        /**
         * Cuerpo de la barra inferior (64 dp, sin los 20 dp del botón "Añadir" que sobresale): con la escala tipográfica
         * mayor (§12) "+ Nueva Lista" queda justo encima de la barra, donde el prototipo tiene fondo.
         */
        const val BARRA_INFERIOR_DP = 64
    }

    private fun captura(): BufferedImage {
        compose.waitForIdle()
        return aBuffered(compose.onRoot().captureToImage().asAndroidBitmap()).escalada(ANCHO_DP)
    }

    private fun referencia(
        pantalla: String,
        altoDp: Int,
    ): BufferedImage = prototipo(pantalla).escalada(ANCHO_DP).recorte(BARRA_ESTADO_FALSA_DP, altoDp)

    private fun barraSuperior(
        app: BufferedImage,
        ref: BufferedImage,
    ) = comparar(sinTextosBarraSuperior(app.recorte(0, BARRA_SUPERIOR_DP)), sinTextosBarraSuperior(ref.recorte(0, BARRA_SUPERIOR_DP)))

    /** Las cuatro listas del prototipo, con títulos de ejemplo. Devuelve los ids: ranking, prioridad, normal, series. */
    private fun crearDatosDelPrototipo(): List<Long> =
        enBd {
            val peliculas =
                listOf(
                    "El Padrino" to 100,
                    "Dune: Parte Dos" to 98,
                    "El Secreto de sus Ojos" to 96,
                    "Oppenheimer" to 94,
                    "Blade Runner 2049" to 92,
                    "Alien" to 90,
                    "Interstellar" to 89,
                    "Matrix" to 88,
                    "Gattaca" to 85,
                    "Contact" to 80,
                ).map { (n, nota) -> perfiles.crear(NuevoPerfil(n, TipoPerfil.PELICULA, puntuacion = Puntuacion.of(nota))) }
            val series =
                listOf("Breaking Bad", "Severance", "Succession", "Shogun", "La Mesías", "Dark").map {
                    perfiles.crear(NuevoPerfil(it, TipoPerfil.SERIE))
                }
            val soloSeries = listas.crear("Series pendientes del año", TipoLista.NORMAL, Restriccion.SOLO_SERIES)
            series.forEach { listas.anadir(soloSeries, it) }
            val clasicos = listas.crear("Clásicos de Ciencia Ficción", TipoLista.NORMAL, Restriccion.SOLO_PELICULAS)
            peliculas.take(8).forEach { listas.anadir(clasicos, it) }
            val proximas = listas.crear("Próximas a ver", TipoLista.PRIORIDAD)
            (peliculas + series.take(4)).forEachIndexed { i, p ->
                listas.anadir(proximas, p, prioridad = if (i < 4) Prioridad.ALTA else Prioridad.MEDIA)
            }
            val top = listas.crear("Top 10 Histórico", TipoLista.RANKING, Restriccion.SOLO_PELICULAS)
            peliculas.forEach { listas.anadir(top, it) }
            listOf(top, proximas, clasicos, soloSeries)
        }

    @Test
    @Config(qualifiers = "w390dp-h1017dp-xhdpi")
    fun t5_35_misListasComoEnElPrototipo() {
        val ids = crearDatosDelPrototipo()
        listas.abrir()
        compose.esperar { ids.all { compose.existe(TagsListas.tarjeta(it)) } }

        val app = captura()
        val ref = referencia("gesti_n_de_listas", app.height)
        guardar("listas_app", app)
        guardar("listas_prototipo", ref)
        val superior = barraSuperior(app, ref)
        val inferior =
            comparar(
                app.recorte(app.height - BARRA_INFERIOR_DP, BARRA_INFERIOR_DP),
                ref.recorte(ref.height - BARRA_INFERIOR_DP, BARRA_INFERIOR_DP),
            )
        println("T5.35 Listas: barra superior $superior; barra inferior $inferior")

        // Diálogo "Nueva Lista" (sin captura en el prototipo): revisión manual.
        listas.abrirNueva()
        listas.escribirNombre("Joyas Ocultas")
        guardar("listas_dialogo_nueva_app", captura())

        assertTrue("Barra superior: $superior fuera de ${Capturas.BARRAS}", superior.cumple(Capturas.BARRAS))
        assertTrue("Barra inferior: $inferior fuera de ${Capturas.BARRAS}", inferior.cumple(Capturas.BARRAS))
    }

    @Test
    @Config(qualifiers = "w390dp-h1476dp-xhdpi")
    fun t5_35_listaAbiertaComoEnElPrototipo() {
        val ids = crearDatosDelPrototipo()
        listas.abrir()
        listas.abrirLista(ids[0])
        compose.esperar { compose.existe(TagsLista.fila(enBd { listas.elementos(ids[0]) }.last().perfilId)) }

        val app = captura()
        val ref = referencia("lista_ranking_top_10_hist_rico", app.height)
        guardar("lista_ranking_app", app)
        guardar("lista_ranking_prototipo", ref)
        val superior = barraSuperior(app, ref)
        println("T5.35 Lista abierta: barra superior $superior")

        // Variantes para revisión manual: selector "Añadir título", hoja "Quitar", lista normal y lista vacía.
        listas.abrirSelector()
        guardar("lista_selector_app", captura())
        listas.cerrarSelector()
        val primero = enBd { listas.elementos(ids[0]) }.first().perfilId
        listas.pedirQuitar(primero)
        guardar("lista_hoja_quitar_app", captura())
        listas.tocar(TagsHoja.CANCELAR)
        compose.esperar { !compose.existe(TagsHoja.HOJA) }
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        listas.abrirLista(ids[2])
        guardar("lista_normal_app", captura())

        assertTrue("Barra superior: $superior fuera de ${Capturas.BARRAS}", superior.cumple(Capturas.BARRAS))
    }
}
