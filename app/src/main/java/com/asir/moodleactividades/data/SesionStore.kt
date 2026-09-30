package com.asir.moodleactividades.data

import android.content.Context

data class Sesion(
    val urlSitio: String,
    val token: String,
    val usuario: String,
    val nombreCompleto: String,
    val nombreSitio: String,
    val idUsuario: Long = 0
)

/**
 * La sesión de Moodle. El token da acceso a todo el Moodle del alumno, así que va cifrado
 * con el almacén de claves; lo demás (nombre, sitio) no es secreto y va en preferencias
 * normales. Las versiones anteriores lo guardaban en claro: se migra solo al leerlo.
 */
class SesionStore(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("sesion", Context.MODE_PRIVATE)

    private val cifradas by lazy { PreferenciasCifradas.abrir(contexto, "sesion_cifrada") }

    fun guardar(sesion: Sesion) {
        PreferenciasCifradas.escribir(cifradas, prefs, CLAVE_TOKEN, sesion.token)
        prefs.edit()
            .putString(CLAVE_URL, sesion.urlSitio)
            .putString(CLAVE_USUARIO, sesion.usuario)
            .putString(CLAVE_NOMBRE, sesion.nombreCompleto)
            .putString(CLAVE_SITIO, sesion.nombreSitio)
            .putLong(CLAVE_ID_USUARIO, sesion.idUsuario)
            .apply()
    }

    fun leer(): Sesion? {
        val url = prefs.getString(CLAVE_URL, null) ?: return null
        val token = PreferenciasCifradas.leerMigrando(cifradas, prefs, CLAVE_TOKEN) ?: return null
        return Sesion(
            urlSitio = url,
            token = token,
            usuario = prefs.getString(CLAVE_USUARIO, "").orEmpty(),
            nombreCompleto = prefs.getString(CLAVE_NOMBRE, "").orEmpty(),
            nombreSitio = prefs.getString(CLAVE_SITIO, "").orEmpty(),
            idUsuario = prefs.getLong(CLAVE_ID_USUARIO, 0)
        )
    }

    fun ultimaUrl(): String = prefs.getString(CLAVE_URL, "").orEmpty()

    fun guardarSsoPendiente(urlSitio: String, passport: String) {
        prefs.edit()
            .putString(CLAVE_URL, urlSitio)
            .putString(CLAVE_PASSPORT, passport)
            .apply()
    }

    fun ssoPendiente(): Pair<String, String>? {
        val url = prefs.getString(CLAVE_URL, null) ?: return null
        val passport = prefs.getString(CLAVE_PASSPORT, null) ?: return null
        return url to passport
    }

    fun limpiarSsoPendiente() {
        prefs.edit().remove(CLAVE_PASSPORT).apply()
    }

    /** Cierra la sesión. Se queda solo la URL del centro, para no tener que volver a escribirla. */
    fun borrar() {
        PreferenciasCifradas.borrar(cifradas, prefs, CLAVE_TOKEN)
        prefs.edit()
            .remove(CLAVE_USUARIO)
            .remove(CLAVE_NOMBRE)
            .remove(CLAVE_SITIO)
            .remove(CLAVE_ID_USUARIO)
            .remove(CLAVE_PASSPORT)
            .apply()
    }

    private companion object {
        const val CLAVE_URL = "url_sitio"
        const val CLAVE_TOKEN = "token"
        const val CLAVE_USUARIO = "usuario"
        const val CLAVE_NOMBRE = "nombre"
        const val CLAVE_SITIO = "sitio"
        const val CLAVE_PASSPORT = "passport"
        const val CLAVE_ID_USUARIO = "id_usuario"
    }
}
