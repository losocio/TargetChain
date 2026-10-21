package com.targetchain.targetchain.ui.home

import com.targetchain.targetchain.permissions.PermissionStatus

data class HomeUiState(
    val permissions: PermissionStatus = PermissionStatus(),
    /** Start time of the scheduled test alarm in minutes since midnight, or null if none. */
    val scheduledTimeMinutes: Int? = null,
    val scheduleFailed: Boolean = false,
)
