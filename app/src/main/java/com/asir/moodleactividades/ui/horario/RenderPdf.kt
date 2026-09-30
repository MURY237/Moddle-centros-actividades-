package com.asir.moodleactividades.ui.horario

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

data class PaginaPdf(val imagen: Bitmap, val total: Int)

object RenderPdf {

    /** Ancho al que se rasteriza: suficiente para ampliar un horario sin verlo borroso. */
    private const val ANCHO_RENDER = 1800

    /**
     * Tope de píxeles de una página: una página muy alargada a 1800 de ancho pediría cientos
     * de megas y tumbaría la app. Unos 12 Mpx son 48 MB, que cualquier móvil aguanta.
     */
    private const val MAX_PIXELES = 12_000_000L

    /** Lado mayor al que se reduce una foto: la de una cámara de 50 Mpx no cabe en memoria. */
    private const val LADO_MAXIMO_FOTO = 2400

    fun renderizar(archivo: File, indice: Int): PaginaPdf? = runCatching {
        abrir(archivo).use { renderizador ->
            val total = renderizador.pageCount
            if (indice !in 0 until total) return null

            renderizador.openPage(indice).use { pagina ->
                var ancho = ANCHO_RENDER
                var alto = (pagina.height * (ancho.toFloat() / pagina.width)).toInt().coerceAtLeast(1)
                if (ancho.toLong() * alto > MAX_PIXELES) {
                    val reduccion = Math.sqrt(MAX_PIXELES.toDouble() / (ancho.toLong() * alto))
                    ancho = (ancho * reduccion).toInt().coerceAtLeast(1)
                    alto = (alto * reduccion).toInt().coerceAtLeast(1)
                }

                val imagen = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
                // Un PDF sin fondo se renderiza sobre transparencia y se vuelve ilegible.
                imagen.eraseColor(Color.WHITE)
                pagina.render(imagen, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                PaginaPdf(imagen, total)
            }
        }
    }.getOrNull()

    /**
     * Una foto del horario, reducida a potencias de dos hasta que su lado mayor no pase de
     * [LADO_MAXIMO_FOTO]. Antes se decodificaba entera, y en el hilo de la interfaz.
     */
    fun decodificarImagen(archivo: File): Bitmap? = runCatching {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.path, medidas)
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null

        var muestreo = 1
        while (maxOf(medidas.outWidth, medidas.outHeight) / (muestreo * 2) >= LADO_MAXIMO_FOTO) {
            muestreo *= 2
        }
        BitmapFactory.decodeFile(archivo.path, BitmapFactory.Options().apply { inSampleSize = muestreo })
    }.getOrNull()

    /** Si el PDF está roto, PdfRenderer falla al crearse y el descriptor quedaría abierto. */
    private fun abrir(archivo: File): PdfRenderer {
        val descriptor = ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY)
        return try {
            PdfRenderer(descriptor)
        } catch (e: Exception) {
            descriptor.close()
            throw e
        }
    }
}
