package com.petfeeder.app

/**
 * Configuración de la dirección de la API.
 *
 * ⚙️  CAMBIA AQUÍ la URL según dónde pruebes la app:
 *
 *  ┌────────────────────────────────────────────────────────────────────┐
 *  │ OPCIÓN 1 — EMULADOR de Android Studio (la de por defecto):          │
 *  │   "http://10.0.2.2:5172/"                                           │
 *  │   10.0.2.2 = el "localhost" de la PC donde corre el emulador.       │
 *  │                                                                     │
 *  │ OPCIÓN 2 — TELÉFONO FÍSICO en la misma red WiFi:                    │
 *  │   "http://IP_DE_TU_PC:5172/"   (ej. "http://192.168.1.50:5172/")    │
 *  │   - Averigua la IP con 'ipconfig' en Windows -> "Dirección IPv4".   │
 *  │   - El teléfono y la PC deben estar en la MISMA red WiFi.           │
 *  │   - Abre el Firewall de Windows para el puerto 5172.                │
 *  └────────────────────────────────────────────────────────────────────┘
 *
 *  NOTA: la API debe estar CORRIENDO en esa PC (en Visual Studio, F5).
 */
object ApiConfig {

    const val BASE_URL = "http://10.0.2.2:5172/"
}
