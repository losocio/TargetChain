package com.targetchain.targetchain.ui.alarm

import com.targetchain.targetchain.challenge.ChallengeProgress
import com.targetchain.targetchain.data.model.ChallengeType

data class AlarmUiState(
    val isLoading: Boolean = true,
    val routineName: String = "",
    /** Name of the task that starts; blank is shown as "Task <number>". */
    val taskName: String = "",
    /** Position of the task in the routine (1-based); 0 if unknown. */
    val taskNumber: Int = 0,
    val taskCount: Int = 0,
    /** Countdown that starts once the challenges are completed. */
    val taskDurationSeconds: Int? = null,
    val challenges: List<AlarmChallenge> = emptyList(),
    val completedCount: Int = 0,
    val isCheckingPassword: Boolean = false,
    val showWrongPassword: Boolean = false,
) {
    val progress: ChallengeProgress
        get() = ChallengeProgress(total = challenges.size, completed = completedCount)

    val currentChallenge: AlarmChallenge?
        get() = progress.currentIndex?.let(challenges::get)

    /** The last challenge was completed: the alarm stops. */
    val isFinished: Boolean
        get() = !isLoading && progress.isFinished

    /**
     * No challenge could be read. Can't happen through the UI (an alarm can't be saved without
     * challenges), but the user must never be left without a way to stop the alarm.
     */
    val hasNoChallenges: Boolean
        get() = !isLoading && challenges.isEmpty()
}

data class AlarmChallenge(
    val type: ChallengeType,
    val passwordId: Long?,
    val passwordName: String?,
)
