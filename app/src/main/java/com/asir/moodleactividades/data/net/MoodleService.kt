package com.asir.moodleactividades.data.net

import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface MoodleService {

    @FormUrlEncoded
    @POST("login/token.php")
    suspend fun obtenerToken(@FieldMap parametros: Map<String, String>): String

    @FormUrlEncoded
    @POST("webservice/rest/server.php")
    suspend fun llamar(@FieldMap parametros: Map<String, String>): String
}
