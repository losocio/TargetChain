package com.targetchain.targetchain.alarm

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Next moment, strictly after [now], at which the local time of day equals [minutesOfDay].
 * If that time does not exist on a given day (DST gap), it is shifted forward by the gap length.
 */
fun nextTriggerTime(now: ZonedDateTime, minutesOfDay: Int): ZonedDateTime {
    require(minutesOfDay in 0 until MINUTES_PER_DAY) { "minutesOfDay out of range: $minutesOfDay" }
    val time = LocalTime.of(minutesOfDay / 60, minutesOfDay % 60)
    val today = ZonedDateTime.of(now.toLocalDate(), time, now.zone)
    return if (today.isAfter(now)) {
        today
    } else {
        ZonedDateTime.of(now.toLocalDate().plusDays(1), time, now.zone)
    }
}

const val MINUTES_PER_DAY = 24 * 60
