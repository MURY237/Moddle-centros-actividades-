package com.asir.moodleactividades.domain

import com.asir.moodleactividades.data.netacad.RespuestaNetacad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetacadTest {
    private fun t(titulo: String, curso: String, fecha: Long? = 100) =
        TrabajoNetacad("${curso.lowercase()}|${titulo.lowercase()}", titulo, curso, fecha, EstadoActividad.PENDIENTE)

    @Test fun `leer un curso no borra los demas`() {
        val previos = listOf(t("Examen 1", "CCNA1"), t("Quiz Linux", "Linux"))
        val nuevos = listOf(t("Examen 2", "CCNA1"))
        val r = ResumenNetacad.fusionar(previos, nuevos).map { it.titulo }.toSet()
        // «Examen 1» ya no está en CCNA1: se va. Linux no se ha mirado: se queda.
        assertEquals(setOf("Examen 2", "Quiz Linux"), r)
    }

    @Test fun `fusionar ordena por fecha`() {
        val r = ResumenNetacad.fusionar(emptyList(), listOf(t("Tarde", "A", 300), t("Pronto", "A", 100), t("Sin", "A", null)))
        assertEquals(listOf("Pronto", "Tarde", "Sin"), r.map { it.titulo })
    }

    @Test fun `un curso anadido por primera vez no dispara avisos`() {
        val antes = listOf(t("Examen 1", "CCNA1"))
        val ahora = listOf(t("Examen 1", "CCNA1"), t("Quiz 1", "Linux"), t("Quiz 2", "Linux"))
        assertTrue(ResumenNetacad.recienPuestos(antes, ahora).isEmpty())
    }

    @Test fun `un trabajo nuevo en un curso conocido si avisa`() {
        val antes = listOf(t("Examen 1", "CCNA1"))
        val ahora = listOf(t("Examen 1", "CCNA1"), t("Examen 2", "CCNA1"))
        assertEquals(listOf("Examen 2"), ResumenNetacad.recienPuestos(antes, ahora).map { it.titulo })
    }

    @Test fun `lo pendiente que ya vencio se muestra fuera de plazo`() {
        // Leído ayer como pendiente, hoy ya ha pasado: sin conexión tiene que verse igual.
        val leidos = listOf(
            t("Vencido", "A", 50),
            t("Futuro", "A", 500),
            t("SinFecha", "A", null),
            t("Hecho", "A", 50).copy(estado = EstadoActividad.ENTREGADA)
        )
        val r = ResumenNetacad.alDia(leidos, ahora = 100).associate { it.titulo to it.estado }
        assertEquals(EstadoActividad.NO_ENTREGADA, r["Vencido"])
        assertEquals(EstadoActividad.PENDIENTE, r["Futuro"])
        assertEquals(EstadoActividad.PENDIENTE, r["SinFecha"])
        // Lo hecho sigue hecho, venza cuando venza.
        assertEquals(EstadoActividad.ENTREGADA, r["Hecho"])
    }

    @Test fun `respuesta envuelta por evaluateJavascript`() {
        // Así llega de verdad: una cadena JSON dentro de otra.
        val crudo = "\"{\\\"pagina\\\":\\\"acceso\\\"}\""
        val r = RespuestaNetacad.leer(crudo)!!
        assertTrue(r.pideAcceso)
    }

    @Test fun `respuesta nula o rota no revienta`() {
        assertNull(RespuestaNetacad.leer(null))
        assertNull(RespuestaNetacad.leer("null"))
        assertNull(RespuestaNetacad.leer("\"no es json\""))
    }
}
