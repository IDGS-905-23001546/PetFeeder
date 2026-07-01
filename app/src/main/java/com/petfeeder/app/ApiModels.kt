package com.petfeeder.app

/**
 * Modelos que viajan por la red (JSON) entre la app y la API .NET.
 * Los nombres de campo coinciden con el JSON camelCase que devuelve ASP.NET Core,
 * y con las columnas de petfeeder_db a través de los modelos del servidor.
 */

data class MascotaApi(
    val id: Int = 0,
    val usuarioId: Int,
    val nombre: String,
    val raza: String,
    val edadAnos: Int = 0,
    val edadMeses: Int = 0,
    val pesoKg: Double = 0.0,
    val tamano: String = "mediano",
    val activa: Boolean = false,
    val fotoUri: String? = ""
)

// ── Conversiones entre el modelo de red y el modelo local ──

fun MascotaApi.toMascota() = Mascota(
    id = id,
    nombre = nombre,
    raza = raza,
    edadAnos = edadAnos,
    edadMeses = edadMeses,
    pesoKg = pesoKg,
    tamano = tamano,
    activa = activa,
    fotoUri = fotoUri ?: ""
)

fun Mascota.toApi(usuarioId: Int) = MascotaApi(
    id = id,
    usuarioId = usuarioId,
    nombre = nombre,
    raza = raza,
    edadAnos = edadAnos,
    edadMeses = edadMeses,
    pesoKg = pesoKg,
    tamano = tamano,
    activa = activa,
    fotoUri = fotoUri
)

// ===== HORARIOS (comida) =====

data class HorarioApi(
    val id: Int = 0,
    val usuarioId: Int,
    val mascotaId: Int? = null,
    val dispensadorId: Int? = null,
    val nombre: String,
    val icono: String = "sun",
    val hora: String,
    val lunes: Boolean = false,
    val martes: Boolean = false,
    val miercoles: Boolean = false,
    val jueves: Boolean = false,
    val viernes: Boolean = false,
    val sabado: Boolean = false,
    val domingo: Boolean = false,
    val porcionGramos: Double = 100.0,
    val activo: Boolean = true
)

fun HorarioApi.toHorario() = Horario(
    id = id, nombre = nombre, icono = icono, hora = hora,
    lunes = lunes, martes = martes, miercoles = miercoles, jueves = jueves,
    viernes = viernes, sabado = sabado, domingo = domingo,
    porcionGramos = porcionGramos, activo = activo
)

fun Horario.toApi(usuarioId: Int) = HorarioApi(
    id = id, usuarioId = usuarioId, nombre = nombre, icono = icono, hora = hora,
    lunes = lunes, martes = martes, miercoles = miercoles, jueves = jueves,
    viernes = viernes, sabado = sabado, domingo = domingo,
    porcionGramos = porcionGramos, activo = activo
)

// ===== HORARIOS DE AGUA =====

data class HorarioAguaApi(
    val id: Int = 0,
    val usuarioId: Int,
    val mascotaId: Int? = null,
    val dispensadorId: Int? = null,
    val nombre: String,
    val icono: String = "water",
    val hora: String,
    val lunes: Boolean = false,
    val martes: Boolean = false,
    val miercoles: Boolean = false,
    val jueves: Boolean = false,
    val viernes: Boolean = false,
    val sabado: Boolean = false,
    val domingo: Boolean = false,
    val cantidadMl: Double = 200.0,
    val activo: Boolean = true
)

fun HorarioAguaApi.toHorarioAgua() = HorarioAgua(
    id = id, nombre = nombre, icono = icono, hora = hora,
    lunes = lunes, martes = martes, miercoles = miercoles, jueves = jueves,
    viernes = viernes, sabado = sabado, domingo = domingo,
    cantidadMl = cantidadMl, activo = activo
)

fun HorarioAgua.toApi(usuarioId: Int) = HorarioAguaApi(
    id = id, usuarioId = usuarioId, nombre = nombre, icono = icono, hora = hora,
    lunes = lunes, martes = martes, miercoles = miercoles, jueves = jueves,
    viernes = viernes, sabado = sabado, domingo = domingo,
    cantidadMl = cantidadMl, activo = activo
)

// ===== DISPENSACIONES (historial: comida y agua) =====

data class DispensacionApi(
    val id: Int = 0,
    val usuarioId: Int,
    val mascotaId: Int? = null,
    val dispensadorId: Int? = null,
    val horarioId: Int? = null,
    val tipo: String = "manual",
    val nombre: String = "Manual",
    val porcionGramos: Double,
    val fechaHora: String? = null,   // el servidor pone la fecha si va null
    val estado: String = "ejecutada"
)

data class DispensacionAguaApi(
    val id: Int = 0,
    val usuarioId: Int,
    val mascotaId: Int? = null,
    val dispensadorId: Int? = null,
    val horarioAguaId: Int? = null,
    val tipo: String = "manual",
    val nombre: String = "Manual",
    val cantidadMl: Double,
    val fechaHora: String? = null,
    val estado: String = "ejecutada"
)
