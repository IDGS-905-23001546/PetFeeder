package com.petfeeder.app

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    // ── AUTH ──────────────────────────────────────────────
    @POST("api/auth/registro")
    suspend fun registro(@Body body: RegistroRequest): Response<RespuestaResponse>

    @POST("api/auth/verificar")
    suspend fun verificar(@Body body: VerificarRequest): Response<RespuestaResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<UsuarioResponse>

    // ── MASCOTAS (petfeeder_db) ───────────────────────────
    @GET("api/mascotas/usuario/{usuarioId}")
    suspend fun getMascotas(@Path("usuarioId") usuarioId: Int): Response<List<MascotaApi>>

    @POST("api/mascotas")
    suspend fun crearMascota(@Body mascota: MascotaApi): Response<MascotaApi>

    @PUT("api/mascotas/{id}")
    suspend fun editarMascota(@Path("id") id: Int, @Body mascota: MascotaApi): Response<MascotaApi>

    @DELETE("api/mascotas/{id}")
    suspend fun borrarMascota(@Path("id") id: Int): Response<RespuestaResponse>

    // ── HORARIOS (comida) ─────────────────────────────────
    @GET("api/horarios/usuario/{usuarioId}")
    suspend fun getHorarios(@Path("usuarioId") usuarioId: Int): Response<List<HorarioApi>>

    @POST("api/horarios")
    suspend fun crearHorario(@Body h: HorarioApi): Response<HorarioApi>

    @PUT("api/horarios/{id}")
    suspend fun editarHorario(@Path("id") id: Int, @Body h: HorarioApi): Response<HorarioApi>

    @PUT("api/horarios/{id}/activo/{valor}")
    suspend fun activoHorario(@Path("id") id: Int, @Path("valor") valor: Boolean): Response<HorarioApi>

    @DELETE("api/horarios/{id}")
    suspend fun borrarHorario(@Path("id") id: Int): Response<RespuestaResponse>

    // ── HORARIOS DE AGUA ──────────────────────────────────
    @GET("api/horariosagua/usuario/{usuarioId}")
    suspend fun getHorariosAgua(@Path("usuarioId") usuarioId: Int): Response<List<HorarioAguaApi>>

    @POST("api/horariosagua")
    suspend fun crearHorarioAgua(@Body h: HorarioAguaApi): Response<HorarioAguaApi>

    @PUT("api/horariosagua/{id}")
    suspend fun editarHorarioAgua(@Path("id") id: Int, @Body h: HorarioAguaApi): Response<HorarioAguaApi>

    @PUT("api/horariosagua/{id}/activo/{valor}")
    suspend fun activoHorarioAgua(@Path("id") id: Int, @Path("valor") valor: Boolean): Response<HorarioAguaApi>

    @DELETE("api/horariosagua/{id}")
    suspend fun borrarHorarioAgua(@Path("id") id: Int): Response<RespuestaResponse>

    // ── DISPENSACIONES (historial) ────────────────────────
    @GET("api/dispensaciones/usuario/{usuarioId}")
    suspend fun getDispensaciones(@Path("usuarioId") usuarioId: Int): Response<List<DispensacionApi>>

    @POST("api/dispensaciones")
    suspend fun crearDispensacion(@Body d: DispensacionApi): Response<DispensacionApi>

    @GET("api/dispensacionesagua/usuario/{usuarioId}")
    suspend fun getDispensacionesAgua(@Path("usuarioId") usuarioId: Int): Response<List<DispensacionAguaApi>>

    @POST("api/dispensacionesagua")
    suspend fun crearDispensacionAgua(@Body d: DispensacionAguaApi): Response<DispensacionAguaApi>
}
