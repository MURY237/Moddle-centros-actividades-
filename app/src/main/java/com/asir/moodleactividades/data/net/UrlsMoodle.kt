package com.asir.moodleactividades.data.net

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object UrlsMoodle {

    /**
     * La URL de un archivo con el token puesto, que es como la sirve `pluginfile.php`.
     *
     * Solo si el archivo es del mismo sitio que la sesión y va por HTTPS: el token abre todo
     * el Moodle del alumno, y añadirlo a una URL de otro dominio se lo regalaría a ese
     * servidor. Antes se añadía a cualquier URL que viniera como adjunto.
     */
    fun conToken(urlArchivo: String, sitio: String, token: String): String? {
        val archivo = urlArchivo.trim().toHttpUrlOrNull() ?: return null
        val base = sitio.trim().toHttpUrlOrNull() ?: return null
        if (!archivo.isHttps) return null
        if (!archivo.host.equals(base.host, ignoreCase = true)) return null
        // setQueryParameter y no concatenar: si ya traía un token, no queda duplicado.
        return archivo.newBuilder().setQueryParameter("token", token).build().toString()
    }
}
