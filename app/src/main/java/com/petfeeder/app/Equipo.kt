package com.petfeeder.app

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class Equipo : AppCompatActivity() {

    private lateinit var db: PawFeederDatabase
    private lateinit var emptyState: LinearLayout
    private lateinit var deviceCard: LinearLayout
    private lateinit var stepsSection: LinearLayout
    private lateinit var btnConectarWifi: LinearLayout
    private lateinit var btnDesvincular: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equipo)

        db = PawFeederDatabase(this)
        emptyState = findViewById(R.id.emptyState)
        deviceCard = findViewById(R.id.deviceCard)
        stepsSection = findViewById(R.id.stepsSection)
        btnConectarWifi = findViewById(R.id.btnConectarWifi)
        btnDesvincular = findViewById(R.id.btnDesvincular)

        setupBottomNav()
        setupButtons()
        loadDispensador()
    }

    override fun onResume() {
        super.onResume()
        loadDispensador()
    }

    // ── CARGA Y RENDERIZADO ──────────────────────────────

    private fun loadDispensador() {
        val dispensador = db.getDispensador()

        if (dispensador == null) {
            mostrarEstadoVacio()
        } else {
            mostrarDispensador(dispensador)
        }
    }

    private fun mostrarEstadoVacio() {
        emptyState.visibility = View.VISIBLE
        deviceCard.visibility = View.GONE
        stepsSection.visibility = View.VISIBLE   // pasos siempre visibles
        btnConectarWifi.visibility = View.VISIBLE
        btnDesvincular.visibility = View.GONE

        emptyState.alpha = 0f
        emptyState.animate().alpha(1f).setDuration(400)
            .setInterpolator(DecelerateInterpolator()).start()
        animarSeccion(stepsSection, 100)
        animarSeccion(btnConectarWifi, 200)
    }

    private fun mostrarDispensador(d: Dispensador) {
        emptyState.visibility = View.GONE
        deviceCard.visibility = View.VISIBLE
        stepsSection.visibility = View.VISIBLE
        btnConectarWifi.visibility = View.VISIBLE
        btnDesvincular.visibility = View.VISIBLE

        // Datos en la tarjeta
        findViewById<TextView>(R.id.tvDeviceName).text = d.nombre
        findViewById<TextView>(R.id.tvDeviceCode).text = "${d.codigoUnico} · ${d.firmwareVersion}"
        findViewById<TextView>(R.id.tvBateriaValue).text = "${d.bateriaPercent}%"
        findViewById<TextView>(R.id.tvWifiValue).text =
            if (d.ssidWifi.isNotEmpty()) d.ssidWifi else "Buena"
        findViewById<TextView>(R.id.tvTolvaValue).text =
            String.format("%.1fkg", d.tolvaKg)

        // Badge estado
        val tvBadge = findViewById<TextView>(R.id.tvEstadoBadge)
        if (d.estaEnLinea) {
            tvBadge.text = "En linea"
            tvBadge.setBackgroundResource(R.drawable.bg_badge_connected)
        } else {
            tvBadge.text = "Sin conexion"
            tvBadge.setBackgroundResource(R.drawable.bg_offline_badge)
        }

        // Texto del botón según estado
        val tvBtn = findViewById<TextView>(R.id.tvBtnConectar)
        tvBtn.text = if (d.estaEnLinea) "Reconectar dispositivo" else "Conectar por WiFi"

        // Animaciones de entrada
        animarSeccion(deviceCard, 0)
        animarSeccion(stepsSection, 120)
        animarSeccion(btnConectarWifi, 220)
        animarSeccion(btnDesvincular, 280)
    }

    private fun animarSeccion(view: View, delay: Long) {
        view.alpha = 0f
        view.translationY = 30f
        view.animate().alpha(1f).translationY(0f)
            .setDuration(420).setStartDelay(delay)
            .setInterpolator(DecelerateInterpolator(1.4f)).start()
    }

    // ── CRUD ─────────────────────────────────────────────

    private fun setupButtons() {
        val btnVincularEmpty = findViewById<Button>(R.id.btnVincularEmpty)
        btnVincularEmpty.setOnClickListener { showVincularDialog() }
        btnConectarWifi.setOnClickListener { onConectarWifi() }
        btnDesvincular.setOnClickListener { confirmarDesvincular() }
    }

    private fun showVincularDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_vincular_dispensador, null)

        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreDispensador)
        val etCodigo = dialogView.findViewById<EditText>(R.id.etCodigoDispensador)
        val etSsid = dialogView.findViewById<EditText>(R.id.etSsidWifi)

        AlertDialog.Builder(this)
            .setTitle("Vincular dispensador")
            .setView(dialogView)
            .setPositiveButton("Vincular") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val codigo = etCodigo.text.toString().trim().uppercase()
                val ssid = etSsid.text.toString().trim()

                if (nombre.isEmpty() || codigo.isEmpty()) {
                    Toast.makeText(this, "Nombre y código son requeridos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                LoadingDialog.show(supportFragmentManager, "Vinculando dispensador...")
                lifecycleScope.launch {
                    val dispensador = Dispensador(
                        nombre = nombre,
                        codigoUnico = codigo,
                        ssidWifi = ssid,
                        estado = "offline"
                    )
                    db.insertDispensador(dispensador)
                    LoadingDialog.dismiss(supportFragmentManager)
                    Toast.makeText(this@Equipo, "Dispensador vinculado. Toca Conectar para sincronizar.", Toast.LENGTH_LONG).show()
                    loadDispensador()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun onConectarWifi() {
        val dispensador = db.getDispensador() ?: run {
            showVincularDialog()
            return
        }

        // Pedir/confirmar la IP del ESP32 (la muestra en el monitor serie al encender)
        val input = EditText(this).apply {
            hint = "IP del ESP32 (ej. 192.168.1.42)"
            setText(DeviceConfig.getIp(this@Equipo))
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Conectar por WiFi")
            .setMessage("Escribe la IP que muestra el ESP32 al encender. El teléfono y el ESP32 deben estar en la MISMA red WiFi.")
            .setView(input)
            .setPositiveButton("Conectar") { _, _ ->
                val ip = input.text.toString().trim()
                if (ip.isEmpty()) {
                    Toast.makeText(this, "Escribe la IP del dispositivo", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                DeviceConfig.setIp(this, ip)
                probarConexion(dispensador)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Hace ping real al ESP32 (/status) para confirmar que responde. */
    private fun probarConexion(dispensador: Dispensador) {
        val tvBtn = findViewById<TextView>(R.id.tvBtnConectar)
        tvBtn.text = "Conectando..."
        btnConectarWifi.isEnabled = false
        LoadingDialog.show(supportFragmentManager, "Conectando al dispositivo...")

        lifecycleScope.launch {
            val r = DeviceClient.consultarEstado(this@Equipo)
            LoadingDialog.dismiss(supportFragmentManager)
            btnConectarWifi.isEnabled = true
            if (r.exito) {
                db.updateDispensadorEstado(dispensador.id, "activo")
                Toast.makeText(this@Equipo, "¡Conectado al dispositivo!", Toast.LENGTH_SHORT).show()
            } else {
                db.updateDispensadorEstado(dispensador.id, "offline")
                Toast.makeText(
                    this@Equipo,
                    "No respondió. Revisa la IP y que estén en la misma WiFi.",
                    Toast.LENGTH_LONG
                ).show()
            }
            loadDispensador()
        }
    }

    private fun confirmarDesvincular() {
        val dispensador = db.getDispensador() ?: return
        AlertDialog.Builder(this)
            .setTitle("Desvincular dispositivo")
            .setMessage("¿Desvincular ${dispensador.nombre}? Deberás volver a configurarlo para usarlo.")
            .setPositiveButton("Desvincular") { _, _ ->
                LoadingDialog.show(supportFragmentManager, "Desvinculando...")
                lifecycleScope.launch {
                    db.deleteDispensador(dispensador.id)
                    LoadingDialog.dismiss(supportFragmentManager)
                    Toast.makeText(this@Equipo, "Dispositivo desvinculado", Toast.LENGTH_SHORT).show()
                    loadDispensador()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── NAVEGACIÓN ───────────────────────────────────────

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_equipment
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_equipment -> true
                R.id.nav_home -> { navigateTo(Principal::class.java); false }
                R.id.nav_pets -> { navigateTo(Mascotas::class.java); false }
                R.id.nav_schedule -> { navigateTo(Horarios::class.java); false }
                R.id.nav_profile -> { navigateTo(Perfil::class.java); false }
                else -> false
            }
        }
    }

    private fun navigateTo(cls: Class<*>) {
        LoadingDialog.show(supportFragmentManager, "Cargando...")
        findViewById<View>(android.R.id.content).postDelayed({
            LoadingDialog.dismiss(supportFragmentManager)
            startActivity(Intent(this, cls).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }, 500)
    }
}
