package com.jhonsu.seguimientoprecios.data

import kotlinx.coroutines.flow.Flow

/** Fachada unica sobre los DAOs (Capa 3 - trabajo deterministico). */
class Repositorio(private val db: AppDatabase) {

    val productos: Flow<List<Producto>> = db.productoDao().observarTodos()
    val precios: Flow<List<Precio>> = db.precioDao().observarTodos()
    val tiendas: Flow<List<Tienda>> = db.tiendaDao().observarTodas()
    val alacena: Flow<List<Alacena>> = db.alacenaDao().observarTodos()

    fun preciosDe(productoId: String): Flow<List<Precio>> =
        db.precioDao().observarPorProducto(productoId)

    suspend fun guardarProducto(producto: Producto) = db.productoDao().upsert(producto)

    suspend fun eliminarProducto(producto: Producto) {
        db.alacenaDao().eliminarPorProducto(producto.id)
        db.productoDao().eliminar(producto)
    }

    suspend fun guardarPrecio(precio: Precio) = db.precioDao().upsert(precio)
    suspend fun eliminarPrecio(precio: Precio) = db.precioDao().eliminar(precio)

    suspend fun guardarTienda(tienda: Tienda) = db.tiendaDao().upsert(tienda)

    suspend fun guardarAlacena(item: Alacena) = db.alacenaDao().upsert(item)

    /** Reemplaza TODOS los datos (usado al importar un respaldo JSON, multiplataforma). */
    suspend fun reemplazarTodo(
        productos: List<Producto>,
        precios: List<Precio>,
        tiendas: List<Tienda>,
        alacena: List<Alacena>
    ) {
        db.precioDao().borrarTodos()
        db.alacenaDao().borrarTodos()
        db.productoDao().borrarTodos()
        db.tiendaDao().borrarTodas()
        db.productoDao().insertarTodos(productos)
        db.tiendaDao().insertarTodas(tiendas)
        db.precioDao().insertarTodos(precios)
        db.alacenaDao().insertarTodos(alacena)
    }
}
