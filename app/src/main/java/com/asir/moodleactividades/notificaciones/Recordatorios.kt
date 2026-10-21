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
import com.asir.moodleactividades.data.Aviso
import com.asir.moodleactividades.data.HistorialAvisos
import com.asir.moodleactividades.data.TipoAviso
import com.asir.moodleactividades.domain.Actividad
import com.asir.moodleactividades.domain.Calificacion
import com.asir.moodleactividades.domain.Examen
import com.asir.moodleactividades.domain.Grupo
import com.asir.moodleactividades.domain.NovedadesGrupo
import com.asir.moodleactividades.ui.textoRelativo

object Recordatorios {

    const val CANAL_ENTREGAS = "entregas"
    const val CANAL_NOVEDADES = "novedades"
    const val CANAL_NOTAS = "notas"
    const val CANAL_GRUPOS = "grupos"

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
        gestor.createNotificationChannel(
            NotificationChannel(
                CANAL_NOTAS,
                "Notas publicadas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avisos cuando un profesor califica una tarea o un examen" }
        )
        // Un mensaje se quiere ver al momento: este canal suena y asoma por arriba.
        gestor.createNotificationChannel(
            NotificationChannel(
                CANAL_GRUPOS,
                "Grupos: chat y exámenes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Mensajes y exámenes nuevos de tus grupos" }
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
            tipo = TipoAviso.ENTREGA,
            id = actividad.id.hashCode(),
            titulo = actividad.nombre,
            texto = "${actividad.curso} · ${textoRelativo(actividad.fechaLimite)}",
            url = actividad.url
        )
    }

    /**
     * Una notificación por nota, no una agrupada: lo que se quiere saber de un vistazo es
     * qué han corregido y con qué nota, y eso no cabe en un resumen.
     */
    fun avisarDeNotas(contexto: Context, publicadas: List<Calificacion>) {
        publicadas.take(MAX_NOTAS_POR_VEZ).forEach { calificacion ->
            notificar(
                contexto = contexto,
                canal = CANAL_NOTAS,
                tipo = TipoAviso.NOTA,
                // Estable entre comprobaciones, para que la misma nota no se duplique.
                id = (calificacion.curso + "|" + calificacion.nombre).hashCode(),
                titulo = "Nota publicada: ${calificacion.nota}",
                texto = "${calificacion.nombre} · ${calificacion.curso}",
                url = calificacion.url
            )
        }
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

        notificar(
            contexto = contexto,
            canal = CANAL_NOVEDADES,
            tipo = TipoAviso.NUEVA,
            id = 1,
            titulo = titulo,
            texto = texto,
            url = nuevas.singleOrNull()?.url
        )
    }

    /** Una notificación por grupo, con los últimos mensajes; la siguiente la sustituye. */
    fun avisarDeMensajes(contexto: Context, grupo: Grupo, mensajes: List<Pair<String, String>>) {
        if (mensajes.isEmpty() || !puedeNotificar(contexto)) return
        val lineas = mensajes.takeLast(MAX_LINEAS_CHAT).map { (apodo, texto) -> "$apodo: $texto" }
        val texto = if (mensajes.size == 1) lineas.first() else "${mensajes.size} mensajes nuevos"
        val estilo = NotificationCompat.InboxStyle().setBigContentTitle(grupo.nombre)
        lineas.forEach { estilo.addLine(it) }
        mostrar(
            contexto,
            NotificationCompat.Builder(contexto, CANAL_GRUPOS)
                .setContentTitle(grupo.nombre)
                .setContentText(texto)
                .setStyle(estilo)
                .setNumber(mensajes.size)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(abrir(contexto, "GRUPOS", CODIGO_GRUPOS)),
            CANAL_GRUPOS,
            grupo.id.hashCode()
        )
    }

    fun avisarDeExamen(contexto: Context, grupo: Grupo, examen: Examen) {
        if (!puedeNotificar(contexto)) return
        val texto = NovedadesGrupo.cuando(examen) + " · " + grupo.nombre
        mostrar(
            contexto,
            NotificationCompat.Builder(contexto, CANAL_GRUPOS)
                .setContentTitle("Examen: ${examen.asignatura}")
                .setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(abrir(contexto, "GRUPOS", CODIGO_GRUPOS)),
            CANAL_GRUPOS,
            ("examen-" + examen.id).hashCode()
        )
    }

    private fun mostrar(contexto: Context, aviso: NotificationCompat.Builder, canal: String, id: Int) {
        aviso.setSmallIcon(R.drawable.ic_aviso)
            .setColor(COLOR_MARCA)
            .setAutoCancel(true)
        runCatching { NotificationManagerCompat.from(contexto).notify(canal, id, aviso.build()) }
    }

    /** Tocar el aviso lleva a la pestaña de lo que cuenta. */
    private fun abrir(contexto: Context, seccion: String, codigo: Int): PendingIntent =
        PendingIntent.getActivity(
            contexto,
            codigo,
            Intent(contexto, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_SECCION, seccion),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun notificar(
        contexto: Context,
        canal: String,
        tipo: TipoAviso,
        id: Int,
        titulo: String,
        texto: String,
        url: String? = null
    ) {
        // El historial se guarda aunque Android tenga las notificaciones bloqueadas: dentro de
        // la app el aviso sigue teniendo sentido.
        HistorialAvisos(contexto).anadir(
            Aviso(
                momento = System.currentTimeMillis() / 1000,
                tipo = tipo,
                titulo = titulo,
                texto = texto,
                url = url
            )
        )

        if (!puedeNotificar(contexto)) return

        // Tocar el aviso lleva a donde está lo que cuenta: una nota a Notas, una entrega a
        // Tareas. Un PendingIntent por tipo basta, porque solo cambia la sección.
        val abrirApp = abrir(contexto, if (tipo == TipoAviso.NOTA) "NOTAS" else "ACTIVIDADES", tipo.ordinal)

        val aviso = NotificationCompat.Builder(contexto, canal)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(abrirApp)

        // La etiqueta es el canal: así el número de una tarea no puede pisar el de una nota.
        mostrar(contexto, aviso, canal, id)
    }

    private const val MAX_NOTAS_POR_VEZ = 5
    private const val MAX_LINEAS_CHAT = 5

    /** Lejos de los ordinales de [TipoAviso], que también son códigos de PendingIntent. */
    private const val CODIGO_GRUPOS = 100

    /** El azul de la app, para el icono pequeño de la barra de estado. */
    private val COLOR_MARCA = 0xFF1F5FD1.toInt()
}
