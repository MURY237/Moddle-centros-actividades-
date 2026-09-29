package com.asir.moodleactividades.ui.grupos

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.asir.moodleactividades.domain.CalendarioExamenes
import com.asir.moodleactividades.domain.Chat
import com.asir.moodleactividades.domain.CodigoInvitacion
import com.asir.moodleactividades.domain.Examen
import com.asir.moodleactividades.domain.Grupo
import com.asir.moodleactividades.domain.LimitesGrupo
import com.asir.moodleactividades.domain.Mensaje
import com.asir.moodleactividades.domain.Miembro
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.theme.AmbarPendiente
import com.asir.moodleactividades.ui.theme.AmbarPendienteFondo
import com.asir.moodleactividades.ui.theme.AmbarPendienteOscuro
import com.asir.moodleactividades.ui.theme.DegradadoCabecera
import com.asir.moodleactividades.ui.theme.fondoDeEstado
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val ESPANOL = Locale.forLanguageTag("es-ES")
private val HORA = DateTimeFormatter.ofPattern("HH:mm")
private val DIA_Y_HORA = DateTimeFormatter.ofPattern("d MMM, HH:mm", ESPANOL)
private val DIA_LARGO = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ESPANOL)
private val DIA_CORTO = DateTimeFormatter.ofPattern("EEE d MMM", ESPANOL)

/** Cada cuánto se mira si hay mensajes nuevos con el chat abierto. */
private const val REFRESCO_CHAT_MS = 4_000L

/** El calendario cambia mucho menos: con medio minuto sobra. */
private const val REFRESCO_EXAMENES_MS = 30_000L

@Composable
fun GruposScreen(viewModel: GruposViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(estado.aviso) {
        val texto = estado.aviso ?: return@LaunchedEffect
        avisos.showSnackbar(texto)
        viewModel.descartarAviso()
    }

    Box(modifier = modifier.fillMaxSize()) {
        val abierto = estado.abierto
        if (abierto == null) {
            ListaGrupos(estado, viewModel)
        } else {
            GrupoAbierto(abierto, estado, viewModel)
        }
        SnackbarHost(avisos, modifier = Modifier.align(Alignment.BottomCenter))
    }

    // Por encima de todo, también de los diálogos abiertos: si falla «Unirme», el error
    // tiene que verse sin cerrar el diálogo, para poder corregir el código y reintentar.
    estado.error?.let { texto ->
        AlertDialog(
            onDismissRequest = viewModel::descartarError,
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text("No se pudo") },
            text = { Text(texto) },
            confirmButton = { TextButton(onClick = viewModel::descartarError) { Text("Entendido") } }
        )
    }
}

// ---------------------------------------------------------------------------- lista

