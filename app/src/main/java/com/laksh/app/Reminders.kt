package com.laksh.app

import android.content.Context
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Offline storage and rules for reminders. Everything lives in private app storage. */
object Reminders {
    private const val PREFS = "laksh"
    private const val KEY = "items"
    const val SNOOZE_MS = 5 * 60_000L
    const val STALE_MS = 15 * 60_000L

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun all(ctx: Context): List<Reminder> {
        val raw = prefs(ctx).getString(KEY, null) ?: return emptyList()
        return try {
            val a = JSONArray(raw)
            (0 until a.length()).map { Reminder.fromJson(a.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    private fun saveAll(ctx: Context, list: List<Reminder>) {
        val a = JSONArray()
        list.forEach { a.put(it.toJson()) }
        prefs(ctx).edit().putString(KEY, a.toString()).apply()
    }

    fun get(ctx: Context, id: String): Reminder? = all(ctx).firstOrNull { it.id == id }

    @Synchronized
    fun upsert(ctx: Context, r: Reminder) {
        val list = all(ctx).toMutableList()
        val i = list.indexOfFirst { it.id == r.id }
        if (i >= 0) list[i] = r else list.add(r)
        saveAll(ctx, list)
    }

    /** Saves a reminder and sets its real alarm. */
    fun add(ctx: Context, r: Reminder) {
        upsert(ctx, r)
        if (!r.completed) Scheduler.schedule(ctx, r)
    }

    @Synchronized
    fun delete(ctx: Context, id: String, removeVoiceFile: Boolean = true) {
        val r = get(ctx, id) ?: return
        Scheduler.cancel(ctx, id)
        saveAll(ctx, all(ctx).filter { it.id != id })
        if (removeVoiceFile) r.audioPath?.let { runCatching { File(it).delete() } }
    }

    fun cleanupOrphanedAudio(ctx: Context) {
        val activePaths = all(ctx).mapNotNull { it.audioPath }.toSet()
        ctx.filesDir.listFiles { f -> f.name.startsWith("voice_") && f.name.endsWith(".m4a") }
            ?.forEach { f ->
                if (f.absolutePath !in activePaths) {
                    runCatching { f.delete() }
                }
            }
    }

    fun newReminder(
        caller: String,
        task: String,
        hour: Int,
        minute: Int,
        freq: Freq,
        dateMillis: Long,
        dow: Int,
        speak: Boolean,
        audioPath: String?,
    ): Reminder {
        val base = Reminder(
            id = UUID.randomUUID().toString(),
            caller = caller.ifBlank { "Reminder" },
            task = task,
            hour = hour,
            minute = minute,
            freq = freq,
            dateMillis = dateMillis,
            dow = dow,
            speak = speak,
            audioPath = audioPath,
            completed = false,
            nextAt = 0L,
            snoozeUntil = 0L,
            tries = 0,
            last = "",
            created = System.currentTimeMillis(),
            testOnly = false,
        )
        return base.copy(nextAt = computeNext(base, System.currentTimeMillis()))
    }

    /** First time after [from] that this reminder should ring. */
    fun computeNext(r: Reminder, from: Long): Long {
        fun at(base: Long, plusDays: Int): Calendar = Calendar.getInstance().apply {
            timeInMillis = base
            add(Calendar.DAY_OF_YEAR, plusDays)
            set(Calendar.HOUR_OF_DAY, r.hour)
            set(Calendar.MINUTE, r.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return when (r.freq) {
            Freq.ONCE -> at(r.dateMillis, 0).timeInMillis
            Freq.DAILY -> {
                val c = at(from, 0)
                if (c.timeInMillis <= from) c.add(Calendar.DAY_OF_YEAR, 1)
                c.timeInMillis
            }
            Freq.WEEKLY -> {
                var result = from + 7 * 86_400_000L
                for (i in 0..7) {
                    val c = at(from, i)
                    if (c.get(Calendar.DAY_OF_WEEK) == r.dow && c.timeInMillis > from) {
                        result = c.timeInMillis
                        break
                    }
                }
                result
            }
        }
    }

    /** Rings the reminder again in 5 seconds, for trying it out. */
    fun testIn5s(ctx: Context, id: String) {
        val r = get(ctx, id) ?: return
        if (r.completed) return
        val u = r.copy(snoozeUntil = System.currentTimeMillis() + 5_000L, tries = 0, testOnly = true)
        upsert(ctx, u)
        Scheduler.schedule(ctx, u)
    }

    /** Moves a reminder a little later without counting it as missed. */
    fun postpone(ctx: Context, id: String, ms: Long) {
        val r = get(ctx, id) ?: return
        val u = r.copy(snoozeUntil = System.currentTimeMillis() + ms, testOnly = false)
        upsert(ctx, u)
        Scheduler.schedule(ctx, u)
    }

    private fun finish(r: Reminder, note: String): Reminder {
        val now = System.currentTimeMillis()
        val wasRealCall = r.nextAt <= now
        if (!wasRealCall) {
            return r.copy(snoozeUntil = 0L, tries = 0, testOnly = false, last = note)
        }
        return if (r.freq == Freq.ONCE) {
            r.copy(completed = true, snoozeUntil = 0L, tries = 0, testOnly = false, last = note)
        } else {
            r.copy(nextAt = computeNext(r, now), snoozeUntil = 0L, tries = 0, testOnly = false, last = note)
        }
    }

    /** outcome: done, snooze, declined, missed or expired. */
    fun applyOutcome(ctx: Context, id: String, outcome: String) {
        val r = get(ctx, id) ?: return
        val now = System.currentTimeMillis()
        val t = Fmt.time(now)

        if (r.testOnly) {
            val note = when (outcome) {
                "done" -> "Test answered at $t"
                "snooze" -> "Test snoozed at $t"
                "declined" -> "Test declined at $t"
                "expired" -> "Test expired at $t"
                else -> "Test missed at $t"
            }
            val u = r.copy(snoozeUntil = 0L, tries = 0, testOnly = false, last = note)
            upsert(ctx, u)
            if (u.completed) Scheduler.cancel(ctx, u.id) else Scheduler.schedule(ctx, u)
            return
        }

        val u = when (outcome) {
            "done" -> finish(r, "Answered at $t")
            "snooze" -> r.copy(snoozeUntil = now + SNOOZE_MS, testOnly = false, last = "Snoozed at $t")
            "expired" -> finish(r, "Missed while the phone was off")
            else -> {
                val tries = r.tries + 1
                if (tries > 3) {
                    finish(r, "Missed 3 calls")
                } else {
                    r.copy(
                        tries = tries,
                        snoozeUntil = now + SNOOZE_MS,
                        testOnly = false,
                        last = if (outcome == "declined") "Declined at $t" else "Missed at $t",
                    )
                }
            }
        }
        upsert(ctx, u)
        if (u.completed) Scheduler.cancel(ctx, u.id) else Scheduler.schedule(ctx, u)
    }

    /** Called after boot, app update or clock change. */
    fun rescheduleAll(ctx: Context) {
        val now = System.currentTimeMillis()
        for (r in all(ctx)) {
            if (r.completed) continue
            when {
                r.dueAt < now - STALE_MS -> applyOutcome(ctx, r.id, "expired")
                r.dueAt < now -> postpone(ctx, r.id, 3_000L)
                else -> Scheduler.schedule(ctx, r)
            }
        }
    }
}

object Fmt {
    fun time(ms: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
    fun day(ms: Long): String = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(ms))

    fun clock(hour: Int, minute: Int): String {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return time(c.timeInMillis)
    }

    fun initials(name: String): String {
        val w = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (w.isEmpty()) return "?"
        val first = w.first().first().uppercaseChar()
        return if (w.size > 1) "$first${w.last().first().uppercaseChar()}" else "$first"
    }

    private val dayNames = mapOf(
        Calendar.SUNDAY to "Sunday", Calendar.MONDAY to "Monday", Calendar.TUESDAY to "Tuesday",
        Calendar.WEDNESDAY to "Wednesday", Calendar.THURSDAY to "Thursday",
        Calendar.FRIDAY to "Friday", Calendar.SATURDAY to "Saturday",
    )

    fun freq(r: Reminder): String = when (r.freq) {
        Freq.ONCE -> day(r.dateMillis)
        Freq.DAILY -> "Every day"
        Freq.WEEKLY -> "Every ${dayNames[r.dow] ?: "week"}"
    }

    fun eta(r: Reminder, now: Long): String {
        val diff = r.dueAt - now
        val pre = if (r.snoozeUntil > 0L) "Rings again " else "Next call "
        if (diff <= 0) return "Ringing now"
        if (diff < 60_000L) return pre + "in under a minute"
        val mins = diff / 60_000L
        if (mins < 60) return pre + "in $mins min"
        if (diff < 86_400_000L) return pre + "in ${mins / 60}h ${mins % 60}m"
        return pre + day(r.dueAt) + ", " + time(r.dueAt)
    }
}
