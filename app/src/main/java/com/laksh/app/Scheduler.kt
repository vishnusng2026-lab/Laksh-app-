package com.laksh.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Uses setAlarmClock, the alarm type Android treats like a clock-app alarm.
 * It fires on time in Doze and needs no exact-alarm permission.
 */
object Scheduler {
    const val ACTION_FIRE = "com.laksh.app.FIRE"

    private fun operation(ctx: Context, id: String): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .setData(Uri.parse("laksh://reminder/$id"))
            .putExtra(CallService.EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            ctx.applicationContext,
            0,
            i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun schedule(ctx: Context, r: Reminder) {
        val app = ctx.applicationContext
        val am = app.getSystemService(AlarmManager::class.java)
        val show = PendingIntent.getActivity(
            app,
            0,
            Intent(app, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(r.dueAt, show), operation(app, r.id))
    }

    fun cancel(ctx: Context, id: String) {
        val app = ctx.applicationContext
        app.getSystemService(AlarmManager::class.java).cancel(operation(app, id))
    }
}
