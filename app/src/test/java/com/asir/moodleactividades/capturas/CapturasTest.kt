package com.asir.moodleactividades.capturas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.asir.moodleactividades.data.AjustesAvisos
import com.asir.moodleactividades.data.net.Actualizacion
import com.asir.moodleactividades.domain.SondeoAsistencia
import com.asir.moodleactividades.ui.acercade.AcercaDeScreen
import com.asir.moodleactividades.ui.asistencia.AsistenciaUiState
import com.asir.moodleactividades.ui.actividades.AccionesTareas
import com.asir.moodleactividades.ui.actividades.ActividadesUiState
import com.asir.moodleactividades.ui.actividades.DescargaUi
import com.asir.moodleactividades.ui.actividades.DetalleActividadContenido
import com.asir.moodleactividades.ui.actividades.EstadoDescarga
import com.asir.moodleactividades.ui.actividades.TareasContenido
import com.asir.moodleactividades.ui.actualizacion.ActualizacionUiState
import com.asir.moodleactividades.ui.avisos.AccionesAvisos
import com.asir.moodleactividades.ui.avisos.AvisosContenido
import com.asir.moodleactividades.ui.bus.AccionesBus
import com.asir.moodleactividades.ui.bus.BusContenido
import com.asir.moodleactividades.ui.componentes.Aviso
import com.asir.moodleactividades.ui.faltas.AccionesFaltas
import com.asir.moodleactividades.ui.netacad.AccionesNetacad
import com.asir.moodleactividades.ui.netacad.NetacadContenido
import com.asir.moodleactividades.ui.netacad.NetacadUiState
import com.asir.moodleactividades.ui.faltas.FaltasContenido
import com.asir.moodleactividades.ui.faltas.FaltasUiState
import com.asir.moodleactividades.ui.horario.AccionesHorario
import com.asir.moodleactividades.ui.horario.HorarioContenido
import com.asir.moodleactividades.ui.horario.HorarioUiState
import com.asir.moodleactividades.ui.horario.VistaHorario
import com.asir.moodleactividades.ui.login.AccionesLogin
import com.asir.moodleactividades.ui.login.LoginContenido
import com.asir.moodleactividades.ui.login.LoginUiState
import com.asir.moodleactividades.ui.notas.AccionesNotas
import com.asir.moodleactividades.ui.notas.NotasContenido
import com.asir.moodleactividades.ui.componentes.BarraProgreso
import com.asir.moodleactividades.ui.componentes.CabeceraPantalla
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.componentes.EtiquetaEstado
import com.asir.moodleactividades.ui.componentes.FranjaResumen
import com.asir.moodleactividades.ui.componentes.IconoTonal
import com.asir.moodleactividades.ui.componentes.Metrica
import com.asir.moodleactividades.ui.componentes.Tarjeta
import com.asir.moodleactividades.ui.componentes.TituloSeccion
import com.asir.moodleactividades.ui.theme.MoodleActividadesTheme
import com.asir.moodleactividades.ui.theme.Tono
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Pinta pantallas y componentes a PNG sin emulador, con Robolectric. No comprueba nada: sirve
 * para ver el diseño en CI, que es el único sitio donde se compila la app. Solo se ejecuta
 * con `-Pcapturas=true`; en la batería normal se salta.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class CapturasTest {

    @Before
    fun soloSiSePiden() {
        Assume.assumeTrue(System.getProperty("capturas") == "true")
    }

    @Test fun catalogo() = capturar("00-catalogo") { Catalogo() }

    @Test fun catalogoOscuro() = capturar("00-catalogo-oscuro", oscuro = true) { Catalogo() }

    @Test fun tareas() = capturar("10-tareas") {
        TareasContenido(Muestras.tareas, ActualizacionUiState(), AccionesTareas())
    }

    @Test fun tareasOscuro() = capturar("10-tareas-oscuro", oscuro = true) {
        TareasContenido(Muestras.tareas, ActualizacionUiState(), AccionesTareas())
    }

    @Test fun tareasConAvisos() = capturar("11-tareas-avisos") {
        TareasContenido(
            Muestras.tareas.copy(
                datosDeCache = true,
                error = "No hay conexión con el centro.",
                momentoDatos = Muestras.ahora - 3 * Muestras.HORA
            ),
            ActualizacionUiState(disponible = Actualizacion("1.60", "", "")),
            AccionesTareas()
        )
    }

    @Test fun tareasError() = capturar("12-tareas-error") {
        TareasContenido(
            ActividadesUiState(error = "El servidor de Moodle no responde."),
            ActualizacionUiState(),
            AccionesTareas()
        )
    }

    @Test fun detalle() = capturar("13-detalle") {
        // La ficha va en una hoja inferior, que es de color superficie y no de fondo.
        Surface(color = MaterialTheme.colorScheme.surface) {
            DetalleActividadContenido(
                actividad = Muestras.actividades.first { it.adjuntos.isNotEmpty() },
                descargas = mapOf(
                    "https://moodle.ejemplo.es/a.pdf" to DescargaUi(EstadoDescarga.LISTA, File("enunciado.pdf"))
                )
            )
        }
    }

    @Test fun notas() = capturar("20-notas") { NotasContenido(Muestras.notas, AccionesNotas()) }

    @Test fun notasOscuro() = capturar("20-notas-oscuro", oscuro = true) {
        NotasContenido(Muestras.notas, AccionesNotas())
    }

    @Test fun avisos() = capturar("30-avisos") {
        AvisosContenido(Muestras.avisos, AccionesAvisos(), ahora = Muestras.ahora)
    }

    @Test fun avisosOscuro() = capturar("30-avisos-oscuro", oscuro = true) {
        AvisosContenido(Muestras.avisos, AccionesAvisos(), ahora = Muestras.ahora)
    }

    @Test fun faltas() = capturar("25-faltas", alto = 1100) { FaltasContenido(Muestras.faltas, AccionesFaltas()) }

    @Test fun faltasOscuro() = capturar("25-faltas-oscuro", oscuro = true, alto = 1100) {
        FaltasContenido(Muestras.faltas.copy(necesitaAcceso = true), AccionesFaltas())
    }

    @Test fun faltasVacio() = capturar("26-faltas-vacio") { FaltasContenido(FaltasUiState(), AccionesFaltas()) }

    @Test fun netacad() = capturar("15-netacad") { NetacadContenido(Muestras.netacad, AccionesNetacad()) }

    @Test fun netacadOscuro() = capturar("15-netacad-oscuro", oscuro = true) {
        NetacadContenido(Muestras.netacad.copy(necesitaAcceso = true), AccionesNetacad())
    }

    @Test fun netacadVacio() = capturar("16-netacad-vacio") { NetacadContenido(NetacadUiState(), AccionesNetacad()) }

    @Test fun login() = capturar("60-login", alto = 1000) {
        LoginContenido(LoginUiState(url = "https://educacionadistancia.juntadeandalucia.es/centros/sevilla"), AccionesLogin())
    }

    @Test fun loginOscuro() = capturar("60-login-oscuro", oscuro = true, alto = 1000) {
        LoginContenido(
            LoginUiState(url = "moodle.ejemplo.es", error = "No se ha podido contactar con ese servidor."),
            AccionesLogin()
        )
    }

    @Test fun horarioVacio() =capturar("50-horario-vacio") {
        HorarioContenido(HorarioUiState(), VistaHorario.CLASES, {}, AccionesHorario()) {}
    }

    @Test fun bus() = capturar("51-bus", alto = 1100) {
        HorarioContenido(HorarioUiState(), VistaHorario.BUS, {}, AccionesHorario()) {
            BusContenido(Muestras.bus, HorarioUiState(), AccionesBus())
        }
    }

    @Test fun busOscuro() = capturar("51-bus-oscuro", oscuro = true, alto = 1100) {
        HorarioContenido(HorarioUiState(), VistaHorario.BUS, {}, AccionesHorario()) {
            BusContenido(Muestras.bus, HorarioUiState(), AccionesBus())
        }
    }

    @Test fun ajustes() = capturar("40-ajustes", alto = 1500) { Ajustes() }

    @Test fun ajustesOscuro() = capturar("40-ajustes-oscuro", oscuro = true, alto = 1500) { Ajustes() }

    @Test fun detalleOscuro() = capturar("13-detalle-oscuro", oscuro = true) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            DetalleActividadContenido(
                actividad = Muestras.actividades.first { it.adjuntos.isNotEmpty() },
                descargas = emptyMap()
            )
        }
    }
}

