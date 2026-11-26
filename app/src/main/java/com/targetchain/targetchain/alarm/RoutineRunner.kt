package com.targetchain.targetchain.alarm

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import com.targetchain.targetchain.chain.AfterTask
import com.targetchain.targetchain.chain.ClockReading
import com.targetchain.targetchain.chain.Countdown
import com.targetchain.targetchain.chain.CountdownAction
import com.targetchain.targetchain.chain.FireDecision
import com.targetchain.targetchain.chain.afterTask
import com.targetchain.targetchain.chain.countdownAction
import com.targetchain.targetchain.chain.decideAlarmFire
import com.targetchain.targetchain.chain.reanchored
import com.targetchain.targetchain.chain.startCountdown
import com.targetchain.targetchain.data.RoutineRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Moves the running routine through its tasks: a task's alarm rings, its challenges are completed,
 * its countdown runs, and the next task's alarm rings, until the last countdown ends.
 *
 * The state is persisted in [AlarmStateStore] before acting, so a reboot or a killed process
 * continues from it. The next alarm is always scheduled in AlarmManager, so it rings even if the
 * countdown service is gone. Calls are serialized with a mutex, so two events (e.g. the next alarm
 * and "Next task") can't interleave.
 *
 * [context] is the Application.
 */
class RoutineRunner(
    private val context: Context,
    private val store: AlarmStateStore,
    private val repository: RoutineRepository,
    private val scheduler: AlarmScheduler,
) {

    private val mutex = Mutex()

    /** A task's alarm fired (or the debug "Start now" button was pressed). */
    suspend fun onAlarmFired(routineId: Long, alarmId: Long, isRoutineStart: Boolean, isWatchdog: Boolean) =
        mutex.withLock {
            when (decideAlarmFire(store.run, routineId, alarmId, isRoutineStart, isWatchdog)) {
                FireDecision.RING -> startRinging(routineId, alarmId)
                FireDecision.SKIP_OTHER_ROUTINE_RUNNING -> skipRoutine(routineId, alarmId)
                FireDecision.IGNORE -> Log.i(TAG, "Ignored alarm $alarmId of routine $routineId")
            }
        }

    /**
     * The ringing task's last challenge was completed (or Stop, in debug builds): the tone stops and
     * the task's countdown starts, measured from now. The next task's alarm is scheduled at its end.
     * Called from the alarm screen while it's still visible, so the countdown service may start.
     */
    suspend fun completeRingingTask() = mutex.withLock {
        val run = store.run
        val routineId = run.routineId
        val alarmId = run.ringingAlarmId
        if (routineId == null || alarmId == null) {
            AlarmRingingService.stop(context)
            return@withLock
        }
        val tasks = runCatching { repository.getTasks(routineId) }.getOrDefault(emptyList())
        val durationMs = (tasks.firstOrNull { it.id == alarmId }?.durationSeconds ?: 0) * 1_000L
        val next = afterTask(tasks.map { it.id }, alarmId)
        // A task without duration only exists in corrupted data: the routine ends.
        if (durationMs <= 0 && next == AfterTask.EndRoutine) {
            endRoutineLocked()
            return@withLock
        }
        val countdown = Countdown(
            alarmId = alarmId,
            nextAlarmId = (next as? AfterTask.NextTask)?.alarmId,
            timing = startCountdown(clockNow(), durationMs),
        )
        // One write replaces the ringing state: if the process dies after it, the countdown continues.
        store.setCountdown(routineId, countdown)
        scheduleCountdownEnd(routineId, countdown, minDelayMs = 0)
        RoutineCountdownService.startSafely(context)
        AlarmRingingService.stop(context)
    }

    /**
     * "Next task" during a countdown: the next task's alarm rings now, or the routine ends if it was
     * the last task. Called from the app while it's visible, so the ringing service may start.
     */
    suspend fun advance() = mutex.withLock {
        val routineId = store.run.routineId ?: return@withLock
        val countdown = store.run.countdown ?: return@withLock
        val next = countdown.nextAlarmId
        if (next == null) {
            endRoutineLocked()
        } else {
            scheduler.cancel(next)
            startRinging(routineId, next)
        }
    }

    /** Ends the running routine now: the last countdown ended, or debug "End routine". */
    suspend fun endRoutine() = mutex.withLock { endRoutineLocked() }

    /** The last task's countdown alarm fired. Ignored if the countdown was re-anchored later. */
    suspend fun onCountdownEnded() = mutex.withLock {
        val countdown = store.run.countdown ?: return@withLock
        if (countdown.nextAlarmId != null) return@withLock
        when (val action = countdownAction(countdown, clockNow(), RESUME_DELAY_MS)) {
            CountdownAction.EndRoutineNow -> endRoutineLocked()
            is CountdownAction.ScheduleAt -> scheduler.scheduleRoutineEnd(action.atWallMs)
        }
    }

    /**
     * After a reboot, an app update or a time or time zone change: the running routine continues.
     * A countdown keeps the time it had left (see [com.targetchain.targetchain.chain.CountdownTiming]):
     * its end is re-anchored and scheduled again. An alarm that was still ringing rings again.
     */
    suspend fun resume() = mutex.withLock {
        val run = store.run
        val routineId = run.routineId ?: return@withLock
        val ringing = run.ringingAlarmId
        val countdown = run.countdown
        when {
            // Through an alarm instead of starting the service here, because Android 15+ forbids
            // starting a mediaPlayback foreground service from BOOT_COMPLETED.
            ringing != null -> scheduler.scheduleAt(routineId, ringing, System.currentTimeMillis() + RESUME_DELAY_MS)
            countdown != null -> {
                val reanchored = countdown.copy(timing = countdown.timing.reanchored(clockNow()))
                store.setCountdown(routineId, reanchored)
                if (scheduleCountdownEnd(routineId, reanchored, RESUME_DELAY_MS)) {
                    RoutineCountdownService.startSafely(context)
                }
            }
            // Running but neither ringing nor in a countdown: can't happen, but never stay locked.
            else -> endRoutineLocked()
        }
    }

    /** The countdown notification is restored if its service was killed. */
    fun onCountdownWatchdog() {
        if (store.run.countdown == null) return
        scheduler.scheduleCountdownWatchdog()
        if (!RoutineCountdownService.isRunning) RoutineCountdownService.startSafely(context)
    }

    private fun startRinging(routineId: Long, alarmId: Long) {
        val wasInCountdown = store.run.countdown != null
        store.setRinging(routineId, alarmId)
        if (wasInCountdown) stopCountdown()
        AlarmRingingService.start(context, alarmId)
    }

    /** Returns false if the routine ended instead. */
    private suspend fun scheduleCountdownEnd(routineId: Long, countdown: Countdown, minDelayMs: Long): Boolean {
        val action = countdownAction(countdown, clockNow(), minDelayMs)
        val next = countdown.nextAlarmId
        when {
            action == CountdownAction.EndRoutineNow -> {
                endRoutineLocked()
                return false
            }
            action is CountdownAction.ScheduleAt && next != null -> scheduler.scheduleAt(routineId, next, action.atWallMs)
            action is CountdownAction.ScheduleAt -> scheduler.scheduleRoutineEnd(action.atWallMs)
        }
        scheduler.scheduleCountdownWatchdog()
        return true
    }

    private suspend fun endRoutineLocked() {
        val run = store.run
        store.clearRun()
        run.countdown?.nextAlarmId?.let { scheduler.cancel(it) }
        stopCountdown()
        if (run.ringingAlarmId != null) AlarmRingingService.stop(context)
        val routineId = run.routineId ?: return
        // One-shot: its switch turns off and its start isn't scheduled any more.
        runCatching {
            repository.getRoutine(routineId)?.alarms?.forEach { scheduler.cancel(it.id) }
            repository.disableRoutine(routineId)
        }.onFailure { Log.e(TAG, "Cannot disable routine $routineId", it) }
    }

    private fun stopCountdown() {
        scheduler.cancelRoutineEnd()
        scheduler.cancelCountdownWatchdog()
        RoutineCountdownService.stop(context)
    }

    /** It doesn't ring or lock anything: its switch turns off and a notification explains it. */
    private suspend fun skipRoutine(routineId: Long, alarmId: Long) {
        scheduler.cancel(alarmId)
        val name = runCatching {
            repository.disableRoutine(routineId)
            repository.getRoutine(routineId)?.routine?.name
        }.getOrNull()
        AlarmNotifications.showRoutineSkipped(context, routineId, name.orEmpty())
    }

    private fun clockNow() = ClockReading(
        wallMs = System.currentTimeMillis(),
        elapsedMs = SystemClock.elapsedRealtime(),
        bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0),
    )

    private companion object {
        const val TAG = "RoutineRunner"
        const val RESUME_DELAY_MS = 3_000L
    }
}
