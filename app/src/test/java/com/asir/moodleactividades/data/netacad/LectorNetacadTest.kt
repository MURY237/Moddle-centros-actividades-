package com.asir.moodleactividades.data.netacad

import com.asir.moodleactividades.domain.EstadoActividad
import com.asir.moodleactividades.domain.FiltroNetacad
import com.asir.moodleactividades.domain.GrupoPlazo
import com.asir.moodleactividades.domain.ResumenNetacad
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LectorNetacadTest {

    private val zona = ZoneId.of("Europe/Madrid")
    private val ahora = LocalDateTime.of(2025, 11, 20, 10, 0).atZone(zona).toEpochSecond()

    private fun leer(vararg c: CandidatoNetacad) =
        LectorNetacad.leer(ResultadoNetacad(pagina = "trabajos", candidatos = c.toList()), ahora, zona)

    private fun candidato(titulo: String, fecha: String = "", estado: String = "", nota: String = "", curso: String = "CCNA1") =
        CandidatoNetacad(titulo = titulo, curso = curso, fecha = fecha, estado = estado, nota = nota)

    @Test fun `lo que vence mas adelante esta pendiente`() {
        val t = leer(candidato("Examen capítulo 3", fecha = "05/12/2025")).single()
        assertEquals(EstadoActividad.PENDIENTE, t.estado)
    }

    @Test fun `lo que ya paso sin hacer esta fuera de plazo`() {
        val t = leer(candidato("Práctica PT 2.3.8", fecha = "01/11/2025")).single()
        assertEquals(EstadoActividad.NO_ENTREGADA, t.estado)
        assertTrue(t.vencido)
    }

    @Test fun `completado manda aunque la fecha haya pasado`() {
        val t = leer(candidato("Práctica PT 2.3.8", fecha = "01/11/2025", estado = "Completado")).single()
        assertEquals(EstadoActividad.ENTREGADA, t.estado)
    }

    @Test fun `incomplete no se confunde con complete`() {
        // «incomplete» contiene «complete»: mirando primero lo hecho se daba por terminado.
        val t = leer(candidato("Quiz", fecha = "01/12/2025", estado = "Incomplete")).single()
        assertEquals(EstadoActividad.PENDIENTE, t.estado)
    }

    @Test fun `no entregado no se confunde con entregado`() {
        val t = leer(candidato("Quiz", fecha = "01/11/2025", estado = "No entregado")).single()
        assertEquals(EstadoActividad.NO_ENTREGADA, t.estado)
    }

    @Test fun `una nota por encima de cero es que se hizo`() {
        val t = leer(candidato("Examen final", fecha = "01/11/2025", nota = "85%")).single()
        assertEquals(EstadoActividad.ENTREGADA, t.estado)
        assertEquals("85%", t.nota)
    }

    @Test fun `un cero no afirma nada y decide la fecha`() {
        val t = leer(candidato("Examen final", fecha = "01/11/2025", nota = "0%")).single()
        assertEquals(EstadoActividad.NO_ENTREGADA, t.estado)
    }

    @Test fun `puntos sobre maximo cuentan como hecho`() {
        assertEquals(EstadoActividad.ENTREGADA, leer(candidato("Quiz", nota = "18/20")).single().estado)
    }

    @Test fun `una fecha colada en el estado no es una nota`() {
        // «5/12/2025» no es «5 sobre 12».
        assertNull(LectorNetacad.marcaDeEstado(CandidatoNetacad(estado = "5/12/2025")))
    }

    @Test fun `el ingles de NetAcad tambien se entiende`() {
        val t = leer(
            candidato("Chapter 4 Exam", fecha = "Dec 5, 2025 11:59 PM", estado = "Not started"),
            candidato("Packet Tracer 1.1", fecha = "Nov 1, 2025", estado = "Passed")
        )
        assertEquals(EstadoActividad.PENDIENTE, t.first { it.titulo == "Chapter 4 Exam" }.estado)
        assertEquals(EstadoActividad.ENTREGADA, t.first { it.titulo == "Packet Tracer 1.1" }.estado)
    }

    @Test fun `sin fecha ni estado es ruido y se tira`() {
        assertTrue(leer(candidato("Inicio"), candidato("Mis cursos")).isEmpty())
    }

    @Test fun `un titulo diminuto es maquetacion`() {
        assertTrue(leer(candidato("x", fecha = "05/12/2025")).isEmpty())
    }

    @Test fun `la misma actividad repetida sale una vez y con la fecha`() {
        val t = leer(
            candidato("Examen capítulo 3", estado = "Pendiente"),
            candidato("Examen  Capítulo 3", fecha = "05/12/2025")
        )
        assertEquals(1, t.size)
        assertTrue(t.single().fechaLimite != null)
    }

    @Test fun `el orden americano se deduce de las otras fechas`() {
        // 11/25 solo puede ser 25 de noviembre: todas las fechas van mes primero.
        val t = leer(
            candidato("Examen A", fecha = "11/25/2025"),
            candidato("Examen B", fecha = "12/05/2025")
        )
        val b = t.first { it.titulo == "Examen B" }
        val dia = java.time.LocalDate.ofInstant(java.time.Instant.ofEpochSecond(b.fechaLimite!!), zona)
        assertEquals(java.time.LocalDate.of(2025, 12, 5), dia)
    }

    @Test fun `mismo titulo en dos cursos son dos trabajos`() {
        val t = leer(
            candidato("Examen final", fecha = "05/12/2025", curso = "CCNA1"),
            candidato("Examen final", fecha = "06/12/2025", curso = "CCNA2")
        )
        assertEquals(2, t.size)
    }

    @Test fun `se agrupa por plazo como en Moodle`() {
        val t = leer(
            candidato("Vencido", fecha = "01/11/2025"),
            candidato("Hoy", fecha = "20/11/2025"),
            candidato("Semana", fecha = "24/11/2025"),
            candidato("Mes", fecha = "10/12/2025"),
            candidato("Lejos", fecha = "01/03/2026"),
            candidato("SinFecha", estado = "Pendiente")
        )
        val grupos = ResumenNetacad.agrupar(t, ahora, zona).map { it.grupo }
        assertEquals(
            listOf(GrupoPlazo.VENCIDA, GrupoPlazo.HOY, GrupoPlazo.ESTA_SEMANA,
                GrupoPlazo.ESTE_MES, GrupoPlazo.MAS_ADELANTE, GrupoPlazo.SIN_FECHA),
            grupos
        )
    }

    @Test fun `lo de hoy no sale vencido por la manana`() {
        // Sin hora se toma el final del día: a las 10:00 lo del día 20 sigue en plazo.
        assertEquals(EstadoActividad.PENDIENTE, leer(candidato("Hoy", fecha = "20/11/2025")).single().estado)
    }

    @Test fun `filtros y resumen`() {
        val t = leer(
            candidato("Pendiente uno", fecha = "05/12/2025"),
            candidato("Vencido uno", fecha = "01/11/2025"),
            candidato("Hecho uno", fecha = "01/11/2025", estado = "Completed")
        )
        assertEquals(listOf("Pendiente uno"), ResumenNetacad.filtrar(t, FiltroNetacad.PENDIENTES).map { it.titulo })
        assertEquals(listOf("Vencido uno"), ResumenNetacad.filtrar(t, FiltroNetacad.VENCIDOS).map { it.titulo })
        assertEquals(listOf("Hecho uno"), ResumenNetacad.filtrar(t, FiltroNetacad.HECHOS).map { it.titulo })
        assertEquals(3, ResumenNetacad.filtrar(t, FiltroNetacad.TODOS).size)
        val r = ResumenNetacad.resumir(t)
        assertEquals(1, r.pendientes); assertEquals(1, r.noEntregadas); assertEquals(1, r.entregadas)
    }

    @Test fun `solo avisa de lo nuevo y nunca en la primera lectura`() {
        val antes = leer(candidato("Examen A", fecha = "05/12/2025"))
        val despues = leer(candidato("Examen A", fecha = "05/12/2025"), candidato("Examen B", fecha = "06/12/2025"))
        assertEquals(listOf("Examen B"), ResumenNetacad.recienPuestos(antes, despues).map { it.titulo })
        assertTrue(ResumenNetacad.recienPuestos(emptyList(), despues).isEmpty())
    }

    @Test fun `por vencer ignora lo hecho y lo pasado`() {
        val t = leer(
            candidato("Pronto", fecha = "21/11/2025"),
            candidato("HechoPronto", fecha = "21/11/2025", estado = "Done"),
            candidato("Pasado", fecha = "01/11/2025"),
            candidato("Lejos", fecha = "01/03/2026")
        )
        val aviso = ResumenNetacad.porVencer(t, ahora, 3 * 86_400L).map { it.titulo }
        assertEquals(listOf("Pronto"), aviso)
    }

    @Test fun `el tope evita llenar el almacen`() {
        val muchos = (1..1000).map { candidato("Trabajo número $it", fecha = "05/12/2025") }
        assertFalse(leer(*muchos.toTypedArray()).size > 400)
    }
}