/**
 * Pinta [contenido] con el tema de la app en `build/capturas/<nombre>.png`. Con [alto] la
 * pantalla se alarga para que quepa entera una que se desplaza.
 */
fun capturar(nombre: String, oscuro: Boolean = false, alto: Int? = null, contenido: @Composable () -> Unit) {
    if (alto != null) RuntimeEnvironment.setQualifiers("+h${alto}dp")
    captureRoboImage("build/capturas/$nombre.png") {
        MoodleActividadesTheme(oscuro = oscuro) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                contenido()
            }
        }
    }
}

@Composable
private fun Ajustes() {
    AcercaDeScreen(
        ajustes = AjustesAvisos(),
        puedeNotificar = false,
        alCambiarEntregas = {},
        alCambiarNuevas = {},
        alCambiarNotas = {},
        alCambiarAntelacion = {},
        alCambiarFrecuencia = {},
        alCambiarHora = {},
        asistencia = AsistenciaUiState(
            sondeo = SondeoAsistencia("moodle.ejemplo.es", "4.1.9", 412, emptyList())
        ),
        alComprobarAsistencia = {},
        alVerFaltasSeneca = {},
        alVolver = {}
    )
}

@Composable
private fun Catalogo() {
    Column {
        CabeceraPantalla(titulo = "Catálogo", subtitulo = "Componentes del rediseño") {
            IconButton(onClick = {}) { Icon(Icons.Default.Refresh, contentDescription = null) }
        }
        Column(
            modifier = Modifier.padding(horizontal = Espacio.lateral),
            verticalArrangement = Arrangement.spacedBy(Espacio.m)
        ) {
            FranjaResumen(
                listOf(
                    Metrica("4", "Pendientes", Tono.AVISO),
                    Metrica("12", "Entregadas", Tono.EXITO),
                    Metrica("1", "Sin entregar", Tono.PELIGRO)
                )
            )
            BarraProgreso(0.7f)
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                FilterChip(selected = true, onClick = {}, label = { Text("Todas") })
                FilterChip(selected = false, onClick = {}, label = { Text("Pendientes") })
            }
            Aviso(
                texto = "Sin conexión. Lo que ves es de la última consulta.",
                tono = Tono.AVISO,
                accion = "Reintentar",
                alPulsarAccion = {}
            )
            TituloSeccion("Próximos 7 días", extra = "2")
            Tarjeta(alPulsar = {}) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconoTonal(Icons.AutoMirrored.Filled.Assignment, Tono.AVISO)
                    Column(modifier = Modifier.padding(start = Espacio.m).weight(1f)) {
                        Text("Práctica 3: VLAN y enrutamiento", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Planificación y Administración de Redes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.padding(top = Espacio.s),
                            horizontalArrangement = Arrangement.spacedBy(Espacio.s)
                        ) {
                            EtiquetaEstado("Pendiente", Tono.AVISO)
                            EtiquetaEstado("Vence mañana", Tono.NEUTRO, conPunto = false)
                        }
                    }
                }
            }
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconoTonal(Icons.Default.Quiz, Tono.EXITO)
                    Column(modifier = Modifier.padding(start = Espacio.m).weight(1f)) {
                        Text("Cuestionario tema 2", style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.padding(top = Espacio.s),
                            horizontalArrangement = Arrangement.spacedBy(Espacio.s)
                        ) {
                            EtiquetaEstado("Entregada", Tono.EXITO)
                            EtiquetaEstado("Nota 8,5", Tono.INFO, conPunto = false)
                        }
                    }
                }
            }
            EstadoVacio(
                icono = Icons.AutoMirrored.Filled.Assignment,
                titulo = "Nada pendiente",
                detalle = "No hay ninguna actividad en tu Moodle ahora mismo."
            )
        }
    }
}
