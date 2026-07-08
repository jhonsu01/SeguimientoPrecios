package com.jhonsu.seguimientoprecios.util

import com.jhonsu.seguimientoprecios.data.Precio

/**
 * IA predictiva de precios (Guia.md - Futuras Mejoras): regresion lineal por minimos
 * cuadrados sobre la serie temporal (dias vs precio). Deterministico, sin API externa.
 */
object Prediccion {
    data class Resultado(
        val prediccion: Double,
        val pendientePorDia: Double,
        val hayDatos: Boolean
    )

    fun predecir(precios: List<Precio>, diasAdelante: Int = 7): Resultado {
        if (precios.size < 2) {
            return Resultado(precios.lastOrNull()?.precio ?: 0.0, 0.0, false)
        }
        val ord = precios.sortedBy { it.fecha }
        val base = ord.first().fecha
        val xs = ord.map { (it.fecha - base).toDouble() / 86_400_000.0 } // dias
        val ys = ord.map { it.precio }
        val n = xs.size
        val mx = xs.average()
        val my = ys.average()
        var num = 0.0
        var den = 0.0
        for (i in 0 until n) {
            num += (xs[i] - mx) * (ys[i] - my)
            den += (xs[i] - mx) * (xs[i] - mx)
        }
        val pendiente = if (den == 0.0) 0.0 else num / den
        val intercepto = my - pendiente * mx
        val xPred = xs.last() + diasAdelante
        val pred = (intercepto + pendiente * xPred).coerceAtLeast(0.0)
        return Resultado(pred, pendiente, true)
    }
}
