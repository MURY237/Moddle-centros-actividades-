package com.asir.moodleactividades

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asir.moodleactividades.data.ActividadesRepository
import com.asir.moodleactividades.data.CacheActividades
import com.asir.moodleactividades.data.Conectividad
import com.asir.moodleactividades.data.SesionStore
import com.asir.moodleactividades.data.net.SsoLogin
import com.asir.moodleactividades.notificaciones.RecordatoriosWorker
import com.asir.moodleactividades.ui.actividades.ActividadesScreen
import com.asir.moodleactividades.ui.actividades.ActividadesViewModel
import com.asir.moodleactividades.ui.login.LoginScreen
import com.asir.moodleactividades.ui.login.LoginViewModel
import com.asir.moodleactividades.ui.notas.NotasScreen
import com.asir.moodleactividades.ui.notas.NotasViewModel
import com.asir.moodleactividades.ui.theme.MoodleActividadesTheme

class MainActivity : ComponentActivity() {

    private var enlaceEntrante by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enlaceEntrante = enlaceDeSso(intent)
        setContent {
            MoodleActividadesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(
                        enlaceSso = enlaceEntrante,
                        alConsumirEnlace = { enlaceEntrante = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        enlaceDeSso(intent)?.let { enlaceEntrante = it }
    }

    private fun enlaceDeSso(intent: Intent?): String? =
        intent?.data?.takeIf { it.scheme in SsoLogin.ESQUEMAS_ACEPTADOS }?.toString()
}

private enum class Seccion(val etiqueta: String) {
    ACTIVIDADES("Actividades"),
    NOTAS("Notas")
}

@Composable
private fun App(enlaceSso: String?, alConsumirEnlace: () -> Unit) {
    val contexto = LocalContext.current
    val repositorio = remember {
        ActividadesRepository(SesionStore(contexto), CacheActividades(contexto))
    }
    val conectividad = remember { Conectividad(contexto) }

    var haySesion by remember { mutableStateOf(repositorio.sesionGuardada() != null) }
    // Cada entrada y salida estrena ViewModel: reutilizarlo arrastraría el estado de la sesión anterior.
    var generacion by remember { mutableIntStateOf(0) }

    if (haySesion) {
        PantallaPrincipal(
            repositorio = repositorio,
            conectividad = conectividad,
            generacion = generacion,
            alCerrarSesion = {
                RecordatoriosWorker.cancelar(contexto)
                generacion++
                haySesion = false
            }
        )
    } else {
        val viewModel: LoginViewModel = viewModel(
            key = "login-$generacion",
            factory = fabrica { LoginViewModel(repositorio) }
        )

        LaunchedEffect(enlaceSso) {
            enlaceSso?.let { enlace ->
                viewModel.procesarRespuesta(enlace) {
                    generacion++
                    haySesion = true
                }
                alConsumirEnlace()
            }
        }

        LoginScreen(
            viewModel = viewModel,
            alEntrar = {
                generacion++
                haySesion = true
            }
        )
    }
}

@Composable
private fun PantallaPrincipal(
    repositorio: ActividadesRepository,
    conectividad: Conectividad,
    generacion: Int,
    alCerrarSesion: () -> Unit
) {
    var seccion by remember { mutableStateOf(Seccion.ACTIVIDADES) }

    val actividadesViewModel: ActividadesViewModel = viewModel(
        key = "actividades-$generacion",
        factory = fabrica { ActividadesViewModel(repositorio, conectividad) }
    )
    val estadoActividades by actividadesViewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estadoActividades.sesionCaducada) {
        if (estadoActividades.sesionCaducada) alCerrarSesion()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = seccion == Seccion.ACTIVIDADES,
                    onClick = { seccion = Seccion.ACTIVIDADES },
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null
                        )
                    },
                    label = { Text(Seccion.ACTIVIDADES.etiqueta) }
                )
                NavigationBarItem(
                    selected = seccion == Seccion.NOTAS,
                    onClick = { seccion = Seccion.NOTAS },
                    icon = { Icon(Icons.Default.Grade, contentDescription = null) },
                    label = { Text(Seccion.NOTAS.etiqueta) }
                )
            }
        }
    ) { relleno ->
        when (seccion) {
            Seccion.ACTIVIDADES -> ActividadesScreen(
                viewModel = actividadesViewModel,
                modifier = Modifier.padding(relleno)
            )

            Seccion.NOTAS -> {
                val notasViewModel: NotasViewModel = viewModel(
                    key = "notas-$generacion",
                    factory = fabrica { NotasViewModel(repositorio, conectividad) }
                )
                NotasScreen(
                    viewModel = notasViewModel,
                    modifier = Modifier.padding(relleno)
                )
            }
        }
    }
}

private fun fabrica(crear: () -> ViewModel) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = crear() as T
}
