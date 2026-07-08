package com.jhonsu.seguimientoprecios.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jhonsu.seguimientoprecios.data.Precio
import com.jhonsu.seguimientoprecios.data.Producto

val UNIDADES = listOf("unidad", "ml", "L", "g", "kg", "lb")
val TIPOS_PRECIO = listOf("unitario", "promocion")
val CATEGORIAS = listOf("General", "Alimentos", "Bebidas", "Aseo", "Hogar", "Tecnologia", "Otros")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownCampo(
    etiqueta: String,
    opciones: List<String>,
    seleccion: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = abierto,
        onExpandedChange = { abierto = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = seleccion,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { op ->
                DropdownMenuItem(
                    text = { Text(op) },
                    onClick = { onSelect(op); abierto = false }
                )
            }
        }
    }
}

@Composable
fun ProductoDialog(
    inicial: Producto?,
    onDismiss: () -> Unit,
    onGuardar: (Producto) -> Unit
) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var categoria by remember { mutableStateOf(inicial?.categoria ?: "General") }
    var tipo by remember { mutableStateOf(inicial?.tipo ?: "") }
    var unidad by remember { mutableStateOf(inicial?.unidadMedida ?: "unidad") }
    var codigo by remember { mutableStateOf(inicial?.codigoBarras ?: "") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inicial == null) "Nuevo producto" else "Editar producto") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; error = false },
                    label = { Text("Nombre *") },
                    isError = error,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownCampo("Categoria", CATEGORIAS, categoria, { categoria = it })
                OutlinedTextField(
                    value = tipo,
                    onValueChange = { tipo = it },
                    label = { Text("Tipo / marca (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownCampo("Unidad de medida", UNIDADES, unidad, { unidad = it })
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it },
                    label = { Text("Codigo de barras (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isBlank()) { error = true; return@TextButton }
                val p = (inicial ?: Producto(nombre = nombre)).copy(
                    nombre = nombre.trim(),
                    categoria = categoria,
                    tipo = tipo.trim(),
                    unidadMedida = unidad,
                    codigoBarras = codigo.trim().ifBlank { null }
                )
                onGuardar(p)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun PrecioDialog(
    productoId: String,
    tiendasSugeridas: List<String>,
    onDismiss: () -> Unit,
    onGuardar: (Precio) -> Unit
) {
    var precioTxt by remember { mutableStateOf("") }
    var cantidadTxt by remember { mutableStateOf("1") }
    var tipoPrecio by remember { mutableStateOf("unitario") }
    var tienda by remember { mutableStateOf(tiendasSugeridas.firstOrNull() ?: "") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar precio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = precioTxt,
                    onValueChange = { precioTxt = it; error = false },
                    label = { Text("Precio *") },
                    isError = error,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cantidadTxt,
                    onValueChange = { cantidadTxt = it },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownCampo("Tipo de precio", TIPOS_PRECIO, tipoPrecio, { tipoPrecio = it })
                OutlinedTextField(
                    value = tienda,
                    onValueChange = { tienda = it },
                    label = { Text("Tienda") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val precio = precioTxt.replace(",", ".").toDoubleOrNull()
                if (precio == null || precio <= 0.0) { error = true; return@TextButton }
                val cantidad = cantidadTxt.replace(",", ".").toDoubleOrNull() ?: 1.0
                onGuardar(
                    Precio(
                        productoId = productoId,
                        precio = precio,
                        cantidad = cantidad,
                        tipoPrecio = tipoPrecio,
                        tienda = tienda.trim()
                    )
                )
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
