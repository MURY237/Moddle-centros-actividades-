package com.asir.moodleactividades.data.grupos

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.asir.moodleactividades.notificaciones.Recordatorios
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

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

    /**
     * Las líneas del aviso del grupo que aún no se ha abierto, con las nuevas al final: así
     * el aviso enseña todo lo que ha llegado y no solo el último mensaje.
     */
    fun acumular(grupo: String, nuevas: List<String>): List<String> {
        val todas = (pendientes(grupo) + nuevas).takeLast(MAX_PENDIENTES)
        prefs.edit().putString("p-$grupo", json.encodeToString(LISTA, todas)).apply()
        return todas
    }

    /** Al abrir el grupo, su aviso ya no hace falta en la barra. */
    fun abierto(grupo: String) {
        prefs.edit().remove("p-$grupo").apply()
        runCatching { NotificationManagerCompat.from(app).cancel(Recordatorios.CANAL_GRUPOS, grupo.hashCode()) }
    }

    fun borrar() = prefs.edit().clear().apply()

    private fun pendientes(grupo: String): List<String> =
        prefs.getString("p-$grupo", null)
            ?.let { runCatching { json.decodeFromString(LISTA, it) }.getOrNull() }
            .orEmpty()

    private fun leer(clave: String): Long? = if (prefs.contains(clave)) prefs.getLong(clave, 0) else null

    /** Solo hacia delante: una consulta lenta que llega tarde no puede hacer olvidar lo visto. */
    private fun marcar(clave: String, id: Long) {
        if ((leer(clave) ?: -1L) < id) prefs.edit().putLong(clave, id).apply()
    }

    companion object {
        private const val MAX_PENDIENTES = 50
        private val LISTA = ListSerializer(String.serializer())
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * El grupo cuyo chat está ahora mismo en pantalla, con la app delante. Sus mensajes ya
         * se están viendo: no hace falta avisar de ellos.
         */
        @Volatile
        var enPantalla: String? = null
    }
}
