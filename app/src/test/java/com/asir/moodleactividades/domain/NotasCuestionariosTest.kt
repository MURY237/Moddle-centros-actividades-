package com.asir.moodleactividades.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class NotasCuestionariosTest {

    private val enlace = "https://moodle.example/mod/quiz/view.php?id=439863"

    private val examen = Actividad(
        id = 81,
        nombre = "Examen UD1",
        curso = "Administración de sistemas",
        tipo = TipoActividad.CUESTIONARIO,
        fechaLimite = 1_761_815_700,
        estado = EstadoActividad.ENTREGADA,
        calificada = true,
        url = enlace,
        nota = "6,51",
        notaMaxima = 10.0
    )

    private fun calificacion(nombre: String, nota: String = "", url: String? = null, maxima: Double = 10.0) =
        Calificacion(
            curso = "Administración de sistemas",
            nombre = nombre,
            nota = nota,
            porcentaje = "",
            notaMaxima = maxima,
            esTotalDelCurso = false,
            tipo = TipoActividad.CUESTIONARIO,
            url = url
        )

    private fun curso(vararg calificaciones: Calificacion) =
        NotasDeCurso(curso = "Administración de sistemas", total = null, calificaciones = calificaciones.toList())

    @Test
    fun `el examen que falta en el libro se anade con su nota y sobre cuanto es`() {
        val resultado = NotasCuestionarios.completar(listOf(curso(calificacion("Práctica 1", "8"))), listOf(examen))

        val anadida = resultado.single().calificaciones.single { it.nombre == "Examen UD1" }
        assertEquals("6,51", anadida.nota)
        assertEquals(10.0, anadida.notaMaxima, 0.0)
        assertEquals(enlace, anadida.url)
        assertEquals(2, resultado.single().calificaciones.size)
    }

    @Test
    fun `si el libro lo trae sin nota se rellena en vez de duplicarlo`() {
        val resultado = NotasCuestionarios.completar(listOf(curso(calificacion("Examen UD1", url = enlace))), listOf(examen))

        assertEquals(listOf("6,51"), resultado.single().calificaciones.map { it.nota })
    }

    @Test
    fun `la nota del libro manda sobre la del cuestionario`() {
        val delLibro = calificacion("Examen UD1", nota = "7", url = enlace)
        val resultado = NotasCuestionarios.completar(listOf(curso(delLibro)), listOf(examen))

        assertEquals(listOf(delLibro), resultado.single().calificaciones)
    }

    @Test
    fun `sin enlace en el libro se reconoce por el nombre`() {
        val resultado = NotasCuestionarios.completar(listOf(curso(calificacion("Examen UD1"))), listOf(examen))

        assertEquals(listOf("6,51"), resultado.single().calificaciones.map { it.nota })
    }

    @Test
    fun `sin cuestionarios con nota no se toca nada`() {
        val cursos = listOf(curso(calificacion("Práctica 1", "8")))

        assertSame(cursos, NotasCuestionarios.completar(cursos, listOf(examen.copy(nota = null))))
        assertSame(cursos[0], NotasCuestionarios.completar(cursos, listOf(examen.copy(curso = "Otra")))[0])
    }
}
