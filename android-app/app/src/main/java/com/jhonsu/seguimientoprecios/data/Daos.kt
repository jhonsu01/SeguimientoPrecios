package com.jhonsu.seguimientoprecios.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos ORDER BY nombre COLLATE NOCASE ASC")
    fun observarTodos(): Flow<List<Producto>>

    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun porId(id: String): Producto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(producto: Producto)

    @Delete
    suspend fun eliminar(producto: Producto)
}

@Dao
interface PrecioDao {
    @Query("SELECT * FROM precios ORDER BY fecha DESC")
    fun observarTodos(): Flow<List<Precio>>

    @Query("SELECT * FROM precios WHERE productoId = :productoId ORDER BY fecha ASC")
    fun observarPorProducto(productoId: String): Flow<List<Precio>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(precio: Precio)

    @Delete
    suspend fun eliminar(precio: Precio)
}

@Dao
interface TiendaDao {
    @Query("SELECT * FROM tiendas ORDER BY nombre COLLATE NOCASE ASC")
    fun observarTodas(): Flow<List<Tienda>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tienda: Tienda)

    @Delete
    suspend fun eliminar(tienda: Tienda)
}
