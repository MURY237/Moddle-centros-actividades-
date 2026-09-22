package com.asir.moodleactividades.data.seneca

import com.asir.moodleactividades.domain.Falta
import kotlinx.serialization.Serializable

@Serializable
data class ResultadoExtraccion(
    val encontrada: Boolean = false,
    val faltas: List<Falta> = emptyList(),
    val cabeceras: List<String> = emptyList(),
    /** Se cambió el filtro a «Todas»: la tabla se recarga y hay que volver a mirar. */
    val ajustado: Boolean = false
)

@Serializable
data class ResultadoNavegacion(
    val pulsado: Boolean = false,
    val destino: String = ""
)

@Serializable
data class ResultadoAcceso(
    /** Qué hizo el guion: «aviso», «enviado», «sin-boton» o nada. */
    val accion: String = "",
    /** Había un formulario de acceso delante, se rellenase o no. */
    val formulario: Boolean = false,
    /** Había un aviso de sesión caducada tapando la página. */
    val aviso: Boolean = false
) {
    val actuo: Boolean get() = accion == "aviso" || accion == "enviado"

    /** Se mandó la contraseña: solo entonces tiene sentido juzgar si valía. */
    val enviado: Boolean get() = accion == "enviado"

    /** Se cerró el aviso: no gasta intentos de contraseña, porque no se mandó ninguna. */
    val cerroAviso: Boolean get() = accion == "aviso"
}
