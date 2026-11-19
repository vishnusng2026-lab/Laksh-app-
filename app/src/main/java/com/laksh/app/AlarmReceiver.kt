package com.laksh.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(CallService.EXTRA_ID) ?: return
        val r = Reminders.get(context, id) ?: return
        if (r.completed) return

        val now = System.currentTimeMillis()
        if (now - r.dueAt > Reminders.STALE_MS) {
            Reminders.applyOutcome(context, id, "expired")
            return
        }

        // Never ring over a real phone call or a video call. Try again in a minute.
        val audio = context.getSystemService(AudioManager::class.java)
        val mode = audio.mode
        if (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION) {
            Reminders.postpone(context, id, 60_000L)
            return
        }

        ContextCompat.startForegroundService(
            context,
            Intent(context, CallService::class.java).putExtra(CallService.EXTRA_ID, id),
        )
    }
}
