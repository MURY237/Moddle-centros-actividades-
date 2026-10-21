package com.asir.moodleactividades.notificaciones

import com.asir.moodleactividades.AppActividades
import com.asir.moodleactividades.data.PreferenciasAvisos
import com.asir.moodleactividades.data.grupos.VistosGrupos
import com.asir.moodleactividades.domain.Examen
import com.asir.moodleactividades.domain.Grupo
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Recibe los avisos que manda la función «avisar» del servidor de grupos y los pinta como
 * el resto de avisos de la app. Llegan solo con datos, así que siempre pasan por aquí, con
 * la app abierta o cerrada.
 */
class ServicioEmpuje : FirebaseMessagingService() {

    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val dependencias = (application as? AppActividades)?.dependencias ?: return
        ambito.launch { runCatching { Empuje.registrar(applicationContext, dependencias, token) } }
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        val datos = mensaje.data
        if (!PreferenciasAvisos(this).leer().avisarGrupos) return
        val grupoId = datos["grupo"] ?: return
        val id = datos["id"]?.toLongOrNull() ?: return
        val grupo = Grupo(id = grupoId, nombre = datos["grupoNombre"].orEmpty().ifBlank { "Grupo" }, codigo = "", creador = "")
        val apodo = datos["apodo"].orEmpty().ifBlank { "Alguien" }
        val vistos = VistosGrupos(this)

        when (datos["tipo"]) {
            "mensaje" -> {
                // Ya leído con el chat abierto, o ya avisado por la comprobación periódica.
                if ((vistos.ultimoMensaje(grupoId) ?: -1L) >= id) return
                vistos.marcarMensaje(grupoId, id)
                if (VistosGrupos.enPantalla == grupoId) return
                val pendientes = vistos.acumular(grupoId, listOf(apodo + ": " + datos["texto"].orEmpty()))
                Recordatorios.avisarDeMensajes(this, grupo, pendientes)
            }

            "examen" -> {
                if ((vistos.ultimoExamen(grupoId) ?: -1L) >= id) return
                vistos.marcarExamen(grupoId, id)
                val examen = Examen(
                    id = id,
                    grupo = grupoId,
                    autor = "",
                    asignatura = datos["asignatura"].orEmpty(),
                    fecha = datos["fecha"].orEmpty(),
                    hora = datos["hora"]?.ifBlank { null }
                )
                Recordatorios.avisarDeExamen(this, grupo, examen)
            }
        }
    }
}
