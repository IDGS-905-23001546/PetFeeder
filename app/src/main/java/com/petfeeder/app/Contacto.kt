package com.petfeeder.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

/**
 * Módulo "Ayuda y soporte" (Contáctanos). Muestra vías de contacto (correo,
 * GitHub, manual) y un formulario. Al enviar, abre la app de correo con el
 * mensaje ya redactado hacia el correo de soporte.
 */
class Contacto : AppCompatActivity() {

    private val correoSoporte = "carlosriosrmz17@gmail.com"
    private val urlGithub = "https://github.com/pawfeeder"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacto)

        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener { finish() }

        // Asuntos del formulario
        val asuntos = listOf(
            "Duda general", "Problema técnico", "Sugerencia",
            "Reporte de bug", "Ayuda con el dispositivo", "Otro"
        )
        val spinner = findViewById<Spinner>(R.id.spinnerAsunto)
        spinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, asuntos
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Tarjetas
        findViewById<LinearLayout>(R.id.cardCorreo).setOnClickListener { abrirCorreo("", "") }
        findViewById<LinearLayout>(R.id.cardGithub).setOnClickListener { abrirUrl(urlGithub) }
        findViewById<LinearLayout>(R.id.cardManual).setOnClickListener { abrirManual() }

        // Prellenar con la sesión actual
        findViewById<EditText>(R.id.etNombre).setText(UserSession.getNombre(this))
        findViewById<EditText>(R.id.etCorreo).setText(UserSession.getEmail(this))

        findViewById<Button>(R.id.btnEnviar).setOnClickListener { enviar() }
    }

    private fun enviar() {
        val nombre = findViewById<EditText>(R.id.etNombre).text.toString().trim()
        val correo = findViewById<EditText>(R.id.etCorreo).text.toString().trim()
        val asunto = findViewById<Spinner>(R.id.spinnerAsunto).selectedItem.toString()
        val mensaje = findViewById<EditText>(R.id.etMensaje).text.toString().trim()

        if (nombre.isEmpty() || correo.isEmpty() || mensaje.isEmpty()) {
            Toast.makeText(this, "Llena nombre, correo y mensaje", Toast.LENGTH_SHORT).show()
            return
        }

        val cuerpo = "Nombre: $nombre\nCorreo: $correo\n\n$mensaje"
        abrirCorreo("[PetFeeder] $asunto", cuerpo)
    }

    /** Abre la app de correo con destinatario, asunto y cuerpo ya puestos. */
    private fun abrirCorreo(asunto: String, cuerpo: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(correoSoporte))
            if (asunto.isNotEmpty()) putExtra(Intent.EXTRA_SUBJECT, asunto)
            if (cuerpo.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, cuerpo)
        }
        try {
            startActivity(Intent.createChooser(intent, "Enviar con"))
        } catch (e: Exception) {
            Toast.makeText(this, "No hay app de correo. Escríbenos a $correoSoporte", Toast.LENGTH_LONG).show()
        }
    }

    /** Copia el PDF del manual (empaquetado en assets) a cache y lo abre. */
    private fun abrirManual() {
        try {
            val pdf = File(cacheDir, "manual_petfeeder.pdf")
            assets.open("manual_petfeeder.pdf").use { input ->
                pdf.outputStream().use { output -> input.copyTo(output) }
            }
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", pdf)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Abrir manual con"))
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir el manual. Instala un lector de PDF.", Toast.LENGTH_LONG).show()
        }
    }

    private fun abrirUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, url, Toast.LENGTH_LONG).show()
        }
    }
}
