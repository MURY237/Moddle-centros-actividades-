package com.asir.moodleactividades.domain

import kotlinx.serialization.Serializable
import java.text.Normalizer
import java.util.Locale

/**
 * Lo que el alumno corrige a mano de una nota. Moodle no siempre dice sobre cuánto es una
 * actividad, y sin ese dato un 1,80 sobre 2 se tomaba por un suspenso sobre 10.
 *
 * Se queda en el móvil: Moodle no se entera de nada.
 */
@Serializable
data class AjusteNota(
    /** La nota máxima real de la actividad; null para usar la de Moodle. */
    val notaMaxima: Double? = null,
    /** El resultado que ha decidido el alumno; null para calcularlo. */
    val aprobada: Boolean? = null
) {
    val vacio: Boolean get() = notaMaxima == null && aprobada == null
}

/** De dónde sale el veredicto: la pantalla lo explica para que se sepa cuánto fiarse. */
enum class OrigenVeredicto {
    /** El alumno ha marcado el resultado. */
    MANUAL,

    /** Calculado con la nota máxima que ha indicado el alumno. */
    MAXIMA_PROPIA,

    /** Calculado con el porcentaje de Moodle, que ya tiene en cuenta el mínimo y el máximo. */
    PORCENTAJE,

    /** Calculado con la nota máxima de Moodle. */
    MAXIMA_MOODLE,

    /** Moodle no dice el máximo y se supone sobre 10. */
    SUPUESTO,

    /** Una nota con palabras: «Apto», «No apto», «Superado»… */
    TEXTO,

    /** No hay forma de saberlo, o todavía no hay nota. */
    DESCONOCIDO
}

data class Veredicto(
    /** Null cuando no se puede decidir. */
    val aprobada: Boolean?,
    val origen: OrigenVeredicto,
    /** La nota máxima que se conoce o se ha usado para decidir. */
    val notaMaxima: Double? = null
)

object Aprobado {

    /** Lo habitual cuando nadie dice otra cosa. */
    const val MAXIMA_SUPUESTA = 10.0

    private val NUMERO = Regex("""-?\d+(?:[.,]\d+)?""")
    private val ESPANOL = Locale.forLanguageTag("es-ES")

    // Las negativas van antes: «No apto» contiene «apto» e «Insuficiente», «suficiente».
    private val SUSPENSO = Regex("""no apto|no superad|no aprobad|suspens|insuficiente|no satisfactori|\bfail""")
    private val APROBADO = Regex("""apto|superad|aprobad|suficiente|\bbien\b|notable|sobresaliente|satisfactori|\bpass""")

    /**
     * Por orden: lo que haya marcado el alumno, la nota máxima que haya indicado, el
     * porcentaje de Moodle, la nota máxima de Moodle y, si no hay nada, sobre 10.
     */
    fun evaluar(calificacion: Calificacion, ajuste: AjusteNota? = null): Veredicto {
        val maximaPropia = ajuste?.notaMaxima?.takeIf { it > 0 }
        val maximaMoodle = calificacion.notaMaxima.takeIf { it > 0 }
        val maxima = maximaPropia ?: maximaMoodle

        if (!calificacion.calificada) return Veredicto(null, OrigenVeredicto.DESCONOCIDO, maxima)
        ajuste?.aprobada?.let { return Veredicto(it, OrigenVeredicto.MANUAL, maxima) }

        val valor = valorDe(calificacion.nota) ?: return porTexto(calificacion.nota)
            ?.let { Veredicto(it, OrigenVeredicto.TEXTO, maxima) }
            ?: Veredicto(null, OrigenVeredicto.DESCONOCIDO, maxima)

        if (maximaPropia != null) {
            return Veredicto(valor >= maximaPropia / 2, OrigenVeredicto.MAXIMA_PROPIA, maximaPropia)
        }
        valorDe(calificacion.porcentaje)?.let { porcentaje ->
            return Veredicto(porcentaje >= 50, OrigenVeredicto.PORCENTAJE, maximaMoodle)
        }
        if (maximaMoodle != null) {
            return Veredicto(valor >= maximaMoodle / 2, OrigenVeredicto.MAXIMA_MOODLE, maximaMoodle)
        }
        // Por encima de 10 está claro que no es sobre 10, y suponer otra cosa sería inventar.
        if (valor > MAXIMA_SUPUESTA) return Veredicto(null, OrigenVeredicto.DESCONOCIDO)
        return Veredicto(valor >= MAXIMA_SUPUESTA / 2, OrigenVeredicto.SUPUESTO, MAXIMA_SUPUESTA)
    }

    /**
     * El primer número de lo que muestra Moodle. Un «7,50 (75,00 %)» es un 7,5: quitar todo
     * lo que no fuera cifra lo convertía en «7.5075.00», que no es nada.
     */
    fun valorDe(texto: String): Double? =
        NUMERO.find(texto)?.value?.replace(',', '.')?.toDoubleOrNull()

    /** Una nota máxima escrita por el alumno: «2», «2,5» o «2.5». */
    fun maximaEscrita(texto: String): Double? =
        texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it <= 10_000 }

    /** Las cifras como se escriben aquí: «2», «1,5», «1,25». */
    fun cifra(valor: Double): String =
        if (valor % 1.0 == 0.0) {
            valor.toLong().toString()
        } else {
            String.format(ESPANOL, "%.2f", valor).trimEnd('0').trimEnd(',')
        }

    private fun porTexto(nota: String): Boolean? {
        val limpio = Normalizer.normalize(nota.lowercase(ESPANOL), Normalizer.Form.NFD)
            .replace(Regex("""\p{M}"""), "")
        return when {
            SUSPENSO.containsMatchIn(limpio) -> false
            APROBADO.containsMatchIn(limpio) -> true
            else -> null
        }
    }
}
