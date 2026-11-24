package com.asir.moodleactividades.data

import android.content.Context
import com.asir.moodleactividades.domain.DiasCompletos
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Lo que el alumno ha rellenado para cada día completo. Solo en el móvil. */
class AlmacenDiasCompletos(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("dias_completos", Context.MODE_PRIVATE)

    fun leer(): DiasCompletos {
        val guardado = prefs.getString(CLAVE, null) ?: return DiasCompletos()
        return runCatching { json.decodeFromString<DiasCompletos>(guardado) }.getOrDefault(DiasCompletos())
    }

    fun guardar(rellenos: DiasCompletos) {
        val serializado = runCatching { json.encodeToString(rellenos) }.getOrNull() ?: return
        prefs.edit().putString(CLAVE, serializado).apply()
    }

    private companion object {
        const val CLAVE = "rellenos"
        val json = Json { ignoreUnknownKeys = true }
    }
}
