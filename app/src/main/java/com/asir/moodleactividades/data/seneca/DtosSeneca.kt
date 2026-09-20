package com.asir.moodleactividades.data.seneca

import com.asir.moodleactividades.domain.Falta
import kotlinx.serialization.Serializable

@Serializable
data class ResultadoExtraccion(
    val encontrada: Boolean = false,
    val faltas: List<Falta> = emptyList(),
    val cabeceras: List<String> = emptyList()
)

@Serializable
data class ResultadoNavegacion(
    val pulsado: Boolean = false,
    val destino: String = ""
)
