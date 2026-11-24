package com.targetchain.targetchain.ui.alarmedit

import com.targetchain.targetchain.data.model.ChallengeConfig

/** The whole alarm being edited, challenges included. Nothing is saved until Save. */
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
    /** Challenges in the order they'll be shown. */
    val challenges: List<ChallengeConfig> = emptyList(),
    /** Names of the saved passwords, by id, to show password challenges. */
    val passwordNames: Map<Long, String> = emptyMap(),
    /** The alarm's routine is running: it can't be saved until it ends. */
    val isRoutineRunning: Boolean = false,
    val isSaving: Boolean = false,
    /** Set once the alarm is saved and scheduled; the screen then closes. */
    val isSaved: Boolean = false,
) {
    val hasChallenges: Boolean
        get() = challenges.isNotEmpty()

    val canSave: Boolean
        get() = !isLoading && !isSaving && !isSaved && hasChallenges && !isRoutineRunning

    /** Saved passwords used by the challenges being edited; they can't be deleted meanwhile. */
    val usedPasswordIds: Set<Long>
        get() = challenges.mapNotNullTo(mutableSetOf()) { it.passwordId }

    companion object {
        const val DEFAULT_START_TIME_MINUTES = 7 * 60
    }
}
