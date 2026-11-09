package com.cinesstar.cinetrack.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Lectura del formato de la copia (CU-22 paso 2, E1, E2, E3) y reglas de fusión en seco (S-20), sin base de datos. */
class FormatoCopiaTest {
    private fun problema(texto: String): ProblemaCopia = assertThrows<CopiaException> { FormatoCopia.leer(texto) }.problema

    private fun perfil(
        nombre: String,
        puntuacion: Double? = null,
    ) = PerfilCopia(nombre, TipoPerfil.PELICULA, puntuacion = puntuacion)

    @Test
    fun noEsJsonOEsDeOtraAppDaM17SinDetalle() {
        listOf(
            "",
            "hola",
            "{\"app\": \"CineTrack\", \"version\": 2, ",
            "[1, 2, 3]",
            "{\"foo\": 1}",
            "{\"app\": \"OtraApp\", \"version\": 2}",
            "{\"app\": \"CineTrack\"}",
            "{\"app\": \"CineTrack\", \"version\": \"2\"}",
            "{\"app\": \"CineTrack\", \"version\": 2, \"perfiles\": [{\"nombre\": \"X\", \"tipo\": \"DOCUMENTAL\"}]}",
            "{\"app\": \"CineTrack\", \"version\": 2, \"perfiles\": [{\"nombre\": \"X\"}]}",
            "{\"app\": \"CineTrack\", \"version\": 2, \"perfiles\": 3}",
            "{\"app\": \"CineTrack\", \"version\": 2, \"desconocido\": true}",
        ).forEach { assertEquals(ProblemaCopia.NoValida(), problema(it), it) }
    }

    @Test
    fun versionMasRecienteDaM18() {
        assertEquals(ProblemaCopia.VersionFutura, problema("{\"app\": \"CineTrack\", \"version\": ${CopiaSeguridad.VERSION_ACTUAL + 1}}"))
    }

    @Test
    fun archivoVacioValidoDeLaVersionActual() {
        val c = FormatoCopia.leer("{\"app\": \"CineTrack\", \"version\": ${CopiaSeguridad.VERSION_ACTUAL}}")
        assertTrue(c.perfiles.isEmpty() && c.listas.isEmpty() && c.sagas.isEmpty())
    }

    @Test
    fun laVersion1SeMigraALaActual() {
        val c =
            FormatoCopia.leer(
                """{"app": "CineTrack", "version": 1, "perfiles": [
                    {"nombre": "A", "tipo": "PELICULA", "puntuacionDecimas": 85},
                    {"nombre": "B", "tipo": "PELICULA", "puntuacionDecimas": null},
                    {"nombre": "C", "tipo": "PELICULA", "puntuacionDecimas": 100}]}""",
            )
        assertEquals(CopiaSeguridad.VERSION_ACTUAL, c.version)
        assertEquals(listOf(8.5, null, 10.0), c.perfiles.map { it.puntuacion })
        assertEquals(
            ProblemaCopia.NoValida(DetalleCopia.PuntuacionInvalida("A")),
            problema("""{"app": "CineTrack", "version": 1, "perfiles": [{"nombre": "A", "tipo": "PELICULA", "puntuacionDecimas": 101}]}"""),
        )
    }

    /** Casos límite de puntuación de sprints.md §1.4, tal como pueden venir en el archivo. */
    @Test
    fun puntuacionesDeLaTablaDeCasosLimite() {
        listOf(
            0.0,
            7.0,
            8.5,
            9.9,
            10.0,
        ).forEach { assertNull(ReglasCopia.validar(CopiaSeguridad(perfiles = listOf(perfil("A", it)))), "$it") }
        listOf(-0.1, -1.0, 10.1, 11.0, 100.0, 7.25, 8.55, Double.NaN).forEach {
            assertEquals(
                DetalleCopia.PuntuacionInvalida("A"),
                ReglasCopia.validar(CopiaSeguridad(perfiles = listOf(perfil("A", it)))),
                "$it",
            )
        }
        assertEquals(Puntuacion.of(85), ReglasCopia.puntuacion(perfil("A", 8.5)))
        assertEquals(Puntuacion.of(100), ReglasCopia.puntuacion(perfil("A", 10.0)))
        assertEquals(8.5, ReglasCopia.valor(Puntuacion.of(85)))
    }

