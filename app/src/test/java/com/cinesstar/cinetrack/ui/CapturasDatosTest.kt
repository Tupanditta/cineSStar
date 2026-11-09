package com.cinesstar.cinetrack.ui

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.soporte.Capturas
import com.cinesstar.cinetrack.soporte.Capturas.aBuffered
import com.cinesstar.cinetrack.soporte.Capturas.comparar
import com.cinesstar.cinetrack.soporte.Capturas.escalada
import com.cinesstar.cinetrack.soporte.Capturas.guardar
import com.cinesstar.cinetrack.soporte.Capturas.prototipo
import com.cinesstar.cinetrack.soporte.Capturas.recorte
import com.cinesstar.cinetrack.soporte.Capturas.sinTextosBarraSuperior
import com.cinesstar.cinetrack.soporte.FixturePequeno
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.ui.buscar.TagsBuscar
import com.cinesstar.cinetrack.ui.components.tag
import com.cinesstar.cinetrack.ui.datos.TagsDatos
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.awt.image.BufferedImage

/**
 * T8.21 (G): pantalla "Datos Locales" (pestaña de Buscar) y Ajustes frente a `b_squeda_y_copia_de_seguridad/screen.png`.
 * El `screen.png` del prototipo muestra la pestaña "Filtros Cine" (la de "Datos Locales" está oculta en el HTML), así que
 * la comparación automática se limita a las barras, como en T7.23; el contenido sigue el HTML del prototipo con los
 * textos de design.md §12 (sin "SQLite" ni "Commit OK"; "Importar" en vez de "Restaurar"). Las capturas quedan en
 * `app/build/capturas` para revisarlas a mano.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h1138dp-xhdpi")
class CapturasDatosTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private companion object {
        const val BARRA_ESTADO_FALSA_DP = 28
        const val ANCHO_DP = 390
        const val BARRA_SUPERIOR_DP = 52
        const val BARRA_INFERIOR_DP = 64
    }

    private fun captura(): BufferedImage {
        compose.waitForIdle()
        return aBuffered(compose.onRoot().captureToImage().asAndroidBitmap()).escalada(ANCHO_DP)
    }

    @Test
    fun t8_21_datosLocalesYAjustesComoEnElPrototipo() {
        enBd { FixturePequeno.cargar(database) }
        compose.onNodeWithTag(DestinoBarra.BUSCAR.tag()).performClick()
        compose.onNodeWithTag(TagsBuscar.TAB_DATOS).performClick()
        compose.esperar { compose.existe(TagsDatos.TAMANO) }

        val app = captura()
        val ref = prototipo("b_squeda_y_copia_de_seguridad").escalada(ANCHO_DP).recorte(BARRA_ESTADO_FALSA_DP, app.height)
        guardar("datos_app", app)
        val superior =
            comparar(sinTextosBarraSuperior(app.recorte(0, BARRA_SUPERIOR_DP)), sinTextosBarraSuperior(ref.recorte(0, BARRA_SUPERIOR_DP)))
        val inferior =
            comparar(
                app.recorte(app.height - BARRA_INFERIOR_DP, BARRA_INFERIOR_DP),
                ref.recorte(ref.height - BARRA_INFERIOR_DP, BARRA_INFERIOR_DP),
            )
        println("T8.21 Datos Locales: barra superior $superior; barra inferior $inferior")

        compose.onNodeWithTag(TagsBuscar.LISTA).performScrollToNode(hasTestTag(TagsDatos.IMPORTAR))
        guardar("datos_acciones_app", captura())

        compose.onNodeWithTag(DestinoBarra.AJUSTES.tag()).performClick()
        compose.esperar { compose.existe(TagsDatos.LISTA_AJUSTES) && compose.existe(TagsDatos.TAMANO) }
        guardar("ajustes_app", captura())

        assertTrue("Barra superior: $superior fuera de ${Capturas.BARRAS}", superior.cumple(Capturas.BARRAS))
        assertTrue("Barra inferior: $inferior fuera de ${Capturas.BARRAS}", inferior.cumple(Capturas.BARRAS))
    }
}
