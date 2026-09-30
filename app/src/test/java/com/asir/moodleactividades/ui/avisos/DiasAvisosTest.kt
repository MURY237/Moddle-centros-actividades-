package com.asir.moodleactividades.ui.avisos

import com.asir.moodleactividades.data.Aviso
import com.asir.moodleactividades.data.TipoAviso
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DiasAvisosTest {

    private val zona = ZoneId.of("Europe/Madrid")

    private fun momento(dia: Int, hora: Int) =
        LocalDateTime.of(2026, 3, dia, hora, 0).atZone(zona).toEpochSecond()

    private fun aviso(id: String, dia: Int, hora: Int) =
        Aviso(momento(dia, hora), TipoAviso.NOTA, "Aviso $id", "", id = id)

    @Test
    fun repartePorDiasNaturalesYNoPorHoras() {
        val ahora = momento(20, 9)
        val avisos = listOf(
            aviso("a", 20, 8),
            // Hace menos de 24 horas, pero fue ayer.
            aviso("b", 19, 23),
            aviso("c", 19, 1),
            aviso("d", 14, 12),
            aviso("e", 13, 12),
            aviso("f", 1, 12)
        )

        val grupos = agruparPorDia(avisos, ahora, zona)

        assertEquals(listOf(DiaAviso.HOY, DiaAviso.AYER, DiaAviso.SEMANA, DiaAviso.ANTES), grupos.map { it.first })
        assertEquals(listOf("a"), grupos[0].second.map { it.id })
        assertEquals(listOf("b", "c"), grupos[1].second.map { it.id })
        assertEquals(listOf("d"), grupos[2].second.map { it.id })
        assertEquals(listOf("e", "f"), grupos[3].second.map { it.id })
    }

    @Test
    fun unAvisoConLaHoraAdelantadaCuentaComoDeHoy() {
        val ahora = momento(20, 9)
        val grupos = agruparPorDia(listOf(aviso("x", 21, 10)), ahora, zona)
        assertEquals(DiaAviso.HOY, grupos.single().first)
    }

    @Test
    fun sinAvisosNoHayGrupos() {
        assertEquals(emptyList<Pair<DiaAviso, List<Aviso>>>(), agruparPorDia(emptyList(), 0, zona))
    }
}
