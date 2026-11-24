package com.targetchain.targetchain.ui.alarmedit

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
import com.targetchain.targetchain.data.PasswordRepository
import com.targetchain.targetchain.data.RoutineRepository
import com.targetchain.targetchain.data.model.ChallengeConfig
import com.targetchain.targetchain.data.model.firstAlarm
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
 * Edits a single-alarm routine and its challenges. Shared by the editor and its challenge screens
 * (scoped to their navigation graph), so all changes are kept here until saved; leaving the editor
 * discards them. Saving enables the alarm and schedules it.
 */
class AlarmEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: RoutineRepository,
    private val passwordRepository: PasswordRepository,
    private val scheduler: AlarmScheduler,
    private val ringtoneCatalog: RingtoneCatalog,
    activeRoutineId: StateFlow<Long?>,
) : ViewModel() {

    private val routineId: Long? =
        savedStateHandle.get<Long>(ARG_ROUTINE_ID)?.takeIf { it != NEW_ROUTINE_ID }

    private val _uiState = MutableStateFlow(
        AlarmEditUiState(isNew = routineId == null, isLoading = true)
    )
    val uiState: StateFlow<AlarmEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val routine = routineId?.let { repository.getRoutine(it) }
            val alarm = routine?.firstAlarm
            val challenges = alarm?.let { repository.getChallenges(it.id) }.orEmpty().map { it.toConfig() }
            val ringtoneUri = alarm?.ringtoneUri
            val ringtoneTitle = withContext(Dispatchers.IO) { ringtoneCatalog.title(ringtoneUri) }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    name = routine?.routine?.name ?: it.name,
                    startTimeMinutes = alarm?.startTimeMinutes ?: it.startTimeMinutes,
                    ringtoneUri = ringtoneUri,
                    ringtoneTitle = ringtoneTitle,
                    challenges = challenges,
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
                activeRoutineId.collect { active ->
                    _uiState.update { it.copy(isRoutineRunning = active == routineId) }
                }
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }

    fun onTimeChange(minutesOfDay: Int) = _uiState.update { it.copy(startTimeMinutes = minutesOfDay) }

    /** Debug shortcut to test the alarm quickly. */
    fun onSetNextMinute() {
        val next = LocalTime.now().plusMinutes(1)
        onTimeChange(next.hour * 60 + next.minute)
    }

    fun onRingtonePicked(uri: String?) {
        _uiState.update { it.copy(ringtoneUri = uri) }
        viewModelScope.launch {
            val title = withContext(Dispatchers.IO) { ringtoneCatalog.title(uri) }
            // Ignore the result if another tone was picked meanwhile.
            _uiState.update { if (it.ringtoneUri == uri) it.copy(ringtoneTitle = title) else it }
        }
    }

    /** Adds a configured challenge at the end of the list. */
    fun onChallengeAdded(challenge: ChallengeConfig) =
        _uiState.update { it.copy(challenges = it.challenges + challenge) }

    fun onChallengeMoved(from: Int, to: Int) =
        _uiState.update { it.copy(challenges = it.challenges.moved(from, to)) }

    fun onChallengeDeleted(index: Int) =
        _uiState.update { state ->
            state.copy(challenges = state.challenges.filterIndexed { i, _ -> i != index })
        }

    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val alarm = repository.saveSingleAlarmRoutine(
                routineId = routineId,
                name = state.name.trim(),
                startTimeMinutes = state.startTimeMinutes,
                ringtoneUri = state.ringtoneUri,
                challenges = state.challenges,
            )
            if (alarm == null) {
                // Its routine started running meanwhile; isRoutineRunning explains it on screen.
                _uiState.update { it.copy(isSaving = false) }
                return@launch
            }
            // Only fails without the exact alarm permission; the list then shows it disabled
            // and the permissions card shows what's missing.
            if (!scheduler.schedule(alarm)) repository.setRoutineEnabled(alarm.routineId, false)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    companion object {
        const val ARG_ROUTINE_ID = "routineId"
        const val NEW_ROUTINE_ID = -1L

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val container = app.appContainer
                AlarmEditViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    repository = container.routineRepository,
                    passwordRepository = container.passwordRepository,
                    scheduler = container.alarmScheduler,
                    ringtoneCatalog = container.ringtoneCatalog,
                    activeRoutineId = container.alarmStateStore.activeRoutineIdFlow,
                )
            }
        }
    }
}
