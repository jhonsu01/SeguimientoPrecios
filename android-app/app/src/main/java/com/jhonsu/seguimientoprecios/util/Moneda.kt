package com.jhonsu.seguimientoprecios.util

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Moneda configurable (Guia.md: app universal). Formatea valores segun el pais:
 * simbolo, decimales y separadores de miles/decimales.
 */
object Moneda {
    data class Def(
        val code: String,
        val nombre: String,
        val symbol: String,
        val decimals: Int,
        val miles: Char,
        val decimal: Char
    ) {
        val etiqueta get() = "$code - $nombre"
    }

    val LISTA = listOf(
        Def("COP", "Peso colombiano", "$", 0, '.', ','),
        Def("USD", "Dolar", "$", 2, ',', '.'),
        Def("EUR", "Euro", "€", 2, '.', ','),
        Def("MXN", "Peso mexicano", "$", 2, ',', '.'),
        Def("ARS", "Peso argentino", "$", 2, '.', ','),
        Def("CLP", "Peso chileno", "$", 0, '.', ','),
        Def("PEN", "Sol peruano", "S/", 2, ',', '.'),
        Def("BRL", "Real brasileno", "R$", 2, '.', ','),
        Def("GBP", "Libra", "£", 2, ',', '.'),
        Def("JPY", "Yen", "¥", 0, ',', '.')
    )

    @Volatile
    var actual: Def = LISTA[0]

    fun porCodigo(code: String?): Def = LISTA.firstOrNull { it.code == code } ?: LISTA[0]

    fun format(valor: Double): String {
        val def = actual
        val neg = valor < 0
        val absoluto = abs(valor)
        val factor = Math.pow(10.0, def.decimals.toDouble())
        val redondeado = (absoluto * factor).roundToLong() / factor
        val entero = redondeado.toLong()
        val frac = ((redondeado - entero) * factor).roundToLong().toInt()

        val sb = StringBuilder()
        if (neg) sb.append("-")
        sb.append(def.symbol).append(agrupar(entero.toString(), def.miles))
        if (def.decimals > 0) {
            sb.append(def.decimal).append(frac.toString().padStart(def.decimals, '0'))
        }
        return sb.toString()
    }

    private fun agrupar(numero: String, sep: Char): String {
        val rev = numero.reversed()
        val out = StringBuilder()
        for (i in rev.indices) {
            if (i > 0 && i % 3 == 0) out.append(sep)
            out.append(rev[i])
        }
        return out.reverse().toString()
    }
}
