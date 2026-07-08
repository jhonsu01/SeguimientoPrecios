package com.jhonsu.seguimientoprecios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jhonsu.seguimientoprecios.ui.screens.DashboardScreen
import com.jhonsu.seguimientoprecios.ui.screens.GraficosScreen
import com.jhonsu.seguimientoprecios.ui.screens.HistorialScreen
import com.jhonsu.seguimientoprecios.ui.screens.ProductosScreen
import com.jhonsu.seguimientoprecios.ui.theme.SeguimientoPreciosTheme

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

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val tabs = listOf(
        Tab("dashboard", "Inicio", Icons.Filled.Home),
        Tab("productos", "Productos", Icons.Filled.Inventory2),
        Tab("graficos", "Graficos", Icons.Filled.ShowChart)
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
                ProductosScreen(vm, onAbrirProducto = { id -> nav.navigate("historial/$id") })
            }
            composable("graficos") { GraficosScreen(vm) }
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
}
