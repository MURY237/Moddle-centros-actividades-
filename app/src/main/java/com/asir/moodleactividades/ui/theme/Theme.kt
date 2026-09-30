package com.asir.moodleactividades.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * Una marca, un acento. El azul se reserva para lo que se puede tocar y para lo que está
 * seleccionado; el resto de la interfaz va en grises fríos, y los estados (entregado,
 * pendiente, vencido) tienen su propio juego de tonos, iguales en todas las pantallas.
 */

private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF1F5FD1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE7FB),
    onPrimaryContainer = Color(0xFF0A2A6B),
    secondary = Color(0xFF50607A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E8F1),
    onSecondaryContainer = Color(0xFF1B2536),
    tertiary = Color(0xFF00786F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD2F1EC),
    onTertiaryContainer = Color(0xFF00201D),
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF14171C),
    surface = Color.White,
    onSurface = Color(0xFF14171C),
    surfaceVariant = Color(0xFFE9ECF1),
    onSurfaceVariant = Color(0xFF5A6270),
    surfaceTint = Color(0xFF1F5FD1),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFBFC),
    surfaceContainer = Color(0xFFF1F3F6),
    surfaceContainerHigh = Color(0xFFEBEEF2),
    surfaceContainerHighest = Color(0xFFE4E7EC),
    outline = Color(0xFFC4C9D2),
    outlineVariant = Color(0xFFE1E4EA),
    error = Color(0xFFB42318),
    onError = Color.White,
    errorContainer = Color(0xFFFDE8E7),
    onErrorContainer = Color(0xFF55120C)
)

private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFFA8C3FF),
    onPrimary = Color(0xFF0A2A6B),
    primaryContainer = Color(0xFF1B3F8F),
    onPrimaryContainer = Color(0xFFDDE7FB),
    secondary = Color(0xFFB8C4D8),
    onSecondary = Color(0xFF223047),
    secondaryContainer = Color(0xFF323C4D),
    onSecondaryContainer = Color(0xFFE3E8F1),
    tertiary = Color(0xFF7ED8CC),
    onTertiary = Color(0xFF003733),
    tertiaryContainer = Color(0xFF00504A),
    onTertiaryContainer = Color(0xFFD2F1EC),
    background = Color(0xFF0E1014),
    onBackground = Color(0xFFE5E8ED),
    surface = Color(0xFF15181D),
    onSurface = Color(0xFFE5E8ED),
    surfaceVariant = Color(0xFF2A2F37),
    onSurfaceVariant = Color(0xFFA0A8B5),
    surfaceTint = Color(0xFFA8C3FF),
    surfaceContainerLowest = Color(0xFF0B0D10),
    surfaceContainerLow = Color(0xFF13161B),
    surfaceContainer = Color(0xFF1A1E24),
    surfaceContainerHigh = Color(0xFF22262D),
    surfaceContainerHighest = Color(0xFF2A2F37),
    outline = Color(0xFF4A515C),
    outlineVariant = Color(0xFF2C313A),
    error = Color(0xFFFF8F86),
    onError = Color(0xFF55120C),
    errorContainer = Color(0xFF3F1A18),
    onErrorContainer = Color(0xFFFDE8E7)
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Tono de un estado. Cada pantalla pinta «entregado», «pendiente» o «vencido» con estos, y
 * no con colores sueltos: así una misma cosa se ve igual en todas partes.
 */
enum class Tono { NEUTRO, INFO, EXITO, AVISO, PELIGRO }

/** El color del texto o icono y el del fondo suave sobre el que va. */
@Immutable
data class ColoresTono(val contenido: Color, val contenedor: Color)

private val TonosClaros = mapOf(
    Tono.NEUTRO to ColoresTono(Color(0xFF4A5261), Color(0xFFEEF0F3)),
    Tono.INFO to ColoresTono(Color(0xFF1F5FD1), Color(0xFFE6EDFB)),
    Tono.EXITO to ColoresTono(Color(0xFF1B7F4E), Color(0xFFE3F4EA)),
    Tono.AVISO to ColoresTono(Color(0xFF9A5B00), Color(0xFFFDF1DC)),
    Tono.PELIGRO to ColoresTono(Color(0xFFB42318), Color(0xFFFDE8E7))
)

private val TonosOscuros = mapOf(
    Tono.NEUTRO to ColoresTono(Color(0xFFB7BECA), Color(0xFF262B33)),
    Tono.INFO to ColoresTono(Color(0xFFA8C3FF), Color(0xFF1C2A4D)),
    Tono.EXITO to ColoresTono(Color(0xFF6FD3A0), Color(0xFF133427)),
    Tono.AVISO to ColoresTono(Color(0xFFF2B45A), Color(0xFF3A2A10)),
    Tono.PELIGRO to ColoresTono(Color(0xFFFF8F86), Color(0xFF3F1A18))
)

/**
 * Si el tema en uso es el oscuro. Va en el tema y no se pregunta al sistema cada vez: así
 * una vista previa o una captura en oscuro se pinta entera en oscuro, estados incluidos.
 */
val LocalTemaOscuro = staticCompositionLocalOf { false }

@Composable
fun Tono.colores(): ColoresTono =
    (if (LocalTemaOscuro.current) TonosOscuros else TonosClaros).getValue(this)

@Composable
fun MoodleActividadesTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTemaOscuro provides oscuro) {
        MaterialTheme(
            colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
            typography = TipografiaApp,
            shapes = Formas,
            content = content
        )
    }
}
