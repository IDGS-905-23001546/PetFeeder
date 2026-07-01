package com.petfeeder.app

import android.content.Context

/**
 * Maneja la sesión del usuario guardada en SharedPreferences.
 *
 * Guarda id, nombre y email tras un login exitoso. Sirve como "fuente de verdad"
 * para saber si hay una sesión activa (control de acceso) y para identificar
 * al usuario en las llamadas a la API (mascotas, horarios, dispensaciones...).
 */
object UserSession {
    private const val PREFS = "pawfeeder_user"
    private const val KEY_ID = "id"
    private const val KEY_NOMBRE = "nombre"
    private const val KEY_EMAIL = "email"

    fun getId(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_ID, -1)

    fun getNombre(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_NOMBRE, "Usuario") ?: "Usuario"

    fun getEmail(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_EMAIL, "usuario@correo.com") ?: "usuario@correo.com"

    /**
     * Verdadero solo si hay un usuario logueado (se guardó un id válido).
     * MainActivity usa esto como compuerta de acceso.
     */
    fun isLoggedIn(ctx: Context): Boolean =
        getId(ctx) != -1

    fun save(ctx: Context, id: Int, nombre: String, email: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_ID, id)
            .putString(KEY_NOMBRE, nombre)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
