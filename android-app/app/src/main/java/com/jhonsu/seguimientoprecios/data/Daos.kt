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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(items: List<Producto>)

    @Delete
    suspend fun eliminar(producto: Producto)

    @Query("DELETE FROM productos")
    suspend fun borrarTodos()
}

@Dao
interface PrecioDao {
    @Query("SELECT * FROM precios ORDER BY fecha DESC")
    fun observarTodos(): Flow<List<Precio>>

    @Query("SELECT * FROM precios WHERE productoId = :productoId ORDER BY fecha ASC")
    fun observarPorProducto(productoId: String): Flow<List<Precio>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(precio: Precio)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(items: List<Precio>)

    @Delete
    suspend fun eliminar(precio: Precio)

    @Query("DELETE FROM precios")
    suspend fun borrarTodos()
}

@Dao
interface AlacenaDao {
    @Query("SELECT * FROM alacena")
    fun observarTodos(): Flow<List<Alacena>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: Alacena)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(items: List<Alacena>)

    @Query("DELETE FROM alacena WHERE productoId = :productoId")
    suspend fun eliminarPorProducto(productoId: String)

    @Query("DELETE FROM alacena")
    suspend fun borrarTodos()
}

@Dao
interface TiendaDao {
    @Query("SELECT * FROM tiendas ORDER BY nombre COLLATE NOCASE ASC")
    fun observarTodas(): Flow<List<Tienda>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tienda: Tienda)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(items: List<Tienda>)

    @Delete
    suspend fun eliminar(tienda: Tienda)

    @Query("DELETE FROM tiendas")
    suspend fun borrarTodas()
}
