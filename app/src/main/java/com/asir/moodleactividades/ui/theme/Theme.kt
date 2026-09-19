package com.asir.moodleactividades.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val Indigo = Color(0xFF5B4FE9)
private val IndigoClaro = Color(0xFF8B7DFF)
private val Violeta = Color(0xFF9B5DE5)
private val Turquesa = Color(0xFF00B8A9)

val VerdeEntregada = Color(0xFF00A67E)
val AmbarPendiente = Color(0xFFE08700)
val RojoNoEntregada = Color(0xFFE5484D)

val VerdeEntregadaFondo = Color(0xFFD7F5EC)
val AmbarPendienteFondo = Color(0xFFFFEFD6)
val RojoNoEntregadaFondo = Color(0xFFFFE4E4)

val VerdeEntregadaOscuro = Color(0xFF123A31)
val AmbarPendienteOscuro = Color(0xFF3D2D10)
val RojoNoEntregadaOscuro = Color(0xFF3D1E1F)

/** Degradado de la cabecera: es lo que da carácter a la pantalla principal. */
val DegradadoCabecera = Brush.linearGradient(listOf(Indigo, Violeta))

private val EsquemaClaro = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E2FF),
    onPrimaryContainer = Color(0xFF1B1259),
    secondary = Turquesa,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2F5F1),
    onSecondaryContainer = Color(0xFF00382F),
    background = Color(0xFFF7F6FC),
    onBackground = Color(0xFF16151D),
    surface = Color.White,
    onSurface = Color(0xFF16151D),
    surfaceVariant = Color(0xFFEDEBF6),
    onSurfaceVariant = Color(0xFF605D75),
    outline = Color(0xFFCFCBE0),
    error = RojoNoEntregada,
    errorContainer = RojoNoEntregadaFondo,
    onErrorContainer = Color(0xFF6E1114)
)

private val EsquemaOscuro = darkColorScheme(
    primary = IndigoClaro,
    onPrimary = Color(0xFF17123F),
    primaryContainer = Color(0xFF2E2666),
    onPrimaryContainer = Color(0xFFE2DEFF),
    secondary = Color(0xFF4FD8C9),
    onSecondary = Color(0xFF00352D),
    secondaryContainer = Color(0xFF005346),
    onSecondaryContainer = Color(0xFFB8F2EA),
    background = Color(0xFF111019),
    onBackground = Color(0xFFE7E5F0),
    surface = Color(0xFF1B1A25),
    onSurface = Color(0xFFE7E5F0),
    surfaceVariant = Color(0xFF2A2837),
    onSurfaceVariant = Color(0xFFA9A5BD),
    outline = Color(0xFF474459),
    error = Color(0xFFFF8A8F),
    errorContainer = RojoNoEntregadaOscuro,
    onErrorContainer = Color(0xFFFFD6D7)
)

private val FormasRedondeadas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MoodleActividadesTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        typography = TipografiaApp,
        shapes = FormasRedondeadas,
        content = content
    )
}

/** Los fondos de estado necesitan variante propia: los claros se vuelven ilegibles en oscuro. */
@Composable
fun fondoDeEstado(claro: Color, oscuro: Color): Color =
    if (isSystemInDarkTheme()) oscuro else claro
