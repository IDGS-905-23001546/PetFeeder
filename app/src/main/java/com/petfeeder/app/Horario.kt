package com.petfeeder.app

data class Horario(
    val id: Int = 0,
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
