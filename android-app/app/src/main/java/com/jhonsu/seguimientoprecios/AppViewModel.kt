package com.jhonsu.seguimientoprecios

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jhonsu.seguimientoprecios.data.AppDatabase
import com.jhonsu.seguimientoprecios.data.Precio
import com.jhonsu.seguimientoprecios.data.Producto
import com.jhonsu.seguimientoprecios.data.Repositorio
import com.jhonsu.seguimientoprecios.data.Tienda
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repositorio(AppDatabase.get(app))

    val productos: StateFlow<List<Producto>> =
        repo.productos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val precios: StateFlow<List<Precio>> =
        repo.precios.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tiendas: StateFlow<List<Tienda>> =
        repo.tiendas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun preciosDe(productoId: String): Flow<List<Precio>> = repo.preciosDe(productoId)

    fun guardarProducto(producto: Producto) = viewModelScope.launch {
        repo.guardarProducto(producto)
    }

    fun eliminarProducto(producto: Producto) = viewModelScope.launch {
        repo.eliminarProducto(producto)
    }

    fun guardarPrecio(precio: Precio) = viewModelScope.launch {
        repo.guardarPrecio(precio)
        if (precio.tienda.isNotBlank() &&
            tiendas.value.none { it.nombre.equals(precio.tienda, ignoreCase = true) }
        ) {
            repo.guardarTienda(Tienda(nombre = precio.tienda))
        }
    }

    fun eliminarPrecio(precio: Precio) = viewModelScope.launch {
        repo.eliminarPrecio(precio)
    }
}
