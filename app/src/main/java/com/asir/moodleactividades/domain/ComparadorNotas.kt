package com.asir.moodleactividades.domain

/**
 * Decide qué calificaciones acaba de publicar el profesorado comparando la consulta de ahora
 * con la copia guardada de la anterior. Va aquí, y no dentro del trabajo en segundo plano,
 * para que sea lógica pura y los tests puedan cubrirla.
 */
object ComparadorNotas {

    /**
     * Una nota es nueva cuando esa misma actividad ya se conocía y su calificación ha cambiado.
     * Lo que aparece por primera vez no cuenta: eso es una actividad nueva, no una corrección,
     * y en la primera sincronización avisaría del curso entero de golpe.
     */
    fun recienPublicadas(
        antes: List<NotasDeCurso>,
        ahora: List<NotasDeCurso>
    ): List<Calificacion> {
        val previas = antes.flatMap { it.calificaciones }.associate { clave(it) to it.nota }
        return ahora.flatMap { it.calificaciones }
            // El total del curso se mueve con cada nota; avisar de él sería avisar dos veces.
            .filterNot { it.esTotalDelCurso }
            .filter { it.calificada }
            .filter { actual ->
                val anterior = previas[clave(actual)]
                anterior != null && anterior != actual.nota
            }
    }

    /** Las calificaciones no traen identificador, así que la clave es dónde y cómo se llaman. */
    private fun clave(calificacion: Calificacion) =
        calificacion.curso + "|" + calificacion.nombre
}
