package com.petfeeder.app

import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.materialswitch.MaterialSwitch

class Horarios : AppCompatActivity() {

    private lateinit var db: PawFeederDatabase
    private lateinit var horariosContainer: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var btnNuevoHorario: LinearLayout
    private lateinit var cardTotalDiario: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horarios)

        db = PawFeederDatabase(this)
        horariosContainer = findViewById(R.id.horariosContainer)
        emptyState = findViewById(R.id.emptyState)
        btnNuevoHorario = findViewById(R.id.btnNuevoHorario)
        cardTotalDiario = findViewById(R.id.cardTotalDiario)

        setupBottomNav()
        setupAddButtons()
        loadHorarios()
    }

    override fun onResume() {
        super.onResume()
        loadHorarios()
    }

    // ── CARGA Y RENDERIZADO ──────────────────────────────

    private fun loadHorarios() {
        val horarios = db.getAllHorarios()
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

        // Subtítulo con mascota activa
        val mascotaActiva = db.getActivaMascota()
        if (mascotaActiva != null) {
            findViewById<TextView>(R.id.tvSubtitleHorarios)?.text =
                "Programa las comidas de ${mascotaActiva.nombre}"
        }
    }

    private fun inflateHorarioItem(horario: Horario): View {
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
        tvInfo.text = "${horario.hora} · ${horario.porcionGramos.toInt()}g"
        switchActivo.isChecked = horario.activo
        view.alpha = if (horario.activo) 1f else 0.55f

        // Ícono según nombre
        if (horario.nombre.equals("cena", ignoreCase = true)) {
            ivIcon.setImageResource(R.drawable.ic_moon)
            ivIcon.setColorFilter(getColor(R.color.blue_primary))
            iconBg.setBackgroundResource(R.drawable.bg_icon_circle_blue)
        } else {
            ivIcon.setImageResource(R.drawable.ic_sun)
            ivIcon.setColorFilter(Color.parseColor("#C68A00"))
            iconBg.setBackgroundResource(R.drawable.bg_icon_circle_cream)
        }

        // Fila de días
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

        // Toggle activo/inactivo
        switchActivo.setOnCheckedChangeListener { _, isChecked ->
            db.updateHorarioActivo(horario.id, isChecked)
            view.animate().alpha(if (isChecked) 1f else 0.55f).setDuration(250).start()
            updateTotal(db.getAllHorarios())
        }

        // Eliminar
        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Eliminar horario")
                .setMessage("¿Eliminar el horario \"${horario.nombre}\"?")
                .setPositiveButton("Eliminar") { _, _ ->
                    db.deleteHorario(horario.id)
                    loadHorarios()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        return view
    }

    private fun updateTotal(horarios: List<Horario>) {
        val total = horarios.filter { it.activo }.sumOf { it.porcionGramos }.toInt()
        val mascota = db.getActivaMascota()
        val recomendado = when (mascota?.tamano) {
            "pequeño" -> 80
            "mediano" -> 180
            "grande" -> 280
            "gigante" -> 450
            else -> 320
        }
        findViewById<TextView>(R.id.tvTotalGramsSchedule)?.text = "${total}g"
        findViewById<TextView>(R.id.tvRecomendado)?.text = "/ ${recomendado}g recomendados"
        val progress = findViewById<ProgressBar>(R.id.progressDaily)
        progress?.max = recomendado
        progress?.progress = total.coerceAtMost(recomendado)
    }

    // ── CRUD ─────────────────────────────────────────────

    private fun setupAddButtons() {
        btnNuevoHorario.setOnClickListener { showHorarioDialog() }
        findViewById<Button>(R.id.btnAgregarEmpty)?.setOnClickListener { showHorarioDialog() }
    }

    private fun showHorarioDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_horario, null)

        val spinnerNombre = dialogView.findViewById<Spinner>(R.id.spinnerNombreHorario)
        val btnHora = dialogView.findViewById<LinearLayout>(R.id.btnSeleccionarHora)
        val tvHora = dialogView.findViewById<TextView>(R.id.tvHoraSeleccionada)
        val etGramos = dialogView.findViewById<EditText>(R.id.etPorcionGramos)
        val cbLunes = dialogView.findViewById<CheckBox>(R.id.cbLunes)
        val cbMartes = dialogView.findViewById<CheckBox>(R.id.cbMartes)
        val cbMiercoles = dialogView.findViewById<CheckBox>(R.id.cbMiercoles)
        val cbJueves = dialogView.findViewById<CheckBox>(R.id.cbJueves)
        val cbViernes = dialogView.findViewById<CheckBox>(R.id.cbViernes)
        val cbSabado = dialogView.findViewById<CheckBox>(R.id.cbSabado)
        val cbDomingo = dialogView.findViewById<CheckBox>(R.id.cbDomingo)

        val nombres = listOf("Desayuno", "Almuerzo", "Cena", "Snack", "Otro")
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
            .setTitle("Nuevo horario")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                if (!cbLunes.isChecked && !cbMartes.isChecked && !cbMiercoles.isChecked &&
                    !cbJueves.isChecked && !cbViernes.isChecked &&
                    !cbSabado.isChecked && !cbDomingo.isChecked
                ) {
                    Toast.makeText(this, "Selecciona al menos un día", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val gramos = etGramos.text.toString().toDoubleOrNull() ?: 100.0
                db.insertHorario(
                    Horario(
                        nombre = spinnerNombre.selectedItem.toString(),
                        hora = formatHora(selectedHour, selectedMinute),
                        lunes = cbLunes.isChecked,
                        martes = cbMartes.isChecked,
                        miercoles = cbMiercoles.isChecked,
                        jueves = cbJueves.isChecked,
                        viernes = cbViernes.isChecked,
                        sabado = cbSabado.isChecked,
                        domingo = cbDomingo.isChecked,
                        porcionGramos = gramos
                    )
                )
                loadHorarios()
            }
            .setNegativeButton("Cancelar", null)
            .show()
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

    // ── NAVEGACIÓN ───────────────────────────────────────

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_schedule
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_schedule -> true
                R.id.nav_home -> { navigateTo(Principal::class.java); false }
                R.id.nav_pets -> { navigateTo(Mascotas::class.java); false }
                R.id.nav_equipment -> { navigateTo(Equipo::class.java); false }
                R.id.nav_profile -> { navigateTo(Perfil::class.java); false }
                else -> false
            }
        }
    }

    private fun navigateTo(cls: Class<*>) {
        startActivity(Intent(this, cls).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
