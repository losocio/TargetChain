package com.targetchain.targetchain.ui.routinelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.targetchain.targetchain.alarm.AlarmScheduler
import com.targetchain.targetchain.alarm.AlarmStateStore
import com.targetchain.targetchain.alarm.RoutineRunner
import com.targetchain.targetchain.appContainer
import com.targetchain.targetchain.chain.RoutineRun
import com.targetchain.targetchain.data.RoutineRepository
import com.targetchain.targetchain.data.model.RoutineWithAlarms
import com.targetchain.targetchain.data.model.firstAlarm
import com.targetchain.targetchain.data.model.tasks
import com.targetchain.targetchain.permissions.AlarmPermissions
import com.targetchain.targetchain.permissions.PermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutineListViewModel(
    private val repository: RoutineRepository,
    private val scheduler: AlarmScheduler,
    private val runner: RoutineRunner,
    private val checkPermissions: () -> PermissionStatus,
    private val alarmStateStore: AlarmStateStore,
) : ViewModel() {

    private val permissions = MutableStateFlow(checkPermissions())
    private val scheduleFailed = MutableStateFlow(false)
    private val silentTestMode = MutableStateFlow(alarmStateStore.silentTestMode)

    val uiState: StateFlow<RoutineListUiState> = combine(
        repository.observeRoutines(),
        permissions,
        scheduleFailed,
        silentTestMode,
        alarmStateStore.runFlow,
    ) { routines, permissions, failed, silent, run ->
        RoutineListUiState(
            routines = routines.mapNotNull { it.toListItem(run) }
                .sortedWith(compareBy({ it.startTimeMinutes }, { it.routineId })),
            permissions = permissions,
            scheduleFailed = failed,
            silentTestMode = silent,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        // Known values up front, so the permissions card doesn't flash before the first emission.
        RoutineListUiState(permissions = permissions.value, silentTestMode = silentTestMode.value),
    )

    fun onResume() {
        permissions.value = checkPermissions()
    }

    /**
     * Enabling schedules the routine's first task; disabling cancels it. A running routine can't
     * be changed at all.
     */
    fun onEnabledChange(routineId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            if (repository.isRoutineRunning(routineId)) return@launch
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

    /** A running routine can't be deleted. */
    fun onDelete(routineId: Long) {
        viewModelScope.launch {
            if (repository.isRoutineRunning(routineId)) return@launch
            repository.getRoutine(routineId)?.firstAlarm?.let { scheduler.cancel(it.id) }
            repository.deleteRoutine(routineId)
        }
    }

    /** Debug builds only: the routine's first task rings now, as if its start time had arrived. */
    fun onStartNow(routineId: Long) {
        viewModelScope.launch {
            val first = repository.getRoutine(routineId)?.firstAlarm ?: return@launch
            runner.onAlarmFired(routineId, first.id, isRoutineStart = true, isWatchdog = false)
        }
    }

    /** Debug builds only: ends the running routine now, without confirmation. */
    fun onDebugEndRoutine() {
        viewModelScope.launch { runner.endRoutine() }
    }

    fun onSilentTestModeChange(enabled: Boolean) {
        alarmStateStore.silentTestMode = enabled
        silentTestMode.value = enabled
    }

    private fun RoutineWithAlarms.toListItem(run: RoutineRun): RoutineListItem? {
        val tasks = tasks
        val startTime = tasks.firstOrNull()?.startTimeMinutes ?: return null
        val running = if (run.routineId == routine.id) {
            val currentAlarmId = run.ringingAlarmId ?: run.countdown?.alarmId
            val index = tasks.indexOfFirst { it.id == currentAlarmId }
            RunningTask(
                taskNumber = index + 1,
                countdownEndAtWallMs = run.countdown?.timing?.endAtWallMs,
                isLastTask = run.countdown != null && run.countdown.nextAlarmId == null,
            )
        } else {
            null
        }
        return RoutineListItem(
            routineId = routine.id,
            name = routine.name,
            startTimeMinutes = startTime,
            taskCount = tasks.size,
            isEnabled = routine.isEnabled,
            running = running,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val container = app.appContainer
                RoutineListViewModel(
                    repository = container.routineRepository,
                    scheduler = container.alarmScheduler,
                    runner = container.routineRunner,
                    checkPermissions = { AlarmPermissions.status(app) },
                    alarmStateStore = container.alarmStateStore,
                )
            }
        }
    }
}
