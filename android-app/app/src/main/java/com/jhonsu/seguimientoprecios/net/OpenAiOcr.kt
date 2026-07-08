package com.jhonsu.seguimientoprecios.net

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.jhonsu.seguimientoprecios.util.Moneda
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

data class ItemOcr(
    val nombre: String,
    val precio: Double,
    val cantidad: Double,
    val unidad: String
)

data class OcrResultado(
    val tienda: String,
    val items: List<ItemOcr>
)

/** OCR de facturas via OpenAI (vision + PDF). Acepta imagen, PDF o ZIP (con PDF/imagen dentro). */
object OpenAiOcr {

    private const val ENDPOINT = "https://api.openai.com/v1/chat/completions"
    private const val MODELO = "gpt-4o-mini"

    private fun prompt(): String = """Eres un extractor de facturas/recibos. Devuelve SOLO un objeto JSON con esta forma:
{"tienda": "nombre del establecimiento o vacio", "productos": [{"nombre": "nombre limpio", "precio": 0, "cantidad": 1, "unidad": "unidad|kg|g|L|ml|lb"}]}

REGLAS DE NUMEROS (MUY IMPORTANTE):
- La moneda es ${Moneda.actual.code}. Muchos paises (Colombia, etc.) usan el PUNTO como separador de MILES y la COMA como separador decimal.
- Interpreta los montos en ese contexto. Ejemplos: "9.120" = 9120 ; "22.950" = 22950 ; "1.234.567" = 1234567 ; "1.234,50" = 1234.50.
- Devuelve "precio" como numero real SIN separadores de miles (ej: 9120, NUNCA 9.12).
- Usa el precio UNITARIO (columna VR. UNIT o similar) cuando exista.
Normaliza los nombres (sin codigos). Si no hay datos, usa listas/valores vacios."""

    /** Lee la Uri (imagen/PDF/ZIP) y devuelve (bytes, mime) listos para enviar. */
    fun prepararDesdeUri(context: Context, uri: Uri): Pair<ByteArray, String> {
        val mimeOrig = context.contentResolver.getType(uri) ?: adivinarMime(uri.toString())
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("No se pudo leer el archivo.")
        val ruta = uri.toString().lowercase()
        return when {
            mimeOrig.contains("zip") || ruta.endsWith(".zip") -> extraerDeZip(bytes)
            mimeOrig == "application/pdf" || ruta.endsWith(".pdf") -> bytes to "application/pdf"
            else -> escalarImagen(bytes) to "image/jpeg"
        }
    }

    private fun adivinarMime(nombre: String): String {
        val n = nombre.lowercase()
        return when {
            n.endsWith(".pdf") -> "application/pdf"
            n.endsWith(".zip") -> "application/zip"
            n.endsWith(".png") -> "image/png"
            n.endsWith(".webp") -> "image/webp"
            else -> "image/jpeg"
        }
    }

    private fun extraerDeZip(bytes: ByteArray): Pair<ByteArray, String> {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                val n = e.name.lowercase()
                val esValido = n.endsWith(".pdf") || n.endsWith(".jpg") || n.endsWith(".jpeg") ||
                    n.endsWith(".png") || n.endsWith(".webp")
                if (!e.isDirectory && esValido) {
                    val contenido = zip.readBytes()
                    val mime = if (n.endsWith(".pdf")) "application/pdf" else "image/jpeg"
                    return if (mime == "application/pdf") contenido to mime
                    else escalarImagen(contenido) to "image/jpeg"
                }
                e = zip.nextEntry
            }
        }
        throw IllegalArgumentException("El ZIP no contiene un PDF ni una imagen de factura.")
    }

    private fun escalarImagen(bytes: ByteArray, maxDim: Int = 1400): ByteArray {
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalArgumentException("Formato de imagen no soportado.")
        val mayor = maxOf(bmp.width, bmp.height)
        val escalada = if (mayor <= maxDim) bmp else {
            val f = maxDim.toFloat() / mayor
            Bitmap.createScaledBitmap(bmp, (bmp.width * f).toInt(), (bmp.height * f).toInt(), true)
        }
        val baos = ByteArrayOutputStream()
        escalada.compress(Bitmap.CompressFormat.JPEG, 82, baos)
        return baos.toByteArray()
    }

    suspend fun extraer(apiKey: String, bytes: ByteArray, mime: String): OcrResultado =
        withContext(Dispatchers.IO) {
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val parteArchivo = if (mime == "application/pdf") {
                JSONObject().put("type", "file").put(
                    "file",
                    JSONObject().put("filename", "factura.pdf")
                        .put("file_data", "data:application/pdf;base64,$b64")
                )
            } else {
                JSONObject().put("type", "image_url").put(
                    "image_url", JSONObject().put("url", "data:$mime;base64,$b64")
                )
            }
            val contenido = JSONArray()
                .put(JSONObject().put("type", "text").put("text", prompt()))
                .put(parteArchivo)

            val body = JSONObject()
                .put("model", MODELO)
                .put("response_format", JSONObject().put("type", "json_object"))
                .put("max_tokens", 2000)
                .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", contenido)))

            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 30000
                readTimeout = 90000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val respuesta = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) throw RuntimeException("OpenAI ($code): ${respuesta.take(300)}")

            val content = JSONObject(respuesta)
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
            parsear(content)
        }

    private fun parsear(contenido: String): OcrResultado {
        val obj = JSONObject(contenido)
        val tienda = obj.optString("tienda", "")
        val arr = obj.optJSONArray("productos") ?: JSONArray()
        val items = ArrayList<ItemOcr>()
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            val nombre = p.optString("nombre", "").trim()
            if (nombre.isEmpty()) continue
            items.add(
                ItemOcr(
                    nombre = nombre,
                    precio = p.optDouble("precio", 0.0),
                    cantidad = p.optDouble("cantidad", 1.0),
                    unidad = p.optString("unidad", "unidad")
                )
            )
        }
        return OcrResultado(tienda, items)
    }
}
