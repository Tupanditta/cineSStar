package com.cinesstar.cinetrack.ui.listas

import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista

/** Etiquetas de test de "Mis Listas" y de sus diálogos (no son textos visibles). */
object TagsListas {
    const val LISTA = "listas_lista"
    const val PESTANA_MIS_LISTAS = "listas_pestana_mis_listas"
    const val PESTANA_TOP = "listas_pestana_top"
    const val BANNER = "listas_banner"
    const val MOSTRADAS = "listas_mostradas"
    const val VACIA = "listas_vacia"
    const val NUEVA = "listas_nueva"
    const val ERROR = "listas_error"
    const val REINTENTAR = "listas_reintentar"
    const val MENU_RENOMBRAR = "listas_menu_renombrar"
    const val MENU_ELIMINAR = "listas_menu_eliminar"

    // Diálogo "Nueva Lista"
    const val DIALOGO_NUEVA = "listas_dialogo_nueva"
    const val NOMBRE = "listas_nombre"
    const val ERROR_NOMBRE = "listas_error_nombre"

    // Diálogo "Renombrar"
    const val DIALOGO_RENOMBRAR = "listas_dialogo_renombrar"

    fun tarjeta(listaId: Long) = "listas_tarjeta_$listaId"

    fun tipoTarjeta(listaId: Long) = "listas_tarjeta_${listaId}_tipo"

    fun totalTarjeta(listaId: Long) = "listas_tarjeta_${listaId}_total"

    fun restriccionTarjeta(listaId: Long) = "listas_tarjeta_${listaId}_restriccion"

    fun menu(listaId: Long) = "listas_tarjeta_${listaId}_menu"

    fun tipo(tipo: TipoLista) = "listas_tipo_${tipo.name.lowercase()}"

    fun restriccion(restriccion: Restriccion) = "listas_restriccion_${restriccion.name.lowercase()}"
}
