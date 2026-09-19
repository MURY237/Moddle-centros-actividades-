package com.asir.moodleactividades.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NaranjaMoodle = Color(0xFFF98012)
private val NaranjaOscuro = Color(0xFFBF5B00)

val VerdeEntregada = Color(0xFF2E7D32)
val AmbarPendiente = Color(0xFFB26A00)
val RojoNoEntregada = Color(0xFFC62828)

val VerdeEntregadaFondo = Color(0xFFD9F0DC)
val AmbarPendienteFondo = Color(0xFFFFEBCC)
val RojoNoEntregadaFondo = Color(0xFFFBDCDC)

private val EsquemaClaro = lightColorScheme(
    primary = NaranjaOscuro,
    secondary = Color(0xFF41618A),
    background = Color(0xFFF7F7F8),
    surface = Color(0xFFFFFFFF)
)

private val EsquemaOscuro = darkColorScheme(
    primary = NaranjaMoodle,
    secondary = Color(0xFF9CC3F5),
    background = Color(0xFF121316),
    surface = Color(0xFF1C1E22)
)

@Composable
fun MoodleActividadesTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        content = content
    )
}
