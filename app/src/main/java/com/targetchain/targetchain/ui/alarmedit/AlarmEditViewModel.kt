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
import com.targetchain.targetchain.data.RoutineRepository
import com.targetchain.targetchain.data.model.firstAlarm
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
 * Edits a single-alarm routine. Changes are kept here until saved; saving enables the alarm and
 * schedules it.
 */
class AlarmEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: RoutineRepository,
    private val scheduler: AlarmScheduler,
    private val ringtoneCatalog: RingtoneCatalog,
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
            val ringtoneUri = alarm?.ringtoneUri
            val ringtoneTitle = withContext(Dispatchers.IO) { ringtoneCatalog.title(ringtoneUri) }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    name = routine?.routine?.name ?: it.name,
                    startTimeMinutes = alarm?.startTimeMinutes ?: it.startTimeMinutes,
                    ringtoneUri = ringtoneUri,
                    ringtoneTitle = ringtoneTitle,
                )
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

    fun onSave() {
        val state = _uiState.value
        if (state.isLoading || state.isSaving || state.isSaved) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val alarm = repository.saveSingleAlarmRoutine(
                routineId = routineId,
                name = state.name.trim(),
                startTimeMinutes = state.startTimeMinutes,
                ringtoneUri = state.ringtoneUri,
            )
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
                AlarmEditViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    repository = app.appContainer.routineRepository,
                    scheduler = app.appContainer.alarmScheduler,
                    ringtoneCatalog = app.appContainer.ringtoneCatalog,
                )
            }
        }
    }
}
