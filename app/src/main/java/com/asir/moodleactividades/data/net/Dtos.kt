package com.asir.moodleactividades.data.net

import kotlinx.serialization.Serializable

@Serializable
data class SiteInfoDto(
    val sitename: String = "",
    val username: String = "",
    val fullname: String = "",
    val userid: Long = 0
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
    val allowsubmissionsfromdate: Long = 0
)

@Serializable
data class EstadoEntregaDto(
    val lastattempt: UltimoIntentoDto? = null
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
    val itemmodule: String? = null,
    val iteminstance: Long = 0,
    val gradeformatted: String = "",
    val percentageformatted: String = ""
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
