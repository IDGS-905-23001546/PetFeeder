package com.petfeeder.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.materialswitch.MaterialSwitch
import java.io.File

class Mascotas : AppCompatActivity() {

    private lateinit var db: PawFeederDatabase
    private lateinit var petsContainer: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var btnAgregarMascota: LinearLayout

    private lateinit var photoPickerLauncher: ActivityResultLauncher<String>
    private var dialogPhotoView: ImageView? = null
    private var dialogPlaceholder: LinearLayout? = null
    private var selectedPhotoPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mascotas)

        db = PawFeederDatabase(this)
        petsContainer = findViewById(R.id.petsContainer)
        emptyState = findViewById(R.id.emptyState)
        btnAgregarMascota = findViewById(R.id.btnAgregarMascota)

        photoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val savedPath = copyImageToInternal(uri)
                if (savedPath != null) {
                    selectedPhotoPath = savedPath
                    dialogPhotoView?.setImageURI(Uri.fromFile(File(savedPath)))
                    dialogPhotoView?.visibility = View.VISIBLE
                    dialogPlaceholder?.visibility = View.GONE
                }
            }
        }

        setupBottomNav()
        setupAddButtons()
        loadMascotas()
    }

    override fun onResume() {
        super.onResume()
        loadMascotas()
    }

    // ── CARGA Y RENDERIZADO ──────────────────────────────

    private fun loadMascotas() {
        val pets = db.getAllMascotas()
        petsContainer.removeAllViews()

        if (pets.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            petsContainer.visibility = View.GONE
            btnAgregarMascota.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            petsContainer.visibility = View.VISIBLE
            btnAgregarMascota.visibility = View.VISIBLE

            pets.forEachIndexed { i, mascota ->
                val itemView = inflatePetItem(mascota)
                petsContainer.addView(itemView)
                itemView.alpha = 0f
                itemView.translationY = 30f
                itemView.animate()
                    .alpha(1f).translationY(0f)
                    .setDuration(400).setStartDelay(i * 70L)
                    .setInterpolator(DecelerateInterpolator(1.4f))
                    .start()
            }
        }
    }

    private fun inflatePetItem(mascota: Mascota): View {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.item_mascota, petsContainer, false)

        val tvNombre = view.findViewById<TextView>(R.id.tvItemPetName)
        val tvInfo = view.findViewById<TextView>(R.id.tvItemPetInfo)
        val badge = view.findViewById<TextView>(R.id.badgeActiva)
        val avatarFrame = view.findViewById<FrameLayout>(R.id.petAvatarFrame)
        val tvInitial = view.findViewById<TextView>(R.id.tvPetInitial)
        val ivPhoto = view.findViewById<ImageView>(R.id.ivPetPhoto)
        val btnEdit = view.findViewById<ImageView>(R.id.btnEditPet)
        val btnDelete = view.findViewById<ImageView>(R.id.btnDeletePet)

        tvNombre.text = mascota.nombre
        tvInfo.text = "${mascota.raza} · ${mascota.edadAnos} año(s) · ${mascota.pesoKg} kg"
        tvInitial.text = mascota.nombre.firstOrNull()?.uppercase() ?: "?"
        badge.visibility = if (mascota.activa) View.VISIBLE else View.GONE

        if (mascota.fotoUri.isNotEmpty()) {
            val file = File(mascota.fotoUri)
            if (file.exists()) {
                ivPhoto.setImageURI(Uri.fromFile(file))
                ivPhoto.visibility = View.VISIBLE
                tvInitial.visibility = View.GONE
            }
        }

        if (ivPhoto.visibility != View.VISIBLE) {
            val avatarBgs = listOf(R.drawable.bg_avatar_amber, R.drawable.bg_avatar_blue_light)
            val textColors = listOf("#C68A00", "#4A7FA0")
            val idx = mascota.id % 2
            avatarFrame.setBackgroundResource(avatarBgs[idx])
            tvInitial.setTextColor(android.graphics.Color.parseColor(textColors[idx]))
        }

        btnEdit.setOnClickListener { showMascotaDialog(mascota) }
        btnDelete.setOnClickListener { confirmDeleteMascota(mascota) }

        return view
    }

    // ── CRUD ─────────────────────────────────────────────

    private fun setupAddButtons() {
        btnAgregarMascota.setOnClickListener { showMascotaDialog(null) }
        findViewById<Button>(R.id.btnAgregarEmpty).setOnClickListener { showMascotaDialog(null) }
    }

    private fun showMascotaDialog(mascota: Mascota?) {
        val isEdit = mascota != null
        selectedPhotoPath = mascota?.fotoUri ?: ""

        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_mascota, null)

        val etNombre = dialogView.findViewById<EditText>(R.id.etMascotaNombre)
        val spinnerRaza = dialogView.findViewById<Spinner>(R.id.spinnerRaza)
        val etEdad = dialogView.findViewById<EditText>(R.id.etMascotaEdad)
        val etPeso = dialogView.findViewById<EditText>(R.id.etMascotaPeso)
        val switchActiva = dialogView.findViewById<MaterialSwitch>(R.id.switchMascotaActiva)
        val framePhoto = dialogView.findViewById<FrameLayout>(R.id.framePhotoMascota)
        val ivFoto = dialogView.findViewById<ImageView>(R.id.ivMascotaFoto)
        val placeholder = dialogView.findViewById<LinearLayout>(R.id.layoutFotoPlaceholder)

        dialogPhotoView = ivFoto
        dialogPlaceholder = placeholder

        // Pre-cargar foto si existe
        if (selectedPhotoPath.isNotEmpty()) {
            val file = File(selectedPhotoPath)
            if (file.exists()) {
                ivFoto.setImageURI(Uri.fromFile(file))
                ivFoto.visibility = View.VISIBLE
                placeholder.visibility = View.GONE
            }
        }

        framePhoto.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        val razas = listOf(
            "Chihuahua", "Poodle Toy", "Shih Tzu",
            "Beagle", "Cocker Spaniel", "Bulldog Francés",
            "Labrador", "Golden Retriever", "Pastor Alemán",
            "Gran Danés", "Otra"
        )
        spinnerRaza.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, razas
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        mascota?.let {
            etNombre.setText(it.nombre)
            spinnerRaza.setSelection(razas.indexOf(it.raza).coerceAtLeast(0))
            etEdad.setText(it.edadAnos.toString())
            etPeso.setText(it.pesoKg.toString())
            switchActiva.isChecked = it.activa
        }

        AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Editar mascota" else "Nueva mascota")
            .setView(dialogView)
            .setPositiveButton(if (isEdit) "Guardar" else "Agregar") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                if (nombre.isEmpty()) {
                    Toast.makeText(this, "El nombre es requerido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val nueva = Mascota(
                    id = mascota?.id ?: 0,
                    nombre = nombre,
                    raza = spinnerRaza.selectedItem.toString(),
                    edadAnos = etEdad.text.toString().toIntOrNull() ?: 0,
                    pesoKg = etPeso.text.toString().toDoubleOrNull() ?: 0.0,
                    tamano = razaToTamano(spinnerRaza.selectedItem.toString()),
                    activa = switchActiva.isChecked,
                    fotoUri = selectedPhotoPath
                )
                if (isEdit) db.updateMascota(nueva) else db.insertMascota(nueva)
                loadMascotas()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                dialogPhotoView = null
                dialogPlaceholder = null
            }
            .setOnDismissListener {
                dialogPhotoView = null
                dialogPlaceholder = null
            }
            .show()
    }

    private fun confirmDeleteMascota(mascota: Mascota) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar mascota")
            .setMessage("¿Eliminar a ${mascota.nombre}? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                if (mascota.fotoUri.isNotEmpty()) {
                    File(mascota.fotoUri).delete()
                }
                db.deleteMascota(mascota.id)
                loadMascotas()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun razaToTamano(raza: String) = when (raza) {
        "Chihuahua", "Poodle Toy", "Shih Tzu" -> "pequeño"
        "Beagle", "Cocker Spaniel", "Bulldog Francés" -> "mediano"
        "Labrador", "Golden Retriever", "Pastor Alemán" -> "grande"
        "Gran Danés" -> "gigante"
        else -> "mediano"
    }

    private fun copyImageToInternal(sourceUri: Uri): String? {
        return try {
            val fileName = "pet_${System.currentTimeMillis()}.jpg"
            val destFile = File(filesDir, fileName)
            contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    // ── NAVEGACIÓN ───────────────────────────────────────

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_pets
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_pets -> true
                R.id.nav_home -> { navigateTo(Principal::class.java); false }
                R.id.nav_schedule -> { navigateTo(Horarios::class.java); false }
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
