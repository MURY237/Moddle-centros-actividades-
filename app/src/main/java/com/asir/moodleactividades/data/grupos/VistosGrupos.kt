package com.asir.moodleactividades.data.grupos

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.asir.moodleactividades.notificaciones.Recordatorios

/**
 * Hasta qué mensaje y qué examen ha visto este móvil en cada grupo, para avisar solo de lo
 * nuevo. Lo que se lee con el chat abierto también cuenta como visto.
 */
class VistosGrupos(contexto: Context) {

    private val app = contexto.applicationContext
    private val prefs = app.getSharedPreferences("vistos_grupos", Context.MODE_PRIVATE)

    fun ultimoMensaje(grupo: String): Long? = leer("m-$grupo")

    fun ultimoExamen(grupo: String): Long? = leer("e-$grupo")

    fun marcarMensaje(grupo: String, id: Long) = marcar("m-$grupo", id)

    fun marcarExamen(grupo: String, id: Long) = marcar("e-$grupo", id)

    /** Al abrir el grupo, su aviso ya no hace falta en la barra. */
    fun abierto(grupo: String) {
        runCatching { NotificationManagerCompat.from(app).cancel(Recordatorios.CANAL_GRUPOS, grupo.hashCode()) }
    }

    fun borrar() = prefs.edit().clear().apply()

    private fun leer(clave: String): Long? = if (prefs.contains(clave)) prefs.getLong(clave, 0) else null

    /** Solo hacia delante: una consulta lenta que llega tarde no puede hacer olvidar lo visto. */
    private fun marcar(clave: String, id: Long) {
        if ((leer(clave) ?: -1L) < id) prefs.edit().putLong(clave, id).apply()
    }
}
