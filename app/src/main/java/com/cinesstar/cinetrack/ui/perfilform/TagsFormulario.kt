package com.cinesstar.cinetrack.ui.perfilform

import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.TipoPerfil

/** Etiquetas de test del formulario (no son textos visibles). */
object TagsFormulario {
    const val NOMBRE = "form_nombre"
    const val NOMBRE_ERROR = "form_nombre_error"
    const val DISPONIBILIDAD = "form_disponibilidad"
    const val PUNTUACION = "form_puntuacion"
    const val MENOS = "form_menos"
    const val MAS = "form_mas"
    const val SLIDER = "form_slider"
    const val NOTA_GRANDE = "form_nota_grande"
    const val MENSAJE_PUNTUACION = "form_mensaje_puntuacion"
    const val SAGA = "form_saga"
    const val CREAR_SAGA = "form_crear_saga"
    const val SAGA_CAMPO = "form_saga_campo"
    const val SAGA_GUARDAR = "form_saga_guardar"
    const val SAGA_CERRAR = "form_saga_cerrar"
    const val SAGA_ERROR = "form_saga_error"
    const val SAGA_EXISTENTE = "form_saga_existente"
    const val CONTADOR_GENEROS = "form_contador_generos"
    const val CONTADOR_IDIOMAS = "form_contador_idiomas"
    const val COMENTARIO = "form_comentario"
    const val ERROR_ESCRITURA = "form_error_escritura"
    const val GUARDAR = "form_guardar"
    const val CANCELAR = "form_cancelar"
    const val SCROLL = "form_scroll"

    /** Pantalla "Editar Medio" (CU-02); al crear se usa la etiqueta del destino "Añadir". */
    const val PANTALLA_EDITAR = "pantalla_editar"

    fun tipo(tipo: TipoPerfil) = "form_tipo_" + tipo.name.lowercase()

    fun estado(estado: Estado) = "form_estado_" + estado.name.lowercase()
}
