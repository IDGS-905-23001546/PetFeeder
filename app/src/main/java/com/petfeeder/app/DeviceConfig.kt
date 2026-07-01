package com.petfeeder.app

import android.content.Context

/**
 * Guarda la dirección del prototipo ESP32 en la red WiFi.
 *
 * El ESP32 (ver firmware) levanta un servidor HTTP en el puerto 80 y muestra
 * su IP por el monitor serie al arrancar (ej. 192.168.1.42). Esa IP se guarda
 * aquí desde la pantalla Equipo y se usa para mandarle órdenes de dispensado.
 */
object DeviceConfig {
    private const val PREFS = "pawfeeder_device"
    private const val KEY_IP = "esp32_ip"

    /** IP del ESP32, o cadena vacía si aún no se ha configurado. */
    fun getIp(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_IP, "") ?: ""

    fun setIp(ctx: Context, ip: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_IP, ip.trim())
            .apply()
    }

    fun hasIp(ctx: Context): Boolean = getIp(ctx).isNotBlank()

    /** URL base del ESP32, ej. "http://192.168.1.42" (sin barra final). */
    fun baseUrl(ctx: Context): String {
        val ip = getIp(ctx)
        return if (ip.startsWith("http")) ip.trimEnd('/') else "http://$ip"
    }
}
