package com.petfeeder.app

data class Dispensacion(
    val id: Int = 0,
    val tipo: String,              // "programada" | "manual"
    val nombre: String = "Manual",
    val porcionGramos: Double,
    val fechaHora: Long = System.currentTimeMillis(),
    val estado: String = "ejecutada"
)
