package com.petfeeder.app

/**
 * Modelo de un horario de AGUA (tabla nueva horarios_agua).
 * Es igual a Horario pero la cantidad se mide en mililitros (ml) en vez de gramos.
 */
data class HorarioAgua(
    val id: Int = 0,
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
