package com.asir.moodleactividades.ui.grupos

import java.time.format.DateTimeFormatter
import java.util.Locale

/* Formatos de fecha que comparten el chat, el calendario y los exámenes. */

internal val ESPANOL: Locale = Locale.forLanguageTag("es-ES")
internal val HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
internal val DIA_LARGO: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ESPANOL)
internal val DIA_CORTO: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", ESPANOL)
internal val MES_CORTO: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", ESPANOL)

/** «Lunes 3 de marzo»: los formatos de Java dan el día en minúscula. */
internal fun String.conMayuscula(): String = replaceFirstChar { it.uppercase(ESPANOL) }
