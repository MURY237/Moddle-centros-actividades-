package com.asir.moodleactividades.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asir.moodleactividades.Dependencias
import com.asir.moodleactividades.fabrica
import com.asir.moodleactividades.notificaciones.Recordatorios
import com.asir.moodleactividades.notificaciones.RecordatoriosWorker
import com.asir.moodleactividades.ui.acercade.AcercaDeScreen
import com.asir.moodleactividades.ui.actividades.ActividadesScreen
import com.asir.moodleactividades.ui.actividades.ActividadesViewModel
import com.asir.moodleactividades.ui.actualizacion.ActualizacionViewModel
import com.asir.moodleactividades.ui.ajustes.AjustesViewModel
import com.asir.moodleactividades.ui.asistencia.AsistenciaViewModel
import com.asir.moodleactividades.ui.avisos.AvisosScreen
import com.asir.moodleactividades.ui.avisos.AvisosViewModel
import com.asir.moodleactividades.ui.bus.BusViewModel
import com.asir.moodleactividades.ui.faltas.FaltasScreen
import com.asir.moodleactividades.ui.faltas.FaltasViewModel
import com.asir.moodleactividades.ui.grupos.GruposScreen
import com.asir.moodleactividades.ui.grupos.GruposViewModel
import com.asir.moodleactividades.ui.horario.HorarioScreen
import com.asir.moodleactividades.ui.horario.HorarioViewModel
import com.asir.moodleactividades.ui.navegacion.BarraNavegacion
import com.asir.moodleactividades.ui.navegacion.Seccion
import com.asir.moodleactividades.ui.netacad.NetacadScreen
import com.asir.moodleactividades.ui.netacad.NetacadViewModel
import com.asir.moodleactividades.ui.notas.NotasScreen
import com.asir.moodleactividades.ui.notas.NotasViewModel

