package com.cinesstar.cinetrack.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.RangoPuntuacion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import kotlinx.coroutines.flow.Flow

/** Rango de puntuación de un perfil, calculado en SQL (RF-25). */
data class PerfilRango(
    val perfilId: Long,
    val rango: RangoPuntuacion,
)

@Dao
interface PerfilDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(perfil: PerfilEntity): Long

    @Update
    suspend fun actualizar(perfil: PerfilEntity)

    @Query("DELETE FROM perfil WHERE id = :id")
    suspend fun eliminar(id: Long): Int

    @Query("SELECT * FROM perfil WHERE id = :id")
    suspend fun obtener(id: Long): PerfilEntity?

    /**
     * RES-06: ¿hay otro perfil con ese nombre normalizado y tipo? (comprobación en línea del formulario, CU-01 paso 4).
     * [excluirId] es el perfil que se está editando (CU-02 E2); 0 al crear (los ids empiezan en 1).
     */
    @Query("SELECT EXISTS(SELECT 1 FROM perfil WHERE nombreBusqueda = :nombreBusqueda AND tipo = :tipo AND id != :excluirId)")
    suspend fun existe(
        nombreBusqueda: String,
        tipo: TipoPerfil,
        excluirId: Long,
    ): Boolean

    @Query("SELECT * FROM perfil WHERE nombreBusqueda = :nombreBusqueda AND tipo = :tipo")
    suspend fun buscarPorNombre(
        nombreBusqueda: String,
        tipo: TipoPerfil,
    ): PerfilEntity?

    /** Más recientes primero (S-10). */
    @Query("SELECT * FROM perfil ORDER BY creadoEn DESC, id DESC")
    fun observarTodos(): Flow<List<PerfilEntity>>

    @Query("SELECT COUNT(*) FROM perfil")
    suspend fun contar(): Int

    /** CU-05: devuelve las filas cambiadas (0 si el perfil ya no existe). */
    @Query("UPDATE perfil SET estado = :estado WHERE id = :perfilId")
    suspend fun actualizarEstado(
        perfilId: Long,
        estado: Estado,
    ): Int

    /** CU-03: null quita la puntuación. */
    @Query("UPDATE perfil SET puntuacionDecimas = :decimas WHERE id = :perfilId")
    suspend fun actualizarPuntuacion(
        perfilId: Long,
        decimas: Int?,
    ): Int

    /** CU-04: null quita el comentario (y su fecha). */
    @Query("UPDATE perfil SET comentario = :comentario, comentarioEditadoEn = :editadoEn WHERE id = :perfilId")
    suspend fun actualizarComentario(
        perfilId: Long,
        comentario: String?,
        editadoEn: Long?,
    ): Int

    @Query("UPDATE perfil SET sagaId = :sagaId WHERE id = :perfilId")
    suspend fun asignarSaga(
        perfilId: Long,
        sagaId: Long?,
    ): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarGeneros(relaciones: List<PerfilGeneroEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarIdiomas(relaciones: List<PerfilIdiomaEntity>)

    @Query("DELETE FROM perfil_genero WHERE perfilId = :perfilId")
    suspend fun borrarGeneros(perfilId: Long)

    @Query("DELETE FROM perfil_idioma WHERE perfilId = :perfilId")
    suspend fun borrarIdiomas(perfilId: Long)

    @Query("SELECT g.* FROM genero g JOIN perfil_genero pg ON pg.generoId = g.id WHERE pg.perfilId = :perfilId ORDER BY g.orden")
    suspend fun generosDe(perfilId: Long): List<GeneroEntity>

    @Query("SELECT i.* FROM idioma i JOIN perfil_idioma pi ON pi.idiomaId = i.id WHERE pi.perfilId = :perfilId ORDER BY i.orden")
    suspend fun idiomasDe(perfilId: Long): List<IdiomaEntity>

    /** Los límites coinciden con [RangoPuntuacion] (en décimas, ambos extremos incluidos). */
    @Query(
        """
        SELECT id AS perfilId,
            CASE
                WHEN puntuacionDecimas IS NULL THEN 'SIN_PUNTUACION'
                WHEN puntuacionDecimas <= 29 THEN 'DE_0_A_2_9'
                WHEN puntuacionDecimas <= 49 THEN 'DE_3_A_4_9'
                WHEN puntuacionDecimas <= 69 THEN 'DE_5_A_6_9'
                WHEN puntuacionDecimas <= 89 THEN 'DE_7_A_8_9'
                ELSE 'DE_9_A_10'
            END AS rango
        FROM perfil
        """,
    )
    suspend fun rangos(): List<PerfilRango>
}

