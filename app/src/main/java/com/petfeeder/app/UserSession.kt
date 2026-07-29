package com.petfeeder.app

import android.content.Context
import android.content.Intent

object UserSession {
    private const val PREFS = "pawfeeder_user"
    private const val KEY_ID = "id"
    private const val KEY_NOMBRE = "nombre"
    private const val KEY_EMAIL = "email"
    private const val KEY_LAST_TOUCH = "last_touch"

    private const val SESSION_TIMEOUT_MS = 15 * 60 * 1000L
    private const val WARNING_MINUTES = 10

    fun getId(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_ID, -1)

    fun getNombre(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_NOMBRE, "Usuario") ?: "Usuario"

    fun getEmail(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_EMAIL, "usuario@correo.com") ?: "usuario@correo.com"

    fun isLoggedIn(ctx: Context): Boolean =
        getId(ctx) != -1

    fun save(ctx: Context, id: Int, nombre: String, email: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_ID, id)
            .putString(KEY_NOMBRE, nombre)
            .putString(KEY_EMAIL, email)
            .putLong(KEY_LAST_TOUCH, System.currentTimeMillis())
            .apply()
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun touch(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_TOUCH, System.currentTimeMillis())
            .apply()
    }

    private fun lastTouchMs(ctx: Context): Long =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_TOUCH, 0L)

    fun msSinceLastTouch(ctx: Context): Long =
        System.currentTimeMillis() - lastTouchMs(ctx)

    fun isSessionExpired(ctx: Context): Boolean =
        isLoggedIn(ctx) && msSinceLastTouch(ctx) > SESSION_TIMEOUT_MS

    fun shouldShowWarning(ctx: Context): Boolean {
        if (!isLoggedIn(ctx)) return false
        val elapsed = msSinceLastTouch(ctx)
        return elapsed > WARNING_MINUTES * 60 * 1000L && elapsed <= SESSION_TIMEOUT_MS
    }

    fun logoutAndRedirect(ctx: Context) {
        clear(ctx)
        val i = Intent(ctx, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        ctx.startActivity(i)
    }
}
