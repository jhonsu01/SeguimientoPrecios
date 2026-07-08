package com.jhonsu.seguimientoprecios.util

import android.content.Context
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Import/Export de la base de datos (Guia.md). El ZIP contiene:
 *  - backup.json  -> fuente de verdad PORTABLE (import multiplataforma Android <-> Windows)
 *  - seguimiento.db -> copia cruda del SQLite (bonus, mismo dispositivo)
 *
 * El import SIEMPRE se hace desde backup.json (no se reemplaza el archivo de Room,
 * lo que causaba fallos de hash de identidad al mover el ZIP entre plataformas).
 */
object Backup {

    fun exportar(context: Context, salida: OutputStream, jsonRespaldo: String) {
        ZipOutputStream(salida).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(jsonRespaldo.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            val db = context.getDatabasePath("seguimiento.db")
            if (db.exists()) {
                zip.putNextEntry(ZipEntry("seguimiento.db"))
                db.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /** Extrae el contenido de backup.json del ZIP. */
    fun leerJsonDeZip(entrada: InputStream): String {
        ZipInputStream(entrada).use { zip ->
            var e: ZipEntry? = zip.nextEntry
            while (e != null) {
                if (e.name == "backup.json") {
                    return zip.readBytes().toString(Charsets.UTF_8)
                }
                e = zip.nextEntry
            }
        }
        throw IllegalArgumentException("El ZIP no contiene backup.json (respaldo no valido).")
    }
}
