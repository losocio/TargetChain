package com.targetchain.targetchain.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.targetchain.targetchain.appContainer
import kotlinx.coroutines.launch

/**
 * Reschedules the enabled routines when the system drops or invalidates our alarms:
 * reboot, app update, clock or time zone change, and exact alarm permission granted.
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
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
