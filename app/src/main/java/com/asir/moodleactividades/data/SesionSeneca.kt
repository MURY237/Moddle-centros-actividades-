package com.asir.moodleactividades.data

import android.content.Context
import android.webkit.CookieManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Séneca mantiene la sesión con cookies que el navegador incrustado tira al cerrarse la app,
 * porque no llevan fecha de caducidad. Guardarlas aquí evita tener que identificarse en cada
 * arranque: se restauran antes de cargar la página y la sesión continúa donde estaba.
 *
 * Lo guardado equivale a una sesión abierta: quien copie esas cookies entra en Séneca como
 * el alumno mientras dure. Por eso van cifradas con el almacén de claves, igual que el token
 * de Moodle (hasta la 2.5 iban en claro; se pasan solas al leerlas). Desaparecen al
 * desconectar o cuando Séneca caduca la sesión por su cuenta.
 */
class SesionSeneca(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("sesion_seneca", Context.MODE_PRIVATE)

    private val cifradas by lazy { PreferenciasCifradas.abrir(contexto, "sesion_seneca_cifrada") }

    private fun cookiesGuardadas(): String? =
        PreferenciasCifradas.leerMigrando(cifradas, prefs, COOKIES)

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

        PreferenciasCifradas.escribir(cifradas, prefs, COOKIES, recogidas.joinToString("; "))
        prefs.edit().putLong(MOMENTO, System.currentTimeMillis() / 1000).apply()
    }

    fun restaurar() {
        val guardadas = cookiesGuardadas() ?: return
        runCatching {
            val gestor = CookieManager.getInstance()
            gestor.setAcceptCookie(true)
            // getCookie devuelve «nombre=valor; nombre=valor», sin atributos: se reponen una
            // a una y en todas las rutas, porque no se sabe de cuál venía cada una. Con
            // «Secure», para que solo viajen por HTTPS aunque el original no lo dijera.
            guardadas.split(';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { cookie -> RUTAS.forEach { ruta -> gestor.setCookie(ruta, "$cookie; Secure") } }
            gestor.flush()
        }
    }

    fun hay(): Boolean = !cookiesGuardadas().isNullOrBlank()

    /**
     * Seneca no lleva la sesion solo en las cookies: tambien en la ruta `/seneca/nav/<algo>`
     * que genera al entrar. Volviendo a la raiz se pierde ese hilo, asi que se guarda la
     * pagina donde se encontro la tabla y se vuelve directamente a ella.
     */
    fun guardarUrl(url: String?) {
        if (url.isNullOrBlank() || !esDeSeneca(url)) return
        prefs.edit().putString(URL, url).apply()
    }

    /** Se comprueba también al leer: lo guardado por versiones anteriores no pasó este filtro. */
    fun urlGuardada(): String? = prefs.getString(URL, null)?.takeIf { esDeSeneca(it) }

    /** Solo los nombres de las cookies: sus valores son la sesion y no deben salir de aqui. */
    fun nombres(): List<String> =
        cookiesGuardadas().orEmpty()
            .split(';')
            .map { it.trim().substringBefore('=') }
            .filter { it.isNotEmpty() }

    fun momento(): Long? = prefs.getLong(MOMENTO, 0).takeIf { it > 0 }

    /** Cuantas cookies ve el navegador ahora mismo, que puede no ser lo guardado. */
    fun vivasAhora(): Int = runCatching {
        val gestor = CookieManager.getInstance()
        RUTAS.mapNotNull { gestor.getCookie(it) }
            .flatMap { it.split(';') }
            .map { it.trim().substringBefore('=') }
            .filter { it.isNotEmpty() }
            .distinct()
            .size
    }.getOrDefault(0)

    /**
     * Olvida la sesión: lo guardado y las cookies de Séneca que tenga el navegador. Antes se
     * usaba removeAllCookies, que cerraba también la sesión de NetAcad; ahora se caducan una
     * a una, en cada ruta y dominio donde pueden vivir, porque una cookie solo se borra
     * nombrando exactamente su ruta y su dominio.
     */
    fun borrar() {
        runCatching {
            val gestor = CookieManager.getInstance()
            val nombres = RUTAS
                .flatMap { gestor.getCookie(it).orEmpty().split(';') }
                .map { it.trim().substringBefore('=') }
                .filter { it.isNotEmpty() }
                .distinct()
            nombres.forEach { nombre ->
                RUTAS.forEach { ruta ->
                    CAMINOS.forEach { camino ->
                        gestor.setCookie(ruta, "$nombre=; Max-Age=0; Path=$camino")
                        DOMINIOS.forEach { dominio ->
                            gestor.setCookie(ruta, "$nombre=; Max-Age=0; Path=$camino; Domain=$dominio")
                        }
                    }
                }
            }
            gestor.flush()
        }
        PreferenciasCifradas.borrar(cifradas, prefs, COOKIES)
        prefs.edit().clear().apply()
    }

    companion object {
        /**
         * Solo el Séneca de verdad y por HTTPS. Antes bastaba con que la dirección empezara
         * por «https://seneca.», y eso lo cumple también «https://seneca.loquesea.com»: una
         * página así se habría guardado y cargado sola en la siguiente apertura.
         */
        fun esDeSeneca(url: String): Boolean {
            val destino = url.trim().toHttpUrlOrNull() ?: return false
            return destino.isHttps && destino.host == HOST
        }

        private const val HOST = "seneca.juntadeandalucia.es"

        private val RUTAS = listOf(
            "https://seneca.juntadeandalucia.es/",
            "https://seneca.juntadeandalucia.es/seneca/",
            "https://seneca.juntadeandalucia.es/seneca/jsp/"
        )

        /** Las rutas con que pueden venir: las del servidor y las que pone restaurar(). */
        private val CAMINOS = listOf("/", "/seneca", "/seneca/", "/seneca/jsp", "/seneca/jsp/")
        private val DOMINIOS = listOf(".seneca.juntadeandalucia.es", ".juntadeandalucia.es")
        private const val COOKIES = "cookies"
        private const val MOMENTO = "momento"
        private const val URL = "url"
    }
}
