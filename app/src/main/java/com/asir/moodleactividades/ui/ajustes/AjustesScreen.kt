package com.asir.moodleactividades.ui.ajustes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.asir.moodleactividades.data.AjustesAvisos
import com.asir.moodleactividades.ui.componentes.Aviso
import com.asir.moodleactividades.ui.componentes.DivisorFila
import com.asir.moodleactividades.ui.componentes.EntreFiltros
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TituloSeccion("Notificaciones")

        if (!puedeNotificar) {
            Aviso(
                titulo = "Notificaciones bloqueadas",
                texto = "Android tiene bloqueadas las notificaciones de esta app. Actívalas en " +
                    "los ajustes del sistema para que estos avisos lleguen.",
                tono = Tono.PELIGRO,
                icono = Icons.Default.NotificationsOff,
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
        }

        Text(
            text = "Android decide el momento exacto para ahorrar batería, así que las " +
                "comprobaciones pueden retrasarse unos minutos respecto a la hora elegida.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Espacio.xs, vertical = Espacio.s)
        )
    }
}

private val HORAS = listOf(6, 7, 8, 9, 12, 15, 18, 20, 22)

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
