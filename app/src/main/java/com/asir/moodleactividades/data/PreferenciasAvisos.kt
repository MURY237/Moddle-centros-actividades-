package com.asir.moodleactividades.data

import android.content.Context

data class AjustesAvisos(
    val avisarEntregas: Boolean = true,
    val antelacionHoras: Int = 48,
    val comprobacionesDiarias: Int = 4,
    val horaPreferida: Int = 8,
    val avisarNuevas: Boolean = true
) {
    val horasEntreComprobaciones: Long
        get() = (24L / comprobacionesDiarias.coerceIn(1, 24)).coerceAtLeast(1L)

    val antelacionSegundos: Long get() = antelacionHoras * 3600L

    companion object {
        val ANTELACIONES = listOf(6, 12, 24, 48, 72, 168)
        val FRECUENCIAS = listOf(1, 2, 4, 6)

        fun etiquetaAntelacion(horas: Int): String = when {
            horas < 24 -> "$horas horas antes"
            horas == 24 -> "1 día antes"
            horas % 168 == 0 -> "${horas / 168} semana antes"
            else -> "${horas / 24} días antes"
        }

        fun etiquetaFrecuencia(veces: Int): String =
            if (veces == 1) "1 vez al día" else "$veces veces al día"

        fun etiquetaHora(hora: Int): String = "%02d:00".format(hora)
    }
}

class PreferenciasAvisos(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("avisos", Context.MODE_PRIVATE)

    fun leer(): AjustesAvisos {
        val porDefecto = AjustesAvisos()
        return AjustesAvisos(
            avisarEntregas = prefs.getBoolean(ENTREGAS, porDefecto.avisarEntregas),
            antelacionHoras = prefs.getInt(ANTELACION, porDefecto.antelacionHoras),
            comprobacionesDiarias = prefs.getInt(FRECUENCIA, porDefecto.comprobacionesDiarias),
            horaPreferida = prefs.getInt(HORA, porDefecto.horaPreferida),
            avisarNuevas = prefs.getBoolean(NUEVAS, porDefecto.avisarNuevas)
        )
    }

    fun guardar(ajustes: AjustesAvisos) {
        prefs.edit()
            .putBoolean(ENTREGAS, ajustes.avisarEntregas)
            .putInt(ANTELACION, ajustes.antelacionHoras)
            .putInt(FRECUENCIA, ajustes.comprobacionesDiarias)
            .putInt(HORA, ajustes.horaPreferida)
            .putBoolean(NUEVAS, ajustes.avisarNuevas)
            .apply()
    }

    private companion object {
        const val ENTREGAS = "avisar_entregas"
        const val ANTELACION = "antelacion_horas"
        const val FRECUENCIA = "comprobaciones_diarias"
        const val HORA = "hora_preferida"
        const val NUEVAS = "avisar_nuevas"
    }
}
