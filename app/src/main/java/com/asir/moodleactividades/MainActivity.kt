package com.asir.moodleactividades

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asir.moodleactividades.data.net.SsoLogin
import com.asir.moodleactividades.ui.PantallaPrincipal
import com.asir.moodleactividades.ui.login.LoginScreen
import com.asir.moodleactividades.ui.login.LoginViewModel
import com.asir.moodleactividades.ui.navegacion.Seccion
import com.asir.moodleactividades.ui.theme.MoodleActividadesTheme

class MainActivity : ComponentActivity() {

    /** El enlace con el que vuelve el navegador tras entrar con iDEA; null si no hay. */
    private var enlaceEntrante by mutableStateOf<String?>(null)

    /** La sección que pide una notificación al tocarla; null si no hay. */
    private var seccionPedida by mutableStateOf<Seccion?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Al girar el móvil la actividad se recrea con el mismo intent: sin esta comprobación
        // se volvería a procesar el enlace de iDEA o a saltar a la sección de la notificación.
        if (savedInstanceState == null) {
            enlaceEntrante = enlaceDeSso(intent)
            seccionPedida = seccionDe(intent)
        }
        val dependencias = (application as AppActividades).dependencias
        setContent {
            MoodleActividadesTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    App(
                        dependencias = dependencias,
                        enlaceSso = enlaceEntrante,
                        alConsumirEnlace = { enlaceEntrante = null },
                        seccionPedida = seccionPedida,
                        alConsumirSeccion = { seccionPedida = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        enlaceDeSso(intent)?.let { enlaceEntrante = it }
        seccionDe(intent)?.let { seccionPedida = it }
    }

    private fun enlaceDeSso(intent: Intent?): String? =
        intent?.data?.takeIf { it.scheme in SsoLogin.ESQUEMAS_ACEPTADOS }?.toString()

    /** Solo pestañas: una notificación no debe poder abrir a ciegas cualquier pantalla. */
    private fun seccionDe(intent: Intent?): Seccion? =
        intent?.getStringExtra(EXTRA_SECCION)
            ?.let { nombre -> Seccion.PESTANAS.firstOrNull { it.name == nombre } }

    companion object {
        const val EXTRA_SECCION = "seccion"
    }
}

/** Con sesión, la app; sin ella, el acceso. */
@Composable
private fun App(
    dependencias: Dependencias,
    enlaceSso: String?,
    alConsumirEnlace: () -> Unit,
    seccionPedida: Seccion?,
    alConsumirSeccion: () -> Unit
) {
    var haySesion by rememberSaveable { mutableStateOf(dependencias.repositorio.sesionGuardada() != null) }
    // Cada entrada y salida estrena ViewModel: reutilizarlo arrastraría el estado de la sesión anterior.
    var generacion by rememberSaveable { mutableIntStateOf(0) }

    val entrar = {
        generacion++
        haySesion = true
    }

    if (haySesion) {
        PantallaPrincipal(
            dependencias = dependencias,
            generacion = generacion,
            seccionPedida = seccionPedida,
            alConsumirSeccion = alConsumirSeccion,
            alCerrarSesion = {
                dependencias.cerrarSesion()
                generacion++
                haySesion = false
            },
            alCaducarSesion = {
                dependencias.sesionCaducada()
                generacion++
                haySesion = false
            }
        )
    } else {
        val viewModel: LoginViewModel = viewModel(key = "login-$generacion", factory = fabrica(dependencias::login))

        LaunchedEffect(enlaceSso) {
            enlaceSso?.let { enlace ->
                viewModel.procesarRespuesta(enlace, entrar)
                alConsumirEnlace()
            }
        }

        LoginScreen(viewModel = viewModel, alEntrar = entrar)
    }
}
