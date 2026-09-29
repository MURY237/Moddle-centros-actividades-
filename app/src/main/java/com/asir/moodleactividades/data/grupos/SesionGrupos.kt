package com.asir.moodleactividades.data.grupos

import kotlinx.serialization.Serializable

/**
 * La identidad del móvil en los grupos. Es una cuenta anónima de Supabase: no lleva correo
 * ni nombre, solo un identificador. Perderla es perder los grupos —habría que volver a
 * entrar con los códigos—, así que se guarda y se renueva con cuidado.
 */
@Serializable
data class SesionAuth(
    val acceso: String,
    val refresco: String,
    /** Segundos desde época en que caduca [acceso]. */
    val expira: Long,
    val usuario: String
)

interface GuardaSesion {
    fun leer(): SesionAuth?
    fun guardar(sesion: SesionAuth)
    fun borrar()
}
