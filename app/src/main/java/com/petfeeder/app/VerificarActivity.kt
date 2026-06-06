package com.petfeeder.app

import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class VerificarActivity : AppCompatActivity() {

    private lateinit var digits: Array<TextView>
    private lateinit var etOtp: EditText
    private lateinit var tvSecurityText: TextView
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verificar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        findViewById<TextView>(R.id.tvReenviar).setOnClickListener {
            countDownTimer?.cancel()
            etOtp.setText("")
            startCountdown(300_000L)
        }

        findViewById<Button>(R.id.btnVerificar).setOnClickListener {
            if (etOtp.text.toString().length == 6) {
                // TODO: validar código con backend
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
