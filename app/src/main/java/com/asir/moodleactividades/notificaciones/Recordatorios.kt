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

    const val CANAL = "entregas"
    const val VENTANA_AVISO_SEGUNDOS = 60L * 60 * 48

    fun crearCanal(contexto: Context) {
        val canal = NotificationChannel(
            CANAL,
            "Entregas próximas",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos de las tareas cuyo plazo está a punto de terminar"
        }
        NotificationManagerCompat.from(contexto).createNotificationChannel(canal)
    }

    fun puedeNotificar(contexto: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun avisar(contexto: Context, actividad: Actividad) {
        if (!puedeNotificar(contexto)) return

        val abrirApp = PendingIntent.getActivity(
            contexto,
            actividad.id.toInt(),
            Intent(contexto, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val aviso = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_aviso)
            .setContentTitle(actividad.nombre)
            .setContentText("${actividad.curso} · ${textoRelativo(actividad.fechaLimite)}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(abrirApp)
            .build()

        runCatching {
            NotificationManagerCompat.from(contexto).notify(actividad.id.toInt(), aviso)
        }
    }
}
