package com.asir.moodleactividades.notificaciones

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.asir.moodleactividades.AppActividades
import com.asir.moodleactividades.data.AjustesAvisos
import com.asir.moodleactividades.data.PreferenciasAvisos
import com.asir.moodleactividades.data.grupos.RepositorioGrupos
import com.asir.moodleactividades.data.grupos.VistosGrupos
import com.asir.moodleactividades.domain.Chat
import com.asir.moodleactividades.domain.Grupo
import com.asir.moodleactividades.domain.NovedadesGrupo
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

/**
 * Mira los grupos con la app cerrada y avisa de los mensajes y exámenes nuevos. Sin servidor
 * de notificaciones propio, es lo más rápido que permite Android: una vez cada 15 minutos.
 */
class GruposWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val contexto = applicationContext
        if (!PreferenciasAvisos(contexto).leer().avisarGrupos) return Result.success()
        val dependencias = (contexto as? AppActividades)?.dependencias ?: return Result.success()
        // Sin identidad guardada este móvil no está en ningún grupo, y preguntar crearía una.
        if (!dependencias.hayIdentidadGrupos()) return Result.success()
        val repositorio = dependencias.repositorioGrupos ?: return Result.success()
        val vistos = VistosGrupos(contexto)

        return try {
            val yo = repositorio.yo()
            repositorio.misGrupos().forEach { grupo ->
                // Que falle un grupo no deja sin avisos a los demás.
                runCatching { revisar(contexto, repositorio, vistos, grupo, yo) }
                    .onFailure { if (it is CancellationException) throw it }
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Dentro de 15 minutos se vuelve a mirar: reintentar antes no aporta nada.
            Result.success()
        }
    }

    private suspend fun revisar(
        contexto: Context,
        repositorio: RepositorioGrupos,
        vistos: VistosGrupos,
        grupo: Grupo,
        yo: String
    ) {
        val ultimoMensaje = vistos.ultimoMensaje(grupo.id)
        val mensajes = repositorio.mensajes(grupo.id, ultimoMensaje)
        vistos.marcarMensaje(grupo.id, NovedadesGrupo.ultimo(ultimoMensaje, mensajes.map { it.id }))
        val nuevos = NovedadesGrupo.mensajes(mensajes, ultimoMensaje, yo)
        if (nuevos.isNotEmpty()) {
            val miembros = runCatching { repositorio.miembros(grupo.id) }.getOrDefault(emptyList())
            Recordatorios.avisarDeMensajes(
                contexto,
                grupo,
                nuevos.map { Chat.apodoDe(it.autor, miembros) to it.texto }
            )
        }

        val ultimoExamen = vistos.ultimoExamen(grupo.id)
        val examenes = repositorio.examenes(grupo.id)
        vistos.marcarExamen(grupo.id, NovedadesGrupo.ultimo(ultimoExamen, examenes.map { it.id }))
        NovedadesGrupo.examenes(examenes, ultimoExamen, yo).forEach { examen ->
            Recordatorios.avisarDeExamen(contexto, grupo, examen)
        }
    }

    companion object {
        private const val TRABAJO = "avisos-grupos"

        /** [reprogramar] solo al cambiar los ajustes: al abrir la app se respeta lo que hay. */
        fun programar(contexto: Context, ajustes: AjustesAvisos, reprogramar: Boolean = false) {
            if (!ajustes.avisarGrupos) {
                WorkManager.getInstance(contexto).cancelUniqueWork(TRABAJO)
                return
            }
            val peticion = PeriodicWorkRequestBuilder<GruposWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(
                TRABAJO,
                if (reprogramar) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
                peticion
            )
        }
    }
}
