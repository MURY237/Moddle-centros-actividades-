package com.asir.moodleactividades.data

import com.asir.moodleactividades.data.net.AssignmentsDto
import com.asir.moodleactividades.data.net.CursoMatriculadoDto
import com.asir.moodleactividades.data.net.EstadoEntregaDto
import com.asir.moodleactividades.data.net.EventosCalendarioDto
import com.asir.moodleactividades.data.net.FeedbackDto
import com.asir.moodleactividades.data.net.ItemNotaDto
import com.asir.moodleactividades.data.net.MoodleClient
import com.asir.moodleactividades.data.net.MoodleException
import com.asir.moodleactividades.data.net.NotasCursoDto
import com.asir.moodleactividades.data.net.ResultadoSso
import com.asir.moodleactividades.data.net.SiteInfoDto
import com.asir.moodleactividades.data.net.SsoLogin
import com.asir.moodleactividades.data.net.decodificar
import com.asir.moodleactividades.domain.Actividad
import com.asir.moodleactividades.domain.Calificacion
import com.asir.moodleactividades.domain.Clasificador
import com.asir.moodleactividades.domain.EstadoActividad
import com.asir.moodleactividades.domain.NotasDeCurso
import com.asir.moodleactividades.domain.TipoActividad
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
            nombreSitio = info.sitename,
            idUsuario = info.userid
        )
        sesionStore.guardar(sesion)
        return sesion
    }

    suspend fun cargarActividades(ahora: Long = System.currentTimeMillis() / 1000): List<Actividad> {
        val sesion = sesionStore.leer() ?: throw MoodleException(null, "No hay ninguna sesión iniciada.")
        val cliente = MoodleClient(sesion.urlSitio, sesion.token)

        val previas = cache.leer()?.actividades?.associateBy { it.id }.orEmpty()
        val actividades = cargarTareas(cliente, ahora, previas) + cargarEventosNoTarea(cliente, ahora)
        cache.guardar(actividades, ahora)
        return actividades
    }

    private suspend fun cargarTareas(
        cliente: MoodleClient,
        ahora: Long,
        previas: Map<Long, Actividad>
    ): List<Actividad> = coroutineScope {
        val respuesta: AssignmentsDto = cliente.decodificar(cliente.invocar("mod_assign_get_assignments"))
        val limitador = Semaphore(MAX_PETICIONES_SIMULTANEAS)

        val notas = async {
            respuesta.courses.map { curso ->
                async { limitador.withPermit { consultarNotas(cliente, curso.id) } }
            }.awaitAll().fold(emptyMap<Long, String>()) { acumulado, parcial -> acumulado + parcial }
        }

        val pares = respuesta.courses.flatMap { curso ->
            curso.assignments.map { tarea -> curso to tarea }
        }
        val notasPorTarea = notas.await()

        // Un alumno con varios cursos arrastra cientos de tareas de años anteriores, y una
        // petición por cada una acaba siendo rechazada por el centro. Solo se consulta el
        // estado de las que no tienen nota todavía, empezando por las de plazo más cercano.
        val aConsultar = pares
            .filterNot { (_, tarea) -> notasPorTarea.containsKey(tarea.id) }
            .sortedBy { (_, tarea) -> distanciaAlPlazo(tarea.duedate, ahora) }
            .take(MAX_CONSULTAS_ESTADO)
            .mapTo(mutableSetOf()) { (_, tarea) -> tarea.id }

        pares.map { (curso, tarea) ->
            async {
                val estadoEntrega = if (tarea.id in aConsultar) {
                    limitador.withPermit { consultarEntrega(cliente, tarea.id) }
                } else {
                    null
                }
                val limite = tarea.duedate.takeIf { it > 0 }
                val nota = notasPorTarea[tarea.id]
                val previa = previas[tarea.id]

                Actividad(
                    id = tarea.id,
                    nombre = tarea.name,
                    curso = curso.fullname.ifBlank { curso.shortname },
                    tipo = TipoActividad.TAREA,
                    fechaLimite = limite,
                    // Sin respuesta del centro no se puede afirmar que falte la entrega: una
                    // tarea con nota está entregada, y si no, se conserva lo último que se supo.
                    estado = when {
                        estadoEntrega != null -> Clasificador.estado(estadoEntrega.estado, limite, ahora)
                        nota != null -> EstadoActividad.ENTREGADA
                        previa != null -> previa.estado
                        else -> EstadoActividad.PENDIENTE
                    },
                    calificada = estadoEntrega?.calificada ?: (nota != null),
                    url = "${cliente.urlSitio}mod/assign/view.php?id=${tarea.cmid}",
                    nota = nota ?: estadoEntrega?.nota ?: previa?.nota
                )
            }
        }.awaitAll()
    }

    /** Sin fecha límite va al final: no hay urgencia que justifique gastar una consulta. */
    private fun distanciaAlPlazo(plazo: Long, ahora: Long): Long =
        if (plazo <= 0) Long.MAX_VALUE else kotlin.math.abs(plazo - ahora)

    suspend fun cargarCalificaciones(): List<NotasDeCurso> = coroutineScope {
        val sesion = sesionStore.leer() ?: throw MoodleException(null, "No hay ninguna sesión iniciada.")
        val cliente = MoodleClient(sesion.urlSitio, sesion.token)

        val cursos: List<CursoMatriculadoDto> = cliente.decodificar(
            cliente.invocar(
                "core_enrol_get_users_courses",
                mapOf("userid" to idUsuario(cliente, sesion).toString())
            )
        )

        val limitador = Semaphore(MAX_PETICIONES_SIMULTANEAS)
        val resultados = cursos.map { curso ->
            async {
                limitador.withPermit {
                    runCatching {
                        notasDelCurso(cliente, curso.id, curso.fullname.ifBlank { curso.shortname })
                    }
                }
            }
        }.awaitAll()

        val obtenidos = resultados.mapNotNull { it.getOrNull() }
            .filter { it.calificaciones.isNotEmpty() || it.total != null }

        if (obtenidos.isNotEmpty()) return@coroutineScope obtenidos

        // Si el centro no deja leer su libro de calificaciones, al menos se enseña lo que ya
        // se sabe por las propias tareas antes de darse por vencido.
        val respaldo = notasDesdeActividades()
        if (respaldo.isNotEmpty()) return@coroutineScope respaldo

        // Y si tampoco hay nada, el motivo real, no un «no tienes notas» que es mentira.
        resultados.firstNotNullOfOrNull { it.exceptionOrNull() }?.let { throw it }
        if (cursos.isEmpty()) {
            throw MoodleException(null, "Moodle no ha devuelto ninguna asignatura para tu usuario.")
        }
        emptyList()
    }

    private fun notasDesdeActividades(): List<NotasDeCurso> =
        cache.leer()?.actividades.orEmpty()
            .filter { it.curso.isNotBlank() }
            .groupBy { it.curso }
            .map { (curso, actividades) ->
                NotasDeCurso(
                    curso = curso,
                    total = null,
                    calificaciones = actividades.map { actividad ->
                        Calificacion(
                            curso = curso,
                            nombre = actividad.nombre,
                            nota = actividad.nota.orEmpty(),
                            porcentaje = "",
                            notaMaxima = 0.0,
                            esTotalDelCurso = false,
                            tipo = actividad.tipo
                        )
                    }
                )
            }
            .sortedBy { it.curso }

    /**
     * Las sesiones abiertas antes de que se guardara el identificador lo tienen a cero, y sin
     * él Moodle devuelve una lista de cursos vacía en lugar de un error.
     */
    private suspend fun idUsuario(cliente: MoodleClient, sesion: Sesion): Long {
        if (sesion.idUsuario > 0) return sesion.idUsuario

        val info: SiteInfoDto = cliente.decodificar(cliente.invocar("core_webservice_get_site_info"))
        if (info.userid > 0) sesionStore.guardar(sesion.copy(idUsuario = info.userid))
        return info.userid
    }

    private suspend fun notasDelCurso(
        cliente: MoodleClient,
        idCurso: Long,
        nombreCurso: String
    ): NotasDeCurso {
        val dto: NotasCursoDto = cliente.decodificar(
            cliente.invocar(
                "gradereport_user_get_grade_items",
                mapOf("courseid" to idCurso.toString())
            )
        )

        val items = dto.usergrades.flatMap { it.gradeitems }
        // Todo lo evaluable del curso, con nota o sin ella: una tarea aún sin calificar
        // también es algo que el alumno necesita ver.
        val calificaciones = items
            .filter { it.itemtype in TIPOS_EVALUABLES }
            .map { it.aCalificacion(nombreCurso) }
        val total = items
            .firstOrNull { it.itemtype == "course" && it.gradeformatted.esNotaReal() }
            ?.aCalificacion(nombreCurso)

        return NotasDeCurso(curso = nombreCurso, total = total, calificaciones = calificaciones)
    }

    private fun ItemNotaDto.aCalificacion(nombreCurso: String) = Calificacion(
        curso = nombreCurso,
        nombre = itemname?.takeIf { it.isNotBlank() } ?: "Total del curso",
        nota = gradeformatted.takeIf { it.esNotaReal() }.orEmpty(),
        porcentaje = percentageformatted.takeIf { it.esNotaReal() }.orEmpty(),
        notaMaxima = grademax,
        esTotalDelCurso = itemtype == "course",
        tipo = Clasificador.tipoDesdeModulo(itemmodule)
    )

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

    private suspend fun consultarEntrega(cliente: MoodleClient, idTarea: Long): EntregaResumen? {
        repeat(INTENTOS_POR_TAREA) { intentoNumero ->
            val resultado = runCatching {
                val dto: EstadoEntregaDto = cliente.decodificar(
                    cliente.invocar(
                        "mod_assign_get_submission_status",
                        mapOf("assignid" to idTarea.toString())
                    )
                )
                val intento = dto.lastattempt
                val entrega = intento?.submission ?: intento?.teamsubmission
                EntregaResumen(
                    estado = entrega?.status,
                    calificada = intento?.graded == true || entrega?.gradingstatus == "graded",
                    nota = dto.feedback?.notaLegible()
                )
            }.getOrNull()

            if (resultado != null) return resultado
            if (intentoNumero < INTENTOS_POR_TAREA - 1) delay(ESPERA_ENTRE_INTENTOS_MS)
        }
        return null
    }

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

    private data class EntregaResumen(
        val estado: String?,
        val calificada: Boolean,
        val nota: String? = null
    )

    /** Moodle devuelve esta nota con etiquetas HTML alrededor cuando la formatea para la web. */
    private fun FeedbackDto.notaLegible(): String? {
        val bruta = gradefordisplay.ifBlank { grade?.grade.orEmpty() }
        val limpia = bruta.replace(Regex("<[^>]*>"), "").trim()
        return limpia.takeIf { it.esNotaReal() }
    }

    private companion object {
        val TIPOS_EVALUABLES = setOf("mod", "manual")
        const val MAX_PETICIONES_SIMULTANEAS = 3
        const val MAX_CONSULTAS_ESTADO = 60
        const val INTENTOS_POR_TAREA = 2
        const val ESPERA_ENTRE_INTENTOS_MS = 900L
        const val VENTANA_PASADA = 60L * 60 * 24 * 60
        const val VENTANA_FUTURA = 60L * 60 * 24 * 180
    }
}
