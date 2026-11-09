package com.cinesstar.cinetrack.ui.datos

/** Lo que necesita el contenido de "Datos Locales": el estado y las acciones con los selectores del sistema. */
class PanelDatos(
    val estado: EstadoDatos,
    val vm: DatosViewModel,
    val onExportar: () -> Unit,
    val onImportar: () -> Unit,
)
