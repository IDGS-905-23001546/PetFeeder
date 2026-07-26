package com.petfeeder.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class Seguridad : AppCompatActivity() {

    private lateinit var etPasswordActual: EditText
    private lateinit var etPasswordNueva: EditText
    private lateinit var etPasswordConfirmar: EditText
    private lateinit var btnGuardar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seguridad)

        etPasswordActual = findViewById(R.id.etPasswordActual)
        etPasswordNueva = findViewById(R.id.etPasswordNueva)
        etPasswordConfirmar = findViewById(R.id.etPasswordConfirmar)
        btnGuardar = findViewById(R.id.btnGuardar)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        btnGuardar.setOnClickListener { cambiarPassword() }
    }

    private fun cambiarPassword() {
        val actual = etPasswordActual.text.toString()
        val nueva = etPasswordNueva.text.toString()
        val confirmar = etPasswordConfirmar.text.toString()

        if (actual.isEmpty() || nueva.isEmpty() || confirmar.isEmpty()) {
            Toast.makeText(this, "Llena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        if (nueva.length < 6) {
            Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }
        if (nueva != confirmar) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        btnGuardar.isEnabled = false
        LoadingDialog.show(supportFragmentManager, "Guardando...")

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.cambiarPassword(
                    CambiarPasswordRequest(
                        usuarioId = UserSession.getId(this@Seguridad),
                        passwordActual = actual,
                        passwordNueva = nueva
                    )
                )
                LoadingDialog.dismiss(supportFragmentManager)
                if (resp.isSuccessful) {
                    Toast.makeText(this@Seguridad, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val msg = resp.body()?.mensaje ?: "Error al cambiar contraseña"
                    Toast.makeText(this@Seguridad, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                LoadingDialog.dismiss(supportFragmentManager)
                Toast.makeText(this@Seguridad, "No se pudo conectar al servidor", Toast.LENGTH_LONG).show()
            } finally {
                btnGuardar.isEnabled = true
            }
        }
    }
}
