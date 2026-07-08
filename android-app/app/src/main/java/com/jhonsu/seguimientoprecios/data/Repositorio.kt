package com.jhonsu.seguimientoprecios.data

import kotlinx.coroutines.flow.Flow

/** Fachada unica sobre los DAOs (Capa 3 - trabajo deterministico). */
class Repositorio(private val db: AppDatabase) {

    val productos: Flow<List<Producto>> = db.productoDao().observarTodos()
    val precios: Flow<List<Precio>> = db.precioDao().observarTodos()
    val tiendas: Flow<List<Tienda>> = db.tiendaDao().observarTodas()

    fun preciosDe(productoId: String): Flow<List<Precio>> =
        db.precioDao().observarPorProducto(productoId)

    suspend fun guardarProducto(producto: Producto) = db.productoDao().upsert(producto)
    suspend fun eliminarProducto(producto: Producto) = db.productoDao().eliminar(producto)

    suspend fun guardarPrecio(precio: Precio) = db.precioDao().upsert(precio)
    suspend fun eliminarPrecio(precio: Precio) = db.precioDao().eliminar(precio)

    suspend fun guardarTienda(tienda: Tienda) = db.tiendaDao().upsert(tienda)
}
