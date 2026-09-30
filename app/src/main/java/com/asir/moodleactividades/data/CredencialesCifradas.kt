package com.asir.moodleactividades.data

import android.content.Context
import android.content.SharedPreferences

data class Credenciales(val usuario: String, val clave: String) {
    val completas: Boolean get() = usuario.isNotBlank() && clave.isNotBlank()
}

/**
 * Usuario y contraseña de un servicio —Séneca o Cisco NetAcad—, para volver a entrar cuando
 * el servidor caduca la sesión. Cada servicio va en su propio fichero, [archivo].
 *
 * Son los únicos datos realmente sensibles que guarda la aplicación, así que no van a unas
 * preferencias normales: se cifra con una clave que vive en el almacén de claves de Android,
 * fuera del alcance del sistema de ficheros. Aun así, guardar una contraseña nunca sale
 * gratis, y por eso esto es opcional y se borra de un toque.
 */
class CredencialesCifradas(contexto: Context, private val archivo: String) {

    private val app = contexto.applicationContext

    /**
     * Si el almacén de claves falla —ocurre en algunos dispositivos tras restaurar una copia
     * de seguridad—, se prefiere quedarse sin credenciales a guardarlas sin cifrar.
     */
    private val prefs: SharedPreferences? by lazy { PreferenciasCifradas.abrir(app, archivo) }

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

    companion object {
        /** Se queda con el nombre de siempre: así la cuenta ya guardada sigue valiendo. */
        const val SENECA = "credenciales_seneca"
        const val NETACAD = "credenciales_netacad"

        private const val USUARIO = "usuario"
        private const val CLAVE = "clave"
    }
}
