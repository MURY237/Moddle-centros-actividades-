package com.asir.moodleactividades.data

import android.content.Context
import android.webkit.CookieManager

/**
 * Séneca mantiene la sesión con cookies que el navegador incrustado tira al cerrarse la app,
 * porque no llevan fecha de caducidad. Guardarlas aquí evita tener que identificarse en cada
 * arranque: se restauran antes de cargar la página y la sesión continúa donde estaba.
 *
 * Lo guardado equivale a una sesión abierta, no a la contraseña: vive en las preferencias
 * privadas de la app, con la copia de seguridad del sistema desactivada, y desaparece al
 * desconectar o cuando Séneca la caduca por su cuenta.
 */
class SesionSeneca(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("sesion_seneca", Context.MODE_PRIVATE)

    fun guardar() {
        val cookies = runCatching { CookieManager.getInstance().getCookie(DOMINIO) }.getOrNull()
        if (cookies.isNullOrBlank()) return
        prefs.edit()
            .putString(COOKIES, cookies)
            .putLong(MOMENTO, System.currentTimeMillis() / 1000)
            .apply()
    }

    fun restaurar() {
        val guardadas = prefs.getString(COOKIES, null) ?: return
        runCatching {
            val gestor = CookieManager.getInstance()
            gestor.setAcceptCookie(true)
            // getCookie devuelve «nombre=valor; nombre=valor», sin atributos: hay que
            // volver a ponerlas una a una sobre el dominio.
            guardadas.split(';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { gestor.setCookie(DOMINIO, it) }
            gestor.flush()
        }
    }

    fun hay(): Boolean = !prefs.getString(COOKIES, null).isNullOrBlank()

    fun borrar() = prefs.edit().clear().apply()

    private companion object {
        const val DOMINIO = "https://seneca.juntadeandalucia.es"
        const val COOKIES = "cookies"
        const val MOMENTO = "momento"
    }
}
