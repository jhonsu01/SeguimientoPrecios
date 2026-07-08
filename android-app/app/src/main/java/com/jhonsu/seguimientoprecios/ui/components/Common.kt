package com.jhonsu.seguimientoprecios.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhonsu.seguimientoprecios.ui.Tendencia
import com.jhonsu.seguimientoprecios.ui.theme.Ambar
import com.jhonsu.seguimientoprecios.ui.theme.Emerald
import com.jhonsu.seguimientoprecios.ui.theme.Rojo
import kotlin.math.abs

/**
 * Texto que se auto-reduce hasta caber en UNA sola linea (sin recortar digitos).
 * Util para valores monetarios grandes (ej. pesos colombianos) en tarjetas estrechas.
 */
@Composable
fun AutoSizeValor(
    texto: String,
    color: Color = MaterialTheme.colorScheme.primary,
    tamanoInicial: Int = 22,
    tamanoMinimo: Int = 12,
    modifier: Modifier = Modifier
) {
    var fontSize by remember(texto) { mutableStateOf(tamanoInicial.sp) }
    var listo by remember(texto) { mutableStateOf(false) }
    Text(
        text = texto,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Visible,
        fontWeight = FontWeight.Bold,
        color = color,
        fontSize = fontSize,
        lineHeight = fontSize,
        modifier = modifier.graphicsLayer { alpha = if (listo) 1f else 0f },
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize.value > tamanoMinimo) {
                fontSize = (fontSize.value * 0.92f).sp
            } else if (!listo) {
                listo = true
            }
        }
    )
}

@Composable
fun StatCard(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            AutoSizeValor(valor, modifier = Modifier.padding(bottom = 2.dp))
            Text(
                titulo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TendenciaChip(tendencia: Tendencia, pct: Double) {
    val (color, icon, txt) = when (tendencia) {
        Tendencia.SUBE -> Triple(Rojo, Icons.Filled.ArrowUpward, "+${"%.1f".format(abs(pct))}%")
        Tendencia.BAJA -> Triple(Emerald, Icons.Filled.ArrowDownward, "-${"%.1f".format(abs(pct))}%")
        Tendencia.ESTABLE -> Triple(Ambar, Icons.Filled.Remove, "estable")
        Tendencia.NINGUNA -> Triple(Color.Gray, Icons.Filled.Remove, "—")
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(txt, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}
