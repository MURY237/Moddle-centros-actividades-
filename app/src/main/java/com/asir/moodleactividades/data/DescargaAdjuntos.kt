package com.asir.moodleactividades.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.asir.moodleactividades.data.net.MoodleClient
import com.asir.moodleactividades.domain.Adjunto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Guarda los adjuntos de una actividad dentro de la caché de la app para poder abrirlos sin
 * entrar en Moodle. Van a la caché y no a `filesDir` porque son copias recuperables: si el
 * sistema necesita espacio puede tirarlas y se vuelven a bajar.
 */
class DescargaAdjuntos(
    contexto: Context,
    private val repositorio: ActividadesRepository
) {

    private val app = contexto.applicationContext

    private val carpeta: File
        get() = File(app.cacheDir, "adjuntos")

    /**
     * Dos asignaturas pueden tener un «Enunciado.pdf» cada una, así que el nombre se prefija
     * con la huella de su URL para que no se pisen entre ellos.
     */
    fun archivoDe(adjunto: Adjunto): File =
        File(carpeta, "${huella(adjunto.url)}-${nombreSeguro(adjunto.nombre)}")

    fun yaDescargado(adjunto: Adjunto): File? =
        archivoDe(adjunto).takeIf { it.exists() && it.length() > 0 }

    suspend fun descargar(adjunto: Adjunto): File? = withContext(Dispatchers.IO) {
        yaDescargado(adjunto)?.let { return@withContext it }
        val url = repositorio.urlDescargable(adjunto) ?: return@withContext null
        val destino = archivoDe(adjunto)
        if (MoodleClient.descargarArchivo(url, destino)) destino else null
    }

    fun intentAbrir(archivo: File, tipo: String): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriDe(archivo), tipoUtil(tipo))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun intentCompartir(archivo: File, tipo: String): Intent {
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = tipoUtil(tipo)
            putExtra(Intent.EXTRA_STREAM, uriDe(archivo))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(envio, "Guardar o compartir")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun uriDe(archivo: File) =
        FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", archivo)

    /** Sin tipo MIME muchos visores descartan el archivo; el comodín deja elegir al usuario. */
    private fun tipoUtil(tipo: String) = tipo.ifBlank { "*/*" }

    private fun nombreSeguro(nombre: String): String =
        nombre.replace(Regex("[^A-Za-z0-9._-]"), "_").take(60).ifBlank { "adjunto" }

    private fun huella(url: String): String = Integer.toHexString(url.hashCode())
}
