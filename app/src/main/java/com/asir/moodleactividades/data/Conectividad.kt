package com.asir.moodleactividades.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class Conectividad(contexto: Context) {

    private val gestor = contexto.applicationContext
        .getSystemService(ConnectivityManager::class.java)

    /** Distingue «el móvil no tiene internet» de «el centro no responde», que se confunden. */
    fun hayInternet(): Boolean {
        val red = gestor?.activeNetwork ?: return false
        val capacidades = gestor.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
