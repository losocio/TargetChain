package com.targetchain.targetchain.ui.alarmedit

data class AlarmEditUiState(
    val isNew: Boolean = true,
    /** True while the alarm and its ringtone title are being read; the form isn't shown until then. */
    val isLoading: Boolean = true,
    val name: String = "",
    /** Minutes since midnight. */
    val startTimeMinutes: Int = DEFAULT_START_TIME_MINUTES,
    /** Null means the system default alarm tone. */
    val ringtoneUri: String? = null,
    val ringtoneTitle: String = "",
    val isSaving: Boolean = false,
    /** Set once the alarm is saved and scheduled; the screen then closes. */
    val isSaved: Boolean = false,
) {
    companion object {
        const val DEFAULT_START_TIME_MINUTES = 7 * 60
    }
}
