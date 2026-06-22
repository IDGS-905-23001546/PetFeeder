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

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener { iniciarSesion() }

        findViewById<TextView>(R.id.tvRegister).setOnClickListener {
            startActivity(Intent(this, RegistrarActivity::class.java))
        }
    }

    private fun iniciarSesion() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        // 1. Validar que no estén vacíos
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Llena correo y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Evitar doble clic mientras se procesa
        btnLogin.isEnabled = false

        // 3. Llamar a la API en segundo plano (corrutina)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.login(LoginRequest(email, password))

                if (response.isSuccessful) {
                    // Login correcto: la API devolvió los datos del usuario
                    val usuario = response.body()!!
                    UserSession.save(this@LoginActivity, usuario.nombre, usuario.email)

                    Toast.makeText(
                        this@LoginActivity,
                        "Bienvenido, ${usuario.nombre}",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this@LoginActivity, Principal::class.java))
                    finish()
                } else {
                    // Credenciales malas o cuenta sin verificar (401 / 400)
                    val mensaje = when (response.code()) {
                        401 -> "Correo o contraseña incorrectos."
                        400 -> "Tu cuenta aún no está verificada."
                        else -> "Error al iniciar sesión (${response.code()})."
                    }
                    Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                // Sin conexión / API apagada
                Toast.makeText(
                    this@LoginActivity,
                    "No se pudo conectar con el servidor. ¿Está corriendo la API?",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnLogin.isEnabled = true
            }
        }
    }
}
