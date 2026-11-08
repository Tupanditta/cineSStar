package com.cinesstar.cinetrack

import android.app.Application
import android.content.Context
import com.cinesstar.cinetrack.data.repository.ListaRepository
import com.cinesstar.cinetrack.data.repository.PerfilRepository
import com.cinesstar.cinetrack.data.repository.SagaRepository
import com.cinesstar.cinetrack.data.room.CineDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Dependencias de la app. Se sustituirá por Hilt cuando haya ViewModels (S2). */
class AppContainer(
    context: Context,
) {
    val database: CineDatabase by lazy { CineDatabase.construir(context) }
    val perfiles: PerfilRepository by lazy { PerfilRepository(database) }
    val sagas: SagaRepository by lazy { SagaRepository(database) }
    val listas: ListaRepository by lazy { ListaRepository(database) }
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
}
