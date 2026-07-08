package com.jhonsu.seguimientoprecios.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jhonsu.seguimientoprecios.AppViewModel
import com.jhonsu.seguimientoprecios.data.Precio
import com.jhonsu.seguimientoprecios.data.Producto
import com.jhonsu.seguimientoprecios.ui.components.StatCard
import com.jhonsu.seguimientoprecios.ui.components.TendenciaChip
import com.jhonsu.seguimientoprecios.ui.moneda
import com.jhonsu.seguimientoprecios.ui.tendenciaDe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(vm: AppViewModel, onAbrirProducto: (String) -> Unit) {
    val productos by vm.productos.collectAsState()
    val precios by vm.precios.collectAsState()
    val tiendas by vm.tiendas.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Seguimiento de Precios", fontWeight = FontWeight.Bold)
                        Text(
                            "Tu control de gastos inteligente",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Productos", productos.size.toString(), Modifier.weight(1f))
                    StatCard("Registros", precios.size.toString(), Modifier.weight(1f))
                    StatCard("Tiendas", tiendas.size.toString(), Modifier.weight(1f))
                }
            }
            item {
                Text(
                    "Productos rastreados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (productos.isEmpty()) {
                item {
                    Text(
                        "Aun no hay productos. Ve a la pestana Productos y agrega el primero.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(productos, key = { it.id }) { prod ->
                    val propios = precios.filter { it.productoId == prod.id }
                    val ultimo = propios.maxByOrNull { it.fecha }
                    val (tend, pct) = tendenciaDe(propios)
                    ProductoResumenCard(prod, ultimo, tend, pct) { onAbrirProducto(prod.id) }
                }
            }
            item { Spacer(Modifier.width(1.dp)) }
        }
    }
}

@Composable
private fun ProductoResumenCard(
    prod: Producto,
    ultimo: Precio?,
    tendencia: com.jhonsu.seguimientoprecios.ui.Tendencia,
    pct: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(prod.nombre, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${prod.categoria} · ${prod.unidadMedida}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    ultimo?.let { moneda(it.precio) } ?: "—",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(2.dp))
                TendenciaChip(tendencia, pct)
            }
        }
    }
}
