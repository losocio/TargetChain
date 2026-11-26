package com.targetchain.targetchain.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.targetchain.targetchain.appContainer
import kotlinx.coroutines.launch

/**
 * Reschedules the enabled routines when the system drops or invalidates our alarms:
 * reboot, app update, clock or time zone change, and exact alarm permission granted.
 * Also continues the running routine (see [RoutineRunner.resume]).
 */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val container = context.appContainer
        val pendingResult = goAsync()
        container.applicationScope.launch {
            try {
                // The running routine's first task isn't scheduled again: it already started.
                val activeRoutineId = container.alarmStateStore.activeRoutineId
                container.routineRepository.getAlarmsToReschedule()
                    .filter { it.routineId != activeRoutineId }
                    .forEach { container.alarmScheduler.schedule(it) }
                // The running routine continues: a ringing alarm rings again, a countdown keeps the
                // time it had left and its next alarm is scheduled again.
                container.routineRunner.resume()
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
