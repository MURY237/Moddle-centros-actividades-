package com.asir.moodleactividades.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Fuente = FontFamily.SansSerif

val TipografiaApp = Typography(
    displaySmall = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-1).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Fuente,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp
    )
)
