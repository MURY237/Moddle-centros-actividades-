package com.asir.moodleactividades.data

import android.content.Context
import com.asir.moodleactividades.domain.HorarioClases
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Lo que el alumno corrige del horario de clase. Lo deducido de las faltas no se guarda:
 * se recalcula con cada lectura de Séneca, así que mejora solo.
 */
class AlmacenHorarioClases(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("horario_clases", Context.MODE_PRIVATE)

    fun leer(): HorarioClases {
        val guardado = prefs.getString(CLAVE, null) ?: return HorarioClases()
        return runCatching { json.decodeFromString<HorarioClases>(guardado) }.getOrDefault(HorarioClases())
    }

    fun guardar(horario: HorarioClases) {
        val serializado = runCatching { json.encodeToString(horario) }.getOrNull() ?: return
        prefs.edit().putString(CLAVE, serializado).apply()
    }

    fun borrar() = prefs.edit().remove(CLAVE).apply()

    private companion object {
        const val CLAVE = "propio"
        val json = Json { ignoreUnknownKeys = true }
    }
}
