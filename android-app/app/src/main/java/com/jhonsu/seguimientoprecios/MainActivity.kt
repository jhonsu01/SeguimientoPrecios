package com.jhonsu.seguimientoprecios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jhonsu.seguimientoprecios.net.OcrResultado
import com.jhonsu.seguimientoprecios.ui.components.OcrResultDialog
import com.jhonsu.seguimientoprecios.ui.screens.AjustesScreen
import com.jhonsu.seguimientoprecios.ui.screens.AlacenaScreen
import com.jhonsu.seguimientoprecios.ui.screens.DashboardScreen
import com.jhonsu.seguimientoprecios.ui.screens.GraficosScreen
import com.jhonsu.seguimientoprecios.ui.screens.HistorialScreen
import com.jhonsu.seguimientoprecios.ui.screens.LockScreen
import com.jhonsu.seguimientoprecios.ui.screens.ProductosScreen
import com.jhonsu.seguimientoprecios.ui.theme.SeguimientoPreciosTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SeguimientoPreciosTheme {
                AppRoot()
            }
        }
    }
}

private data class Tab(val ruta: String, val label: String, val icon: ImageVector)

private fun nombreBackup(): String =
    "SeguimientoPrecios-backup-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".zip"

private val MIMES_OCR = arrayOf(
    "image/*", "application/pdf", "application/zip",
    "application/x-zip-compressed", "application/octet-stream"
)

private val MIMES_ZIP = arrayOf(
    "application/zip", "application/x-zip-compressed", "application/octet-stream"
)

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    if (!vm.desbloqueado) {
        LockScreen(
            titulo = "Ingresa tu PIN",
            onIntento = { pin -> if (vm.verificarPin(pin)) { vm.desbloqueado = true; true } else false }
        )
        return
    }

    val nav = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var ocrCargando by remember { mutableStateOf(false) }
    var ocrResultado by remember { mutableStateOf<OcrResultado?>(null) }
    var ocrError by remember { mutableStateOf<String?>(null) }

    val pickOcr = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            ocrCargando = true
            scope.launch {
                try {
                    ocrResultado = vm.correrOcr(uri)
                } catch (e: Exception) {
                    ocrError = e.message ?: "Error al procesar la factura"
                } finally {
                    ocrCargando = false
                }
            }
        }
    }

    val exportar = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) context.contentResolver.openOutputStream(uri)?.let { vm.exportar(it) }
    }

    val importar = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) context.contentResolver.openInputStream(uri)?.let { vm.importar(it) }
    }

    val tabs = listOf(
        Tab("dashboard", "Inicio", Icons.Filled.Home),
        Tab("productos", "Productos", Icons.Filled.Inventory2),
        Tab("alacena", "Alacena", Icons.Filled.Kitchen),
        Tab("graficos", "Graficos", Icons.Filled.ShowChart),
        Tab("ajustes", "Ajustes", Icons.Filled.Settings)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val rutaActual = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = rutaActual == t.ruta,
                        onClick = {
                            nav.navigate(t.ruta) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding)
        ) {
            composable("dashboard") {
                DashboardScreen(vm, onAbrirProducto = { id -> nav.navigate("historial/$id") })
            }
            composable("productos") {
                ProductosScreen(
                    vm = vm,
                    onAbrirProducto = { id -> nav.navigate("historial/$id") },
                    onEscanearFactura = { pickOcr.launch(MIMES_OCR) }
                )
            }
            composable("alacena") {
                AlacenaScreen(vm, onAbrirProducto = { id -> nav.navigate("historial/$id") })
            }
            composable("graficos") { GraficosScreen(vm) }
            composable("ajustes") {
                AjustesScreen(
                    vm = vm,
                    onExportar = { exportar.launch(nombreBackup()) },
                    onImportar = { importar.launch(MIMES_ZIP) }
                )
            }
            composable(
                "historial/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                HistorialScreen(
                    vm = vm,
                    productoId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }

    if (ocrCargando) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
    ocrResultado?.let { res ->
        OcrResultDialog(
            resultado = res,
            onDismiss = { ocrResultado = null },
            onConfirmar = { vm.agregarDesdeOcr(res); ocrResultado = null }
        )
    }
    ocrError?.let { msg ->
        AlertDialog(
            onDismissRequest = { ocrError = null },
            confirmButton = { TextButton(onClick = { ocrError = null }) { Text("Entendido") } },
            title = { Text("OCR de factura") },
            text = { Text(msg) }
        )
    }
    vm.mensaje?.let { m ->
        AlertDialog(
            onDismissRequest = { vm.mensaje = null },
            confirmButton = { TextButton(onClick = { vm.mensaje = null }) { Text("Ok") } },
            title = { Text("Copia de seguridad") },
            text = { Text(m) }
        )
    }
}
