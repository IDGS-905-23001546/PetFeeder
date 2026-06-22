package com.petfeeder.app

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/auth/registro")
    suspend fun registro(@Body body: RegistroRequest): Response<RespuestaResponse>

    @POST("api/auth/verificar")
    suspend fun verificar(@Body body: VerificarRequest): Response<RespuestaResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<UsuarioResponse>
}
