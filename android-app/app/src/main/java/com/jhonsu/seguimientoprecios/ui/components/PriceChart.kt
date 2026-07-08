package com.jhonsu.seguimientoprecios.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhonsu.seguimientoprecios.data.Precio
import com.jhonsu.seguimientoprecios.ui.moneda

/**
 * Grafico de linea de la evolucion de un precio en el tiempo.
 * Dibujado 100% en Canvas (sin librerias externas -> CI mas robusto).
 */
@Composable
fun PriceChart(
    precios: List<Precio>,
    modifier: Modifier = Modifier
) {
    if (precios.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Sin datos de precios todavia",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic
            )
        }
        return
    }

    val puntos = precios.sortedBy { it.fecha }
    val valores = puntos.map { it.precio }
    val minV = valores.min()
    val maxV = valores.max()
    val rango = (maxV - minV).let { if (it == 0.0) 1.0 else it }

    val lineColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val pointColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fillTop = lineColor.copy(alpha = 0.28f)
    val fillBottom = lineColor.copy(alpha = 0.02f)

    Box(modifier = modifier.fillMaxWidth().height(200.dp).padding(top = 4.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftPad = 64f
            val rightPad = 12f
            val topPad = 12f
            val bottomPad = 24f
            val w = size.width - leftPad - rightPad
            val h = size.height - topPad - bottomPad

            // Lineas guia horizontales + etiquetas de valor
            val filas = 4
            for (i in 0..filas) {
                val y = topPad + h * i / filas
                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + w, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
                )
                val valor = maxV - rango * i / filas
                drawContext.canvas.nativeCanvas.drawText(
                    moneda(valor),
                    8f,
                    y + 4.sp.toPx() / 2,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(
                            (labelColor.alpha * 255).toInt(),
                            (labelColor.red * 255).toInt(),
                            (labelColor.green * 255).toInt(),
                            (labelColor.blue * 255).toInt()
                        )
                        textSize = 10.sp.toPx()
                        isAntiAlias = true
                    }
                )
            }

            fun px(index: Int): Float =
                if (puntos.size == 1) leftPad + w / 2
                else leftPad + w * index / (puntos.size - 1)

            fun py(valor: Double): Float =
                topPad + (h - (h * ((valor - minV) / rango)).toFloat())

            // Area bajo la curva
            if (puntos.size >= 2) {
                val fill = Path().apply {
                    moveTo(px(0), py(puntos[0].precio))
                    puntos.forEachIndexed { i, p -> lineTo(px(i), py(p.precio)) }
                    lineTo(px(puntos.size - 1), topPad + h)
                    lineTo(px(0), topPad + h)
                    close()
                }
                drawPath(
                    path = fill,
                    brush = Brush.verticalGradient(
                        listOf(fillTop, fillBottom),
                        startY = topPad,
                        endY = topPad + h
                    )
                )
            }

            // Linea principal
            if (puntos.size >= 2) {
                val linea = Path().apply {
                    moveTo(px(0), py(puntos[0].precio))
                    puntos.forEachIndexed { i, p -> lineTo(px(i), py(p.precio)) }
                }
                drawPath(linea, color = lineColor, style = Stroke(width = 4f))
            }

            // Puntos
            puntos.forEachIndexed { i, p ->
                drawCircle(
                    color = pointColor,
                    radius = 5f,
                    center = Offset(px(i), py(p.precio))
                )
                drawCircle(
                    color = Color.White,
                    radius = 2f,
                    center = Offset(px(i), py(p.precio))
                )
            }
        }
    }
}
