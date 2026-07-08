package com.jhonsu.seguimientoprecios.util

import java.security.MessageDigest

/** Hash SHA-256 para el PIN de acceso (Guia.md - Seguridad). */
object Seguridad {
    fun sha256(texto: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
