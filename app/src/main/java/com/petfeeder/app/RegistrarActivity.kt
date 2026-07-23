package com.petfeeder.app

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
    private var passwordVisible = false

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

        // Toggle visibilidad de contraseña
        findViewById<ImageView>(R.id.ivTogglePassword).setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                findViewById<ImageView>(R.id.ivTogglePassword).setImageResource(R.drawable.ic_eye_hidden)
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                findViewById<ImageView>(R.id.ivTogglePassword).setImageResource(R.drawable.ic_eye_visible)
            }
            etPassword.setSelection(etPassword.text.length)
        }

        btnContinuar.setOnClickListener { registrar() }

        findViewById<TextView>(R.id.tvIniciarSesion).setOnClickListener {
            LoadingDialog.show(supportFragmentManager, "Cargando...")
            findViewById<View>(android.R.id.content).postDelayed({
                LoadingDialog.dismiss(supportFragmentManager)
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }, 400)
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
        LoadingDialog.show(supportFragmentManager, "Creando tu cuenta...")

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
                    LoadingDialog.dismiss(supportFragmentManager)

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
                    LoadingDialog.dismiss(supportFragmentManager)
                    // 400 = correo ya registrado
                    val mensaje = when (response.code()) {
                        400 -> "Ya existe una cuenta con ese correo."
                        else -> "Ocurrió un problema al registrar. Intenta de nuevo."
                    }
                    Toast.makeText(this@RegistrarActivity, mensaje, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                LoadingDialog.dismiss(supportFragmentManager)
                // Sin conexión / API apagada
                Toast.makeText(
                    this@RegistrarActivity,
                    "No se pudo conectar con el servidor. Verifica que esté encendido.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnContinuar.isEnabled = true
            }
        }
    }
}