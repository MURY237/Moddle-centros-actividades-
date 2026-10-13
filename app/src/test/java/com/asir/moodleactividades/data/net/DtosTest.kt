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
}
