package com.jhonsu.seguimientoprecios.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Producto::class, Precio::class, Tienda::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
    abstract fun precioDao(): PrecioDao
    abstract fun tiendaDao(): TiendaDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "seguimiento.db"
                ).fallbackToDestructiveMigration().build().also { instancia = it }
            }
    }
}
