package com.targetchain.targetchain.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fired by AlarmManager at the alarm time. setAlarmClock allows starting a foreground service from here. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, NO_ALARM_ID)
        if (alarmId == NO_ALARM_ID) return
        AlarmRingingService.start(context, alarmId)
    }

    companion object {
        const val EXTRA_ALARM_ID = "com.targetchain.targetchain.extra.ALARM_ID"
        const val NO_ALARM_ID = -1L
    }
}
