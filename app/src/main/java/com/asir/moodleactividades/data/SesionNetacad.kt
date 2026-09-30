package com.asir.moodleactividades.data

import android.content.Context
import android.webkit.CookieManager
import com.asir.moodleactividades.data.netacad.LectorNetacad

/**
 * Conserva la sesión de NetAcad entre aperturas de la app, igual que [SesionSeneca]: el
 * navegador incrustado tira al cerrarse las cookies sin fecha de caducidad, y sin esta copia
 * habría que pasar por el acceso de Cisco cada vez.
 *
 * Solo se copian las cookies de netacad.com, que son la sesión de la plataforma. Las de
 * id.cisco.com no: son la identidad de Cisco para todos sus servicios, y guardarlas aparte
 * daría a esta app más de lo que necesita. Si Cisco las marca como persistentes, ya las
 * conserva el propio WebView; si no, se vuelve a entrar y ya está.
 *
 * Lo guardado equivale a una sesión abierta, no a la contraseña: vive en las preferencias
 * privadas de la app, con la copia de seguridad del sistema desactivada.
 */
class SesionNetacad(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("sesion_netacad", Context.MODE_PRIVATE)

    fun guardar() {
        val gestor = runCatching { CookieManager.getInstance() }.getOrNull() ?: return
        val recogidas = RUTAS
            .mapNotNull { runCatching { gestor.getCookie(it) }.getOrNull() }
            .flatMap { it.split(';') }
            .map { it.trim() }
            .filter { it.isNotEmpty() && '=' in it }
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
            guardadas.split(';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { cookie -> RUTAS.forEach { ruta -> gestor.setCookie(ruta, cookie) } }
            gestor.flush()
        }
    }

    fun hay(): Boolean = !prefs.getString(COOKIES, null).isNullOrBlank()

    /**
     * Las páginas donde aparecieron trabajos, una por curso, para volver directamente a
     * ellas en el refresco a oscuras. Cada curso de NetAcad tiene su propia página de
     * calificaciones y el panel no las enumera, así que se aprenden cuando el alumno las abre.
     * La más reciente va primero; con más de [MAX_PAGINAS] se olvida la más antigua.
     */
    fun guardarUrl(url: String?) {
        if (url.isNullOrBlank() || !ORIGEN.containsMatchIn(url)) return
        if (LectorNetacad.esUrlDeInicio(url)) return
        val limpia = url.substringBefore('#')
        val lista = (listOf(limpia) + urls().filterNot { it == limpia }).take(MAX_PAGINAS)
        prefs.edit().putString(URLS, lista.joinToString("\n")).apply()
    }

    // El panel se llegó a aprender como página de curso: se descarta también al leer.
    fun urls(): List<String> =
        prefs.getString(URLS, null).orEmpty().split('\n')
            .filter { it.isNotBlank() && !LectorNetacad.esUrlDeInicio(it) }

    /** Solo los nombres: los valores son la sesión y no deben salir de aquí. */
    fun nombres(): List<String> =
        prefs.getString(COOKIES, null).orEmpty()
            .split(';')
            .map { it.trim().substringBefore('=') }
            .filter { it.isNotEmpty() }

    fun momento(): Long? = prefs.getLong(MOMENTO, 0).takeIf { it > 0 }

    /**
     * Desconectar tiene que cerrar de verdad la sesión, también la de Cisco: si no, la
     * siguiente apertura entraría sola con la cuenta anterior. No se usa removeAllCookies
     * porque se llevaría por delante la sesión de Séneca, así que se caducan una a una.
     */
    fun borrar() {
        runCatching {
            val gestor = CookieManager.getInstance()
            (RUTAS + CISCO).forEach { ruta ->
                val dominio = ruta.removePrefix("https://").substringBefore('/')
                gestor.getCookie(ruta).orEmpty()
                    .split(';')
                    .map { it.trim().substringBefore('=') }
                    .filter { it.isNotEmpty() }
                    .forEach { nombre ->
                        gestor.setCookie(ruta, "$nombre=; Max-Age=0; Path=/")
                        gestor.setCookie(ruta, "$nombre=; Max-Age=0; Path=/; Domain=.$dominio")
                    }
            }
            gestor.flush()
        }
        prefs.edit().clear().apply()
    }

    companion object {
        const val INICIO = "https://www.netacad.com/dashboard"

        private val RUTAS = listOf(
            "https://www.netacad.com/",
            "https://netacad.com/"
        )
        private val CISCO = listOf("https://id.cisco.com/")
        private val ORIGEN = Regex("""^https://(www\.)?netacad\.com/""")
        private const val COOKIES = "cookies"
        private const val MOMENTO = "momento"
        private const val URLS = "urls"
        private const val MAX_PAGINAS = 6
    }
}
