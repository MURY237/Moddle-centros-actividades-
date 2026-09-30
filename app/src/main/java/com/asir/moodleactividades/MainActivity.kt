package com.asir.moodleactividades

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asir.moodleactividades.data.ActividadesRepository
import com.asir.moodleactividades.data.AlmacenBus
import com.asir.moodleactividades.data.AlmacenHorario
import com.asir.moodleactividades.data.CacheActividades
import com.asir.moodleactividades.data.Conectividad
import com.asir.moodleactividades.data.AlmacenFaltas
import com.asir.moodleactividades.data.AlmacenNetacad
import com.asir.moodleactividades.data.CacheCalificaciones
import com.asir.moodleactividades.data.CredencialesCifradas
import com.asir.moodleactividades.data.DescargaAdjuntos
import com.asir.moodleactividades.data.HistorialAvisos
import com.asir.moodleactividades.data.PreferenciasAvisos
import com.asir.moodleactividades.data.SesionNetacad
import com.asir.moodleactividades.data.SesionSeneca
import com.asir.moodleactividades.data.SesionStore
import com.asir.moodleactividades.data.grupos.ClienteSupabase
import com.asir.moodleactividades.data.grupos.ConfigSupabase
import com.asir.moodleactividades.data.grupos.GuardaSesionPrefs
import com.asir.moodleactividades.data.grupos.RepositorioGrupos
import com.asir.moodleactividades.data.net.SsoLogin
import com.asir.moodleactividades.notificaciones.Recordatorios
import com.asir.moodleactividades.notificaciones.RecordatoriosWorker
import com.asir.moodleactividades.ui.acercade.AcercaDeScreen
import com.asir.moodleactividades.ui.actualizacion.ActualizacionViewModel
import com.asir.moodleactividades.ui.ajustes.AjustesViewModel
import com.asir.moodleactividades.ui.asistencia.AsistenciaViewModel
import com.asir.moodleactividades.ui.bus.BusViewModel
import com.asir.moodleactividades.ui.faltas.FaltasScreen
import com.asir.moodleactividades.ui.faltas.FaltasViewModel
import com.asir.moodleactividades.ui.grupos.GruposScreen
import com.asir.moodleactividades.ui.grupos.GruposViewModel
import com.asir.moodleactividades.ui.netacad.NetacadScreen
import com.asir.moodleactividades.ui.netacad.NetacadViewModel
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
    FALTAS("Faltas"),
    AVISOS("Avisos"),
    HORARIO("Horario"),
    GRUPOS("Grupos"),

    /** Subpantalla de Avisos: son los ajustes de los avisos, y así Grupos cabe en la barra. */
    AJUSTES("Ajustes"),

    /** Subpantalla de Tareas: no tiene hueco propio en la barra, que ya va llena. */
    NETACAD("NetAcad")
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
                DescargaAdjuntos(contexto, repositorio).borrarTodo()
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    val notasViewModel: NotasViewModel = viewModel(
        key = "notas-$generacion",
        factory = fabrica { NotasViewModel(repositorio, conectividad) }
    )

    val sesionSeneca = remember { SesionSeneca(contexto) }
    val faltasViewModel: FaltasViewModel = viewModel(
        factory = fabrica {
            FaltasViewModel(
                AlmacenFaltas(contexto),
                sesionSeneca,
                HistorialAvisos(contexto),
                CredencialesCifradas(contexto, CredencialesCifradas.SENECA)
            )
        }
    )

    val sesionNetacad = remember { SesionNetacad(contexto) }
    val netacadViewModel: NetacadViewModel = viewModel(
        factory = fabrica {
            NetacadViewModel(
                AlmacenNetacad(contexto),
                sesionNetacad,
                HistorialAvisos(contexto),
                CredencialesCifradas(contexto, CredencialesCifradas.NETACAD)
            )
        }
    )

    val gruposViewModel: GruposViewModel = viewModel(
        factory = fabrica {
            val config = ConfigSupabase(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_CLAVE)
            // Compilada sin servidor, la pantalla explica qué falta en vez de fallar.
            val repositorio = if (config.configurado) {
                RepositorioGrupos(ClienteSupabase(config, GuardaSesionPrefs(contexto)))
            } else {
                null
            }
            GruposViewModel(repositorio)
        }
    )

    // Ajustes cuelga de Avisos: «atrás» vuelve ahí y no cierra la app.
    BackHandler(enabled = seccion == Seccion.AJUSTES) { seccion = Seccion.AVISOS }

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
        Recordatorios.crearCanales(contexto)
        if (Recordatorios.puedeNotificar(contexto)) {
            RecordatoriosWorker.programar(contexto)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Volver a la app es el momento en que se quiere ver lo último, así que ahí se refresca
    // todo. Cada pantalla decide si le toca: si acaba de cargar, no repite la consulta.
    val cicloDeVida = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(cicloDeVida) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                actividadesViewModel.refrescarSiConviene()
                notasViewModel.refrescarSiConviene()
                faltasViewModel.actualizarSiConviene()
                // El navegador que lee NetAcad vive en su pantalla: fuera de ella no hay quien lo haga.
                if (seccion == Seccion.NETACAD) netacadViewModel.actualizarSiConviene()
                avisosViewModel.recargar()
                actualizacionViewModel.comprobar()
            }
        }
        cicloDeVida.addObserver(observador)
        onDispose { cicloDeVida.removeObserver(observador) }
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
                    selected = seccion == Seccion.ACTIVIDADES || seccion == Seccion.NETACAD,
                    onClick = { seccion = Seccion.ACTIVIDADES },
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null
                        )
                    },
                    label = { EtiquetaBarra(Seccion.ACTIVIDADES.etiqueta) }
                )
                NavigationBarItem(
                    selected = seccion == Seccion.NOTAS,
                    onClick = { seccion = Seccion.NOTAS },
                    icon = { Icon(Icons.Default.Grade, contentDescription = null) },
                    label = { EtiquetaBarra(Seccion.NOTAS.etiqueta) }
                )
                NavigationBarItem(
                    selected = seccion == Seccion.FALTAS,
                    onClick = { seccion = Seccion.FALTAS },
                    icon = { Icon(Icons.Default.EventBusy, contentDescription = null) },
                    label = { EtiquetaBarra(Seccion.FALTAS.etiqueta) }
                )
                NavigationBarItem(
                    selected = seccion == Seccion.AVISOS || seccion == Seccion.AJUSTES,
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
                    label = { EtiquetaBarra(Seccion.AVISOS.etiqueta) }
                )
                NavigationBarItem(
                    selected = seccion == Seccion.HORARIO,
                    onClick = { seccion = Seccion.HORARIO },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { EtiquetaBarra(Seccion.HORARIO.etiqueta) }
                )
                NavigationBarItem(
                    selected = seccion == Seccion.GRUPOS,
                    onClick = { seccion = Seccion.GRUPOS },
                    icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    label = { EtiquetaBarra(Seccion.GRUPOS.etiqueta) }
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
                alAbrirNetacad = { seccion = Seccion.NETACAD },
                modifier = Modifier.padding(relleno)
            )

            Seccion.NETACAD -> NetacadScreen(
                viewModel = netacadViewModel,
                sesion = sesionNetacad,
                alVolver = { seccion = Seccion.ACTIVIDADES },
                modifier = Modifier.padding(relleno)
            )

            Seccion.NOTAS -> NotasScreen(
                viewModel = notasViewModel,
                alAbrirFaltas = { seccion = Seccion.FALTAS },
                modifier = Modifier.padding(relleno)
            )

            Seccion.FALTAS -> FaltasScreen(
                viewModel = faltasViewModel,
                sesion = sesionSeneca,
                modifier = Modifier.padding(relleno)
            )

            Seccion.AVISOS -> AvisosScreen(
                viewModel = avisosViewModel,
                alAbrirAjustes = { seccion = Seccion.AJUSTES },
                modifier = Modifier.padding(relleno)
            )

            // consumeWindowInsets: la barra inferior ya ocupa su sitio, y sin esto el teclado
            // del chat dejaría un hueco de su altura entre el cuadro de texto y el teclado.
            Seccion.GRUPOS -> GruposScreen(
                viewModel = gruposViewModel,
                modifier = Modifier
                    .padding(relleno)
                    .consumeWindowInsets(relleno)
            )

            Seccion.HORARIO -> {
                val horarioViewModel: HorarioViewModel = viewModel(
                    factory = fabrica { HorarioViewModel(AlmacenHorario(contexto)) }
                )
                val busViewModel: BusViewModel = viewModel(
                    factory = fabrica { BusViewModel(AlmacenBus(contexto)) }
                )
                // Un almacén aparte para el papel de la parada: es otro documento distinto
                // del horario de clase y no debe pisarlo.
                val documentoBus: HorarioViewModel = viewModel(
                    key = "documento-bus",
                    factory = fabrica { HorarioViewModel(AlmacenHorario(contexto, "horario_bus")) }
                )
                HorarioScreen(
                    viewModel = horarioViewModel,
                    busViewModel = busViewModel,
                    documentoBus = documentoBus,
                    modifier = Modifier.padding(relleno)
                )
            }

            Seccion.AJUSTES -> AcercaDeScreen(
                alVolver = { seccion = Seccion.AVISOS },
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
                alVerFaltasSeneca = { seccion = Seccion.FALTAS },
                modifier = Modifier.padding(relleno)
            )
        }
    }
}

private fun fabrica(crear: () -> ViewModel) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = crear() as T
}

/**
 * Una sola línea siempre. Con la letra grande del sistema, «Horario» y «Grupos» se partían
 * en dos («Horari / o»), subían el icono y descuadraban la barra entera.
 */
@Composable
private fun EtiquetaBarra(texto: String) {
    Text(texto, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
}
