package com.asir.moodleactividades.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class TipoAviso(val etiqueta: String) {
    ENTREGA("Entrega cercana"),
    NUEVA("Actividad nueva"),
    NOTA("Nota publicada")
}

@Serializable
data class Aviso(
    val momento: Long,
    val tipo: TipoAviso,
    val titulo: String,
    val texto: String,
    val url: String? = null
)

/**
 * Las notificaciones de Android se descartan de un barrido y no hay forma de recuperarlas.
 * Aquí quedan guardadas para poder consultarlas después desde la propia aplicación.
 */
class HistorialAvisos(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("historial_avisos", Context.MODE_PRIVATE)

    fun anadir(aviso: Aviso) {
        val actuales = leer()
        // El mismo aviso repetido en comprobaciones seguidas no aporta nada, pero pasado un
        // día sí: puede ser una entrega aplazada o una nota que el profesor ha rectificado.
        val repetido = actuales.any {
            it.tipo == aviso.tipo &&
                it.titulo == aviso.titulo &&
                it.texto == aviso.texto &&
                aviso.momento - it.momento < VENTANA_REPETICION
        }
        if (repetido) return
        guardar(listOf(aviso) + actuales)
    }

    fun leer(): List<Aviso> {
        val guardado = prefs.getString(LISTA, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Aviso>>(guardado) }.getOrDefault(emptyList())
    }

    fun sinLeer(): Int {
        val corte = prefs.getLong(LEIDOS_HASTA, 0)
        return leer().count { it.momento > corte }
    }

    fun marcarLeidos() {
        prefs.edit().putLong(LEIDOS_HASTA, System.currentTimeMillis() / 1000).apply()
    }

    fun borrar() = prefs.edit().remove(LISTA).apply()

    /** Para que la lista abierta en pantalla se entere de lo que escribe el trabajo de fondo. */
    fun escuchar(alCambiar: () -> Unit): SharedPreferences.OnSharedPreferenceChangeListener {
        val oyente = SharedPreferences.OnSharedPreferenceChangeListener { _, clave ->
            if (clave == LISTA) alCambiar()
        }
        prefs.registerOnSharedPreferenceChangeListener(oyente)
        return oyente
    }

    fun dejarDeEscuchar(oyente: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(oyente)
    }

    private fun guardar(avisos: List<Aviso>) {
        // Sin tope, el historial crecería sin fin dentro de las preferencias.
        val recortado = avisos.take(MAXIMO)
        val serializado = runCatching { json.encodeToString(recortado) }.getOrNull() ?: return
        prefs.edit().putString(LISTA, serializado).apply()
    }

    private companion object {
        const val LISTA = "lista"
        const val LEIDOS_HASTA = "leidos_hasta"
        const val MAXIMO = 100
        const val VENTANA_REPETICION = 24 * 3600L
        val json = Json { ignoreUnknownKeys = true }
    }
}
