package com.asir.moodleactividades.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class DiagnosticoCuestionariosTest {

    private val zona: ZoneId = ZoneId.of("Europe/Madrid")
    private val ahora = 1_763_840_000L

    private fun diagnostico(
        errorLista: String? = null,
        revisados: List<DiagnosticoCuestionarios.Fila> = emptyList()
    ) = DiagnosticoCuestionarios(
        version = "2.7.1",
        funciones = mapOf(
            "mod_quiz_get_quizzes_by_courses" to true,
            "mod_quiz_get_user_attempts" to true,
            "mod_quiz_get_user_best_grade" to false
        ),
        ultimaCarga = ahora - 180,
        cargaConApi = true,
        enLaLista = 3,
        errorLista = errorLista,
        total = revisados.size,
        revisados = revisados,
        ahora = ahora
    )

    @Test
    fun `cada examen dice sus intentos y su nota`() {
        val texto = diagnostico(
            revisados = listOf(
                DiagnosticoCuestionarios.Fila(
                    nombre = "Examen UD1",
                    curso = "Administración de sistemas",
                    cierre = 1_761_815_700,
                    intentos = listOf("finished"),
                    nota = "6,51",
                    notaMaxima = 10.0,
                    notaOculta = false,
                    error = null
                ),
                DiagnosticoCuestionarios.Fila(
                    nombre = "Repaso",
                    curso = "Redes",
                    cierre = null,
                    intentos = listOf("finished"),
                    nota = null,
                    notaMaxima = 10.0,
                    notaOculta = true,
                    error = null
                )
            )
        ).comoTexto(zona)

        assertTrue(texto, texto.startsWith("Actividades Moodle 2.7.1"))
        assertTrue(texto, "hace 3 min · exámenes leídos por su API: sí · 3 en la lista" in texto)
        assertTrue(texto, "get_quizzes_by_courses sí · get_user_attempts sí · get_user_best_grade no" in texto)
        assertTrue(texto, "• Examen UD1 · Administración de sistemas · cierra 30/10/2025 10:15" in texto)
        assertTrue(texto, "intentos: finished · nota: 6,51 / 10" in texto)
        assertTrue(texto, "• Repaso · Redes · sin cierre" in texto)
        assertTrue(texto, "nota: Moodle no deja verla" in texto)
    }

    @Test
    fun `si Moodle no da la lista se dice por que y nada mas`() {
        val texto = diagnostico(errorLista = "accessexception: Excepción al control de acceso").comoTexto(zona)

        assertTrue(texto, texto.endsWith("Moodle no devuelve los cuestionarios: accessexception: Excepción al control de acceso"))
        assertFalse(texto, "•" in texto)
    }

    @Test
    fun `sin carga previa ni lista de funciones tambien se entiende`() {
        val texto = diagnostico().copy(ultimaCarga = null, funciones = null).comoTexto(zona)

        assertTrue(texto, "Última carga de Tareas: ninguna" in texto)
        assertTrue(texto, "el centro no publica la lista" in texto)
        assertEquals(true, texto.contains("Moodle devuelve 0 cuestionarios:"))
    }
}
