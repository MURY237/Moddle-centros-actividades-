package com.asir.moodleactividades

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.asir.moodleactividades.data.AlmacenHorario
import com.asir.moodleactividades.data.CacheActividades
import com.asir.moodleactividades.data.Conectividad
import com.asir.moodleactividades.data.CacheCalificaciones
import com.asir.moodleactividades.data.DescargaAdjuntos
import com.asir.moodleactividades.data.HistorialAvisos
import com.asir.moodleactividades.data.PreferenciasAvisos
import com.asir.moodleactividades.data.SesionStore
import com.asir.moodleactividades.data.net.SsoLogin
import com.asir.moodleactividades.notificaciones.Recordatorios
import com.asir.moodleactividades.notificaciones.RecordatoriosWorker
import com.asir.moodleactividades.ui.acercade.AcercaDeScreen
import com.asir.moodleactividades.ui.actualizacion.ActualizacionViewModel
import com.asir.moodleactividades.ui.ajustes.AjustesViewModel
import com.asir.moodleactividades.ui.asistencia.AsistenciaViewModel
import com.asir.moodleactividades.ui.avisos.AvisosScreen
import com.asir.moodleactividades.ui.avisos.AvisosViewModel
import com.asir.moodleactividades.ui.horario.HorarioScreen
import com.asir.moodleactividades.ui.horario.HorarioViewModel
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
        enableEdgeToEdge()
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
    ACTIVIDADES("Tareas"),
    NOTAS("Notas"),
    AVISOS("Avisos"),
    HORARIO("Horario"),
    AJUSTES("Ajustes")
}

@Composable
private fun App(enlaceSso: String?, alConsumirEnlace: () -> Unit) {
    val contexto = LocalContext.current
    val repositorio = remember {
        ActividadesRepository(
            SesionStore(contexto),
            CacheActividades(contexto),
            CacheCalificaciones(contexto)
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantallaPrincipal(
    repositorio: ActividadesRepository,
    conectividad: Conectividad,
    generacion: Int,
    alCerrarSesion: () -> Unit
) {
    val contexto = LocalContext.current
    var seccion by remember { mutableStateOf(Seccion.ACTIVIDADES) }

    val actividadesViewModel: ActividadesViewModel = viewModel(
        key = "actividades-$generacion",
        factory = fabrica {
            ActividadesViewModel(repositorio, conectividad, DescargaAdjuntos(contexto, repositorio))
        }
    )
    val estadoActividades by actividadesViewModel.estado.collectAsStateWithLifecycle()

    val actualizacionViewModel: ActualizacionViewModel = viewModel()
    val actualizacion by actualizacionViewModel.estado.collectAsStateWithLifecycle()

    val ajustesViewModel: AjustesViewModel = viewModel(
        factory = fabrica { AjustesViewModel(PreferenciasAvisos(contexto)) }
    )
    val ajustes by ajustesViewModel.estado.collectAsStateWithLifecycle()

    val asistenciaViewModel: AsistenciaViewModel = viewModel(
        key = "asistencia-$generacion",
        factory = fabrica { AsistenciaViewModel(repositorio) }
    )
    val asistencia by asistenciaViewModel.estado.collectAsStateWithLifecycle()

    // Vive fuera de la pestaña porque el contador de no leídos se pinta en la barra inferior.
    val avisosViewModel: AvisosViewModel = viewModel(
        factory = fabrica { AvisosViewModel(HistorialAvisos(contexto)) }
    )
    val avisos by avisosViewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(seccion) {
        if (seccion == Seccion.AVISOS) avisosViewModel.marcarLeidos()
    }

    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) RecordatoriosWorker.programar(contexto)
    }

    // Aquí y no en la pantalla: dentro de una pestaña esto se repetía en cada cambio de
    // sección, volviendo a pedir el permiso y a consultar la API de GitHub.
    LaunchedEffect(Unit) {
        actualizacionViewModel.comprobar()
        Recordatorios.crearCanales(contexto)
        if (Recordatorios.puedeNotificar(contexto)) {
            RecordatoriosWorker.programar(contexto)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(estadoActividades.sesionCaducada) {
        if (estadoActividades.sesionCaducada) alCerrarSesion()
    }

    Scaffold(
        // La cabecera de cada pantalla pinta bajo la barra de estado y aplica su propio inset.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                NavigationBarItem(
                    selected = seccion == Seccion.AVISOS,
                    onClick = { seccion = Seccion.AVISOS },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (avisos.sinLeer > 0) {
                                    Badge { Text(avisos.sinLeer.coerceAtMost(99).toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                    },
                    label = { Text(Seccion.AVISOS.etiqueta) }
                )
                NavigationBarItem(
                    selected = seccion == Seccion.HORARIO,
                    onClick = { seccion = Seccion.HORARIO },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text(Seccion.HORARIO.etiqueta) }
                )
                NavigationBarItem(
                    selected = seccion == Seccion.AJUSTES,
                    onClick = { seccion = Seccion.AJUSTES },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(Seccion.AJUSTES.etiqueta) }
                )
            }
        }
    ) { relleno ->
        when (seccion) {
            Seccion.ACTIVIDADES -> ActividadesScreen(
                viewModel = actividadesViewModel,
                actualizacion = actualizacion,
                alInstalarActualizacion = { actualizacionViewModel.instalar(contexto) },
                alDescartarActualizacion = actualizacionViewModel::descartar,
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

            Seccion.AVISOS -> AvisosScreen(
                viewModel = avisosViewModel,
                modifier = Modifier.padding(relleno)
            )

            Seccion.HORARIO -> {
                val horarioViewModel: HorarioViewModel = viewModel(
                    factory = fabrica { HorarioViewModel(AlmacenHorario(contexto)) }
                )
                HorarioScreen(
                    viewModel = horarioViewModel,
                    modifier = Modifier.padding(relleno)
                )
            }

            Seccion.AJUSTES -> AcercaDeScreen(
                ajustes = ajustes,
                puedeNotificar = Recordatorios.puedeNotificar(contexto),
                alCambiarEntregas = { ajustesViewModel.cambiarAvisoEntregas(it, contexto) },
                alCambiarNuevas = { ajustesViewModel.cambiarAvisoNuevas(it, contexto) },
                alCambiarNotas = { ajustesViewModel.cambiarAvisoNotas(it, contexto) },
                alCambiarAntelacion = { ajustesViewModel.cambiarAntelacion(it, contexto) },
                alCambiarFrecuencia = { ajustesViewModel.cambiarFrecuencia(it, contexto) },
                alCambiarHora = { ajustesViewModel.cambiarHora(it, contexto) },
                asistencia = asistencia,
                alComprobarAsistencia = asistenciaViewModel::comprobar,
                modifier = Modifier.padding(relleno)
            )
        }
    }
}

private fun fabrica(crear: () -> ViewModel) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = crear() as T
}
