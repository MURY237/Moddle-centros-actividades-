package com.asir.moodleactividades.domain

import kotlinx.serialization.Serializable

@Serializable
data class Falta(
    val fecha: String,
    val tramo: String,
    val asignatura: String,
    val estado: String,
    /** Sale de repartir un «Día completo» entre las clases de ese día; Séneca no la trae así. */
    val deDiaCompleto: Boolean = false
) {
    /**
     * Séneca apunta el día entero en una sola fila, sin asignatura y con «Día completo» como
     * tramo. Hasta repartirla entre las clases del día no cuenta para ninguna asignatura.
     */
    val esDiaCompleto: Boolean
        get() = asignatura.isBlank()

    /** Séneca escribe «Justificada» o «Injustificada»; lo segundo empieza por «In». */
    val justificada: Boolean
        get() = estado.trim().lowercase().startsWith("justific")

    /**
     * La fecha llega como `dd/MM/yyyy`. Se convierte a un número comparable en vez de a una
     * fecha real: solo hace falta para ordenar, y así un formato inesperado no rompe nada.
     */
    val ordenable: Long
        get() = fechaOrdenable(fecha)
}

/** `dd/MM/yyyy` → `yyyyMMdd` como número, para ordenar; 0 si el formato no es ese. */
fun fechaOrdenable(fecha: String): Long {
    val partes = fecha.trim().split('/')
    if (partes.size != 3) return 0
    val dia = partes[0].trim().toLongOrNull() ?: return 0
    val mes = partes[1].trim().toLongOrNull() ?: return 0
    val anio = partes[2].trim().toLongOrNull() ?: return 0
    return anio * 10_000 + mes * 100 + dia
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
     * Cambia cada «Día completo» rellenado por una falta en cada clase que el alumno ha puesto
     * para esa fecha, con el mismo estado. Los tramos que ya tienen su propia falta ese día no
     * se cuentan dos veces. Uno sin rellenar se queda como estaba y no suma en ninguna.
     */
    fun repartirDiasCompletos(faltas: List<Falta>, rellenos: DiasCompletos): List<Falta> {
        val propias = faltas
            .filter { !it.esDiaCompleto }
            .groupBy({ it.fecha.trim() }, { it.tramo.trim() })

        return faltas.flatMap { falta ->
            if (!falta.esDiaCompleto) return@flatMap listOf(falta)

            val clases = rellenos.de(falta.fecha) ?: return@flatMap listOf(falta)

            val yaApuntadas = propias[falta.fecha.trim()].orEmpty().toSet()
            clases
                .filterKeys { it !in yaApuntadas }
                .entries
                .sortedBy { HorarioClases.minutosDeInicio(it.key) ?: Int.MAX_VALUE }
                .map { (tramo, asignatura) ->
                    Falta(
                        fecha = falta.fecha,
                        tramo = tramo,
                        asignatura = asignatura,
                        estado = falta.estado,
                        deDiaCompleto = true
                    )
                }
        }
    }

    /** Las faltas de día completo, la más reciente primero. */
    fun diasCompletos(faltas: List<Falta>): List<Falta> =
        faltas.filter { it.esDiaCompleto }.sortedByDescending { it.ordenable }

    /** Los días completos que el alumno aún no ha rellenado: no suman en ninguna asignatura. */
    fun sinRellenar(faltas: List<Falta>, rellenos: DiasCompletos): List<Falta> =
        diasCompletos(faltas).filterNot { rellenos.relleno(it.fecha) }

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
