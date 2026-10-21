package com.asir.moodleactividades.ui.ajustes

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.asir.moodleactividades.data.AjustesAvisos
import com.asir.moodleactividades.notificaciones.Empuje
import com.asir.moodleactividades.ui.componentes.Aviso
import com.asir.moodleactividades.ui.componentes.DivisorFila
import com.asir.moodleactividades.ui.componentes.EntreFiltros
import com.asir.moodleactividades.ui.componentes.FilaAjuste
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.FilaInterruptor
import com.asir.moodleactividades.ui.componentes.Tarjeta
import com.asir.moodleactividades.ui.componentes.TituloSeccion
import com.asir.moodleactividades.ui.theme.Tono

@Composable
fun AjustesAvisosSeccion(
    ajustes: AjustesAvisos,
    puedeNotificar: Boolean,
    alCambiarEntregas: (Boolean) -> Unit,
    alCambiarNuevas: (Boolean) -> Unit,
    alCambiarNotas: (Boolean) -> Unit,
    alCambiarAntelacion: (Int) -> Unit,
    alCambiarFrecuencia: (Int) -> Unit,
    alCambiarHora: (Int) -> Unit,
    modifier: Modifier = Modifier,
    alCambiarGrupos: (Boolean) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TituloSeccion("Notificaciones")

        if (!puedeNotificar) {
            val contexto = LocalContext.current
            Aviso(
                titulo = "Notificaciones bloqueadas",
                texto = "Android tiene bloqueadas las notificaciones de esta app. Actívalas en " +
                    "los ajustes del sistema para que estos avisos lleguen.",
                tono = Tono.PELIGRO,
                icono = Icons.Default.NotificationsOff,
                accion = "Abrir ajustes",
                alPulsarAccion = {
                    runCatching {
                        contexto.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
                        )
                    }
                },
                modifier = Modifier.padding(bottom = Espacio.m)
            )
        }

        Tarjeta(relleno = PaddingValues(0.dp)) {
            FilaInterruptor(
                titulo = "Entregas cercanas",
                detalle = "Tareas sin entregar cuyo plazo se acerca.",
                icono = Icons.Default.NotificationsActive,
                tono = Tono.AVISO,
                activo = ajustes.avisarEntregas,
                alCambiar = alCambiarEntregas
            )
            if (ajustes.avisarEntregas) {
                FilaOpciones(
                    titulo = "Con cuánta antelación",
                    opciones = AjustesAvisos.ANTELACIONES,
                    seleccionada = ajustes.antelacionHoras,
                    etiqueta = { AjustesAvisos.etiquetaAntelacion(it) },
                    alElegir = alCambiarAntelacion
                )
            }
            DivisorFila()
            FilaInterruptor(
                titulo = "Actividades nuevas",
                detalle = "Cuando un profesor publica una tarea o un examen.",
                icono = Icons.Default.NewReleases,
                tono = Tono.INFO,
                activo = ajustes.avisarNuevas,
                alCambiar = alCambiarNuevas
            )
            DivisorFila()
            FilaInterruptor(
                titulo = "Notas publicadas",
                detalle = "En cuanto un profesor califica una tarea o un examen.",
                icono = Icons.Default.Grade,
                tono = Tono.EXITO,
                activo = ajustes.avisarNotas,
                alCambiar = alCambiarNotas
            )
            DivisorFila()
            FilaInterruptor(
                titulo = "Mensajes de los grupos",
                detalle = if (Empuje.disponible(LocalContext.current)) {
                    "Mensajes del chat y exámenes nuevos, al momento y con la app cerrada."
                } else {
                    "Mensajes del chat y exámenes nuevos, con la app cerrada. Se mira cada 15 minutos."
                },
                icono = Icons.Default.Forum,
                tono = Tono.INFO,
                activo = ajustes.avisarGrupos,
                alCambiar = alCambiarGrupos
            )
        }

        TituloSeccion("Comprobaciones")

        Tarjeta(relleno = PaddingValues(0.dp)) {
            FilaOpciones(
                titulo = "Veces al día",
                opciones = AjustesAvisos.FRECUENCIAS,
                seleccionada = ajustes.comprobacionesDiarias,
                etiqueta = { AjustesAvisos.etiquetaFrecuencia(it) },
                alElegir = alCambiarFrecuencia,
                arriba = Espacio.l
            )
            DivisorFila()
            FilaOpciones(
                titulo = "Primera del día",
                opciones = HORAS,
                seleccionada = ajustes.horaPreferida,
                etiqueta = { AjustesAvisos.etiquetaHora(it) },
                alElegir = alCambiarHora,
                arriba = Espacio.l
            )
            DivisorFila()
            FilaBateria()
        }

        Text(
            text = "Los avisos llegan con la app cerrada. Android decide el momento exacto para " +
                "ahorrar batería, así que pueden retrasarse unos minutos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Espacio.xs, vertical = Espacio.s)
        )
    }
}

private val HORAS = listOf(6, 7, 8, 9, 12, 15, 18, 20, 22)

/**
 * Con el ahorro de batería activo, muchos móviles (Xiaomi, Samsung, Huawei…) retrasan horas
 * o cortan las comprobaciones con la app cerrada, y los avisos solo llegaban al abrirla.
 */
@Composable
private fun FilaBateria() {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current.lifecycle
    var libre by remember { mutableStateOf(sinRestriccionBateria(contexto)) }
    // Se vuelve a mirar al volver de los ajustes de Android.
    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) libre = sinRestriccionBateria(contexto)
        }
        ciclo.addObserver(observador)
        onDispose { ciclo.removeObserver(observador) }
    }
    val alPulsar: (() -> Unit)? = if (libre) null else ({ pedirSinRestriccion(contexto) })
    FilaAjuste(
        titulo = if (libre) "Funciona en segundo plano" else "Batería: avisos retrasados",
        detalle = if (libre) {
            "Android deja a la app comprobar con la app cerrada."
        } else {
            "El ahorro de batería puede retrasar los avisos hasta que abras la app. Toca para quitarlo."
        },
        icono = if (libre) Icons.Default.BatteryFull else Icons.Default.BatteryAlert,
        tono = if (libre) Tono.EXITO else Tono.AVISO,
        alPulsar = alPulsar
    )
}

private fun sinRestriccionBateria(contexto: Context): Boolean =
    runCatching {
        contexto.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(contexto.packageName)
    }.getOrDefault(true)

// La app no está en Play, así que puede pedirlo directamente con el diálogo del sistema.
@SuppressLint("BatteryLife")
private fun pedirSinRestriccion(contexto: Context) {
    val directo = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        .setData(Uri.parse("package:" + contexto.packageName))
    if (runCatching { contexto.startActivity(directo) }.isFailure) {
        runCatching { contexto.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
    }
}

@Composable
private fun FilaOpciones(
    titulo: String,
    opciones: List<Int>,
    seleccionada: Int,
    etiqueta: (Int) -> String,
    alElegir: (Int) -> Unit,
    arriba: Dp = 0.dp
) {
    Column(
        modifier = Modifier.padding(top = arriba, bottom = Espacio.m),
        verticalArrangement = Arrangement.spacedBy(Espacio.xs)
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Espacio.l)
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Espacio.l),
            horizontalArrangement = EntreFiltros
        ) {
            opciones.forEach { opcion ->
                FilterChip(
                    selected = seleccionada == opcion,
                    onClick = { alElegir(opcion) },
                    label = { Text(etiqueta(opcion)) }
                )
            }
        }
    }
}
