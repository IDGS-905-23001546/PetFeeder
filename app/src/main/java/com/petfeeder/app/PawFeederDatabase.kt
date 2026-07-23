package com.petfeeder.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Calendar

class PawFeederDatabase(context: Context) :
    SQLiteOpenHelper(context, "pawfeeder.db", null, 4) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS mascotas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                raza TEXT NOT NULL,
                edad_anos INTEGER DEFAULT 0,
                edad_meses INTEGER DEFAULT 0,
                peso_kg REAL DEFAULT 0,
                tamano TEXT DEFAULT 'mediano',
                activa INTEGER DEFAULT 0,
                foto_uri TEXT DEFAULT '',
                created_at INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS dispensaciones (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tipo TEXT NOT NULL,
                nombre TEXT DEFAULT 'Manual',
                porcion_gramos REAL NOT NULL,
                fecha_hora INTEGER NOT NULL,
                estado TEXT DEFAULT 'ejecutada'
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS dispensadores (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                codigo_unico TEXT NOT NULL,
                firmware_version TEXT DEFAULT 'v1.0.0',
                estado TEXT DEFAULT 'offline',
                bateria_percent INTEGER DEFAULT 100,
                nivel_tolva_pct INTEGER DEFAULT 60,
                ssid_wifi TEXT DEFAULT '',
                activo INTEGER DEFAULT 1,
                created_at INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS horarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                icono TEXT DEFAULT 'sun',
                hora TEXT NOT NULL,
                lunes INTEGER DEFAULT 0,
                martes INTEGER DEFAULT 0,
                miercoles INTEGER DEFAULT 0,
                jueves INTEGER DEFAULT 0,
                viernes INTEGER DEFAULT 0,
                sabado INTEGER DEFAULT 0,
                domingo INTEGER DEFAULT 0,
                porcion_gramos REAL DEFAULT 100,
                activo INTEGER DEFAULT 1,
                created_at INTEGER
            )
        """.trimIndent())

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE mascotas ADD COLUMN foto_uri TEXT DEFAULT ''")
            } catch (_: Exception) {}
        }
        if (oldVersion < 3) {
            // Migración v2 -> v3: solo quedaba agua pero ya no se usa
        }
        if (oldVersion < 4) {
            // Migración v3 -> v4: edad en meses para el cálculo de porciones
            try {
                db.execSQL("ALTER TABLE mascotas ADD COLUMN edad_meses INTEGER DEFAULT 0")
                // Backfill: quien tenía edad en años se convierte a meses
                db.execSQL("UPDATE mascotas SET edad_meses = edad_anos * 12 WHERE edad_meses = 0")
            } catch (_: Exception) {}
        }
    }

    // ── MASCOTAS ──────────────────────────────────────────

    fun getAllMascotas(): List<Mascota> {
        val list = mutableListOf<Mascota>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM mascotas ORDER BY activa DESC, created_at DESC", null
        )
        with(cursor) {
            while (moveToNext()) {
                list.add(
                    Mascota(
                        id = getInt(getColumnIndexOrThrow("id")),
                        nombre = getString(getColumnIndexOrThrow("nombre")),
                        raza = getString(getColumnIndexOrThrow("raza")),
                        edadAnos = getInt(getColumnIndexOrThrow("edad_anos")),
                        edadMeses = getInt(getColumnIndexOrThrow("edad_meses")),
                        pesoKg = getDouble(getColumnIndexOrThrow("peso_kg")),
                        tamano = getString(getColumnIndexOrThrow("tamano")),
                        activa = getInt(getColumnIndexOrThrow("activa")) == 1,
                        fotoUri = getString(getColumnIndexOrThrow("foto_uri")) ?: ""
                    )
                )
            }
            close()
        }
        return list
    }

    fun getActivaMascota(): Mascota? {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM mascotas WHERE activa = 1 LIMIT 1", null
        )
        return if (cursor.moveToFirst()) {
            Mascota(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                raza = cursor.getString(cursor.getColumnIndexOrThrow("raza")),
                edadAnos = cursor.getInt(cursor.getColumnIndexOrThrow("edad_anos")),
                edadMeses = cursor.getInt(cursor.getColumnIndexOrThrow("edad_meses")),
                pesoKg = cursor.getDouble(cursor.getColumnIndexOrThrow("peso_kg")),
                tamano = cursor.getString(cursor.getColumnIndexOrThrow("tamano")),
                activa = true,
                fotoUri = cursor.getString(cursor.getColumnIndexOrThrow("foto_uri")) ?: ""
            ).also { cursor.close() }
        } else {
            cursor.close()
            null
        }
    }

    fun insertMascota(m: Mascota): Long {
        if (m.activa) writableDatabase.execSQL("UPDATE mascotas SET activa = 0")
        return writableDatabase.insert("mascotas", null, mascotaToValues(m))
    }

    fun updateMascota(m: Mascota) {
        if (m.activa) {
            writableDatabase.execSQL("UPDATE mascotas SET activa = 0 WHERE id != ${m.id}")
        }
        writableDatabase.update("mascotas", mascotaToValues(m), "id = ?", arrayOf(m.id.toString()))
    }

    fun deleteMascota(id: Int) {
        writableDatabase.delete("mascotas", "id = ?", arrayOf(id.toString()))
    }

    /**
     * Reemplaza la caché local de mascotas con la lista que viene del servidor.
     * Conserva los IDs del servidor (para poder editar/borrar contra la API).
     */
    fun replaceAllMascotas(list: List<Mascota>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("mascotas", null, null)
            for (m in list) {
                val v = mascotaToValues(m).apply { put("id", m.id) }
                db.insert("mascotas", null, v)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun mascotaToValues(m: Mascota) = ContentValues().apply {
        put("nombre", m.nombre)
        put("raza", m.raza)
        put("edad_anos", m.edadAnos)
        put("edad_meses", m.edadMeses)
        put("peso_kg", m.pesoKg)
        put("tamano", m.tamano)
        put("activa", if (m.activa) 1 else 0)
        put("foto_uri", m.fotoUri)
        put("created_at", System.currentTimeMillis())
    }

    // ── HORARIOS ──────────────────────────────────────────

    fun getAllHorarios(): List<Horario> {
        val list = mutableListOf<Horario>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM horarios ORDER BY hora ASC", null
        )
        with(cursor) {
            while (moveToNext()) {
                list.add(
                    Horario(
                        id = getInt(getColumnIndexOrThrow("id")),
                        nombre = getString(getColumnIndexOrThrow("nombre")),
                        icono = getString(getColumnIndexOrThrow("icono")),
                        hora = getString(getColumnIndexOrThrow("hora")),
                        lunes = getInt(getColumnIndexOrThrow("lunes")) == 1,
                        martes = getInt(getColumnIndexOrThrow("martes")) == 1,
                        miercoles = getInt(getColumnIndexOrThrow("miercoles")) == 1,
                        jueves = getInt(getColumnIndexOrThrow("jueves")) == 1,
                        viernes = getInt(getColumnIndexOrThrow("viernes")) == 1,
                        sabado = getInt(getColumnIndexOrThrow("sabado")) == 1,
                        domingo = getInt(getColumnIndexOrThrow("domingo")) == 1,
                        porcionGramos = getDouble(getColumnIndexOrThrow("porcion_gramos")),
                        activo = getInt(getColumnIndexOrThrow("activo")) == 1
                    )
                )
            }
            close()
        }
        return list
    }

    fun insertHorario(h: Horario): Long =
        writableDatabase.insert("horarios", null, horarioToValues(h))

    /** Reemplaza la caché local de horarios con la lista del servidor (IDs del servidor). */
    fun replaceAllHorarios(list: List<Horario>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("horarios", null, null)
            for (h in list) {
                val v = horarioToValues(h).apply { put("id", h.id) }
                db.insert("horarios", null, v)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateHorarioActivo(id: Int, activo: Boolean) {
        writableDatabase.update(
            "horarios",
            ContentValues().apply { put("activo", if (activo) 1 else 0) },
            "id = ?",
            arrayOf(id.toString())
        )
    }

    fun deleteHorario(id: Int) {
        writableDatabase.delete("horarios", "id = ?", arrayOf(id.toString()))
    }

    // ── DISPENSACIONES ────────────────────────────────────

    fun insertDispensacion(d: Dispensacion): Long {
        val values = ContentValues().apply {
            put("tipo", d.tipo)
            put("nombre", d.nombre)
            put("porcion_gramos", d.porcionGramos)
            put("fecha_hora", d.fechaHora)
            put("estado", d.estado)
        }
        return writableDatabase.insert("dispensaciones", null, values)
    }

    fun getAllDispensaciones(): List<Dispensacion> {
        val list = mutableListOf<Dispensacion>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM dispensaciones ORDER BY fecha_hora DESC LIMIT 100", null
        )
        with(cursor) {
            while (moveToNext()) {
                list.add(Dispensacion(
                    id = getInt(getColumnIndexOrThrow("id")),
                    tipo = getString(getColumnIndexOrThrow("tipo")),
                    nombre = getString(getColumnIndexOrThrow("nombre")),
                    porcionGramos = getDouble(getColumnIndexOrThrow("porcion_gramos")),
                    fechaHora = getLong(getColumnIndexOrThrow("fecha_hora")),
                    estado = getString(getColumnIndexOrThrow("estado"))
                ))
            }
            close()
        }
        return list
    }

    fun getTodayDispensaciones(): List<Dispensacion> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val list = mutableListOf<Dispensacion>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM dispensaciones WHERE fecha_hora >= ? ORDER BY fecha_hora DESC",
            arrayOf(startOfDay.toString())
        )
        with(cursor) {
            while (moveToNext()) {
                list.add(Dispensacion(
                    id = getInt(getColumnIndexOrThrow("id")),
                    tipo = getString(getColumnIndexOrThrow("tipo")),
                    nombre = getString(getColumnIndexOrThrow("nombre")),
                    porcionGramos = getDouble(getColumnIndexOrThrow("porcion_gramos")),
                    fechaHora = getLong(getColumnIndexOrThrow("fecha_hora")),
                    estado = getString(getColumnIndexOrThrow("estado"))
                ))
            }
            close()
        }
        return list
    }

    // ── DISPENSADORES ─────────────────────────────────────

    fun getDispensador(): Dispensador? {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM dispensadores WHERE activo = 1 LIMIT 1", null
        )
        return if (cursor.moveToFirst()) {
            Dispensador(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                codigoUnico = cursor.getString(cursor.getColumnIndexOrThrow("codigo_unico")),
                firmwareVersion = cursor.getString(cursor.getColumnIndexOrThrow("firmware_version")),
                estado = cursor.getString(cursor.getColumnIndexOrThrow("estado")),
                bateriaPercent = cursor.getInt(cursor.getColumnIndexOrThrow("bateria_percent")),
                nivelTolvaPct = cursor.getInt(cursor.getColumnIndexOrThrow("nivel_tolva_pct")),
                ssidWifi = cursor.getString(cursor.getColumnIndexOrThrow("ssid_wifi"))
            ).also { cursor.close() }
        } else {
            cursor.close()
            null
        }
    }

    fun insertDispensador(d: Dispensador): Long {
        writableDatabase.execSQL("UPDATE dispensadores SET activo = 0")
        return writableDatabase.insert("dispensadores", null, dispensadorToValues(d))
    }

    fun updateDispensadorEstado(id: Int, estado: String) {
        writableDatabase.update(
            "dispensadores",
            ContentValues().apply { put("estado", estado) },
            "id = ?", arrayOf(id.toString())
        )
    }

    fun deleteDispensador(id: Int) {
        writableDatabase.delete("dispensadores", "id = ?", arrayOf(id.toString()))
    }

    private fun dispensadorToValues(d: Dispensador) = ContentValues().apply {
        put("nombre", d.nombre)
        put("codigo_unico", d.codigoUnico)
        put("firmware_version", d.firmwareVersion)
        put("estado", d.estado)
        put("bateria_percent", d.bateriaPercent)
        put("nivel_tolva_pct", d.nivelTolvaPct)
        put("ssid_wifi", d.ssidWifi)
        put("activo", 1)
        put("created_at", System.currentTimeMillis())
    }

    private fun horarioToValues(h: Horario) = ContentValues().apply {
        put("nombre", h.nombre)
        put("icono", h.icono)
        put("hora", h.hora)
        put("lunes", if (h.lunes) 1 else 0)
        put("martes", if (h.martes) 1 else 0)
        put("miercoles", if (h.miercoles) 1 else 0)
        put("jueves", if (h.jueves) 1 else 0)
        put("viernes", if (h.viernes) 1 else 0)
        put("sabado", if (h.sabado) 1 else 0)
        put("domingo", if (h.domingo) 1 else 0)
        put("porcion_gramos", h.porcionGramos)
        put("activo", if (h.activo) 1 else 0)
        put("created_at", System.currentTimeMillis())
    }
}
