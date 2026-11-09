package com.cinesstar.cinetrack

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.cinesstar.cinetrack.data.repository.BibliotecaRepository
import com.cinesstar.cinetrack.data.repository.CatalogoRepository
import com.cinesstar.cinetrack.data.repository.ListaRepository
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.repository.SagaRepository
import com.cinesstar.cinetrack.data.room.CineDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Dependencias de la app, creadas a mano (design.md §15 propone Hilt; con un solo contenedor y pocas dependencias no
 * compensa el plugin). Los ViewModels las reciben por constructor; los tests pueden sustituir el contenedor completo.
 */
class AppContainer(
    val database: CineDatabase,
    val perfiles: PerfilRepository = PerfilRepository(database),
    val sagas: SagaRepository = SagaRepository(database),
    val listas: ListaRepository = ListaRepository(database),
    val catalogo: CatalogoRepository = CatalogoRepository(database),
    val biblioteca: BibliotecaRepository = BibliotecaRepository(database),
) {
    constructor(context: Context) : this(CineDatabase.construir(context))
}

class CineTrackApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // CU-23 FP: abre la base de datos al arrancar; en la primera ejecución crea el esquema y precarga el catálogo.
        scope.launch { container.database.openHelper.writableDatabase }
    }

    /** Sustituye las dependencias (fallos simulados, reinicio de la app en los tests). */
    @VisibleForTesting
    fun reemplazarContenedor(nuevo: AppContainer) {
        container = nuevo
    }
}

/** Contenedor de la app a partir de cualquier [Context]. */
val Context.contenedor: AppContainer get() = (applicationContext as CineTrackApplication).container
