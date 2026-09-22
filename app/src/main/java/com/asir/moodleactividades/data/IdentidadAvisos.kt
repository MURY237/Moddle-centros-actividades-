package com.asir.moodleactividades.data

/**
 * Cada aviso necesita un identificador propio porque la lista de la pantalla distingue unos
 * de otros por él, y dos con la misma clave tumban la aplicación entera.
 *
 * La hora y el título no bastan: cinco faltas de la misma asignatura se anotan en el mismo
 * segundo y comparten las dos cosas. De ahí que se mire también el texto, y que aun así se
 * lleve la cuenta de los repetidos, porque dos avisos idénticos siguen siendo posibles.
 */
object IdentidadAvisos {

    /** Sale del contenido y no de la posición, para que no cambie al llegar otro por encima. */
    fun de(aviso: Aviso): String =
        aviso.momento.toString() + "-" + aviso.tipo.name + "-" +
            (aviso.titulo + "|" + aviso.texto).hashCode()

    /**
     * Le pone identificador a los que no lo traen y desempata los que coinciden. Se aplica al
     * leer, no solo al guardar, porque en el historial ya hay avisos escritos antes de que
     * esto existiera y son justo los que hacían que la pantalla se cerrase sola.
     */
    fun unicos(avisos: List<Aviso>): List<Aviso> {
        val usados = HashSet<String>()
        return avisos.map { aviso ->
            val base = aviso.id.ifBlank { de(aviso) }
            var id = base
            var repeticion = 1
            while (!usados.add(id)) {
                id = base + "-" + repeticion
                repeticion++
            }
            if (id == aviso.id) aviso else aviso.copy(id = id)
        }
    }
}
