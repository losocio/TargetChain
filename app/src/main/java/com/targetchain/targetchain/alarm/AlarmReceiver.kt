package com.targetchain.targetchain.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.targetchain.targetchain.appContainer
import kotlinx.coroutines.launch

/**
 * Fired by AlarmManager when a task's alarm must ring. setAlarmClock allows starting a foreground
 * service from here. [RoutineRunner] decides whether it rings: a routine whose start time arrives
 * while another one runs is skipped.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, NO_ID)
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, NO_ID)
        // Alarms scheduled by older versions of the app have no routine id: ignored.
        if (routineId == NO_ID || alarmId == NO_ID) return
        val container = context.appContainer
        val pendingResult = goAsync()
        container.applicationScope.launch {
            try {
                container.routineRunner.onAlarmFired(
                    routineId = routineId,
                    alarmId = alarmId,
                    isRoutineStart = intent.getBooleanExtra(EXTRA_IS_ROUTINE_START, false),
                    isWatchdog = intent.getBooleanExtra(EXTRA_IS_WATCHDOG, false),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ROUTINE_ID = "com.targetchain.targetchain.extra.ROUTINE_ID"
        const val EXTRA_ALARM_ID = "com.targetchain.targetchain.extra.ALARM_ID"
        const val EXTRA_IS_ROUTINE_START = "com.targetchain.targetchain.extra.IS_ROUTINE_START"
        const val EXTRA_IS_WATCHDOG = "com.targetchain.targetchain.extra.IS_WATCHDOG"
        const val NO_ID = -1L
    }
}
