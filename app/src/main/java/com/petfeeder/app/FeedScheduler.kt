package com.petfeeder.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar
import java.util.TimeZone

/**
 * Programa las alarmas para que el dispensador suelte comida a la hora
 * de cada horario. Por cada horario activo y cada día marcado, crea una alarma
 * exacta que dispara FeedAlarmReceiver.
 *
 * Se debe llamar rescheduleAll() cada vez que cambian los horarios (crear,
 * borrar, activar/desactivar) y al iniciar la app.
 */
object FeedScheduler {

    /** Zona horaria del proyecto: León, Guanajuato (México), UTC-6 sin DST. */
    private val ZONA_MEXICO: TimeZone = TimeZone.getTimeZone("America/Mexico_City")

    const val EXTRA_TIPO = "tipo"        // "comida"
    const val EXTRA_ID = "horarioId"
    const val EXTRA_NOMBRE = "nombre"
    const val EXTRA_CANTIDAD = "cantidad"
    const val EXTRA_REQCODE = "reqCode"
    const val EXTRA_TRIGGER = "triggerAt"

    // Días de la semana -> constante de Calendar
    private val DIA_A_CALENDAR = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )

    fun rescheduleAll(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val db = PawFeederDatabase(ctx)

        // Horarios de COMIDA
        db.getAllHorarios().filter { it.activo }.forEach { h ->
            val dias = listOf(h.lunes, h.martes, h.miercoles, h.jueves, h.viernes, h.sabado, h.domingo)
            dias.forEachIndexed { idx, activo ->
                if (activo) scheduleOne(ctx, am, "comida", h.id, h.nombre,
                    h.porcionGramos.toInt(), h.hora, idx)
            }
        }
    }

    private fun scheduleOne(
        ctx: Context, am: AlarmManager, tipo: String, id: Int,
        nombre: String, cantidad: Int, hora12h: String, diaIndex: Int
    ) {
        val (hour, minute) = parseHora(hora12h) ?: return
        val triggerAt = nextTrigger(DIA_A_CALENDAR[diaIndex], hour, minute)
        // Código único por id + día
        val reqCode = 1_000_000 + id * 10 + diaIndex

        val pi = buildPendingIntent(ctx, tipo, id, nombre, cantidad, reqCode, triggerAt)
        setExact(am, triggerAt, pi)
    }

    /** Reconstruye el PendingIntent con los mismos datos (usado por el receiver). */
    fun buildPendingIntent(
        ctx: Context, tipo: String, id: Int, nombre: String, cantidad: Int,
        reqCode: Int, triggerAt: Long
    ): PendingIntent {
        val intent = Intent(ctx, FeedAlarmReceiver::class.java).apply {
            putExtra(EXTRA_TIPO, tipo)
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_NOMBRE, nombre)
            putExtra(EXTRA_CANTIDAD, cantidad)
            putExtra(EXTRA_REQCODE, reqCode)
            putExtra(EXTRA_TRIGGER, triggerAt)
        }
        return PendingIntent.getBroadcast(
            ctx, reqCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun setExact(am: AlarmManager, triggerAt: Long, pi: PendingIntent) {
        try {
            // En API 31+ puede requerir permiso de alarma exacta; si no, usamos inexacta
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /** Próxima fecha/hora futura que cae en ese día de la semana (hora de México). */
    private fun nextTrigger(calendarDay: Int, hour: Int, minute: Int): Long {
        val now = Calendar.getInstance(ZONA_MEXICO)
        val target = Calendar.getInstance(ZONA_MEXICO).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Avanzar hasta el día de la semana correcto
        while (target.get(Calendar.DAY_OF_WEEK) != calendarDay || target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    /** Convierte "07:30 AM" / "06:00 PM" a (hora24, minuto). */
    private fun parseHora(hora12h: String): Pair<Int, Int>? {
        return try {
            val parts = hora12h.trim().split(" ")
            val hm = parts[0].split(":")
            var h = hm[0].toInt()
            val m = hm[1].toInt()
            val amPm = parts.getOrNull(1)?.uppercase() ?: "AM"
            if (amPm == "PM" && h != 12) h += 12
            if (amPm == "AM" && h == 12) h = 0
            Pair(h, m)
        } catch (e: Exception) {
            null
        }
    }
}
