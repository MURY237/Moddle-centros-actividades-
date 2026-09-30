package com.asir.moodleactividades.data.grupos

import android.content.Context
import com.asir.moodleactividades.data.PreferenciasCifradas
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * La sesión de los grupos, cifrada con el almacén de claves como el token de Moodle: con el
 * token de refresco se puede entrar en los grupos como este móvil. Si el almacén falla, va en
 * las preferencias privadas de la app, con la copia de seguridad del sistema desactivada.
 */
class GuardaSesionPrefs(contexto: Context) : GuardaSesion {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("sesion_grupos", Context.MODE_PRIVATE)

    private val cifradas by lazy { PreferenciasCifradas.abrir(contexto, "sesion_grupos_cifrada") }

    override fun leer(): SesionAuth? {
        val guardada = PreferenciasCifradas.leerMigrando(cifradas, prefs, CLAVE) ?: return null
        return runCatching { json.decodeFromString<SesionAuth>(guardada) }.getOrNull()
    }

    override fun guardar(sesion: SesionAuth) {
        PreferenciasCifradas.escribir(cifradas, prefs, CLAVE, json.encodeToString(sesion))
    }

    override fun borrar() = PreferenciasCifradas.borrar(cifradas, prefs, CLAVE)

    private companion object {
        const val CLAVE = "sesion"
        val json = Json { ignoreUnknownKeys = true }
    }
}
