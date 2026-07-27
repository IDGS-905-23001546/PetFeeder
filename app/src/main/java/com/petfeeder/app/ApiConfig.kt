package com.petfeeder.app

import android.content.Context

/**
 * Dirección de la API. CONFIGURABLE en tiempo de ejecución (pantalla de login ->
 * "Configurar servidor"), para funcionar en emulador y en teléfono físico sin
 * recompilar.
 *
 *  EMULADOR:        http://10.0.2.2:5172/     (10.0.2.2 = localhost de la PC)
 *  TELÉFONO FÍSICO: http://IP_DE_TU_PC:5172/  (ej. http://192.168.1.50:5172/)
 *    - La IP la sacas con 'ipconfig' en Windows (IPv4).
 *    - Teléfono y PC en la MISMA red WiFi + firewall abierto (5172).
 *    - La API debe escuchar en 0.0.0.0 (ver launchSettings del proyecto API).
 */
object ApiConfig {

    // URL de Render (produccion). Cambiar cuando crees el servicio en Render.
    // Para desarrollo local, usa "Configurar servidor" desde el login.
    const val RENDER_URL = "https://petfeeder-api-dgjx.onrender.com/"

    val DEFAULT_BASE_URL: String = RENDER_URL

    private const val PREFS = "pawfeeder_api"
    private const val KEY_URL = "base_url"
    private const val KEY_MANUAL = "override_manual"

    /**
     * URL actual de la API (siempre termina en "/").
     * Por defecto usa la IP detectada al compilar (BuildConfig); solo usa una URL
     * guardada si el usuario la puso a mano en "Configurar servidor".
     */
    fun getBaseUrl(ctx: Context): String {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val manual = prefs.getBoolean(KEY_MANUAL, false)
        return if (manual) {
            normalizar(prefs.getString(KEY_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL)
        } else {
            normalizar(DEFAULT_BASE_URL)   // IP fresca de cada compilación
        }
    }

    /** Guarda una URL manual (marca override). Usado solo desde "Configurar servidor". */
    fun setBaseUrl(ctx: Context, url: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_URL, normalizar(url))
            .putBoolean(KEY_MANUAL, true)
            .apply()
    }

    /** Vuelve a usar la IP automática del build (quita el override manual). */
    fun usarAutomatica(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_MANUAL, false)
            .apply()
    }

    /** Asegura prefijo http:// y barra final. */
    private fun normalizar(raw: String): String {
        var u = raw.trim()
        if (u.isEmpty()) return DEFAULT_BASE_URL
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
        if (!u.endsWith("/")) u = "$u/"
        return u
    }
}
