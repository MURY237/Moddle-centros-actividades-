package com.asir.moodleactividades.data.netacad

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * NetAcad escribe las fechas de entrega de maneras distintas según el idioma del curso y la
 * plantilla de la página: «05/12/2025», «Dec 5, 2025», «5 de diciembre de 2025», «2025-12-05»
 * o incluso «hoy». Aquí se convierten todas a segundos desde época, que es lo que maneja el
 * resto de la app.
 *
 * Todo el trabajo delicado vive en Kotlin y no en el guion de JavaScript a propósito: así se
 * puede probar entero sin un navegador ni una cuenta de Cisco delante. El guion solo recoge
 * el texto tal cual lo pinta la página.
 */
object FechaNetacad {

    /**
     * «05/12/2025» es 5 de diciembre en español y 12 de mayo en inglés, y la página no dice
     * cuál de los dos usa. Se deduce mirando todas las fechas juntas, que es donde sí hay
     * pistas: un 25 en la primera posición solo puede ser un día.
     */
    enum class Orden { DIA_PRIMERO, MES_PRIMERO }

    /**
     * Nombres completos y abreviados, en los dos idiomas en que NetAcad pinta sus fechas.
     * Se listan enteros en vez de por prefijo porque «mar» + cualquier letra también casaba
     * con palabras como «marcador», y un título cualquiera pasaba por fecha.
     */
    private val MESES = mapOf(
        "enero" to 1, "ene" to 1, "january" to 1, "jan" to 1,
        "febrero" to 2, "feb" to 2, "february" to 2,
        "marzo" to 3, "mar" to 3, "march" to 3,
        "abril" to 4, "abr" to 4, "april" to 4, "apr" to 4,
        "mayo" to 5, "may" to 5,
        "junio" to 6, "jun" to 6, "june" to 6,
        "julio" to 7, "jul" to 7, "july" to 7,
        "agosto" to 8, "ago" to 8, "august" to 8, "aug" to 8,
        "septiembre" to 9, "setiembre" to 9, "september" to 9, "sept" to 9, "sep" to 9, "set" to 9,
        "octubre" to 10, "octubre" to 10, "october" to 10, "oct" to 10,
        "noviembre" to 11, "november" to 11, "nov" to 11,
        "diciembre" to 12, "december" to 12, "dic" to 12, "dec" to 12
    )

    // De más largo a más corto: si no, «sep» ganaría a «septiembre» y se comería el resto.
    private val MES_SUELTO = MESES.keys.sortedByDescending { it.length }.joinToString("|")

    private val ISO = Regex("""(\d{4})-(\d{1,2})-(\d{1,2})""")
    private val NUMERICA = Regex("""(?<!\d)(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{2,4})(?!\d)""")
    private val DIA_MES = Regex("""(?<!\d)(\d{1,2})\s*(?:de\s+)?\b($MES_SUELTO)\b\.?(?:\s*(?:de\s+|,\s*)?(\d{4}))?""")
    private val MES_DIA = Regex("""\b($MES_SUELTO)\b\.?\s+(\d{1,2})(?:st|nd|rd|th)?(?:\s*,?\s*(\d{4}))?""")
    private val HORA = Regex("""(?<!\d)(\d{1,2}):(\d{2})(?::\d{2})?\s*(a\.?\s?m\.?|p\.?\s?m\.?)?""")

    /** Quita tildes y sobra de espacios: «Diciembre» y «diciembre» tienen que valer igual. */
    fun normalizar(texto: String): String =
        java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
            .lowercase()
            .replace(Regex("""\s+"""), " ")
            .trim()

    /**
     * Mira todas las fechas numéricas a la vez y se queda con el orden que explique a todas.
     * Sin ninguna pista se supone día primero, que es como lo escribe NetAcad en español.
     */
    fun detectarOrden(textos: List<String>): Orden {
        var dia = 0
        var mes = 0
        for (texto in textos) {
            for (coincidencia in NUMERICA.findAll(normalizar(texto))) {
                val primero = coincidencia.groupValues[1].toIntOrNull() ?: continue
                val segundo = coincidencia.groupValues[2].toIntOrNull() ?: continue
                if (primero > 12 && segundo <= 12) dia++
                if (segundo > 12 && primero <= 12) mes++
            }
        }
        return if (mes > dia) Orden.MES_PRIMERO else Orden.DIA_PRIMERO
    }

