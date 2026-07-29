package com.petfeeder.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class Principal : AppCompatActivity() {

    private lateinit var db: PawFeederDatabase
    private var firstLoad = true
    private var warningShown = false
    private val sessionHandler = Handler(Looper.getMainLooper())
    private val sessionCheckInterval = 30_000L

    private val sessionCheckRunnable = object : Runnable {
        override fun run() {
            checkSession()
            sessionHandler.postDelayed(this, sessionCheckInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        db = PawFeederDatabase(this)

        setupBottomNav()
        setupDispenseButton()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        UserSession.touch(this)
        warningShown = false
        loadData()
        if (firstLoad) {
            firstLoad = false
            runEntranceAnimations()
        }
        sessionHandler.postDelayed(sessionCheckRunnable, sessionCheckInterval)
    }

    override fun onPause() {
        super.onPause()
        sessionHandler.removeCallbacks(sessionCheckRunnable)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        UserSession.touch(this)
    }

    // ── CARGA DE DATOS REALES ────────────────────────────

    private fun loadData() {
        loadUserName()
        loadActivePet()
        loadNextMeal()
        loadDaySummary()
    }

    private fun loadUserName() {
        val nombre = UserSession.getNombre(this)
        findViewById<TextView>(R.id.tvUserName).text = nombre
    }

    private fun loadActivePet() {
        val petCard = findViewById<LinearLayout>(R.id.petCard)
        val petCardEmpty = findViewById<LinearLayout>(R.id.petCardEmpty)
        val activePet = db.getActivaMascota()

        if (activePet != null) {
            petCard.visibility = View.VISIBLE
            petCardEmpty.visibility = View.GONE

            findViewById<TextView>(R.id.tvPetName).text = activePet.nombre
            val info = buildString {
                append(activePet.raza)
                if (activePet.edadAnos > 0) append(" · ${activePet.edadAnos} año(s)")
                if (activePet.pesoKg > 0) append(" · ${activePet.pesoKg}kg")
            }
            findViewById<TextView>(R.id.tvPetInfo).text = info

            val recomendado = when (activePet.tamano) {
                "pequeño" -> 80
                "mediano" -> 180
                "grande" -> 280
                "gigante" -> 450
                else -> 120
            }
            findViewById<TextView>(R.id.tvDispensarSubtitle).text =
                "Porcion recomendada: ${recomendado}g"

            val dispensador = db.getDispensador()
            val badgesRow = findViewById<LinearLayout>(R.id.petCardBadges)
            if (dispensador != null) {
                badgesRow.visibility = View.VISIBLE
                val estadoTexto = if (dispensador.estaEnLinea) "Conectado" else "Sin conexion"
                // El primer badge muestra estado WiFi — usamos el badge "Conectado" siempre visible
                // Actualizar batería
                findViewById<TextView>(R.id.tvBattery).text = "${dispensador.bateriaPercent}%"
            } else {
                badgesRow.visibility = View.GONE
            }
        } else {
            petCard.visibility = View.GONE
            petCardEmpty.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvDispensarSubtitle).text = "Dispensa comida manualmente"
        }
    }

    private fun loadNextMeal() {
        val cardNextMeal = findViewById<LinearLayout>(R.id.cardNextMeal)
        val cardNextMealEmpty = findViewById<LinearLayout>(R.id.cardNextMealEmpty)
        val horarios = db.getAllHorarios().filter { it.activo }

        if (horarios.isEmpty()) {
            cardNextMeal.visibility = View.GONE
            cardNextMealEmpty.visibility = View.VISIBLE
        } else {
            cardNextMeal.visibility = View.VISIBLE
            cardNextMealEmpty.visibility = View.GONE

            val next = findNextHorario(horarios)
            if (next != null) {
                findViewById<TextView>(R.id.tvNextMealName).text = next.nombre
                findViewById<TextView>(R.id.tvNextMealTime).text = "Hoy · ${next.hora}"
                findViewById<TextView>(R.id.tvNextMealGrams).text = "${next.porcionGramos.toInt()}g"
                val targetMins = parseHoraToMinutes(next.hora)
                findViewById<TextView>(R.id.tvNextMealCountdown).text = minutesToCountdown(targetMins)
            }
        }
    }

    private fun loadDaySummary() {
        val cardSummaryRow = findViewById<LinearLayout>(R.id.cardSummaryRow)
        val cardSummaryEmpty = findViewById<LinearLayout>(R.id.cardSummaryEmpty)
        val dispensaciones = db.getTodayDispensaciones()

        if (dispensaciones.isEmpty()) {
            cardSummaryRow.visibility = View.GONE
            cardSummaryEmpty.visibility = View.VISIBLE
        } else {
            cardSummaryRow.visibility = View.VISIBLE
            cardSummaryEmpty.visibility = View.GONE

            val totalGrams = dispensaciones.sumOf { it.porcionGramos }.toInt()
            val horariosActivos = db.getAllHorarios().filter { it.activo }.size
            val countHoy = dispensaciones.size

            val mealsText = if (horariosActivos > 0) "$countHoy/$horariosActivos" else "$countHoy"
            findViewById<TextView>(R.id.tvMealsCount).text = mealsText
            findViewById<TextView>(R.id.tvTotalGrams).text = "${totalGrams}g"
        }
    }

    // ── HELPERS HORARIOS ─────────────────────────────────

    private fun findNextHorario(horarios: List<Horario>): Horario? {
        val cal = Calendar.getInstance()
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        var nextHorario: Horario? = null
        var nextMins = Int.MAX_VALUE
        var firstOfDay: Horario? = null
        var firstMins = Int.MAX_VALUE

        for (h in horarios) {
            val mins = parseHoraToMinutes(h.hora)
            if (mins < firstMins) { firstMins = mins; firstOfDay = h }
            if (mins > currentMins && mins < nextMins) { nextMins = mins; nextHorario = h }
        }
        return nextHorario ?: firstOfDay
    }

    private fun parseHoraToMinutes(hora: String): Int {
        return try {
            val sdf = SimpleDateFormat("hh:mm a", Locale.US)
            val date = sdf.parse(hora) ?: return 0
            val cal = Calendar.getInstance()
            cal.time = date
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (_: Exception) { 0 }
    }

    private fun minutesToCountdown(targetMins: Int): String {
        val cal = Calendar.getInstance()
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val diff = if (targetMins > currentMins) targetMins - currentMins
                   else (24 * 60 - currentMins) + targetMins
        val h = diff / 60
        val m = diff % 60
        return when {
            h > 0 && m > 0 -> "en ${h}h ${m}m"
            h > 0 -> "en ${h}h"
            m > 0 -> "en ${m}m"
            else -> "ahora"
        }
    }

    // ── SESIÓN ───────────────────────────────────────────

    private fun checkSession() {
        if (UserSession.isSessionExpired(this)) {
            sessionHandler.removeCallbacks(sessionCheckRunnable)
            UserSession.logoutAndRedirect(this)
            finish()
            return
        }
        if (UserSession.shouldShowWarning(this) && !warningShown) {
            warningShown = true
            AlertDialog.Builder(this)
                .setTitle("Sesión próxima a expirar")
                .setMessage("Por inactividad, tu sesión cerrará en 5 minutos. Toca la pantalla para continuar.")
                .setPositiveButton("Entendido") { _, _ ->
                    UserSession.touch(this)
                    warningShown = false
                }
                .setCancelable(false)
                .show()
        }
    }

    // ── NAVEGACIÓN Y BOTONES ─────────────────────────────

    private fun setupClickListeners() {
        // Ver todas → Horarios
        findViewById<TextView>(R.id.tvVerTodas).setOnClickListener {
            navigateTo(Horarios::class.java)
        }
        // Agregar mascota desde inicio
        findViewById<LinearLayout>(R.id.btnAddPetFromHome).setOnClickListener {
            navigateTo(Mascotas::class.java)
        }
        // Agregar horario desde inicio
        findViewById<LinearLayout>(R.id.btnAddScheduleFromHome).setOnClickListener {
            navigateTo(Horarios::class.java)
        }
        // Botón campana → Notificaciones
        findViewById<FrameLayout>(R.id.btnNotifications).setOnClickListener {
            navigateTo(Notificaciones::class.java)
        }
        // Card mascota activa → Mascotas
        findViewById<LinearLayout>(R.id.petCard).setOnClickListener {
            navigateTo(Mascotas::class.java)
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_pets -> { navigateTo(Mascotas::class.java); false }
                R.id.nav_schedule -> { navigateTo(Horarios::class.java); false }
                R.id.nav_equipment -> { navigateTo(Equipo::class.java); false }
                R.id.nav_profile -> { navigateTo(Perfil::class.java); false }
                else -> false
            }
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        LoadingDialog.show(supportFragmentManager, "Cargando...")
        findViewById<View>(android.R.id.content).postDelayed({
            LoadingDialog.dismiss(supportFragmentManager)
            val intent = Intent(this, activityClass)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }, 500)
    }

    private fun setupDispenseButton() {
        val btn = findViewById<LinearLayout>(R.id.btnDispensarAhora)
        btn.setOnClickListener { v ->
            v.animate()
                .scaleX(0.96f).scaleY(0.96f)
                .setDuration(80)
                .withEndAction {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).withEndAction {
                        LoadingDialog.show(supportFragmentManager, "Abriendo dispensar...")
                        v.postDelayed({
                            LoadingDialog.dismiss(supportFragmentManager)
                            startActivity(Intent(this, DispensarManual::class.java))
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        }, 600)
                    }.start()
                }
                .start()
        }
    }

    private fun runEntranceAnimations() {
        val allIds = listOf(
            R.id.petCard to 0L, R.id.petCardEmpty to 0L,
            R.id.btnDispensarAhora to 120L,
            R.id.cardNextMeal to 220L, R.id.cardNextMealEmpty to 220L,
            R.id.cardSummaryRow to 300L, R.id.cardSummaryEmpty to 300L
        )
        for ((id, delay) in allIds) {
            val v = findViewById<View>(id) ?: continue
            if (v.visibility == View.VISIBLE) animateEntrance(id, delay)
        }
    }

    private fun animateEntrance(viewId: Int, delay: Long) {
        val v = findViewById<View>(viewId) ?: return
        v.alpha = 0f
        v.translationY = 50f
        v.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(480)
            .setStartDelay(delay)
            .setInterpolator(DecelerateInterpolator(1.6f))
            .start()
    }
}
