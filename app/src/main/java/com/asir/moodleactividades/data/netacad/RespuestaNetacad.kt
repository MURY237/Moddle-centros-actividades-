package com.asir.moodleactividades.data.netacad

import com.asir.moodleactividades.data.seneca.RespuestaJs
import kotlinx.serialization.json.Json

object RespuestaNetacad {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Mismo envoltorio que con Séneca: `evaluateJavascript` entrega la cadena ya entrecomillada. */
    fun leer(crudo: String?): ResultadoNetacad? {
        val contenido = RespuestaJs.desenvolver(crudo) ?: return null
        return runCatching { json.decodeFromString<ResultadoNetacad>(contenido) }.getOrNull()
    }

    fun leerAcceso(crudo: String?): ResultadoAccesoNetacad? {
        val contenido = RespuestaJs.desenvolver(crudo) ?: return null
        return runCatching { json.decodeFromString<ResultadoAccesoNetacad>(contenido) }.getOrNull()
    }
}
