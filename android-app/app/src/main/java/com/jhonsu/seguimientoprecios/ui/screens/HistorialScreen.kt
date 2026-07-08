package com.jhonsu.seguimientoprecios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jhonsu.seguimientoprecios.AppViewModel
import com.jhonsu.seguimientoprecios.data.Precio
import com.jhonsu.seguimientoprecios.ui.components.PriceChart
import com.jhonsu.seguimientoprecios.ui.components.PrecioDialog
import com.jhonsu.seguimientoprecios.ui.components.StatCard
import com.jhonsu.seguimientoprecios.ui.fechaHora
import com.jhonsu.seguimientoprecios.ui.moneda

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(vm: AppViewModel, productoId: String, onBack: () -> Unit) {
    val productos by vm.productos.collectAsState()
    val producto = productos.find { it.id == productoId }
    val precios by vm.preciosDe(productoId).collectAsState(initial = emptyList())
    val tiendas by vm.tiendas.collectAsState()
    var mostrarPrecio by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(producto?.nombre ?: "Historial", fontWeight = FontWeight.Bold)
                        if (producto != null) {
                            Text(
                                "${producto.categoria} · ${producto.unidadMedida}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarPrecio = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Filled.Add, contentDescription = "Registrar precio") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Evolucion del precio",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        PriceChart(precios)
                    }
                }
            }
            if (precios.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Minimo", moneda(precios.minOf { it.precio }), Modifier.weight(1f))
                        StatCard("Promedio", moneda(precios.map { it.precio }.average()), Modifier.weight(1f))
                        StatCard("Maximo", moneda(precios.maxOf { it.precio }), Modifier.weight(1f))
                    }
                }
            }
            item {
                Text(
                    "Registros (${precios.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (precios.isEmpty()) {
                item {
                    Text(
                        "Toca + para registrar el primer precio.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(precios.sortedByDescending { it.fecha }, key = { it.id }) { precio ->
                    PrecioRow(precio) { vm.eliminarPrecio(precio) }
                }
            }
        }
    }

    if (mostrarPrecio) {
        PrecioDialog(
            productoId = productoId,
            tiendasSugeridas = tiendas.map { it.nombre },
            onDismiss = { mostrarPrecio = false },
            onGuardar = { vm.guardarPrecio(it); mostrarPrecio = false }
        )
    }
}

@Composable
private fun PrecioRow(precio: Precio, onEliminar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    moneda(precio.precio),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    buildString {
                        if (precio.tienda.isNotBlank()) append(precio.tienda) else append("Sin tienda")
                        append(" · ")
                        append(precio.tipoPrecio)
                        if (precio.cantidad != 1.0) append(" · x${precio.cantidad}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                fechaHora(precio.fecha),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onEliminar) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
