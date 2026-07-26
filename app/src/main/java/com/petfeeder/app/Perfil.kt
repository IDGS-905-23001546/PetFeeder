package com.petfeeder.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class Perfil : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        loadUserData()
        setupMenu()
        setupBottomNav()
        runEntranceAnimations()
    }

    private fun loadUserData() {
        val nombre = UserSession.getNombre(this)
        val email = UserSession.getEmail(this)
        findViewById<TextView>(R.id.tvUserName).text = nombre
        findViewById<TextView>(R.id.tvUserEmail).text = email
        findViewById<TextView>(R.id.tvUserInitial).text =
            nombre.firstOrNull()?.uppercase() ?: "U"
    }

    private fun setupMenu() {
        findViewById<LinearLayout>(R.id.menuHistorial).setOnClickListener {
            LoadingDialog.show(supportFragmentManager, "Cargando historial...")
            findViewById<View>(android.R.id.content).postDelayed({
                LoadingDialog.dismiss(supportFragmentManager)
                startActivity(Intent(this, Historial::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }, 500)
        }
        // Ayuda y soporte -> módulo de Contacto
        findViewById<LinearLayout>(R.id.menuAyuda).setOnClickListener {
            LoadingDialog.show(supportFragmentManager, "Cargando ayuda...")
            findViewById<View>(android.R.id.content).postDelayed({
                LoadingDialog.dismiss(supportFragmentManager)
                startActivity(Intent(this, Contacto::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }, 500)
        }

        // Los siguientes siguen como pantallas futuras — placeholder
        listOf(R.id.menuCompartir, R.id.menuNotificaciones)
            .forEach { id ->
                findViewById<LinearLayout>(id).setOnClickListener {
                    Toast.makeText(this, "Proximamente...", Toast.LENGTH_SHORT).show()
                }
            }

        // Seguridad -> pantalla completa
        findViewById<LinearLayout>(R.id.menuSeguridad).setOnClickListener {
            LoadingDialog.show(supportFragmentManager, "Cargando...")
            findViewById<View>(android.R.id.content).postDelayed({
                LoadingDialog.dismiss(supportFragmentManager)
                startActivity(Intent(this, Seguridad::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }, 500)
        }
        findViewById<LinearLayout>(R.id.btnCerrarSesion).setOnClickListener {
            confirmarCerrarSesion()
        }
    }

    private fun confirmarCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesion")
            .setMessage("¿Seguro que deseas cerrar sesion?")
            .setPositiveButton("Cerrar sesion") { _, _ ->
                LoadingDialog.show(supportFragmentManager, "Cerrando sesión...")
                findViewById<View>(android.R.id.content).postDelayed({
                    UserSession.clear(this)
                    LoadingDialog.dismiss(supportFragmentManager)
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                }, 600)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun runEntranceAnimations() {
        listOf(R.id.menuHistorial, R.id.menuCompartir, R.id.menuNotificaciones,
            R.id.menuSeguridad, R.id.menuAyuda, R.id.btnCerrarSesion)
            .forEachIndexed { i, id ->
                val v = findViewById<View>(id) ?: return@forEachIndexed
                v.alpha = 0f
                v.translationY = 20f
                v.animate().alpha(1f).translationY(0f)
                    .setDuration(350).setStartDelay(i * 50L)
                    .setInterpolator(DecelerateInterpolator(1.4f)).start()
            }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> true
                R.id.nav_home -> { navigateTo(Principal::class.java); false }
                R.id.nav_pets -> { navigateTo(Mascotas::class.java); false }
                R.id.nav_schedule -> { navigateTo(Horarios::class.java); false }
                R.id.nav_equipment -> { navigateTo(Equipo::class.java); false }
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
