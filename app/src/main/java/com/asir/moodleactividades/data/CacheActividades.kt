package com.asir.moodleactividades.data

import android.content.Context
import com.asir.moodleactividades.domain.Actividad
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Instantanea(
    val actividades: List<Actividad>,
    val momento: Long
)

class CacheActividades(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("cache_actividades", Context.MODE_PRIVATE)

    fun guardar(actividades: List<Actividad>, momento: Long) {
        val serializado = runCatching {
            json.encodeToString(Instantanea(actividades, momento))
        }.getOrNull() ?: return
        prefs.edit().putString(CLAVE, serializado).apply()
    }

    fun leer(): Instantanea? {
        val guardado = prefs.getString(CLAVE, null) ?: return null
        return runCatching { json.decodeFromString<Instantanea>(guardado) }.getOrNull()
    }

    fun borrar() = prefs.edit().remove(CLAVE).apply()

    private companion object {
        const val CLAVE = "instantanea"
        val json = Json { ignoreUnknownKeys = true }
    }
}
