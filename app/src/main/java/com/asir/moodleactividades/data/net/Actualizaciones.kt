package com.asir.moodleactividades.data.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class ReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    val assets: List<AssetDto> = emptyList()
)

@Serializable
data class AssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val urlDescarga: String = "",
    /** «sha256:…», la huella que calcula GitHub al subir el fichero. */
    val digest: String = ""
)

data class Actualizacion(
    val version: String,
    val urlApk: String,
    val notas: String,
    /** Huella SHA-256 esperada, en hexadecimal; null si GitHub no la dio. */
    val sha256: String? = null
)

class Actualizaciones(private val versionInstalada: String) {

    private val cliente = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun buscar(): Actualizacion? = runCatching {
        val peticion = Request.Builder()
            .url(URL_ULTIMA)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", MoodleClient.USER_AGENT)
            .build()

        val cuerpo = cliente.newCall(peticion).execute().use { respuesta ->
            if (!respuesta.isSuccessful) return null
            respuesta.body?.string() ?: return null
        }

        val release = json.decodeFromString<ReleaseDto>(cuerpo)
        val apk = release.assets.firstOrNull { it.name.endsWith(".apk") } ?: return null
        if (!urlPermitida(apk.urlDescarga)) return null
        val version = release.tagName.removePrefix("v")

        if (comparar(version, versionInstalada) <= 0) return null

        Actualizacion(
            version = version,
            urlApk = apk.urlDescarga,
            notas = release.body,
            sha256 = huellaDe(apk.digest)
        )
    }.getOrNull()

    /**
     * Baja el APK a un temporal y solo lo pone en [destino] si llegó entero y su huella es la
     * que publicó GitHub. Un fichero a medias o distinto no llega nunca al instalador.
     */
    fun descargar(url: String, destino: java.io.File, sha256: String? = null): Boolean {
        if (!urlPermitida(url)) return false
        val temporal = java.io.File(destino.parentFile, destino.name + ".parte")
        val correcto = runCatching {
            val peticion = Request.Builder()
                .url(url)
                .header("User-Agent", MoodleClient.USER_AGENT)
                .build()

            val resumen = java.security.MessageDigest.getInstance("SHA-256")
            cliente.newCall(peticion).execute().use { respuesta ->
                if (!respuesta.isSuccessful) return@runCatching false
                val flujo = respuesta.body?.byteStream() ?: return@runCatching false
                temporal.outputStream().use { salida ->
                    val bloque = ByteArray(64 * 1024)
                    while (true) {
                        val leidos = flujo.read(bloque)
                        if (leidos < 0) break
                        resumen.update(bloque, 0, leidos)
                        salida.write(bloque, 0, leidos)
                    }
                }
            }
            val obtenida = resumen.digest().joinToString("") { "%02x".format(it) }
            sha256 == null || obtenida.equals(sha256, ignoreCase = true)
        }.getOrDefault(false)

        if (!correcto) {
            temporal.delete()
            return false
        }
        destino.delete()
        return temporal.renameTo(destino)
    }

    companion object {
        const val REPOSITORIO = "MURY237/Moddle-centros-actividades-"
        const val URL_ULTIMA = "https://api.github.com/repos/$REPOSITORIO/releases/latest"

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * El APK solo se baja de las descargas de este repositorio en GitHub y por HTTPS: si
         * la respuesta de la API trajera otra dirección, no se sigue.
         */
        fun urlPermitida(url: String): Boolean {
            val destino = runCatching { java.net.URI(url) }.getOrNull() ?: return false
            return destino.scheme.equals("https", ignoreCase = true) &&
                destino.host.equals("github.com", ignoreCase = true) &&
                (destino.path ?: "").startsWith("/$REPOSITORIO/releases/download/", ignoreCase = true)
        }

        /** «sha256:ab12…» → «ab12…»; null si no es una huella SHA-256 válida. */
        fun huellaDe(digest: String): String? {
            val valor = digest.trim().removePrefix("sha256:")
            return valor.lowercase().takeIf { it.length == 64 && it.all { c -> c in "0123456789abcdef" } }
        }

        /** Compara "1.10" con "1.9" por número de segmento, no alfabéticamente. */
        fun comparar(a: String, b: String): Int {
            val partesA = a.split('.').map { it.toIntOrNull() ?: 0 }
            val partesB = b.split('.').map { it.toIntOrNull() ?: 0 }

            for (indice in 0 until maxOf(partesA.size, partesB.size)) {
                val valorA = partesA.getOrElse(indice) { 0 }
                val valorB = partesB.getOrElse(indice) { 0 }
                if (valorA != valorB) return valorA.compareTo(valorB)
            }
            return 0
        }
    }
}
