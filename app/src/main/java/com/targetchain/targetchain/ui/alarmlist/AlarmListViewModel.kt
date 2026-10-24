package com.targetchain.targetchain.ui.alarmlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.targetchain.targetchain.alarm.AlarmScheduler
import com.targetchain.targetchain.alarm.AlarmStateStore
import com.targetchain.targetchain.appContainer
import com.targetchain.targetchain.data.RoutineRepository
import com.targetchain.targetchain.data.model.RoutineWithAlarms
import com.targetchain.targetchain.data.model.firstAlarm
import com.targetchain.targetchain.permissions.AlarmPermissions
import com.targetchain.targetchain.permissions.PermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(
    private val repository: RoutineRepository,
    private val scheduler: AlarmScheduler,
    private val checkPermissions: () -> PermissionStatus,
    private val alarmStateStore: AlarmStateStore,
) : ViewModel() {

    private val permissions = MutableStateFlow(checkPermissions())
    private val scheduleFailed = MutableStateFlow(false)
    private val silentTestMode = MutableStateFlow(alarmStateStore.silentTestMode)

    val uiState: StateFlow<AlarmListUiState> = combine(
        repository.observeRoutines(),
        permissions,
        scheduleFailed,
        silentTestMode,
    ) { routines, permissions, failed, silent ->
        AlarmListUiState(
            alarms = routines.mapNotNull { it.toListItem() }
                .sortedWith(compareBy({ it.startTimeMinutes }, { it.routineId })),
            permissions = permissions,
            scheduleFailed = failed,
            silentTestMode = silent,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        // Known values up front, so the permissions card doesn't flash before the first emission.
        AlarmListUiState(permissions = permissions.value, silentTestMode = silentTestMode.value),
    )

    fun onResume() {
        permissions.value = checkPermissions()
    }

    /**
     * Enabling schedules the alarm; disabling cancels its pending ring. Disabling doesn't stop an
     * alarm that is already ringing: that only happens from the alarm screen.
     */
    fun onEnabledChange(routineId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            val alarm = repository.getRoutine(routineId)?.firstAlarm ?: return@launch
            if (isEnabled) {
                val scheduled = scheduler.schedule(alarm)
                if (scheduled) repository.setRoutineEnabled(routineId, true)
                scheduleFailed.value = !scheduled
            } else {
                scheduler.cancel(alarm.id)
                repository.setRoutineEnabled(routineId, false)
                scheduleFailed.value = false
            }
        }
    }

    /** Like disabling, deleting doesn't stop an alarm that is already ringing. */
    fun onDelete(routineId: Long) {
        viewModelScope.launch {
            repository.getRoutine(routineId)?.firstAlarm?.let { scheduler.cancel(it.id) }
            repository.deleteRoutine(routineId)
        }
    }

    fun onSilentTestModeChange(enabled: Boolean) {
        alarmStateStore.silentTestMode = enabled
        silentTestMode.value = enabled
    }

    private fun RoutineWithAlarms.toListItem(): AlarmListItem? {
        val startTime = firstAlarm?.startTimeMinutes ?: return null
        return AlarmListItem(
            routineId = routine.id,
            name = routine.name,
            startTimeMinutes = startTime,
            isEnabled = routine.isEnabled,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                AlarmListViewModel(
                    repository = app.appContainer.routineRepository,
                    scheduler = app.appContainer.alarmScheduler,
                    checkPermissions = { AlarmPermissions.status(app) },
                    alarmStateStore = app.appContainer.alarmStateStore,
                )
            }
        }
    }
}
