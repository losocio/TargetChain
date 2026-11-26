package com.targetchain.targetchain.ui.routineedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.targetchain.targetchain.alarm.AlarmScheduler
import com.targetchain.targetchain.appContainer
import com.targetchain.targetchain.challenge.moved
import com.targetchain.targetchain.chain.RoutineRun
import com.targetchain.targetchain.chain.parseDurationFields
import com.targetchain.targetchain.data.PasswordRepository
import com.targetchain.targetchain.data.RoutineRepository
import com.targetchain.targetchain.data.model.ChallengeConfig
import com.targetchain.targetchain.data.model.TaskDraft
import com.targetchain.targetchain.data.model.tasks
import com.targetchain.targetchain.data.model.toConfig
import com.targetchain.targetchain.ringtone.RingtoneCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime

/**
 * Edits a routine, its tasks and their challenges. Shared by the routine editor, the task editor and
 * their sub-screens (scoped to their navigation graph), so all changes are kept here until saved;
 * leaving the routine editor discards them. Saving enables the routine and schedules its first task.
 */
class RoutineEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: RoutineRepository,
    private val passwordRepository: PasswordRepository,
    private val scheduler: AlarmScheduler,
    private val ringtoneCatalog: RingtoneCatalog,
    runFlow: StateFlow<RoutineRun>,
) : ViewModel() {

    private val routineId: Long? =
        savedStateHandle.get<Long>(ARG_ROUTINE_ID)?.takeIf { it != NEW_ROUTINE_ID }

    /** Saved first task, whose scheduled alarm is replaced on save. */
    private var savedFirstAlarmId: Long? = null

    private val _uiState = MutableStateFlow(
        RoutineEditUiState(isNew = routineId == null, isLoading = true)
    )
    val uiState: StateFlow<RoutineEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val routine = routineId?.let { repository.getRoutine(it) }
            val savedTasks = routine?.tasks.orEmpty()
            savedFirstAlarmId = savedTasks.firstOrNull()?.id
            val tasks = savedTasks.map { alarm ->
                TaskDraft(
                    name = alarm.name,
                    ringtoneUri = alarm.ringtoneUri,
                    durationSeconds = alarm.durationSeconds,
                    challenges = repository.getChallenges(alarm.id).map { it.toConfig() },
                )
            }.ifEmpty { listOf(TaskDraft()) }
            val titles = withContext(Dispatchers.IO) {
                (tasks.map { it.ringtoneUri } + null).distinct().associateWith { ringtoneCatalog.title(it) }
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    name = routine?.routine?.name ?: it.name,
                    startTimeMinutes = savedTasks.firstOrNull()?.startTimeMinutes ?: it.startTimeMinutes,
                    tasks = tasks,
                    ringtoneTitles = titles,
                )
            }
        }
        viewModelScope.launch {
            passwordRepository.observePasswords().collect { passwords ->
                _uiState.update { state -> state.copy(passwordNames = passwords.associate { it.id to it.name }) }
            }
        }
        if (routineId != null) {
            viewModelScope.launch {
                runFlow.collect { run ->
                    _uiState.update { it.copy(isRoutineRunning = run.routineId == routineId) }
                }
            }
        }
    }

    // Routine

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }

    fun onTimeChange(minutesOfDay: Int) = _uiState.update { it.copy(startTimeMinutes = minutesOfDay) }

    /** Debug shortcut to test the routine quickly. */
    fun onSetNextMinute() {
        val next = LocalTime.now().plusMinutes(1)
        onTimeChange(next.hour * 60 + next.minute)
    }

    /** Debug shortcut: every task lasts [DEBUG_DURATION_SECONDS]. */
    fun onSetAllDurationsDebug() =
        _uiState.update { state ->
            state.copy(
                tasks = state.tasks.map { it.copy(durationSeconds = DEBUG_DURATION_SECONDS) },
                durationFields = DurationFields.of(DEBUG_DURATION_SECONDS),
            )
        }

    /** Adds a new task at the end and opens it in the task editor. */
    fun onAddTask() =
        _uiState.update { state ->
            state.copy(
                tasks = state.tasks + TaskDraft(),
                editingTaskIndex = state.tasks.size,
                durationFields = DurationFields(),
            )
        }

    fun onTaskMoved(from: Int, to: Int) =
        _uiState.update { it.copy(tasks = it.tasks.moved(from, to)) }

    /** A routine keeps at least one task. */
    fun onTaskDeleted(index: Int) =
        _uiState.update { state ->
            if (state.tasks.size <= 1) state else state.copy(tasks = state.tasks.filterIndexed { i, _ -> i != index })
        }

    /** Opens task [index] in the task editor. */
    fun onOpenTask(index: Int) =
        _uiState.update { state ->
            state.copy(
                editingTaskIndex = index,
                durationFields = DurationFields.of(state.tasks.getOrNull(index)?.durationSeconds),
            )
        }

    // Editing task

    fun onTaskNameChange(name: String) = updateEditingTask { it.copy(name = name) }

    fun onRingtonePicked(uri: String?) {
        updateEditingTask { it.copy(ringtoneUri = uri) }
        if (uri in _uiState.value.ringtoneTitles) return
        viewModelScope.launch {
            val title = withContext(Dispatchers.IO) { ringtoneCatalog.title(uri) }
            _uiState.update { it.copy(ringtoneTitles = it.ringtoneTitles + (uri to title)) }
        }
    }

    /** Invalid or empty fields leave the task without duration (it can't be saved). */
    fun onDurationFieldsChange(fields: DurationFields) {
        _uiState.update { it.copy(durationFields = fields) }
        updateEditingTask { it.copy(durationSeconds = parseDurationFields(fields.hours, fields.minutes, fields.seconds)) }
    }

    /** Debug shortcut: the editing task lasts [DEBUG_DURATION_SECONDS]. */
    fun onSetDurationDebug() = onDurationFieldsChange(DurationFields.of(DEBUG_DURATION_SECONDS))

    /** Adds a configured challenge at the end of the editing task's list. */
    fun onChallengeAdded(challenge: ChallengeConfig) =
        updateEditingTask { it.copy(challenges = it.challenges + challenge) }

    fun onChallengeMoved(from: Int, to: Int) =
        updateEditingTask { it.copy(challenges = it.challenges.moved(from, to)) }

    fun onChallengeDeleted(index: Int) =
        updateEditingTask { task -> task.copy(challenges = task.challenges.filterIndexed { i, _ -> i != index }) }

    private fun updateEditingTask(transform: (TaskDraft) -> TaskDraft) =
        _uiState.update { state ->
            val index = state.editingTaskIndex
            if (index !in state.tasks.indices) return@update state
            state.copy(tasks = state.tasks.toMutableList().apply { set(index, transform(get(index))) })
        }

    // Save

    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val saved = repository.saveRoutine(
                routineId = routineId,
                name = state.name.trim(),
                startTimeMinutes = state.startTimeMinutes,
                tasks = state.tasks.map { it.copy(name = it.name.trim()) },
            )
            if (saved == null) {
                // The routine started running meanwhile; isRoutineRunning explains it on screen.
                _uiState.update { it.copy(isSaving = false) }
                return@launch
            }
            // The tasks were replaced: the old first task's pending alarm goes too.
            savedFirstAlarmId?.let { scheduler.cancel(it) }
            val first = saved.first()
            savedFirstAlarmId = first.id
            // Only fails without the exact alarm permission; the list then shows it disabled
            // and the permissions card shows what's missing.
            if (!scheduler.schedule(first)) repository.setRoutineEnabled(first.routineId, false)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    companion object {
        const val ARG_ROUTINE_ID = "routineId"
        const val NEW_ROUTINE_ID = -1L

        /** Duration set by the debug buttons: short, but long enough to see the countdown screens. */
        const val DEBUG_DURATION_SECONDS = 3

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val container = app.appContainer
                RoutineEditViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    repository = container.routineRepository,
                    passwordRepository = container.passwordRepository,
                    scheduler = container.alarmScheduler,
                    ringtoneCatalog = container.ringtoneCatalog,
                    runFlow = container.alarmStateStore.runFlow,
                )
            }
        }
    }
}
