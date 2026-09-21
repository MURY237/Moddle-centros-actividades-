package com.asir.moodleactividades.domain

import java.time.Instant
import java.time.ZoneId

object Clasificador {

    private val ESTADOS_ENTREGADOS = setOf("submitted")

    fun estado(estadoEntrega: String?, fechaLimite: Long?, ahora: Long): EstadoActividad = when {
        estadoEntrega?.lowercase() in ESTADOS_ENTREGADOS -> EstadoActividad.ENTREGADA
        fechaLimite != null && fechaLimite > 0 && fechaLimite < ahora -> EstadoActividad.NO_ENTREGADA
        else -> EstadoActividad.PENDIENTE
    }

    fun grupo(fechaLimite: Long?, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): GrupoPlazo {
        if (fechaLimite == null || fechaLimite <= 0) return GrupoPlazo.SIN_FECHA
        if (fechaLimite < ahora) return GrupoPlazo.VENCIDA

        val hoy = Instant.ofEpochSecond(ahora).atZone(zona).toLocalDate()
        val dia = Instant.ofEpochSecond(fechaLimite).atZone(zona).toLocalDate()
        val diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(hoy, dia)

        return when {
            diasRestantes <= 0L -> GrupoPlazo.HOY
            diasRestantes <= 7L -> GrupoPlazo.ESTA_SEMANA
            diasRestantes <= 30L -> GrupoPlazo.ESTE_MES
            else -> GrupoPlazo.MAS_ADELANTE
        }
    }

    fun tipoDesdeModulo(modulo: String?): TipoActividad = when (modulo?.lowercase()) {
        "assign" -> TipoActividad.TAREA
        "quiz" -> TipoActividad.CUESTIONARIO
        "forum" -> TipoActividad.FORO
        else -> TipoActividad.OTRA
    }

    /**
     * El rango mira hacia delante: son los plazos que quedan por llegar. Lo ya vencido solo
     * escapa al filtro cuando sigue sin entregarse, porque entonces continúa siendo accionable.
     */
    fun dentroDelRango(actividad: Actividad, rango: RangoTiempo, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): Boolean {
        if (rango.dias == null) return true
        if (actividad.estado == EstadoActividad.NO_ENTREGADA) return true

        // Una tarea sin fecha límite no vence nunca, así que ningún recorte temporal debería
        // hacerla desaparecer: sigue estando por hacer. Va a su propio grupo, al final.
        val limite = actividad.fechaLimite ?: return true
        if (limite <= 0) return true

        val hoy = Instant.ofEpochSecond(ahora).atZone(zona).toLocalDate()
        val dia = Instant.ofEpochSecond(limite).atZone(zona).toLocalDate()
        return java.time.temporal.ChronoUnit.DAYS.between(hoy, dia) in 0..rango.dias.toLong()
    }

    fun agrupar(actividades: List<Actividad>, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): List<SeccionActividades> =
        actividades
            .sortedWith(compareBy({ it.fechaLimite ?: Long.MAX_VALUE }, { it.nombre }))
            .groupBy { grupo(it.fechaLimite, ahora, zona) }
            .toSortedMap(compareBy { it.ordinal })
            .map { (grupo, lista) -> SeccionActividades(grupo, lista) }

    /** Merece aviso lo que sigue sin entregarse y vence dentro de la ventana indicada. */
    fun porVencer(actividades: List<Actividad>, ahora: Long, ventanaSegundos: Long): List<Actividad> =
        actividades.filter { actividad ->
            val limite = actividad.fechaLimite ?: return@filter false
            actividad.estado != EstadoActividad.ENTREGADA &&
                limite > ahora &&
                limite - ahora <= ventanaSegundos
        }

    fun resumir(actividades: List<Actividad>): ResumenActividades = ResumenActividades(
        pendientes = actividades.count { it.estado == EstadoActividad.PENDIENTE },
        entregadas = actividades.count { it.estado == EstadoActividad.ENTREGADA },
        noEntregadas = actividades.count { it.estado == EstadoActividad.NO_ENTREGADA }
    )
}
