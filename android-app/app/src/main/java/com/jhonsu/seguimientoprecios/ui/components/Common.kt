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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jhonsu.seguimientoprecios.ui.Tendencia
import com.jhonsu.seguimientoprecios.ui.theme.Ambar
import com.jhonsu.seguimientoprecios.ui.theme.Emerald
import com.jhonsu.seguimientoprecios.ui.theme.Rojo
import kotlin.math.abs

@Composable
fun StatCard(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                valor,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                titulo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
