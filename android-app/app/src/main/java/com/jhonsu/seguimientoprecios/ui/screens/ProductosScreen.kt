package com.jhonsu.seguimientoprecios.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.jhonsu.seguimientoprecios.data.Producto
import com.jhonsu.seguimientoprecios.ui.components.ProductoDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductosScreen(vm: AppViewModel, onAbrirProducto: (String) -> Unit) {
    val productos by vm.productos.collectAsState()
    var mostrarNuevo by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Producto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Productos", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarNuevo = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Filled.Add, contentDescription = "Agregar producto") }
        }
    ) { padding ->
        if (productos.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(24.dp), Alignment.Center) {
                Text(
                    "Toca + para agregar tu primer producto.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(productos, key = { it.id }) { prod ->
                    ProductoRow(
                        prod = prod,
                        onAbrir = { onAbrirProducto(prod.id) },
                        onEditar = { editando = prod },
                        onEliminar = { vm.eliminarProducto(prod) }
                    )
                }
            }
        }
    }

    if (mostrarNuevo) {
        ProductoDialog(
            inicial = null,
            onDismiss = { mostrarNuevo = false },
            onGuardar = { vm.guardarProducto(it); mostrarNuevo = false }
        )
    }
    editando?.let { e ->
        ProductoDialog(
            inicial = e,
            onDismiss = { editando = null },
            onGuardar = { vm.guardarProducto(it); editando = null }
        )
    }
}

@Composable
private fun ProductoRow(
    prod: Producto,
    onAbrir: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAbrir),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(prod.nombre, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                Text(
                    buildString {
                        append(prod.categoria)
                        append(" · ")
                        append(prod.unidadMedida)
                        if (prod.tipo.isNotBlank()) append(" · ${prod.tipo}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
