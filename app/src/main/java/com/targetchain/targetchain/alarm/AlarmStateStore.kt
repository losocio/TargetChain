package com.targetchain.targetchain.alarm

import android.content.Context
import androidx.core.content.edit

/**
 * Small persistent state that must survive the process dying or a reboot while an alarm rings.
 * Writes use commit (synchronous) so the state is on disk before a possible reboot or kill.
 */
class AlarmStateStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Alarm that is ringing and hasn't been stopped yet, or null. */
    var ringingAlarmId: Long?
        get() = prefs.getLong(KEY_RINGING_ALARM_ID, NO_ALARM_ID).takeIf { it != NO_ALARM_ID }
        set(value) = prefs.edit(commit = true) {
            if (value == null) remove(KEY_RINGING_ALARM_ID) else putLong(KEY_RINGING_ALARM_ID, value)
        }

    /** Alarm stream volume before ringing forced it to max, restored when the alarm stops. */
    var previousAlarmVolume: Int?
        get() = prefs.getInt(KEY_PREVIOUS_ALARM_VOLUME, NO_VOLUME).takeIf { it != NO_VOLUME }
        set(value) = prefs.edit(commit = true) {
            if (value == null) remove(KEY_PREVIOUS_ALARM_VOLUME) else putInt(KEY_PREVIOUS_ALARM_VOLUME, value)
        }

    /** Debug builds only: ring with the tone muted, to test without making noise. */
    var silentTestMode: Boolean
        get() = prefs.getBoolean(KEY_SILENT_TEST_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_SILENT_TEST_MODE, value) }

    private companion object {
        const val PREFS_NAME = "alarm_state"
        const val KEY_RINGING_ALARM_ID = "ringing_alarm_id"
        const val KEY_PREVIOUS_ALARM_VOLUME = "previous_alarm_volume"
        const val KEY_SILENT_TEST_MODE = "silent_test_mode"
        const val NO_ALARM_ID = -1L
        const val NO_VOLUME = -1
    }
}
