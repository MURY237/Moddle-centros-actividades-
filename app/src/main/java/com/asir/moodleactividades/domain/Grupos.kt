package com.asir.moodleactividades.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlinx.serialization.Serializable

/*
 * Los nombres de los campos son los de las columnas de supabase/esquema.sql: así llegan de
 * la API y así se decodifican, sin capa de traducción en medio. Las fechas se guardan como
 * vienen (texto ISO) y se interpretan al usarlas: un formato inesperado deja un campo vacío
 * en vez de tumbar la lista entera.
 */

@Serializable
data class Grupo(
    val id: String,
    val nombre: String,
    val codigo: String,
    val creador: String,
    val creado: String = ""
)

@Serializable
data class Miembro(
    val grupo: String = "",
    val usuario: String,
    val apodo: String,
    val unido: String = ""
)

@Serializable
data class Mensaje(
    val id: Long,
    val grupo: String = "",
    val autor: String,
    val texto: String,
    val enviado: String
) {
    /** Segundos desde época; null si el servidor mandó algo que no es una fecha. */
    val momento: Long?
        get() = runCatching { OffsetDateTime.parse(enviado).toEpochSecond() }.getOrNull()
}

@Serializable
data class Examen(
    val id: Long,
    val grupo: String = "",
    val autor: String,
    val asignatura: String,
    /** `yyyy-MM-dd`, como lo devuelve una columna `date`. */
    val fecha: String,
    /** `HH:mm:ss`, o null si no se puso hora. */
    val hora: String? = null,
    val notas: String = "",
    val creado: String = ""
) {
    val dia: LocalDate? get() = runCatching { LocalDate.parse(fecha) }.getOrNull()

    val horaLocal: LocalTime? get() = hora?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
}

/**
 * El código de invitación de un grupo. Lo genera el servidor; aquí solo se limpia lo que
 * escribe el alumno y se presenta. El alfabeto es el de Crockford: 32 símbolos, sin I, L,
 * O ni U, que se confunden al dictarlos. Si alguien escribe una O se entiende un 0, y una I
 * o una L se entienden un 1, que es lo que habrá querido decir.
 *
 * La normalización es la misma que hace `normalizar_codigo` en el servidor: la de aquí solo
 * sirve para avisar antes de enviar; quien decide es el servidor.
 */
object CodigoInvitacion {

    const val ALFABETO = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    const val LONGITUD = 8

    fun normalizar(texto: String): String =
        texto.filter { it.isLetterOrDigit() && it.code < 128 }
            .uppercase()
            .map { caracter ->
                when (caracter) {
                    'O' -> '0'
                    'I', 'L' -> '1'
                    else -> caracter
                }
            }
            .joinToString("")

    fun valido(texto: String): Boolean {
        val limpio = normalizar(texto)
        return limpio.length == LONGITUD && limpio.all { it in ALFABETO }
    }

    /** «ABCD-EFGH»: partido en dos se dicta y se copia mucho mejor. */
    fun formatear(codigo: String): String {
        val limpio = normalizar(codigo)
        return if (limpio.length == LONGITUD) limpio.take(4) + "-" + limpio.drop(4) else limpio
    }
}

/** Los mismos límites que las restricciones de la base de datos, para avisar antes de enviar. */
object LimitesGrupo {
    const val NOMBRE = 60
    const val APODO = 30
    const val MENSAJE = 2000
    const val ASIGNATURA = 80
    const val NOTAS = 500

    fun nombreValido(texto: String) = texto.trim().length in 1..NOMBRE
    fun apodoValido(texto: String) = texto.trim().length in 1..APODO
    fun mensajeValido(texto: String) = texto.trim().length in 1..MENSAJE
    fun asignaturaValida(texto: String) = texto.trim().length in 1..ASIGNATURA
}

object CalendarioExamenes {

    /**
     * Las casillas de un mes en una rejilla de semanas que empiezan en lunes, como los
     * calendarios de aquí. Las casillas de antes del día 1 y de después del último van a
     * null, y siempre salen semanas completas.
     */
    fun celdas(mes: YearMonth): List<LocalDate?> {
        val primero = mes.atDay(1)
        val huecosDelante = (primero.dayOfWeek.value - DayOfWeek.MONDAY.value)
        val dias = (1..mes.lengthOfMonth()).map { mes.atDay(it) }
        val antes: List<LocalDate?> = List(huecosDelante) { null }
        val sinRelleno = antes + dias
        val huecosDetras = (7 - sinRelleno.size % 7) % 7
        return sinRelleno + List(huecosDetras) { null }
    }

    /** Exámenes de cada día, los de hora temprana primero y los sin hora al final. */
    fun porDia(examenes: List<Examen>): Map<LocalDate, List<Examen>> =
        examenes
            .mapNotNull { examen -> examen.dia?.let { it to examen } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, lista) -> ordenar(lista) }

    /** Lo que queda por delante, empezando por hoy. Lo pasado no se enseña aquí. */
    fun proximos(examenes: List<Examen>, hoy: LocalDate): List<Examen> =
        ordenar(examenes.filter { examen -> examen.dia?.let { !it.isBefore(hoy) } == true })

    fun diasHasta(examen: Examen, hoy: LocalDate): Long? =
        examen.dia?.let { ChronoUnit.DAYS.between(hoy, it) }

    private fun ordenar(examenes: List<Examen>): List<Examen> =
        examenes.sortedWith(
            compareBy<Examen>({ it.dia ?: LocalDate.MAX })
                .thenBy { it.horaLocal ?: LocalTime.MAX }
                .thenBy { it.asignatura }
        )
}

object Chat {

    /** Cuántos mensajes se guardan en memoria: más no se leen y solo gastan. */
    const val MAXIMO_EN_PANTALLA = 500

    /**
     * Junta los mensajes que ya había con los recién llegados. Pueden solaparse —la consulta
     * pide «desde el último» y el envío propio se añade antes de que vuelva la consulta—,
     * así que se quita lo repetido por su identificador, que da el servidor y es creciente.
     */
    fun fusionar(actuales: List<Mensaje>, nuevos: List<Mensaje>): List<Mensaje> =
        (actuales + nuevos)
            .distinctBy { it.id }
            .sortedBy { it.id }
            .takeLast(MAXIMO_EN_PANTALLA)

    fun ultimoId(mensajes: List<Mensaje>): Long? = mensajes.maxOfOrNull { it.id }

    /** El apodo de quien escribió; si ya se fue del grupo no queda rastro de cómo se llamaba. */
    fun apodoDe(autor: String, miembros: List<Miembro>): String =
        miembros.firstOrNull { it.usuario == autor }?.apodo ?: "Antiguo miembro"
}
