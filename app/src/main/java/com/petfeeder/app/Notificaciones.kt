package com.petfeeder.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class Notificaciones : AppCompatActivity() {

    private lateinit var db: PawFeederDatabase
    private lateinit var container: LinearLayout
    private lateinit var emptyState: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notificaciones)

        db = PawFeederDatabase(this)
        container = findViewById(R.id.notificacionesContainer)
        emptyState = findViewById(R.id.emptyState)

        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnMarcarTodas).setOnClickListener { marcarTodasLeidas() }

        loadNotificaciones()
    }

    private fun loadNotificaciones() {
        val userId = UserSession.getId(this)
        LoadingDialog.show(supportFragmentManager, "Cargando notificaciones...")
        lifecycleScope.launch {
            val notifs: List<NotificacionApi> = try {
                val resp = RetrofitClient.api.getNotificaciones(userId)
                if (resp.isSuccessful && resp.body() != null) resp.body()!!
                else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            LoadingDialog.dismiss(supportFragmentManager)
            renderNotificaciones(notifs)
        }
    }

    private fun renderNotificaciones(notifs: List<NotificacionApi>) {
        container.removeAllViews()
        if (notifs.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            container.visibility = View.GONE
            return
        }
        emptyState.visibility = View.GONE
        container.visibility = View.VISIBLE

        val sdf = SimpleDateFormat("d 'de' MMMM, h:mm a", Locale("es"))

        notifs.forEachIndexed { i, n ->
            val item = inflateNotificacion(n, sdf)
            container.addView(item)
            item.alpha = 0f
            item.translationY = 20f
            item.animate().alpha(1f).translationY(0f)
                .setDuration(350).setStartDelay(i * 50L)
                .setInterpolator(DecelerateInterpolator(1.3f)).start()
        }
    }

    private fun inflateNotificacion(n: NotificacionApi, sdf: SimpleDateFormat): View {
        val dp = resources.displayMetrics.density

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_white_card)
            elevation = 2f * dp
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, 0, (10 * dp).toInt())
            layoutParams = lp
            setPadding((16 * dp).toInt(), (14 * dp).toInt(), (16 * dp).toInt(), (14 * dp).toInt())
        }

        // Icono según tipo
        val iconFrame = FrameLayout(this).apply {
            val size = (40 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = (14 * dp).toInt() }
            val bg = if (n.leida) R.drawable.bg_icon_circle_gray else R.drawable.bg_icon_circle_blue
            setBackgroundResource(bg)
        }
        val icon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams((20 * dp).toInt(), (20 * dp).toInt(), Gravity.CENTER)
            val res = when (n.tipo) {
                "dispensado" -> R.drawable.ic_check_circle
                "bateria" -> R.drawable.ic_bell
                "tolva" -> R.drawable.ic_bell
                else -> R.drawable.ic_bell
            }
            setImageResource(res)
            val color = if (n.leida) Color.parseColor("#ADB5BD") else getColor(R.color.blue_primary)
            setColorFilter(color)
        }
        iconFrame.addView(icon)
        card.addView(iconFrame)

        // Info
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvTitulo = TextView(this).apply {
            text = n.titulo
            textSize = 14f
            setTypeface(null, if (n.leida) Typeface.NORMAL else Typeface.BOLD)
            setTextColor(getColor(R.color.text_primary))
        }
        val tvMensaje = TextView(this).apply {
            text = n.mensaje ?: ""
            textSize = 12f
            setTextColor(getColor(R.color.text_secondary))
            maxLines = 2
        }
        val tvFecha = TextView(this).apply {
            val fecha = try {
                val limpia = n.createdAt.substringBefore('.')
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(limpia)
            } catch (_: Exception) { null }
            text = if (fecha != null) sdf.format(fecha) else ""
            textSize = 11f
            setTextColor(getColor(R.color.text_hint))
            setPadding(0, (4 * dp).toInt(), 0, 0)
        }
        info.addView(tvTitulo)
        if (!n.mensaje.isNullOrEmpty()) info.addView(tvMensaje)
        info.addView(tvFecha)
        card.addView(info)

        // Tap para marcar como leída
        if (!n.leida) {
            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener {
                marcarLeida(n.id)
                tvTitulo.setTypeface(null, Typeface.NORMAL)
                iconFrame.setBackgroundResource(R.drawable.bg_icon_circle_gray)
                icon.setColorFilter(Color.parseColor("#ADB5BD"))
            }
        }

        return card
    }

    private fun marcarLeida(notifId: Int) {
        lifecycleScope.launch {
            try { RetrofitClient.api.marcarNotificacionLeida(notifId) } catch (_: Exception) {}
        }
    }

    private fun marcarTodasLeidas() {
        val userId = UserSession.getId(this)
        LoadingDialog.show(supportFragmentManager, "Marcando todas...")
        lifecycleScope.launch {
            try { RetrofitClient.api.marcarTodasLeidas(userId) } catch (_: Exception) {}
            LoadingDialog.dismiss(supportFragmentManager)
            loadNotificaciones()
        }
    }
}
