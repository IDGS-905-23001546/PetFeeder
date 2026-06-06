package com.petfeeder.app

import android.content.Context

object UserSession {
    private const val PREFS = "pawfeeder_user"

    fun getNombre(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("nombre", "Usuario") ?: "Usuario"

    fun getEmail(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("email", "usuario@correo.com") ?: "usuario@correo.com"

    fun save(ctx: Context, nombre: String, email: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("nombre", nombre)
            .putString("email", email)
            .apply()
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
