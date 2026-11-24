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
     * Cambia cada «Día completo» por una falta en cada clase de ese día, con el mismo estado.
     * Los tramos que ya tienen su propia falta ese día no se cuentan dos veces. Si no se sabe
     * qué clases hay ese día, se queda como estaba: ver [sinRepartir].
     */
    fun repartirDiasCompletos(faltas: List<Falta>, horario: HorarioClases): List<Falta> {
        val propias = faltas
            .filter { !it.esDiaCompleto }
            .groupBy({ it.fecha.trim() }, { it.tramo.trim() })

        return faltas.flatMap { falta ->
            if (!falta.esDiaCompleto) return@flatMap listOf(falta)

            val clases = HorarioClases.diaDeLaSemana(falta.fecha)?.let(horario::de).orEmpty()
            if (clases.isEmpty()) return@flatMap listOf(falta)

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

    /** Los días completos que no se han podido repartir porque falta el horario de ese día. */
    fun sinRepartir(repartidas: List<Falta>): List<Falta> = repartidas.filter { it.esDiaCompleto }

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
