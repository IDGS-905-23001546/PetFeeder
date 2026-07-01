package com.petfeeder.app

import android.app.Application

/**
 * Clase Application: se ejecuta una vez al arrancar el proceso.
 * Carga la URL de la API guardada (emulador o IP del teléfono) en RetrofitClient.
 */
class PetFeederApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
    }
}
