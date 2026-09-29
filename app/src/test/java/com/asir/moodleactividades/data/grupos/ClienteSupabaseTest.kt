package com.asir.moodleactividades.data.grupos

import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Un Supabase de mentira: cada petición pasa por una función que decide la respuesta, sin
 * salir a la red. Así se comprueba qué manda el cliente y cómo reacciona a cada respuesta.
 */
class ClienteSupabaseTest {

    private class Memoria(var sesion: SesionAuth? = null) : GuardaSesion {
        override fun leer() = sesion
        override fun guardar(sesion: SesionAuth) { this.sesion = sesion }
        override fun borrar() { sesion = null }
    }

    private class Llamada(val metodo: String, val url: String, val cabeceras: Map<String, String>, val cuerpo: String)

    private val llamadas = mutableListOf<Llamada>()
    private var ahora = 1_000_000L

    private fun cliente(guarda: GuardaSesion, responder: (Request) -> Pair<Int, String>): ClienteSupabase {
        val http = OkHttpClient.Builder().addInterceptor { cadena ->
            val peticion = cadena.request()
            val cuerpo = peticion.body?.let { b -> Buffer().also { b.writeTo(it) }.readUtf8() }.orEmpty()
            llamadas += Llamada(
                peticion.method, peticion.url.toString(),
                peticion.headers.names().associateWith { peticion.header(it)!! }, cuerpo
            )
            val (estado, respuesta) = responder(peticion)
            Response.Builder().request(peticion).protocol(Protocol.HTTP_1_1).code(estado).message("x")
                .body(respuesta.toResponseBody()).build()
        }.build()
        return ClienteSupabase(ConfigSupabase("https://p.supabase.co/", "clave-publica"), guarda, http) { ahora }
    }

    private fun auth(acceso: String, refresco: String, usuario: String = "u1") =
        """{"access_token":"$acceso","refresh_token":"$refresco","expires_in":3600,"token_type":"bearer","user":{"id":"$usuario","is_anonymous":true}}"""

    @Test fun `la primera vez crea la cuenta anonima y la usa`() = runBlocking {
        val guarda = Memoria()
        val c = cliente(guarda) { p ->
            if (p.url.encodedPath == "/auth/v1/signup") 200 to auth("A1", "R1") else 200 to "[]"
        }
        val repo = RepositorioGrupos(c)
        assertTrue(repo.misGrupos().isEmpty())

        assertEquals("/auth/v1/signup", llamadas[0].url.substringAfter(".co"))
        assertEquals("clave-publica", llamadas[0].cabeceras["apikey"])
        val consulta = llamadas[1]
        assertEquals("Bearer A1", consulta.cabeceras["Authorization"])
        assertEquals("clave-publica", consulta.cabeceras["apikey"])
        assertTrue(consulta.url.contains("/rest/v1/grupos"))
        assertEquals("u1", guarda.sesion!!.usuario)
        assertEquals(ahora + 3600, guarda.sesion!!.expira)
    }

    @Test fun `con la sesion viva no se renueva`() = runBlocking {
        val guarda = Memoria(SesionAuth("A1", "R1", ahora + 3000, "u1"))
        val c = cliente(guarda) { 200 to "[]" }
        RepositorioGrupos(c).misGrupos()
        assertEquals(1, llamadas.size)
    }

    @Test fun `a punto de caducar se renueva y se guarda el refresco nuevo`() = runBlocking {
        val guarda = Memoria(SesionAuth("A1", "R1", ahora + 30, "u1"))
        val c = cliente(guarda) { p ->
            if (p.url.encodedPath == "/auth/v1/token") 200 to auth("A2", "R2") else 200 to "[]"
        }
        RepositorioGrupos(c).misGrupos()
        assertTrue(llamadas[0].url.contains("grant_type=refresh_token"))
        assertTrue(llamadas[0].cuerpo.contains("\"R1\""))
        assertEquals("Bearer A2", llamadas[1].cabeceras["Authorization"])
        // Supabase gira el refresco en cada uso: guardar el viejo rompería la siguiente.
        assertEquals("R2", guarda.sesion!!.refresco)
        assertFalse(c.identidadRenovada)
    }

