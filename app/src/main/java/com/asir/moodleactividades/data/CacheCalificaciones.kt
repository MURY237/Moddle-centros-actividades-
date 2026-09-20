package com.asir.moodleactividades.data

import android.content.Context
import com.asir.moodleactividades.domain.NotasDeCurso
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class InstantaneaNotas(
    val cursos: List<NotasDeCurso>,
    val momento: Long
)

/**
 * Copia de las calificaciones de la última consulta. Sirve para dos cosas: enseñar las notas
 * sin conexión y, sobre todo, saber cuáles son nuevas cuando el centro publica una.
 */
class CacheCalificaciones(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("cache_calificaciones", Context.MODE_PRIVATE)

    fun guardar(cursos: List<NotasDeCurso>, momento: Long = System.currentTimeMillis() / 1000) {
        val serializado = runCatching {
            json.encodeToString(InstantaneaNotas(cursos, momento))
        }.getOrNull() ?: return
        prefs.edit().putString(CLAVE, serializado).apply()
    }

    fun leer(): InstantaneaNotas? {
        val guardado = prefs.getString(CLAVE, null) ?: return null
        return runCatching { json.decodeFromString<InstantaneaNotas>(guardado) }.getOrNull()
    }

    fun borrar() = prefs.edit().remove(CLAVE).apply()

    private companion object {
        const val CLAVE = "instantanea"
        val json = Json { ignoreUnknownKeys = true }
    }
}
