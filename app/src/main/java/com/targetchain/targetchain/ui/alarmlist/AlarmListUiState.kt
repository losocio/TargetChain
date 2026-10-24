package com.targetchain.targetchain.ui.alarmlist

import com.targetchain.targetchain.permissions.PermissionStatus

data class AlarmListUiState(
    val alarms: List<AlarmListItem> = emptyList(),
    val permissions: PermissionStatus = PermissionStatus(),
    /** Enabling an alarm failed because it could not be scheduled (exact alarm permission missing). */
    val scheduleFailed: Boolean = false,
    /** Debug builds only: the alarm rings with the tone muted. */
    val silentTestMode: Boolean = false,
) {
    /** The card stays visible while any permission is missing, including the optional battery one. */
    val showPermissions: Boolean
        get() = !permissions.allRequiredGranted || !permissions.ignoringBatteryOptimizations
}

/** One alarm of the list. Each one is a routine with a single alarm. */
data class AlarmListItem(
    val routineId: Long,
    val name: String,
    /** Minutes since midnight. */
    val startTimeMinutes: Int,
    val isEnabled: Boolean,
)
