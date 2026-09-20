package com.asir.moodleactividades.data.seneca

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive

object RespuestaJs {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * `evaluateJavascript` entrega el valor ya codificado como JSON, así que una cadena llega
     * entrecomillada y con las comillas de dentro escapadas. Hay que deshacer esa capa antes
     * de poder leer el objeto que devuelve el guion.
     */
    fun desenvolver(crudo: String?): String? {
        val texto = crudo?.trim().orEmpty()
        if (texto.isEmpty() || texto == "null") return null
        if (!texto.startsWith("\"")) return texto
        return runCatching { json.parseToJsonElement(texto).jsonPrimitive.content }.getOrNull()
    }

    fun leerExtraccion(crudo: String?): ResultadoExtraccion? {
        val contenido = desenvolver(crudo) ?: return null
        return runCatching { json.decodeFromString<ResultadoExtraccion>(contenido) }.getOrNull()
    }

    fun leerNavegacion(crudo: String?): ResultadoNavegacion? {
        val contenido = desenvolver(crudo) ?: return null
        return runCatching { json.decodeFromString<ResultadoNavegacion>(contenido) }.getOrNull()
    }
}
