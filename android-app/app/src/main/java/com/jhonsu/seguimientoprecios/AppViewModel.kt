package com.jhonsu.seguimientoprecios

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jhonsu.seguimientoprecios.data.Alacena
import com.jhonsu.seguimientoprecios.data.AppDatabase
import com.jhonsu.seguimientoprecios.data.Precio
import com.jhonsu.seguimientoprecios.data.Prefs
import com.jhonsu.seguimientoprecios.data.Producto
import com.jhonsu.seguimientoprecios.data.Repositorio
import com.jhonsu.seguimientoprecios.data.Tienda
import com.jhonsu.seguimientoprecios.net.OcrResultado
import com.jhonsu.seguimientoprecios.net.OpenAiOcr
import com.jhonsu.seguimientoprecios.util.Backup
import com.jhonsu.seguimientoprecios.util.Seguridad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repositorio(AppDatabase.get(app))
    private val prefs = Prefs(app)

    val productos: StateFlow<List<Producto>> =
        repo.productos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val precios: StateFlow<List<Precio>> =
        repo.precios.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tiendas: StateFlow<List<Tienda>> =
        repo.tiendas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alacena: StateFlow<List<Alacena>> =
        repo.alacena.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun preciosDe(productoId: String): Flow<List<Precio>> = repo.preciosDe(productoId)

    // ---------- Productos / Precios ----------
    fun guardarProducto(producto: Producto) = viewModelScope.launch {
        repo.guardarProducto(producto)
    }

    fun eliminarProducto(producto: Producto) = viewModelScope.launch {
        repo.eliminarProducto(producto)
    }

    fun guardarPrecio(precio: Precio) = viewModelScope.launch {
        repo.guardarPrecio(precio)
        registrarTiendaSiNueva(precio.tienda)
    }

    fun eliminarPrecio(precio: Precio) = viewModelScope.launch {
        repo.eliminarPrecio(precio)
    }

    private suspend fun registrarTiendaSiNueva(nombre: String) {
        if (nombre.isNotBlank() &&
            tiendas.value.none { it.nombre.equals(nombre, ignoreCase = true) }
        ) {
            repo.guardarTienda(Tienda(nombre = nombre))
        }
    }

    // ---------- Alacena ----------
    fun guardarAlacena(item: Alacena) = viewModelScope.launch { repo.guardarAlacena(item) }

    fun ajustarStock(productoId: String, delta: Double) = viewModelScope.launch {
        val actual = alacena.value.find { it.productoId == productoId }
            ?: Alacena(productoId)
        val nuevo = (actual.cantidadActual + delta).coerceAtLeast(0.0)
        repo.guardarAlacena(actual.copy(cantidadActual = nuevo))
    }

    // ---------- Seguridad (PIN) ----------
    val tienePin: Boolean get() = prefs.tienePin
    fun verificarPin(pin: String): Boolean = prefs.pinHash == Seguridad.sha256(pin)
    fun definirPin(pin: String) { prefs.pinHash = Seguridad.sha256(pin) }
    fun quitarPin() { prefs.pinHash = null }

    // ---------- OpenAI ----------
    fun getOpenAiKey(): String = prefs.openAiKey
    fun setOpenAiKey(key: String) { prefs.openAiKey = key.trim() }

    suspend fun correrOcr(uri: Uri): OcrResultado {
        val key = prefs.openAiKey
        require(key.isNotBlank()) { "Configura tu API key de OpenAI en Ajustes." }
        val b64 = withContext(Dispatchers.IO) {
            OpenAiOcr.uriABase64(getApplication<Application>(), uri)
        }
        return OpenAiOcr.extraer(key, b64)
    }

    fun agregarDesdeOcr(res: OcrResultado) = viewModelScope.launch {
        res.items.forEach { item ->
            val existente = productos.value.firstOrNull {
                it.nombre.equals(item.nombre, ignoreCase = true)
            }
            val producto = existente ?: Producto(
                nombre = item.nombre,
                unidadMedida = item.unidad.ifBlank { "unidad" }
            ).also { repo.guardarProducto(it) }

            repo.guardarPrecio(
                Precio(
                    productoId = producto.id,
                    precio = item.precio,
                    cantidad = if (item.cantidad > 0) item.cantidad else 1.0,
                    tienda = res.tienda
                )
            )
        }
        registrarTiendaSiNueva(res.tienda)
    }

    // ---------- Import / Export ----------
    fun exportar(salida: OutputStream) = viewModelScope.launch(Dispatchers.IO) {
        Backup.exportar(getApplication<Application>(), salida, construirJson())
        salida.close()
    }

    fun importar(entrada: InputStream) = viewModelScope.launch {
        withContext(Dispatchers.IO) { Backup.importar(getApplication<Application>(), entrada) }
        Backup.reiniciarApp(getApplication<Application>())
    }

    private fun construirJson(): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("productos", JSONArray().apply {
            productos.value.forEach {
                put(
                    JSONObject()
                        .put("id", it.id).put("nombre", it.nombre)
                        .put("categoria", it.categoria).put("tipo", it.tipo)
                        .put("unidadMedida", it.unidadMedida).put("creadoEn", it.creadoEn)
                )
            }
        })
        root.put("precios", JSONArray().apply {
            precios.value.forEach {
                put(
                    JSONObject()
                        .put("id", it.id).put("productoId", it.productoId)
                        .put("precio", it.precio).put("cantidad", it.cantidad)
                        .put("tipoPrecio", it.tipoPrecio).put("tienda", it.tienda)
                        .put("fecha", it.fecha)
                )
            }
        })
        root.put("tiendas", JSONArray().apply {
            tiendas.value.forEach { put(JSONObject().put("id", it.id).put("nombre", it.nombre)) }
        })
        root.put("alacena", JSONArray().apply {
            alacena.value.forEach {
                put(
                    JSONObject().put("productoId", it.productoId)
                        .put("cantidadActual", it.cantidadActual)
                        .put("cantidadMinima", it.cantidadMinima)
                )
            }
        })
        return root.toString(2)
    }
}
