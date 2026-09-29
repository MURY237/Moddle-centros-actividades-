package com.asir.moodleactividades.data.netacad

import kotlinx.serialization.Serializable

/**
 * Una fila candidata tal y como la pinta NetAcad, sin interpretar: el guion de JavaScript
 * solo recoge texto. Decidir qué es una fecha y qué estado significa se hace en Kotlin, que
 * es donde se puede probar sin un navegador delante.
 */
@Serializable
data class CandidatoNetacad(
    val titulo: String = "",
    val curso: String = "",
    val fecha: String = "",
    val estado: String = "",
    val nota: String = "",
    val url: String = ""
)

@Serializable
data class ResultadoNetacad(
    /** «trabajos», «acceso», «cargando» o «desconocida». */
    val pagina: String = "",
    val candidatos: List<CandidatoNetacad> = emptyList(),
    /**
     * Cabeceras y rótulos vistos cuando no se sacó nada. Sirven para afinar el guion si Cisco
     * cambia la página; no llevan ningún dato personal, solo la estructura.
     */
    val pistas: List<String> = emptyList()
) {
    val pideAcceso: Boolean get() = pagina == "acceso"
    val aunCargando: Boolean get() = pagina == "cargando"
}
