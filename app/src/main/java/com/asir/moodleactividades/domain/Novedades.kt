package com.asir.moodleactividades.domain

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Qué hay de nuevo en un grupo desde la última vez que se miró, para avisar con el móvil en
 * el bolsillo. Lo propio no se avisa: nadie quiere una notificación de su propio mensaje.
 */
object NovedadesGrupo {

    private val FECHA: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-ES"))

    /**
     * Sin [ultimoVisto] es la primera vez que se mira el grupo: lo que ya hubiera no es
     * nuevo, y avisar de todo el historial de golpe sería ruido.
     */
    fun mensajes(mensajes: List<Mensaje>, ultimoVisto: Long?, yo: String): List<Mensaje> =
        if (ultimoVisto == null) emptyList()
        else mensajes.filter { it.id > ultimoVisto && it.autor != yo }.sortedBy { it.id }

    fun examenes(examenes: List<Examen>, ultimoVisto: Long?, yo: String): List<Examen> =
        if (ultimoVisto == null) emptyList()
        else examenes.filter { it.id > ultimoVisto && it.autor != yo }.sortedBy { it.id }

    /** Lo último visto tras esta consulta; con el grupo vacío, cero, para avisar del primero. */
    fun ultimo(anterior: Long?, ids: List<Long>): Long = maxOf(anterior ?: 0L, ids.maxOrNull() ?: 0L)

    /** «Martes 4 de noviembre a las 09:15». */
    fun cuando(examen: Examen): String {
        val dia = examen.dia?.format(FECHA)?.replaceFirstChar { it.uppercase() } ?: examen.fecha
        val hora = examen.horaLocal?.let { " a las %02d:%02d".format(it.hour, it.minute) }.orEmpty()
        return dia + hora
    }
}

/** Cuándo cae la siguiente comprobación de fondo. */
object Programacion {

    /**
     * Minutos hasta la próxima hora alineada con [hora] cada [periodoHoras]: con la primera a
     * las 8 y cada 6 horas, las 8, las 14, las 20 y las 2. Antes la primera esperaba siempre
     * a las 8 del día siguiente, y hasta entonces no llegaba ningún aviso.
     */
    fun minutosHastaLaProxima(ahora: LocalDateTime, hora: Int, periodoHoras: Long): Long {
        val periodo = periodoHoras.coerceIn(1, 24)
        var objetivo = ahora.toLocalDate().atTime(hora.coerceIn(0, 23), 0)
        while (objetivo.isAfter(ahora)) objetivo = objetivo.minusHours(periodo)
        while (!objetivo.isAfter(ahora)) objetivo = objetivo.plusHours(periodo)
        return Duration.between(ahora, objetivo).toMinutes()
    }
}
