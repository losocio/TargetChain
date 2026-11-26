package com.targetchain.targetchain.ui.routinerun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.targetchain.targetchain.alarm.RoutineRunner
import com.targetchain.targetchain.appContainer
import com.targetchain.targetchain.chain.RoutineRun
import com.targetchain.targetchain.data.RoutineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Confirmation before "Next task" (or "End routine" during the last task's countdown), asked from
 * the routine list or from the countdown notification. Scoped to MainActivity, so it can be shown
 * over any screen.
 *
 * @property dialog what the dialog says; null while it isn't shown or there's no countdown.
 */
data class AdvanceRoutineUiState(val dialog: AdvanceDialog? = null)

/**
 * @property nextTaskName task whose alarm will ring; null when the routine will end instead.
 * @property taskNumber number of the next task, to name it when it has no name.
 */
data class AdvanceDialog(
    val routineName: String,
    val nextTaskName: String?,
    val taskNumber: Int,
)

class AdvanceRoutineViewModel(
    private val runner: RoutineRunner,
    private val repository: RoutineRepository,
    private val runFlow: StateFlow<RoutineRun>,
) : ViewModel() {

    /**
     * Countdown (its task) the confirmation was asked for. The dialog only shows during that same
     * countdown: once the next task rings or the routine ends, it's gone.
     */
    private val requestedFor = MutableStateFlow<Long?>(null)

    /** What the dialog would say for the current countdown, or null if there's none. */
    private val countdownDialog = runFlow
        .map { run -> run.routineId to run.countdown }
        .distinctUntilChanged()
        .map { (routineId, countdown) ->
            if (routineId == null || countdown == null) return@map null
            val routine = repository.getRoutine(routineId) ?: return@map null
            val tasks = repository.getTasks(routineId)
            val nextIndex = tasks.indexOfFirst { it.id == countdown.nextAlarmId }
            countdown.alarmId to AdvanceDialog(
                routineName = routine.routine.name,
                nextTaskName = tasks.getOrNull(nextIndex)?.name,
                taskNumber = nextIndex + 1,
            )
        }

    val uiState: StateFlow<AdvanceRoutineUiState> =
        combine(requestedFor, countdownDialog) { requested, current ->
            val (alarmId, dialog) = current ?: return@combine AdvanceRoutineUiState()
            AdvanceRoutineUiState(dialog = dialog.takeIf { requested == alarmId })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdvanceRoutineUiState())

    fun onRequest() {
        requestedFor.value = runFlow.value.countdown?.alarmId
    }

    fun onDismiss() {
        requestedFor.value = null
    }

    fun onConfirm() {
        requestedFor.value = null
        viewModelScope.launch { runner.advance() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                AdvanceRoutineViewModel(
                    runner = container.routineRunner,
                    repository = container.routineRepository,
                    runFlow = container.alarmStateStore.runFlow,
                )
            }
        }
    }
}
