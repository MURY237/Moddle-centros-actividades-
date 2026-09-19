package com.asir.moodleactividades.domain

import kotlinx.serialization.Serializable

enum class EstadoActividad(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    ENTREGADA("Entregada"),
    NO_ENTREGADA("No entregada")
}

enum class TipoActividad(val etiqueta: String) {
    TAREA("Tarea"),
    CUESTIONARIO("Cuestionario"),
    FORO("Foro"),
    OTRA("Actividad")
}

enum class GrupoPlazo(val etiqueta: String) {
    VENCIDA("Plazo vencido"),
    HOY("Hoy"),
    ESTA_SEMANA("Próximos 7 días"),
    ESTE_MES("Próximos 30 días"),
    MAS_ADELANTE("Más adelante"),
    SIN_FECHA("Sin fecha límite")
}

enum class RangoTiempo(val etiqueta: String, val dias: Int?) {
    HOY("Hoy", 0),
    SEMANA("7 días", 7),
    MES("30 días", 30),
    TRIMESTRE("3 meses", 90),
    TODO("Todo", null)
}

enum class FiltroEstado(val etiqueta: String, val estado: EstadoActividad?) {
    TODAS("Todas", null),
    PENDIENTES("Pendientes", EstadoActividad.PENDIENTE),
    ENTREGADAS("Entregadas", EstadoActividad.ENTREGADA),
    NO_ENTREGADAS("No entregadas", EstadoActividad.NO_ENTREGADA)
}

@Serializable
data class Actividad(
    val id: Long,
    val nombre: String,
    val curso: String,
    val tipo: TipoActividad,
    val fechaLimite: Long?,
    val estado: EstadoActividad,
    val calificada: Boolean,
    val url: String?,
    val nota: String? = null
)

data class SeccionActividades(
    val grupo: GrupoPlazo,
    val actividades: List<Actividad>
)

data class ResumenActividades(
    val pendientes: Int,
    val entregadas: Int,
    val noEntregadas: Int
)
