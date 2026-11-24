package com.asir.moodleactividades.domain

import kotlinx.serialization.Serializable

/**
 * Lo que el alumno ha rellenado para cada falta de «Día completo»: qué asignatura había en
 * cada tramo de esa fecha. Séneca no lo dice, así que un día completo no cuenta en ninguna
 * asignatura hasta que se rellena.
 */
@Serializable
data class DiasCompletos(
    /** Fecha `dd/MM/yyyy` → tramo → asignatura. Una asignatura en blanco es «sin clase». */
    val dias: Map<String, Map<String, String>> = emptyMap()
) {
    /** Las clases puestas para esa fecha; null si aún no se ha rellenado. */
    fun de(fecha: String): Map<String, String>? = dias[fecha.trim()]?.filterValues { it.isNotBlank() }

    fun relleno(fecha: String): Boolean = fecha.trim() in dias

    fun con(fecha: String, clases: Map<String, String>): DiasCompletos =
        copy(dias = dias + (fecha.trim() to clases))

    fun sin(fecha: String): DiasCompletos = copy(dias = dias - fecha.trim())

    companion object {
        /**
         * Lo que se propone al rellenar un día, para no empezar de cero: el último día igual
         * de la semana que ya se rellenó o, si no hay, lo que se deduce de las faltas. Es solo
         * una propuesta: no cuenta hasta que el alumno la guarda.
         */
        fun sugerencia(fecha: String, faltas: List<Falta>, rellenos: DiasCompletos): Map<String, String> {
            val dia = HorarioClases.diaDeLaSemana(fecha) ?: return emptyMap()
            val anterior = rellenos.dias.entries
                .filter { (otra, _) -> otra != fecha.trim() && HorarioClases.diaDeLaSemana(otra) == dia }
                .maxByOrNull { fechaOrdenable(it.key) }
                ?.value
                ?.filterValues { it.isNotBlank() }
            if (!anterior.isNullOrEmpty()) return anterior
            return HorarioClases.deducir(faltas).de(dia)
        }
    }
}