    @Test fun `si el servidor rechaza la sesion se crea otra y se avisa`() = runBlocking {
        val guarda = Memoria(SesionAuth("A1", "R1", ahora - 10, "viejo"))
        val c = cliente(guarda) { p ->
            when (p.url.encodedPath) {
                "/auth/v1/token" -> 400 to """{"error":"invalid_grant","error_description":"Invalid Refresh Token"}"""
                "/auth/v1/signup" -> 200 to auth("B1", "S1", "nuevo")
                else -> 200 to "[]"
            }
        }
        RepositorioGrupos(c).misGrupos()
        assertEquals("nuevo", guarda.sesion!!.usuario)
        assertTrue(c.identidadRenovada)
    }

    @Test fun `un fallo de red no cambia de identidad`() = runBlocking {
        val guarda = Memoria(SesionAuth("A1", "R1", ahora - 10, "u1"))
        val c = ClienteSupabase(
            ConfigSupabase("https://p.supabase.co", "k"), guarda,
            OkHttpClient.Builder().addInterceptor { throw java.io.IOException("sin red") }.build()
        ) { ahora }
        try {
            RepositorioGrupos(c).misGrupos()
            fail("tenía que fallar")
        } catch (e: ErrorGrupos) {
            assertEquals("Sin conexión con el servidor de grupos.", e.message)
        }
        // Crear otra identidad por un corte de red costaría todos los grupos.
        assertEquals("u1", guarda.sesion!!.usuario)
        assertFalse(c.identidadRenovada)
    }

    @Test fun `un 401 a mitad de camino renueva y repite una vez`() = runBlocking {
        val guarda = Memoria(SesionAuth("A1", "R1", ahora + 3000, "u1"))
        var consultas = 0
        val c = cliente(guarda) { p ->
            when (p.url.encodedPath) {
                "/auth/v1/token" -> 200 to auth("A2", "R2")
                else -> { consultas++; if (consultas == 1) 401 to """{"message":"JWT expired"}""" else 200 to "[]" }
            }
        }
        RepositorioGrupos(c).misGrupos()
        assertEquals(2, consultas)
        assertEquals("Bearer A2", llamadas.last().cabeceras["Authorization"])
    }

    @Test fun `los errores del esquema llegan en castellano`() = runBlocking {
        val guarda = Memoria(SesionAuth("A1", "R1", ahora + 3000, "u1"))
        val c = cliente(guarda) {
            400 to """{"code":"P0001","details":null,"hint":null,"message":"codigo_no_valido"}"""
        }
        try {
            RepositorioGrupos(c).unirse("ZZZZZZZZ", "Ana")
            fail("tenía que fallar")
        } catch (e: ErrorGrupos) {
            assertEquals("Ese código no corresponde a ningún grupo.", e.message)
        }
        assertTrue(llamadas.single().url.endsWith("/rest/v1/rpc/unirse"))
        assertTrue(llamadas.single().cuerpo.contains("\"p_codigo\":\"ZZZZZZZZ\""))
    }

    @Test fun `unirse devuelve el grupo de la funcion`() = runBlocking {
        val guarda = Memoria(SesionAuth("A1", "R1", ahora + 3000, "u1"))
        val c = cliente(guarda) {
            200 to """{"id":"g1","nombre":"2º ASIR","codigo":"ABCD1234","creador":"u9","creado":"2025-11-20T10:00:00+00:00"}"""
        }
        val grupo = RepositorioGrupos(c).unirse("abcd-1234", "  Ana  ")
        assertEquals("2º ASIR", grupo.nombre)
        assertTrue(llamadas.single().cuerpo.contains("\"p_apodo\":\"Ana\""))
    }

