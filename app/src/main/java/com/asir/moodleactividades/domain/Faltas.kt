package com.asir.moodleactividades.domain

import kotlinx.serialization.Serializable

@Serializable
data class Falta(
    val fecha: String,
    val tramo: String,
    val asignatura: String,
    val estado: String
) {
    /** Séneca escribe «Justificada» o «Injustificada»; lo segundo empieza por «In». */
    val justificada: Boolean
        get() = estado.trim().lowercase().startsWith("justific")

    /**
     * La fecha llega como `dd/MM/yyyy`. Se convierte a un número comparable en vez de a una
     * fecha real: solo hace falta para ordenar, y así un formato inesperado no rompe nada.
     */
    val ordenable: Long
        get() {
            val partes = fecha.trim().split('/')
            if (partes.size != 3) return 0
            val dia = partes[0].toLongOrNull() ?: return 0
            val mes = partes[1].toLongOrNull() ?: return 0
            val anio = partes[2].toLongOrNull() ?: return 0
            return anio * 10_000 + mes * 100 + dia
        }
}

data class FaltasDeAsignatura(
    val asignatura: String,
    val faltas: List<Falta>
) {
    val total: Int get() = faltas.size
    val justificadas: Int get() = faltas.count { it.justificada }
    val injustificadas: Int get() = total - justificadas
}

object ResumenFaltas {

    /**
     * Agrupa por asignatura y pone delante la que más faltas acumula, que es la que interesa
     * vigilar. A igualdad, orden alfabético para que la lista no baile entre consultas.
     */
    fun porAsignatura(faltas: List<Falta>): List<FaltasDeAsignatura> =
        faltas
            .filter { it.asignatura.isNotBlank() }
            .groupBy { it.asignatura }
            .map { (asignatura, suyas) ->
                FaltasDeAsignatura(asignatura, suyas.sortedByDescending { it.ordenable })
            }
            .sortedWith(compareByDescending<FaltasDeAsignatura> { it.total }.thenBy { it.asignatura })

    fun totalInjustificadas(faltas: List<Falta>): Int = faltas.count { !it.justificada }

    /**
     * Las faltas que no estaban en la consulta anterior. Sirve para avisar de lo nuevo sin
     * repetir lo ya visto; con la lista previa vacía no hay nada con qué comparar.
     */
    fun recienPuestas(antes: List<Falta>, ahora: List<Falta>): List<Falta> {
        if (antes.isEmpty()) return emptyList()
        val conocidas = antes.mapTo(mutableSetOf()) { clave(it) }
        return ahora.filterNot { clave(it) in conocidas }
    }

    /** Una falta no trae identificador: la distinguen su día, su hora y su asignatura. */
    private fun clave(falta: Falta) =
        falta.fecha + "|" + falta.tramo + "|" + falta.asignatura
}
