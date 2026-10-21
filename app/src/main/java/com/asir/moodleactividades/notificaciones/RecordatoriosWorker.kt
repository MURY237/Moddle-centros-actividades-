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
import com.asir.moodleactividades.data.EntregasManuales
import com.asir.moodleactividades.data.PreferenciasAvisos
import com.asir.moodleactividades.data.SesionStore
import com.asir.moodleactividades.domain.Actividad
import com.asir.moodleactividades.domain.Clasificador
import com.asir.moodleactividades.domain.ComparadorNotas
import com.asir.moodleactividades.domain.Programacion
import java.time.LocalDateTime
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
            // Lo que el alumno ha dado por entregado no se le recuerda.
            val marcadas = EntregasManuales(contexto).leer()
            avisarDeEntregasProximas(contexto, Clasificador.aplicarMarcas(actuales, marcadas), ajustes)
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
        yaAvisadas.olvidarVencidos(System.currentTimeMillis() / 1000)
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

        /**
         * Programa las comprobaciones de Moodle y las de los grupos. [reprogramar] solo al
         * cambiar los ajustes: al abrir la app se respeta lo programado, porque rehacerlo cada
         * vez volvía a retrasar la primera comprobación y los avisos no llegaban nunca.
         */
        fun programar(
            contexto: Context,
            ajustes: AjustesAvisos = PreferenciasAvisos(contexto).leer(),
            reprogramar: Boolean = false
        ) {
            GruposWorker.programar(contexto, ajustes, reprogramar)
            if (!ajustes.avisarEntregas && !ajustes.avisarNuevas && !ajustes.avisarNotas) {
                cancelar(contexto)
                return
            }

            val peticion = PeriodicWorkRequestBuilder<RecordatoriosWorker>(
                ajustes.horasEntreComprobaciones,
                TimeUnit.HOURS
            )
                .setInitialDelay(
                    Programacion.minutosHastaLaProxima(
                        LocalDateTime.now(),
                        ajustes.horaPreferida,
                        ajustes.horasEntreComprobaciones
                    ),
                    TimeUnit.MINUTES
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(
                TRABAJO,
                // Al cambiar los ajustes hay que rehacer el trabajo, no conservar el anterior.
                if (reprogramar) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
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

    /**
     * Lo avisado de plazos que vencieron hace más de un mes ya no se va a volver a mirar.
     * Sin esto la lista crecía con cada entrega de cada curso y no se vaciaba nunca.
     */
    fun olvidarVencidos(ahora: Long) {
        val corte = ahora - 30L * 24 * 3600
        val viejas = prefs.all.keys.filter { clave ->
            val fecha = clave.substringAfterLast('-').toLongOrNull()
            fecha != null && fecha in 1 until corte
        }
        if (viejas.isEmpty()) return
        prefs.edit().apply { viejas.forEach { remove(it) } }.apply()
    }

    fun borrarTodo() = prefs.edit().clear().apply()

    // La fecha entra en la clave para que un plazo aplazado vuelva a avisar.
    private fun clave(id: Long, fechaLimite: Long?) = "$id-${fechaLimite ?: 0}"
}
