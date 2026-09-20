package com.asir.moodleactividades.ui.horario

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

data class PaginaPdf(val imagen: Bitmap, val total: Int)

object RenderPdf {

    /** Ancho al que se rasteriza: suficiente para ampliar un horario sin verlo borroso. */
    private const val ANCHO_RENDER = 1800

    fun numeroDePaginas(archivo: File): Int = runCatching {
        abrir(archivo).use { it.pageCount }
    }.getOrDefault(0)

    fun renderizar(archivo: File, indice: Int): PaginaPdf? = runCatching {
        abrir(archivo).use { renderizador ->
            val total = renderizador.pageCount
            if (indice !in 0 until total) return null

            renderizador.openPage(indice).use { pagina ->
                val escala = ANCHO_RENDER.toFloat() / pagina.width
                val alto = (pagina.height * escala).toInt().coerceAtLeast(1)

                val imagen = Bitmap.createBitmap(ANCHO_RENDER, alto, Bitmap.Config.ARGB_8888)
                // Un PDF sin fondo se renderiza sobre transparencia y se vuelve ilegible.
                imagen.eraseColor(Color.WHITE)
                pagina.render(imagen, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                PaginaPdf(imagen, total)
            }
        }
    }.getOrNull()

    private fun abrir(archivo: File): PdfRenderer =
        PdfRenderer(ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY))
}
