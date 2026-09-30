package com.asir.moodleactividades.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Preferencias cifradas con una clave que vive en el almacén de claves de Android, fuera del
 * sistema de ficheros: aunque alguien copie los datos de la app, sin el dispositivo no puede
 * leerlas. Es donde van las contraseñas y los tokens.
 *
 * Devuelve null si el almacén de claves falla, que pasa en algunos móviles tras restaurar una
 * copia de seguridad. Qué hacer entonces lo decide cada uso: una contraseña no se guarda; un
 * token de sesión sí, en claro, porque sin él habría que entrar en cada arranque.
 */
object PreferenciasCifradas {

    fun abrir(contexto: Context, nombre: String): SharedPreferences? = runCatching {
        // Solo se maneja el alias de la clave maestra, nunca el material de la clave.
        val alias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            nombre,
            alias,
            contexto.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrNull()

    /**
     * Lee [clave] de las cifradas; si solo está en las normales —versiones anteriores de la
     * app la guardaban así—, la pasa a las cifradas y la borra de las normales.
     */
    fun leerMigrando(cifradas: SharedPreferences?, normales: SharedPreferences, clave: String): String? {
        cifradas?.getString(clave, null)?.let { return it }
        val enClaro = normales.getString(clave, null) ?: return null
        if (cifradas == null) return enClaro
        // commit y no apply: si la app muere justo aquí, no puede quedar borrada de las dos.
        if (cifradas.edit().putString(clave, enClaro).commit()) {
            normales.edit().remove(clave).apply()
        }
        return enClaro
    }

    /** Escribe en las cifradas si las hay, y si no en las normales. Nunca en las dos. */
    fun escribir(cifradas: SharedPreferences?, normales: SharedPreferences, clave: String, valor: String) {
        if (cifradas != null) {
            cifradas.edit().putString(clave, valor).apply()
            normales.edit().remove(clave).apply()
        } else {
            normales.edit().putString(clave, valor).apply()
        }
    }

    fun borrar(cifradas: SharedPreferences?, normales: SharedPreferences, clave: String) {
        cifradas?.edit()?.remove(clave)?.apply()
        normales.edit().remove(clave).apply()
    }
}
