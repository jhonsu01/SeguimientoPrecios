package com.jhonsu.seguimientoprecios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.app.Activity
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jhonsu.seguimientoprecios.AppViewModel
import com.jhonsu.seguimientoprecios.ui.components.DropdownCampo
import com.jhonsu.seguimientoprecios.ui.components.PinDialog
import com.jhonsu.seguimientoprecios.ui.theme.Emerald
import com.jhonsu.seguimientoprecios.util.Moneda

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(vm: AppViewModel, onExportar: () -> Unit, onImportar: () -> Unit) {
    var apiKey by remember { mutableStateOf(vm.getOpenAiKey()) }
    var keyGuardada by remember { mutableStateOf(false) }
    var tienePin by remember { mutableStateOf(vm.tienePin) }
    var mostrarPin by remember { mutableStateOf(false) }
    var monedaCode by remember { mutableStateOf(vm.getMoneda()) }

    val ctx = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
        } catch (e: Exception) { "" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Seccion("OCR de facturas (OpenAI)") {
                Text(
                    "Pega tu API key de OpenAI. Se guarda solo en este dispositivo y se usa para leer facturas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; keyGuardada = false },
                    label = { Text("API key (sk-...)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Button(
                    onClick = { vm.setOpenAiKey(apiKey); keyGuardada = true },
                    modifier = Modifier.padding(top = 8.dp)
                ) { Text("Guardar key") }
                if (keyGuardada) {
                    Text("Guardada ✓", color = Emerald, style = MaterialTheme.typography.labelMedium)
                }
            }

            Seccion("Moneda") {
                Text(
                    "Selecciona la moneda para mostrar los precios (por defecto COP).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DropdownCampo(
                    etiqueta = "Moneda",
                    opciones = Moneda.LISTA.map { it.etiqueta },
                    seleccion = Moneda.porCodigo(monedaCode).etiqueta,
                    onSelect = { etiqueta ->
                        val def = Moneda.LISTA.first { it.etiqueta == etiqueta }
                        monedaCode = def.code
                        vm.setMoneda(def.code)
                        (ctx as? Activity)?.recreate()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            Seccion("Seguridad (PIN)") {
                if (tienePin) {
                    Text(
                        "El acceso a la app esta protegido con PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { vm.quitarPin(); tienePin = false },
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Quitar PIN") }
                } else {
                    Text(
                        "Protege el acceso con un PIN (hash SHA-256).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { mostrarPin = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Definir PIN") }
                }
            }

            Seccion("Copia de seguridad") {
                Text(
                    "Exporta o restaura toda tu base de datos (ZIP con SQLite + JSON).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Button(onClick = onExportar) { Text("Exportar ZIP") }
                    OutlinedButton(onClick = onImportar) { Text("Importar ZIP") }
                }
                Text(
                    "Al importar, la app se reiniciara para cargar los datos.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Seccion("Acerca de") {
                Text("Seguimiento de Precios", fontWeight = FontWeight.SemiBold)
                Text(
                    "Version $version · modo oscuro · offline-first",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Button(
                        onClick = { uriHandler.openUri("https://ko-fi.com/V7V81LV7GX") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29ABE0))
                    ) { Text("☕ Apoyame en Ko-fi", color = Color.White) }
                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://github.com/jhonsu01/SeguimientoPrecios") }
                    ) { Text("Repositorio") }
                }
            }
        }
    }

    if (mostrarPin) {
        PinDialog(
            onDismiss = { mostrarPin = false },
            onDefinir = { pin -> vm.definirPin(pin); tienePin = true; mostrarPin = false }
        )
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Column(Modifier.padding(top = 4.dp)) { contenido() }
        }
    }
}
