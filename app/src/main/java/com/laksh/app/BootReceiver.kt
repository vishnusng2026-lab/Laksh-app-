package com.laksh.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Puts every alarm back after a reboot, app update or clock change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.rescheduleAll(context)
    }
}
