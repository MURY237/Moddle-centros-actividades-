package com.asir.moodleactividades.ui.grupos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.asir.moodleactividades.domain.CodigoInvitacion
import com.asir.moodleactividades.domain.Examen
import com.asir.moodleactividades.domain.Grupo
import com.asir.moodleactividades.domain.LimitesGrupo
import com.asir.moodleactividades.domain.Miembro
import com.asir.moodleactividades.ui.componentes.Aviso
import com.asir.moodleactividades.ui.componentes.CabeceraPantalla
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.componentes.EstadoVacio
import com.asir.moodleactividades.ui.componentes.EtiquetaEstado
import com.asir.moodleactividades.ui.componentes.Tarjeta
import com.asir.moodleactividades.ui.componentes.TituloSeccion
import com.asir.moodleactividades.ui.theme.Tono
import com.asir.moodleactividades.ui.theme.colores
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

/** Cada cuánto se mira si hay mensajes nuevos con el chat abierto. */
private const val REFRESCO_CHAT_MS = 4_000L

/** El calendario cambia mucho menos: con medio minuto sobra. */
private const val REFRESCO_EXAMENES_MS = 30_000L

/**
 * Todo lo que las pantallas de grupos pueden pedir. Así se pintan sin ViewModel —para las
 * capturas— y el sondeo periódico queda en un solo sitio, fuera de lo que se dibuja.
 */
data class AccionesGrupos(
    val cargar: () -> Unit = {},
    val crear: (nombre: String, apodo: String, alTerminar: () -> Unit) -> Unit = { _, _, _ -> },
    val unirse: (codigo: String, apodo: String, alTerminar: () -> Unit) -> Unit = { _, _, _ -> },
    val abrir: (Grupo) -> Unit = {},
    val cerrar: () -> Unit = {},
    val elegirPestana: (PestanaGrupo) -> Unit = {},
    val enviar: (texto: String, alEnviar: () -> Unit) -> Unit = { _, _ -> },
    val anadirExamen: (String, LocalDate, LocalTime?, String, () -> Unit) -> Unit = { _, _, _, _, _ -> },
    val borrarExamen: (Examen) -> Unit = {},
    val cambiarApodo: (String, () -> Unit) -> Unit = { _, _ -> },
    val renovarCodigo: () -> Unit = {},
    val expulsar: (Miembro) -> Unit = {},
    val salir: () -> Unit = {},
    val eliminar: () -> Unit = {},
    val avisar: (String) -> Unit = {},
    val descartarIdentidadRenovada: () -> Unit = {}
)

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

    // Con un grupo abierto se pregunta cada poco por lo nuevo, y solo con la app a la vista:
    // en segundo plano no se gasta batería ni datos.
    val ciclo = LocalLifecycleOwner.current.lifecycle
    val abierto = estado.abierto?.id
    val pestana = estado.pestana
    LaunchedEffect(abierto, pestana) {
        if (abierto == null) return@LaunchedEffect
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

    Box(modifier = modifier.fillMaxSize()) {
        GruposContenido(
            estado = estado,
            acciones = AccionesGrupos(
                cargar = viewModel::cargar,
                crear = viewModel::crear,
                unirse = viewModel::unirse,
                abrir = viewModel::abrir,
                cerrar = viewModel::cerrar,
                elegirPestana = viewModel::elegirPestana,
                enviar = viewModel::enviar,
                anadirExamen = viewModel::anadirExamen,
                borrarExamen = viewModel::borrarExamen,
                cambiarApodo = viewModel::cambiarApodo,
                renovarCodigo = viewModel::renovarCodigo,
                expulsar = viewModel::expulsar,
                salir = viewModel::salir,
                eliminar = viewModel::eliminar,
                avisar = viewModel::avisar,
                descartarIdentidadRenovada = viewModel::descartarIdentidadRenovada
            )
        )
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

@Composable
fun GruposContenido(estado: GruposUiState, acciones: AccionesGrupos, modifier: Modifier = Modifier) {
    val abierto = estado.abierto
    if (abierto == null) {
        ListaGrupos(estado, acciones, modifier)
    } else {
        GrupoAbierto(abierto, estado, acciones, modifier)
    }
}

@Composable
private fun ListaGrupos(estado: GruposUiState, acciones: AccionesGrupos, modifier: Modifier = Modifier) {
    var creando by rememberSaveable { mutableStateOf(false) }
    var uniendo by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        CabeceraPantalla(
            titulo = "Grupos",
            subtitulo = when (estado.grupos.size) {
                0 -> "Chat y exámenes con tu clase"
                1 -> "1 grupo"
                else -> "${estado.grupos.size} grupos"
            }
        ) {
            if (estado.configurado) {
                if (estado.cargando) {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                } else {
                    IconButton(onClick = acciones.cargar) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                }
            }
        }

        if (estado.identidadRenovada) {
            Aviso(
                texto = "La sesión de grupos de este móvil se perdió y se ha creado otra. Vuelve a " +
                    "entrar en tus grupos con su código.",
                tono = Tono.AVISO,
                alCerrar = acciones.descartarIdentidadRenovada,
                modifier = Modifier.padding(horizontal = Espacio.lateral, vertical = Espacio.xs)
            )
        }

        when {
            !estado.configurado -> EstadoVacio(
                icono = Icons.Default.CloudOff,
                titulo = "Grupos sin configurar",
                detalle = "Esta versión de la app se compiló sin servidor de grupos. Hace falta un " +
                    "proyecto de Supabase con supabase/esquema.sql ejecutado; los pasos están en el README.",
                modifier = Modifier.padding(top = Espacio.xxl)
            )

            estado.cargando && !estado.cargadoAlgunaVez -> Box(
                Modifier.fillMaxWidth().padding(Espacio.xxl),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
            }

            estado.grupos.isEmpty() -> EstadoVacio(
                icono = Icons.Default.Groups,
                titulo = "Aún no estás en ningún grupo",
                detalle = "Crea uno para tu clase y pasa el código, o entra con el código que te " +
                    "hayan dado. Dentro hay chat y un calendario de exámenes.",
                modifier = Modifier.padding(top = Espacio.xxl)
            ) {
                BotonesGrupo(alCrear = { creando = true }, alUnirse = { uniendo = true })
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = Espacio.lateral, end = Espacio.lateral, bottom = Espacio.l),
                    verticalArrangement = Arrangement.spacedBy(Espacio.s)
                ) {
                    item(key = "titulo") { TituloSeccion("Tus grupos", extra = estado.grupos.size.toString()) }
                    items(estado.grupos, key = { it.id }) { grupo ->
                        TarjetaGrupo(grupo, esMio = grupo.creador == estado.yo) { acciones.abrir(grupo) }
                    }
                }
                BotonesGrupo(
                    alCrear = { creando = true },
                    alUnirse = { uniendo = true },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = Espacio.lateral, vertical = Espacio.m)
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
            valido = { nombre, apodo -> LimitesGrupo.nombreValido(nombre) && LimitesGrupo.apodoValido(apodo) },
            confirmar = "Crear",
            trabajando = estado.trabajando,
            alCancelar = { creando = false },
            alConfirmar = { nombre, apodo -> acciones.crear(nombre, apodo) { creando = false } }
        )
    }

    if (uniendo) {
        DialogoDosCampos(
            titulo = "Unirme con código",
            explicacion = "Da igual mayúsculas, espacios o guion. La O se lee como 0 y la I o la L, como 1.",
            etiqueta1 = "Código (8 caracteres)",
            maximo1 = 12,
            codigo = true,
            valido = { codigo, apodo -> CodigoInvitacion.valido(codigo) && LimitesGrupo.apodoValido(apodo) },
            confirmar = "Unirme",
            trabajando = estado.trabajando,
            alCancelar = { uniendo = false },
            alConfirmar = { codigo, apodo -> acciones.unirse(codigo, apodo) { uniendo = false } }
        )
    }
}

