package com.asir.moodleactividades.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val ESPANOL = Locale.forLanguageTag("es-ES")
private val FORMATO_FECHA = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", ESPANOL)

fun formatearFecha(epochSegundos: Long?, zona: ZoneId = ZoneId.systemDefault()): String {
    if (epochSegundos == null || epochSegundos <= 0) return "Sin fecha límite"
    return Instant.ofEpochSecond(epochSegundos).atZone(zona).format(FORMATO_FECHA)
        .replaceFirstChar { it.uppercase(ESPANOL) }
}

fun textoRelativo(
    epochSegundos: Long?,
    ahora: Long = System.currentTimeMillis() / 1000,
    zona: ZoneId = ZoneId.systemDefault()
): String {
    if (epochSegundos == null || epochSegundos <= 0) return ""

    val hoy = Instant.ofEpochSecond(ahora).atZone(zona).toLocalDate()
    val dia = Instant.ofEpochSecond(epochSegundos).atZone(zona).toLocalDate()
    val dias = ChronoUnit.DAYS.between(hoy, dia)

    return when {
        dias == 0L -> "Vence hoy"
        dias == 1L -> "Vence mañana"
        dias > 1L -> "Faltan $dias días"
        dias == -1L -> "Venció ayer"
        else -> "Venció hace ${-dias} días"
    }
}

/** Para el historial de avisos, donde lo que importa es cuánto hace que llegó. */
fun haceCuanto(epochSegundos: Long, ahora: Long = System.currentTimeMillis() / 1000): String {
    val segundos = (ahora - epochSegundos).coerceAtLeast(0)
    return when {
        segundos < 60 -> "Ahora mismo"
        segundos < 3600 -> "Hace ${segundos / 60} min"
        segundos < 86_400 -> "Hace ${segundos / 3600} h"
        segundos < 172_800 -> "Ayer"
        segundos < 2_592_000 -> "Hace ${segundos / 86_400} días"
        else -> formatearFecha(epochSegundos)
    }
}
