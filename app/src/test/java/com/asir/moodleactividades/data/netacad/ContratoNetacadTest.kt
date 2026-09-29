package com.asir.moodleactividades.data.netacad

import com.asir.moodleactividades.domain.EstadoActividad
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** Lo que el guion devuelve de verdad en jsdom, leído por el mismo código que en la app. */
class ContratoNetacadTest {
    private val zona = ZoneId.of("Europe/Madrid")
    private val ahora = LocalDateTime.of(2025, 11, 20, 10, 0).atZone(zona).toEpochSecond()
    private val json = Json { ignoreUnknownKeys = true }

    private fun leer(recurso: String) = LectorNetacad.leer(
        json.decodeFromString<ResultadoNetacad>(javaClass.getResource("/netacad" + recurso)!!.readText()), ahora, zona
    ).associateBy { it.titulo }

    private fun dia(s: Long?) = java.time.LocalDate.ofInstant(java.time.Instant.ofEpochSecond(s!!), zona)

    @Test fun tabla() {
        val t = leer("/tabla.json")
        assertEquals(4, t.size)
        assertEquals(EstadoActividad.PENDIENTE, t["Examen del capítulo 1"]!!.estado)
        assertEquals(java.time.LocalDate.of(2025, 12, 5), dia(t["Examen del capítulo 1"]!!.fechaLimite))
        assertEquals(EstadoActividad.ENTREGADA, t["Packet Tracer 2.3.8"]!!.estado)
        assertEquals("92%", t["Packet Tracer 2.3.8"]!!.nota)
        assertEquals(EstadoActividad.PENDIENTE, t["Práctica 3.4.6"]!!.estado)
        assertEquals(java.time.LocalDate.of(2025, 12, 10), dia(t["Práctica 3.4.6"]!!.fechaLimite))
        assertEquals(EstadoActividad.NO_ENTREGADA, t["Quiz vencido"]!!.estado)
        assertEquals("CCNA1: Introducción a las Redes", t["Quiz vencido"]!!.curso)
    }

    @Test fun tarjetas() {
        val t = leer("/tarjetas.json")
        assertEquals(3, t.size)
        val examen = t["Module 4 Exam"]!!
        assertEquals(EstadoActividad.PENDIENTE, examen.estado)
        // La de entrega (5 dic), no la de apertura (1 nov): con la otra saldría vencido.
        assertEquals(java.time.LocalDate.of(2025, 12, 5), dia(examen.fechaLimite))
        assertEquals(EstadoActividad.ENTREGADA, t["Skills Assessment"]!!.estado)
        assertEquals("18/20", t["Skills Assessment"]!!.nota)
        // Incomplete y ya pasado: fuera de plazo, no «hecho» por contener «complete».
        assertEquals(EstadoActividad.NO_ENTREGADA, t["Chapter 9 Quiz"]!!.estado)
    }
}
