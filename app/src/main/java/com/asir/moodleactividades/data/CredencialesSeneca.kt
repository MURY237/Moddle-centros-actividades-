package com.asir.moodleactividades.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

data class Credenciales(val usuario: String, val clave: String) {
    val completas: Boolean get() = usuario.isNotBlank() && clave.isNotBlank()
}

/**
 * Usuario y contraseña de Séneca, para volver a entrar cuando el servidor caduca la sesión.
 *
 * Es el único dato realmente sensible que guarda la aplicación, así que no va a unas
 * preferencias normales: se cifra con una clave que vive en el almacén de claves de Android,
 * fuera del alcance del sistema de ficheros. Aun así, guardar una contraseña nunca sale
 * gratis, y por eso esto es opcional y se borra de un toque.
 */
class CredencialesSeneca(contexto: Context) {

    private val app = contexto.applicationContext

    /**
     * Si el almacén de claves falla —ocurre en algunos dispositivos tras restaurar una copia
     * de seguridad—, se prefiere quedarse sin credenciales a guardarlas sin cifrar.
     */
    private val prefs: SharedPreferences? by lazy {
        runCatching {
            // La clave maestra se crea dentro del almacén de claves del dispositivo; aquí
            // solo se maneja su alias, nunca el material de la clave.
            val alias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "credenciales_seneca",
                alias,
                app,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrNull()
    }

    val disponible: Boolean get() = prefs != null

    fun guardar(credenciales: Credenciales) {
        if (!credenciales.completas) return
        prefs?.edit()
            ?.putString(USUARIO, credenciales.usuario.trim())
            ?.putString(CLAVE, credenciales.clave)
            ?.apply()
    }

    fun leer(): Credenciales? {
        val almacen = prefs ?: return null
        val usuario = almacen.getString(USUARIO, null).orEmpty()
        val clave = almacen.getString(CLAVE, null).orEmpty()
        return Credenciales(usuario, clave).takeIf { it.completas }
    }

    fun hay(): Boolean = leer() != null

    /** Solo el usuario: sirve para enseñar en pantalla con qué cuenta se va a entrar. */
    fun usuario(): String = prefs?.getString(USUARIO, null).orEmpty()

    fun borrar() {
        prefs?.edit()?.clear()?.apply()
    }

    private companion object {
        const val USUARIO = "usuario"
        const val CLAVE = "clave"
    }
}
