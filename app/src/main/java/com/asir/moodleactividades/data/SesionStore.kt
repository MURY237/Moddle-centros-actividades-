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

class SesionStore(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("sesion", Context.MODE_PRIVATE)

    fun guardar(sesion: Sesion) {
        prefs.edit()
            .putString(CLAVE_URL, sesion.urlSitio)
            .putString(CLAVE_TOKEN, sesion.token)
            .putString(CLAVE_USUARIO, sesion.usuario)
            .putString(CLAVE_NOMBRE, sesion.nombreCompleto)
            .putString(CLAVE_SITIO, sesion.nombreSitio)
            .putLong(CLAVE_ID_USUARIO, sesion.idUsuario)
            .apply()
    }

    fun leer(): Sesion? {
        val url = prefs.getString(CLAVE_URL, null) ?: return null
        val token = prefs.getString(CLAVE_TOKEN, null) ?: return null
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

    fun borrar() {
        prefs.edit().remove(CLAVE_TOKEN).remove(CLAVE_USUARIO).remove(CLAVE_NOMBRE).apply()
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
