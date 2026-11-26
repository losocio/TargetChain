package com.targetchain.targetchain.ui.routinelist

import com.targetchain.targetchain.permissions.PermissionStatus

data class RoutineListUiState(
    val routines: List<RoutineListItem> = emptyList(),
    val permissions: PermissionStatus = PermissionStatus(),
    /** Enabling a routine failed because it could not be scheduled (exact alarm permission missing). */
    val scheduleFailed: Boolean = false,
    /** Debug builds only: the alarm rings with the tone muted. */
    val silentTestMode: Boolean = false,
) {
    /** The card stays visible while any permission is missing, including the optional battery one. */
    val showPermissions: Boolean
        get() = !permissions.allRequiredGranted || !permissions.ignoringBatteryOptimizations
}

data class RoutineListItem(
    val routineId: Long,
    val name: String,
    /** Minutes since midnight. */
    val startTimeMinutes: Int,
    val taskCount: Int,
    val isEnabled: Boolean,
    /** Set while the routine runs: it can't be edited, disabled or deleted until it ends. */
    val running: RunningTask? = null,
) {
    val isRunning: Boolean
        get() = running != null
}

/**
 * Where a running routine is.
 *
 * @property taskNumber task that is ringing or in its countdown (1-based).
 * @property countdownEndAtWallMs end of the task's countdown; null while its alarm rings.
 * @property isLastTask its countdown ends the routine.
 */
data class RunningTask(
    val taskNumber: Int,
    val countdownEndAtWallMs: Long?,
    val isLastTask: Boolean,
)
