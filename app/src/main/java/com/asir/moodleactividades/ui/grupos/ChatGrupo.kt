package com.asir.moodleactividades.ui.grupos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.asir.moodleactividades.domain.Chat
import com.asir.moodleactividades.domain.LimitesGrupo
import com.asir.moodleactividades.domain.Mensaje
import com.asir.moodleactividades.ui.componentes.DivisorFila
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Una fila del chat: un mensaje o la marca del día en que empiezan los siguientes. */
private sealed interface FilaChat {
    val clave: String

    data class DeMensaje(val mensaje: Mensaje) : FilaChat {
        override val clave: String get() = "m-" + mensaje.id
    }

    data class DeDia(val dia: LocalDate) : FilaChat {
        override val clave: String get() = "d-$dia"
    }
}

/** Los mensajes en orden, con una marca delante del primero de cada día. */
private fun filasDe(mensajes: List<Mensaje>, zona: ZoneId): List<FilaChat> = buildList {
    var anterior: LocalDate? = null
    mensajes.forEach { mensaje ->
        val dia = mensaje.momento?.let { Instant.ofEpochSecond(it).atZone(zona).toLocalDate() }
        if (dia != null && dia != anterior) {
            add(FilaChat.DeDia(dia))
            anterior = dia
        }
        add(FilaChat.DeMensaje(mensaje))
    }
}

@Composable
internal fun PanelChat(
    estado: GruposUiState,
    acciones: AccionesGrupos,
    hoy: LocalDate = LocalDate.now()
) {
    var texto by rememberSaveable { mutableStateOf("") }
    val zona = remember { ZoneId.systemDefault() }
    // Al revés: la lista va pegada abajo, a lo último, y se queda ahí cuando llega algo nuevo.
    val filas = remember(estado.mensajes) { filasDe(estado.mensajes, zona).asReversed() }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Box(modifier = Modifier.weight(1f)) {
            when {
                !estado.mensajesCargados -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                }
                estado.mensajes.isEmpty() -> EstadoVacio(
                    icono = Icons.Default.Forum,
                    titulo = "Nadie ha escrito todavía",
                    detalle = "Estrena el chat del grupo.",
                    modifier = Modifier.padding(top = Espacio.xxl)
                )
                else -> LazyColumn(
                    reverseLayout = true,
                    contentPadding = PaddingValues(horizontal = Espacio.lateral, vertical = Espacio.s),
                    verticalArrangement = Arrangement.spacedBy(Espacio.xs + 2.dp)
                ) {
                    items(filas, key = { it.clave }) { fila ->
                        when (fila) {
                            is FilaChat.DeDia -> MarcaDia(fila.dia, hoy)
                            is FilaChat.DeMensaje -> Burbuja(
                                mensaje = fila.mensaje,
                                propio = fila.mensaje.autor == estado.yo,
                                apodo = Chat.apodoDe(fila.mensaje.autor, estado.miembros),
                                zona = zona
                            )
                        }
                    }
                }
            }
        }

        DivisorFila()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Espacio.m, vertical = Espacio.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = texto,
                onValueChange = { if (it.length <= LimitesGrupo.MENSAJE) texto = it },
                placeholder = { Text("Escribe un mensaje") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.weight(1f)
            )
            FilledIconButton(
                onClick = {
                    val enviado = texto
                    // Solo se vacía si no se ha seguido escribiendo mientras se enviaba.
                    acciones.enviar(enviado) { if (texto == enviado) texto = "" }
                },
                enabled = LimitesGrupo.mensajeValido(texto) && !estado.enviando,
                modifier = Modifier.padding(start = Espacio.s).size(48.dp)
            ) {
                if (estado.enviando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}

@Composable
private fun MarcaDia(dia: LocalDate, hoy: LocalDate) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = Espacio.s), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(
                text = when (dia) {
                    hoy -> "Hoy"
                    hoy.minusDays(1) -> "Ayer"
                    else -> dia.format(DIA_LARGO).conMayuscula()
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Espacio.m, vertical = Espacio.xs)
            )
        }
    }
}

@Composable
private fun Burbuja(mensaje: Mensaje, propio: Boolean, apodo: String, zona: ZoneId) {
    val hora = mensaje.momento?.let { Instant.ofEpochSecond(it).atZone(zona).format(HORA) }.orEmpty()
    val fondo = if (propio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val tinta = if (propio) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (propio) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (propio) 16.dp else 4.dp,
                bottomEnd = if (propio) 4.dp else 16.dp
            ),
            color = fondo,
            border = if (propio) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = Espacio.m, vertical = Espacio.s)) {
                if (!propio) {
                    Text(
                        text = apodo,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(text = mensaje.texto, style = MaterialTheme.typography.bodyMedium, color = tinta)
                Text(
                    text = hora,
                    style = MaterialTheme.typography.labelSmall,
                    color = tinta.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
