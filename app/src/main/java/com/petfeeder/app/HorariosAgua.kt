package com.petfeeder.app

import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

/**
 * Módulo de HORARIOS DE AGUA. Es idéntico en funcionamiento al de comida
 * (Horarios.kt) pero trabaja sobre la tabla nueva horarios_agua y mide en ml.
 *
 * Se abre desde el botón flotante de la pantalla Horarios. Incluye su propio
 * botón flotante que abre la pantalla de dispensar agua manual.
 */
class HorariosAgua : AppCompatActivity() {

    private lateinit var db: PawFeederDatabase
    private lateinit var horariosContainer: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var btnNuevoHorario: LinearLayout
    private lateinit var cardTotalDiario: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horarios_agua)

        db = PawFeederDatabase(this)
        horariosContainer = findViewById(R.id.horariosAguaContainer)
        emptyState = findViewById(R.id.emptyStateAgua)
        btnNuevoHorario = findViewById(R.id.btnNuevoHorarioAgua)
        cardTotalDiario = findViewById(R.id.cardTotalDiarioAgua)

        findViewById<FrameLayout>(R.id.btnBackAgua).setOnClickListener { finish() }

        // Botón flotante -> dispensar agua manualmente
        findViewById<FloatingActionButton>(R.id.fabDispensarAgua).setOnClickListener {
            startActivity(Intent(this, DispensarAguaManual::class.java))
        }

        setupAddButtons()
        loadHorarios()
    }

    override fun onResume() {
        super.onResume()
        loadHorarios()
    }

    // ── CARGA Y RENDERIZADO ──────────────────────────────

    private fun loadHorarios() {
        val userId = UserSession.getId(this)
        lifecycleScope.launch {
            val horarios: List<HorarioAgua> = try {
                val resp = RetrofitClient.api.getHorariosAgua(userId)
                if (resp.isSuccessful && resp.body() != null) {
                    val lista = resp.body()!!.map { it.toHorarioAgua() }
                    db.replaceAllHorariosAgua(lista)
                    lista
                } else db.getAllHorariosAgua()
            } catch (e: Exception) {
                db.getAllHorariosAgua()
            }
            renderHorarios(horarios)
        }
    }

    private fun renderHorarios(horarios: List<HorarioAgua>) {
        horariosContainer.removeAllViews()

        if (horarios.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            horariosContainer.visibility = View.GONE
            btnNuevoHorario.visibility = View.GONE
            cardTotalDiario.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            horariosContainer.visibility = View.VISIBLE
            btnNuevoHorario.visibility = View.VISIBLE
            cardTotalDiario.visibility = View.VISIBLE

            horarios.forEachIndexed { i, horario ->
                val itemView = inflateHorarioItem(horario)
                horariosContainer.addView(itemView)
                itemView.alpha = 0f
                itemView.translationY = 30f
                itemView.animate()
                    .alpha(1f).translationY(0f)
                    .setDuration(400).setStartDelay(i * 70L)
                    .setInterpolator(DecelerateInterpolator(1.4f))
                    .start()
            }
            updateTotal(horarios)
        }

        val mascotaActiva = db.getActivaMascota()
        if (mascotaActiva != null) {
            findViewById<TextView>(R.id.tvSubtitleAgua)?.text =
                "Programa el agua de ${mascotaActiva.nombre}"
        }

        // Reprograma las alarmas de dispensado con los horarios actuales
        FeedScheduler.rescheduleAll(this)
    }

    private fun inflateHorarioItem(horario: HorarioAgua): View {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.item_horario, horariosContainer, false)

        val tvNombre = view.findViewById<TextView>(R.id.tvHorarioNombre)
        val tvInfo = view.findViewById<TextView>(R.id.tvHorarioInfo)
        val ivIcon = view.findViewById<ImageView>(R.id.ivHorarioIcon)
        val iconBg = view.findViewById<FrameLayout>(R.id.ivHorarioIconBg)
        val switchActivo = view.findViewById<MaterialSwitch>(R.id.switchHorarioActivo)
        val diasRow = view.findViewById<LinearLayout>(R.id.diasRowHorario)
        val btnDelete = view.findViewById<ImageView>(R.id.btnDeleteHorario)

        tvNombre.text = horario.nombre
        tvInfo.text = "${horario.hora} · ${horario.cantidadMl.toInt()} ml"
        switchActivo.isChecked = horario.activo
        view.alpha = if (horario.activo) 1f else 0.55f

        // Todos los horarios de agua usan el ícono de gota azul
        ivIcon.setImageResource(R.drawable.ic_water)
        ivIcon.setColorFilter(getColor(R.color.blue_primary))
        iconBg.setBackgroundResource(R.drawable.bg_icon_circle_blue)

        val dias = listOf(
            "L" to horario.lunes,
            "M" to horario.martes,
            "M" to horario.miercoles,
            "J" to horario.jueves,
            "V" to horario.viernes,
            "S" to horario.sabado,
            "D" to horario.domingo
        )
        val dp = resources.displayMetrics.density
        dias.forEach { (letra, activo) ->
            val tv = TextView(this).apply {
                text = letra
                textSize = 11f
                gravity = Gravity.CENTER
                val size = (28 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = (4 * dp).toInt()
                }
                if (activo) {
                    setBackgroundResource(R.drawable.bg_day_active)
                    setTextColor(getColor(R.color.blue_primary))
                    setTypeface(null, Typeface.BOLD)
                } else {
                    setTextColor(getColor(R.color.text_hint))
                }
            }
            diasRow.addView(tv)
        }

        switchActivo.setOnCheckedChangeListener { _, isChecked ->
            view.animate().alpha(if (isChecked) 1f else 0.55f).setDuration(250).start()
            lifecycleScope.launch {
                try { RetrofitClient.api.activoHorarioAgua(horario.id, isChecked) } catch (_: Exception) {}
                db.updateHorarioAguaActivo(horario.id, isChecked)
                updateTotal(db.getAllHorariosAgua())
                FeedScheduler.rescheduleAll(this@HorariosAgua)
            }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Eliminar horario")
                .setMessage("¿Eliminar el horario de agua \"${horario.nombre}\"?")
                .setPositiveButton("Eliminar") { _, _ ->
                    lifecycleScope.launch {
                        try { RetrofitClient.api.borrarHorarioAgua(horario.id) } catch (_: Exception) {}
                        db.deleteHorarioAgua(horario.id)
                        loadHorarios()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        return view
    }

    private fun updateTotal(horarios: List<HorarioAgua>) {
        val total = horarios.filter { it.activo }.sumOf { it.cantidadMl }.toInt()
        val mascota = db.getActivaMascota()
        // Necesidad de agua aproximada: ~55 ml por kg de peso al día
        val recomendado = if (mascota != null && mascota.pesoKg > 0)
            (mascota.pesoKg * 55).toInt() else 500
        findViewById<TextView>(R.id.tvTotalMlSchedule)?.text = "$total ml"
        findViewById<TextView>(R.id.tvRecomendadoAgua)?.text = "/ $recomendado ml recomendados"
        val progress = findViewById<ProgressBar>(R.id.progressDailyAgua)
        progress?.max = recomendado
        progress?.progress = total.coerceAtMost(recomendado)
    }

    // ── CRUD ─────────────────────────────────────────────

    private fun setupAddButtons() {
        btnNuevoHorario.setOnClickListener { showHorarioDialog() }
        findViewById<Button>(R.id.btnAgregarEmptyAgua)?.setOnClickListener { showHorarioDialog() }
    }

    private fun showHorarioDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_horario, null)

        val spinnerNombre = dialogView.findViewById<Spinner>(R.id.spinnerNombreHorario)
        val btnHora = dialogView.findViewById<LinearLayout>(R.id.btnSeleccionarHora)
        val tvHora = dialogView.findViewById<TextView>(R.id.tvHoraSeleccionada)
        val etCantidad = dialogView.findViewById<EditText>(R.id.etPorcionGramos)
        val cbLunes = dialogView.findViewById<CheckBox>(R.id.cbLunes)
        val cbMartes = dialogView.findViewById<CheckBox>(R.id.cbMartes)
        val cbMiercoles = dialogView.findViewById<CheckBox>(R.id.cbMiercoles)
        val cbJueves = dialogView.findViewById<CheckBox>(R.id.cbJueves)
        val cbViernes = dialogView.findViewById<CheckBox>(R.id.cbViernes)
        val cbSabado = dialogView.findViewById<CheckBox>(R.id.cbSabado)
        val cbDomingo = dialogView.findViewById<CheckBox>(R.id.cbDomingo)

        // Sugerencia de cantidad en ml (el campo de layout dice "gramos", aquí es ml)
        etCantidad.hint = "Cantidad en ml"
        etCantidad.setText("200")

        val nombres = listOf("Mañana", "Mediodía", "Tarde", "Noche", "Otro")
        spinnerNombre.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, nombres
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        var selectedHour = 8
        var selectedMinute = 0

        btnHora.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                selectedHour = h
                selectedMinute = m
                tvHora.text = formatHora(h, m)
            }, selectedHour, selectedMinute, false).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Nuevo horario de agua")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                if (!cbLunes.isChecked && !cbMartes.isChecked && !cbMiercoles.isChecked &&
                    !cbJueves.isChecked && !cbViernes.isChecked &&
                    !cbSabado.isChecked && !cbDomingo.isChecked
                ) {
                    Toast.makeText(this, "Selecciona al menos un día", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val ml = etCantidad.text.toString().toDoubleOrNull() ?: 200.0
                guardarHorarioAgua(
                    HorarioAgua(
                        nombre = spinnerNombre.selectedItem.toString(),
                        hora = formatHora(selectedHour, selectedMinute),
                        lunes = cbLunes.isChecked,
                        martes = cbMartes.isChecked,
                        miercoles = cbMiercoles.isChecked,
                        jueves = cbJueves.isChecked,
                        viernes = cbViernes.isChecked,
                        sabado = cbSabado.isChecked,
                        domingo = cbDomingo.isChecked,
                        cantidadMl = ml
                    )
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Crea el horario de agua en la API (petfeeder_db); si falla, lo guarda local. */
    private fun guardarHorarioAgua(h: HorarioAgua) {
        val userId = UserSession.getId(this)
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.crearHorarioAgua(h.toApi(userId))
                if (!resp.isSuccessful) db.insertHorarioAgua(h)
            } catch (e: Exception) {
                db.insertHorarioAgua(h)
            }
            loadHorarios()
        }
    }

    private fun formatHora(h: Int, m: Int): String {
        val amPm = if (h < 12) "AM" else "PM"
        val displayH = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format("%02d:%02d %s", displayH, m, amPm)
    }
}
