package com.asir.moodleactividades.data.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64

class SsoLoginTest {

    private val sitio = "https://educacionadistancia.juntadeandalucia.es/centros/micentro/"
    private val passport = "1234567890"

    private fun md5(texto: String) = MessageDigest.getInstance("MD5")
        .digest(texto.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun respuestaDeMoodle(
        firma: String,
        token: String,
        privado: String? = null,
        esquema: String = "moodlemobile"
    ): String {
        val carga = listOfNotNull(firma, token, privado).joinToString(":::")
        return "$esquema://token=" + Base64.getEncoder().encodeToString(carga.toByteArray())
    }

    @Test
    fun `la url de lanzamiento pide el esquema que los centros autorizan`() {
        val url = SsoLogin.urlDeLanzamiento(sitio, passport)

        assertEquals(
            "https://educacionadistancia.juntadeandalucia.es/centros/micentro" +
                "/admin/tool/mobile/launch.php" +
                "?service=moodle_mobile_app&passport=1234567890&urlscheme=moodlemobile",
            url
        )
    }

    @Test
    fun `acepta la respuesta llegue por el esquema que llegue`() {
        val firma = md5(sitio.trimEnd('/') + passport)

        SsoLogin.ESQUEMAS_ACEPTADOS.forEach { esquema ->
            val resultado = SsoLogin.extraerToken(
                respuestaDeMoodle(firma, "abc123", esquema = esquema),
                sitio,
                passport
            )
            assertEquals(ResultadoSso.Ok("abc123"), resultado)
        }
    }

    @Test
    fun `acepta el token cuando la firma corresponde al sitio`() {
        val firma = md5(sitio.trimEnd('/') + passport)
        val resultado = SsoLogin.extraerToken(respuestaDeMoodle(firma, "abc123"), sitio, passport)

        assertEquals(ResultadoSso.Ok("abc123"), resultado)
    }

    @Test
    fun `ignora el privatetoken que Moodle anade al final`() {
        val firma = md5(sitio.trimEnd('/') + passport)
        val resultado = SsoLogin.extraerToken(
            respuestaDeMoodle(firma, "abc123", "privado999"),
            sitio,
            passport
        )

        assertEquals(ResultadoSso.Ok("abc123"), resultado)
    }

    @Test
    fun `rechaza un token firmado por otro sitio`() {
        val firmaAjena = md5("https://sitio-falso.example.com" + passport)
        val resultado = SsoLogin.extraerToken(respuestaDeMoodle(firmaAjena, "robado"), sitio, passport)

        assertTrue(resultado is ResultadoSso.Error)
    }

    @Test
    fun `rechaza un passport que no es el que se envio`() {
        val firma = md5(sitio.trimEnd('/') + passport)
        val resultado = SsoLogin.extraerToken(respuestaDeMoodle(firma, "abc123"), sitio, "9999999999")

        assertTrue(resultado is ResultadoSso.Error)
    }

    @Test
    fun `informa cuando la respuesta no trae token`() {
        val resultado = SsoLogin.extraerToken("moodlemobile://error=1", sitio, passport)

        assertTrue(resultado is ResultadoSso.Error)
    }
}
