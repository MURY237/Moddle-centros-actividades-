package com.asir.moodleactividades.ui.netacad

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.viewinterop.AndroidView
import com.asir.moodleactividades.data.Credenciales
import com.asir.moodleactividades.data.SesionNetacad
import com.asir.moodleactividades.data.netacad.AutoAccesoNetacad
import com.asir.moodleactividades.data.netacad.ExtractorNetacad
import com.asir.moodleactividades.data.netacad.RespuestaNetacad
import com.asir.moodleactividades.data.netacad.ResultadoNetacad
import com.asir.moodleactividades.ui.componentes.CabeceraPantalla
import com.asir.moodleactividades.ui.componentes.Espacio
import com.asir.moodleactividades.ui.theme.Tono
import com.asir.moodleactividades.ui.theme.colores

/*
 * El navegador de NetAcad y el recorrido que hace dentro: sondear cada página hasta que
 * aparecen los trabajos, pasar a la siguiente página de curso guardada y, si aparece el
 * acceso de Cisco, entrar con la cuenta guardada. Va aparte de la pantalla: esto habla con
 * una web ajena; la pantalla solo pinta trabajos.
 */

private const val ANCHO_OCULTO = 1080
private const val ALTO_OCULTO = 1920
private const val ESPERA_MS = 1500L

/** NetAcad es una aplicación de una sola página: tarda en pintar tras cargar. ~18 s por página. */
private const val MAX_SONDEOS_OCULTO = 12

/** Tope por página a oscuras aunque la página siga navegando sola y reinicie el recuento. */
private const val MAX_TOTAL_OCULTO = 30

/** A la vista se mira un rato tras cada carga; luego se espera a que el alumno navegue. */
private const val MAX_SONDEOS_VISIBLE = 20

/** «Buscar aquí» responde rápido: si en unos segundos no hay nada, no lo va a haber. */
private const val MAX_SONDEOS_MANUAL = 3

/**
 * El OAuth de Cisco pasa por su página de acceso aunque la sesión siga viva, y rebota solo.
 * Solo se da por caducada si el acceso sigue ahí varios sondeos seguidos.
 */
private const val ACCESOS_PARA_RENDIRSE = 3

/** Con cuenta guardada se espera más al formulario de Cisco: si no, no da tiempo a rellenarlo. */
private const val ACCESOS_CON_CUENTA = 8

/** Pasos de correo antes de dejarlo: más ya es que el formulario no avanza. */
private const val MAX_CORREOS = 3

/**
 * Envíos de contraseña como mucho. Insistir con una que no vale bloquea la cuenta de
 * Cisco, así que dos: el primero, y otro por si el primero se perdió por el camino.
 */
private const val MAX_CLAVES = 2

/** Tras mandar un paso, Cisco tarda en pasar al siguiente. */
private const val ESPERA_ACCESO_MS = 2_500L

/** Presupuesto total del recorrido oculto por página, por si una nunca llega a terminar. */
internal const val VIGILANTE_POR_PAGINA_MS = 35_000L

/** Guarda el WebView y el recuento de sondeos sin ser estado de Compose. */
private class ContenedorNetacad {
    var web: WebView? = null

    /** Sube con cada carga de página: un sondeo de una página anterior ya no vale. */
    var generacion = 0
    var intentos = 0
    var totales = 0
    var accesosSeguidos = 0
    var visible = false
    var manual = false

    /**
     * Lo enviado al acceso de Cisco en toda la vida de este navegador. No se reinicia con
     * cada página, a diferencia de lo demás: es lo que evita repetir una contraseña mala.
     */
    var correosEnviados = 0
    var clavesEnviadas = 0
}

