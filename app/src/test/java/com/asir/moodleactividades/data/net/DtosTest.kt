package com.asir.moodleactividades.data.net

import org.junit.Assert.assertEquals
import org.junit.Test

class DtosTest {

    @Test
    fun `la tarea trae sobre cuanto es`() {
        // Así lo devuelve mod_assign_get_assignments: entero, negativo si va con escala.
        val crudo = """
            {"courses":[{"id":7,"fullname":"Redes","assignments":[
              {"id":1,"cmid":10,"name":"Repaso de ACL","grade":2},
              {"id":2,"cmid":11,"name":"Con escala","grade":-3},
              {"id":3,"cmid":12,"name":"Antigua"}
            ]}]}
        """.trimIndent()

        val tareas = MoodleClient.json.decodeFromString<AssignmentsDto>(crudo).courses.single().assignments

        assertEquals(listOf(2.0, -3.0, 0.0), tareas.map { it.grade })
    }

    @Test
    fun `el cuestionario trae su cierre, su enlace y sobre cuanto es`() {
        // Recorte de mod_quiz_get_quizzes_by_courses; los campos que no se usan se ignoran.
        val crudo = """
            {"quizzes":[
              {"id":81,"course":7,"coursemodule":439863,"name":"Examen UD1","intro":"<p>Tipo test</p>",
               "introformat":1,"timeopen":1761725700,"timeclose":1761815700,"grade":10,"sumgrades":20,
               "grademethod":1,"attempts":1},
              {"id":82,"course":7,"coursemodule":439900,"name":"Sin cierre"}
            ],"warnings":[]}
        """.trimIndent()

        val cuestionarios = MoodleClient.json.decodeFromString<CuestionariosDto>(crudo).quizzes

        assertEquals(listOf(81L, 82L), cuestionarios.map { it.id })
        assertEquals(439863L, cuestionarios[0].coursemodule)
        assertEquals(1761815700L, cuestionarios[0].timeclose)
        assertEquals(10.0, cuestionarios[0].grade, 0.0)
        assertEquals(0L, cuestionarios[1].timeclose)
    }

    @Test
    fun `los intentos y la mejor nota del cuestionario se leen tal cual`() {
        val intentos = MoodleClient.json.decodeFromString<IntentosCuestionarioDto>(
            """{"attempts":[{"id":5,"quiz":81,"attempt":1,"state":"finished","sumgrades":13.02}],"warnings":[]}"""
        )
        val conNota = MoodleClient.json.decodeFromString<MejorNotaCuestionarioDto>(
            """{"hasgrade":true,"grade":6.51,"gradetopass":5,"warnings":[]}"""
        )
        // Si las opciones de revisión ocultan la nota, Moodle no manda «grade».
        val oculta = MoodleClient.json.decodeFromString<MejorNotaCuestionarioDto>(
            """{"hasgrade":false,"warnings":[]}"""
        )

        assertEquals(listOf("finished"), intentos.attempts.map { it.state })
        assertEquals(6.51, conNota.grade!!, 0.0)
        assertEquals(false, oculta.hasgrade)
        assertEquals(null, oculta.grade)
    }
}
