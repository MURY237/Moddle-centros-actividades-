package com.asir.moodleactividades.data.net

import kotlinx.serialization.Serializable

@Serializable
data class SiteInfoDto(
    val sitename: String = "",
    val username: String = "",
    val fullname: String = "",
    val userid: Long = 0,
    val release: String = "",
    // Cada sitio decide qué funciones abre a la app móvil, así que la única forma de saber
    // qué se puede pedir es preguntárselo a él.
    val functions: List<FuncionDto> = emptyList()
)

@Serializable
data class FuncionDto(
    val name: String = "",
    val version: String = ""
)

@Serializable
data class AssignmentsDto(
    val courses: List<CursoConTareasDto> = emptyList()
)

@Serializable
data class CursoConTareasDto(
    val id: Long = 0,
    val fullname: String = "",
    val shortname: String = "",
    val assignments: List<TareaDto> = emptyList()
)

@Serializable
data class TareaDto(
    val id: Long = 0,
    val cmid: Long = 0,
    val course: Long = 0,
    val name: String = "",
    val duedate: Long = 0,
    val cutoffdate: Long = 0,
    val allowsubmissionsfromdate: Long = 0,
    val intro: String = "",
    val introattachments: List<ArchivoDto> = emptyList(),
    val introfiles: List<ArchivoDto> = emptyList()
)

@Serializable
data class ArchivoDto(
    val filename: String = "",
    val fileurl: String = "",
    val filesize: Long = 0,
    val mimetype: String = ""
)

@Serializable
data class EstadoEntregaDto(
    val lastattempt: UltimoIntentoDto? = null,
    val feedback: FeedbackDto? = null
)

@Serializable
data class FeedbackDto(
    val gradefordisplay: String = "",
    val grade: NotaEntregaDto? = null
)

@Serializable
data class NotaEntregaDto(
    val grade: String = ""
)

@Serializable
data class UltimoIntentoDto(
    val submission: EntregaDto? = null,
    val teamsubmission: EntregaDto? = null,
    val graded: Boolean = false,
    val submissionsenabled: Boolean = true
)

@Serializable
data class EntregaDto(
    val status: String = "new",
    val gradingstatus: String = "",
    val timemodified: Long = 0
)

@Serializable
data class NotasTareasDto(
    val assignments: List<NotasDeTareaDto> = emptyList()
)

@Serializable
data class NotasDeTareaDto(
    val assignmentid: Long = 0,
    val grades: List<NotaAlumnoDto> = emptyList()
)

@Serializable
data class NotaAlumnoDto(
    val userid: Long = 0,
    val grade: String = ""
)

@Serializable
data class NotasCursoDto(
    val usergrades: List<NotasUsuarioDto> = emptyList()
)

@Serializable
data class NotasUsuarioDto(
    val courseid: Long = 0,
    val gradeitems: List<ItemNotaDto> = emptyList()
)

@Serializable
data class ItemNotaDto(
    val itemname: String? = null,
    val itemtype: String = "",
    val itemmodule: String? = null,
    val iteminstance: Long = 0,
    val cmid: Long? = null,
    val gradeformatted: String = "",
    val percentageformatted: String = "",
    val grademax: Double = 0.0,
    val gradedategraded: Long? = null,
    val gradedatesubmitted: Long? = null
)

@Serializable
data class CursoMatriculadoDto(
    val id: Long = 0,
    val fullname: String = "",
    val shortname: String = ""
)

@Serializable
data class EventosCalendarioDto(
    val events: List<EventoDto> = emptyList()
)

@Serializable
data class EventoDto(
    val id: Long = 0,
    val name: String = "",
    val timesort: Long = 0,
    val modulename: String? = null,
    val url: String = "",
    val course: CursoEventoDto? = null
)

@Serializable
data class CursoEventoDto(
    val id: Long = 0,
    val fullname: String = ""
)