@Dao
interface SagaDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(saga: SagaEntity): Long

    @Query("SELECT * FROM saga WHERE id = :id")
    suspend fun obtener(id: Long): SagaEntity?

    @Query("SELECT * FROM saga WHERE nombreBusqueda = :nombreBusqueda")
    suspend fun buscarPorNombre(nombreBusqueda: String): SagaEntity?

    @Query("SELECT * FROM saga ORDER BY nombreBusqueda")
    suspend fun todas(): List<SagaEntity>

    /** Perfiles de una saga de un tipo ("{n} películas/series guardadas en tu archivo", design.md §7.4). */
    @Query("SELECT COUNT(*) FROM perfil WHERE sagaId = :sagaId AND tipo = :tipo")
    suspend fun contarPerfiles(
        sagaId: Long,
        tipo: TipoPerfil,
    ): Int
}

/** Una lista a la que pertenece un perfil, con su posición o prioridad en ella (CU-07, design.md §7.5). */
data class PertenenciaLista(
    val listaId: Long,
    val nombre: String,
    val tipo: TipoLista,
    val posicion: Int?,
    val prioridad: Prioridad?,
    /** Elementos de la lista. */
    val total: Int,
)

@Dao
interface CatalogoDao {
    @Query("SELECT * FROM genero ORDER BY orden")
    suspend fun generos(): List<GeneroEntity>

    @Query("SELECT * FROM idioma ORDER BY orden")
    suspend fun idiomas(): List<IdiomaEntity>
}

@Dao
interface ListaDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(lista: ListaEntity): Long

    @Query("SELECT * FROM lista WHERE id = :id")
    suspend fun obtener(id: Long): ListaEntity?

    @Query("SELECT * FROM lista WHERE nombreBusqueda = :nombreBusqueda")
    suspend fun buscarPorNombre(nombreBusqueda: String): ListaEntity?

    @Query("DELETE FROM lista WHERE id = :id")
    suspend fun eliminar(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarItem(item: ListaItemEntity)

    @Query("DELETE FROM lista_item WHERE listaId = :listaId AND perfilId = :perfilId")
    suspend fun eliminarItem(
        listaId: Long,
        perfilId: Long,
    ): Int

    @Query("SELECT * FROM lista_item WHERE listaId = :listaId AND perfilId = :perfilId")
    suspend fun item(
        listaId: Long,
        perfilId: Long,
    ): ListaItemEntity?

    @Query("SELECT COUNT(*) FROM lista_item WHERE listaId = :listaId")
    suspend fun contarItems(listaId: Long): Int

    @Query("SELECT * FROM lista_item WHERE listaId = :listaId ORDER BY anadidoEn, perfilId")
    suspend fun itemsPorAntiguedad(listaId: Long): List<ListaItemEntity>

    @Query("SELECT * FROM lista_item WHERE listaId = :listaId ORDER BY posicion, anadidoEn, perfilId")
    suspend fun itemsPorPosicion(listaId: Long): List<ListaItemEntity>

    /** Alta → Media → Baja; a igual prioridad, el más antiguo primero. */
    @Query(
        """
        SELECT * FROM lista_item WHERE listaId = :listaId
        ORDER BY CASE prioridad WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 WHEN 'BAJA' THEN 2 ELSE 3 END, anadidoEn, perfilId
        """,
    )
    suspend fun itemsPorPrioridad(listaId: Long): List<ListaItemEntity>

    /** Suma [delta] a las posiciones >= [desde] de un ranking (hace hueco al insertar en medio). */
    @Query("UPDATE lista_item SET posicion = posicion + :delta WHERE listaId = :listaId AND posicion >= :desde")
    suspend fun desplazarPosiciones(
        listaId: Long,
        desde: Int,
        delta: Int,
    )

    @Query("UPDATE lista_item SET posicion = :posicion WHERE listaId = :listaId AND perfilId = :perfilId")
    suspend fun actualizarPosicion(
        listaId: Long,
        perfilId: Long,
        posicion: Int,
    )

    /** Rankings en los que está un perfil (para recompactar posiciones al borrarlo, S-07). */
    @Query(
        """
        SELECT DISTINCT li.listaId FROM lista_item li JOIN lista l ON l.id = li.listaId
        WHERE li.perfilId = :perfilId AND l.tipo = 'RANKING'
        """,
    )
    suspend fun rankingsCon(perfilId: Long): List<Long>

    /** Listas que contienen un perfil, por nombre. */
    @Query(
        """
        SELECT l.id AS listaId, l.nombre AS nombre, l.tipo AS tipo, li.posicion AS posicion, li.prioridad AS prioridad,
            (SELECT COUNT(*) FROM lista_item x WHERE x.listaId = l.id) AS total
        FROM lista_item li JOIN lista l ON l.id = li.listaId
        WHERE li.perfilId = :perfilId
        ORDER BY l.nombreBusqueda
        """,
    )
    suspend fun listasDe(perfilId: Long): List<PertenenciaLista>
}
