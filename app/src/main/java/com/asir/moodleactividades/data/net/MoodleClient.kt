package com.asir.moodleactividades.data.net

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class MoodleClient(
    val urlSitio: String,
    private val token: String? = null
) {

    private val servicio: MoodleService = Retrofit.Builder()
        .baseUrl(urlSitio)
        .client(
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                // Sin User-Agent propio OkHttp manda el suyo, que los cortafuegos de centro filtran.
                .addInterceptor { cadena ->
                    cadena.proceed(
                        cadena.request().newBuilder()
                            .header("User-Agent", USER_AGENT)
                            .build()
                    )
                }
                .build()
        )
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
        .create(MoodleService::class.java)

    suspend fun pedirToken(usuario: String, contrasena: String): String {
        val crudo = servicio.obtenerToken(
            mapOf(
                "username" to usuario,
                "password" to contrasena,
                "service" to SERVICIO_MOVIL
            )
        )
        val objeto = json.parseToJsonElement(crudo) as? JsonObject
            ?: throw MoodleException(null, "Respuesta inesperada del servidor Moodle.")

        objeto.texto("token")?.let { return it }

        throw MoodleException(
            objeto.texto("errorcode"),
            objeto.texto("error") ?: objeto.texto("message") ?: "No se pudo iniciar sesión."
        )
    }

    suspend fun invocar(funcion: String, parametros: Map<String, String> = emptyMap()): String {
        val autenticacion = token ?: throw MoodleException(null, "Sesión no iniciada.")
        val crudo = servicio.llamar(
            buildMap {
                put("wstoken", autenticacion)
                put("wsfunction", funcion)
                put("moodlewsrestformat", "json")
                putAll(parametros)
            }
        )
        comprobarError(json.parseToJsonElement(crudo))
        return crudo
    }

    private fun comprobarError(elemento: JsonElement) {
        val objeto = elemento as? JsonObject ?: return
        val codigo = objeto.texto("errorcode") ?: return
        throw MoodleException(
            codigo,
            objeto.texto("message") ?: objeto.texto("error") ?: "Error de Moodle ($codigo)."
        )
    }

    companion object {
        const val SERVICIO_MOVIL = "moodle_mobile_app"
        const val USER_AGENT = "MoodleActividades/1.2 (Android)"

        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }

        fun JsonObject.texto(clave: String): String? =
            this[clave]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

        /**
         * La URL se copia normalmente desde la barra del navegador, sin esquema y con la ruta
         * de la página en la que estaba el usuario colgando del final.
         */
        fun normalizarUrl(entrada: String): String {
            var url = entrada.trim()
            if (url.isEmpty()) return ""
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            url = url.substringBefore('?').substringBefore('#')
            for (ruta in RUTAS_CONOCIDAS) {
                val indice = url.indexOf(ruta)
                if (indice > 0) {
                    url = url.substring(0, indice)
                    break
                }
            }
            return url.trimEnd('/') + "/"
        }

        private val RUTAS_CONOCIDAS = listOf(
            "/login/index.php",
            "/login/token.php",
            "/webservice/",
            "/my/",
            "/course/",
            "/user/",
            "/calendar/",
            "/mod/"
        )
    }
}

inline fun <reified T> MoodleClient.decodificar(crudo: String): T =
    MoodleClient.json.decodeFromString(crudo)
