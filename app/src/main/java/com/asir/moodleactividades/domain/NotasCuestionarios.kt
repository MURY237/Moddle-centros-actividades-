package com.asir.moodleactividades.domain

/**
 * Las notas de los cuestionarios (exámenes tipo test) se leen de su propia API, que las da
 * aunque el libro de calificaciones no las enseñe: porque el profesor oculta la columna,
 * porque aún no la ha añadido o porque el centro no deja leer el libro.
 */
object NotasCuestionarios {

    fun comoCalificacion(actividad: Actividad) = Calificacion(
        curso = actividad.curso,
        nombre = actividad.nombre,
        nota = actividad.nota.orEmpty(),
        porcentaje = "",
        // Sin el libro de calificaciones, la actividad es lo único que dice sobre cuánto
        // es: sin esto, un 1,80 sobre 2 se daba por suspenso.
        notaMaxima = actividad.notaMaxima ?: 0.0,
        esTotalDelCurso = false,
        tipo = actividad.tipo,
        fecha = actividad.fechaLimite,
        url = actividad.url
    )

    /**
     * Añade a lo leído del libro los cuestionarios con nota que falten en él, y rellena los
     * que sí están pero sin nota. Un cuestionario se reconoce por su enlace o, si el libro no
     * lo trae, por asignatura y nombre.
     */
    fun completar(cursos: List<NotasDeCurso>, actividades: List<Actividad>): List<NotasDeCurso> {
        val porCurso = actividades
            .filter { it.tipo == TipoActividad.CUESTIONARIO && !it.nota.isNullOrBlank() }
            .groupBy { it.curso }
        if (porCurso.isEmpty()) return cursos

        return cursos.map { curso ->
            val cuestionarios = porCurso[curso.curso].orEmpty()
            if (cuestionarios.isEmpty()) return@map curso

            val usados = mutableSetOf<Actividad>()
            val rellenas = curso.calificaciones.map { calificacion ->
                val propio = cuestionarios.firstOrNull { it.mismoQue(calificacion) }
                    ?: return@map calificacion
                usados += propio
                if (calificacion.calificada) {
                    calificacion
                } else {
                    calificacion.copy(
                        nota = propio.nota.orEmpty(),
                        notaMaxima = calificacion.notaMaxima.takeIf { it > 0 } ?: propio.notaMaxima ?: 0.0
                    )
                }
            }
            val faltan = cuestionarios.filter { it !in usados }.map(::comoCalificacion)
            if (faltan.isEmpty() && rellenas == curso.calificaciones) {
                curso
            } else {
                curso.copy(
                    calificaciones = (rellenas + faltan)
                        .sortedWith(compareByDescending<Calificacion> { it.fecha ?: Long.MIN_VALUE }.thenBy { it.nombre })
                )
            }
        }
    }

    private fun Actividad.mismoQue(calificacion: Calificacion): Boolean =
        if (!calificacion.url.isNullOrBlank()) {
            calificacion.url == url
        } else {
            calificacion.tipo == TipoActividad.CUESTIONARIO && calificacion.nombre.trim() == nombre.trim()
        }
}
