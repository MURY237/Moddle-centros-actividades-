package com.asir.moodleactividades.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * Qué asignatura hay en cada tramo de cada día de la semana (1 = lunes … 5 = viernes).
 * Séneca apunta una falta de «Día completo» sin asignatura; sin saber qué clases había
 * ese día no se puede sumar a ninguna.
 *
 * Una asignatura en blanco significa «ese tramo no hay clase»: así el alumno puede quitar
 * un tramo que se dedujo mal.
 */
@Serializable
data class HorarioClases(
    val dias: Map<Int, Map<String, String>> = emptyMap()
) {
    /** Las clases de ese día, sin los tramos marcados como «sin clase». */
    fun de(dia: Int): Map<String, String> = dias[dia].orEmpty().filterValues { it.isNotBlank() }

    fun con(dia: Int, tramo: String, asignatura: String): HorarioClases =
        copy(dias = dias + (dia to (dias[dia].orEmpty() + (tramo to asignatura))))

    /** Lo de [encima] manda; lo que no toca se queda como estaba. */
    fun sobre(encima: HorarioClases): HorarioClases {
        val todos = dias.keys + encima.dias.keys
        return HorarioClases(todos.associateWith { dia -> dias[dia].orEmpty() + encima.dias[dia].orEmpty() })
    }

    val vacio: Boolean get() = dias.values.all { tramos -> tramos.values.all { it.isBlank() } }

    companion object {
        val DIAS_LECTIVOS = 1..5

        /**
         * Lo que se puede saber del horario por las propias faltas: si un miércoles de 10:15
         * a 11:15 se faltó a «Itinerario», esa es la clase de ese tramo. Si en un mismo
         * tramo salen asignaturas distintas, la más repetida, y a igualdad la más reciente.
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

        /** Los tramos que aparecen en las faltas, por hora de inicio. */
        fun tramos(faltas: List<Falta>, horario: HorarioClases = HorarioClases()): List<String> =
            (faltas.filter { !it.esDiaCompleto }.map { it.tramo.trim() } + horario.dias.values.flatMap { it.keys })
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
