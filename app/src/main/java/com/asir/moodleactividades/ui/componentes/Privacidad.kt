package com.asir.moodleactividades.ui.componentes

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Mientras esta pantalla está delante no se puede capturar ni grabar, y no sale en la vista
 * de aplicaciones recientes. Es para donde se escriben contraseñas o tokens: otra app que
 * estuviera grabando la pantalla se los llevaría.
 */
@Composable
fun SinCapturas() {
    val actividad = LocalContext.current.actividad()
    DisposableEffect(actividad) {
        actividad?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { actividad?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

private tailrec fun Context.actividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.actividad()
    else -> null
}
