package com.asir.moodleactividades.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

data class HorarioGuardado(
    val archivo: File,
    val esPdf: Boolean,
    val nombre: String
)

/**
 * El horario se copia dentro de la app en lugar de recordar el Uri elegido: los permisos
 * sobre un documento externo se pierden y el archivo original puede moverse o borrarse.
 */
class AlmacenHorario(private val contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("horario", Context.MODE_PRIVATE)

    private val destino: File
        get() = File(contexto.applicationContext.filesDir, "horario.dat")

    fun guardar(origen: Uri): HorarioGuardado? = runCatching {
        val tipo = contexto.contentResolver.getType(origen).orEmpty()
        val nombre = nombreDe(origen)

        contexto.contentResolver.openInputStream(origen)?.use { entrada ->
            destino.outputStream().use { salida -> entrada.copyTo(salida) }
        } ?: return null

        val esPdf = tipo.contains("pdf", ignoreCase = true) ||
            nombre.endsWith(".pdf", ignoreCase = true)

        prefs.edit()
            .putBoolean(ES_PDF, esPdf)
            .putString(NOMBRE, nombre)
            .apply()

        HorarioGuardado(destino, esPdf, nombre)
    }.getOrNull()

    fun leer(): HorarioGuardado? {
        if (!destino.exists() || destino.length() == 0L) return null
        return HorarioGuardado(
            archivo = destino,
            esPdf = prefs.getBoolean(ES_PDF, false),
            nombre = prefs.getString(NOMBRE, "").orEmpty().ifBlank { "Mi horario" }
        )
    }

    fun borrar() {
        runCatching { destino.delete() }
        prefs.edit().clear().apply()
    }

    private fun nombreDe(uri: Uri): String = runCatching {
        contexto.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val columna = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columna >= 0 && cursor.moveToFirst()) cursor.getString(columna) else null
        }
    }.getOrNull().orEmpty().ifBlank { uri.lastPathSegment.orEmpty() }

    private companion object {
        const val ES_PDF = "es_pdf"
        const val NOMBRE = "nombre"
    }
}
