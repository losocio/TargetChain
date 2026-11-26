package com.targetchain.targetchain.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.targetchain.targetchain.MainActivity
import com.targetchain.targetchain.data.model.Alarm
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * Schedules a routine's first task [alarm] at the next occurrence of the routine's start time
     * with setAlarmClock, which is exempt from Doze and battery restrictions. Returns false if it
     * could not be scheduled.
     */
    fun schedule(alarm: Alarm, now: ZonedDateTime = ZonedDateTime.now()): Boolean {
        val startTime = alarm.startTimeMinutes ?: return false
        val triggerAtMillis = nextTriggerTime(now, startTime).toInstant().toEpochMilli()
        return setAlarmClock(
            triggerAtMillis,
            ringIntent(alarm.id.toInt(), alarm.routineId, alarm.id, isRoutineStart = true, isWatchdog = false),
        )
    }

    /**
     * Schedules task [alarmId] of the running routine [routineId] to ring at [triggerAtMillis]:
     * the next task when a countdown ends, or a ringing alarm again after a reboot. Replaces any
     * pending ring of that task.
     */
    fun scheduleAt(routineId: Long, alarmId: Long, triggerAtMillis: Long): Boolean =
        setAlarmClock(
            triggerAtMillis,
            ringIntent(alarmId.toInt(), routineId, alarmId, isRoutineStart = false, isWatchdog = false),
        )

    fun cancel(alarmId: Long) = cancelIntent(alarmIntent(alarmId.toInt()))

    /**
     * While an alarm rings, a watchdog alarm fires every [WATCHDOG_INTERVAL_MS] and starts the
     * ringing service again in case the process was killed. setAlarmClock also allows that
     * foreground service start from the background.
     */
    fun scheduleWatchdog(routineId: Long, alarmId: Long): Boolean =
        setAlarmClock(
            System.currentTimeMillis() + WATCHDOG_INTERVAL_MS,
            ringIntent(WATCHDOG_REQUEST_CODE, routineId, alarmId, isRoutineStart = false, isWatchdog = true),
        )

    fun cancelWatchdog() = cancelIntent(alarmIntent(WATCHDOG_REQUEST_CODE))

    /**
     * End of the last task's countdown: the routine ends silently. Nothing rings, so it isn't an
     * alarm clock (it wouldn't belong in the status bar's next alarm), just an exact alarm.
     */
    fun scheduleRoutineEnd(triggerAtMillis: Long): Boolean =
        setExact(triggerAtMillis, routineEventIntent(RoutineEventReceiver.ACTION_COUNTDOWN_ENDED))

    fun cancelRoutineEnd() = cancelIntent(routineEventIntent(RoutineEventReceiver.ACTION_COUNTDOWN_ENDED, create = false))

    /**
     * During a countdown, brings the countdown notification back if its service was killed. The
     * next alarm doesn't depend on it: it's already scheduled in AlarmManager.
     */
    fun scheduleCountdownWatchdog(): Boolean =
        setExact(
            System.currentTimeMillis() + WATCHDOG_INTERVAL_MS,
            routineEventIntent(RoutineEventReceiver.ACTION_COUNTDOWN_WATCHDOG),
        )

    fun cancelCountdownWatchdog() =
        cancelIntent(routineEventIntent(RoutineEventReceiver.ACTION_COUNTDOWN_WATCHDOG, create = false))

    private fun setAlarmClock(triggerAtMillis: Long, operation: PendingIntent): Boolean {
        if (!alarmManager.canScheduleExactAlarms()) return false
        val showIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), operation)
        return true
    }

    private fun setExact(triggerAtMillis: Long, operation: PendingIntent?): Boolean {
        if (operation == null || !alarmManager.canScheduleExactAlarms()) return false
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        return true
    }

    private fun cancelIntent(intent: PendingIntent?) {
        intent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /** Extras don't take part in PendingIntent matching, so the request code identifies it. */
    private fun alarmIntent(requestCode: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun ringIntent(
        requestCode: Int,
        routineId: Long,
        alarmId: Long,
        isRoutineStart: Boolean,
        isWatchdog: Boolean,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java)
                .putExtra(AlarmReceiver.EXTRA_ROUTINE_ID, routineId)
                .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                .putExtra(AlarmReceiver.EXTRA_IS_ROUTINE_START, isRoutineStart)
                .putExtra(AlarmReceiver.EXTRA_IS_WATCHDOG, isWatchdog),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Each action has its own PendingIntent (the action takes part in matching). */
    private fun routineEventIntent(action: String, create: Boolean = true): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, RoutineEventReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE,
        )

    private companion object {
        // Alarm ids are positive (autoGenerate), so this request code can't clash with them.
        const val WATCHDOG_REQUEST_CODE = -1
        const val WATCHDOG_INTERVAL_MS = 60_000L
    }
}
