package com.asir.moodleactividades.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.asir.moodleactividades.MainActivity
import com.asir.moodleactividades.R
import com.asir.moodleactividades.domain.Actividad
import com.asir.moodleactividades.ui.textoRelativo

object Recordatorios {

    const val CANAL_ENTREGAS = "entregas"
    const val CANAL_NOVEDADES = "novedades"

    fun crearCanales(contexto: Context) {
        val gestor = NotificationManagerCompat.from(contexto)
        gestor.createNotificationChannel(
            NotificationChannel(
                CANAL_ENTREGAS,
                "Entregas próximas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avisos de las tareas cuyo plazo está a punto de terminar" }
        )
        gestor.createNotificationChannel(
            NotificationChannel(
                CANAL_NOVEDADES,
                "Actividades nuevas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avisos cuando un profesor publica una actividad" }
        )
    }

    fun puedeNotificar(contexto: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun avisarDeEntrega(contexto: Context, actividad: Actividad) {
        notificar(
            contexto = contexto,
            canal = CANAL_ENTREGAS,
            id = actividad.id.toInt(),
            titulo = actividad.nombre,
            texto = "${actividad.curso} · ${textoRelativo(actividad.fechaLimite)}"
        )
    }

    fun avisarDeNovedades(contexto: Context, nuevas: List<Actividad>) {
        if (nuevas.isEmpty()) return

        val (titulo, texto) = if (nuevas.size == 1) {
            val unica = nuevas.first()
            "Nueva actividad: ${unica.nombre}" to unica.curso
        } else {
            val asignaturas = nuevas.map { it.curso }.filter { it.isNotBlank() }.distinct()
            "${nuevas.size} actividades nuevas" to asignaturas.take(3).joinToString(", ")
        }

        notificar(contexto, CANAL_NOVEDADES, ID_NOVEDADES, titulo, texto)
    }

    private fun notificar(
        contexto: Context,
        canal: String,
        id: Int,
        titulo: String,
        texto: String
    ) {
        if (!puedeNotificar(contexto)) return

        val abrirApp = PendingIntent.getActivity(
            contexto,
            id,
            Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val aviso = NotificationCompat.Builder(contexto, canal)
            .setSmallIcon(R.drawable.ic_aviso)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(abrirApp)
            .build()

        runCatching { NotificationManagerCompat.from(contexto).notify(id, aviso) }
    }

    private const val ID_NOVEDADES = 90_001
}
