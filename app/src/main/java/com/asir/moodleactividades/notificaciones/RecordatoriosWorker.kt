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
import com.asir.moodleactividades.data.AjustesAvisos
import com.asir.moodleactividades.data.CacheActividades
import com.asir.moodleactividades.data.CacheCalificaciones
import com.asir.moodleactividades.data.PreferenciasAvisos
import com.asir.moodleactividades.data.SesionStore
import com.asir.moodleactividades.domain.Actividad
import com.asir.moodleactividades.domain.Clasificador
import com.asir.moodleactividades.domain.ComparadorNotas
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RecordatoriosWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val contexto = applicationContext
        val sesionStore = SesionStore(contexto)
        if (sesionStore.leer() == null) return Result.success()

        val ajustes = PreferenciasAvisos(contexto).leer()
        if (!ajustes.avisarEntregas && !ajustes.avisarNuevas && !ajustes.avisarNotas) {
            return Result.success()
        }

        val cache = CacheActividades(contexto)
        val cacheNotas = CacheCalificaciones(contexto)
        // Las dos copias se leen antes de consultar: la consulta las sobrescribe.
        val anteriores = cache.leer()?.actividades.orEmpty()
        val notasAnteriores = cacheNotas.leer()?.cursos

        val repositorio = ActividadesRepository(sesionStore, cache, cacheNotas)
        val actuales = runCatching { repositorio.cargarActividades() }
            .getOrElse { return Result.retry() }

        if (ajustes.avisarNuevas && anteriores.isNotEmpty()) {
            Recordatorios.avisarDeNovedades(contexto, reciénPublicadas(anteriores, actuales))
        }

        if (ajustes.avisarEntregas) {
            avisarDeEntregasProximas(contexto, actuales, ajustes)
        }

        if (ajustes.avisarNotas) {
            // Que falle el libro de calificaciones no invalida el resto de la comprobación.
            runCatching { repositorio.cargarCalificaciones() }.getOrNull()?.let { notasActuales ->
                if (notasAnteriores != null) {
                    Recordatorios.avisarDeNotas(
                        contexto,
                        ComparadorNotas.recienPublicadas(notasAnteriores, notasActuales)
                    )
                }
            }
        }

        return Result.success()
    }

    /** Lo que no estaba en la carga anterior. Con la caché vacía no hay con qué comparar. */
    private fun reciénPublicadas(
        anteriores: List<Actividad>,
        actuales: List<Actividad>
    ): List<Actividad> {
        val conocidas = anteriores.mapTo(mutableSetOf()) { "${it.tipo.name}-${it.id}" }
        return actuales.filterNot { "${it.tipo.name}-${it.id}" in conocidas }
    }

    private fun avisarDeEntregasProximas(
        contexto: Context,
        actividades: List<Actividad>,
        ajustes: AjustesAvisos
    ) {
        val yaAvisadas = AvisosEnviados(contexto)
        Clasificador.porVencer(
            actividades,
            System.currentTimeMillis() / 1000,
            ajustes.antelacionSegundos
        )
            .filterNot { yaAvisadas.contiene(it.id, it.fechaLimite) }
            .forEach { actividad ->
                Recordatorios.avisarDeEntrega(contexto, actividad)
                yaAvisadas.marcar(actividad.id, actividad.fechaLimite)
            }
    }

    companion object {
        private const val TRABAJO = "recordatorios-entregas"

        fun programar(contexto: Context, ajustes: AjustesAvisos = PreferenciasAvisos(contexto).leer()) {
            if (!ajustes.avisarEntregas && !ajustes.avisarNuevas && !ajustes.avisarNotas) {
                cancelar(contexto)
                return
            }

            val peticion = PeriodicWorkRequestBuilder<RecordatoriosWorker>(
                ajustes.horasEntreComprobaciones,
                TimeUnit.HOURS
            )
                .setInitialDelay(minutosHastaLaHora(ajustes.horaPreferida), TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(
                TRABAJO,
                // Al cambiar los ajustes hay que rehacer el trabajo, no conservar el anterior.
                ExistingPeriodicWorkPolicy.UPDATE,
                peticion
            )
        }

        fun cancelar(contexto: Context) {
            WorkManager.getInstance(contexto).cancelUniqueWork(TRABAJO)
        }

        /** Alinea la primera comprobación con la hora elegida; las siguientes van por periodo. */
        private fun minutosHastaLaHora(hora: Int): Long {
            val ahora = Calendar.getInstance()
            val objetivo = (ahora.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, hora.coerceIn(0, 23))
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(ahora)) add(Calendar.DAY_OF_YEAR, 1)
            }
            return (objetivo.timeInMillis - ahora.timeInMillis) / 60_000
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
