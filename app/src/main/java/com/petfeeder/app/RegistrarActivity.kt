package com.petfeeder.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RegistrarActivity : AppCompatActivity(){
    private lateinit var etNombre: EditText
    private lateinit var etEmail: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnContinuar: Button

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registrar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val  systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        etNombre = findViewById(R.id.etNombre)
        etEmail = findViewById(R.id.etEmail)
        etTelefono = findViewById(R.id.etTelefono)
        etPassword = findViewById(R.id.etPassword)
        btnContinuar = findViewById(R.id.btnContinuar)

        btnContinuar.setOnClickListener { registrar() }

        findViewById<TextView>(R.id.tvIniciarSesion).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registrar(){
        val nombre = etNombre.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val password = etPassword.text.toString()

        // 1. Validar campos obligatorios (el telefono es opcional)
        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Campos nombre, correo y contraseña obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Evitar doble clic mientras se procesa
        btnContinuar.isEnabled = false

        // 3. Llamar a la API en segundo plano (corrutina)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.registro(
                    RegistroRequest(
                        nombre = nombre,
                        email = email,
                        telefono = if(telefono.isEmpty()) null else telefono,
                        password = password
                    )
                )

                if (response.isSuccessful) {
                    // La API ya creó la cuenta y mandó el OTP al correo
                    Toast.makeText(
                        this@RegistrarActivity,
                        "Te enviamos un código a tu correo",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Pasamos el email a VerificarActivity (lo necesita para verificar)
                    val intent = Intent(this@RegistrarActivity, VerificarActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                } else {
                    // 400 = correo ya registrado (único error del endpoint)
                    val mensaje = when (response.code()) {
                        400 -> "Ya existe una cuenta con ese correo."
                        else -> "Error al registrar (${response.code()})."
                    }
                    Toast.makeText(this@RegistrarActivity, mensaje, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                // Sin conexión / API apagada
                Toast.makeText(
                    this@RegistrarActivity,
                    "No se pudo conectar con el servidor. ¿Está corriendo la API?",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnContinuar.isEnabled = true
            }
        }
    }
}