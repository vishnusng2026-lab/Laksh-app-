package com.laksh.app

import org.json.JSONObject

enum class Freq { ONCE, DAILY, WEEKLY }

data class Reminder(
    val id: String,
    val caller: String,
    val task: String,
    val hour: Int,
    val minute: Int,
    val freq: Freq,
    val dateMillis: Long,
    val dow: Int,
    val speak: Boolean,
    val audioPath: String?,
    val completed: Boolean,
    val nextAt: Long,
    val snoozeUntil: Long,
    val tries: Int,
    val last: String,
    val created: Long,
    val testOnly: Boolean = false,
) {
    /** The moment the phone should ring next. */
    val dueAt: Long get() = if (snoozeUntil > 0L) snoozeUntil else nextAt

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("caller", caller)
        put("task", task)
        put("hour", hour)
        put("minute", minute)
        put("freq", freq.name)
        put("dateMillis", dateMillis)
        put("dow", dow)
        put("speak", speak)
        put("audio", audioPath ?: "")
        put("completed", completed)
        put("nextAt", nextAt)
        put("snoozeUntil", snoozeUntil)
        put("tries", tries)
        put("last", last)
        put("created", created)
        put("testOnly", testOnly)
    }

    companion object {
        fun fromJson(o: JSONObject): Reminder = Reminder(
            id = o.getString("id"),
            caller = o.optString("caller", "Reminder"),
            task = o.optString("task", ""),
            hour = o.optInt("hour", 9),
            minute = o.optInt("minute", 0),
            freq = try {
                Freq.valueOf(o.optString("freq", "ONCE"))
            } catch (e: Exception) {
                Freq.ONCE
            },
            dateMillis = o.optLong("dateMillis", 0L),
            dow = o.optInt("dow", 2),
            speak = o.optBoolean("speak", true),
            audioPath = o.optString("audio", "").ifEmpty { null },
            completed = o.optBoolean("completed", false),
            nextAt = o.optLong("nextAt", 0L),
            snoozeUntil = o.optLong("snoozeUntil", 0L),
            tries = o.optInt("tries", 0),
            last = o.optString("last", ""),
            created = o.optLong("created", 0L),
            testOnly = o.optBoolean("testOnly", false),
        )
    }
}
