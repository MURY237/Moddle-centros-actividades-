package com.asir.moodleactividades.data.grupos

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * En las preferencias privadas de la app, con la copia de seguridad del sistema desactivada,
 * igual que el token de Moodle: son una sesión, no una contraseña.
 */
class GuardaSesionPrefs(contexto: Context) : GuardaSesion {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("sesion_grupos", Context.MODE_PRIVATE)

    override fun leer(): SesionAuth? {
        val guardada = prefs.getString(CLAVE, null) ?: return null
        return runCatching { json.decodeFromString<SesionAuth>(guardada) }.getOrNull()
    }

    override fun guardar(sesion: SesionAuth) {
        prefs.edit().putString(CLAVE, json.encodeToString(sesion)).apply()
    }

    override fun borrar() = prefs.edit().remove(CLAVE).apply()

    private companion object {
        const val CLAVE = "sesion"
        val json = Json { ignoreUnknownKeys = true }
    }
}
