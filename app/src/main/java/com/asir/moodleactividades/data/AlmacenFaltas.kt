package com.asir.moodleactividades.data

import android.content.Context
import com.asir.moodleactividades.domain.Falta
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FaltasGuardadas(
    val faltas: List<Falta>,
    val momento: Long
)

/**
 * Las faltas leídas de Séneca se quedan en el móvil para poder consultarlas sin volver a
 * entrar. Nunca se guarda la contraseña ni la sesión: eso vive en las cookies del WebView,
 * que se borran al desconectar.
 */
class AlmacenFaltas(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("faltas_seneca", Context.MODE_PRIVATE)

    fun guardar(faltas: List<Falta>, momento: Long = System.currentTimeMillis() / 1000) {
        val serializado = runCatching {
            json.encodeToString(FaltasGuardadas(faltas, momento))
        }.getOrNull() ?: return
        prefs.edit().putString(CLAVE, serializado).apply()
    }

    fun leer(): FaltasGuardadas? {
        val guardado = prefs.getString(CLAVE, null) ?: return null
        return runCatching { json.decodeFromString<FaltasGuardadas>(guardado) }.getOrNull()
    }

    fun borrar() = prefs.edit().remove(CLAVE).apply()

    private companion object {
        const val CLAVE = "faltas"
        val json = Json { ignoreUnknownKeys = true }
    }
}
