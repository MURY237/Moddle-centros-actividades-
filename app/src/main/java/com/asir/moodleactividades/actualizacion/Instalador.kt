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

    fun archivoDestino(contexto: Context) = File(contexto.cacheDir, NOMBRE_APK)

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

    fun instalar(contexto: Context, apk: File): Boolean = runCatching {
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
