package com.asir.moodleactividades.ui.notas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.asir.moodleactividades.domain.AjusteNota
import com.asir.moodleactividades.domain.Aprobado
import com.asir.moodleactividades.domain.Calificacion
import com.asir.moodleactividades.domain.OrigenVeredicto
import com.asir.moodleactividades.domain.Veredicto
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.EtiquetaEstado
import com.asir.moodleactividades.ui.componentes.TituloSeccion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaDetalleNota(
    calificacion: Calificacion,
    ajuste: AjusteNota?,
    veredicto: Veredicto,
    alAjustar: (AjusteNota) -> Unit,
    alCerrar: () -> Unit,
    alAbrirEnMoodle: () -> Unit
) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = alCerrar,
        sheetState = estadoHoja,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        DetalleNotaContenido(calificacion, ajuste, veredicto, alAjustar, alAbrirEnMoodle)
    }
}

/**
 * La ficha de una nota: sobre cuánto es, por qué sale aprobada o suspensa y, si Moodle no lo
 * dice bien, la forma de corregirlo. Va aparte de la hoja para poder capturarla sin abrirla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleNotaContenido(
    calificacion: Calificacion,
    ajuste: AjusteNota?,
    veredicto: Veredicto,
    alAjustar: (AjusteNota) -> Unit = {},
    alAbrirEnMoodle: () -> Unit = {}
) {
    val actual = ajuste ?: AjusteNota()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Espacio.xl)
            .padding(bottom = Espacio.xxl)
    ) {
        Text(
            text = calificacion.curso,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )
        Text(
            text = calificacion.nombre,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = Espacio.xs)
        )

        Row(
            modifier = Modifier.padding(top = Espacio.l),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CasillaNota(calificacion, veredicto)
            Column(modifier = Modifier.weight(1f).padding(horizontal = Espacio.m)) {
                Text(
                    text = veredicto.notaMaxima?.let { "sobre " + Aprobado.cifra(it) } ?: "Nota máxima desconocida",
                    style = MaterialTheme.typography.titleSmall
                )
                if (calificacion.porcentaje.isNotBlank()) {
                    Text(
                        text = calificacion.porcentaje,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            EtiquetaEstado(
                texto = when (veredicto.aprobada) {
                    true -> "Aprobada"
                    false -> "Suspensa"
                    null -> if (calificacion.calificada) "Sin decidir" else "Sin calificar"
                },
                tono = tonoDe(calificacion, veredicto)
            )
        }

        Text(
            text = explicacion(calificacion, veredicto),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Espacio.m)
        )

        // Sin nota no hay nada que aprobar; la nota máxima sí se puede dejar puesta.
        if (calificacion.calificada) {
            TituloSeccion("Resultado", fondo = MaterialTheme.colorScheme.surface)
            val opciones = listOf(null to "Automático", true to "Aprobada", false to "Suspensa")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                opciones.forEachIndexed { indice, (valor, etiqueta) ->
                    SegmentedButton(
                        selected = actual.aprobada == valor,
                        onClick = { alAjustar(actual.copy(aprobada = valor)) },
                        shape = SegmentedButtonDefaults.itemShape(indice, opciones.size),
                        label = { Text(etiqueta) }
                    )
                }
            }
        }

        TituloSeccion("Nota máxima", fondo = MaterialTheme.colorScheme.surface)
        CampoMaxima(calificacion, actual, alAjustar)

        if (calificacion.url != null) {
            Spacer(Modifier.height(Espacio.xl))
            Button(onClick = alAbrirEnMoodle, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Abrir en Moodle")
            }
        }

        Text(
            text = "Lo que cambies aquí se queda en este móvil: en Moodle no cambia nada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = Espacio.m)
        )
    }
}

@Composable
private fun CampoMaxima(calificacion: Calificacion, actual: AjusteNota, alAjustar: (AjusteNota) -> Unit) {
    val foco = LocalFocusManager.current
    var texto by rememberSaveable(calificacion.clave) {
        mutableStateOf(actual.notaMaxima?.let(Aprobado::cifra).orEmpty())
    }
    val escrita = Aprobado.maximaEscrita(texto)
    val aplicable = escrita != null && escrita != actual.notaMaxima
    val aplicar = {
        foco.clearFocus()
        alAjustar(actual.copy(notaMaxima = escrita))
    }

    Text(
        text = if (calificacion.notaMaxima > 0) {
            "Según Moodle es sobre ${Aprobado.cifra(calificacion.notaMaxima)}. Si no es así, pon la real."
        } else {
            "Moodle no dice sobre cuánto es esta actividad. Si lo sabes, ponlo aquí."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
        modifier = Modifier.padding(top = Espacio.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = texto,
            onValueChange = { if (it.length <= 8) texto = it },
            label = { Text("Nota máxima") },
            singleLine = true,
            isError = texto.isNotBlank() && escrita == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (aplicable) aplicar() }),
            modifier = Modifier.weight(1f)
        )
        FilledTonalButton(
            onClick = aplicar,
            enabled = aplicable,
            modifier = Modifier.padding(start = Espacio.m)
        ) {
            Text("Aplicar")
        }
    }
    if (actual.notaMaxima != null) {
        TextButton(onClick = {
            texto = ""
            alAjustar(actual.copy(notaMaxima = null))
        }) {
            Text("Usar la de Moodle")
        }
    }
}

private fun explicacion(calificacion: Calificacion, veredicto: Veredicto): String {
    val maxima = veredicto.notaMaxima?.let(Aprobado::cifra)
    val mitad = veredicto.notaMaxima?.let { Aprobado.cifra(it / 2) }
    return when (veredicto.origen) {
        OrigenVeredicto.MANUAL ->
            "La has marcado tú como " + (if (veredicto.aprobada == true) "aprobada" else "suspensa") + "."
        OrigenVeredicto.MAXIMA_PROPIA ->
            "Calculado sobre $maxima, la nota máxima que has puesto: se aprueba con $mitad."
        OrigenVeredicto.PORCENTAJE ->
            "Moodle la cuenta como un ${calificacion.porcentaje.trim()} de la nota máxima: se aprueba con el 50 %."
        OrigenVeredicto.MAXIMA_MOODLE ->
            "Moodle dice que la nota máxima es $maxima: se aprueba con $mitad."
        OrigenVeredicto.SUPUESTO ->
            "Moodle no dice sobre cuánto es, así que se supone sobre 10. Si es sobre otra nota, ponla abajo."
        OrigenVeredicto.TEXTO ->
            "Moodle la califica con palabras, no con un número."
        OrigenVeredicto.DESCONOCIDO ->
            if (calificacion.calificada) {
                "No se puede saber si está aprobada: pon abajo la nota máxima o márcala tú."
            } else {
                "Todavía no tiene nota."
            }
    }
}
