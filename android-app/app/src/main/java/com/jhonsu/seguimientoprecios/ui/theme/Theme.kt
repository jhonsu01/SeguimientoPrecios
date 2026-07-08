package com.jhonsu.seguimientoprecios.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta de marca (coincide con el icono: teal -> indigo, acento esmeralda).
val Teal = Color(0xFF14B8A6)
val Indigo = Color(0xFF6366F1)
val Emerald = Color(0xFF34D399)
val Rojo = Color(0xFFF87171)
val Ambar = Color(0xFFFBBF24)

val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val SlateText = Color(0xFFE2E8F0)
val SlateSub = Color(0xFF94A3B8)

private val EsquemaOscuro = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF04211C),
    primaryContainer = Color(0xFF115E56),
    onPrimaryContainer = Color(0xFFB8FFF2),
    secondary = Indigo,
    onSecondary = Color.White,
    tertiary = Emerald,
    onTertiary = Color(0xFF04211C),
    background = Slate900,
    onBackground = SlateText,
    surface = Slate800,
    onSurface = SlateText,
    surfaceVariant = Slate700,
    onSurfaceVariant = SlateSub,
    error = Rojo,
    onError = Color(0xFF3A0A0A),
    outline = Slate700
)

@Composable
fun SeguimientoPreciosTheme(content: @Composable () -> Unit) {
    // App con identidad de marca: siempre modo oscuro (Guia.md).
    MaterialTheme(
        colorScheme = EsquemaOscuro,
        typography = Typography(),
        content = content
    )
}
