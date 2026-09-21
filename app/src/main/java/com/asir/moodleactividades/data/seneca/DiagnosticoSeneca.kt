package com.asir.moodleactividades.data.seneca

import com.asir.moodleactividades.data.SesionSeneca

/**
 * Qué ha visto la app en su último intento de leer las faltas. Existe porque Séneca no se
 * puede probar desde fuera: sin esto, cada arreglo sería una suposición.
 *
 * No recoge ningún dato del alumno ni ningún valor de sesión: solo nombres de cookies,
 * cuántas había y la ruta de la página con su parte variable recortada.
 */
data class DiagnosticoSeneca(
    val cookiesGuardadas: List<String> = emptyList(),
    val cookiesVivas: Int = 0,
    val urlUltimaPagina: String = "",
    val tablaEncontrada: Boolean = false,
    val pidioAcceso: Boolean = false,
    val paginasVistas: Int = 0,
    /** Qué fue lo último que la app consiguió pulsar del menú de Séneca. */
    val ultimoPaso: String = ""
) {
    fun comoTexto(): String = buildString {
        appendLine("Cookies guardadas: " + if (cookiesGuardadas.isEmpty()) "ninguna" else
            "${cookiesGuardadas.size} (${cookiesGuardadas.joinToString(", ")})")
        appendLine("Cookies vivas en el navegador: $cookiesVivas")
        appendLine("Páginas cargadas en el intento: $paginasVistas")
        appendLine("Última página: ${urlUltimaPagina.ifBlank { "ninguna" }}")
        appendLine("Último paso del menú: ${ultimoPaso.ifBlank { "ninguno" }}")
        appendLine("Tabla encontrada: " + if (tablaEncontrada) "sí" else "no")
        appendLine("Pidió identificarse: " + if (pidioAcceso) "sí" else "no")
    }.trim()

    companion object {
        /**
         * La ruta de Séneca lleva el identificador de sesión dentro, así que se corta: para
         * saber en qué parte del sitio está basta con el principio.
         */
        fun rutaSegura(url: String?): String {
            val limpia = url.orEmpty().substringBefore('?').substringBefore('#')
            if (limpia.isBlank()) return ""
            val sinEsquema = limpia.removePrefix("https://").removePrefix("http://")
            val trozos = sinEsquema.split('/')
            val host = trozos.firstOrNull().orEmpty()
            // Se conservan dos tramos de ruta; lo que venga después es el identificador.
            val ruta = trozos.drop(1).filter { it.isNotBlank() }
            return buildString {
                append(host)
                ruta.take(2).forEach { append('/').append(it) }
                if (ruta.size > 2) append("/…")
            }
        }

        fun de(sesion: SesionSeneca) = DiagnosticoSeneca(
            cookiesGuardadas = sesion.nombres(),
            cookiesVivas = sesion.vivasAhora()
        )
    }
}
