package com.cinesstar.cinetrack.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cinesstar.cinetrack.domain.GENEROS_PREDEFINIDOS
import com.cinesstar.cinetrack.domain.IDIOMAS_PREDEFINIDOS

@Database(
    entities = [
        PerfilEntity::class,
        SagaEntity::class,
        GeneroEntity::class,
        IdiomaEntity::class,
        PerfilGeneroEntity::class,
        PerfilIdiomaEntity::class,
        ListaEntity::class,
        ListaItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Conversores::class)
abstract class CineDatabase : RoomDatabase() {
    abstract fun perfilDao(): PerfilDao

    abstract fun sagaDao(): SagaDao

    abstract fun catalogoDao(): CatalogoDao

    abstract fun listaDao(): ListaDao

    abstract fun bibliotecaDao(): BibliotecaDao

    abstract fun busquedaDao(): BusquedaDao

    abstract fun copiaDao(): CopiaDao

    companion object {
        const val NOMBRE_ARCHIVO = "cinetrack.db"

        /**
         * Base de datos en el almacenamiento interno de la app (RES-04, RNF-07). Con [nombre] nulo se crea en memoria
         * (tests). No hay migración destructiva: si una migración falla, la app lo indica y no borra nada (CU-23 E1).
         */
        fun construir(
            context: Context,
            nombre: String? = NOMBRE_ARCHIVO,
        ): CineDatabase {
            val builder =
                if (nombre == null) {
                    Room.inMemoryDatabaseBuilder(context.applicationContext, CineDatabase::class.java)
                } else {
                    Room.databaseBuilder(context.applicationContext, CineDatabase::class.java, nombre)
                }
            return builder.addCallback(PrecargaCatalogo).build()
        }
    }
}

/**
 * Precarga de géneros e idiomas (CU-23 FP paso 2). Se ejecuta en cada apertura con `INSERT OR IGNORE` sobre el nombre
 * único: la primera vez crea la lista completa y las siguientes no duplica nada; si una versión futura añade
 * elementos a la lista predefinida, se incorporan sin migración.
 */
internal object PrecargaCatalogo : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            GENEROS_PREDEFINIDOS.forEachIndexed { orden, nombre ->
                db.execSQL("INSERT OR IGNORE INTO genero (nombre, orden) VALUES (?, ?)", arrayOf<Any>(nombre, orden))
            }
            IDIOMAS_PREDEFINIDOS.forEachIndexed { orden, nombre ->
                db.execSQL("INSERT OR IGNORE INTO idioma (nombre, orden) VALUES (?, ?)", arrayOf<Any>(nombre, orden))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