/** Lo que el sondeo puede hacer, sin atar las funciones de más abajo a Compose. */
private class RespuestasNavegador(
    val recibir: (ResultadoNetacad, String?) -> Boolean,
    val pedirAcceso: () -> Unit,
    val siguiente: () -> String?,
    val terminar: () -> Unit,
    val sinTrabajos: (List<String>) -> Unit,
    val cuenta: () -> Credenciales?,
    val anotar: (String) -> Unit,
    val rechazar: () -> Unit
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun NavegadorNetacad(
    visible: Boolean,
    viewModel: NetacadViewModel,
    sesion: SesionNetacad,
    ultimaLectura: String
) {
    val contenedor = remember { ContenedorNetacad() }
    contenedor.visible = visible
    var sinTrabajosAqui by remember { mutableStateOf<List<String>?>(null) }

    val acciones = remember(viewModel) {
        RespuestasNavegador(
            recibir = { resultado, url ->
                val hubo = viewModel.recibir(resultado, url)
                if (hubo) sinTrabajosAqui = null
                hubo
            },
            pedirAcceso = viewModel::pedirAcceso,
            siguiente = viewModel::siguientePagina,
            terminar = viewModel::terminarRecorrido,
            sinTrabajos = { pistas -> sinTrabajosAqui = pistas },
            cuenta = viewModel::cuentaParaEntrar,
            anotar = viewModel::anotarAcceso,
            rechazar = viewModel::rechazarCuenta
        )
    }

    // Antes de crear el WebView: si se reponen durante su construcción, la primera petición
    // puede salir sin ellas y NetAcad contesta con el acceso de Cisco.
    val paginaInicial = remember {
        sesion.restaurar()
        viewModel.paginaInicial()
    }

    BackHandler(enabled = visible) {
        val web = contenedor.web
        if (web != null && web.canGoBack()) web.goBack() else viewModel.cerrarNavegador()
    }

    // Al salir, cualquier sondeo pendiente se da por muerto.
    DisposableEffect(Unit) {
        onDispose {
            contenedor.generacion++
            contenedor.web?.stopLoading()
            contenedor.web = null
        }
    }

    Column(
        modifier = if (visible) {
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        } else {
            Modifier
        }
    ) {
        if (visible) {
            BarraNetacad(
                ultimaLectura = ultimaLectura,
                sinTrabajosAqui = sinTrabajosAqui,
                alCerrar = viewModel::cerrarNavegador,
                alRecargar = { contenedor.web?.reload() },
                alBuscarAqui = {
                    sinTrabajosAqui = null
                    empezar(contenedor, acciones, manual = true)
                }
            )
        }

        AndroidView(
            modifier = if (visible) {
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            } else {
                // Oculto no quiere decir diminuto: con 1 dp la página se maqueta en un píxel
                // y los elementos miden cero. Se le da tamaño de móvil y se le quita el sitio.
                Modifier
                    .layout { medible, _ ->
                        val colocable = medible.measure(Constraints.fixed(ANCHO_OCULTO, ALTO_OCULTO))
                        layout(0, 0) { colocable.place(0, 0) }
                    }
                    .alpha(0f)
            },
            factory = { contexto ->
                WebView(contexto).apply {
                    settings.javaScriptEnabled = true
                    // NetAcad no necesita leer ficheros del móvil: en Android 10 y anteriores
                    // venía permitido por defecto, y una página podría pedirlos.
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    // NetAcad guarda su estado en localStorage: sin esto no arranca.
                    settings.domStorageEnabled = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    // El acceso de Cisco rebota entre id.cisco.com y netacad.com.
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(vistaWeb: WebView?, url: String?) {
                            CookieManager.getInstance().flush()
                            sesion.guardar()
                            empezar(contenedor, acciones)
                        }

                        // NetAcad cambia de sección sin cargar página nueva: onPageFinished
                        // no se entera, pero el historial sí.
                        override fun doUpdateVisitedHistory(
                            vistaWeb: WebView?,
                            url: String?,
                            isReload: Boolean
                        ) {
                            empezar(contenedor, acciones)
                        }
                    }
                    contenedor.web = this
                    loadUrl(paginaInicial)
                }
            },
            // Cada refresco estrena navegador: sin destruir el anterior, su proceso de
            // renderizado seguía vivo hasta que el sistema lo reclamaba.
            onRelease = { web ->
                web.stopLoading()
                web.destroy()
            }
        )
    }
}



