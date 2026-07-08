package com.jhonsu.seguimientoprecios.net

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

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

/** OCR de facturas via OpenAI (vision). La API key la provee el usuario (Ajustes). */
object OpenAiOcr {

    private const val ENDPOINT = "https://api.openai.com/v1/chat/completions"
    private const val MODELO = "gpt-4o-mini"

    private const val PROMPT = """Eres un extractor de facturas. Analiza la imagen de la factura/recibo y devuelve
SOLO un objeto JSON con esta forma exacta:
{"tienda": "nombre del establecimiento o vacio", "productos": [{"nombre": "nombre limpio del producto", "precio": 0.0, "cantidad": 1, "unidad": "unidad|kg|g|L|ml|lb"}]}
Normaliza los nombres (sin codigos ni abreviaturas raras). El precio es el precio unitario numerico sin simbolos. Si no hay datos, usa listas/valores vacios."""

    fun uriABase64(context: Context, uri: Uri, maxDim: Int = 1024): String {
        val bmp: Bitmap = context.contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input)
        } ?: throw IllegalArgumentException("No se pudo leer la imagen.")
        val escalada = escalar(bmp, maxDim)
        val baos = ByteArrayOutputStream()
        escalada.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    private fun escalar(bmp: Bitmap, maxDim: Int): Bitmap {
        val w = bmp.width
        val h = bmp.height
        val mayor = maxOf(w, h)
        if (mayor <= maxDim) return bmp
        val factor = maxDim.toFloat() / mayor
        return Bitmap.createScaledBitmap(bmp, (w * factor).toInt(), (h * factor).toInt(), true)
    }

    suspend fun extraer(apiKey: String, imagenBase64: String): OcrResultado =
        withContext(Dispatchers.IO) {
            val contenido = JSONArray()
                .put(JSONObject().put("type", "text").put("text", PROMPT))
                .put(
                    JSONObject().put("type", "image_url").put(
                        "image_url",
                        JSONObject().put("url", "data:image/jpeg;base64,$imagenBase64")
                    )
                )
            val body = JSONObject()
                .put("model", MODELO)
                .put("response_format", JSONObject().put("type", "json_object"))
                .put("max_tokens", 1500)
                .put(
                    "messages",
                    JSONArray().put(
                        JSONObject().put("role", "user").put("content", contenido)
                    )
                )

            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 30000
                readTimeout = 60000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val respuesta = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                throw RuntimeException("OpenAI ($code): ${respuesta.take(300)}")
            }

            val content = JSONObject(respuesta)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
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
