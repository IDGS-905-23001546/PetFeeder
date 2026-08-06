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

class DispensarManual : AppCompatActivity() {

    private lateinit var db: PawFeederDatabase
    private var amountGrams = 120
    private val presets = listOf(60, 120, 180)

    // Límite de seguridad: nunca se dispensan más de 300 g por orden
    companion object {
        const val MAX_GRAMOS = 300
    }

    private var holdHandler: Handler? = null
    private var holdRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dispensar_manual)

        db = PawFeederDatabase(this)

        setupBack()
        loadMascotaInfo()
        loadTolvaInfo()
        setupAmountControls()
        setupHoldButton()
        runEntranceAnimations()
    }

    private fun setupBack() {
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun loadMascotaInfo() {
        val mascota = db.getActivaMascota()
        val tvSub = findViewById<TextView>(R.id.tvSubtitleDispensador)
        if (mascota != null) {
            tvSub.text = "Para ${mascota.nombre} · ${mascota.raza} ${mascota.pesoKg.toInt()} kg"
            // Porción recomendada por edad + tamaño + peso (por comida)
            val meses = if (mascota.edadMeses > 0) mascota.edadMeses else mascota.edadAnos * 12
            val r = PorcionCalculator.calcular(mascota.tamano, meses, mascota.pesoKg)
            amountGrams = r.gramosPorComida.coerceIn(10, MAX_GRAMOS)
        }
        updateAmountDisplay()
    }

    private fun loadTolvaInfo() {
        val dispensador = db.getDispensador()
        val tvTolva = findViewById<TextView>(R.id.tvTolvaInfo)
        if (dispensador != null) {
            val kg = dispensador.tolvaKg
            val comidas = (kg * 1000 / 120).toInt()
            tvTolva.text = String.format("%.1f kg · ~%d comidas", kg, comidas)
        } else {
            tvTolva.text = "Sin dispensador vinculado"
        }
    }

    private fun setupAmountControls() {
        val tvAmount = findViewById<TextView>(R.id.tvAmountValue)
        val p60 = findViewById<TextView>(R.id.preset60)
        val p120 = findViewById<TextView>(R.id.preset120)
        val p180 = findViewById<TextView>(R.id.preset180)
        val btnMinus = findViewById<FrameLayout>(R.id.btnMinus)
        val btnPlus = findViewById<FrameLayout>(R.id.btnPlus)

        fun updatePresets() {
            listOf(p60 to 60, p120 to 120, p180 to 180).forEach { (tv, v) ->
                val selected = amountGrams == v
                tv.setBackgroundResource(
                    if (selected) R.drawable.bg_amount_preset_selected
                    else R.drawable.bg_amount_preset
                )
                tv.setTextColor(
                    if (selected) getColor(R.color.white)
                    else getColor(R.color.text_secondary)
                )
            }
            tvAmount.text = amountGrams.toString()
        }

        p60.setOnClickListener { amountGrams = 60; updatePresets() }
        p120.setOnClickListener { amountGrams = 120; updatePresets() }
        p180.setOnClickListener { amountGrams = 180; updatePresets() }

        btnMinus.setOnClickListener {
            if (amountGrams > 10) { amountGrams -= 10; updatePresets() }
        }
        btnPlus.setOnClickListener {
            if (amountGrams < MAX_GRAMOS) { amountGrams += 10; updatePresets() }
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
        // Guard de seguridad: nunca superar el máximo
        if (amountGrams > MAX_GRAMOS) {
            amountGrams = MAX_GRAMOS
            updateAmountDisplay()
        }

        val mascota = db.getActivaMascota()
        val nombreMascota = mascota?.nombre ?: "tu mascota"

        // Sin IP configurada -> modo simulado (la app funciona aunque no haya hardware)
        if (!DeviceConfig.hasIp(this)) {
            registrarYSalir(nombreMascota, simulado = true)
            return
        }

        // Con dispositivo -> mandar la orden real al ESP32
        LoadingDialog.show(supportFragmentManager, "Dispensando $amountGrams g...")
        lifecycleScope.launch {
            val r = DeviceClient.dispensarCroquetas(this@DispensarManual, amountGrams)
            LoadingDialog.dismiss(supportFragmentManager)
            if (r.exito) {
                registrarYSalir(nombreMascota, simulado = false)
            } else {
                Toast.makeText(this@DispensarManual, r.mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun registrarYSalir(nombreMascota: String, simulado: Boolean) {
        LoadingDialog.show(supportFragmentManager, "Registrando dispensación...")
        lifecycleScope.launch {
            // 1. Historial local (caché)
            db.insertDispensacion(
                Dispensacion(
                    tipo = "manual",
                    nombre = "Manual",
                    porcionGramos = amountGrams.toDouble()
                )
            )
            // 2. Historial en petfeeder_db (best-effort)
            try {
                RetrofitClient.api.crearDispensacion(
                    DispensacionApi(
                        usuarioId = UserSession.getId(this@DispensarManual),
                        tipo = "manual",
                        nombre = "Manual",
                        porcionGramos = amountGrams.toDouble()
                    )
                )
            } catch (_: Exception) {}

            LoadingDialog.dismiss(supportFragmentManager)
            val extra = if (simulado) " (simulado)" else ""
            Toast.makeText(
                this@DispensarManual,
                "¡${amountGrams}g dispensados para $nombreMascota!$extra",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun updateAmountDisplay() {
        findViewById<TextView>(R.id.tvAmountValue)?.text = amountGrams.toString()
    }

    private fun runEntranceAnimations() {
        listOf(R.id.tvAmountValue, R.id.preset60, R.id.tvTolvaInfo, R.id.btnDispensarHold)
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
