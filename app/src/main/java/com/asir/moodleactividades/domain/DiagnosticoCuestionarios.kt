package com.asir.moodleactividades.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Lo que contesta Moodle sobre los exámenes tipo test, para cuando uno no aparece en la app.
 * Se enseña al alumno tal cual y puede copiarlo: no lleva el token ni nada de la sesión.
 */
data class DiagnosticoCuestionarios(
    val version: String,
    /** Si el centro deja usar cada función; null si no publica la lista. */
    val funciones: Map<String, Boolean>?,
    /** La última carga guardada de Tareas. */
    val ultimaCarga: Long?,
    val cargaConApi: Boolean,
    val enLaLista: Int,
    /** Por qué Moodle no ha dado la lista de cuestionarios; null si la ha dado. */
    val errorLista: String?,
    val total: Int,
    val revisados: List<Fila>,
    val ahora: Long
) {
    data class Fila(
        val nombre: String,
        val curso: String,
        val cierre: Long?,
        /** El estado de cada intento; null si no se pudo leer. */
        val intentos: List<String>?,
        val nota: String?,
        val notaMaxima: Double?,
        /** Hay intento terminado, pero Moodle no deja ver la nota. */
        val notaOculta: Boolean,
        val error: String?
    )

    fun comoTexto(zona: ZoneId = ZoneId.systemDefault()): String = buildString {
        appendLine("Actividades Moodle $version")

        append("Última carga de Tareas: ")
        if (ultimaCarga == null) {
            appendLine("ninguna")
        } else {
            val minutos = ((ahora - ultimaCarga) / 60).coerceAtLeast(0)
            appendLine(
                "hace $minutos min · exámenes leídos por su API: ${siNo(cargaConApi)} · " +
                    "$enLaLista en la lista"
            )
        }

        append("Funciones que permite el centro: ")
        appendLine(
            funciones?.entries?.joinToString(" · ") { (nombre, permitida) ->
                nombre.removePrefix("mod_quiz_") + " " + siNo(permitida)
            } ?: "el centro no publica la lista"
        )

        if (errorLista != null) {
            appendLine("Moodle no devuelve los cuestionarios: $errorLista")
            return@buildString
        }

        append("Moodle devuelve $total cuestionarios")
        appendLine(if (revisados.size < total) "; estos son los ${revisados.size} más cercanos a hoy:" else ":")
        revisados.forEach { fila ->
            append("• ").append(fila.nombre)
            if (fila.curso.isNotBlank()) append(" · ").append(fila.curso)
            append(" · ").appendLine(fila.cierre?.let { "cierra " + fecha(it, zona) } ?: "sin cierre")
            append("   intentos: ")
            append(
                when {
                    fila.intentos == null -> "no se pudieron leer"
                    fila.intentos.isEmpty() -> "ninguno"
                    else -> fila.intentos.joinToString(", ")
                }
            )
            when {
                fila.nota != null -> append(" · nota: ").append(fila.nota)
                    .append(fila.notaMaxima?.let { " / " + Aprobado.cifra(it) }.orEmpty())
                fila.notaOculta -> append(" · nota: Moodle no deja verla")
            }
            appendLine()
            fila.error?.let { appendLine("   error: $it") }
        }
    }.trimEnd()

    private fun siNo(valor: Boolean) = if (valor) "sí" else "no"

    private fun fecha(segundos: Long, zona: ZoneId) =
        Instant.ofEpochSecond(segundos).atZone(zona).format(FORMATO)

    companion object {
        private val FORMATO: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

        val FUNCIONES = listOf(
            "mod_quiz_get_quizzes_by_courses",
            "mod_quiz_get_user_attempts",
            "mod_quiz_get_user_best_grade"
        )
    }
}
