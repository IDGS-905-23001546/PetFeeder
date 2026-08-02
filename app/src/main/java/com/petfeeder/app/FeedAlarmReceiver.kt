package com.petfeeder.app

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Se dispara a la hora de un horario. Manda la orden al ESP32 (si hay IP),
 * registra la dispensación en la BD local, y reprograma la MISMA alarma para
 * la próxima semana (para que el horario se repita cada semana).
 */
class FeedAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val tipo = intent.getStringExtra(FeedScheduler.EXTRA_TIPO) ?: "comida"
        val id = intent.getIntExtra(FeedScheduler.EXTRA_ID, -1)
        val nombre = intent.getStringExtra(FeedScheduler.EXTRA_NOMBRE) ?: "Programado"
        val cantidad = intent.getIntExtra(FeedScheduler.EXTRA_CANTIDAD, 0)
        val reqCode = intent.getIntExtra(FeedScheduler.EXTRA_REQCODE, 0)
        val triggerAt = intent.getLongExtra(FeedScheduler.EXTRA_TRIGGER, 0L)

        val appCtx = context.applicationContext
        val pending = goAsync()

        // El trabajo de red debe salir del hilo principal
        Thread {
            try {
                val db = PawFeederDatabase(appCtx)

                // 0. ¿El horario sigue existiendo y activo? Si no, no dispenses ni
                //    reprogrames (así muere la alarma de un horario borrado/apagado)
                val sigueActivo = db.getAllHorarios().any { it.id == id && it.activo }
                if (!sigueActivo) {
                    return@Thread   // el finally hace pending.finish()
                }

                // 1. Mandar la orden al dispositivo (si está configurado)
                var estado = "ejecutada"
                if (DeviceConfig.hasIp(appCtx)) {
                    val path = "/dispense"
                    val r = DeviceClient.postDispense(DeviceConfig.baseUrl(appCtx), path, cantidad)
                    if (!r.exito) estado = "fallida"
                }

                // 2. Registrar la dispensación programada en la BD local
                db.insertDispensacion(
                    Dispensacion(
                        tipo = "programada",
                        nombre = nombre,
                        porcionGramos = cantidad.toDouble(),
                        estado = estado
                    )
                )

                // 3. Subir la dispensación al servidor (best-effort) para que
                //    la web y el historial en la nube la vean reflejada.
                try {
                    kotlinx.coroutines.runBlocking {
                        RetrofitClient.api.crearDispensacion(
                            DispensacionApi(
                                usuarioId = UserSession.getId(appCtx),
                                horarioId = id.takeIf { it > 0 },
                                tipo = "programada",
                                nombre = nombre,
                                porcionGramos = cantidad.toDouble(),
                                estado = estado
                            )
                        )
                    }
                } catch (_: Exception) {}

                // 4. Reprogramar la misma alarma para dentro de 7 días
                val proximo = triggerAt + AlarmManager.INTERVAL_DAY * 7
                val am = appCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val pi = FeedScheduler.buildPendingIntent(appCtx, tipo, id, nombre, cantidad, reqCode, proximo)
                FeedScheduler.setExact(am, proximo, pi)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
