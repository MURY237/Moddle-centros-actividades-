package com.asir.moodleactividades.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.asir.moodleactividades.data.ActividadesRepository
import com.asir.moodleactividades.data.CacheActividades
import com.asir.moodleactividades.data.SesionStore
import com.asir.moodleactividades.domain.Clasificador
import java.util.concurrent.TimeUnit

class RecordatoriosWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val contexto = applicationContext
        val sesionStore = SesionStore(contexto)
        if (sesionStore.leer() == null) return Result.success()

        val repositorio = ActividadesRepository(sesionStore, CacheActividades(contexto))
        val actividades = runCatching { repositorio.cargarActividades() }
            .getOrElse { return Result.retry() }

        val yaAvisadas = AvisosEnviados(contexto)
        Clasificador.porVencer(
            actividades,
            System.currentTimeMillis() / 1000,
            Recordatorios.VENTANA_AVISO_SEGUNDOS
        )
            .filterNot { yaAvisadas.contiene(it.id, it.fechaLimite) }
            .forEach { actividad ->
                Recordatorios.avisar(contexto, actividad)
                yaAvisadas.marcar(actividad.id, actividad.fechaLimite)
            }

        return Result.success()
    }

    companion object {
        private const val TRABAJO = "recordatorios-entregas"

        fun programar(contexto: Context) {
            val peticion = PeriodicWorkRequestBuilder<RecordatoriosWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(
                TRABAJO,
                ExistingPeriodicWorkPolicy.KEEP,
                peticion
            )
        }

        fun cancelar(contexto: Context) {
            WorkManager.getInstance(contexto).cancelUniqueWork(TRABAJO)
        }
    }
}

/** Recuerda qué se avisó para no repetir el mismo aviso en cada comprobación. */
class AvisosEnviados(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("avisos_enviados", Context.MODE_PRIVATE)

    fun contiene(id: Long, fechaLimite: Long?): Boolean =
        prefs.contains(clave(id, fechaLimite))

    fun marcar(id: Long, fechaLimite: Long?) {
        prefs.edit().putBoolean(clave(id, fechaLimite), true).apply()
    }

    // La fecha entra en la clave para que un plazo aplazado vuelva a avisar.
    private fun clave(id: Long, fechaLimite: Long?) = "$id-${fechaLimite ?: 0}"
}
