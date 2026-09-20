package com.asir.moodleactividades.domain

/**
 * Qué ofrece el centro en materia de asistencia. En los Moodle de Andalucía las faltas suelen
 * vivir en Séneca, no en Moodle, así que antes de construir nada hay que comprobar si el
 * centro publica el módulo de asistencia por los servicios web.
 */
data class SondeoAsistencia(
    val sitio: String,
    val version: String,
    val totalFunciones: Int,
    val funcionesAsistencia: List<String>
) {
    val disponible: Boolean get() = funcionesAsistencia.isNotEmpty()

    /** Texto plano para poder pegarlo en una consulta. No lleva token ni datos personales. */
    fun comoTexto(): String = buildString {
        appendLine("Sitio: $sitio")
        appendLine("Moodle: ${version.ifBlank { "desconocido" }}")
        appendLine("Funciones disponibles para la app: $totalFunciones")
        if (funcionesAsistencia.isEmpty()) {
            appendLine("Funciones de asistencia: ninguna")
        } else {
            appendLine("Funciones de asistencia:")
            funcionesAsistencia.forEach { appendLine("  - $it") }
        }
    }.trim()

    companion object {
        /**
         * El módulo se llama `attendance`, pero algunos centros instalan derivados con otro
         * nombre, así que se busca por varias pistas en lugar de por un nombre exacto.
         */
        private val PISTAS = listOf("attendance", "asisten", "absence", "falta")

        fun esDeAsistencia(nombreFuncion: String): Boolean {
            val nombre = nombreFuncion.lowercase()
            return PISTAS.any { it in nombre }
        }
    }
}
