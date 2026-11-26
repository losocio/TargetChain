package com.targetchain.targetchain.chain

/**
 * The routine being run, persisted so it survives a reboot or a killed process. Only one routine
 * runs at a time. While [routineId] is set, the routine is locked: it can't be edited, disabled or
 * deleted.
 *
 * @property ringingAlarmId task whose alarm is ringing (its challenges aren't completed yet).
 * @property countdown task whose countdown is running.
 */
data class RoutineRun(
    val routineId: Long? = null,
    val ringingAlarmId: Long? = null,
    val countdown: Countdown? = null,
) {
    val isRunning: Boolean
        get() = routineId != null
}

/**
 * @property alarmId task whose countdown is running.
 * @property nextAlarmId task whose alarm rings when it ends; null when it's the last task.
 */
data class Countdown(
    val alarmId: Long,
    val nextAlarmId: Long?,
    val timing: CountdownTiming,
)

/** What follows when a task's challenges are completed. */
sealed interface AfterTask {
    data class NextTask(val alarmId: Long) : AfterTask
    data object EndRoutine : AfterTask
}

/** [taskAlarmIds] are the routine's tasks in order. An unknown task ends the routine. */
fun afterTask(taskAlarmIds: List<Long>, completedAlarmId: Long): AfterTask {
    val index = taskAlarmIds.indexOf(completedAlarmId)
    val next = taskAlarmIds.getOrNull(index + 1)
    return if (index >= 0 && next != null) AfterTask.NextTask(next) else AfterTask.EndRoutine
}

enum class FireDecision {
    /** The alarm rings (a routine starts, the next task starts, or it rings again). */
    RING,

    /** A routine's start time arrived while another routine was running: it doesn't run. */
    SKIP_OTHER_ROUTINE_RUNNING,

    /** Stale alarm (e.g. a watchdog after the alarm was stopped): nothing happens. */
    IGNORE,
}

/**
 * Decides what to do when an alarm of task [alarmId] of routine [routineId] fires.
 *
 * @param isRoutineStart it's the first task's alarm, scheduled at the routine's start time.
 * @param isWatchdog it's the watchdog that makes a ringing alarm ring again after a kill.
 */
fun decideAlarmFire(
    run: RoutineRun,
    routineId: Long,
    alarmId: Long,
    isRoutineStart: Boolean,
    isWatchdog: Boolean,
): FireDecision {
    if (isWatchdog) return if (run.ringingAlarmId == alarmId) FireDecision.RING else FireDecision.IGNORE
    return when (run.routineId) {
        null -> if (isRoutineStart) FireDecision.RING else FireDecision.IGNORE
        routineId -> {
            val expected = run.ringingAlarmId == alarmId || run.countdown?.nextAlarmId == alarmId
            if (expected) FireDecision.RING else FireDecision.IGNORE
        }
        else -> if (isRoutineStart) FireDecision.SKIP_OTHER_ROUTINE_RUNNING else FireDecision.IGNORE
    }
}

/** What to do with a countdown after a reboot, a time change or a killed process. */
sealed interface CountdownAction {
    /** Schedule the next task's alarm (or the routine's end) at [atWallMs]. */
    data class ScheduleAt(val atWallMs: Long) : CountdownAction

    /** The last task's countdown already ended: the routine ends now. */
    data object EndRoutineNow : CountdownAction
}

/**
 * A next task's alarm whose time has passed rings as soon as possible, but at least [minDelayMs]
 * from now, because alarms can't start the ringing service right at boot on every Android version.
 */
fun countdownAction(countdown: Countdown, now: ClockReading, minDelayMs: Long): CountdownAction {
    val remaining = countdown.timing.remainingMs(now)
    return when {
        countdown.nextAlarmId != null ->
            CountdownAction.ScheduleAt(now.wallMs + remaining.coerceAtLeast(minDelayMs))
        remaining <= 0 -> CountdownAction.EndRoutineNow
        else -> CountdownAction.ScheduleAt(now.wallMs + remaining)
    }
}
