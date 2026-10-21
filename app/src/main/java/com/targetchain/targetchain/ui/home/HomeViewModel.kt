package com.targetchain.targetchain.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.targetchain.targetchain.alarm.AlarmScheduler
import com.targetchain.targetchain.appContainer
import com.targetchain.targetchain.data.RoutineRepository
import com.targetchain.targetchain.permissions.AlarmPermissions
import com.targetchain.targetchain.permissions.PermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

class HomeViewModel(
    private val repository: RoutineRepository,
    private val scheduler: AlarmScheduler,
    private val checkPermissions: () -> PermissionStatus,
) : ViewModel() {

    private val permissions = MutableStateFlow(checkPermissions())
    private val scheduleFailed = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        permissions,
        repository.observeTestAlarm(),
        scheduleFailed,
    ) { permissions, alarm, failed ->
        HomeUiState(
            permissions = permissions,
            scheduledTimeMinutes = alarm?.startTimeMinutes,
            scheduleFailed = failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onResume() {
        permissions.value = checkPermissions()
    }

    fun onScheduleAt(minutesOfDay: Int) {
        viewModelScope.launch {
            val alarm = repository.saveTestAlarm(minutesOfDay)
            val scheduled = scheduler.schedule(alarm)
            if (!scheduled) repository.disableTestRoutine()
            scheduleFailed.value = !scheduled
        }
    }

    fun onScheduleInOneMinute() {
        val next = LocalTime.now().plusMinutes(1)
        onScheduleAt(next.hour * 60 + next.minute)
    }

    fun onCancel() {
        viewModelScope.launch {
            repository.observeTestAlarm().first()?.let { scheduler.cancel(it.id) }
            repository.disableTestRoutine()
            scheduleFailed.value = false
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                HomeViewModel(
                    repository = app.appContainer.routineRepository,
                    scheduler = app.appContainer.alarmScheduler,
                    checkPermissions = { AlarmPermissions.status(app) },
                )
            }
        }
    }
}
