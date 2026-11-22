package com.asir.moodleactividades.data

import android.content.Context
import com.asir.moodleactividades.domain.Actividad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Instantanea(
    val actividades: List<Actividad>,
    val momento: Long,
    /**
     * Todas las asignaturas en las que está matriculado, tengan actividades o no. Sin esta
     * lista, una asignatura recién creada no existiría para la app hasta tener su primera
     * tarea, porque el resto de la pantalla se deduce de las actividades.
     */
    val asignaturas: List<String> = emptyList(),
    /**
     * Los cuestionarios salen de su propia API (desde la 2.7). Hasta entonces solo se veían
     * los pendientes, por el calendario: con esto el aviso de «actividades nuevas» sabe que
     * los que aparecen al actualizar no son nuevos, sino que antes no se leían.
     */
    val cuestionarios: Boolean = false
)

class CacheActividades(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("cache_actividades", Context.MODE_PRIVATE)

    fun guardar(
        actividades: List<Actividad>,
        momento: Long,
        asignaturas: List<String> = emptyList(),
        cuestionarios: Boolean = false
    ) {
        val serializado = runCatching {
            json.encodeToString(Instantanea(actividades, momento, asignaturas, cuestionarios))
        }.getOrNull() ?: return
        prefs.edit().putString(CLAVE, serializado).apply()
        _guardadas.update { it + 1 }
    }

    fun leer(): Instantanea? {
        val guardado = prefs.getString(CLAVE, null) ?: return null
        return runCatching { json.decodeFromString<Instantanea>(guardado) }.getOrNull()
    }

    fun borrar() = prefs.edit().remove(CLAVE).apply()

    companion object {
        private const val CLAVE = "instantanea"
        private val json = Json { ignoreUnknownKeys = true }

        private val _guardadas = MutableStateFlow(0)

        /**
         * Cuántas cargas se han guardado en este proceso. Notas se construye a la vez que
         * Tareas y leía los exámenes de la carga anterior: con esto se entera de la nueva.
         */
        val guardadas: StateFlow<Int> = _guardadas.asStateFlow()
    }
}
