package com.petfeeder.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Pantalla de arranque (launcher). Actúa como COMPUERTA DE ACCESO:
 *  - Si ya hay una sesión activa  -> entra directo a Principal (auto-login).
 *  - Si NO hay sesión (o se cerró) -> manda a LoginActivity y no deja pasar.
 *
 * Combinado con el FLAG_ACTIVITY_CLEAR_TASK del logout, esto garantiza que
 * después de cerrar sesión no se pueda "regresar" a las pantallas internas
 * sin volver a iniciar sesión.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val destino = if (UserSession.isLoggedIn(this)) {
            Principal::class.java
        } else {
            LoginActivity::class.java
        }

        startActivity(Intent(this, destino))
        finish()
    }
}
