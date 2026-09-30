package com.asir.moodleactividades.ui.actualizacion

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.asir.moodleactividades.ui.componentes.Aviso
import com.asir.moodleactividades.ui.theme.Tono

@Composable
fun BannerActualizacion(
    estado: ActualizacionUiState,
    alInstalar: () -> Unit,
    alDescartar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actualizacion = estado.disponible ?: return

    Aviso(
        titulo = "Versión ${actualizacion.version} disponible",
        texto = when {
            estado.error != null -> estado.error
            estado.faltaPermiso -> "Autoriza a la app a instalar aplicaciones y vuelve a pulsar Actualizar."
            estado.descargando -> "Descargando…"
            else -> "Se descarga e instala sin salir de la app."
        },
        tono = Tono.INFO,
        icono = Icons.Default.SystemUpdate,
        accion = if (estado.descargando) null else "Actualizar",
        alPulsarAccion = alInstalar,
        alCerrar = if (estado.descargando) null else alDescartar,
        modifier = modifier
    )
}
