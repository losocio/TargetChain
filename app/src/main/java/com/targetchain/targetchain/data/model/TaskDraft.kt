package com.targetchain.targetchain.data.model

/** A task as configured in the routine editor, before it's saved. */
data class TaskDraft(
    val name: String = "",
    /** Null means the system default alarm tone. */
    val ringtoneUri: String? = null,
    val durationSeconds: Int? = null,
    val challenges: List<ChallengeConfig> = emptyList(),
)
