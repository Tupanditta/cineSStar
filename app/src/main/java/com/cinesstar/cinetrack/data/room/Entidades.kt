package com.cinesstar.cinetrack.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil

/*
 * Modelo de datos de design.md §16. Las fechas se guardan en milisegundos desde 1970 (UTC).
 * Posición y prioridad viven en ListaItem, nunca en Perfil (RNF-02, RNF-03, RES-07).
 */

@Entity(
    tableName = "perfil",
    foreignKeys = [
        ForeignKey(entity = SagaEntity::class, parentColumns = ["id"], childColumns = ["sagaId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        // RES-06: un perfil es único por nombre normalizado y tipo.
        Index(value = ["nombreBusqueda", "tipo"], unique = true),
        Index("sagaId"),
        Index("puntuacionDecimas"),
        Index("creadoEn"),
    ],
)
data class PerfilEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val nombreBusqueda: String,
    val tipo: TipoPerfil,
    val estado: Estado,
    /** Puntuación en décimas (0..100) o null si no tiene (S-04). Una sola por perfil, también en series (RF-11). */
    val puntuacionDecimas: Int?,
    val comentario: String?,
    val comentarioEditadoEn: Long?,
    /** Como máximo una saga (RF-09); el orden dentro de la saga no se guarda. */
    val sagaId: Long?,
    val caratulaUri: String?,
    val creadoEn: Long,
)

@Entity(tableName = "saga", indices = [Index(value = ["nombreBusqueda"], unique = true)])
data class SagaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val nombreBusqueda: String,
)

/** Géneros predefinidos (RF-07). [orden] es la posición en la que se muestran. */
@Entity(tableName = "genero", indices = [Index(value = ["nombre"], unique = true)])
data class GeneroEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val orden: Int,
)

/** Idiomas predefinidos (RF-08). */
@Entity(tableName = "idioma", indices = [Index(value = ["nombre"], unique = true)])
data class IdiomaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val orden: Int,
)

@Entity(
    tableName = "perfil_genero",
    primaryKeys = ["perfilId", "generoId"],
    foreignKeys = [
        ForeignKey(entity = PerfilEntity::class, parentColumns = ["id"], childColumns = ["perfilId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = GeneroEntity::class, parentColumns = ["id"], childColumns = ["generoId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("generoId")],
)
data class PerfilGeneroEntity(
    val perfilId: Long,
    val generoId: Long,
)

@Entity(
    tableName = "perfil_idioma",
    primaryKeys = ["perfilId", "idiomaId"],
    foreignKeys = [
        ForeignKey(entity = PerfilEntity::class, parentColumns = ["id"], childColumns = ["perfilId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = IdiomaEntity::class, parentColumns = ["id"], childColumns = ["idiomaId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("idiomaId")],
)
data class PerfilIdiomaEntity(
    val perfilId: Long,
    val idiomaId: Long,
)

@Entity(tableName = "lista", indices = [Index(value = ["nombreBusqueda"], unique = true)])
data class ListaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    /** Nombre normalizado: no hay dos listas con el mismo (S-15). */
    val nombreBusqueda: String,
    val tipo: TipoLista,
    val restriccion: Restriccion,
    /** Aviso de lista vacía ya mostrado en este vaciado (S-17). */
    @ColumnInfo(defaultValue = "0") val avisoVaciaMostrado: Boolean = false,
    val creadoEn: Long,
)

/**
 * Elemento de lista. La clave primaria (listaId, perfilId) impide repetir un perfil en la misma lista (RES-08).
 * [posicion] solo en RANKING (1..n contiguo, S-07); [prioridad] solo en PRIORIDAD.
 */
@Entity(
    tableName = "lista_item",
    primaryKeys = ["listaId", "perfilId"],
    foreignKeys = [
        ForeignKey(entity = ListaEntity::class, parentColumns = ["id"], childColumns = ["listaId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PerfilEntity::class, parentColumns = ["id"], childColumns = ["perfilId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("perfilId"), Index(value = ["listaId", "posicion"])],
)
data class ListaItemEntity(
    val listaId: Long,
    val perfilId: Long,
    val posicion: Int?,
    val prioridad: Prioridad?,
    val anadidoEn: Long,
)
