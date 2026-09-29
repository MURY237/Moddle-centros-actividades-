package com.asir.moodleactividades.data.grupos

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Un fallo con un mensaje que se puede enseñar tal cual al alumno. */
class ErrorGrupos(mensaje: String) : Exception(mensaje)

object TraductorErrores {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * El servidor contesta con los códigos que lanza esquema.sql («codigo_no_valido»…) o
     * con los de PostgREST y Supabase. Aquí se convierten en algo que se entienda.
     */
    fun traducir(estado: Int, cuerpo: String): ErrorGrupos {
        val detalle = runCatching {
            val objeto = json.parseToJsonElement(cuerpo).jsonObject
            listOf("message", "msg", "error_description", "error")
                .firstNotNullOfOrNull { objeto[it]?.jsonPrimitive?.content }
        }.getOrNull().orEmpty()

        val clave = detalle.lowercase()
        val texto = when {
            "codigo_no_valido" in clave -> "Ese código no corresponde a ningún grupo."
            "apodo_no_valido" in clave -> "El apodo tiene que tener entre 1 y 30 caracteres."
            "nombre_no_valido" in clave -> "El nombre del grupo tiene que tener entre 1 y 60 caracteres."
            "grupo_lleno" in clave -> "El grupo ya tiene 100 miembros."
            "demasiados_grupos" in clave -> "Ya has creado 20 grupos, que es el máximo."
            "solo_creador" in clave -> "Eso solo lo puede hacer quien lleva el grupo."
            "no_a_ti_mismo" in clave -> "No puedes expulsarte a ti: usa «Salir del grupo»."
            "sin_sesion" in clave -> "No se pudo iniciar la sesión de grupos."
            "anonymous sign-ins are disabled" in clave ->
                "El servidor no tiene activado el acceso anónimo. Hay que activarlo en " +
                    "Supabase: Authentication → Sign In / Providers."
            "row-level security" in clave || estado == 403 ->
                "No tienes permiso para eso. Puede que ya no estés en el grupo."
            "check constraint" in clave -> "Algún dato es demasiado largo o está vacío."
            estado == 404 -> "El servidor de grupos no tiene las tablas. Falta ejecutar esquema.sql."
            estado == 429 -> "Demasiadas peticiones seguidas. Espera un momento."
            estado >= 500 -> "El servidor de grupos no responde bien ahora mismo."
            else -> "No se pudo completar ($estado)."
        }
        return ErrorGrupos(texto)
    }

    fun sinConexion() = ErrorGrupos("Sin conexión con el servidor de grupos.")
}
