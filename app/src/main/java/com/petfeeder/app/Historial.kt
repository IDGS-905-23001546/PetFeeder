package com.petfeeder.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class Historial : AppCompatActivity() {

    private lateinit var db: PawFeederDatabase
    private lateinit var container: LinearLayout
    private lateinit var emptyState: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        db = PawFeederDatabase(this)
        container = findViewById(R.id.historialContainer)
        emptyState = findViewById(R.id.emptyState)

        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener { finish() }

        loadHistorial()
    }

    private fun loadHistorial() {
        val dispensaciones = db.getAllDispensaciones()

        if (dispensaciones.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            container.visibility = View.GONE
            return
        }

        emptyState.visibility = View.GONE
        container.visibility = View.VISIBLE
        container.removeAllViews()

        // Agrupar por día
        val sdfDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val sdfDisplay = SimpleDateFormat("d 'de' MMMM", Locale("es"))
        val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault())

        val hoy = sdfDate.format(Date())
        val ayer = sdfDate.format(Date(System.currentTimeMillis() - 86_400_000))

        val grouped = dispensaciones.groupBy { sdfDate.format(Date(it.fechaHora)) }

        grouped.entries.forEachIndexed { gi, (dateKey, items) ->
            // Encabezado de día
            val headerLabel = when (dateKey) {
                hoy -> "HOY"
                ayer -> "AYER"
                else -> sdfDisplay.format(Date(items.first().fechaHora)).uppercase()
            }
            addDayHeader(headerLabel)

            // Items del día
            items.forEachIndexed { idx, d ->
                val itemView = inflateHistorialItem(d, sdfTime)
                container.addView(itemView)
                itemView.alpha = 0f
                itemView.translationY = 20f
                itemView.animate()
                    .alpha(1f).translationY(0f)
                    .setDuration(350).setStartDelay((gi * items.size + idx) * 50L)
                    .setInterpolator(DecelerateInterpolator(1.3f)).start()
            }
        }
    }

    private fun addDayHeader(label: String) {
        val tv = TextView(this).apply {
            text = label
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.text_secondary))
            letterSpacing = 0.08f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 16.dp, 0, 8.dp)
            layoutParams = lp
        }
        container.addView(tv)
    }

    private fun inflateHistorialItem(d: Dispensacion, sdf: SimpleDateFormat): View {
        val isManual = d.tipo == "manual"
        val dp = resources.displayMetrics.density

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_white_card)
            elevation = 2f * dp
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, 0, 10.dp)
            layoutParams = lp
            setPadding(16.dp, 14.dp, 16.dp, 14.dp)
        }

        // Ícono
        val iconFrame = FrameLayout(this).apply {
            val size = 40.dp
            layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = 14.dp }
            setBackgroundResource(if (isManual) R.drawable.bg_historial_bolt else R.drawable.bg_historial_check)
        }
        val icon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(20.dp, 20.dp, Gravity.CENTER)
            setImageResource(if (isManual) R.drawable.ic_bolt else R.drawable.ic_check_circle)
            setColorFilter(if (isManual) Color.parseColor("#C68A00") else getColor(R.color.blue_primary))
        }
        iconFrame.addView(icon)
        row.addView(iconFrame)

        // Info
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvNombre = TextView(this).apply {
            text = d.nombre
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.text_primary))
        }
        val tvSubtitle = TextView(this).apply {
            val hora = sdf.format(Date(d.fechaHora))
            text = "$hora · ${if (isManual) "Manual" else "Auto"}"
            textSize = 12f
            setTextColor(getColor(R.color.text_secondary))
        }
        info.addView(tvNombre)
        info.addView(tvSubtitle)
        row.addView(info)

        // Gramos
        val tvGramos = TextView(this).apply {
            text = "${d.porcionGramos.toInt()}g"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(getColor(R.color.text_primary))
        }
        row.addView(tvGramos)

        return row
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
