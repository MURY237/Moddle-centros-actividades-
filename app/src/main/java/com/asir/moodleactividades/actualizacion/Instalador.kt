package com.asir.moodleactividades.actualizacion

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

object Instalador {

    const val NOMBRE_APK = "actualizacion.apk"

    /**
     * En su propia carpeta de la caché: el FileProvider solo comparte esa y la de adjuntos,
     * no la caché entera. Se borra de paso el APK que versiones anteriores dejaban en la raíz.
     */
    fun archivoDestino(contexto: Context): File {
        File(contexto.cacheDir, NOMBRE_APK).delete()
        val carpeta = File(contexto.cacheDir, "actualizacion").apply { mkdirs() }
        return File(carpeta, NOMBRE_APK)
    }

    fun puedeInstalar(contexto: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            contexto.packageManager.canRequestPackageInstalls()

    fun pedirPermisoDeInstalacion(contexto: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            contexto.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${contexto.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * El actualizador solo instala esta misma app. Si lo descargado dice ser otro paquete,
     * no se le pasa al instalador: que la firma sea la de siempre ya lo comprueba Android,
     * pero solo cuando el paquete coincide con uno instalado.
     */
    fun esEstaApp(contexto: Context, apk: File): Boolean {
        val paquete = runCatching {
            @Suppress("DEPRECATION")
            contexto.packageManager.getPackageArchiveInfo(apk.path, 0)?.packageName
        }.getOrNull()
        // Si el móvil no sabe leerlo, decide el instalador del sistema, que lo valida entero.
        return paquete == null || paquete == contexto.packageName
    }

    fun instalar(contexto: Context, apk: File): Boolean {
        if (!esEstaApp(contexto, apk)) {
            apk.delete()
            return false
        }
        return runCatching {
            val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", apk)
            contexto.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        }.getOrDefault(false)
    }
}
