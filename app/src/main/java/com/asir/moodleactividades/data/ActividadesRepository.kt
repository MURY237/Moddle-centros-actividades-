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
import com.asir.moodleactividades.data.net.NotasTareasDto
import com.asir.moodleactividades.data.net.ResultadoSso
import com.asir.moodleactividades.data.net.SiteInfoDto
import com.asir.moodleactividades.data.net.SsoLogin
import com.asir.moodleactividades.data.net.decodificar
import com.asir.moodleactividades.data.net.limpiarHtml
import com.asir.moodleactividades.data.net.notaVisible
import com.asir.moodleactividades.domain.Actividad
import com.asir.moodleactividades.domain.Adjunto
import com.asir.moodleactividades.domain.Calificacion
import com.asir.moodleactividades.domain.Clasificador
import com.asir.moodleactividades.domain.EstadoActividad
import com.asir.moodleactividades.domain.NotasDeCurso
import com.asir.moodleactividades.domain.SondeoAsistencia
import com.asir.moodleactividades.domain.TipoActividad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Locale

class ActividadesRepository(
    private val sesionStore: SesionStore,
    private val cache: CacheActividades,
    private val cacheNotas: CacheCalificaciones
) {

    fun sesionGuardada(): Sesion? = sesionStore.leer()

    /**
     * Los archivos de Moodle se sirven por `pluginfile.php`, que exige el token en la propia
     * URL. Se añade solo al descargar para no dejarlo escrito en la caché.
     */
    fun urlDescargable(adjunto: Adjunto): String? {
        val token = sesionStore.leer()?.token ?: return null
        val separador = if ('?' in adjunto.url) '&' else '?'
        return "${adjunto.url}${separador}token=$token"
    }

    fun ultimaUrl(): String = sesionStore.ultimaUrl()

    fun instantaneaGuardada(): Instantanea? = cache.leer()

    fun notasGuardadas(): InstantaneaNotas? = cacheNotas.leer()

    /**
     * Pregunta al centro qué funciones abre a la app móvil y se queda con las de asistencia.
     * En Moodle Centros las faltas suelen estar en Séneca, que no publica API, así que esto
     * sirve para saber si hay una vía por Moodle en lugar de darlo por hecho.
     */
    suspend fun sondearAsistencia(): SondeoAsistencia {
        val sesion = sesionStore.leer()
            ?: throw MoodleException(null, "No hay ninguna sesión iniciada.")
        val cliente = MoodleClient(sesion.urlSitio, sesion.token)
        val info: SiteInfoDto = cliente.decodificar(cliente.invocar("core_webservice_get_site_info"))

        val nombres = info.functions.map { it.name }.filter { it.isNotBlank() }
        return SondeoAsistencia(
            sitio = info.sitename.ifBlank { sesion.nombreSitio },
            version = info.release,
            totalFunciones = nombres.size,
            funcionesAsistencia = nombres.filter { SondeoAsistencia.esDeAsistencia(it) }.sorted()
        )
    }

    fun cerrarSesion() {
        sesionStore.borrar()
        cache.borrar()
        cacheNotas.borrar()
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

    suspend fun prepararSso(url: String): String = withContext(Dispatchers.IO) {
        val sitio = MoodleClient.normalizarUrl(url)
        val passport = SsoLogin.generarPassport()
        val destino = SsoLogin.urlDeLanzamiento(sitio, passport)

        if (SsoLogin.servicioMovilApagado(MoodleClient.descargarTexto(destino))) {
            throw MoodleException(
                "mobileservicesnotenabled",
                "Este portal tiene desactivado el acceso desde aplicaciones móviles, así que " +
                    "ninguna app puede consultarlo, tampoco la oficial de Moodle. Pídeselo al " +
                    "coordinador TIC del centro o entra con un token si tu perfil te deja " +
                    "generarlo."
            )
        }

        sesionStore.guardarSsoPendiente(sitio, passport)
        destino
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

        // El id de una tarea puede coincidir con el de un evento del calendario, así que la
        // clave lleva el tipo: sin él, un evento pisaba la tarea y le borraba estado y nota.
        val previas = cache.leer()?.actividades?.associateBy { claveDe(it.tipo, it.id) }.orEmpty()
        val actividades = cargarTareas(cliente, ahora, previas, idUsuario(cliente, sesion)) +
            cargarEventosNoTarea(cliente, ahora)
        cache.guardar(actividades, ahora)
        return actividades
    }

    private suspend fun cargarTareas(
        cliente: MoodleClient,
        ahora: Long,
        previas: Map<String, Actividad>,
        idUsuario: Long
    ): List<Actividad> = coroutineScope {
        val respuesta: AssignmentsDto = cliente.decodificar(cliente.invocar("mod_assign_get_assignments"))
        val limitador = Semaphore(MAX_PETICIONES_SIMULTANEAS)

        val idsTareas = respuesta.courses.flatMap { curso -> curso.assignments.map { it.id } }

        val notas = async {
            // En lotes: una petición por cada cincuenta tareas en vez de una por tarea.
            val porLotes = idsTareas.chunked(TAMANO_LOTE_NOTAS).map { lote ->
                async { limitador.withPermit { notasDeTareas(cliente, lote, idUsuario) } }
            }.awaitAll().fold(emptyMap<Long, String>()) { acumulado, parcial -> acumulado + parcial }

            // El libro de calificaciones aporta las que no son tareas, cuando el centro lo permite.
            val porCurso = respuesta.courses.map { curso ->
                async { limitador.withPermit { consultarNotas(cliente, curso.id) } }
            }.awaitAll().fold(emptyMap<Long, String>()) { acumulado, parcial -> acumulado + parcial }

            porCurso + porLotes
        }

        val pares = respuesta.courses.flatMap { curso ->
            curso.assignments.map { tarea -> curso to tarea }
        }
        val notasPorTarea = notas.await()

        // La consulta individual es la única que da la nota cuando el centro no abre su libro
        // de calificaciones, así que se reserva para las tareas que aún no la tienen,
        // empezando por las de plazo más cercano a hoy.
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
                val previa = previas[claveDe(TipoActividad.TAREA, tarea.id)]

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
                    nota = nota ?: estadoEntrega?.nota ?: previa?.nota,
                    descripcion = tarea.intro,
                    adjuntos = (tarea.introattachments + tarea.introfiles)
                        .filter { it.filename.isNotBlank() && it.fileurl.isNotBlank() }
                        .distinctBy { it.fileurl }
                        .map {
                            Adjunto(
                                nombre = it.filename,
                                url = it.fileurl,
                                tamano = it.filesize,
                                tipo = it.mimetype
                            )
                        }
                )
            }
        }.awaitAll()
    }

    /** Sin fecha límite va al final: no hay urgencia que justifique gastar una consulta. */
    private fun distanciaAlPlazo(plazo: Long, ahora: Long): Long =
        if (plazo <= 0) Long.MAX_VALUE else kotlin.math.abs(plazo - ahora)

    private fun claveDe(tipo: TipoActividad, id: Long) = "${tipo.name}-$id"

    /**
     * Guarda cada consulta correcta: sin esa copia no habría forma de distinguir una nota
     * recién publicada de una que ya estaba ahí.
     */
    suspend fun cargarCalificaciones(): List<NotasDeCurso> {
        val cursos = consultarCalificaciones()
        if (cursos.isNotEmpty()) cacheNotas.guardar(cursos)
        return cursos
    }

    private suspend fun consultarCalificaciones(): List<NotasDeCurso> = coroutineScope {
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
            .sortedByDescending { it.fechaMasReciente }

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
                            tipo = actividad.tipo,
                            fecha = actividad.fechaLimite,
                            url = actividad.url
                        )
                    }.ordenadasPorRecientes()
                )
            }
            .sortedByDescending { it.fechaMasReciente }

    /** Lo último evaluado es lo que interesa: sin fecha conocida, al final. */
    private fun List<Calificacion>.ordenadasPorRecientes(): List<Calificacion> =
        sortedWith(compareByDescending<Calificacion> { it.fecha ?: Long.MIN_VALUE }.thenBy { it.nombre })

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
            .map { it.aCalificacion(nombreCurso, cliente.urlSitio) }
            .ordenadasPorRecientes()
        val total = items
            .firstOrNull { it.itemtype == "course" && notaVisible(it.gradeformatted).esNotaReal() }
            ?.aCalificacion(nombreCurso, cliente.urlSitio)

        return NotasDeCurso(curso = nombreCurso, total = total, calificaciones = calificaciones)
    }

    private fun ItemNotaDto.aCalificacion(nombreCurso: String, urlSitio: String) = Calificacion(
        curso = nombreCurso,
        nombre = itemname?.takeIf { it.isNotBlank() } ?: "Total del curso",
        nota = notaVisible(gradeformatted).takeIf { it.esNotaReal() }.orEmpty(),
        porcentaje = limpiarHtml(percentageformatted).takeIf { it.esNotaReal() }.orEmpty(),
        notaMaxima = grademax,
        esTotalDelCurso = itemtype == "course",
        tipo = Clasificador.tipoDesdeModulo(itemmodule),
        fecha = (gradedategraded ?: gradedatesubmitted)?.takeIf { it > 0 },
        url = cmid?.takeIf { it > 0 && !itemmodule.isNullOrBlank() }
            ?.let { "${urlSitio}mod/$itemmodule/view.php?id=$it" }
    )

    /**
     * Notas de varias tareas en una sola petición. Es la vía que funciona en centros que no
     * abren el libro de calificaciones a los servicios web.
     */
    private suspend fun notasDeTareas(
        cliente: MoodleClient,
        ids: List<Long>,
        idUsuario: Long
    ): Map<Long, String> =
        runCatching {
            if (ids.isEmpty()) return emptyMap()

            val parametros = ids.withIndex().associate { (posicion, id) ->
                "assignmentids[$posicion]" to id.toString()
            }
            val dto: NotasTareasDto = cliente.decodificar(
                cliente.invocar("mod_assign_get_grades", parametros)
            )

            dto.assignments.mapNotNull { tarea ->
                // Moodle suele devolver solo la nota propia; si el id no cuadra pero viene una
                // sola, es la del alumno igualmente.
                val propia = tarea.grades.firstOrNull { it.userid == idUsuario }
                    ?: tarea.grades.singleOrNull()

                propia?.grade
                    ?.let { formatearNota(it) }
                    ?.let { tarea.assignmentid to it }
            }.toMap()
        }.getOrDefault(emptyMap())

    /** Moodle entrega la nota como «100.00000», y «-1» cuando aún no hay ninguna. */
    private fun formatearNota(bruta: String): String? {
        val valor = bruta.trim().toDoubleOrNull() ?: return null
        if (valor < 0) return null
        return if (valor % 1.0 == 0.0) {
            valor.toLong().toString()
        } else {
            String.format(Locale.forLanguageTag("es-ES"), "%.2f", valor)
        }
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
                .filter { it.itemmodule == "assign" }
                .mapNotNull { item ->
                    notaVisible(item.gradeformatted)
                        .takeIf { it.esNotaReal() }
                        ?.let { item.iteminstance to it }
                }
                .toMap()
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
                // En una tarea de grupo el registro personal se queda en «new» y la entrega
                // real vive en el del grupo, así que gana el que esté más avanzado.
                val entrega = listOfNotNull(intento?.submission, intento?.teamsubmission)
                    .maxByOrNull { avanceDeEntrega(it.status) }
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

    private fun avanceDeEntrega(estado: String?): Int = when (estado?.lowercase()) {
        "submitted" -> 3
        "reopened" -> 2
        "draft" -> 1
        else -> 0
    }

    private data class EntregaResumen(
        val estado: String?,
        val calificada: Boolean,
        val nota: String? = null
    )

    private fun FeedbackDto.notaLegible(): String? =
        notaVisible(gradefordisplay.ifBlank { grade?.grade.orEmpty() })
            .takeIf { it.esNotaReal() }

    private companion object {
        val TIPOS_EVALUABLES = setOf("mod", "manual")
        const val MAX_PETICIONES_SIMULTANEAS = 3
        const val TAMANO_LOTE_NOTAS = 25
        const val MAX_CONSULTAS_ESTADO = 180
        const val INTENTOS_POR_TAREA = 2
        const val ESPERA_ENTRE_INTENTOS_MS = 900L
        const val VENTANA_PASADA = 60L * 60 * 24 * 60
        const val VENTANA_FUTURA = 60L * 60 * 24 * 180
    }
}
