package com.jhonsu.seguimientoprecios.util

import android.content.Context
import android.content.Intent
import com.jhonsu.seguimientoprecios.data.AppDatabase
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Import/Export de la base de datos (Guia.md): ZIP con el SQLite + un JSON de respaldo.
 */
object Backup {

    private val DB_FILES = listOf(
        "seguimiento.db", "seguimiento.db-wal", "seguimiento.db-shm"
    )

    fun exportar(context: Context, salida: OutputStream, jsonRespaldo: String) {
        ZipOutputStream(salida).use { zip ->
            DB_FILES.forEach { nombre ->
                val f = context.getDatabasePath(nombre)
                if (f.exists()) {
                    zip.putNextEntry(ZipEntry(nombre))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(jsonRespaldo.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    /** Restaura los archivos de BD desde el ZIP. Cierra la BD antes de reemplazar. */
    fun importar(context: Context, entrada: InputStream): Boolean {
        AppDatabase.cerrar()
        var restaurado = false
        ZipInputStream(entrada).use { zip ->
            var e: ZipEntry? = zip.nextEntry
            while (e != null) {
                val nombre = e.name
                if (nombre in DB_FILES) {
                    val destino = context.getDatabasePath(nombre)
                    destino.parentFile?.mkdirs()
                    destino.outputStream().use { out -> zip.copyTo(out) }
                    if (nombre == "seguimiento.db") restaurado = true
                }
                zip.closeEntry()
                e = zip.nextEntry
            }
        }
        return restaurado
    }

    /** Reinicia el proceso para recargar la BD tras un import. */
    fun reiniciarApp(context: Context) {
        val intent: Intent? = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
