package com.asir.moodleactividades.data

import android.content.Context

/**
 * Las actividades que el alumno ha dado por entregadas a mano, por su clave. Sirve para los
 * exámenes y demás actividades sin nada que subir, que Moodle deja como «no entregadas» para
 * siempre. Solo vive en este móvil.
 */
class EntregasManuales(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("entregas_manuales", Context.MODE_PRIVATE)

    fun leer(): Set<String> = prefs.getStringSet(CLAVE, null)?.toSet().orEmpty()

    fun cambiar(clave: String, entregada: Boolean): Set<String> {
        val todas = if (entregada) leer() + clave else leer() - clave
        // Siempre un conjunto nuevo: Android no garantiza guardar el que devolvió al leer.
        prefs.edit().putStringSet(CLAVE, HashSet(todas)).apply()
        return todas
    }

    fun borrar() = prefs.edit().remove(CLAVE).apply()

    private companion object {
        const val CLAVE = "claves"
    }
}
