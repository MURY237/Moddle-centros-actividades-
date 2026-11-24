package com.asir.moodleactividades.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * Qué asignatura hay en cada tramo de cada día de la semana (1 = lunes … 5 = viernes),
 * deducido de las propias faltas. Solo sirve para proponer cómo rellenar un día completo.
 */
@Serializable
data class HorarioClases(
    val dias: Map<Int, Map<String, String>> = emptyMap()
) {
    fun de(dia: Int): Map<String, String> = dias[dia].orEmpty().filterValues { it.isNotBlank() }

    companion object {
        /**
         * Si un miércoles de 10:15 a 11:15 se faltó a «Itinerario», esa es la clase de ese
         * tramo. Si en un mismo tramo salen asignaturas distintas, la más repetida, y a
         * igualdad la más reciente.
         */
        fun deducir(faltas: List<Falta>): HorarioClases {
            val porHueco = faltas
                .filter { !it.esDiaCompleto }
                .mapNotNull { falta -> diaDeLaSemana(falta.fecha)?.let { dia -> (dia to falta.tramo.trim()) to falta } }
                .groupBy({ it.first }, { it.second })

            val dias = mutableMapOf<Int, MutableMap<String, String>>()
            porHueco.forEach { (hueco, suyas) ->
                val (dia, tramo) = hueco
                val elegida = suyas
                    .groupBy { it.asignatura }
                    .entries
                    .maxWith(compareBy<Map.Entry<String, List<Falta>>> { it.value.size }
                        .thenBy { entrada -> entrada.value.maxOf { it.ordenable } })
                    .key
                dias.getOrPut(dia) { mutableMapOf() }[tramo] = elegida
            }
            return HorarioClases(dias)
        }

        /** 1 = lunes … 7 = domingo; null si la fecha no es `dd/MM/yyyy`. */
        fun diaDeLaSemana(fecha: String): Int? = runCatching {
            val (dia, mes, anio) = fecha.trim().split('/').map { it.trim().toInt() }
            LocalDate.of(anio, mes, dia).dayOfWeek.value
        }.getOrNull()

        /** Los tramos de las faltas y de lo ya rellenado, por hora de inicio. */
        fun tramos(faltas: List<Falta>, rellenos: DiasCompletos = DiasCompletos()): List<String> =
            (faltas.filter { !it.esDiaCompleto }.map { it.tramo.trim() } + rellenos.dias.values.flatMap { it.keys })
                .filter { it.isNotBlank() }
                .distinct()
                .sortedWith(compareBy<String> { minutosDeInicio(it) ?: Int.MAX_VALUE }.thenBy { it })

        /** «8:15 - 9:15» → 495; null si no empieza por una hora. */
        fun minutosDeInicio(tramo: String): Int? {
            val coincidencia = Regex("""^\s*(\d{1,2})[:.](\d{2})""").find(tramo) ?: return null
            val (horas, minutos) = coincidencia.destructured
            return horas.toInt() * 60 + minutos.toInt()
        }
    }
}
