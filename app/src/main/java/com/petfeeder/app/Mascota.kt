package com.petfeeder.app

data class Mascota(
    val id: Int = 0,
    val nombre: String,
    val raza: String,
    val edadAnos: Int = 0,
    val edadMeses: Int = 0,     // edad TOTAL en meses (fuente de verdad; clave para cachorros)
    val pesoKg: Double = 0.0,
    val tamano: String = "mediano",
    val activa: Boolean = false,
    val fotoUri: String = ""
)
