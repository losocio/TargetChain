package com.targetchain.targetchain.chain

/** A task's duration: the countdown that starts once its challenges are completed. */
const val MIN_TASK_DURATION_SECONDS = 1
const val MAX_TASK_DURATION_SECONDS = 12 * 60 * 60

fun isValidTaskDuration(seconds: Int?): Boolean =
    seconds != null && seconds in MIN_TASK_DURATION_SECONDS..MAX_TASK_DURATION_SECONDS

data class HoursMinutesSeconds(val hours: Int, val minutes: Int, val seconds: Int)

fun splitDuration(totalSeconds: Long): HoursMinutesSeconds =
    HoursMinutesSeconds(
        hours = (totalSeconds / 3600).toInt(),
        minutes = (totalSeconds % 3600 / 60).toInt(),
        seconds = (totalSeconds % 60).toInt(),
    )

/**
 * Duration typed in the H : M : S fields, in seconds. Empty fields count as 0. Null when a field
 * isn't a number, minutes or seconds are over 59, or the total isn't a valid task duration.
 */
fun parseDurationFields(hours: String, minutes: String, seconds: String): Int? {
    fun field(text: String): Int? = if (text.isBlank()) 0 else text.trim().toIntOrNull()
    val h = field(hours) ?: return null
    val m = field(minutes) ?: return null
    val s = field(seconds) ?: return null
    if (h < 0 || m !in 0..59 || s !in 0..59) return null
    val total = h.toLong() * 3600 + m * 60 + s
    return total.toInt().takeIf { total <= Int.MAX_VALUE && isValidTaskDuration(it) }
}

/** Countdown text: "1:02:03" with hours, "02:03" without. Negative values show as 0. */
fun formatCountdown(totalSeconds: Long): String {
    val (h, m, s) = splitDuration(totalSeconds.coerceAtLeast(0))
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