/** La barra del navegador a la vista: salir, buscar en esta página y recargar. */
@Composable
private fun BarraNetacad(
    ultimaLectura: String,
    sinTrabajosAqui: List<String>?,
    alCerrar: () -> Unit,
    alRecargar: () -> Unit,
    alBuscarAqui: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        CabeceraPantalla(titulo = "NetAcad", alVolver = alCerrar) {
            IconButton(onClick = alBuscarAqui) {
                Icon(Icons.Default.Search, contentDescription = "Buscar trabajos en esta página")
            }
            IconButton(onClick = alRecargar) {
                Icon(Icons.Default.Refresh, contentDescription = "Recargar")
            }
        }

        when {
            ultimaLectura.isNotBlank() -> Franja(Tono.EXITO) {
                Text(
                    text = "Leídos $ultimaLectura. Abre otro curso o pulsa Listo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = alCerrar) { Text("Listo") }
            }

            sinTrabajosAqui != null -> Franja(Tono.AVISO) {
                Text(
                    text = "No veo trabajos en esta página. Abre la de calificaciones del curso." +
                        if (sinTrabajosAqui.isEmpty()) "" else {
                            " Visto: " + sinTrabajosAqui.take(4).joinToString(" · ")
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            else -> Text(
                // El botón de Google se niega a funcionar dentro de una app, por política
                // suya: mejor avisarlo antes que dejar al alumno atascado en su pantalla.
                text = "Entra con tu correo y contraseña de Cisco (el botón de Google no " +
                    "funciona dentro de apps) y abre las calificaciones de tu curso: la app " +
                    "lo detecta sola.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Espacio.lateral, vertical = Espacio.s)
            )
        }
    }
}

@Composable
private fun Franja(tono: Tono, contenido: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tono.colores().contenedor)
            .padding(horizontal = Espacio.lateral, vertical = Espacio.s),
        verticalAlignment = Alignment.CenterVertically,
        content = contenido
    )
}

/** Arranca una tanda de sondeos para la página actual y deja muertas las anteriores. */
private fun empezar(c: ContenedorNetacad, a: RespuestasNavegador, manual: Boolean = false) {
    val web = c.web ?: return
    c.generacion++
    c.intentos = 0
    c.accesosSeguidos = 0
    c.manual = manual
    val generacion = c.generacion
    web.postDelayed({ sondear(c, generacion, a) }, if (manual) 0L else ESPERA_MS)
}

private fun sondear(c: ContenedorNetacad, generacion: Int, a: RespuestasNavegador) {
    val web = c.web ?: return
    if (generacion != c.generacion) return

    web.evaluateJavascript(ExtractorNetacad.GUION) { crudo ->
        if (generacion != c.generacion) return@evaluateJavascript
        c.totales++
        val resultado = RespuestaNetacad.leer(crudo) ?: ResultadoNetacad(pagina = "cargando")

        if (resultado.pideAcceso) {
            if (!entrarSolo(c, web, generacion, a)) esperarAcceso(c, web, generacion, a)
            return@evaluateJavascript
        }
        c.accesosSeguidos = 0

        if (a.recibir(resultado, web.url)) {
            if (!c.visible) pasarALaSiguiente(c, a)
            return@evaluateJavascript
        }

        c.intentos++
        val tope = when {
            c.manual -> MAX_SONDEOS_MANUAL
            c.visible -> MAX_SONDEOS_VISIBLE
            else -> MAX_SONDEOS_OCULTO
        }
        val agotado = c.intentos >= tope || (!c.visible && c.totales >= MAX_TOTAL_OCULTO)
        if (!agotado) {
            web.postDelayed({ sondear(c, generacion, a) }, ESPERA_MS)
            return@evaluateJavascript
        }

        if (c.visible) {
            if (c.manual) a.sinTrabajos(resultado.pistas)
        } else {
            pasarALaSiguiente(c, a)
        }
    }
}

/**
 * Delante está el acceso de Cisco y no se va a rellenar: a la vista lo hace el alumno; a
 * oscuras se espera un poco —el OAuth de Cisco pasa por ahí y rebota solo si la sesión sigue
 * viva— y si no se va, se deja de intentar y se pide entrar.
 */
private fun esperarAcceso(c: ContenedorNetacad, web: WebView, generacion: Int, a: RespuestasNavegador) {
    c.accesosSeguidos++
    val tope = if (a.cuenta() != null) ACCESOS_CON_CUENTA else ACCESOS_PARA_RENDIRSE
    if (c.accesosSeguidos >= tope) {
        if (!c.visible) {
            c.generacion++
            a.pedirAcceso()
        }
        return
    }
    // A la vista sin cuenta no hay nada que esperar: la carga siguiente vuelve a mirar sola.
    if (c.visible && a.cuenta() == null) return
    web.postDelayed({ sondear(c, generacion, a) }, ESPERA_MS)
}

/**
 * Rellena el paso del acceso de Cisco que haya en pantalla con la cuenta guardada. Devuelve
 * false si no le toca —no hay cuenta, ya se insistió bastante o la página no es de Cisco—,
 * y entonces se sigue como si no hubiera acceso automático.
 *
 * La comprobación del dominio se hace aquí, antes de meter la contraseña en ningún guion, y
 * otra vez dentro del guion: una página ajena no llega a tenerla en ningún momento.
 */
private fun entrarSolo(c: ContenedorNetacad, web: WebView, generacion: Int, a: RespuestasNavegador): Boolean {
    val cuenta = a.cuenta() ?: return false
    val url = web.url
    if (!AutoAccesoNetacad.hostPermitido(url)) {
        val host = runCatching { java.net.URI(url).host }.getOrNull() ?: "desconocida"
        a.anotar("la página de acceso ($host) no es de Cisco, así que no se escribe nada")
        return false
    }
    if (c.clavesEnviadas >= MAX_CLAVES) {
        a.anotar("contraseña enviada $MAX_CLAVES veces y Cisco sigue pidiendo acceso")
        return false
    }
    if (c.correosEnviados >= MAX_CORREOS) {
        a.anotar("el correo se envió $MAX_CORREOS veces y no aparece la contraseña")
        return false
    }

    web.evaluateJavascript(AutoAccesoNetacad.guion(cuenta.usuario, cuenta.clave)) { crudo ->
        if (generacion != c.generacion) return@evaluateJavascript
        val paso = RespuestaNetacad.leerAcceso(crudo)
        when {
            paso == null -> esperarAcceso(c, web, generacion, a)
            paso.ajena -> {
                a.anotar("la página de acceso (${paso.host}) no es de Cisco, así que no se escribe nada")
                esperarAcceso(c, web, generacion, a)
            }
            paso.error -> {
                // La única señal de que la cuenta no vale: se para y no se reintenta sola.
                c.generacion++
                a.rechazar()
                if (!c.visible) a.pedirAcceso()
            }
            paso.mfa -> {
                c.generacion++
                a.anotar("Cisco pide verificación en dos pasos, y eso hay que hacerlo a mano")
                if (!c.visible) a.pedirAcceso()
            }
            paso.accion == "correo" -> {
                c.correosEnviados++
                a.anotar("correo enviado por ${paso.via}")
                web.postDelayed({ sondear(c, generacion, a) }, ESPERA_ACCESO_MS)
            }
            paso.accion == "clave" -> {
                c.clavesEnviadas++
                a.anotar("contraseña enviada por ${paso.via} (intento ${c.clavesEnviadas})")
                web.postDelayed({ sondear(c, generacion, a) }, ESPERA_ACCESO_MS)
            }
            // El formulario aún no ha aparecido: se espera como sin cuenta.
            else -> esperarAcceso(c, web, generacion, a)
        }
    }
    return true
}

private fun pasarALaSiguiente(c: ContenedorNetacad, a: RespuestasNavegador) {
    // Corta los sondeos que queden de esta página antes de cambiar de página.
    c.generacion++
    c.totales = 0
    val siguiente = a.siguiente()
    if (siguiente == null) {
        a.terminar()
        return
    }
    c.web?.loadUrl(siguiente)
}
