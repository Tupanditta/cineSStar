package com.cinesstar.cinetrack.data

import androidx.test.core.app.ApplicationProvider
import com.cinesstar.cinetrack.data.backup.CopiaRepository
import com.cinesstar.cinetrack.data.repository.ListaRepository
import com.cinesstar.cinetrack.data.repository.NuevoPerfil
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.room.CineDatabase
import com.cinesstar.cinetrack.domain.CopiaSeguridad
import com.cinesstar.cinetrack.domain.ElementoCopia
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.FormatoCopia
import com.cinesstar.cinetrack.domain.ListaCopia
import com.cinesstar.cinetrack.domain.Normalizador
import com.cinesstar.cinetrack.domain.PerfilCopia
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Puntuacion
import com.cinesstar.cinetrack.domain.ResumenFusion
import com.cinesstar.cinetrack.domain.TipoPerfil
import kotlinx.coroutines.runBlocking
import org.junit.After

/**
 * Base de los tests U y D del S8 (CU-21, CU-22) sobre Room en memoria. "Vaciar la base de datos" es importar en otra
 * base de datos nueva (con el catálogo precargado).
 */
abstract class PruebaCopia {
    protected var ahora = 1_000L
    protected val reloj = { ahora++ }
    protected val bases = mutableListOf<CineDatabase>()
    protected val db = nuevaBase()
    protected val copias = CopiaRepository(db, reloj = reloj)
    protected val perfiles = PerfilRepository(db, reloj = reloj)
    protected val listas = ListaRepository(db, reloj = reloj)

    protected fun nuevaBase(): CineDatabase =
        CineDatabase.construir(ApplicationProvider.getApplicationContext(), nombre = null).also {
            bases +=
                it
        }

    @After
    fun cerrar() = bases.forEach { it.close() }

    protected fun prueba(bloque: suspend () -> Unit) = runBlocking { bloque() }

    /** Exporta, pasa por el texto del archivo y lo vuelve a leer, como hace la app. */
    protected suspend fun exportarYLeer(repo: CopiaRepository = copias): CopiaSeguridad =
        FormatoCopia.leer(FormatoCopia.escribir(repo.exportar()))

    protected suspend fun crear(
        nombre: String,
        tipo: TipoPerfil = TipoPerfil.PELICULA,
        estado: Estado = Estado.PENDIENTE,
        decimas: Int? = null,
        comentario: String? = null,
        saga: String? = null,
    ): Long = perfiles.crear(NuevoPerfil(nombre, tipo, estado, decimas?.let { Puntuacion.of(it) }, comentario, sagaNueva = saga))

    protected fun perfil(
        nombre: String,
        tipo: TipoPerfil = TipoPerfil.PELICULA,
        puntuacion: Double? = null,
        saga: String? = null,
        estado: Estado = Estado.PENDIENTE,
        comentario: String? = null,
    ) = PerfilCopia(nombre, tipo, estado, puntuacion, comentario, comentario?.let { 5L }, saga, creadoEn = 7L)

    protected fun elemento(
        nombre: String,
        tipo: TipoPerfil = TipoPerfil.PELICULA,
        posicion: Int? = null,
        prioridad: Prioridad? = null,
    ) = ElementoCopia(nombre, tipo, posicion, prioridad, anadidoEn = 9L)

    protected fun copia(
        perfiles: List<PerfilCopia>,
        listas: List<ListaCopia> = emptyList(),
        sagas: List<String> = emptyList(),
    ) = CopiaSeguridad(fecha = "2026-11-09T12:00:00", sagas = sagas, perfiles = perfiles, listas = listas)

    /** La copia pasa por el texto y la validación completa, como cuando se elige un archivo. */
    protected suspend fun importar(c: CopiaSeguridad): ResumenFusion = copias.importar(FormatoCopia.leer(FormatoCopia.escribir(c)))

    protected fun resumen(
        nuevos: Int,
        omitidos: Int,
        listasNuevas: Int,
        ampliadas: Int,
    ) = ResumenFusion(nuevos, omitidos, listasNuevas, ampliadas)

    protected suspend fun listaId(nombre: String): Long = requireNotNull(db.listaDao().buscarPorNombre(Normalizador.normalizar(nombre))).id

    protected suspend fun nombresEn(listaId: Long): List<String> {
        val nombres = db.copiaDao().perfiles().associate { it.id to it.nombre }
        return listas.elementos(listaId).map { nombres.getValue(it.perfilId) }
    }
}