    /**
     * Devuelve los segundos desde época, o null si en el texto no hay ninguna fecha. Cuando
     * el texto no trae hora se usa el final del día: una entrega «del 5» sigue estando en
     * plazo a las once de la noche del 5, y con las 00:00 aparecería vencida desde el minuto uno.
     */
    fun parsear(
        texto: String,
        ahora: Long,
        orden: Orden = Orden.DIA_PRIMERO,
        zona: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val limpio = normalizar(texto)
        if (limpio.isEmpty()) return null

        val hoy = LocalDate.ofInstant(java.time.Instant.ofEpochSecond(ahora), zona)
        val dia = fechaDe(limpio, hoy, orden) ?: return null
        val hora = horaDe(limpio)

        return dia.atTime(hora).atZone(zona).toEpochSecond()
    }

    private fun fechaDe(limpio: String, hoy: LocalDate, orden: Orden): LocalDate? {
        relativa(limpio, hoy)?.let { return it }

        ISO.find(limpio)?.let { c ->
            return armar(
                c.groupValues[3].toInt(), c.groupValues[2].toInt(), c.groupValues[1].toInt(), hoy
            )
        }

        NUMERICA.find(limpio)?.let { c ->
            val primero = c.groupValues[1].toInt()
            val segundo = c.groupValues[2].toInt()
            // Un número mayor que doce no puede ser un mes, diga lo que diga el orden general.
            val diaMes = when {
                primero > 12 -> primero to segundo
                segundo > 12 -> segundo to primero
                orden == Orden.MES_PRIMERO -> segundo to primero
                else -> primero to segundo
            }
            return armar(diaMes.first, diaMes.second, c.groupValues[3].toInt(), hoy)
        }

        DIA_MES.find(limpio)?.let { c ->
            val mes = MESES[c.groupValues[2]] ?: return@let
            return armar(c.groupValues[1].toInt(), mes, c.groupValues[3].toIntOrNull(), hoy)
        }

        MES_DIA.find(limpio)?.let { c ->
            val mes = MESES[c.groupValues[1]] ?: return@let
            return armar(c.groupValues[2].toInt(), mes, c.groupValues[3].toIntOrNull(), hoy)
        }

        return null
    }

    private fun relativa(limpio: String, hoy: LocalDate): LocalDate? = when {
        Regex("""\b(hoy|today)\b""").containsMatchIn(limpio) -> hoy
        Regex("""\b(manana|tomorrow)\b""").containsMatchIn(limpio) -> hoy.plusDays(1)
        Regex("""\b(ayer|yesterday)\b""").containsMatchIn(limpio) -> hoy.minusDays(1)
        else -> null
    }

    /**
     * Un año de dos cifras es de este siglo. Sin año, NetAcad se refiere al curso en marcha:
     * se toma el actual y, si eso deja la fecha medio año atrás, es que ya es del siguiente.
     */
    private fun armar(dia: Int, mes: Int, anio: Int?, hoy: LocalDate): LocalDate? {
        if (mes !in 1..12 || dia !in 1..31) return null

        val completo = when {
            anio == null -> null
            anio < 100 -> anio + 2000
            else -> anio
        }

        val base = runCatching { LocalDate.of(completo ?: hoy.year, mes, dia) }.getOrNull()
            ?: return null
        if (completo != null) return base

        return if (base.isBefore(hoy.minusDays(180))) base.plusYears(1) else base
    }

    /** Sin hora en el texto, el final del día: una entrega del día 5 vence al acabar el 5. */
    private fun horaDe(limpio: String): LocalTime {
        val c = HORA.find(limpio) ?: return LocalTime.of(23, 59)
        var hora = c.groupValues[1].toIntOrNull() ?: return LocalTime.of(23, 59)
        val minuto = c.groupValues[2].toIntOrNull() ?: return LocalTime.of(23, 59)
        val sufijo = c.groupValues[3].replace(Regex("""[.\s]"""), "")

        if (sufijo == "pm" && hora < 12) hora += 12
        if (sufijo == "am" && hora == 12) hora = 0
        if (hora !in 0..23 || minuto !in 0..59) return LocalTime.of(23, 59)
        return LocalTime.of(hora, minuto)
    }
}
