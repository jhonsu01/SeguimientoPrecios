package com.jhonsu.seguimientoprecios

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.jhonsu.seguimientoprecios.util.Moneda
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

    // Estado de desbloqueo (en el VM para que sobreviva a recreate() al cambiar moneda).
    var desbloqueado by mutableStateOf(false)
    // Mensaje efimero para el usuario (import, etc.)
    var mensaje by mutableStateOf<String?>(null)

    init {
        Moneda.actual = Moneda.porCodigo(prefs.moneda)
        desbloqueado = !prefs.tienePin
    }

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
    fun guardarProducto(producto: Producto) = viewModelScope.launch { repo.guardarProducto(producto) }
    fun eliminarProducto(producto: Producto) = viewModelScope.launch { repo.eliminarProducto(producto) }

    fun guardarPrecio(precio: Precio) = viewModelScope.launch {
        repo.guardarPrecio(precio)
        registrarTiendaSiNueva(precio.tienda)
    }

    fun eliminarPrecio(precio: Precio) = viewModelScope.launch { repo.eliminarPrecio(precio) }

    private suspend fun registrarTiendaSiNueva(nombre: String) {
        if (nombre.isNotBlank() && tiendas.value.none { it.nombre.equals(nombre, ignoreCase = true) }) {
            repo.guardarTienda(Tienda(nombre = nombre))
        }
    }

    // ---------- Alacena ----------
    fun guardarAlacena(item: Alacena) = viewModelScope.launch { repo.guardarAlacena(item) }

    fun ajustarStock(productoId: String, delta: Double) = viewModelScope.launch {
        val actual = alacena.value.find { it.productoId == productoId } ?: Alacena(productoId)
        repo.guardarAlacena(actual.copy(cantidadActual = (actual.cantidadActual + delta).coerceAtLeast(0.0)))
    }

    // ---------- Seguridad (PIN) ----------
    val tienePin: Boolean get() = prefs.tienePin
    fun verificarPin(pin: String): Boolean = prefs.pinHash == Seguridad.sha256(pin)
    fun definirPin(pin: String) { prefs.pinHash = Seguridad.sha256(pin) }
    fun quitarPin() { prefs.pinHash = null }

    // ---------- Moneda ----------
    fun getMoneda(): String = prefs.moneda
    fun setMoneda(code: String) {
        prefs.moneda = code
        Moneda.actual = Moneda.porCodigo(code)
    }

    // ---------- OpenAI ----------
    fun getOpenAiKey(): String = prefs.openAiKey
    fun setOpenAiKey(key: String) { prefs.openAiKey = key.trim() }

    suspend fun correrOcr(uri: Uri): OcrResultado {
        val key = prefs.openAiKey
        require(key.isNotBlank()) { "Configura tu API key de OpenAI en Ajustes." }
        val (bytes, mime) = withContext(Dispatchers.IO) {
            OpenAiOcr.prepararDesdeUri(getApplication<Application>(), uri)
        }
        return OpenAiOcr.extraer(key, bytes, mime)
    }

    fun agregarDesdeOcr(res: OcrResultado) = viewModelScope.launch {
        res.items.forEach { item ->
            val existente = productos.value.firstOrNull { it.nombre.equals(item.nombre, ignoreCase = true) }
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

    fun importar(entrada: InputStream) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val json = Backup.leerJsonDeZip(entrada)
            val root = JSONObject(json)
            repo.reemplazarTodo(
                jsonAProductos(root.optJSONArray("productos")),
                jsonAPrecios(root.optJSONArray("precios")),
                jsonATiendas(root.optJSONArray("tiendas")),
                jsonAAlacena(root.optJSONArray("alacena"))
            )
            mensaje = "Datos importados correctamente."
        } catch (e: Exception) {
            mensaje = "No se pudo importar: ${e.message}"
        } finally {
            try { entrada.close() } catch (_: Exception) {}
        }
    }

    private fun construirJson(): String {
        val root = JSONObject()
        root.put("version", 3)
        root.put("app", "SeguimientoPrecios")
        root.put("productos", JSONArray().apply {
            productos.value.forEach {
                put(
                    JSONObject()
                        .put("id", it.id).put("nombre", it.nombre)
                        .put("categoria", it.categoria).put("tipo", it.tipo)
                        .put("codigoBarras", it.codigoBarras ?: JSONObject.NULL)
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
            tiendas.value.forEach {
                put(JSONObject().put("id", it.id).put("nombre", it.nombre)
                    .put("ubicacion", it.ubicacion ?: JSONObject.NULL))
            }
        })
        root.put("alacena", JSONArray().apply {
            alacena.value.forEach {
                put(JSONObject().put("productoId", it.productoId)
                    .put("cantidadActual", it.cantidadActual)
                    .put("cantidadMinima", it.cantidadMinima))
            }
        })
        return root.toString(2)
    }

    private fun jsonAProductos(arr: JSONArray?): List<Producto> {
        val out = ArrayList<Producto>()
        if (arr == null) return out
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Producto(
                    id = o.optString("id"),
                    nombre = o.optString("nombre"),
                    categoria = o.optString("categoria", "General"),
                    tipo = o.optString("tipo", ""),
                    codigoBarras = o.optString("codigoBarras", "").ifBlank { null },
                    unidadMedida = o.optString("unidadMedida", "unidad"),
                    creadoEn = o.optLong("creadoEn", System.currentTimeMillis())
                )
            )
        }
        return out
    }

    private fun jsonAPrecios(arr: JSONArray?): List<Precio> {
        val out = ArrayList<Precio>()
        if (arr == null) return out
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Precio(
                    id = o.optString("id"),
                    productoId = o.optString("productoId"),
                    precio = o.optDouble("precio", 0.0),
                    cantidad = o.optDouble("cantidad", 1.0),
                    tipoPrecio = o.optString("tipoPrecio", "unitario"),
                    tienda = o.optString("tienda", ""),
                    fecha = o.optLong("fecha", System.currentTimeMillis())
                )
            )
        }
        return out
    }

    private fun jsonATiendas(arr: JSONArray?): List<Tienda> {
        val out = ArrayList<Tienda>()
        if (arr == null) return out
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Tienda(
                    id = o.optString("id"),
                    nombre = o.optString("nombre"),
                    ubicacion = o.optString("ubicacion", "").ifBlank { null }
                )
            )
        }
        return out
    }

    private fun jsonAAlacena(arr: JSONArray?): List<Alacena> {
        val out = ArrayList<Alacena>()
        if (arr == null) return out
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Alacena(
                    productoId = o.optString("productoId"),
                    cantidadActual = o.optDouble("cantidadActual", 0.0),
                    cantidadMinima = o.optDouble("cantidadMinima", 1.0)
                )
            )
        }
        return out
    }
}
