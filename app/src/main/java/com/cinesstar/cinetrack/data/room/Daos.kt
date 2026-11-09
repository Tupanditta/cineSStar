package com.cinesstar.cinetrack.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cinesstar.cinetrack.domain.Estado
import com.cinesstar.cinetrack.domain.Prioridad
import com.cinesstar.cinetrack.domain.RangoPuntuacion
import com.cinesstar.cinetrack.domain.Restriccion
import com.cinesstar.cinetrack.domain.TipoLista
import com.cinesstar.cinetrack.domain.TipoPerfil
import kotlinx.coroutines.flow.Flow

/** Rango de puntuación de un perfil, calculado en SQL (RF-25). */
data class PerfilRango(
    val perfilId: Long,
    val rango: RangoPuntuacion,
)

/** Fila de la Biblioteca: el perfil con el nombre de su saga (CU-08). */
data class FilaBiblioteca(
    val id: Long,
    val nombre: String,
    val tipo: TipoPerfil,
    val estado: Estado,
    val puntuacionDecimas: Int?,
    val sagaId: Long?,
    val sagaNombre: String?,
    val caratulaUri: String?,
    val creadoEn: Long,
)

/** Un género o idioma de un perfil, con su posición en el catálogo. */
data class EtiquetaPerfil(
    val perfilId: Long,
    val id: Long,
    val nombre: String,
    val orden: Int,
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

/** Lectura de la Biblioteca (CU-08, CU-09): tres consultas para todos los perfiles, sin una por perfil (RNF-08). */
@Dao
interface BibliotecaDao {
    /** CU-08: todos los perfiles con su saga, más recientes primero (S-10). */
    @Query(
        """
        SELECT p.id, p.nombre, p.tipo, p.estado, p.puntuacionDecimas, p.sagaId, s.nombre AS sagaNombre, p.caratulaUri,
            p.creadoEn
        FROM perfil p LEFT JOIN saga s ON s.id = p.sagaId
        ORDER BY p.creadoEn DESC, p.id DESC
        """,
    )
    suspend fun filasBiblioteca(): List<FilaBiblioteca>

    /** Géneros de todos los perfiles, en el orden del catálogo (una consulta para toda la Biblioteca, RNF-08). */
    @Query(
        """
        SELECT pg.perfilId AS perfilId, g.id AS id, g.nombre AS nombre, g.orden AS orden
        FROM perfil_genero pg JOIN genero g ON g.id = pg.generoId
        ORDER BY g.orden
        """,
    )
    suspend fun generosDeTodos(): List<EtiquetaPerfil>

    /** Idiomas de todos los perfiles, en el orden del catálogo. */
    @Query(
        """
        SELECT pi.perfilId AS perfilId, i.id AS id, i.nombre AS nombre, i.orden AS orden
        FROM perfil_idioma pi JOIN idioma i ON i.id = pi.idiomaId
        ORDER BY i.orden
        """,
    )
    suspend fun idiomasDeTodos(): List<EtiquetaPerfil>
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

/** Una lista con los datos de su tarjeta en "Mis Listas" (CU-12, design.md §5.2). */
data class FilaResumenLista(
    val id: Long,
    val nombre: String,
    val tipo: TipoLista,
    val restriccion: Restriccion,
    val total: Int,
    val altas: Int,
    val primero: String?,
)

/** Un elemento de lista con los datos del perfil que pinta su fila (design.md §6.2). */
data class FilaElementoLista(
    val perfilId: Long,
    val nombre: String,
    val tipo: TipoPerfil,
    val puntuacionDecimas: Int?,
    val sagaNombre: String?,
    val posicion: Int?,
    val prioridad: Prioridad?,
    val anadidoEn: Long,
)

/** Un perfil del selector "Añadir título" (design.md §6.6) y si ya está en la lista. */
data class FilaCandidato(
    val id: Long,
    val nombre: String,
    val nombreBusqueda: String,
    val tipo: TipoPerfil,
    val sagaNombre: String?,
    val enLista: Boolean,
)

@Dao
interface CatalogoDao {
    @Query("SELECT * FROM genero ORDER BY orden")
    suspend fun generos(): List<GeneroEntity>

    @Query("SELECT * FROM idioma ORDER BY orden")
    suspend fun idiomas(): List<IdiomaEntity>
}

/** Listas y sus elementos: todas las consultas de `lista` y `lista_item` (CU-11..CU-19). */
@Suppress("TooManyFunctions")
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

    /** CU-17. Devuelve las filas cambiadas (0 si el elemento ya no está en la lista). */
    @Query("UPDATE lista_item SET prioridad = :prioridad WHERE listaId = :listaId AND perfilId = :perfilId")
    suspend fun actualizarPrioridad(
        listaId: Long,
        perfilId: Long,
        prioridad: Prioridad,
    ): Int

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

    /** CU-14. Devuelve las filas cambiadas (0 si la lista ya no existe). */
    @Query("UPDATE lista SET nombre = :nombre, nombreBusqueda = :nombreBusqueda WHERE id = :id")
    suspend fun renombrar(
        id: Long,
        nombre: String,
        nombreBusqueda: String,
    ): Int

    /** S-17: marca (o desmarca) que ya se mostró el aviso de lista vacía en este vaciado. */
    @Query("UPDATE lista SET avisoVaciaMostrado = :mostrado WHERE id = :id")
    suspend fun marcarAvisoVacia(
        id: Long,
        mostrado: Boolean,
    ): Int

    /**
     * CU-12: todas las listas con su número de elementos, sus elementos de prioridad Alta y el #1 de los rankings.
     * Las más recientes primero.
     */
    @Query(
        """
        SELECT l.id, l.nombre, l.tipo, l.restriccion,
            (SELECT COUNT(*) FROM lista_item li WHERE li.listaId = l.id) AS total,
            (SELECT COUNT(*) FROM lista_item li WHERE li.listaId = l.id AND li.prioridad = 'ALTA') AS altas,
            (SELECT p.nombre FROM lista_item li JOIN perfil p ON p.id = li.perfilId
                WHERE li.listaId = l.id AND li.posicion = 1) AS primero
        FROM lista l
        ORDER BY l.creadoEn DESC, l.id DESC
        """,
    )
    suspend fun resumenes(): List<FilaResumenLista>

    /** CU-13: elementos de una lista con los datos de su perfil (sin orden: lo da el tipo de lista). */
    @Query(
        """
        SELECT li.perfilId, p.nombre, p.tipo, p.puntuacionDecimas, s.nombre AS sagaNombre, li.posicion, li.prioridad,
            li.anadidoEn
        FROM lista_item li JOIN perfil p ON p.id = li.perfilId LEFT JOIN saga s ON s.id = p.sagaId
        WHERE li.listaId = :listaId
        """,
    )
    suspend fun filasElementos(listaId: Long): List<FilaElementoLista>

    /** Géneros de los perfiles de una lista, en el orden del catálogo (una consulta para toda la lista, RNF-08). */
    @Query(
        """
        SELECT pg.perfilId AS perfilId, g.id AS id, g.nombre AS nombre, g.orden AS orden
        FROM perfil_genero pg JOIN genero g ON g.id = pg.generoId
        WHERE pg.perfilId IN (SELECT perfilId FROM lista_item WHERE listaId = :listaId)
        ORDER BY g.orden
        """,
    )
    suspend fun generosDeLista(listaId: Long): List<EtiquetaPerfil>

    /** CU-15: perfiles de la Biblioteca para el selector, más recientes primero, indicando si ya están en la lista. */
    @Query(
        """
        SELECT p.id, p.nombre, p.nombreBusqueda, p.tipo, s.nombre AS sagaNombre,
            EXISTS(SELECT 1 FROM lista_item li WHERE li.listaId = :listaId AND li.perfilId = p.id) AS enLista
        FROM perfil p LEFT JOIN saga s ON s.id = p.sagaId
        ORDER BY p.creadoEn DESC, p.id DESC
        """,
    )
    suspend fun candidatos(listaId: Long): List<FilaCandidato>
}

/**
 * Búsqueda combinada (CU-20) en una sola consulta. Cada criterio se desactiva con su parámetro nulo (o con su
 * indicador `con...` para las colecciones); entre criterios, AND; entre géneros o estados, OR (S-19, S-18).
 * El nombre se compara con `nombreBusqueda` (sin tildes ni mayúsculas, RF-46) mediante un patrón `LIKE` ya escapado
 * ([com.cinesstar.cinetrack.domain.patronContiene], CU-20 E4). Los límites de puntuación están en décimas; una
 * comparación con NULL es falsa, así que con rango activo los perfiles sin nota quedan fuera (RF-44).
 */
@Dao
interface BusquedaDao {
    @Query(
        """
        SELECT p.id, p.nombre, p.tipo, p.estado, p.puntuacionDecimas, p.sagaId, s.nombre AS sagaNombre, p.caratulaUri,
            p.creadoEn
        FROM perfil p LEFT JOIN saga s ON s.id = p.sagaId
        WHERE (:patron IS NULL OR p.nombreBusqueda LIKE :patron ESCAPE '\')
            AND (:tipo IS NULL OR p.tipo = :tipo)
            AND (:minimo IS NULL OR p.puntuacionDecimas >= :minimo)
            AND (:maximo IS NULL OR p.puntuacionDecimas <= :maximo)
            AND (NOT :conGeneros OR EXISTS(
                SELECT 1 FROM perfil_genero pg WHERE pg.perfilId = p.id AND pg.generoId IN (:generoIds)))
            AND (:idiomaId IS NULL OR EXISTS(
                SELECT 1 FROM perfil_idioma pi WHERE pi.perfilId = p.id AND pi.idiomaId = :idiomaId))
            AND (:sagaId IS NULL OR p.sagaId = :sagaId)
            AND (NOT :conEstados OR p.estado IN (:estados))
        ORDER BY p.creadoEn DESC, p.id DESC
        """,
    )
    @Suppress("LongParameterList")
    suspend fun buscar(
        patron: String?,
        tipo: TipoPerfil?,
        minimo: Int?,
        maximo: Int?,
        conGeneros: Boolean,
        generoIds: List<Long>,
        idiomaId: Long?,
        sagaId: Long?,
        conEstados: Boolean,
        estados: List<Estado>,
    ): List<FilaBiblioteca>
}

/** Recuento de perfiles de un tipo (distribución de "Datos Locales", design.md §10). */
data class TotalTipo(
    val tipo: TipoPerfil,
    val total: Int,
)

/** Lectura completa de los datos para la copia de seguridad (CU-21) y la fusión (CU-22), y métricas de §10. */
@Dao
interface CopiaDao {
    /** Más antiguos primero: al importar en una base vacía los perfiles se crean en el mismo orden. */
    @Query("SELECT * FROM perfil ORDER BY creadoEn, id")
    suspend fun perfiles(): List<PerfilEntity>

    @Query("SELECT * FROM perfil_genero")
    suspend fun perfilGeneros(): List<PerfilGeneroEntity>

    @Query("SELECT * FROM perfil_idioma")
    suspend fun perfilIdiomas(): List<PerfilIdiomaEntity>

    @Query("SELECT * FROM lista ORDER BY creadoEn, id")
    suspend fun listas(): List<ListaEntity>

    @Query("SELECT * FROM lista_item")
    suspend fun items(): List<ListaItemEntity>

    @Query("SELECT tipo, COUNT(*) AS total FROM perfil GROUP BY tipo")
    suspend fun totalesPorTipo(): List<TotalTipo>

    @Query("SELECT COUNT(*) FROM lista")
    suspend fun contarListas(): Int
}
