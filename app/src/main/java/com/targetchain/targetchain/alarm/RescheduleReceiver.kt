package com.targetchain.targetchain.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.targetchain.targetchain.appContainer
import kotlinx.coroutines.launch

/**
 * Reschedules the enabled routines when the system drops or invalidates our alarms:
 * reboot, app update, clock or time zone change, and exact alarm permission granted.
 * Also makes an alarm that was still ringing ring again.
 */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val container = context.appContainer
        val pendingResult = goAsync()
        container.applicationScope.launch {
            try {
                container.routineRepository.getAlarmsToReschedule()
                    .forEach { container.alarmScheduler.schedule(it) }
                // An alarm that was ringing and wasn't stopped (e.g. the phone was rebooted while it
                // rang) rings again. Done through an alarm instead of starting the service here,
                // because Android 15+ forbids starting a mediaPlayback foreground service from
                // BOOT_COMPLETED. Scheduled last, so it replaces the normal schedule of that alarm.
                container.alarmStateStore.ringingAlarmId?.let { alarmId ->
                    container.alarmScheduler.scheduleAt(alarmId, System.currentTimeMillis() + RESUME_DELAY_MS)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val RESUME_DELAY_MS = 3_000L

        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
