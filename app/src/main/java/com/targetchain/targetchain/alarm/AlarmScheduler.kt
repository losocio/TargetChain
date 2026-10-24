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
     * Schedules [alarm] at the next occurrence of its start time with setAlarmClock, which is
     * exempt from Doze and battery restrictions. Returns false if it could not be scheduled.
     */
    fun schedule(alarm: Alarm, now: ZonedDateTime = ZonedDateTime.now()): Boolean {
        val startTime = alarm.startTimeMinutes ?: return false
        val triggerAtMillis = nextTriggerTime(now, startTime).toInstant().toEpochMilli()
        return scheduleAt(alarm.id, triggerAtMillis)
    }

    /** Schedules [alarmId] to ring at [triggerAtMillis]. Replaces any pending ring of that alarm. */
    fun scheduleAt(alarmId: Long, triggerAtMillis: Long): Boolean =
        setAlarmClock(triggerAtMillis, ringIntent(alarmId.toInt(), alarmId, isWatchdog = false))

    fun cancel(alarmId: Long) = cancelIntent(alarmId.toInt())

    /**
     * While an alarm rings, a watchdog alarm fires every [WATCHDOG_INTERVAL_MS] and starts the
     * ringing service again in case the process was killed. setAlarmClock also allows that
     * foreground service start from the background.
     */
    fun scheduleWatchdog(alarmId: Long): Boolean =
        setAlarmClock(
            System.currentTimeMillis() + WATCHDOG_INTERVAL_MS,
            ringIntent(WATCHDOG_REQUEST_CODE, alarmId, isWatchdog = true),
        )

    fun cancelWatchdog() = cancelIntent(WATCHDOG_REQUEST_CODE)

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

    private fun cancelIntent(requestCode: Int) {
        // Extras don't take part in PendingIntent matching, so the request code identifies it.
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun ringIntent(requestCode: Int, alarmId: Long, isWatchdog: Boolean): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java)
                .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                .putExtra(AlarmReceiver.EXTRA_IS_WATCHDOG, isWatchdog),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        // Alarm ids are positive (autoGenerate), so this request code can't clash with them.
        const val WATCHDOG_REQUEST_CODE = -1
        const val WATCHDOG_INTERVAL_MS = 60_000L
    }
}
