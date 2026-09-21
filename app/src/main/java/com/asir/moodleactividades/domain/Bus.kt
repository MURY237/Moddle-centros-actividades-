package com.asir.moodleactividades.domain

import kotlinx.serialization.Serializable

/**
 * Un trayecto de autobús con sus horas de salida: «Pueblo → Instituto» por la mañana y la
 * vuelta por la tarde suelen ser dos líneas distintas, con horarios y días distintos.
 *
 * Las horas se guardan como minutos desde medianoche en lugar de como texto: así se ordenan
 * y se comparan sin volver a interpretar nada, y un «7:5» mal escrito no llega hasta aquí.
 */
@Serializable
data class LineaBus(
    val id: Long,
    val nombre: String,
    val horas: List<Int> = emptyList(),
    /** Días con servicio, en la numeración ISO: 1 lunes … 7 domingo. */
    val dias: Set<Int> = LABORABLES
) {
    val horasOrdenadas: List<Int> get() = horas.distinct().sorted()

    companion object {
        val LABORABLES = setOf(1, 2, 3, 4, 5)
        val NOMBRES_DIAS = listOf("L", "M", "X", "J", "V", "S", "D")
    }
}

/** La salida que toca y cuánto falta para ella. */
data class ProximaSalida(
    val linea: LineaBus,
    val minutoDelDia: Int,
    /** 0 si sale hoy, 1 mañana, y así hasta la semana que viene. */
    val diasDeEspera: Int
) {
    fun minutosQueFaltan(minutoAhora: Int): Int =
        diasDeEspera * MINUTOS_POR_DIA + minutoDelDia - minutoAhora
}

const val MINUTOS_POR_DIA = 24 * 60

object HorariosBus {

    /**
     * Acepta lo que se escribe de verdad en un móvil: «7:45», «07.45» o «0745». Devuelve null
     * si no es una hora válida, para no guardar basura que luego no se pueda ordenar.
     */
    fun parsearHora(texto: String): Int? {
        val limpio = texto.trim().replace('.', ':').replace('h', ':', ignoreCase = true)
        val (horas, minutos) = when {
            ':' in limpio -> {
                val trozos = limpio.split(':')
                if (trozos.size != 2) return null
                (trozos[0].toIntOrNull() ?: return null) to (trozos[1].toIntOrNull() ?: return null)
            }
            // «0745» sin separador: los dos últimos dígitos son los minutos.
            limpio.length == 4 && limpio.all { it.isDigit() } ->
                limpio.take(2).toInt() to limpio.takeLast(2).toInt()

            else -> return null
        }
        if (horas !in 0..23 || minutos !in 0..59) return null
        return horas * 60 + minutos
    }

    fun formatearHora(minutoDelDia: Int): String {
        val normalizado = ((minutoDelDia % MINUTOS_POR_DIA) + MINUTOS_POR_DIA) % MINUTOS_POR_DIA
        return "%02d:%02d".format(normalizado / 60, normalizado % 60)
    }

    /**
     * La próxima salida de una línea. Se miran hasta ocho días por delante: con eso se cubre
     * el caso de la línea que solo circula un día a la semana y cuya hora de hoy ya ha pasado.
     */
    fun proxima(linea: LineaBus, diaHoy: Int, minutoAhora: Int): ProximaSalida? {
        val horas = linea.horasOrdenadas
        if (horas.isEmpty() || linea.dias.isEmpty()) return null

        for (espera in 0..7) {
            val dia = ((diaHoy - 1 + espera) % 7) + 1
            if (dia !in linea.dias) continue
            // Solo hoy hay que descartar las que ya han salido.
            val candidata = horas.firstOrNull { espera > 0 || it > minutoAhora } ?: continue
            return ProximaSalida(linea, candidata, espera)
        }
        return null
    }

    /** Las salidas de hoy, para pintarlas distinguiendo las que ya han pasado. */
    fun salidasDeHoy(linea: LineaBus, diaHoy: Int): List<Int> =
        if (diaHoy in linea.dias) linea.horasOrdenadas else emptyList()

    /** «L, M, X», o «Todos los días» cuando no hay nada que distinguir. */
    fun etiquetaDias(dias: Set<Int>): String = when {
        dias.isEmpty() -> "Sin días"
        dias == (1..7).toSet() -> "Todos los días"
        dias == LineaBus.LABORABLES -> "De lunes a viernes"
        else -> dias.sorted().joinToString(", ") { LineaBus.NOMBRES_DIAS[it - 1] }
    }
}
