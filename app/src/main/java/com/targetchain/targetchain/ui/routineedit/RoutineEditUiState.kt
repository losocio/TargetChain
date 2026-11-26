package com.targetchain.targetchain.ui.routineedit

import com.targetchain.targetchain.chain.EarliestEnd
import com.targetchain.targetchain.chain.earliestEnd
import com.targetchain.targetchain.chain.isTaskComplete
import com.targetchain.targetchain.chain.splitDuration
import com.targetchain.targetchain.data.model.TaskDraft

/** The whole routine being edited, tasks and challenges included. Nothing is saved until Save. */
data class RoutineEditUiState(
    val isNew: Boolean = true,
    /** True while the routine is being read; the form isn't shown until then. */
    val isLoading: Boolean = true,
    val name: String = "",
    /** Minutes since midnight. */
    val startTimeMinutes: Int = DEFAULT_START_TIME_MINUTES,
    /** Tasks in the order they run. A routine always has at least one. */
    val tasks: List<TaskDraft> = listOf(TaskDraft()),
    /** Titles of the tasks' tones, by tone URI (null = default alarm tone). */
    val ringtoneTitles: Map<String?, String> = emptyMap(),
    /** Names of the saved passwords, by id, to show password challenges. */
    val passwordNames: Map<Long, String> = emptyMap(),
    /** Task open in the task editor and its challenge screens. */
    val editingTaskIndex: Int = 0,
    /** What is typed in the editing task's H : M : S duration fields. */
    val durationFields: DurationFields = DurationFields(),
    /** The routine is running: it can't be saved until it ends. */
    val isRoutineRunning: Boolean = false,
    val isSaving: Boolean = false,
    /** Set once the routine is saved and scheduled; the screen then closes. */
    val isSaved: Boolean = false,
) {
    val editingTask: TaskDraft
        get() = tasks.getOrElse(editingTaskIndex) { TaskDraft() }

    val canSave: Boolean
        get() = !isLoading && !isSaving && !isSaved && !isRoutineRunning && allTasksComplete

    val allTasksComplete: Boolean
        get() = tasks.isNotEmpty() && tasks.all { it.isComplete }

    val earliestEnd: EarliestEnd?
        get() = earliestEnd(startTimeMinutes, tasks.map { it.durationSeconds })

    /** Saved passwords used by the challenges being edited; they can't be deleted meanwhile. */
    val usedPasswordIds: Set<Long>
        get() = tasks.flatMap { it.challenges }.mapNotNullTo(mutableSetOf()) { it.passwordId }

    companion object {
        const val DEFAULT_START_TIME_MINUTES = 7 * 60
    }
}

val TaskDraft.isComplete: Boolean
    get() = isTaskComplete(challenges.size, durationSeconds)

/** Text of the three duration fields. Empty fields count as 0. */
data class DurationFields(val hours: String = "", val minutes: String = "", val seconds: String = "") {

    val isBlank: Boolean
        get() = hours.isBlank() && minutes.isBlank() && seconds.isBlank()

    companion object {
        /** Fields showing [seconds]; empty when there's no duration yet. */
        fun of(seconds: Int?): DurationFields {
            if (seconds == null) return DurationFields()
            val (h, m, s) = splitDuration(seconds.toLong())
            return DurationFields(h.toString(), m.toString(), s.toString())
        }
    }
}
