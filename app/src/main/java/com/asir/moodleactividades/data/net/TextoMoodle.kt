package com.asir.moodleactividades.data.net

private val ENTIDADES = mapOf(
    "&nbsp;" to " ",
    "&amp;" to "&",
    "&lt;" to "<",
    "&gt;" to ">",
    "&quot;" to "\"",
    "&#39;" to "'",
    "&apos;" to "'"
)

/**
 * Moodle formatea las notas para la web, así que llegan con etiquetas y entidades HTML:
 * sin limpiarlas, un «100,00&nbsp;/&nbsp;100,00» se lee en pantalla como «100,00 &».
 */
fun limpiarHtml(texto: String): String {
    var limpio = texto.replace(Regex("<[^>]*>"), " ")
    ENTIDADES.forEach { (entidad, caracter) -> limpio = limpio.replace(entidad, caracter) }
    return limpio
        .replace(Regex("&[a-zA-Z]{2,10};|&#[0-9]{1,5};"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

/** De «8,00 / 10,00» interesa solo el «8,00»: el máximo se muestra aparte. */
fun notaVisible(bruta: String): String = limpiarHtml(bruta).substringBefore('/').trim()
