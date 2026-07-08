package com.jhonsu.seguimientoprecios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.jhonsu.seguimientoprecios.data.Alacena
import com.jhonsu.seguimientoprecios.data.Producto
import com.jhonsu.seguimientoprecios.ui.components.AlacenaDialog
import com.jhonsu.seguimientoprecios.ui.theme.Ambar
import com.jhonsu.seguimientoprecios.ui.theme.Emerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlacenaScreen(vm: AppViewModel, onAbrirProducto: (String) -> Unit) {
    val productos by vm.productos.collectAsState()
    val alacena by vm.alacena.collectAsState()
    var editando by remember { mutableStateOf<Producto?>(null) }

    fun itemDe(p: Producto): Alacena =
        alacena.find { it.productoId == p.id } ?: Alacena(p.id, 0.0, 1.0)

    val bajoMinimo = productos.filter { itemDe(it).let { a -> a.cantidadActual <= a.cantidadMinima } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Alacena", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (productos.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(24.dp), Alignment.Center) {
                Text(
                    "Agrega productos para gestionar tu inventario.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (bajoMinimo.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Ambar.copy(alpha = 0.14f))
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ShoppingCart, null, tint = Ambar, modifier = Modifier.size(20.dp))
                                Text(
                                    "  Lista de compras (${bajoMinimo.size})",
                                    fontWeight = FontWeight.Bold,
                                    color = Ambar
                                )
                            }
                            Text(
                                bajoMinimo.joinToString(", ") { it.nombre },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "Inventario",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(productos, key = { it.id }) { prod ->
                AlacenaRow(
                    prod = prod,
                    item = itemDe(prod),
                    onMenos = { vm.ajustarStock(prod.id, -1.0) },
                    onMas = { vm.ajustarStock(prod.id, 1.0) },
                    onEditar = { editando = prod }
                )
            }
        }
    }

    editando?.let { p ->
        AlacenaDialog(
            producto = p,
            inicial = itemDe(p),
            onDismiss = { editando = null },
            onGuardar = { vm.guardarAlacena(it); editando = null }
        )
    }
}

@Composable
private fun AlacenaRow(
    prod: Producto,
    item: Alacena,
    onMenos: () -> Unit,
    onMas: () -> Unit,
    onEditar: () -> Unit
) {
    val bajo = item.cantidadActual <= item.cantidadMinima
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(prod.nombre, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Stock: ${fmt(item.cantidadActual)} · min: ${fmt(item.cantidadMinima)} ${prod.unidadMedida}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bajo) Ambar else Emerald
                )
            }
            IconButton(onClick = onMenos) {
                Icon(Icons.Filled.Remove, contentDescription = "Restar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(fmt(item.cantidadActual), fontWeight = FontWeight.Bold)
            IconButton(onClick = onMas) {
                Icon(Icons.Filled.Add, contentDescription = "Sumar", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar minimo", tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

private fun fmt(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