    @Test
    fun escribirYLeerEsLaMismaCopia() {
        val c =
            CopiaSeguridad(
                fecha = "2026-11-09T12:00:00",
                sagas = listOf("Dune"),
                perfiles =
                    listOf(
                        PerfilCopia(
                            "Dune",
                            TipoPerfil.PELICULA,
                            Estado.VISTO,
                            8.5,
                            "«Hola» \"y\" \\ 🎬\n",
                            3L,
                            "Dune",
                            listOf("Drama"),
                            listOf("Inglés"),
                            2L,
                        ),
                    ),
                listas =
                    listOf(
                        ListaCopia(
                            "Top",
                            TipoLista.RANKING,
                            Restriccion.MIXTO,
                            1L,
                            listOf(ElementoCopia("Dune", TipoPerfil.PELICULA, 1, null, 4L)),
                        ),
                    ),
            )
        assertEquals(c, FormatoCopia.leer(FormatoCopia.escribir(c)))
    }

    @Test
    fun nombreDeArchivoPropuesto() {
        assertEquals("cinetrack_backup_2026-11-09.json", FormatoCopia.nombreArchivo(java.time.LocalDate.of(2026, 11, 9)))
    }

    @Test
    fun elementoSinPosicionOPrioridadOListaDuplicadaNoSonValidos() {
        val p = listOf(perfil("A"), perfil("B"))
        val e = ElementoCopia("A", TipoPerfil.PELICULA)
        assertEquals(
            DetalleCopia.ElementoIncompleto("Top", "A"),
            ReglasCopia.validar(
                CopiaSeguridad(perfiles = p, listas = listOf(ListaCopia("Top", TipoLista.RANKING, elementos = listOf(e)))),
            ),
        )
        assertEquals(
            DetalleCopia.ElementoIncompleto("Top", "B"),
            ReglasCopia.validar(
                CopiaSeguridad(
                    perfiles = p,
                    listas =
                        listOf(
                            ListaCopia(
                                "Top",
                                TipoLista.RANKING,
                                elementos = listOf(e.copy(posicion = 1), ElementoCopia("B", TipoPerfil.PELICULA, posicion = 1)),
                            ),
                        ),
                ),
            ),
        )
        assertEquals(
            DetalleCopia.ElementoIncompleto("Ver", "A"),
            ReglasCopia.validar(
                CopiaSeguridad(perfiles = p, listas = listOf(ListaCopia("Ver", TipoLista.PRIORIDAD, elementos = listOf(e)))),
            ),
        )
        assertEquals(
            DetalleCopia.ListaDuplicada("TOP"),
            ReglasCopia.validar(
                CopiaSeguridad(perfiles = p, listas = listOf(ListaCopia("Top", TipoLista.NORMAL), ListaCopia("TOP", TipoLista.NORMAL))),
            ),
        )
        assertEquals(DetalleCopia.NombreVacio, ReglasCopia.validar(CopiaSeguridad(perfiles = listOf(perfil("  ")))))
    }

    // ---- Fusión en seco ----

    private val vacio = EstadoLocal()

    @Test
    fun fusionConLaBaseVaciaLoCreaTodo() {
        val c =
            CopiaSeguridad(
                perfiles = listOf(perfil("A"), perfil("B")),
                listas = listOf(ListaCopia("L", TipoLista.NORMAL)),
                sagas = listOf("S", " s "),
            )
        val plan = Fusion.planificar(c, vacio)
        assertEquals(ResumenFusion(2, 0, 1, 0), plan.resumen)
        assertEquals(listOf("S"), plan.sagasNuevas)
    }

    @Test
    fun losNombresDeOtrasListasDeLaCopiaQuedanReservados() {
        val local = EstadoLocal(listas = mapOf("x" to ListaLocal(1, "X", TipoLista.NORMAL, Restriccion.MIXTO, emptySet())))
        val c =
            CopiaSeguridad(
                listas = listOf(ListaCopia("X", TipoLista.RANKING), ListaCopia("X (importada)", TipoLista.NORMAL)),
            )
        val plan = Fusion.planificar(c, local)
        assertEquals(listOf("X (importada 2)", "X (importada)"), plan.listasNuevas.map { it.nombre })
    }
}
