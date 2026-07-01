package com.petfeeder.app

// ===== REQUESTS (lo que ENVIAMOS a la API) =====

data class RegistroRequest(
    val nombre: String,
    val email: String,
    val telefono: String?,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class VerificarRequest(
    val email: String,
    val codigo: String
)

data class ReenviarRequest(
    val email: String
)

// ===== RESPONSES (lo que RECIBIMOS de la API) =====

data class RespuestaResponse(
    val exito: Boolean,
    val mensaje: String
)

data class UsuarioResponse(
    val id: Int,
    val nombre: String,
    val email: String,
    val verificado: Boolean
)
