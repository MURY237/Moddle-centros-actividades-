package com.asir.moodleactividades.data.grupos

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Habla con Supabase por su API REST, con el mismo OkHttp que ya usa la app para Moodle:
 * una librería entera para cuatro tipos de petición no compensa lo que pesa ni lo que ata.
 *
 * Se encarga de la sesión anónima: la crea la primera vez, la renueva antes de que caduque
 * y la repite una vez si el servidor la rechaza a mitad de camino.
 */
class ClienteSupabase(
    private val config: ConfigSupabase,
    private val guarda: GuardaSesion,
    private val http: OkHttpClient = HTTP,
    private val reloj: () -> Long = { System.currentTimeMillis() / 1000 }
) {
    /** Dos peticiones a la vez no deben renovar la sesión dos veces: la segunda la invalidaría. */
    private val cerrojo = Mutex()

    /**
     * Se tuvo que crear una identidad nueva porque la guardada ya no valía. Los grupos de
     * la anterior no vuelven solos: hay que entrar otra vez con los códigos, y eso hay que
     * decirlo en vez de enseñar una lista vacía sin explicación.
     */
    @Volatile
    var identidadRenovada = false
        private set

    val base: HttpUrl by lazy { ConfigSupabase.limpiarUrl(config.url).toHttpUrl() }

    suspend fun usuario(): String = sesion(forzar = false).usuario

    fun url(ruta: String): HttpUrl.Builder = base.newBuilder().addPathSegments(ruta)

    suspend fun get(url: HttpUrl): String = rest("GET", url, null, representacion = false)

    suspend fun post(url: HttpUrl, cuerpo: JsonObject, representacion: Boolean = true): String =
        rest("POST", url, cuerpo.toString(), representacion)

    suspend fun patch(url: HttpUrl, cuerpo: JsonObject): String =
        rest("PATCH", url, cuerpo.toString(), representacion = true)

    suspend fun delete(url: HttpUrl): String = rest("DELETE", url, null, representacion = false)

    suspend fun rpc(funcion: String, cuerpo: JsonObject): String =
        rest("POST", url("rest/v1/rpc/$funcion").build(), cuerpo.toString(), representacion = false)

    /** Olvida la identidad de este móvil. Los grupos siguen existiendo para los demás. */
    fun olvidar() = guarda.borrar()

    private suspend fun rest(metodo: String, url: HttpUrl, cuerpo: String?, representacion: Boolean): String {
        var respuesta = ejecutar(peticion(metodo, url, cuerpo, representacion, sesion(false).acceso))
        // El token puede caducar entre que se mira y que llega: se renueva y se repite una vez.
        if (respuesta.estado == 401) {
            respuesta = ejecutar(peticion(metodo, url, cuerpo, representacion, sesion(true).acceso))
        }
        if (respuesta.estado !in 200..299) throw TraductorErrores.traducir(respuesta.estado, respuesta.cuerpo)
        return respuesta.cuerpo
    }

    private fun peticion(
        metodo: String,
        url: HttpUrl,
        cuerpo: String?,
        representacion: Boolean,
        acceso: String
    ): Request {
        val constructor = Request.Builder()
            .url(url)
            .header("apikey", config.clave)
            .header("Authorization", "Bearer $acceso")
            .header("Accept", "application/json")
        if (representacion) constructor.header("Prefer", "return=representation")
        val envio = cuerpo?.toRequestBody(JSON)
        return constructor.method(metodo, envio).build()
    }

    private suspend fun sesion(forzar: Boolean): SesionAuth = cerrojo.withLock {
        val actual = guarda.leer()
        if (actual != null && !forzar && actual.expira - reloj() > MARGEN_S) return@withLock actual

        val nueva = if (actual == null) {
            registrar()
        } else {
            // Solo se estrena identidad si el servidor dice que la guardada ya no vale. Un
            // fallo de red no es eso: ahí se lanza el error y se reintenta luego, porque una
            // identidad nueva cuesta los grupos.
            renovar(actual) ?: registrar().also { identidadRenovada = true }
        }
        guarda.guardar(nueva)
        nueva
    }

    private suspend fun registrar(): SesionAuth {
        val respuesta = ejecutar(
            Request.Builder()
                .url(url("auth/v1/signup").build())
                .header("apikey", config.clave)
                .post("""{"data":{}}""".toRequestBody(JSON))
                .build()
        )
        if (respuesta.estado !in 200..299) throw TraductorErrores.traducir(respuesta.estado, respuesta.cuerpo)
        return leerSesion(respuesta.cuerpo)
    }

    /** null si el servidor rechaza la renovación: la sesión guardada ya no sirve. */
    private suspend fun renovar(actual: SesionAuth): SesionAuth? {
        val respuesta = ejecutar(
            Request.Builder()
                .url(url("auth/v1/token").addQueryParameter("grant_type", "refresh_token").build())
                .header("apikey", config.clave)
                .post(json.encodeToString(Refresco.serializer(), Refresco(actual.refresco)).toRequestBody(JSON))
                .build()
        )
        if (respuesta.estado in 400..499) return null
        if (respuesta.estado !in 200..299) throw TraductorErrores.traducir(respuesta.estado, respuesta.cuerpo)
        return leerSesion(respuesta.cuerpo)
    }

    private fun leerSesion(cuerpo: String): SesionAuth {
        val datos = runCatching { json.decodeFromString(RespuestaAuth.serializer(), cuerpo) }
            .getOrElse { throw ErrorGrupos("El servidor de grupos respondió algo inesperado.") }
        val expira = datos.expiresAt ?: (reloj() + datos.expiresIn)
        return SesionAuth(datos.accessToken, datos.refreshToken, expira, datos.user.id)
    }

    private suspend fun ejecutar(peticion: Request): Respuesta = withContext(Dispatchers.IO) {
        try {
            http.newCall(peticion).execute().use { r ->
                Respuesta(r.code, r.body?.string().orEmpty())
            }
        } catch (e: IOException) {
            throw TraductorErrores.sinConexion()
        }
    }

    private class Respuesta(val estado: Int, val cuerpo: String)

    @Serializable
    private class Refresco(@SerialName("refresh_token") val refreshToken: String)

    @Serializable
    private class UsuarioAuth(val id: String)

    @Serializable
    private class RespuestaAuth(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String,
        @SerialName("expires_in") val expiresIn: Long = 3600,
        @SerialName("expires_at") val expiresAt: Long? = null,
        val user: UsuarioAuth
    )

    companion object {
        /** Se renueva con un minuto de margen para no llegar al servidor con él caducado. */
        const val MARGEN_S = 60L

        private val JSON = "application/json".toMediaType()
        private val json = Json { ignoreUnknownKeys = true }

        private val HTTP: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