    @Test fun `enviar pide la fila creada y la lee de una lista`() = runBlocking {
        val guarda = Memoria(SesionAuth("A1", "R1", ahora + 3000, "u1"))
        val c = cliente(guarda) {
            201 to """[{"id":7,"grupo":"g1","autor":"u1","texto":"hola","enviado":"2025-11-20T10:00:00.5+00:00"}]"""
        }
        val m = RepositorioGrupos(c).enviar("g1", "  hola ")
        assertEquals(7L, m.id)
        assertEquals("return=representation", llamadas.single().cabeceras["Prefer"])
        // Ni autor ni hora: los pone el servidor, y además no se permiten desde fuera.
        assertEquals("""{"grupo":"g1","texto":"hola"}""", llamadas.single().cuerpo)
    }

    @Test fun `los mensajes nuevos se piden desde el ultimo`() = runBlocking {
        val guarda = Memoria(SesionAuth("A1", "R1", ahora + 3000, "u1"))
        val c = cliente(guarda) { 200 to "[]" }
        val repo = RepositorioGrupos(c)
        repo.mensajes("g1", desde = 41)
        repo.mensajes("g1", desde = null)
        assertTrue(llamadas[0].url.contains("id=gt.41"))
        assertTrue(llamadas[0].url.contains("order=id.asc"))
        assertTrue(llamadas[1].url.contains("order=id.desc"))
        assertTrue(llamadas[1].url.contains("limit=100"))
    }

    @Test fun `un examen sin hora manda null y con hora la manda corta`() = runBlocking {
        val guarda = Memoria(SesionAuth("A1", "R1", ahora + 3000, "u1"))
        val c = cliente(guarda) { 201 to """[{"id":1,"autor":"u1","asignatura":"Redes","fecha":"2025-12-05"}]""" }
        val repo = RepositorioGrupos(c)
        repo.anadirExamen("g1", "Redes", LocalDate.of(2025, 12, 5), null, "")
        repo.anadirExamen("g1", "Redes", LocalDate.of(2025, 12, 5), LocalTime.of(9, 0), "Tema 3")
        assertTrue(llamadas[0].cuerpo.contains("\"hora\":null"))
        assertTrue(llamadas[1].cuerpo.contains("\"hora\":\"09:00\""))
        assertTrue(llamadas[1].cuerpo.contains("\"fecha\":\"2025-12-05\""))
    }

    @Test fun `renovar el codigo lee la cadena que devuelve la funcion`() = runBlocking {
        val guarda = Memoria(SesionAuth("A1", "R1", ahora + 3000, "u1"))
        val c = cliente(guarda) { 200 to "\"XYZW5678\"" }
        assertEquals("XYZW5678", RepositorioGrupos(c).renovarCodigo("g1"))
    }

    @Test fun `el acceso anonimo desactivado se explica`() = runBlocking {
        val c = cliente(Memoria()) {
            422 to """{"code":422,"error_code":"anonymous_provider_disabled","msg":"Anonymous sign-ins are disabled"}"""
        }
        try {
            RepositorioGrupos(c).misGrupos(); fail("tenía que fallar")
        } catch (e: ErrorGrupos) {
            assertNotNull(e.message)
            assertTrue(e.message!!.contains("acceso anónimo"))
        }
    }

    @Test fun `la clave secreta se reconoce en los dos formatos`() {
        assertTrue(ConfigSupabase.esClaveSecreta("sb_secret_abc123"))
        assertFalse(ConfigSupabase.esClaveSecreta("sb_publishable_abc123"))
        val cabecera = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        fun jwt(papel: String) = cabecera + "." +
            java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("""{"iss":"supabase","ref":"p","role":"$papel","iat":1}""".toByteArray()) + ".firma"
        assertTrue(ConfigSupabase.esClaveSecreta(jwt("service_role")))
        assertFalse(ConfigSupabase.esClaveSecreta(jwt("anon")))
        assertFalse(ConfigSupabase("https://p.supabase.co", jwt("service_role")).configurado)
        assertTrue(ConfigSupabase("https://p.supabase.co", jwt("anon")).configurado)
        assertFalse(ConfigSupabase("", "").configurado)
    }
}
