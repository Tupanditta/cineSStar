package com.cinesstar.cinetrack.data

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.data.repository.ListaRepository
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.repository.SagaRepository
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.data.room.ListaDao
import com.cinesstar.cinetrack.data.room.ListaItemEntity
import com.cinesstar.cinetrack.data.room.PerfilEntity
import com.cinesstar.cinetrack.domain.DominioException
import com.cinesstar.cinetrack.domain.ErrorDominio
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.GENEROS_PREDEFINIDOS
import com.cinesstar.cinetrack.domain.IDIOMAS_PREDEFINIDOS
import com.cinesstar.cinetrack.domain.Normalizador
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.RangoPuntuacion
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import com.cinesstar.cinetrack.soporte.FixtureGrande
import com.cinesstar.cinetrack.soporte.FixturePequeno
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

/** Tests D del S1: Room en memoria (o en archivo para la persistencia) sobre Robolectric. */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class BaseDatosTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private lateinit var db: CineDatabase
    private lateinit var perfiles: PerfilRepository
    private lateinit var sagas: SagaRepository
    private lateinit var listas: ListaRepository

    /** Reloj controlado: cada lectura avanza 1 ms, así el orden por antigüedad es determinista. */
    private var instante = 1_000_000L
    private val reloj: () -> Long = { instante++ }

    @Before
    fun abrir() {
        db = CineDatabase.construir(context, nombre = null)
        perfiles = PerfilRepository(db, reloj = reloj)
        sagas = SagaRepository(db)
        listas = ListaRepository(db, reloj = reloj)
    }

    @After
    fun cerrar() = db.close()

    private fun prueba(bloque: suspend () -> Unit) = runBlocking { bloque() }

    private suspend fun perfil(
        nombre: String,
        tipo: TipoPerfil = TipoPerfil.PELICULA,
        decimas: Int? = null,
    ) = perfiles.crear(NuevoPerfil(nombre, tipo, puntuacion = decimas?.let { Puntuacion.of(it) }))

    private fun entidad(
        nombre: String,
        tipo: TipoPerfil,
    ) = PerfilEntity(
        nombre = nombre,
        nombreBusqueda = Normalizador.normalizar(nombre),
        tipo = tipo,
        estado = Estado.PENDIENTE,
        puntuacionDecimas = null,
        comentario = null,
        comentarioEditadoEn = null,
        sagaId = null,
        caratulaUri = null,
        creadoEn = 0,
    )

    private fun error(bloque: suspend () -> Unit): ErrorDominio =
        try {
            runBlocking { bloque() }
            fail("Se esperaba un error de dominio")
            error("inalcanzable")
        } catch (e: DominioException) {
            e.error
        }

    private fun columnas(tabla: String): Set<String> =
        db.openHelper.readableDatabase.query("PRAGMA table_info($tabla)").use { c ->
            buildSet { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) }
        }

    private fun contar(sql: String): Int =
        db.openHelper.readableDatabase.query(sql).use { c ->
            c.moveToFirst()
            c.getInt(0)
        }

    // ---------- Perfiles ----------

    @Test
    fun t1_04_mismoNombreNormalizadoYTipoViolaLaUnicidad() =
        prueba {
            db.perfilDao().insertar(entidad("El Padrino", TipoPerfil.PELICULA))
            try {
                db.perfilDao().insertar(entidad("  el  PÁDRINO ", TipoPerfil.PELICULA))
                fail("La base de datos debía rechazar el duplicado")
            } catch (e: SQLiteConstraintException) {
                assertTrue(e.message.orEmpty().contains("UNIQUE"))
            }
            // El repositorio lo traduce a M-02 (CU-01 E2).
            assertEquals(ErrorDominio.PerfilDuplicado(TipoPerfil.PELICULA), error { perfil("EL pádrino") })
            assertEquals(1, db.perfilDao().contar())
        }

    @Test
    fun t1_05_mismoNombreYDistintoTipoSeInsertanAmbos() =
        prueba {
            val a = perfil("Dune", TipoPerfil.PELICULA)
            val b = perfil("dune", TipoPerfil.SERIE)
            assertNotEquals(a, b)
            assertEquals(2, db.perfilDao().contar())
        }

    @Test
    fun t1_06_perfilConSoloNombreYTipo() =
        prueba {
            assertEquals(ErrorDominio.NombreObligatorio, error { perfil("   ") })
            val id = perfil("  Solo   nombre ")
            val detalle = perfiles.obtener(id)!!
            with(detalle.perfil) {
                assertEquals("Solo nombre", nombre)
                assertEquals(TipoPerfil.PELICULA, tipo)
                assertEquals(Estado.PENDIENTE, estado)
                assertNull(puntuacionDecimas)
                assertNull(comentario)
                assertNull(comentarioEditadoEn)
                assertNull(sagaId)
                assertNull(caratulaUri)
            }
            assertTrue(detalle.generos.isEmpty())
            assertTrue(detalle.idiomas.isEmpty())
            assertNull(detalle.saga)
        }

    @Test
    fun t1_07_cadaEstadoConYSinPuntuacion() =
        prueba {
            var n = 0
            for (estado in Estado.entries) {
                for (decimas in listOf(null, 75)) {
                    val id = perfiles.crear(NuevoPerfil("Perfil ${n++}", TipoPerfil.SERIE, estado, decimas?.let { Puntuacion.of(it) }))
                    val guardado = perfiles.obtener(id)!!
                    assertEquals(estado, guardado.perfil.estado)
                    assertEquals(decimas, guardado.perfil.puntuacionDecimas)
                }
            }
            assertEquals(6, db.perfilDao().contar())
        }

    @Test
    fun t1_08_unaSerieTieneUnaSolaPuntuacionYNingunCampoDeProgreso() =
        prueba {
            val cols = columnas("perfil")
            assertEquals(1, cols.count { "puntuacion" in it.lowercase() })
            listOf("progreso", "temporada", "episodio", "capitulo", "minuto").forEach { prohibida ->
                assertTrue("Columna de progreso \"$prohibida\" en perfil: $cols", cols.none { prohibida in it.lowercase() })
            }
            val id = perfiles.crear(NuevoPerfil("Dark", TipoPerfil.SERIE, Estado.VIENDO, Puntuacion.of(88)))
            assertEquals(88, perfiles.obtener(id)!!.perfil.puntuacionDecimas)
        }

    @Test
    fun t1_09_variosGenerosEIdiomasPorPerfil() =
        prueba {
            val generos = db.catalogoDao().generos()
            val idiomas = db.catalogoDao().idiomas()
            val id =
                perfiles.crear(
                    NuevoPerfil(
                        "Amélie",
                        TipoPerfil.PELICULA,
                        generoIds = generos.take(3).map { it.id }.toSet(),
                        idiomaIds = idiomas.take(2).map { it.id }.toSet(),
                    ),
                )
            val detalle = perfiles.obtener(id)!!
            assertEquals(generos.take(3), detalle.generos)
            assertEquals(idiomas.take(2), detalle.idiomas)
        }

    @Test
    fun t1_10_unaSolaSagaYReasignarlaReemplaza() =
        prueba {
            val cols = columnas("perfil")
            assertEquals("Una sola columna de saga en perfil", setOf("sagaId"), cols.filter { "saga" in it.lowercase() }.toSet())
            val a = sagas.crear("El Señor de los Anillos")
            val b = sagas.crear("El Hobbit")
            val id = perfil("La Comunidad del Anillo")
            perfiles.asignarSaga(id, a)
            assertEquals(a, perfiles.obtener(id)!!.saga!!.id)
            perfiles.asignarSaga(id, b)
            assertEquals(b, perfiles.obtener(id)!!.saga!!.id)
            perfiles.asignarSaga(id, null)
            assertNull(perfiles.obtener(id)!!.saga)
            // S-08: las sagas siguen registradas aunque no tengan perfiles.
            assertEquals(2, sagas.todas().size)
        }

    @Test
    fun t1_11_sagaSeReutilizaPorNombreNormalizadoYLaDuplicadaFalla() =
        prueba {
            val id = sagas.obtenerOCrear("Star Wars")
            assertEquals(id, sagas.obtenerOCrear("  STAR   wars "))
            assertEquals("Star Wars", sagas.todas().single().nombre)
            assertEquals(ErrorDominio.SagaDuplicada, error { sagas.crear("star wárs") })
            assertEquals(ErrorDominio.SagaNombreObligatorio, error { sagas.crear(" ") })
            try {
                db.sagaDao().insertar(
                    com.cinesstar.cinetrack.data.room
                        .SagaEntity(nombre = "x", nombreBusqueda = "star wars"),
                )
                fail("La base de datos debía rechazar la saga duplicada")
            } catch (_: SQLiteConstraintException) {
            }
            assertEquals(1, sagas.todas().size)
        }

    // ---------- Arranque (CU-23) y persistencia ----------

    @Test
    fun t1_12_laPrimeraEjecucionPrecargaYLaSegundaNoDuplica() =
        prueba {
            val nombre = "precarga_${System.nanoTime()}.db"
            context.deleteDatabase(nombre)
            val primera = CineDatabase.construir(context, nombre)
            assertEquals(GENEROS_PREDEFINIDOS, primera.catalogoDao().generos().map { it.nombre })
            assertEquals(IDIOMAS_PREDEFINIDOS, primera.catalogoDao().idiomas().map { it.nombre })
            primera.close()

            val segunda = CineDatabase.construir(context, nombre)
            assertEquals(GENEROS_PREDEFINIDOS, segunda.catalogoDao().generos().map { it.nombre })
            assertEquals(IDIOMAS_PREDEFINIDOS, segunda.catalogoDao().idiomas().map { it.nombre })
            segunda.close()
            context.deleteDatabase(nombre)
        }

    @Test
    fun t1_19_losDatosPersistenAlCerrarYReabrir() =
        prueba {
            val nombre = "persistencia_${System.nanoTime()}.db"
            context.deleteDatabase(nombre)
            val antes = CineDatabase.construir(context, nombre)
            val datos = FixturePequeno.cargar(antes)
            val perfilesAntes = antes.perfilDao().contar()
            val rankingAntes = ListaRepository(antes).elementos(datos.listas.getValue(FixturePequeno.LISTA_RANKING))
            antes.close()

            val despues = CineDatabase.construir(context, nombre)
            assertEquals(perfilesAntes, despues.perfilDao().contar())
            assertEquals(FixturePequeno.perfiles.size, perfilesAntes)
            assertEquals(rankingAntes, ListaRepository(despues).elementos(datos.listas.getValue(FixturePequeno.LISTA_RANKING)))
            assertEquals(2, SagaRepository(despues).todas().size)
            despues.close()
            context.deleteDatabase(nombre)
        }

    // ---------- Listas ----------

    @Test
    fun t1_13_elMismoPerfilDosVecesEnLaMismaListaFallaPorClavePrimaria() =
        prueba {
            val p = perfil("Alien")
            val l = listas.crear("Terror", TipoLista.NORMAL)
            listas.anadir(l, p)
            try {
                db.listaDao().insertarItem(ListaItemEntity(l, p, null, null, 0))
                fail("La clave primaria (listaId, perfilId) debía rechazarlo")
            } catch (_: SQLiteConstraintException) {
            }
            assertEquals(ErrorDominio.YaEnLista("Alien"), error { listas.anadir(l, p) })
            assertEquals(1, db.listaDao().contarItems(l))
        }

    @Test
    fun t1_14_elMismoPerfilEnVariasListas() =
        prueba {
            val p = perfil("Alien")
            val ids = TipoLista.entries.map { listas.crear("Lista $it", it) }
            ids.forEach { listas.anadir(it, p) }
            ids.forEach { assertEquals(listOf(p), listas.elementos(it).map { e -> e.perfilId }) }
        }

    @Test
    fun t1_15_borrarUnPerfilNoDejaFilasHuerfanas() =
        prueba {
            val datos = FixturePequeno.cargar(db, reloj)
            val clave = FixturePequeno.clave(FixturePequeno.perfiles[0])
            val id = datos.perfiles.getValue(clave)
            assertTrue(contar("SELECT COUNT(*) FROM lista_item WHERE perfilId = $id") >= 2)
            assertTrue(contar("SELECT COUNT(*) FROM perfil_genero WHERE perfilId = $id") > 0)
            assertTrue(contar("SELECT COUNT(*) FROM perfil_idioma WHERE perfilId = $id") > 0)

            perfiles.eliminar(id)

            assertNull(perfiles.obtener(id))
            listOf("lista_item", "perfil_genero", "perfil_idioma").forEach { tabla ->
                assertEquals("Filas de $tabla del perfil borrado", 0, contar("SELECT COUNT(*) FROM $tabla WHERE perfilId = $id"))
                assertEquals(
                    "Filas huérfanas en $tabla",
                    0,
                    contar("SELECT COUNT(*) FROM $tabla t LEFT JOIN perfil p ON p.id = t.perfilId WHERE p.id IS NULL"),
                )
            }
            assertEquals(FixturePequeno.perfiles.size - 1, db.perfilDao().contar())
            // La saga sigue registrada (S-08) y el ranking sigue contiguo (S-07).
            assertEquals(2, sagas.todas().size)
            val ranking = listas.elementos(datos.listas.getValue(FixturePequeno.LISTA_RANKING))
            assertEquals((1..ranking.size).toList(), ranking.map { it.posicion })
        }

    @Test
    fun t1_16_borrarUnaListaNoBorraPerfiles() =
        prueba {
            val datos = FixturePequeno.cargar(db, reloj)
            val total = db.perfilDao().contar()
            datos.listas.values.forEach { listas.eliminar(it) }
            assertEquals(total, db.perfilDao().contar())
            assertEquals(0, contar("SELECT COUNT(*) FROM lista_item"))
            assertEquals(0, contar("SELECT COUNT(*) FROM lista"))
        }

    @Test
    fun t1_17_posicionYPrioridadSoloEnListaItem() =
        prueba {
            val colsPerfil = columnas("perfil")
            listOf("posicion", "prioridad", "ranking").forEach { c ->
                assertTrue("Perfil no puede tener \"$c\": $colsPerfil", colsPerfil.none { c in it.lowercase() })
            }
            assertTrue(columnas("lista_item").containsAll(setOf("posicion", "prioridad")))

            val a = perfil("A")
            val b = perfil("B")
            val r1 = listas.crear("Ranking 1", TipoLista.RANKING)
            val r2 = listas.crear("Ranking 2", TipoLista.RANKING)
            listas.anadir(r1, a)
            listas.anadir(r1, b)
            listas.anadir(r2, b)
            listas.anadir(r2, a)
            assertEquals(1, db.listaDao().item(r1, a)!!.posicion)
            assertEquals(2, db.listaDao().item(r2, a)!!.posicion)
        }

    @Test
    fun t1_18_unFalloAMitadDeTransaccionNoDejaDatosParciales() =
        prueba {
            // CU-01 E4: el perfil se inserta, pero un género inexistente hace fallar la transacción después.
            try {
                perfiles.crear(NuevoPerfil("Parcial", TipoPerfil.PELICULA, generoIds = setOf(9_999L)))
                fail("Debía fallar por la clave foránea del género")
            } catch (_: SQLiteConstraintException) {
            }
            assertEquals(0, db.perfilDao().contar())
            assertEquals(0, contar("SELECT COUNT(*) FROM perfil_genero"))

            // CU-06 E1: el perfil se borra (en cascada), pero falla la recompactación del ranking.
            val a = perfil("A")
            val b = perfil("B")
            val c = perfil("C")
            val r = listas.crear("Top", TipoLista.RANKING)
            listOf(a, b, c).forEach { listas.anadir(r, it) }
            val real = db.listaDao()
            val conFallo =
                object : ListaDao by real {
                    override suspend fun actualizarPosicion(
                        listaId: Long,
                        perfilId: Long,
                        posicion: Int,
                    ) = throw IOException("Fallo simulado")
                }
            val repoConFallo = PerfilRepository(db, listaDao = conFallo, reloj = reloj)
            try {
                repoConFallo.eliminar(a)
                fail("Debía fallar la recompactación")
            } catch (_: IOException) {
            }
            assertNotNull("El perfil sigue existiendo", perfiles.obtener(a))
            assertEquals(listOf(a to 1, b to 2, c to 3), listas.elementos(r).map { it.perfilId to it.posicion })
        }

    @Test
    fun t1_21_elRankingSiempreEsContiguo() =
        prueba {
            val r = listas.crear("Top", TipoLista.RANKING)
            val ids = (1..5).map { perfil("P$it") }

            fun posiciones() = runBlocking { listas.elementos(r).map { it.perfilId to it.posicion } }

            listas.anadir(r, ids[0])
            listas.anadir(r, ids[1])
            listas.anadir(r, ids[2]) // al final
            listas.anadir(r, ids[3], posicion = 2) // en medio
            assertEquals(listOf(ids[0] to 1, ids[3] to 2, ids[1] to 3, ids[2] to 4), posiciones())
            listas.anadir(r, ids[4], posicion = 1) // al principio
            assertEquals(listOf(ids[4], ids[0], ids[3], ids[1], ids[2]), posiciones().map { it.first })

            listas.quitar(r, ids[3]) // eliminar de en medio
            assertEquals((1..4).toList(), posiciones().map { it.second })
            perfiles.eliminar(ids[4]) // borrar el perfil del primer puesto
            assertEquals(listOf(ids[0] to 1, ids[1] to 2, ids[2] to 3), posiciones())

            listas.mover(r, ids[2], 1)
            assertEquals(listOf(ids[2] to 1, ids[0] to 2, ids[1] to 3), posiciones())

            assertEquals(ErrorDominio.PosicionInvalida, error { listas.anadir(r, ids[3], posicion = 5) })
            assertEquals(ErrorDominio.PosicionInvalida, error { listas.anadir(r, ids[3], posicion = 0) })
            assertEquals(ErrorDominio.PosicionInvalida, error { listas.mover(r, ids[0], 4) })
            assertEquals((1..3).toList(), posiciones().map { it.second })
        }

    @Test
    fun t1_22_prioridadAltaMediaBajaYDesempatePorAntiguedad() =
        prueba {
            val l = listas.crear("Próximas", TipoLista.PRIORIDAD)
            val orden =
                listOf(
                    "Baja antigua" to Prioridad.BAJA,
                    "Media antigua" to null, // S-16: sin indicar → Media
                    "Alta antigua" to Prioridad.ALTA,
                    "Media nueva" to Prioridad.MEDIA,
                    "Baja nueva" to Prioridad.BAJA,
                    "Alta nueva" to Prioridad.ALTA,
                )
            val ids =
                orden.associate { (nombre, prioridad) ->
                    val id = perfil(nombre)
                    listas.anadir(l, id, prioridad = prioridad)
                    nombre to id
                }
            val esperado = listOf("Alta antigua", "Alta nueva", "Media antigua", "Media nueva", "Baja antigua", "Baja nueva")
            assertEquals(esperado.map { ids.getValue(it) }, listas.elementos(l).map { it.perfilId })
            assertEquals(Prioridad.MEDIA, db.listaDao().item(l, ids.getValue("Media antigua"))!!.prioridad)
        }

    @Test
    fun t1_20_laRestriccionSeAplicaAlAnadir() =
        prueba {
            val pelicula = perfil("Alien", TipoPerfil.PELICULA)
            val serie = perfil("Dark", TipoPerfil.SERIE)
            val soloPeliculas = listas.crear("Solo pelis", TipoLista.NORMAL, Restriccion.SOLO_PELICULAS)
            val soloSeries = listas.crear("Solo series", TipoLista.RANKING, Restriccion.SOLO_SERIES)
            assertEquals(ErrorDominio.RestriccionLista(TipoPerfil.PELICULA), error { listas.anadir(soloPeliculas, serie) })
            assertEquals(ErrorDominio.RestriccionLista(TipoPerfil.SERIE), error { listas.anadir(soloSeries, pelicula) })
            assertTrue(listas.elementos(soloPeliculas).isEmpty())
            listas.anadir(soloPeliculas, pelicula)
            listas.anadir(soloSeries, serie)
        }

    @Test
    fun t1_23_bordesDeLosRangosDePuntuacion() =
        prueba {
            val casos =
                mapOf(
                    29 to RangoPuntuacion.DE_0_A_2_9,
                    30 to RangoPuntuacion.DE_3_A_4_9,
                    49 to RangoPuntuacion.DE_3_A_4_9,
                    50 to RangoPuntuacion.DE_5_A_6_9,
                    69 to RangoPuntuacion.DE_5_A_6_9,
                    70 to RangoPuntuacion.DE_7_A_8_9,
                    89 to RangoPuntuacion.DE_7_A_8_9,
                    90 to RangoPuntuacion.DE_9_A_10,
                    100 to RangoPuntuacion.DE_9_A_10,
                    0 to RangoPuntuacion.DE_0_A_2_9,
                )
            val ids = casos.keys.associateWith { perfil("Nota $it", decimas = it) }
            val sinNota = perfil("Sin nota")
            val rangos = db.perfilDao().rangos().associate { it.perfilId to it.rango }
            casos.forEach { (decimas, rango) ->
                assertEquals("${Puntuacion.of(decimas)} en la base de datos", rango, rangos.getValue(ids.getValue(decimas)))
                assertEquals("${Puntuacion.of(decimas)} en el dominio", rango, Puntuacion.of(decimas).rango)
            }
            assertEquals(RangoPuntuacion.SIN_PUNTUACION, rangos.getValue(sinNota))
        }

    // ---------- Otras reglas de listas (S-15) y fixtures ----------

    @Test
    fun listasConNombreDuplicadoONombreVacio() =
        prueba {
            listas.crear("Próximas a ver", TipoLista.PRIORIDAD)
            assertEquals(ErrorDominio.ListaDuplicada, error { listas.crear(" PROXIMAS a  ver", TipoLista.RANKING) })
            assertEquals(ErrorDominio.ListaNombreObligatorio, error { listas.crear("  ", TipoLista.NORMAL) })
        }

    @Test
    fun fixturePequenoCumpleSuDescripcion() =
        prueba {
            val datos = FixturePequeno.cargar(db, reloj)
            val todos = FixturePequeno.perfiles
            assertEquals(12, db.perfilDao().contar())
            assertEquals(6, todos.count { it.tipo == TipoPerfil.PELICULA })
            val notas = todos.map { it.decimas }.toSet()
            assertTrue(notas.containsAll(listOf(0, 29, 30, 49, 50, 69, 70, 89, 90, 100, null)))
            TipoPerfil.entries.forEach { t -> Estado.entries.forEach { e -> assertTrue(todos.any { it.tipo == t && it.estado == e }) } }
            assertEquals(2, datos.sagas.size)
            assertEquals(
                TipoLista.entries.toSet(),
                datos.listas.values
                    .map { runBlocking { listas.obtener(it)!!.tipo } }
                    .toSet(),
            )
        }

    @Test
    fun fixtureGrandeEsDeterministaYCabeEnLaBaseDeDatos() =
        prueba {
            val a = FixtureGrande()
            val b = FixtureGrande()
            assertEquals(a.perfiles, b.perfiles)
            assertEquals(a.elementos, b.elementos)
            assertEquals(FixtureGrande.PERFILES, a.perfiles.size)
            assertEquals(FixtureGrande.ELEMENTOS, a.elementos.size)
            assertEquals(a.elementos.size, a.elementos.toSet().size)

            a.cargar(db)
            assertEquals(FixtureGrande.PERFILES, db.perfilDao().contar())
            assertEquals(FixtureGrande.LISTAS, contar("SELECT COUNT(*) FROM lista"))
            assertEquals(FixtureGrande.ELEMENTOS, contar("SELECT COUNT(*) FROM lista_item"))
            assertEquals(
                "Ninguna lista incumple su restricción",
                0,
                contar(
                    """SELECT COUNT(*) FROM lista_item li JOIN lista l ON l.id = li.listaId JOIN perfil p ON p.id = li.perfilId
                   WHERE (l.restriccion = 'SOLO_PELICULAS' AND p.tipo <> 'PELICULA')
                      OR (l.restriccion = 'SOLO_SERIES' AND p.tipo <> 'SERIE')""",
                ),
            )
            assertFalse(
                "Los rankings son contiguos",
                contar(
                    """SELECT COUNT(*) FROM (SELECT li.listaId, COUNT(*) n, MIN(posicion) mi, MAX(posicion) ma, COUNT(DISTINCT posicion) d
                   FROM lista_item li JOIN lista l ON l.id = li.listaId WHERE l.tipo = 'RANKING' GROUP BY li.listaId)
                   WHERE mi <> 1 OR ma <> n OR d <> n""",
                ) > 0,
            )
        }
}
