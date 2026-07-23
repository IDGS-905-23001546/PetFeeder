package com.petfeeder.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP que le habla DIRECTAMENTE al prototipo ESP32 por WiFi.
 *
 * El firmware del ESP32 expone (puerto 80):
 *   POST /dispense   body: {"grams": 200}   -> abre el servo hasta llegar al peso
 *   GET  /weight                            -> {"weight":123.4,"unit":"g"}
 *   GET  /status                            -> {"status":"idle","weight":..,"target":..}
 *   POST /tare                              -> pone la báscula en cero
 *
 * IMPORTANTE: el teléfono y el ESP32 deben estar en la MISMA red WiFi, y la IP
 * del ESP32 debe estar guardada (DeviceConfig).
 */
object DeviceClient {

    private const val DISPENSE_PATH = "/dispense"
    private const val STATUS_PATH = "/status"

    private val JSON = "application/json; charset=utf-8".toMediaType()

    // Timeouts cortos: si el ESP32 no responde, no dejamos colgada la UI
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    /** Resultado simple de una orden al dispositivo. */
    data class Resultado(val exito: Boolean, val mensaje: String)

    // ── API para las pantallas (corrutinas) ──────────────

    /** Ordena dispensar croquetas (gramos). Llamar desde una corrutina. */
    suspend fun dispensarCroquetas(ctx: Context, gramos: Int): Resultado =
        withContext(Dispatchers.IO) { postDispense(DeviceConfig.baseUrl(ctx), DISPENSE_PATH, gramos) }

    /** Consulta el estado del dispositivo (para saber si está en línea). */
    suspend fun consultarEstado(ctx: Context): Resultado =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(DeviceConfig.baseUrl(ctx) + STATUS_PATH).get().build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string() ?: ""
                    Resultado(resp.isSuccessful, body)
                }
            } catch (e: Exception) {
                Resultado(false, "No responde: ${e.message}")
            }
        }

    // ── Núcleo bloqueante (reutilizable por el AlarmReceiver) ──

    /**
     * Hace el POST bloqueante al ESP32. Se usa también desde FeedAlarmReceiver,
     * que corre en un hilo de fondo (no en una corrutina).
     */
    fun postDispense(baseUrl: String, path: String, cantidad: Int): Resultado {
        if (baseUrl.isBlank() || baseUrl == "http://") {
            return Resultado(false, "Falta configurar la IP del dispositivo (pantalla Equipo).")
        }
        return try {
            // El firmware busca el primer número tras ':' — {"grams":200} funciona
            val json = "{\"grams\":$cantidad}"
            val req = Request.Builder()
                .url(baseUrl + path)
                .post(json.toRequestBody(JSON))
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    Resultado(true, "Dispositivo dispensando $cantidad")
                } else {
                    Resultado(false, "El dispositivo respondió error ${resp.code}")
                }
            }
        } catch (e: Exception) {
            Resultado(false, "No se pudo conectar al dispositivo. ¿Misma WiFi e IP correcta?")
        }
    }
}