/**
 * La app con sesión: la barra inferior y la pantalla de la sección elegida. [generacion]
 * cambia con cada entrada y salida, y con ella los ViewModel de la cuenta de Moodle: si se
 * reutilizasen, arrastrarían el estado de la sesión anterior.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaPrincipal(
    dependencias: Dependencias,
    generacion: Int,
    alCerrarSesion: () -> Unit,
    alCaducarSesion: () -> Unit
) {
    val contexto = LocalContext.current
    // Guardable: al girar el móvil se seguía en la pantalla, pero se volvía a Tareas.
    var seccion by rememberSaveable { mutableStateOf(Seccion.ACTIVIDADES) }

    val actividades: ActividadesViewModel = viewModel(key = "actividades-$generacion", factory = fabrica(dependencias::actividades))
    val notas: NotasViewModel = viewModel(key = "notas-$generacion", factory = fabrica(dependencias::notas))
    val asistencia: AsistenciaViewModel = viewModel(key = "asistencia-$generacion", factory = fabrica(dependencias::asistencia))
    val actualizacion: ActualizacionViewModel = viewModel()
    val ajustes: AjustesViewModel = viewModel(factory = fabrica(dependencias::ajustes))
    val faltas: FaltasViewModel = viewModel(factory = fabrica(dependencias::faltas))
    val netacad: NetacadViewModel = viewModel(factory = fabrica(dependencias::netacad))
    val grupos: GruposViewModel = viewModel(factory = fabrica(dependencias::grupos))
    // Vive fuera de su pestaña porque el contador de no leídos se pinta en la barra inferior.
    val avisos: AvisosViewModel = viewModel(factory = fabrica(dependencias::avisos))

    val estadoActividades by actividades.estado.collectAsStateWithLifecycle()
    val estadoActualizacion by actualizacion.estado.collectAsStateWithLifecycle()
    val estadoAjustes by ajustes.estado.collectAsStateWithLifecycle()
    val estadoAsistencia by asistencia.estado.collectAsStateWithLifecycle()
    val estadoAvisos by avisos.estado.collectAsStateWithLifecycle()

    // Las subpantallas vuelven a su pestaña con «atrás» en vez de cerrar la app.
    BackHandler(enabled = seccion == Seccion.AJUSTES) { seccion = Seccion.AVISOS }

    LaunchedEffect(seccion) {
        if (seccion == Seccion.AVISOS) avisos.marcarLeidos()
    }

    LaunchedEffect(estadoActividades.sesionCaducada) {
        if (estadoActividades.sesionCaducada) alCaducarSesion()
    }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) RecordatoriosWorker.programar(contexto)
    }

    // Una vez por sesión y no en cada pestaña: dentro de una pestaña se repetía en cada cambio
    // de sección, volviendo a pedir el permiso.
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
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                actividades.refrescarSiConviene()
                notas.refrescarSiConviene()
                faltas.actualizarSiConviene()
                // El navegador que lee NetAcad vive en su pantalla: fuera de ella no hay quien lo haga.
                if (seccion == Seccion.NETACAD) netacad.actualizarSiConviene()
                avisos.recargar()
                actualizacion.comprobar()
            }
        }
        ciclo.addObserver(observador)
        onDispose { ciclo.removeObserver(observador) }
    }

    Scaffold(
        // La cabecera de cada pantalla pinta bajo la barra de estado y aplica su propio inset.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BarraNavegacion(actual = seccion, avisosSinLeer = estadoAvisos.sinLeer) { seccion = it }
        }
    ) { relleno ->
        val modificador = Modifier.padding(relleno)
        when (seccion) {
            Seccion.ACTIVIDADES -> ActividadesScreen(
                viewModel = actividades,
                actualizacion = estadoActualizacion,
                alInstalarActualizacion = { actualizacion.instalar(contexto) },
                alDescartarActualizacion = actualizacion::descartar,
                alAbrirNetacad = { seccion = Seccion.NETACAD },
                alCerrarSesion = alCerrarSesion,
                modifier = modificador
            )

            Seccion.NETACAD -> NetacadScreen(
                viewModel = netacad,
                sesion = dependencias.sesionNetacad,
                alVolver = { seccion = Seccion.ACTIVIDADES },
                modifier = modificador
            )

            Seccion.NOTAS -> NotasScreen(viewModel = notas, modifier = modificador)

            Seccion.FALTAS -> FaltasScreen(
                viewModel = faltas,
                sesion = dependencias.sesionSeneca,
                modifier = modificador
            )

            Seccion.AVISOS -> AvisosScreen(
                viewModel = avisos,
                alAbrirAjustes = { seccion = Seccion.AJUSTES },
                modifier = modificador
            )

            // consumeWindowInsets: la barra inferior ya ocupa su sitio, y sin esto el teclado
            // del chat dejaría un hueco de su altura entre el cuadro de texto y el teclado.
            Seccion.GRUPOS -> GruposScreen(
                viewModel = grupos,
                modifier = modificador.consumeWindowInsets(relleno)
            )

            Seccion.HORARIO -> HorarioScreen(
                viewModel = viewModel<HorarioViewModel>(factory = fabrica(dependencias::horario)),
                busViewModel = viewModel<BusViewModel>(factory = fabrica(dependencias::bus)),
                documentoBus = viewModel<HorarioViewModel>(key = "documento-bus", factory = fabrica(dependencias::documentoBus)),
                modifier = modificador
            )

            Seccion.AJUSTES -> AcercaDeScreen(
                alVolver = { seccion = Seccion.AVISOS },
                ajustes = estadoAjustes,
                puedeNotificar = Recordatorios.puedeNotificar(contexto),
                alCambiarEntregas = { ajustes.cambiarAvisoEntregas(it, contexto) },
                alCambiarNuevas = { ajustes.cambiarAvisoNuevas(it, contexto) },
                alCambiarNotas = { ajustes.cambiarAvisoNotas(it, contexto) },
                alCambiarAntelacion = { ajustes.cambiarAntelacion(it, contexto) },
                alCambiarFrecuencia = { ajustes.cambiarFrecuencia(it, contexto) },
                alCambiarHora = { ajustes.cambiarHora(it, contexto) },
                asistencia = estadoAsistencia,
                alComprobarAsistencia = asistencia::comprobar,
                alVerFaltasSeneca = { seccion = Seccion.FALTAS },
                modifier = modificador
            )
        }
    }
}
