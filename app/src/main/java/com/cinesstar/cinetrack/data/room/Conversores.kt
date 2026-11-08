package com.cinesstar.cinetrack.data.room

import androidx.room.TypeConverter
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil

/** Los enumerados se guardan por nombre para que la base de datos y la copia de seguridad sean legibles. */
class Conversores {
    @TypeConverter fun tipoPerfilATexto(valor: TipoPerfil): String = valor.name

    @TypeConverter fun textoATipoPerfil(valor: String): TipoPerfil = TipoPerfil.valueOf(valor)

    @TypeConverter fun estadoATexto(valor: Estado): String = valor.name

    @TypeConverter fun textoAEstado(valor: String): Estado = Estado.valueOf(valor)

    @TypeConverter fun tipoListaATexto(valor: TipoLista): String = valor.name

    @TypeConverter fun textoATipoLista(valor: String): TipoLista = TipoLista.valueOf(valor)

    @TypeConverter fun restriccionATexto(valor: Restriccion): String = valor.name

    @TypeConverter fun textoARestriccion(valor: String): Restriccion = Restriccion.valueOf(valor)

    @TypeConverter fun prioridadATexto(valor: Prioridad?): String? = valor?.name

    @TypeConverter fun textoAPrioridad(valor: String?): Prioridad? = valor?.let { Prioridad.valueOf(it) }
}
