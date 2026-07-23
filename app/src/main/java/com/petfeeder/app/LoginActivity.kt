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

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private var passwordVisible = false

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

        btnLogin.setOnClickListener { iniciarSesion() }

        findViewById<TextView>(R.id.tvRegister).setOnClickListener {
            LoadingDialog.show(supportFragmentManager, "Cargando...")
            findViewById<View>(android.R.id.content).postDelayed({
                LoadingDialog.dismiss(supportFragmentManager)
                startActivity(Intent(this, RegistrarActivity::class.java))
            }, 400)
        }

        findViewById<TextView>(R.id.tvConfigServidor).setOnClickListener {
            mostrarConfigServidor()
        }
    }

    /** Diálogo para escribir la IP/URL de la API (emulador o teléfono físico). */
    private fun mostrarConfigServidor() {
        val input = android.widget.EditText(this).apply {
            hint = "http://192.168.1.50:5172/"
            setText(ApiConfig.getBaseUrl(this@LoginActivity))
            setPadding(48, 32, 48, 32)
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Servidor de la API")
            .setMessage("Automático (recomendado): usa la IP de tu PC detectada al compilar.\n\nSolo escribe una IP si quieres forzar otra: http://IP:5172/")
            .setView(input)
            .setPositiveButton("Guardar manual") { _, _ ->
                ApiConfig.setBaseUrl(this, input.text.toString())
                RetrofitClient.init(this)   // aplica la nueva URL de inmediato
                Toast.makeText(this, "Servidor: ${ApiConfig.getBaseUrl(this)}", Toast.LENGTH_LONG).show()
            }
            .setNeutralButton("Usar automático") { _, _ ->
                ApiConfig.usarAutomatica(this)
                RetrofitClient.init(this)
                Toast.makeText(this, "Automático: ${ApiConfig.getBaseUrl(this)}", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
        LoadingDialog.show(supportFragmentManager, "Iniciando sesión...")

        // 3. Llamar a la API en segundo plano (corrutina)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.login(LoginRequest(email, password))

                if (response.isSuccessful) {
                    // Login correcto: la API devolvió los datos del usuario
                    val usuario = response.body()!!
                    UserSession.save(this@LoginActivity, usuario.id, usuario.nombre, usuario.email)

                    LoadingDialog.dismiss(supportFragmentManager)

                    Toast.makeText(
                        this@LoginActivity,
                        "Bienvenido, ${usuario.nombre}",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this@LoginActivity, Principal::class.java))
                    finish()
                } else {
                    LoadingDialog.dismiss(supportFragmentManager)
                    // Credenciales malas o cuenta sin verificar (401 / 400)
                    val mensaje = when (response.code()) {
                        401 -> "Correo o contraseña incorrectos."
                        400 -> "Tu cuenta aún no está verificada. Revisa tu correo."
                        else -> "Ocurrió un problema. Intenta de nuevo."
                    }
                    Toast.makeText(this@LoginActivity, mensaje, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                LoadingDialog.dismiss(supportFragmentManager)
                // Sin conexión / API apagada
                Toast.makeText(
                    this@LoginActivity,
                    "No se pudo conectar con el servidor. Verifica que esté encendido.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnLogin.isEnabled = true
            }
        }
    }
}
