package com.cinesstar.cinetrack.ui

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.soporte.Capturas
import com.cinesstar.cinetrack.soporte.Capturas.aBuffered
import com.cinesstar.cinetrack.soporte.Capturas.comparar
import com.cinesstar.cinetrack.soporte.Capturas.escalada
import com.cinesstar.cinetrack.soporte.Capturas.guardar
import com.cinesstar.cinetrack.soporte.Capturas.prototipo
import com.cinesstar.cinetrack.soporte.Capturas.recorte
import com.cinesstar.cinetrack.soporte.RobotFormulario
import com.cinesstar.cinetrack.soporte.enBd
import com.cinesstar.cinetrack.soporte.esperar
import com.cinesstar.cinetrack.soporte.existe
import com.cinesstar.cinetrack.ui.biblioteca.TagsBiblioteca
import com.cinesstar.cinetrack.ui.perfil.TagsDetalle
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.awt.image.BufferedImage
import kotlin.math.abs

/**
 * T2.31 y T3.32 (G): "Añadir perfil" y "Detalle" comparados con su `screen.png`. Método en [Capturas]; tolerancia de pantalla
 * completa en [PANTALLA]. Las variantes "con datos" y "con error" de "Añadir perfil" no tienen captura en el prototipo:
 * se generan en `app/build/capturas` para revisarlas a mano.
 *
 * Cada ventana tiene el alto de la captura del prototipo menos su barra de estado falsa (28 dp, §3.1), para que todo
 * el contenido quepa sin desplazar.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CapturasPerfilTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private companion object {
        const val BARRA_ESTADO_FALSA_DP = 28

        /** Las capturas de Stitch de estas dos pantallas son de un viewport de 390 px CSS (= 390 dp). */
        const val ANCHO_DP = 390

        /**
         * Pantalla completa. Medido en el S2 con las diferencias documentadas: Añadir 13.3 / 12.9 % (lista completa de
         * géneros e idiomas, dos filas más que desplazan lo de abajo; texto del aviso de §12). Detalle, medido en el S3
         * con "Ajustar", menú ⋮, iconos de la crítica y "Eliminar": 14.9 / 13.2 % (avatar canónico de §13; textos de
         * ejemplo distintos; sin "+ Añadir a lista" ni flecha de saga hasta el S4/S5); el bloque "Eliminar", alineado
         * aparte, 3.6 / 1.7 %. Detalle sin datos, hasta la ficha: 14.1 / 11.6 %.
         * Un bloque de otro color o tamaño (≈ 10 % de la pantalla) sube la media más de 3 puntos y lo detecta.
         */
        val PANTALLA = Capturas.Tolerancia(media = 15.0, maxCeldasDistintas = 0.15)
    }

    private fun captura(anchoDp: Int): BufferedImage {
        compose.waitForIdle()
        return aBuffered(compose.onRoot().captureToImage().asAndroidBitmap()).escalada(anchoDp)
    }

    private fun referencia(
        pantalla: String,
        anchoDp: Int,
        altoDp: Int,
    ): BufferedImage = prototipo(pantalla).escalada(anchoDp).recorte(BARRA_ESTADO_FALSA_DP, altoDp)

    @Test
    @Config(qualifiers = "w390dp-h1210dp-xhdpi")
    fun t2_31_anadirPerfilComoEnElPrototipo() {
        val form = RobotFormulario(compose)
        form.abrir()
        val vacia = captura(ANCHO_DP)
        val ref = referencia("a_adir_nuevo_perfil", ANCHO_DP, vacia.height)
        guardar("anadir_perfil_app", vacia)
        guardar("anadir_perfil_prototipo", ref)
        val resultado = comparar(vacia, ref)
        println("T2.31 Añadir perfil (vacío): $resultado")

        // Variantes para revisión manual.
        form.nombre("Blade Runner 2049")
        form.estado(Estado.VISTO)
        form.puntuacion("8.5")
        form.chip("Ciencia Ficción")
        form.chip("Drama")
        form.chip("Inglés")
        form.comentario("Fotografía de Roger Deakins.")
        form.esperarHabilitado()
        guardar("anadir_perfil_con_datos_app", captura(ANCHO_DP))
        form.puntuacion("11")
        form.esperarTexto("Puntuación no válida. Introduce un valor entre 0.0 y 10 con un máximo de un decimal.")
        guardar("anadir_perfil_con_error_app", captura(ANCHO_DP))

        assertTrue("Añadir perfil: $resultado fuera de $PANTALLA", resultado.cumple(PANTALLA))
    }

    /** Mismos datos que el prototipo, salvo los que no existen en la app (design.md §12). */
    private fun crearPerfilDelPrototipo(): Long =
        enBd {
            val saga = sagas.crear("Dune")
            val p =
                perfiles.crear(
                    NuevoPerfil(
                        "Dune: Parte Dos",
                        TipoPerfil.PELICULA,
                        Estado.VISTO,
                        Puntuacion.of(94),
                        comentario =
                            "Experiencia audiovisual abrumadora. El diseño sonoro de Hans Zimmer en IMAX te hace sentir la " +
                                "vibración de los gusanos de arena. Feyd-Rautha es intimidante y el clímax político prepara " +
                                "a la perfección la tercera entrega.",
                        sagaId = saga,
                        generoIds =
                            catalogo
                                .generos()
                                .filter {
                                    it.nombre in
                                        setOf(
                                            "Ciencia Ficción",
                                            "Aventura",
                                            "Drama",
                                        )
                                }.map { it.id }
                                .toSet(),
                        idiomaIds =
                            catalogo
                                .idiomas()
                                .filter { it.nombre in setOf("Español", "Inglés") }
                                .map { it.id }
                                .toSet(),
                    ),
                )
            perfiles.crear(NuevoPerfil("Dune", TipoPerfil.PELICULA, sagaId = saga))
            listas.anadir(listas.crear("Favoritas 2024", TipoLista.RANKING), p)
            listas.anadir(listas.crear("Maratón Otoño", TipoLista.PRIORIDAD), p, prioridad = Prioridad.ALTA)
            p
        }

    @Test
    @Config(qualifiers = "w390dp-h1572dp-xhdpi")
    fun t2_31_detalleComoEnElPrototipo() {
        val id = crearPerfilDelPrototipo()
        compose.esperar { compose.existe(TagsBiblioteca.fila(id)) }
        compose.onNodeWithTag(TagsBiblioteca.fila(id)).performClick()
        compose.esperar {
            compose.existe(
                TagsDetalle.TITULO,
            ) &&
                compose.onAllNodes(hasText("Ranking #1")).fetchSemanticsNodes().isNotEmpty()
        }

        val app = captura(ANCHO_DP)
        val ref = referencia("detalle_del_perfil", ANCHO_DP, app.height)
        guardar("detalle_app", app)
        guardar("detalle_prototipo", ref)
        // El hero del prototipo es una foto con carátula; en la app no hay carátulas (design.md §12).
        val hero = (BARRA_SUPERIOR_DP until BARRA_SUPERIOR_DP + ALTO_HERO_DP)
        // El contenido de la app es más bajo (sin "+ Añadir a lista" hasta el S5, textos de ejemplo más cortos): el
        // bloque "Eliminar" (§7.7) queda a otra altura y se compara aparte, alineado por su fondo `error-container`.
        val eliminarApp = filaEliminar(app)
        val eliminarRef = filaEliminar(ref)
        val resultado = comparar(app, ref, excluirFilasDp = listOf(hero, minOf(eliminarApp, eliminarRef) until Int.MAX_VALUE))
        val eliminar = comparar(app.recorte(eliminarApp, ALTO_ELIMINAR_DP), ref.recorte(eliminarRef, ALTO_ELIMINAR_DP))
        println("T2.31/T3.32 Detalle: $resultado; bloque Eliminar: $eliminar")
        assertTrue("Bloque Eliminar: $eliminar fuera de ${Capturas.BARRAS}", eliminar.cumple(Capturas.BARRAS))

        // T3.32: variantes sin captura en el prototipo, para revisión manual (estados, editor de nota abierto).
        compose.onNodeWithTag(TagsDetalle.estado(Estado.VIENDO)).performClick()
        compose.esperar { runCatching { compose.onNodeWithTag(TagsDetalle.estado(Estado.VIENDO)).assertIsSelected() }.isSuccess }
        compose.onNodeWithTag(TagsDetalle.AJUSTAR).performClick()
        compose.esperar { compose.existe(TagsDetalle.EDITOR_NOTA) }
        guardar("detalle_viendo_editor_nota_app", captura(ANCHO_DP))

        assertTrue("Detalle: $resultado fuera de $PANTALLA", resultado.cumple(PANTALLA))
    }

    @Test
    @Config(qualifiers = "w390dp-h1572dp-xhdpi")
    fun t3_32_detalleSinPuntuacionNiCriticaEnPendiente() {
        val id = enBd { perfiles.crear(NuevoPerfil("Solo nombre", TipoPerfil.SERIE)) }
        compose.esperar { compose.existe(TagsBiblioteca.fila(id)) }
        compose.onNodeWithTag(TagsBiblioteca.fila(id)).performClick()
        compose.esperar { compose.existe(TagsDetalle.TITULO) }
        val app = captura(ANCHO_DP)
        guardar("detalle_sin_datos_app", app)
        // Sin datos del prototipo que comparar: solo se comprueba la estructura (fondo, tarjetas y botón Eliminar) en
        // la parte de arriba, que es igual al prototipo salvo los valores (hero, estado, nota y ficha).
        val ref = referencia("detalle_del_perfil", ANCHO_DP, app.height)
        val resultado =
            comparar(
                app,
                ref,
                excluirFilasDp =
                    listOf(
                        BARRA_SUPERIOR_DP until BARRA_SUPERIOR_DP + ALTO_HERO_DP,
                        FIN_FICHA_DP until Int.MAX_VALUE,
                    ),
            )
        println("T3.32 Detalle sin datos (hasta la ficha): $resultado")
        assertTrue("Detalle sin datos: $resultado fuera de $PANTALLA", resultado.cumple(PANTALLA))
    }
}

private const val BARRA_SUPERIOR_DP = 52

/** 224 dp de fondo + 160 de póster − 64 de solape + 16 de padding (design.md §7.1). */
private const val ALTO_HERO_DP = 336

/** Botón "Eliminar de mi catálogo" y su nota (design.md §7.7). */
private const val ALTO_ELIMINAR_DP = 100

/** Primera fila (dp) en la que más de la mitad del ancho es `error-container` (#93000A): el botón "Eliminar". */
private fun filaEliminar(imagen: BufferedImage): Int =
    (0 until imagen.height).first { y ->
        (0 until imagen.width).count { x ->
            val rgb = imagen.getRGB(x, y)
            abs((rgb shr 16 and 0xFF) - 0x93) < 24 && (rgb shr 8 and 0xFF) < 24 && abs((rgb and 0xFF) - 0x0A) < 24
        } > imagen.width / 2
    }

/**
 * Fin de la tarjeta "Ficha Técnica Local" en la captura del prototipo. Por debajo, un perfil sin listas ni crítica
 * tiene tarjetas más bajas y todo sube: no se compara.
 */
private const val FIN_FICHA_DP = 900
