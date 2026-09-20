package com.asir.moodleactividades.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ComparadorNotasTest {

    private fun nota(curso: String, nombre: String, nota: String, total: Boolean = false) =
        Calificacion(curso, nombre, nota, "", 10.0, total, TipoActividad.TAREA)

    private fun curso(nombre: String, vararg notas: Calificacion) =
        NotasDeCurso(nombre, null, notas.toList())

    private fun nombresPublicados(antes: List<NotasDeCurso>, ahora: List<NotasDeCurso>) =
        ComparadorNotas.recienPublicadas(antes, ahora).map { it.nombre }

    @Test
    fun `una tarea sin nota que pasa a tenerla se avisa`() {
        val antes = listOf(curso("Redes", nota("Redes", "Práctica 1", "")))
        val ahora = listOf(curso("Redes", nota("Redes", "Práctica 1", "8,00")))

        assertEquals(listOf("Práctica 1"), nombresPublicados(antes, ahora))
    }

    @Test
    fun `una nota que no cambia no se avisa`() {
        val antes = listOf(curso("Redes", nota("Redes", "Práctica 1", "8,00")))
        val ahora = listOf(curso("Redes", nota("Redes", "Práctica 1", "8,00")))

        assertEquals(emptyList<String>(), nombresPublicados(antes, ahora))
    }

    @Test
    fun `una actividad que aparece ya calificada no se avisa como nota nueva`() {
        val ahora = listOf(curso("Redes", nota("Redes", "Examen", "9,00")))

        assertEquals(emptyList<String>(), nombresPublicados(emptyList(), ahora))
    }

    @Test
    fun `el total del curso no genera aviso`() {
        val antes = listOf(curso("Redes", nota("Redes", "Total", "5,00", total = true)))
        val ahora = listOf(curso("Redes", nota("Redes", "Total", "7,00", total = true)))

        assertEquals(emptyList<String>(), nombresPublicados(antes, ahora))
    }

    @Test
    fun `una nota corregida al alza vuelve a avisar`() {
        val antes = listOf(curso("Redes", nota("Redes", "Práctica 1", "4,00")))
        val ahora = listOf(curso("Redes", nota("Redes", "Práctica 1", "7,50")))

        assertEquals(listOf("Práctica 1"), nombresPublicados(antes, ahora))
    }

    @Test
    fun `dos asignaturas con una actividad del mismo nombre no se confunden`() {
        val antes = listOf(
            curso("Redes", nota("Redes", "Examen", "")),
            curso("Bases de datos", nota("Bases de datos", "Examen", ""))
        )
        val ahora = listOf(
            curso("Redes", nota("Redes", "Examen", "")),
            curso("Bases de datos", nota("Bases de datos", "Examen", "6,00"))
        )

        assertEquals(listOf("Examen"), nombresPublicados(antes, ahora))
        assertEquals(
            listOf("Bases de datos"),
            ComparadorNotas.recienPublicadas(antes, ahora).map { it.curso }
        )
    }
}
