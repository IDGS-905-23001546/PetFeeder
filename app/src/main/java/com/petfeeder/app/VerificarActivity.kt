package com.petfeeder.app

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class VerificarActivity : AppCompatActivity() {

    private lateinit var digits: Array<TextView>
    private lateinit var etOtp: EditText
    private lateinit var tvSecurityText: TextView
    private var countDownTimer: CountDownTimer? = null

    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verificar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        email = intent.getStringExtra("email") ?: ""


        digits = arrayOf(
            findViewById(R.id.otpDigit1),
            findViewById(R.id.otpDigit2),
            findViewById(R.id.otpDigit3),
            findViewById(R.id.otpDigit4),
            findViewById(R.id.otpDigit5),
            findViewById(R.id.otpDigit6)
        )

        etOtp = findViewById(R.id.etOtpHidden)
        tvSecurityText = findViewById(R.id.tvSecurityText)

        setupOtpInput()
        startCountdown(300_000L)

        findViewById<LinearLayout>(R.id.otpContainer).setOnClickListener { showKeyboard() }
        digits.forEach { it.setOnClickListener { showKeyboard() } }

        findViewById<TextView>(R.id.tvReenviar).setOnClickListener { reenviarCodigo() }

        findViewById<Button>(R.id.btnVerificar).setOnClickListener {
            val codigo = etOtp.text.toString()
            if (codigo.length != 6) {
                Toast.makeText(this, "Ingresa los 6 dígitos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verificar(codigo)
        }
    }

    private fun verificar(codigo: String) {
        // Sin email no podemos verificar
        if (email.isEmpty()){
            Toast.makeText(this, "No se recibió el correo. Vuelve a registrarte.", Toast.LENGTH_SHORT).show()
            return
        }

        val btnVerificar = findViewById<Button>(R.id.btnVerificar)
        btnVerificar.isEnabled = false
        LoadingDialog.show(supportFragmentManager, "Verificando...")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.verificar(VerificarRequest(email, codigo))

                if (response.isSuccessful) {
                    // Cuenta verificada -> mandamos a iniciar sesión
                    LoadingDialog.dismiss(supportFragmentManager)

                    Toast.makeText(
                        this@VerificarActivity,
                        "¡Cuenta verificada! Ya puedes iniciar sesión.",
                        Toast.LENGTH_SHORT
                    ).show()
                    LoadingDialog.show(supportFragmentManager, "Redirigiendo...")
                    findViewById<View>(android.R.id.content).postDelayed({
                        LoadingDialog.dismiss(supportFragmentManager)
                        startActivity(Intent(this@VerificarActivity, LoginActivity::class.java))
                        finishAffinity()
                    }, 500)
                } else {
                    LoadingDialog.dismiss(supportFragmentManager)
                    // 400 = código incorrecto, expirado o sin intentos
                    Toast.makeText(
                        this@VerificarActivity,
                        "Código incorrecto o expirado.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                LoadingDialog.dismiss(supportFragmentManager)
                Toast.makeText(
                    this@VerificarActivity,
                    "No se pudo conectar con el servidor. Verifica que esté encendido.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnVerificar.isEnabled = true
            }
        }
    }

    /** Pide a la API un código OTP nuevo y reinicia el contador. */
    private fun reenviarCodigo() {
        if (email.isEmpty()) {
            Toast.makeText(this, "No se recibió el correo. Vuelve a registrarte.", Toast.LENGTH_SHORT).show()
            return
        }
        val tvReenviar = findViewById<TextView>(R.id.tvReenviar)
        tvReenviar.isEnabled = false
        LoadingDialog.show(supportFragmentManager, "Reenviando código...")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.reenviar(ReenviarRequest(email))
                if (response.isSuccessful) {
                    LoadingDialog.dismiss(supportFragmentManager)
                    etOtp.setText("")
                    countDownTimer?.cancel()
                    startCountdown(300_000L)
                    Toast.makeText(
                        this@VerificarActivity,
                        "Te enviamos un nuevo código a tu correo.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    LoadingDialog.dismiss(supportFragmentManager)
                    Toast.makeText(
                        this@VerificarActivity,
                        "No se pudo reenviar el código.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                LoadingDialog.dismiss(supportFragmentManager)
                Toast.makeText(
                    this@VerificarActivity,
                    "No se pudo conectar con el servidor. Verifica que esté encendido.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                tvReenviar.isEnabled = true
            }
        }
    }

    private fun setupOtpInput() {
        etOtp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                digits.forEachIndexed { i, tv ->
                    if (i < text.length) {
                        tv.text = text[i].toString()
                        tv.setBackgroundResource(R.drawable.bg_otp_filled)
                    } else {
                        tv.text = ""
                        tv.setBackgroundResource(R.drawable.bg_otp_empty)
                    }
                }
            }
        })
    }

    private fun showKeyboard() {
        etOtp.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etOtp, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun startCountdown(millis: Long) {
        countDownTimer = object : CountDownTimer(millis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val min = millisUntilFinished / 60000
                val sec = (millisUntilFinished % 60000) / 1000
                updateSecurityText(String.format("%d:%02d min.", min, sec))
            }
            override fun onFinish() {
                updateSecurityText("0:00 min.")
            }
        }.start()
    }

    private fun updateSecurityText(timeStr: String) {
        val prefix = "Cuidamos la seguridad de tu mascota. El código expira en "
        val full = prefix + timeStr
        val spannable = SpannableString(full)
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            prefix.length,
            full.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvSecurityText.text = spannable
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
