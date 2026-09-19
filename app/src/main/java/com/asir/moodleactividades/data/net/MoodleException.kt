package com.asir.moodleactividades.data.net

class MoodleException(
    val codigo: String?,
    mensaje: String
) : Exception(mensaje) {

    val esTokenInvalido: Boolean
        get() = codigo in setOf("invalidtoken", "accessexception", "invalidsesskey")

    val esLoginInvalido: Boolean
        get() = codigo in setOf("invalidlogin", "invalidlogin_moreinfo")
}
