package com.jhonsu.seguimientoprecios.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Producto::class, Precio::class, Tienda::class, Alacena::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
    abstract fun precioDao(): PrecioDao
    abstract fun tiendaDao(): TiendaDao
    abstract fun alacenaDao(): AlacenaDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        // v1 -> v2: agrega la tabla de inventario "alacena" SIN perder datos existentes.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `alacena` (" +
                        "`productoId` TEXT NOT NULL, " +
                        "`cantidadActual` REAL NOT NULL, " +
                        "`cantidadMinima` REAL NOT NULL, " +
                        "PRIMARY KEY(`productoId`))"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: build(context).also { instancia = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "seguimiento.db"
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()

        /** Cierra la instancia (necesario antes de reemplazar el archivo en import). */
        fun cerrar() {
            synchronized(this) {
                instancia?.close()
                instancia = null
            }
        }
    }
}
