package com.asir.moodleactividades.ui

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Los enlaces que llegan de fuera —la dirección de una actividad, de una nota o de un aviso
 * las pone el servidor— solo se abren si son páginas web. Sin mirarlo, una dirección con
 * otro esquema (tel:, content:, market:…) haría que la app lanzara lo que el servidor quisiera.
 */
fun esEnlaceWeb(url: String?): Boolean = url?.trim()?.toHttpUrlOrNull() != null
