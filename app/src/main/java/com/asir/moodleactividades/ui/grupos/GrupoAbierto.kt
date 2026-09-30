package com.asir.moodleactividades.ui.grupos

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.asir.moodleactividades.domain.CodigoInvitacion
import com.asir.moodleactividades.domain.Grupo
import com.asir.moodleactividades.domain.LimitesGrupo
import com.asir.moodleactividades.domain.Miembro
import com.asir.moodleactividades.ui.componentes.CabeceraPantalla
import com.asir.moodleactividades.ui.componentes.DivisorFila
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.EtiquetaEstado
import com.asir.moodleactividades.ui.componentes.MenuMas
import com.asir.moodleactividades.ui.componentes.OpcionMenu
import com.asir.moodleactividades.ui.theme.Tono

/** Un grupo por dentro: cabecera con su código, y el chat o el calendario de exámenes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GrupoAbierto(
    grupo: Grupo,
    estado: GruposUiState,
    acciones: AccionesGrupos,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val portapapeles = LocalClipboardManager.current
    var viendoMiembros by rememberSaveable { mutableStateOf(false) }
    var cambiandoApodo by rememberSaveable { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf<Confirmacion?>(null) }

    BackHandler { acciones.cerrar() }

    val codigo = CodigoInvitacion.formatear(grupo.codigo)
    val copiar = {
        portapapeles.setText(AnnotatedString(codigo))
        acciones.avisar("Código copiado")
    }
    val compartir = {
        val texto = "Únete a «${grupo.nombre}» en la app de Actividades con el código $codigo"
        val envio = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
        runCatching { contexto.startActivity(Intent.createChooser(envio, "Compartir código")) }
        Unit
    }

    Column(modifier = modifier.fillMaxSize()) {
        CabeceraPantalla(
            titulo = grupo.nombre,
            subtitulo = "Código $codigo" +
                if (estado.miembros.isEmpty()) "" else " · ${estado.miembros.size} miembros",
            alVolver = acciones.cerrar
        ) {
            IconButton(onClick = compartir) {
                Icon(Icons.Default.Share, contentDescription = "Compartir código")
            }
            MenuMas(
                buildList {
                    add(OpcionMenu("Copiar código", Icons.Default.ContentCopy, alPulsar = copiar))
                    add(OpcionMenu("Miembros", Icons.Default.Groups) { viendoMiembros = true })
                    add(OpcionMenu("Cambiar mi apodo", Icons.Default.Edit) { cambiandoApodo = true })
                    if (estado.soyCreador) {
                        add(OpcionMenu("Cambiar el código", Icons.Default.Key) { confirmar = Confirmacion.RENOVAR })
                    }
                    add(OpcionMenu("Salir del grupo", Icons.AutoMirrored.Filled.ExitToApp, peligrosa = true) {
                        confirmar = Confirmacion.SALIR
                    })
                    if (estado.soyCreador) {
                        add(OpcionMenu("Eliminar grupo", Icons.Default.Delete, peligrosa = true) {
                            confirmar = Confirmacion.ELIMINAR
                        })
                    }
                }
            )
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Espacio.lateral)
                .padding(bottom = Espacio.s)
        ) {
            PestanaGrupo.entries.forEachIndexed { indice, opcion ->
                SegmentedButton(
                    selected = estado.pestana == opcion,
                    onClick = { acciones.elegirPestana(opcion) },
                    shape = SegmentedButtonDefaults.itemShape(indice, PestanaGrupo.entries.size),
                    icon = {
                        Icon(
                            if (opcion == PestanaGrupo.CHAT) Icons.Default.Forum else Icons.Default.Event,
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    },
                    label = { Text(opcion.etiqueta) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (estado.pestana) {
                PestanaGrupo.CHAT -> PanelChat(estado, acciones)
                PestanaGrupo.EXAMENES -> PanelExamenes(estado, acciones)
            }
        }
    }

    if (viendoMiembros) {
        DialogoMiembros(
            estado = estado,
            alExpulsar = { miembro -> confirmar = Confirmacion.Expulsar(miembro) },
            alCerrar = { viendoMiembros = false }
        )
    }

    if (cambiandoApodo) {
        DialogoApodo(
            actual = estado.miApodo,
            trabajando = estado.trabajando,
            alCancelar = { cambiandoApodo = false },
            alGuardar = { apodo -> acciones.cambiarApodo(apodo) { cambiandoApodo = false } }
        )
    }

    confirmar?.let { pregunta ->
        val (titulo, texto, boton) = when (pregunta) {
            Confirmacion.SALIR -> Triple(
                "¿Salir del grupo?",
                if (estado.soyCreador) {
                    "Si te vas, el grupo pasa al miembro más antiguo. Si no queda nadie, se borra."
                } else {
                    "Dejarás de ver el chat y el calendario. Para volver necesitarás el código."
                },
                "Salir"
            )
            Confirmacion.ELIMINAR -> Triple(
                "¿Eliminar el grupo?",
                "Se borran para todos el chat, el calendario y la lista de miembros. No se puede deshacer.",
                "Eliminar"
            )
            Confirmacion.RENOVAR -> Triple(
                "¿Cambiar el código?",
                "El código actual dejará de servir para entrar. Quien ya está dentro sigue dentro.",
                "Cambiar"
            )
            is Confirmacion.Expulsar -> Triple(
                "¿Expulsar a ${pregunta.miembro.apodo}?",
                "Dejará de ver el grupo. Podría volver a entrar si tiene el código: cámbialo si no quieres que pueda.",
                "Expulsar"
            )
        }
        val peligrosa = pregunta != Confirmacion.RENOVAR
        AlertDialog(
            onDismissRequest = { confirmar = null },
            title = { Text(titulo) },
            text = { Text(texto) },
            confirmButton = {
                TextButton(onClick = {
                    confirmar = null
                    when (pregunta) {
                        Confirmacion.SALIR -> acciones.salir()
                        Confirmacion.ELIMINAR -> acciones.eliminar()
                        Confirmacion.RENOVAR -> acciones.renovarCodigo()
                        is Confirmacion.Expulsar -> acciones.expulsar(pregunta.miembro)
                    }
                }) {
                    Text(boton, color = if (peligrosa) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = { TextButton(onClick = { confirmar = null }) { Text("Cancelar") } }
        )
    }
}

private sealed interface Confirmacion {
    data object SALIR : Confirmacion
    data object ELIMINAR : Confirmacion
    data object RENOVAR : Confirmacion
    data class Expulsar(val miembro: Miembro) : Confirmacion
}

@Composable
private fun DialogoApodo(actual: String, trabajando: Boolean, alCancelar: () -> Unit, alGuardar: (String) -> Unit) {
    var apodo by rememberSaveable { mutableStateOf(actual) }
    AlertDialog(
        onDismissRequest = alCancelar,
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text("Tu apodo en el grupo") },
        text = {
            OutlinedTextField(
                value = apodo,
                onValueChange = { if (it.length <= LimitesGrupo.APODO) apodo = it },
                singleLine = true,
                supportingText = { Text("Es lo que ven los demás junto a tus mensajes.") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { alGuardar(apodo) },
                enabled = LimitesGrupo.apodoValido(apodo) && apodo.trim() != actual && !trabajando
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = alCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogoMiembros(estado: GruposUiState, alExpulsar: (Miembro) -> Unit, alCerrar: () -> Unit) {
    val creador = estado.abierto?.creador
    AlertDialog(
        onDismissRequest = alCerrar,
        icon = { Icon(Icons.Default.Groups, contentDescription = null) },
        title = { Text("Miembros (${estado.miembros.size})") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                estado.miembros.forEachIndexed { indice, miembro ->
                    if (indice > 0) DivisorFila()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Espacio.s),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            miembro.apodo,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (miembro.usuario == estado.yo) {
                            EtiquetaEstado("Tú", Tono.NEUTRO, conPunto = false, modifier = Modifier.padding(start = Espacio.xs))
                        }
                        if (miembro.usuario == creador) {
                            EtiquetaEstado("Lo lleva", Tono.INFO, conPunto = false, modifier = Modifier.padding(start = Espacio.xs))
                        }
                        if (estado.soyCreador && miembro.usuario != estado.yo) {
                            IconButton(onClick = { alExpulsar(miembro) }) {
                                Icon(
                                    Icons.Default.PersonRemove,
                                    contentDescription = "Expulsar a ${miembro.apodo}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } }
    )
}
