package com.asir.moodleactividades.data.grupos

import com.asir.moodleactividades.domain.Examen
import com.asir.moodleactividades.domain.Grupo
import com.asir.moodleactividades.domain.Mensaje
import com.asir.moodleactividades.domain.Miembro
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Lo que la app hace con los grupos, traducido a la API de Supabase. Las altas, bajas y el
 * código pasan por las funciones del esquema; lo demás son lecturas y escrituras normales
 * que las reglas del servidor filtran: aquí no se comprueba quién puede qué, porque desde
 * el cliente se podría saltar. Eso lo decide el servidor.
 */
class RepositorioGrupos(private val cliente: ClienteSupabase) {

    val identidadRenovada: Boolean get() = cliente.identidadRenovada

    suspend fun yo(): String = cliente.usuario()

    suspend fun misGrupos(): List<Grupo> = lista(
        cliente.get(
            cliente.url("rest/v1/grupos")
                .addQueryParameter("select", "id,nombre,codigo,creador,creado")
                .addQueryParameter("order", "creado.desc")
                .build()
        ),
        Grupo.serializer()
    )

    suspend fun crear(nombre: String, apodo: String): Grupo = uno(
        cliente.rpc("crear_grupo", buildJsonObject {
            put("p_nombre", nombre.trim())
            put("p_apodo", apodo.trim())
        }),
        Grupo.serializer()
    )

    suspend fun unirse(codigo: String, apodo: String): Grupo = uno(
        cliente.rpc("unirse", buildJsonObject {
            put("p_codigo", codigo)
            put("p_apodo", apodo.trim())
        }),
        Grupo.serializer()
    )

    suspend fun miembros(grupo: String): List<Miembro> = lista(
        cliente.get(
            cliente.url("rest/v1/miembros")
                .addQueryParameter("grupo", "eq.$grupo")
                .addQueryParameter("select", "grupo,usuario,apodo,unido")
                .addQueryParameter("order", "unido.asc")
                .build()
        ),
        Miembro.serializer()
    )

    /**
     * Sin [desde], los últimos [RECIENTES]; con él, solo lo posterior. Así la consulta que se
     * repite cada pocos segundos no vuelve a bajar el chat entero.
     */
    suspend fun mensajes(grupo: String, desde: Long?): List<Mensaje> {
        val url = cliente.url("rest/v1/mensajes")
            .addQueryParameter("grupo", "eq.$grupo")
            .addQueryParameter("select", "id,grupo,autor,texto,enviado")
        if (desde == null) {
            url.addQueryParameter("order", "id.desc").addQueryParameter("limit", RECIENTES.toString())
        } else {
            url.addQueryParameter("id", "gt.$desde")
                .addQueryParameter("order", "id.asc")
                .addQueryParameter("limit", LOTE.toString())
        }
        return lista(cliente.get(url.build()), Mensaje.serializer()).sortedBy { it.id }
    }

    suspend fun enviar(grupo: String, texto: String): Mensaje = uno(
        cliente.post(
            cliente.url("rest/v1/mensajes").build(),
            buildJsonObject {
                put("grupo", grupo)
                put("texto", texto.trim())
            }
        ),
        Mensaje.serializer()
    )

    suspend fun examenes(grupo: String): List<Examen> = lista(
        cliente.get(
            cliente.url("rest/v1/examenes")
                .addQueryParameter("grupo", "eq.$grupo")
                .addQueryParameter("select", "id,grupo,autor,asignatura,fecha,hora,notas,creado")
                .addQueryParameter("order", "fecha.asc,hora.asc.nullslast")
                .build()
        ),
        Examen.serializer()
    )

    suspend fun anadirExamen(
        grupo: String,
        asignatura: String,
        fecha: LocalDate,
        hora: LocalTime?,
        notas: String
    ): Examen = uno(
        cliente.post(
            cliente.url("rest/v1/examenes").build(),
            buildJsonObject {
                put("grupo", grupo)
                put("asignatura", asignatura.trim())
                put("fecha", fecha.toString())
                if (hora == null) put("hora", JsonNull) else put("hora", hora.format(HORA))
                put("notas", notas.trim())
            }
        ),
        Examen.serializer()
    )

    suspend fun borrarExamen(id: Long) {
        cliente.delete(cliente.url("rest/v1/examenes").addQueryParameter("id", "eq.$id").build())
    }

    suspend fun cambiarApodo(grupo: String, apodo: String) {
        cliente.patch(
            cliente.url("rest/v1/miembros")
                .addQueryParameter("grupo", "eq.$grupo")
                .addQueryParameter("usuario", "eq.${yo()}")
                .build(),
            buildJsonObject { put("apodo", apodo.trim()) }
        )
    }

    suspend fun renovarCodigo(grupo: String): String =
        json.decodeFromString<String>(cliente.rpc("renovar_codigo", buildJsonObject { put("p_grupo", grupo) }))

    suspend fun salir(grupo: String) {
        cliente.rpc("salir", buildJsonObject { put("p_grupo", grupo) })
    }

    suspend fun expulsar(grupo: String, usuario: String) {
        cliente.rpc("expulsar", buildJsonObject {
            put("p_grupo", grupo)
            put("p_usuario", usuario)
        })
    }

    suspend fun eliminar(grupo: String) {
        cliente.delete(cliente.url("rest/v1/grupos").addQueryParameter("id", "eq.$grupo").build())
    }

    private fun <T> lista(cuerpo: String, serializador: kotlinx.serialization.KSerializer<T>): List<T> =
        runCatching { json.decodeFromString(ListSerializer(serializador), cuerpo) }
            .getOrElse { throw ErrorGrupos("El servidor de grupos respondió algo inesperado.") }

    /**
     * Una fila sola: las funciones devuelven un objeto; las inserciones con
     * «return=representation», una lista de uno.
     */
    private fun <T> uno(cuerpo: String, serializador: kotlinx.serialization.KSerializer<T>): T {
        val texto = cuerpo.trim()
        return runCatching {
            if (texto.startsWith("[")) json.decodeFromString(ListSerializer(serializador), texto).first()
            else json.decodeFromString(serializador, texto)
        }.getOrElse { throw ErrorGrupos("El servidor de grupos respondió algo inesperado.") }
    }

    private companion object {
        const val RECIENTES = 100
        const val LOTE = 200
        val HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val json = Json { ignoreUnknownKeys = true }
    }
}