@Composable
private fun ListaGrupos(estado: GruposUiState, viewModel: GruposViewModel) {
    var creando by remember { mutableStateOf(false) }
    var uniendo by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        CabeceraDegradado {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Grupos", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text(
                        text = when (estado.grupos.size) {
                            0 -> "Chat y exámenes con tu clase"
                            1 -> "1 grupo"
                            else -> "${estado.grupos.size} grupos"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }
                if (estado.configurado) {
                    IconButton(onClick = viewModel::cargar, enabled = !estado.cargando) {
                        Icon(Icons.Default.Refresh, "Actualizar", tint = Color.White)
                    }
                }
            }
        }

        if (estado.identidadRenovada) {
            Aviso(
                texto = "La sesión de grupos de este móvil se perdió y se ha creado otra. " +
                    "Vuelve a entrar en tus grupos con su código.",
                boton = "Vale",
                alPulsar = viewModel::descartarIdentidadRenovada
            )
        }

        when {
            !estado.configurado -> Centrado {
                EstadoVacio(
                    icono = Icons.Default.CloudOff,
                    titulo = "Grupos sin configurar",
                    detalle = "Esta versión de la app se compiló sin servidor de grupos. Hace " +
                        "falta un proyecto de Supabase con supabase/esquema.sql ejecutado; " +
                        "los pasos están en el README."
                )
            }

            estado.cargando && !estado.cargadoAlgunaVez -> Centrado { CircularProgressIndicator() }

            estado.grupos.isEmpty() -> Centrado {
                EstadoVacio(
                    icono = Icons.Default.Groups,
                    titulo = "Aún no estás en ningún grupo",
                    detalle = "Crea uno para tu clase y pasa el código, o entra con el código " +
                        "que te hayan dado. Dentro hay chat y un calendario de exámenes."
                ) {
                    BotonesGrupo(alCrear = { creando = true }, alUnirse = { uniendo = true })
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(estado.grupos, key = { it.id }) { grupo ->
                        TarjetaGrupo(grupo, esMio = grupo.creador == estado.yo) { viewModel.abrir(grupo) }
                    }
                }
                BotonesGrupo(
                    alCrear = { creando = true },
                    alUnirse = { uniendo = true },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    if (creando) {
        DialogoDosCampos(
            titulo = "Crear grupo",
            explicacion = "Luego podrás pasar el código a tus compañeros.",
            etiqueta1 = "Nombre del grupo (p. ej. 2º ASIR)",
            maximo1 = LimitesGrupo.NOMBRE,
            etiquetaApodo = "Tu apodo en el grupo",
            valido = { nombre, apodo -> LimitesGrupo.nombreValido(nombre) && LimitesGrupo.apodoValido(apodo) },
            confirmar = "Crear",
            trabajando = estado.trabajando,
            alCancelar = { creando = false },
            alConfirmar = { nombre, apodo -> viewModel.crear(nombre, apodo) { creando = false } }
        )
    }

    if (uniendo) {
        DialogoDosCampos(
            titulo = "Unirme con código",
            explicacion = "Da igual mayúsculas, espacios o guion. La O se lee como 0 y la I o " +
                "la L, como 1.",
            etiqueta1 = "Código (8 caracteres)",
            maximo1 = 12,
            codigo = true,
            etiquetaApodo = "Tu apodo en el grupo",
            valido = { codigo, apodo -> CodigoInvitacion.valido(codigo) && LimitesGrupo.apodoValido(apodo) },
            confirmar = "Unirme",
            trabajando = estado.trabajando,
            alCancelar = { uniendo = false },
            alConfirmar = { codigo, apodo -> viewModel.unirse(codigo, apodo) { uniendo = false } }
        )
    }
}

@Composable
private fun BotonesGrupo(alCrear: () -> Unit, alUnirse: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = alUnirse, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("Unirme")
        }
        Button(onClick = alCrear, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("Crear grupo")
        }
    }
}

@Composable
private fun TarjetaGrupo(grupo: Grupo, esMio: Boolean, alPulsar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = alPulsar),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = grupo.nombre.trim().take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    text = grupo.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (esMio) "Lo llevas tú" else "Miembro",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------- grupo

