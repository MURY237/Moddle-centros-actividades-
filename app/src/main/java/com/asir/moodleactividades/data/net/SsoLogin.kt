package com.asir.moodleactividades.data.net

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

sealed interface ResultadoSso {
    data class Ok(val token: String) : ResultadoSso
    data class Error(val mensaje: String) : ResultadoSso
}

/**
 * Flujo `tool_mobile` de Moodle: el centro autentica en su propio navegador (iDEA/Séneca) y
 * devuelve el token a la app por un esquema propio, sin que la contraseña pase por aquí.
 */
object SsoLogin {

    /**
     * Muchos centros restringen `forcedurlscheme` al esquema de la app oficial, así que pedir ese
     * es lo único que funciona tanto ahí como en los centros sin restricción.
     */
    const val ESQUEMA_SOLICITADO = "moodlemobile"

    val ESQUEMAS_ACEPTADOS = setOf(ESQUEMA_SOLICITADO, "moodleactividades")

    /**
     * La firma que acompaña al token se calcula sobre este valor, y es lo único que impide que
     * otra app entregue un token ajeno por el esquema compartido: tiene que ser impredecible.
     */
    fun generarPassport(): String =
        (SecureRandom().nextLong() and Long.MAX_VALUE).toString()

    fun urlDeLanzamiento(sitio: String, passport: String): String =
        sitio.trimEnd('/') +
            "/admin/tool/mobile/launch.php" +
            "?service=${MoodleClient.SERVICIO_MOVIL}" +
            "&passport=$passport" +
            "&urlscheme=$ESQUEMA_SOLICITADO"

    fun extraerToken(enlace: String, sitio: String, passport: String): ResultadoSso {
        val cargaUtil = enlace.substringAfter("token=", "").substringBefore('&')
        if (cargaUtil.isBlank()) {
            return ResultadoSso.Error("La respuesta del centro no incluye ningún token.")
        }

        val descifrado = decodificarBase64(cargaUtil)
            ?: return ResultadoSso.Error("No se pudo leer la respuesta del centro.")

        val partes = descifrado.split(":::")
        if (partes.size < 2 || partes[1].isBlank()) {
            return ResultadoSso.Error("La respuesta del centro tiene un formato inesperado.")
        }

        // La firma es lo único que impide que otra app nos entregue un token ajeno.
        if (!firmaValida(partes[0], sitio, passport)) {
            return ResultadoSso.Error("La firma de la respuesta no corresponde a este centro.")
        }

        return ResultadoSso.Ok(partes[1])
    }

    private fun firmaValida(firma: String, sitio: String, passport: String): Boolean =
        candidatosDeSitio(sitio).any { md5(it + passport).equals(firma, ignoreCase = true) }

    private fun candidatosDeSitio(sitio: String): List<String> =
        listOf(sitio.trimEnd('/'), sitio.trimEnd('/') + "/").distinct()

    private fun decodificarBase64(valor: String): String? = runCatching {
        String(Base64.getDecoder().decode(valor))
    }.recoverCatching {
        String(Base64.getUrlDecoder().decode(valor))
    }.getOrNull()

    private fun md5(texto: String): String =
        MessageDigest.getInstance("MD5")
            .digest(texto.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