@Composable
private fun BotonesGrupo(alCrear: () -> Unit, alUnirse: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
        OutlinedButton(onClick = alUnirse, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(Espacio.s))
            Text("Unirme")
        }
        Button(onClick = alCrear, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(Espacio.s))
            Text("Crear grupo")
        }
    }
}

@Composable
private fun TarjetaGrupo(grupo: Grupo, esMio: Boolean, alPulsar: () -> Unit) {
    val colores = Tono.INFO.colores()
    Tarjeta(alPulsar = alPulsar, relleno = PaddingValues(Espacio.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // La inicial del grupo hace de icono: se distinguen de un vistazo sin leer.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colores.contenedor, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = grupo.nombre.trim().take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = colores.contenido
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = Espacio.m)) {
                Text(
                    text = grupo.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Código " + CodigoInvitacion.formatear(grupo.codigo),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (esMio) {
                EtiquetaEstado("Lo llevas tú", Tono.INFO, conPunto = false)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Crear o unirse: un campo propio (nombre o código) y el apodo con el que se aparece. */
@Composable
private fun DialogoDosCampos(
    titulo: String,
    explicacion: String,
    etiqueta1: String,
    maximo1: Int,
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
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                Text(explicacion, style = MaterialTheme.typography.bodyMedium)
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
                        capitalization = if (codigo) KeyboardCapitalization.Characters else KeyboardCapitalization.Sentences
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apodo,
                    onValueChange = { if (it.length <= LimitesGrupo.APODO) apodo = it },
                    label = { Text("Tu apodo en el grupo") },
                    supportingText = { Text("Es lo que verán los demás. No hace falta tu nombre real.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { alConfirmar(campo1, apodo) }, enabled = valido(campo1, apodo) && !trabajando) {
                if (trabajando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(confirmar)
            }
        },
        dismissButton = { TextButton(onClick = alCancelar, enabled = !trabajando) { Text("Cancelar") } }
    )
}
