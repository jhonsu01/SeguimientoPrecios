package com.jhonsu.seguimientoprecios.ui

import com.jhonsu.seguimientoprecios.data.Precio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Tendencia { SUBE, BAJA, ESTABLE, NINGUNA }

/** Formatea un valor como moneda simple: "$1,234.56". */
fun moneda(valor: Double): String = "$" + String.format(Locale.US, "%,.2f", valor)

private val fmtFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
private val fmtFechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

fun fecha(ms: Long): String = fmtFecha.format(Date(ms))
fun fechaHora(ms: Long): String = fmtFechaHora.format(Date(ms))

/**
 * Calcula la tendencia comparando los dos ultimos registros por fecha.
 * Devuelve la tendencia y el porcentaje de variacion (positivo/negativo).
 */
fun tendenciaDe(precios: List<Precio>): Pair<Tendencia, Double> {
    if (precios.size < 2) return Tendencia.NINGUNA to 0.0
    val ord = precios.sortedBy { it.fecha }
    val previo = ord[ord.size - 2].precio
    val actual = ord[ord.size - 1].precio
    if (previo == 0.0) return Tendencia.NINGUNA to 0.0
    val pct = (actual - previo) / previo * 100.0
    val t = when {
        pct > 0.5 -> Tendencia.SUBE
        pct < -0.5 -> Tendencia.BAJA
        else -> Tendencia.ESTABLE
    }
    return t to pct
}
