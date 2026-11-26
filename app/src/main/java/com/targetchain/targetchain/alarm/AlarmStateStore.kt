package com.targetchain.targetchain.alarm

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.targetchain.targetchain.chain.Countdown
import com.targetchain.targetchain.chain.CountdownTiming
import com.targetchain.targetchain.chain.RoutineRun
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small persistent state that must survive the process dying or a reboot while a routine runs.
 * Writes use commit (synchronous) so the state is on disk before a possible reboot or kill, and
 * each change of the running routine is written at once, so it's never half updated.
 * Only one instance of this class exists per process.
 */
class AlarmStateStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _run = MutableStateFlow(prefs.readRun())

    /** The running routine, for the screens that show it or lock it. */
    val runFlow: StateFlow<RoutineRun> = _run.asStateFlow()

    val run: RoutineRun
        get() = _run.value

    /** Routine being run, or null. Its tasks, challenges and passwords can't be edited or deleted. */
    val activeRoutineId: Long?
        get() = run.routineId

    /** Task whose alarm is ringing and hasn't been completed yet, or null. */
    val ringingAlarmId: Long?
        get() = run.ringingAlarmId

    /** Task [alarmId] of routine [routineId] starts ringing (any countdown is over). */
    fun setRinging(routineId: Long, alarmId: Long) =
        write(RoutineRun(routineId = routineId, ringingAlarmId = alarmId))

    /** The ringing task's challenges were completed: its countdown starts. */
    fun setCountdown(routineId: Long, countdown: Countdown) =
        write(RoutineRun(routineId = routineId, countdown = countdown))

    /** The routine ended: nothing is running. */
    fun clearRun() = write(RoutineRun())

    private fun write(run: RoutineRun) {
        prefs.edit(commit = true) {
            putLongOrRemove(KEY_ACTIVE_ROUTINE_ID, run.routineId)
            putLongOrRemove(KEY_RINGING_ALARM_ID, run.ringingAlarmId)
            val countdown = run.countdown
            putLongOrRemove(KEY_COUNTDOWN_ALARM_ID, countdown?.alarmId)
            putLongOrRemove(KEY_COUNTDOWN_NEXT_ALARM_ID, countdown?.nextAlarmId)
            putLongOrRemove(KEY_COUNTDOWN_END_AT, countdown?.timing?.endAtWallMs)
            putLongOrRemove(KEY_COUNTDOWN_ANCHOR_ELAPSED, countdown?.timing?.anchorElapsedMs)
            putLongOrRemove(KEY_COUNTDOWN_REMAINING_AT_ANCHOR, countdown?.timing?.remainingAtAnchorMs)
            if (countdown == null) remove(KEY_COUNTDOWN_BOOT_COUNT) else putInt(KEY_COUNTDOWN_BOOT_COUNT, countdown.timing.bootCount)
        }
        _run.value = run
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
        const val KEY_ACTIVE_ROUTINE_ID = "active_routine_id"
        const val KEY_COUNTDOWN_ALARM_ID = "countdown_alarm_id"
        const val KEY_COUNTDOWN_NEXT_ALARM_ID = "countdown_next_alarm_id"
        const val KEY_COUNTDOWN_END_AT = "countdown_end_at"
        const val KEY_COUNTDOWN_ANCHOR_ELAPSED = "countdown_anchor_elapsed"
        const val KEY_COUNTDOWN_REMAINING_AT_ANCHOR = "countdown_remaining_at_anchor"
        const val KEY_COUNTDOWN_BOOT_COUNT = "countdown_boot_count"
        const val KEY_PREVIOUS_ALARM_VOLUME = "previous_alarm_volume"
        const val KEY_SILENT_TEST_MODE = "silent_test_mode"
        const val NO_VALUE = Long.MIN_VALUE
        const val NO_VOLUME = -1

        fun SharedPreferences.Editor.putLongOrRemove(key: String, value: Long?) {
            if (value == null) remove(key) else putLong(key, value)
        }

        fun SharedPreferences.getLongOrNull(key: String): Long? =
            getLong(key, NO_VALUE).takeIf { it != NO_VALUE }

        fun SharedPreferences.readRun(): RoutineRun {
            val routineId = getLongOrNull(KEY_ACTIVE_ROUTINE_ID)
            val countdownAlarmId = getLongOrNull(KEY_COUNTDOWN_ALARM_ID)
            val endAt = getLongOrNull(KEY_COUNTDOWN_END_AT)
            val countdown = if (countdownAlarmId != null && endAt != null) {
                Countdown(
                    alarmId = countdownAlarmId,
                    nextAlarmId = getLongOrNull(KEY_COUNTDOWN_NEXT_ALARM_ID),
                    timing = CountdownTiming(
                        endAtWallMs = endAt,
                        anchorElapsedMs = getLongOrNull(KEY_COUNTDOWN_ANCHOR_ELAPSED) ?: 0,
                        remainingAtAnchorMs = getLongOrNull(KEY_COUNTDOWN_REMAINING_AT_ANCHOR) ?: 0,
                        // Unknown: the wall clock is trusted, as after a reboot.
                        bootCount = getInt(KEY_COUNTDOWN_BOOT_COUNT, -1),
                    ),
                )
            } else {
                null
            }
            return RoutineRun(
                routineId = routineId,
                ringingAlarmId = getLongOrNull(KEY_RINGING_ALARM_ID),
                countdown = countdown,
            )
        }
    }
}
