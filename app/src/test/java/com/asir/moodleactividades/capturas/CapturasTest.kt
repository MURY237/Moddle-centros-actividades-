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
import com.asir.moodleactividades.data.net.Actualizacion
import com.asir.moodleactividades.ui.actividades.AccionesTareas
import com.asir.moodleactividades.ui.actividades.ActividadesUiState
import com.asir.moodleactividades.ui.actividades.DescargaUi
import com.asir.moodleactividades.ui.actividades.DetalleActividadContenido
import com.asir.moodleactividades.ui.actividades.EstadoDescarga
import com.asir.moodleactividades.ui.actividades.TareasContenido
import com.asir.moodleactividades.ui.actualizacion.ActualizacionUiState
import com.asir.moodleactividades.ui.componentes.Aviso
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
                momentoDatos = Muestras.ahora - 3 * 3_600_000L
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
        DetalleActividadContenido(
            actividad = Muestras.actividades.first { it.adjuntos.isNotEmpty() },
            descargas = mapOf(
                "https://moodle.ejemplo.es/a.pdf" to DescargaUi(EstadoDescarga.LISTA)
            )
        )
    }

    @Test fun detalleOscuro() = capturar("13-detalle-oscuro", oscuro = true) {
        DetalleActividadContenido(
            actividad = Muestras.actividades.first { it.adjuntos.isNotEmpty() },
            descargas = emptyMap()
        )
    }
}

fun capturar(nombre: String, oscuro: Boolean = false, contenido: @Composable () -> Unit) {
    captureRoboImage("build/capturas/$nombre.png") {
        MoodleActividadesTheme(oscuro = oscuro) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                contenido()
            }
        }
    }
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
