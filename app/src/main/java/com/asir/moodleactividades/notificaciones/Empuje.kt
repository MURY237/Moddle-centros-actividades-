package com.asir.moodleactividades.notificaciones

import android.content.Context
import com.asir.moodleactividades.BuildConfig
import com.asir.moodleactividades.Dependencias
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Avisos al instante por Firebase Cloud Messaging. Es opcional: si la app se compiló sin la
 * configuración de Firebase, no se inicia nada y los grupos siguen avisando con la
 * comprobación de cada 15 minutos.
 */
object Empuje {

    /** La configuración va en BuildConfig, sacada de google-services.json al compilar. */
    private val configurado: Boolean
        get() = BuildConfig.FIREBASE_APP_ID.isNotEmpty() && BuildConfig.FIREBASE_API_KEY.isNotEmpty()

    fun iniciar(contexto: Context) {
        if (!configurado || FirebaseApp.getApps(contexto).isNotEmpty()) return
        runCatching {
            FirebaseApp.initializeApp(
                contexto,
                FirebaseOptions.Builder()
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                    .build()
            )
        }
    }

    fun disponible(contexto: Context): Boolean =
        configurado && FirebaseApp.getApps(contexto).isNotEmpty()

    /**
     * Apunta este móvil en el servidor de grupos, como mucho una vez al día salvo que cambie
     * el token o la identidad. Sin identidad no hace nada: preguntar crearía una.
     */
    suspend fun registrar(contexto: Context, dependencias: Dependencias, token: String? = null) {
        if (!disponible(contexto) || !dependencias.hayIdentidadGrupos()) return
        val repositorio = dependencias.repositorioGrupos ?: return
        val valor = token ?: tokenActual() ?: return

        val prefs = contexto.getSharedPreferences("empuje", Context.MODE_PRIVATE)
        val huella = "$valor|${repositorio.yo()}|${LocalDate.now()}"
        if (prefs.getString(REGISTRADO, null) == huella) return
        repositorio.registrarDispositivo(valor)
        prefs.edit().putString(REGISTRADO, huella).apply()
    }

    private suspend fun tokenActual(): String? = suspendCancellableCoroutine { continuacion ->
        runCatching {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { tarea ->
                continuacion.resume(if (tarea.isSuccessful) tarea.result else null)
            }
        }.onFailure { continuacion.resume(null) }
    }

    private const val REGISTRADO = "registrado"
}
