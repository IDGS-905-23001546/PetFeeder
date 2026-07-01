package com.petfeeder.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Dispensado MANUAL de agua. Espejo de DispensarManual.kt pero en ml y sobre
 * la tabla nueva dispensaciones_agua. Mantener presionado 2s dispensa.
 */
class DispensarAguaManual : AppCompatActivity() {

    private lateinit var db: PawFeederDatabase
    private var amountMl = 200
    private var holdHandler: Handler? = null
    private var holdRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dispensar_agua_manual)

        db = PawFeederDatabase(this)

        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener { finish() }
        loadMascotaInfo()
        loadEquipoInfo()
        setupAmountControls()
        setupHoldButton()
        runEntranceAnimations()
    }

    private fun loadMascotaInfo() {
        val mascota = db.getActivaMascota()
        val tvSub = findViewById<TextView>(R.id.tvSubtitleDispensador)
        if (mascota != null) {
            tvSub.text = "Para ${mascota.nombre} · ${mascota.raza} ${mascota.pesoKg.toInt()} kg"
        }
        updateAmountDisplay()
    }

    private fun loadEquipoInfo() {
        val dispensador = db.getDispensador()
        val tvInfo = findViewById<TextView>(R.id.tvTolvaInfo)
        tvInfo.text = if (dispensador != null) {
            if (dispensador.estaEnLinea) "En línea" else "Fuera de línea"
        } else {
            "Sin dispensador vinculado"
        }
    }

    private fun setupAmountControls() {
        val tvAmount = findViewById<TextView>(R.id.tvAmountValue)
        val pA = findViewById<TextView>(R.id.presetA)
        val pB = findViewById<TextView>(R.id.presetB)
        val pC = findViewById<TextView>(R.id.presetC)
        val btnMinus = findViewById<FrameLayout>(R.id.btnMinus)
        val btnPlus = findViewById<FrameLayout>(R.id.btnPlus)

        fun updatePresets() {
            listOf(pA to 100, pB to 200, pC to 300).forEach { (tv, v) ->
                val selected = amountMl == v
                tv.setBackgroundResource(
                    if (selected) R.drawable.bg_amount_preset_selected
                    else R.drawable.bg_amount_preset
                )
                tv.setTextColor(
                    if (selected) getColor(R.color.white)
                    else getColor(R.color.text_secondary)
                )
            }
            tvAmount.text = amountMl.toString()
        }

        pA.setOnClickListener { amountMl = 100; updatePresets() }
        pB.setOnClickListener { amountMl = 200; updatePresets() }
        pC.setOnClickListener { amountMl = 300; updatePresets() }

        btnMinus.setOnClickListener {
            if (amountMl > 20) { amountMl -= 20; updatePresets() }
        }
        btnPlus.setOnClickListener {
            if (amountMl < 1000) { amountMl += 20; updatePresets() }
        }

        updatePresets()
    }

    private fun setupHoldButton() {
        val btn = findViewById<LinearLayout>(R.id.btnDispensarHold)
        btn.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start()
                    startHoldTimer(v)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    cancelHoldTimer()
                    true
                }
                else -> false
            }
        }
    }

    private fun startHoldTimer(v: View) {
        holdHandler = Handler(Looper.getMainLooper())
        holdRunnable = Runnable {
            dispensar()
            v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }
        holdHandler?.postDelayed(holdRunnable!!, 2000)
    }

    private fun cancelHoldTimer() {
        holdRunnable?.let { holdHandler?.removeCallbacks(it) }
        holdHandler = null
        holdRunnable = null
    }

    private fun dispensar() {
        val mascota = db.getActivaMascota()
        val nombreMascota = mascota?.nombre ?: "tu mascota"

        if (!DeviceConfig.hasIp(this)) {
            registrarYSalir(nombreMascota, simulado = true)
            return
        }

        Toast.makeText(this, "Enviando al dispensador...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val r = DeviceClient.dispensarAgua(this@DispensarAguaManual, amountMl)
            if (r.exito) {
                registrarYSalir(nombreMascota, simulado = false)
            } else {
                Toast.makeText(this@DispensarAguaManual, r.mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun registrarYSalir(nombreMascota: String, simulado: Boolean) {
        lifecycleScope.launch {
            db.insertDispensacionAgua("manual", "Manual", amountMl.toDouble())
            try {
                RetrofitClient.api.crearDispensacionAgua(
                    DispensacionAguaApi(
                        usuarioId = UserSession.getId(this@DispensarAguaManual),
                        tipo = "manual",
                        nombre = "Manual",
                        cantidadMl = amountMl.toDouble()
                    )
                )
            } catch (_: Exception) {}

            val extra = if (simulado) " (simulado)" else ""
            Toast.makeText(
                this@DispensarAguaManual,
                "¡${amountMl} ml de agua dispensados para $nombreMascota!$extra",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun updateAmountDisplay() {
        findViewById<TextView>(R.id.tvAmountValue)?.text = amountMl.toString()
    }

    private fun runEntranceAnimations() {
        listOf(R.id.tvAmountValue, R.id.presetA, R.id.tvTolvaInfo, R.id.btnDispensarHold)
            .forEachIndexed { i, id ->
                val v = findViewById<View>(id) ?: return@forEachIndexed
                v.alpha = 0f
                v.translationY = 30f
                v.animate().alpha(1f).translationY(0f)
                    .setDuration(400).setStartDelay(i * 80L)
                    .setInterpolator(DecelerateInterpolator(1.4f)).start()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelHoldTimer()
    }
}
