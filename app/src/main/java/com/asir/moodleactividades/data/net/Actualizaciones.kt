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
    @SerialName("browser_download_url") val urlDescarga: String = ""
)

data class Actualizacion(
    val version: String,
    val urlApk: String,
    val notas: String
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
        val version = release.tagName.removePrefix("v")

        if (comparar(version, versionInstalada) <= 0) return null

        Actualizacion(version = version, urlApk = apk.urlDescarga, notas = release.body)
    }.getOrNull()

    fun descargar(url: String, destino: java.io.File): Boolean = runCatching {
        val peticion = Request.Builder()
            .url(url)
            .header("User-Agent", MoodleClient.USER_AGENT)
            .build()

        cliente.newCall(peticion).execute().use { respuesta ->
            if (!respuesta.isSuccessful) return false
            val flujo = respuesta.body?.byteStream() ?: return false
            destino.outputStream().use { salida -> flujo.copyTo(salida) }
        }
        true
    }.getOrDefault(false)

    companion object {
        const val REPOSITORIO = "MURY237/Moddle-centros-actividades-"
        const val URL_ULTIMA = "https://api.github.com/repos/$REPOSITORIO/releases/latest"

        private val json = Json { ignoreUnknownKeys = true }

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
