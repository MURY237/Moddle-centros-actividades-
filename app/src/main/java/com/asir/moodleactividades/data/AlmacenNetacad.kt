package com.asir.moodleactividades.data

import android.content.Context
import com.asir.moodleactividades.domain.TrabajoNetacad
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TrabajosGuardados(
    val trabajos: List<TrabajoNetacad>,
    val momento: Long
)

/**
 * Los trabajos leídos de NetAcad se quedan en el móvil para poder consultarlos sin volver a
 * abrir la web. Nunca se guarda la contraseña de Cisco: el acceso lo hace el alumno en la
 * propia página de Cisco, y la app solo conserva la sesión que ya abrió.
 */
class AlmacenNetacad(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("trabajos_netacad", Context.MODE_PRIVATE)

    fun guardar(trabajos: List<TrabajoNetacad>, momento: Long = System.currentTimeMillis() / 1000) {
        val serializado = runCatching {
            json.encodeToString(TrabajosGuardados(trabajos, momento))
        }.getOrNull() ?: return
        prefs.edit().putString(CLAVE, serializado).apply()
    }

    fun leer(): TrabajosGuardados? {
        val guardado = prefs.getString(CLAVE, null) ?: return null
        return runCatching { json.decodeFromString<TrabajosGuardados>(guardado) }.getOrNull()
    }

    fun borrar() = prefs.edit().remove(CLAVE).apply()

    private companion object {
        const val CLAVE = "trabajos"
        val json = Json { ignoreUnknownKeys = true }
    }
}
