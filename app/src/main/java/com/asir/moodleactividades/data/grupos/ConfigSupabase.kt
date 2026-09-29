package com.asir.moodleactividades.data.grupos

import java.util.Base64

/**
 * Dónde está el servidor de los grupos. La clave es la pública de Supabase (la «anon» o la
 * «publishable»): viaja dentro del APK y la puede leer cualquiera, y está bien que así sea,
 * porque lo que protege los datos son las reglas de supabase/esquema.sql.
 *
 * La secreta es otra cosa: con ella se salta todas las reglas. Si por error se pusiera
 * aquí, cualquiera que abriera el APK podría leer todos los grupos, así que se rechaza.
 * El build también la rechaza antes de llegar a compilar.
 */
data class ConfigSupabase(val url: String, val clave: String) {

    val configurado: Boolean
        get() = url.startsWith("https://") && clave.isNotBlank() && !esClaveSecreta(clave)

    companion object {
        fun esClaveSecreta(clave: String): Boolean {
            val limpia = clave.trim()
            if (limpia.startsWith("sb_secret_")) return true
            // Las claves antiguas son JWT: el papel va dentro, en la parte del medio.
            val partes = limpia.split('.')
            if (partes.size != 3) return false
            val cuerpo = runCatching {
                String(Base64.getUrlDecoder().decode(partes[1]), Charsets.UTF_8)
            }.getOrDefault("")
            return Regex(""""role"\s*:\s*"service_role"""").containsMatchIn(cuerpo)
        }

        /** Quita la barra final: las rutas se añaden empezando por «/». */
        fun limpiarUrl(url: String): String = url.trim().trimEnd('/')
    }
}
