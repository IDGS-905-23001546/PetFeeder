package com.petfeeder.app

data class Dispensador(
    val id: Int = 0,
    val nombre: String,
    val codigoUnico: String,
    val firmwareVersion: String = "v1.0.0",
    val estado: String = "offline",    // activo | offline
    val bateriaPercent: Int = 100,
    val nivelTolvaPct: Int = 60,       // 0-100, usado para calcular kg
    val ssidWifi: String = ""
) {
    val tolvaKg: Double get() = (nivelTolvaPct / 100.0) * 4.0
    val estaEnLinea: Boolean get() = estado == "activo"
}
