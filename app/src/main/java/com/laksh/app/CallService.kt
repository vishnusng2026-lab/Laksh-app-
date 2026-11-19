package com.laksh.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Runs while a reminder call is ringing or being answered.
 * It rings, vibrates, shows the full-screen call notification and records the outcome.
 */
class CallService : Service() {
    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_OUTCOME = "outcome"
        const val ACTION_ANSWER = "com.laksh.app.ANSWER"
        const val ACTION_END = "com.laksh.app.END"
        const val ACTION_ENDED = "com.laksh.app.CALL_ENDED"
        private const val CHANNEL = "laksh_call"
        private const val NOTIFICATION_ID = 4201
        private const val RING_MS = 45_000L
        private const val ANSWERED_MAX_MS = 10 * 60_000L

        @Volatile
        var activeId: String? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ANSWER -> {
                stopRinging()
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({ end("done") }, ANSWERED_MAX_MS)
                return START_NOT_STICKY
            }
            ACTION_END -> {
                end(intent.getStringExtra(EXTRA_OUTCOME) ?: "missed")
                return START_NOT_STICKY
            }
        }

        val id = intent?.getStringExtra(EXTRA_ID)
        val reminder = id?.let { Reminders.get(this, it) }

        // Already on a call: try this one again shortly without counting it as missed.
        if (id != null && activeId != null && activeId != id) {
            Reminders.postpone(this, id, 60_000L)
            return START_NOT_STICKY
        }

        ensureChannel()
        val notification = buildNotification(reminder)
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
            )
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (reminder == null || reminder.completed) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        activeId = reminder.id
        acquireWake()
        startRinging()
        handler.postDelayed({ end("missed") }, RING_MS)

        // Opens the call screen straight away when Android allows it.
        // Otherwise the full-screen notification does the same job.
        try {
            startActivity(callIntent(reminder.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            // Notification fallback is already posted.
        }
        return START_NOT_STICKY
    }

    private fun callIntent(id: String) =
        Intent(this, CallActivity::class.java)
            .putExtra(EXTRA_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    private fun buildNotification(r: Reminder?): Notification {
        val full = PendingIntent.getActivity(
            this, 1, callIntent(r?.id ?: ""),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val decline = PendingIntent.getService(
            this, 2,
            Intent(this, CallService::class.java)
                .setAction(ACTION_END)
                .putExtra(EXTRA_OUTCOME, "declined"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle(r?.caller ?: "Laksh")
            .setContentText("Incoming reminder call")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(full, true)
            .setContentIntent(full)
            .addAction(0, "Decline", decline)
            .addAction(0, "Answer", full)
            .build()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            val ch = NotificationChannel(CHANNEL, "Reminder calls", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Full-screen incoming call alerts for your reminders"
            ch.setSound(null, null)
            ch.enableVibration(false)
            ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            nm.createNotificationChannel(ch)
        }
    }

    private fun acquireWake() {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "laksh:call").apply {
            acquire(ANSWERED_MAX_MS + 30_000L)
        }
    }

    private fun startRinging() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= 28) isLooping = true
                play()
            }
        } catch (e: Exception) {
            // Vibration below still alerts the person.
        }
        try {
            vibrator = if (Build.VERSION.SDK_INT >= 31) {
                getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 700), 0))
        } catch (e: Exception) {
            // Some devices have no vibrator.
        }
    }

    private fun stopRinging() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
        }
        ringtone = null
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
        }
    }

    private fun end(outcome: String) {
        val id = activeId
        handler.removeCallbacksAndMessages(null)
        stopRinging()
        if (id != null) Reminders.applyOutcome(this, id, outcome)
        activeId = null
        sendBroadcast(Intent(ACTION_ENDED).setPackage(packageName))
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopRinging()
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {
        }
        activeId = null
        super.onDestroy()
    }
}
