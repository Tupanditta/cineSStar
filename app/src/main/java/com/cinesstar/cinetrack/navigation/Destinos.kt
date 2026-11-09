package com.cinesstar.cinetrack.navigation

import androidx.annotation.StringRes
import com.cinesstar.cinetrack.R
import kotlinx.serialization.Serializable

/** Rutas de Navigation Compose (type-safe). */
sealed interface Ruta {
    @Serializable
    data object Biblioteca : Ruta

    @Serializable
    data object Listas : Ruta

    @Serializable
    data object Buscar : Ruta

    @Serializable
    data object Ajustes : Ruta

    /** Pantalla secundaria de alta de perfil (sin barra inferior). */
    @Serializable
    data object Anadir : Ruta

    /** Formulario de edición de un perfil (CU-02), pantalla secundaria. */
    @Serializable
    data class Editar(
        val perfilId: Long,
    ) : Ruta

    /** Detalle de un perfil (CU-07), pantalla secundaria. */
    @Serializable
    data class Detalle(
        val perfilId: Long,
    ) : Ruta
}

/**
 * Los 5 destinos de la barra inferior, en orden (design.md §3.4).
 * "Añadir" no es una pestaña: abre la pantalla secundaria de alta.
 */
enum class DestinoBarra(
    val ruta: Ruta,
    @StringRes val etiqueta: Int,
    val icono: String,
) {
    BIBLIOTECA(Ruta.Biblioteca, R.string.destino_biblioteca, "video_library"),
    LISTAS(Ruta.Listas, R.string.destino_listas, "bookmarks"),
    ANADIR(Ruta.Anadir, R.string.destino_anadir, "add"),
    BUSCAR(Ruta.Buscar, R.string.destino_buscar, "search"),
    AJUSTES(Ruta.Ajustes, R.string.destino_ajustes, "database"),
    ;

    /** Es una pestaña principal (con barra inferior y barra superior de marca). */
    val esPestana: Boolean get() = this != ANADIR
}