@Composable
private fun GrupoAbierto(grupo: Grupo, estado: GruposUiState, viewModel: GruposViewModel) {
    val contexto = LocalContext.current
    val portapapeles = LocalClipboardManager.current
    var menu by remember { mutableStateOf(false) }
    var viendoMiembros by remember { mutableStateOf(false) }
    var cambiandoApodo by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf<Confirmacion?>(null) }

    BackHandler { viewModel.cerrar() }

    // Solo con la app a la vista: en segundo plano no se gasta batería ni datos preguntando.
    val ciclo = LocalLifecycleOwner.current.lifecycle
    val pestana = estado.pestana
    LaunchedEffect(grupo.id, pestana) {
        ciclo.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                if (pestana == PestanaGrupo.CHAT) {
                    viewModel.traerMensajes()
                    delay(REFRESCO_CHAT_MS)
                } else {
                    viewModel.traerExamenes()
                    delay(REFRESCO_EXAMENES_MS)
                }
            }
        }
    }

    val compartir: () -> Unit = {
        val texto = "Únete a «${grupo.nombre}» en la app de Actividades con el código " +
            CodigoInvitacion.formatear(grupo.codigo)
        val envio = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
        runCatching { contexto.startActivity(Intent.createChooser(envio, "Compartir código")) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CabeceraDegradado {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::cerrar) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver a los grupos", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = grupo.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Código " + CodigoInvitacion.formatear(grupo.codigo) +
                            if (estado.miembros.isEmpty()) "" else " · ${estado.miembros.size} miembros",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable {
                            portapapeles.setText(AnnotatedString(CodigoInvitacion.formatear(grupo.codigo)))
                            viewModel.avisar("Código copiado")
                        }
                    )
                }
                IconButton(onClick = compartir) {
                    Icon(Icons.Default.Share, "Compartir código", tint = Color.White)
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Default.MoreVert, "Más opciones", tint = Color.White)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Copiar código") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                            onClick = {
                                menu = false
                                portapapeles.setText(AnnotatedString(CodigoInvitacion.formatear(grupo.codigo)))
                                viewModel.avisar("Código copiado")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Miembros") },
                            leadingIcon = { Icon(Icons.Default.Groups, null) },
                            onClick = { menu = false; viendoMiembros = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Cambiar mi apodo") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menu = false; cambiandoApodo = true }
                        )
                        if (estado.soyCreador) {
                            DropdownMenuItem(
                                text = { Text("Cambiar el código") },
                                leadingIcon = { Icon(Icons.Default.Key, null) },
                                onClick = { menu = false; confirmar = Confirmacion.RENOVAR }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Salir del grupo") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                            onClick = { menu = false; confirmar = Confirmacion.SALIR }
                        )
                        if (estado.soyCreador) {
                            DropdownMenuItem(
                                text = { Text("Eliminar grupo", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; confirmar = Confirmacion.ELIMINAR }
                            )
                        }
                    }
                }
            }
        }

        TabRow(selectedTabIndex = estado.pestana.ordinal) {
            PestanaGrupo.entries.forEach { opcion ->
                Tab(
                    selected = estado.pestana == opcion,
                    onClick = { viewModel.elegirPestana(opcion) },
                    text = { Text(opcion.etiqueta) },
                    icon = {
                        Icon(
                            if (opcion == PestanaGrupo.CHAT) Icons.Default.Forum else Icons.Default.Event,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (estado.pestana) {
                PestanaGrupo.CHAT -> PanelChat(estado, viewModel)
                PestanaGrupo.EXAMENES -> PanelExamenes(estado, viewModel)
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
            alGuardar = { apodo -> viewModel.cambiarApodo(apodo) { cambiandoApodo = false } }
        )
    }

    confirmar?.let { pregunta ->
        val (titulo, texto, boton) = when (pregunta) {
            Confirmacion.SALIR -> Triple(
                "Salir del grupo",
                if (estado.soyCreador) {
                    "Si te vas, el grupo pasa al miembro más antiguo. Si no queda nadie, se borra."
                } else {
                    "Dejarás de ver el chat y el calendario. Para volver necesitarás el código."
                },
                "Salir"
            )
            Confirmacion.ELIMINAR -> Triple(
                "Eliminar grupo",
                "Se borran para todos el chat, el calendario y la lista de miembros. No se puede deshacer.",
                "Eliminar"
            )
            Confirmacion.RENOVAR -> Triple(
                "Cambiar el código",
                "El código actual dejará de servir para entrar. Quien ya está dentro sigue dentro.",
                "Cambiar"
            )
            is Confirmacion.Expulsar -> Triple(
                "Expulsar a ${pregunta.miembro.apodo}",
                "Dejará de ver el grupo. Podría volver a entrar si tiene el código: cámbialo si no quieres que pueda.",
                "Expulsar"
            )
        }
        AlertDialog(
            onDismissRequest = { confirmar = null },
            title = { Text(titulo) },
            text = { Text(texto) },
            confirmButton = {
                TextButton(onClick = {
                    confirmar = null
                    when (pregunta) {
                        Confirmacion.SALIR -> viewModel.salir()
                        Confirmacion.ELIMINAR -> viewModel.eliminar()
                        Confirmacion.RENOVAR -> viewModel.renovarCodigo()
                        is Confirmacion.Expulsar -> viewModel.expulsar(pregunta.miembro)
                    }
                }) { Text(boton) }
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

// ---------------------------------------------------------------------------- chat

@Composable
private fun PanelChat(estado: GruposUiState, viewModel: GruposViewModel) {
    var texto by rememberSaveable { mutableStateOf("") }
    val hoy = remember { LocalDate.now() }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Box(modifier = Modifier.weight(1f)) {
            when {
                !estado.mensajesCargados -> Centrado { CircularProgressIndicator() }
                estado.mensajes.isEmpty() -> Centrado {
                    EstadoVacio(
                        icono = Icons.Default.Forum,
                        titulo = "Nadie ha escrito todavía",
                        detalle = "Estrena el chat del grupo."
                    )
                }
                // Al revés: lo último abajo y la lista pegada a ello cuando llega algo nuevo.
                else -> LazyColumn(
                    reverseLayout = true,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(estado.mensajes.asReversed(), key = { it.id }) { mensaje ->
                        Burbuja(
                            mensaje = mensaje,
                            propio = mensaje.autor == estado.yo,
                            apodo = Chat.apodoDe(mensaje.autor, estado.miembros),
                            hoy = hoy
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = texto,
                onValueChange = { if (it.length <= LimitesGrupo.MENSAJE) texto = it },
                placeholder = { Text("Escribe un mensaje") },
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { viewModel.enviar(texto) { texto = "" } },
                enabled = LimitesGrupo.mensajeValido(texto) && !estado.enviando
            ) {
                if (estado.enviando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, "Enviar")
                }
            }
        }
    }
}

@Composable
private fun Burbuja(mensaje: Mensaje, propio: Boolean, apodo: String, hoy: LocalDate) {
    val cuando = mensaje.momento?.let { segundos ->
        val local = Instant.ofEpochSecond(segundos).atZone(ZoneId.systemDefault())
        if (local.toLocalDate() == hoy) local.format(HORA) else local.format(DIA_Y_HORA)
    }.orEmpty()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (propio) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (propio) 18.dp else 4.dp,
                        bottomEnd = if (propio) 4.dp else 18.dp
                    )
                )
                .background(
                    if (propio) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (!propio) {
                Text(
                    text = apodo,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = mensaje.texto,
                style = MaterialTheme.typography.bodyMedium,
                color = if (propio) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = cuando,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

// ---------------------------------------------------------------------------- exámenes

@Composable
private fun PanelExamenes(estado: GruposUiState, viewModel: GruposViewModel) {
    val hoy = remember { LocalDate.now() }
    var mes by rememberSaveable { mutableStateOf(YearMonth.from(hoy)) }
    var elegido by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var anadiendo by remember { mutableStateOf(false) }
    var borrando by remember { mutableStateOf<Examen?>(null) }
    val porDia = remember(estado.examenes) { CalendarioExamenes.porDia(estado.examenes) }

    val dia = elegido
    val lista = if (dia != null) porDia[dia].orEmpty() else CalendarioExamenes.proximos(estado.examenes, hoy)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "calendario") {
                CalendarioMes(
                    mes = mes,
                    examenesPorDia = porDia,
                    hoy = hoy,
                    seleccionado = elegido,
                    alCambiarMes = { mes = it },
                    alElegirDia = { elegido = it }
                )
            }
            item(key = "titulo") {
                Text(
                    text = if (dia != null) {
                        dia.format(DIA_LARGO).replaceFirstChar { it.uppercase(ESPANOL) }
                    } else {
                        "Próximos exámenes"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            when {
                !estado.examenesCargados -> item(key = "cargando") {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                lista.isEmpty() -> item(key = "vacio") {
                    Text(
                        text = if (dia != null) "Ningún examen ese día." else "No hay exámenes apuntados por delante.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> items(lista, key = { "examen-" + it.id }) { examen ->
                    TarjetaExamen(
                        examen = examen,
                        hoy = hoy,
                        autor = Chat.apodoDe(examen.autor, estado.miembros),
                        puedeBorrar = estado.puedeBorrar(examen),
                        alBorrar = { borrando = examen }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { anadiendo = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Añadir examen") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    if (anadiendo) {
        DialogoExamen(
            fechaInicial = elegido ?: hoy,
            trabajando = estado.trabajando,
            alCancelar = { anadiendo = false },
            alGuardar = { asignatura, fecha, hora, notas ->
                viewModel.anadirExamen(asignatura, fecha, hora, notas) {
                    anadiendo = false
                    // Se lleva al alumno al mes del examen, para que lo vea puesto.
                    mes = YearMonth.from(fecha)
                }
            }
        )
    }

    borrando?.let { examen ->
        AlertDialog(
            onDismissRequest = { borrando = null },
            title = { Text("Borrar examen") },
            text = { Text("Se borra «${examen.asignatura}» del calendario de todo el grupo.") },
            confirmButton = {
                TextButton(onClick = {
                    borrando = null
                    viewModel.borrarExamen(examen)
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { borrando = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TarjetaExamen(
    examen: Examen,
    hoy: LocalDate,
    autor: String,
    puedeBorrar: Boolean,
    alBorrar: () -> Unit
) {
    val faltan = CalendarioExamenes.diasHasta(examen, hoy)
    val urgente = faltan != null && faltan in 0..3
    val cuenta = when {
        faltan == null -> ""
        faltan == 0L -> " · Hoy"
        faltan == 1L -> " · Mañana"
        faltan > 1 -> " · Faltan $faltan días"
        else -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (urgente) fondoDeEstado(AmbarPendienteFondo, AmbarPendienteOscuro)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(examen.asignatura, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = buildString {
                        examen.dia?.let { append(it.format(DIA_CORTO).replaceFirstChar { c -> c.uppercase(ESPANOL) }) }
                        examen.horaLocal?.let { append(" · ").append(it.format(HORA)) }
                        append(cuenta)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (urgente) AmbarPendiente else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (examen.notas.isNotBlank()) {
                    Text(
                        text = examen.notas,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    text = "Lo apuntó $autor",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (puedeBorrar) {
                IconButton(onClick = alBorrar) {
                    Icon(Icons.Default.Delete, "Borrar examen", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoExamen(
    fechaInicial: LocalDate,
    trabajando: Boolean,
    alCancelar: () -> Unit,
    alGuardar: (String, LocalDate, LocalTime?, String) -> Unit
) {
    var asignatura by rememberSaveable { mutableStateOf("") }
    var fecha by remember { mutableStateOf(fechaInicial) }
    var hora by remember { mutableStateOf<LocalTime?>(null) }
    var notas by rememberSaveable { mutableStateOf("") }
    var eligiendoFecha by remember { mutableStateOf(false) }
    var eligiendoHora by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = alCancelar,
        icon = { Icon(Icons.Default.Event, contentDescription = null) },
        title = { Text("Nuevo examen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = asignatura,
                    onValueChange = { if (it.length <= LimitesGrupo.ASIGNATURA) asignatura = it },
                    label = { Text("Asignatura") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                OutlinedButton(onClick = { eligiendoFecha = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(fecha.format(DIA_LARGO).replaceFirstChar { it.uppercase(ESPANOL) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { eligiendoHora = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(hora?.format(HORA) ?: "Sin hora")
                    }
                    if (hora != null) {
                        TextButton(onClick = { hora = null }) { Text("Quitar") }
                    }
                }
                OutlinedTextField(
                    value = notas,
                    onValueChange = { if (it.length <= LimitesGrupo.NOTAS) notas = it },
                    label = { Text("Notas (temas, aula…)") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { alGuardar(asignatura, fecha, hora, notas) },
                enabled = LimitesGrupo.asignaturaValida(asignatura) && !trabajando
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = alCancelar) { Text("Cancelar") } }
    )

    if (eligiendoFecha) {
        // El selector trabaja en milisegundos UTC a medianoche: convertir con la zona del
        // móvil movería el día en cuanto la zona no fuese la de Greenwich.
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { eligiendoFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let {
                        fecha = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    eligiendoFecha = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { eligiendoFecha = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = estadoFecha)
        }
    }

    if (eligiendoHora) {
        val estadoHora = rememberTimePickerState(
            initialHour = hora?.hour ?: 9,
            initialMinute = hora?.minute ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { eligiendoHora = false },
            title = { Text("Hora del examen") },
            text = { TimeInput(state = estadoHora) },
            confirmButton = {
                TextButton(onClick = {
                    hora = LocalTime.of(estadoHora.hour, estadoHora.minute)
                    eligiendoHora = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { eligiendoHora = false }) { Text("Cancelar") } }
        )
    }
}

// ---------------------------------------------------------------------------- diálogos

@Composable
private fun DialogoDosCampos(
    titulo: String,
    explicacion: String,
    etiqueta1: String,
    maximo1: Int,
    etiquetaApodo: String,
    valido: (String, String) -> Boolean,
    confirmar: String,
    trabajando: Boolean,
    alCancelar: () -> Unit,
    alConfirmar: (String, String) -> Unit,
    codigo: Boolean = false
) {
    var campo1 by rememberSaveable { mutableStateOf("") }
    var apodo by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!trabajando) alCancelar() },
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(explicacion, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = campo1,
                    onValueChange = { if (it.length <= maximo1) campo1 = it },
                    label = { Text(etiqueta1) },
                    singleLine = true,
                    textStyle = if (codigo) {
                        MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace)
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = if (codigo) KeyboardCapitalization.Characters
                        else KeyboardCapitalization.Sentences
                    )
                )
                OutlinedTextField(
                    value = apodo,
                    onValueChange = { if (it.length <= LimitesGrupo.APODO) apodo = it },
                    label = { Text(etiquetaApodo) },
                    supportingText = { Text("Es lo que verán los demás. No hace falta tu nombre real.") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { alConfirmar(campo1, apodo) },
                enabled = valido(campo1, apodo) && !trabajando
            ) {
                if (trabajando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(confirmar)
            }
        },
        dismissButton = { TextButton(onClick = alCancelar, enabled = !trabajando) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogoApodo(actual: String, trabajando: Boolean, alCancelar: () -> Unit, alGuardar: (String) -> Unit) {
    var apodo by rememberSaveable { mutableStateOf(actual) }
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("Tu apodo en el grupo") },
        text = {
            OutlinedTextField(
                value = apodo,
                onValueChange = { if (it.length <= LimitesGrupo.APODO) apodo = it },
                singleLine = true
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
                estado.miembros.forEach { miembro ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(miembro.apodo, style = MaterialTheme.typography.bodyLarge)
                            val etiquetas = listOfNotNull(
                                "tú".takeIf { miembro.usuario == estado.yo },
                                "lleva el grupo".takeIf { miembro.usuario == creador }
                            )
                            if (etiquetas.isNotEmpty()) {
                                Text(
                                    etiquetas.joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (estado.soyCreador && miembro.usuario != estado.yo) {
                            IconButton(onClick = { alExpulsar(miembro) }) {
                                Icon(Icons.Default.PersonRemove, "Expulsar a ${miembro.apodo}")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } }
    )
}

// ---------------------------------------------------------------------------- piezas

@Composable
private fun CabeceraDegradado(contenido: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(DegradadoCabecera)
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(top = 10.dp, bottom = 18.dp)
        ) { contenido() }
    }
}

@Composable
private fun Centrado(contenido: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { contenido() }
}

@Composable
private fun Aviso(texto: String, boton: String, alPulsar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = fondoDeEstado(AmbarPendienteFondo, AmbarPendienteOscuro))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = AmbarPendiente)
            Text(
                text = texto,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
            )
            TextButton(onClick = alPulsar) { Text(boton) }
        }
    }
}
