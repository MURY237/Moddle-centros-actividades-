package com.asir.moodleactividades.data

import com.asir.moodleactividades.data.net.AssignmentsDto
import com.asir.moodleactividades.data.net.EstadoEntregaDto
import com.asir.moodleactividades.data.net.EventosCalendarioDto
import com.asir.moodleactividades.data.net.MoodleClient
import com.asir.moodleactividades.data.net.MoodleException
import com.asir.moodleactividades.data.net.NotasCursoDto
import com.asir.moodleactividades.data.net.ResultadoSso
import com.asir.moodleactividades.data.net.SiteInfoDto
import com.asir.moodleactividades.data.net.SsoLogin
import com.asir.moodleactividades.data.net.decodificar
import com.asir.moodleactividades.domain.Actividad
import com.asir.moodleactividades.domain.Clasificador
import com.asir.moodleactividades.domain.EstadoActividad
import com.asir.moodleactividades.domain.TipoActividad
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ActividadesRepository(
    private val sesionStore: SesionStore,
    private val cache: CacheActividades
) {

    fun sesionGuardada(): Sesion? = sesionStore.leer()

    fun ultimaUrl(): String = sesionStore.ultimaUrl()

    fun instantaneaGuardada(): Instantanea? = cache.leer()

    fun cerrarSesion() {
        sesionStore.borrar()
        cache.borrar()
    }

    suspend fun iniciarSesion(url: String, usuario: String, contrasena: String): Sesion {
        val sitio = MoodleClient.normalizarUrl(url)
        val token = MoodleClient(sitio).pedirToken(usuario, contrasena)
        return completarSesion(sitio, token)
    }

    suspend fun iniciarSesionConToken(url: String, token: String): Sesion {
        val sitio = MoodleClient.normalizarUrl(url)
        return completarSesion(sitio, token.trim())
    }

    fun prepararSso(url: String): String {
        val sitio = MoodleClient.normalizarUrl(url)
        val passport = SsoLogin.generarPassport()
        sesionStore.guardarSsoPendiente(sitio, passport)
        return SsoLogin.urlDeLanzamiento(sitio, passport)
    }

    suspend fun completarSso(enlace: String): Sesion {
        val (sitio, passport) = sesionStore.ssoPendiente()
            ?: throw MoodleException(null, "No hay ningún acceso en curso. Vuelve a intentarlo.")

        return when (val resultado = SsoLogin.extraerToken(enlace, sitio, passport)) {
            is ResultadoSso.Ok -> completarSesion(sitio, resultado.token)
                .also { sesionStore.limpiarSsoPendiente() }

            is ResultadoSso.Error -> throw MoodleException(null, resultado.mensaje)
        }
    }

    private suspend fun completarSesion(sitio: String, token: String): Sesion {
        val cliente = MoodleClient(sitio, token)
        val info: SiteInfoDto = cliente.decodificar(cliente.invocar("core_webservice_get_site_info"))
        val sesion = Sesion(
            urlSitio = sitio,
            token = token,
            usuario = info.username,
            nombreCompleto = info.fullname.ifBlank { info.username },
            nombreSitio = info.sitename
        )
        sesionStore.guardar(sesion)
        return sesion
    }

    suspend fun cargarActividades(ahora: Long = System.currentTimeMillis() / 1000): List<Actividad> {
        val sesion = sesionStore.leer() ?: throw MoodleException(null, "No hay ninguna sesión iniciada.")
        val cliente = MoodleClient(sesion.urlSitio, sesion.token)

        val actividades = cargarTareas(cliente, ahora) + cargarEventosNoTarea(cliente, ahora)
        cache.guardar(actividades, ahora)
        return actividades
    }

    private suspend fun cargarTareas(cliente: MoodleClient, ahora: Long): List<Actividad> = coroutineScope {
        val respuesta: AssignmentsDto = cliente.decodificar(cliente.invocar("mod_assign_get_assignments"))
        val limitador = Semaphore(MAX_PETICIONES_SIMULTANEAS)

        val notas = async {
            respuesta.courses.map { curso ->
                async { limitador.withPermit { consultarNotas(cliente, curso.id) } }
            }.awaitAll().fold(emptyMap<Long, String>()) { acumulado, parcial -> acumulado + parcial }
        }

        val actividades = respuesta.courses.flatMap { curso ->
            curso.assignments.map { tarea -> curso to tarea }
        }.map { (curso, tarea) ->
            async {
                val estadoEntrega = limitador.withPermit { consultarEntrega(cliente, tarea.id) }
                val limite = tarea.duedate.takeIf { it > 0 }
                Actividad(
                    id = tarea.id,
                    nombre = tarea.name,
                    curso = curso.fullname.ifBlank { curso.shortname },
                    tipo = TipoActividad.TAREA,
                    fechaLimite = limite,
                    estado = Clasificador.estado(estadoEntrega?.estado, limite, ahora),
                    calificada = estadoEntrega?.calificada == true,
                    url = "${cliente.urlSitio}mod/assign/view.php?id=${tarea.cmid}"
                )
            }
        }.awaitAll()

        val notasPorTarea = notas.await()
        actividades.map { it.copy(nota = notasPorTarea[it.id]) }
    }

    /** Devuelve la nota de cada tarea del curso, indexada por el id de la tarea. */
    private suspend fun consultarNotas(cliente: MoodleClient, idCurso: Long): Map<Long, String> =
        runCatching {
            val dto: NotasCursoDto = cliente.decodificar(
                cliente.invocar(
                    "gradereport_user_get_grade_items",
                    mapOf("courseid" to idCurso.toString())
                )
            )
            dto.usergrades
                .flatMap { it.gradeitems }
                .filter { it.itemmodule == "assign" && it.gradeformatted.esNotaReal() }
                .associate { it.iteminstance to it.gradeformatted }
        }.getOrDefault(emptyMap())

    private fun String.esNotaReal(): Boolean =
        isNotBlank() && this != "-" && !equals("Error", ignoreCase = true)

    private suspend fun consultarEntrega(cliente: MoodleClient, idTarea: Long): EntregaResumen? =
        runCatching {
            val dto: EstadoEntregaDto = cliente.decodificar(
                cliente.invocar("mod_assign_get_submission_status", mapOf("assignid" to idTarea.toString()))
            )
            val intento = dto.lastattempt
            val entrega = intento?.submission ?: intento?.teamsubmission
            EntregaResumen(
                estado = entrega?.status,
                calificada = intento?.graded == true || entrega?.gradingstatus == "graded"
            )
        }.getOrNull()

    private suspend fun cargarEventosNoTarea(cliente: MoodleClient, ahora: Long): List<Actividad> = runCatching {
        val dto: EventosCalendarioDto = cliente.decodificar(
            cliente.invocar(
                "core_calendar_get_action_events_by_timesort",
                mapOf(
                    "timesortfrom" to (ahora - VENTANA_PASADA).toString(),
                    "timesortto" to (ahora + VENTANA_FUTURA).toString(),
                    "limitnum" to "50"
                )
            )
        )
        dto.events
            .filter { it.modulename != "assign" }
            .map { evento ->
                val limite = evento.timesort.takeIf { it > 0 }
                Actividad(
                    id = evento.id,
                    nombre = evento.name,
                    curso = evento.course?.fullname.orEmpty(),
                    tipo = Clasificador.tipoDesdeModulo(evento.modulename),
                    fechaLimite = limite,
                    estado = if (limite != null && limite < ahora) {
                        EstadoActividad.NO_ENTREGADA
                    } else {
                        EstadoActividad.PENDIENTE
                    },
                    calificada = false,
                    url = evento.url.ifBlank { null }
                )
            }
    }.getOrDefault(emptyList())

    private data class EntregaResumen(val estado: String?, val calificada: Boolean)

    private companion object {
        const val MAX_PETICIONES_SIMULTANEAS = 3
        const val VENTANA_PASADA = 60L * 60 * 24 * 60
        const val VENTANA_FUTURA = 60L * 60 * 24 * 180
    }
}
