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
        if (!alarmManager.canScheduleExactAlarms()) return false

        val triggerAtMillis = nextTriggerTime(now, startTime).toInstant().toEpochMilli()
        val showIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
            ringIntent(alarm.id, PendingIntent.FLAG_UPDATE_CURRENT)!!,
        )
        return true
    }

    fun cancel(alarmId: Long) {
        ringIntent(alarmId, PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun ringIntent(alarmId: Long, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            Intent(context, AlarmReceiver::class.java).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
}
