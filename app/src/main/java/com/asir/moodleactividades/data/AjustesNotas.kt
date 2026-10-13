package com.asir.moodleactividades.data

import android.content.Context
import com.asir.moodleactividades.domain.AjusteNota
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Lo que el alumno ha corregido de sus notas —la nota máxima real o el resultado—, indexado
 * por la clave de cada calificación. Solo vive en este móvil.
 */
class AjustesNotas(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("ajustes_notas", Context.MODE_PRIVATE)

    fun leer(): Map<String, AjusteNota> {
        val guardado = prefs.getString(CLAVE, null) ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, AjusteNota>>(guardado) }.getOrDefault(emptyMap())
    }

    /** Un ajuste vacío es volver a lo de Moodle: se borra en vez de guardarlo. */
    fun guardar(clave: String, ajuste: AjusteNota): Map<String, AjusteNota> {
        val todos = if (ajuste.vacio) leer() - clave else leer() + (clave to ajuste)
        prefs.edit().putString(CLAVE, json.encodeToString(todos)).apply()
        return todos
    }

    fun borrar() = prefs.edit().remove(CLAVE).apply()

    private companion object {
        const val CLAVE = "ajustes"
        val json = Json { ignoreUnknownKeys = true }
    }
}
