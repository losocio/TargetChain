package com.targetchain.targetchain.alarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class NextTriggerTest {

    private val madrid = ZoneId.of("Europe/Madrid")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0) =
        ZonedDateTime.of(year, month, day, hour, minute, second, 0, madrid)

    @Test
    fun laterTimeToday_triggersToday() {
        val now = at(2026, 10, 21, 6, 30)
        assertEquals(at(2026, 10, 21, 7, 0), nextTriggerTime(now, 7 * 60))
    }

    @Test
    fun earlierTimeToday_triggersTomorrow() {
        val now = at(2026, 10, 21, 8, 0)
        assertEquals(at(2026, 10, 22, 7, 0), nextTriggerTime(now, 7 * 60))
    }

    @Test
    fun sameMinuteAsNow_triggersTomorrow() {
        val now = at(2026, 10, 21, 7, 0, 30)
        assertEquals(at(2026, 10, 22, 7, 0), nextTriggerTime(now, 7 * 60))
    }

    @Test
    fun exactlyNow_triggersTomorrow() {
        val now = at(2026, 10, 21, 7, 0)
        assertEquals(at(2026, 10, 22, 7, 0), nextTriggerTime(now, 7 * 60))
    }

    @Test
    fun midnight_triggersNextDay() {
        val now = at(2026, 10, 21, 23, 59)
        assertEquals(at(2026, 10, 22, 0, 0), nextTriggerTime(now, 0))
    }

    @Test
    fun dstGap_shiftsForward() {
        // 2026-03-29 in Madrid: clocks jump from 02:00 to 03:00.
        val now = at(2026, 3, 29, 1, 0)
        val trigger = nextTriggerTime(now, 2 * 60 + 30)
        assertEquals(3, trigger.hour)
        assertEquals(30, trigger.minute)
        assertEquals(29, trigger.dayOfMonth)
    }

    @Test
    fun dstEnd_keepsLocalTimeOnNextDay() {
        // 2026-10-25 in Madrid: clocks go back from 03:00 to 02:00.
        val now = at(2026, 10, 24, 22, 0)
        val trigger = nextTriggerTime(now, 7 * 60)
        assertEquals(at(2026, 10, 25, 7, 0), trigger)
        assertEquals(10 * 60 * 60L, java.time.Duration.between(now, trigger).seconds)
    }

    @Test(expected = IllegalArgumentException::class)
    fun outOfRangeMinutes_throws() {
        nextTriggerTime(at(2026, 10, 21, 7, 0), MINUTES_PER_DAY)
    }
}
