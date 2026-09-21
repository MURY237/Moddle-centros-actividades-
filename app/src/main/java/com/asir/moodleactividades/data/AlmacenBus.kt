package com.asir.moodleactividades.data

import android.content.Context
import com.asir.moodleactividades.domain.LineaBus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Las líneas de autobús que se apunta el alumno. Es lo único de la app que no viene de
 * ningún servidor: lo escribe él a partir del papel de la parada o de la web del ayuntamiento.
 */
class AlmacenBus(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("bus", Context.MODE_PRIVATE)

    fun guardar(lineas: List<LineaBus>) {
        val serializado = runCatching { json.encodeToString(lineas) }.getOrNull() ?: return
        prefs.edit().putString(LINEAS, serializado).apply()
    }

    fun leer(): List<LineaBus> {
        val guardado = prefs.getString(LINEAS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<LineaBus>>(guardado)
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val LINEAS = "lineas"
        val json = Json { ignoreUnknownKeys = true }
    }
}
