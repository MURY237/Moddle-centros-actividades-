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
        val gestor = runCatching { CookieManager.getInstance() }.getOrNull() ?: return

        // Las cookies de Séneca cuelgan de /seneca, y preguntando solo por la raíz del
        // dominio no se devuelven: hay que pedirlas por cada ruta donde pueden vivir.
        val recogidas = RUTAS
            .mapNotNull { runCatching { gestor.getCookie(it) }.getOrNull() }
            .flatMap { it.split(';') }
            .map { it.trim() }
            .filter { it.isNotEmpty() && '=' in it }
            // Una misma cookie puede venir por dos rutas; se queda la última vista.
            .associateBy { it.substringBefore('=') }
            .values

        if (recogidas.isEmpty()) return

        prefs.edit()
            .putString(COOKIES, recogidas.joinToString("; "))
            .putLong(MOMENTO, System.currentTimeMillis() / 1000)
            .apply()
    }

    fun restaurar() {
        val guardadas = prefs.getString(COOKIES, null) ?: return
        runCatching {
            val gestor = CookieManager.getInstance()
            gestor.setAcceptCookie(true)
            // getCookie devuelve «nombre=valor; nombre=valor», sin atributos: se reponen una
            // a una y en todas las rutas, porque no se sabe de cuál venía cada una.
            guardadas.split(';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { cookie -> RUTAS.forEach { ruta -> gestor.setCookie(ruta, cookie) } }
            gestor.flush()
        }
    }

    fun hay(): Boolean = !prefs.getString(COOKIES, null).isNullOrBlank()

    fun borrar() = prefs.edit().clear().apply()

    private companion object {
        val RUTAS = listOf(
            "https://seneca.juntadeandalucia.es/",
            "https://seneca.juntadeandalucia.es/seneca/",
            "https://seneca.juntadeandalucia.es/seneca/jsp/"
        )
        const val COOKIES = "cookies"
        const val MOMENTO = "momento"
    }
}
