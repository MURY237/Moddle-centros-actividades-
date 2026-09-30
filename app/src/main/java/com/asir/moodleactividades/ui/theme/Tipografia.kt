package com.asir.moodleactividades.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * La fuente del sistema, con una escala corta y pesos contenidos: semibold para títulos,
 * normal para el texto y medio para etiquetas. Con eso basta para jerarquizar sin gritar.
 */
private val Fuente = FontFamily.SansSerif

private fun estilo(tamano: Float, alto: Float, peso: FontWeight, espaciado: Float = 0f) = TextStyle(
    fontFamily = Fuente,
    fontWeight = peso,
    fontSize = tamano.sp,
    lineHeight = alto.sp,
    letterSpacing = espaciado.sp
)

val TipografiaApp = Typography(
    displaySmall = estilo(32f, 40f, FontWeight.SemiBold, -0.4f),
    headlineMedium = estilo(26f, 32f, FontWeight.SemiBold, -0.3f),
    headlineSmall = estilo(22f, 28f, FontWeight.SemiBold, -0.2f),
    titleLarge = estilo(19f, 26f, FontWeight.SemiBold, -0.1f),
    titleMedium = estilo(16f, 22f, FontWeight.SemiBold),
    titleSmall = estilo(14f, 20f, FontWeight.SemiBold),
    bodyLarge = estilo(16f, 24f, FontWeight.Normal),
    bodyMedium = estilo(14f, 20f, FontWeight.Normal),
    bodySmall = estilo(12.5f, 17f, FontWeight.Normal),
    labelLarge = estilo(14f, 20f, FontWeight.Medium, 0.1f),
    labelMedium = estilo(12f, 16f, FontWeight.Medium, 0.2f),
    labelSmall = estilo(11f, 14f, FontWeight.Medium, 0.3f)
)
