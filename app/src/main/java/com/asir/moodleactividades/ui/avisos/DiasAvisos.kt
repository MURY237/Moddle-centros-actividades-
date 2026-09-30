package com.asir.moodleactividades.ui.avisos

import com.asir.moodleactividades.data.Aviso
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class DiaAviso(val etiqueta: String) {
    HOY("Hoy"),
    AYER("Ayer"),
    SEMANA("Últimos 7 días"),
    ANTES("Anteriores")
}

/** Reparte los avisos por antigüedad en días naturales, en el orden en que llegan. */
internal fun agruparPorDia(
    avisos: List<Aviso>,
    ahora: Long,
    zona: ZoneId = ZoneId.systemDefault()
): List<Pair<DiaAviso, List<Aviso>>> {
    val hoy = Instant.ofEpochSecond(ahora).atZone(zona).toLocalDate()
    return avisos
        .groupBy { aviso ->
            val dia = Instant.ofEpochSecond(aviso.momento).atZone(zona).toLocalDate()
            when (ChronoUnit.DAYS.between(dia, hoy)) {
                in Long.MIN_VALUE..0L -> DiaAviso.HOY
                1L -> DiaAviso.AYER
                in 2L..6L -> DiaAviso.SEMANA
                else -> DiaAviso.ANTES
            }
        }
        .toSortedMap(compareBy { it.ordinal })
        .map { (dia, lista) -> dia to lista }
}
