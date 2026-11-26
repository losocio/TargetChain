package com.targetchain.targetchain.chain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChainLogicTest {

    // Duration

    @Test
    fun `duration fields are parsed, empty fields count as zero`() {
        assertEquals(1, parseDurationFields("", "", "1"))
        assertEquals(90 * 60, parseDurationFields("1", "30", ""))
        assertEquals(3723, parseDurationFields(" 1 ", "2", "3"))
        assertEquals(12 * 3600, parseDurationFields("12", "0", "0"))
    }

    @Test
    fun `invalid duration fields give null`() {
        assertNull(parseDurationFields("", "", ""))
        assertNull(parseDurationFields("0", "0", "0"))
        assertNull(parseDurationFields("12", "0", "1"))
        assertNull(parseDurationFields("13", "", ""))
        assertNull(parseDurationFields("", "60", ""))
        assertNull(parseDurationFields("", "", "60"))
        assertNull(parseDurationFields("-1", "", ""))
        assertNull(parseDurationFields("a", "", ""))
        assertNull(parseDurationFields("99999999999", "", ""))
    }

    @Test
    fun `duration range is 1 second to 12 hours`() {
        assertFalse(isValidTaskDuration(null))
        assertFalse(isValidTaskDuration(0))
        assertTrue(isValidTaskDuration(1))
        assertTrue(isValidTaskDuration(12 * 3600))
        assertFalse(isValidTaskDuration(12 * 3600 + 1))
    }

    @Test
    fun `durations are split and formatted`() {
        assertEquals(HoursMinutesSeconds(1, 2, 3), splitDuration(3723))
        assertEquals("1:02:03", formatCountdown(3723))
        assertEquals("02:03", formatCountdown(123))
        assertEquals("00:00", formatCountdown(-5))
    }

    @Test
    fun `a task needs a challenge and a duration`() {
        assertTrue(isTaskComplete(1, 60))
        assertFalse(isTaskComplete(0, 60))
        assertFalse(isTaskComplete(2, null))
        assertFalse(isTaskComplete(2, 0))
    }

    // Earliest end

    @Test
    fun `earliest end adds all durations to the start time`() {
        assertEquals(EarliestEnd(8 * 3600 + 45 * 60, 0), earliestEnd(7 * 60, listOf(30 * 60, 75 * 60)))
        assertEquals(EarliestEnd(7 * 3600 + 1, 0), earliestEnd(7 * 60, listOf(1)))
    }

    @Test
    fun `earliest end can be on a later day`() {
        assertEquals(EarliestEnd(1800, 1), earliestEnd(23 * 60, listOf(3600, 1800)))
        assertEquals(EarliestEnd(0, 2), earliestEnd(0, listOf(12 * 3600, 12 * 3600, 12 * 3600, 12 * 3600)))
    }

    @Test
    fun `earliest end is unknown while a duration is missing`() {
        assertNull(earliestEnd(7 * 60, listOf(60, null)))
        assertNull(earliestEnd(7 * 60, emptyList()))
    }

    // Chain

    @Test
    fun `after a task comes the next one, after the last the routine ends`() {
        val tasks = listOf(10L, 20L, 30L)
        assertEquals(AfterTask.NextTask(20), afterTask(tasks, 10))
        assertEquals(AfterTask.NextTask(30), afterTask(tasks, 20))
        assertEquals(AfterTask.EndRoutine, afterTask(tasks, 30))
        assertEquals(AfterTask.EndRoutine, afterTask(tasks, 99))
    }

    private val timing = CountdownTiming(endAtWallMs = 0, anchorElapsedMs = 0, remainingAtAnchorMs = 0, bootCount = 1)

    @Test
    fun `a routine start rings when nothing is running`() {
        assertEquals(FireDecision.RING, decideAlarmFire(RoutineRun(), 1, 10, isRoutineStart = true, isWatchdog = false))
        // A stale alarm of a later task doesn't start anything.
        assertEquals(FireDecision.IGNORE, decideAlarmFire(RoutineRun(), 1, 20, isRoutineStart = false, isWatchdog = false))
    }

    @Test
    fun `a routine start is skipped while another routine runs`() {
        val ringing = RoutineRun(routineId = 2, ringingAlarmId = 50)
        val inCountdown = RoutineRun(routineId = 2, countdown = Countdown(50, null, timing))
        assertEquals(FireDecision.SKIP_OTHER_ROUTINE_RUNNING, decideAlarmFire(ringing, 1, 10, isRoutineStart = true, isWatchdog = false))
        assertEquals(FireDecision.SKIP_OTHER_ROUTINE_RUNNING, decideAlarmFire(inCountdown, 1, 10, isRoutineStart = true, isWatchdog = false))
        assertEquals(FireDecision.IGNORE, decideAlarmFire(inCountdown, 1, 20, isRoutineStart = false, isWatchdog = false))
    }

    @Test
    fun `the next task of the running routine rings, other alarms are ignored`() {
        val run = RoutineRun(routineId = 1, countdown = Countdown(10, 20, timing))
        assertEquals(FireDecision.RING, decideAlarmFire(run, 1, 20, isRoutineStart = false, isWatchdog = false))
        assertEquals(FireDecision.IGNORE, decideAlarmFire(run, 1, 30, isRoutineStart = false, isWatchdog = false))
        assertEquals(FireDecision.IGNORE, decideAlarmFire(run, 1, 10, isRoutineStart = true, isWatchdog = false))
    }

    @Test
    fun `a ringing alarm rings again after a reboot or a kill`() {
        val run = RoutineRun(routineId = 1, ringingAlarmId = 20)
        assertEquals(FireDecision.RING, decideAlarmFire(run, 1, 20, isRoutineStart = false, isWatchdog = false))
        assertEquals(FireDecision.RING, decideAlarmFire(run, 1, 20, isRoutineStart = false, isWatchdog = true))
    }

    @Test
    fun `a watchdog only rings while its alarm is still ringing`() {
        val inCountdown = RoutineRun(routineId = 1, countdown = Countdown(20, 30, timing))
        assertEquals(FireDecision.IGNORE, decideAlarmFire(inCountdown, 1, 20, isRoutineStart = false, isWatchdog = true))
        assertEquals(FireDecision.IGNORE, decideAlarmFire(RoutineRun(), 1, 20, isRoutineStart = false, isWatchdog = true))
    }

    // Countdown timing

    private val start = ClockReading(wallMs = 1_000_000, elapsedMs = 5_000, bootCount = 3)
    private val tenMinutes = 10 * 60_000L

    @Test
    fun `time left follows the monotonic clock`() {
        val timing = startCountdown(start, tenMinutes)
        assertEquals(start.wallMs + tenMinutes, timing.endAtWallMs)
        val later = ClockReading(wallMs = start.wallMs + 60_000, elapsedMs = start.elapsedMs + 60_000, bootCount = 3)
        assertEquals(tenMinutes - 60_000, timing.remainingMs(later))
    }

    @Test
    fun `moving the clock forward or back doesn't change the time left`() {
        val timing = startCountdown(start, tenMinutes)
        // One minute later, the clock is moved one hour forward.
        val forward = ClockReading(wallMs = start.wallMs + 60_000 + 3_600_000, elapsedMs = start.elapsedMs + 60_000, bootCount = 3)
        assertEquals(tenMinutes - 60_000, timing.remainingMs(forward))
        val reanchored = timing.reanchored(forward)
        assertEquals(forward.wallMs + tenMinutes - 60_000, reanchored.endAtWallMs)

        // Then one hour back.
        val back = ClockReading(wallMs = start.wallMs + 120_000 - 3_600_000, elapsedMs = start.elapsedMs + 120_000, bootCount = 3)
        assertEquals(tenMinutes - 120_000, reanchored.remainingMs(back))
        assertEquals(back.wallMs + tenMinutes - 120_000, reanchored.reanchored(back).endAtWallMs)
    }

    @Test
    fun `after a reboot the wall clock is trusted`() {
        val timing = startCountdown(start, tenMinutes)
        val afterReboot = ClockReading(wallMs = start.wallMs + 120_000, elapsedMs = 30_000, bootCount = 4)
        assertEquals(tenMinutes - 120_000, timing.remainingMs(afterReboot))
        val reanchored = timing.reanchored(afterReboot)
        assertEquals(4, reanchored.bootCount)
        assertEquals(30_000, reanchored.anchorElapsedMs)
        assertEquals(timing.endAtWallMs, reanchored.endAtWallMs)
    }

    @Test
    fun `time left is never negative`() {
        val timing = startCountdown(start, 1_000)
        val muchLater = ClockReading(wallMs = start.wallMs + tenMinutes, elapsedMs = start.elapsedMs + tenMinutes, bootCount = 3)
        assertEquals(0, timing.remainingMs(muchLater))
    }

    @Test
    fun `a passed next alarm rings soon, a passed last task ends the routine`() {
        val timing = startCountdown(start, 1_000)
        val later = ClockReading(wallMs = start.wallMs + 60_000, elapsedMs = 1_000, bootCount = 4)
        assertEquals(
            CountdownAction.ScheduleAt(later.wallMs + 3_000),
            countdownAction(Countdown(10, 20, timing), later, minDelayMs = 3_000),
        )
        assertEquals(CountdownAction.EndRoutineNow, countdownAction(Countdown(10, null, timing), later, minDelayMs = 3_000))
    }

    @Test
    fun `a pending countdown is scheduled at its end`() {
        val timing = startCountdown(start, tenMinutes)
        assertEquals(CountdownAction.ScheduleAt(start.wallMs + tenMinutes), countdownAction(Countdown(10, 20, timing), start, 3_000))
        assertEquals(CountdownAction.ScheduleAt(start.wallMs + tenMinutes), countdownAction(Countdown(10, null, timing), start, 3_000))
    }
}
