package com.jhonsu.seguimientoprecios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import com.jhonsu.seguimientoprecios.ui.components.PriceChart
import com.jhonsu.seguimientoprecios.ui.components.StatCard
import com.jhonsu.seguimientoprecios.ui.components.TendenciaChip
import com.jhonsu.seguimientoprecios.ui.moneda
import com.jhonsu.seguimientoprecios.ui.tendenciaDe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraficosScreen(vm: AppViewModel) {
    val productos by vm.productos.collectAsState()
    val todos by vm.precios.collectAsState()

    var seleccionId by remember { mutableStateOf<String?>(null) }
    var abierto by remember { mutableStateOf(false) }
    val actualId = seleccionId ?: productos.firstOrNull()?.id
    val producto = productos.find { it.id == actualId }
    val precios = todos.filter { it.productoId == actualId }
    val (tend, pct) = tendenciaDe(precios)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Graficos", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (productos.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(24.dp), Alignment.Center) {
                Text(
                    "Agrega productos y precios para ver graficos.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
                OutlinedTextField(
                    value = producto?.nombre ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Producto") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                    productos.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.nombre) },
                            onClick = { seleccionId = p.id; abierto = false }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Variacion reciente",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        TendenciaChip(tend, pct)
                    }
                    PriceChart(precios)
                }
            }

            if (precios.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Minimo", moneda(precios.minOf { it.precio }), Modifier.weight(1f))
                    StatCard("Promedio", moneda(precios.map { it.precio }.average()), Modifier.weight(1f))
                    StatCard("Maximo", moneda(precios.maxOf { it.precio }), Modifier.weight(1f))
                }
            } else {
                Text(
                    "Este producto aun no tiene precios registrados.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
