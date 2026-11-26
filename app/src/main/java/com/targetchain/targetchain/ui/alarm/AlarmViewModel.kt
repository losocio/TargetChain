package com.targetchain.targetchain.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.targetchain.targetchain.alarm.AlarmReceiver
import com.targetchain.targetchain.alarm.AlarmRingingService
import com.targetchain.targetchain.appContainer
import com.targetchain.targetchain.data.PasswordRepository
import com.targetchain.targetchain.data.RoutineRepository
import com.targetchain.targetchain.data.model.tasks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The task whose alarm rings, and its challenges, shown one by one. Progress lives in [AlarmRingingService]
 * (in memory), so it survives this screen being recreated but not a reboot or a killed process.
 */
class AlarmViewModel(
    private val alarmId: Long,
    private val routineRepository: RoutineRepository,
    private val passwordRepository: PasswordRepository,
) : ViewModel() {

    private val localState = MutableStateFlow(AlarmUiState())

    val uiState: StateFlow<AlarmUiState> = combine(
        localState,
        AlarmRingingService.completedChallenges,
    ) { state, completed ->
        state.copy(completedCount = completed)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, localState.value)

    init {
        viewModelScope.launch {
            val alarm = runCatching { routineRepository.getAlarm(alarmId) }.getOrNull()
            val routine = alarm?.let { routineRepository.getRoutine(it.routineId) }
            val tasks = routine?.tasks.orEmpty()
            val challenges = alarm?.let { routineRepository.getChallenges(it.id) }.orEmpty()
            val passwordNames = passwordRepository.observePasswords().first().associate { it.id to it.name }
            localState.update {
                it.copy(
                    isLoading = false,
                    routineName = routine?.routine?.name.orEmpty(),
                    taskName = alarm?.name.orEmpty(),
                    taskNumber = tasks.indexOfFirst { task -> task.id == alarmId } + 1,
                    taskCount = tasks.size,
                    taskDurationSeconds = alarm?.durationSeconds,
                    challenges = challenges.map { challenge ->
                        AlarmChallenge(
                            type = challenge.type,
                            passwordId = challenge.passwordId,
                            passwordName = challenge.passwordId?.let(passwordNames::get),
                        )
                    },
                )
            }
        }
    }

    fun onPasswordSubmitted(typed: String) {
        val state = uiState.value
        val index = state.progress.currentIndex ?: return
        if (state.isCheckingPassword) return
        val challenge = state.challenges[index]
        localState.update { it.copy(isCheckingPassword = true) }
        viewModelScope.launch {
            val correct = passwordRepository.matches(challenge.passwordId, typed)
            if (correct) AlarmRingingService.completeChallenge(index)
            // A wrong password doesn't count: the tone keeps ringing and the user tries again.
            localState.update { it.copy(isCheckingPassword = false, showWrongPassword = !correct) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val container = app.appContainer
                // The persisted ringing alarm is the source of truth; the intent extra is a fallback.
                val alarmId = container.alarmStateStore.ringingAlarmId
                    ?: createSavedStateHandle().get<Long>(AlarmReceiver.EXTRA_ALARM_ID)
                    ?: AlarmReceiver.NO_ID
                AlarmViewModel(alarmId, container.routineRepository, container.passwordRepository)
            }
        }
    }
}
