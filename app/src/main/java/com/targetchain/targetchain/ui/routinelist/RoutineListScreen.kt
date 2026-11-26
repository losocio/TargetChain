package com.targetchain.targetchain.ui.routinelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.targetchain.targetchain.BuildConfig
import com.targetchain.targetchain.R
import com.targetchain.targetchain.chain.formatCountdown
import com.targetchain.targetchain.ui.common.formatTime
import com.targetchain.targetchain.ui.permissions.PermissionsCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RoutineListScreen(
    viewModel: RoutineListViewModel,
    onAddRoutine: () -> Unit,
    onOpenRoutine: (routineId: Long) -> Unit,
    onRequestAdvance: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    RoutineListContent(
        uiState = uiState,
        onAddRoutine = onAddRoutine,
        onOpenRoutine = onOpenRoutine,
        onEnabledChange = viewModel::onEnabledChange,
        onDelete = viewModel::onDelete,
        onRequestAdvance = onRequestAdvance,
        onStartNow = viewModel::onStartNow,
        onDebugEndRoutine = viewModel::onDebugEndRoutine,
        onSilentTestModeChange = viewModel::onSilentTestModeChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineListContent(
    uiState: RoutineListUiState,
    onAddRoutine: () -> Unit,
    onOpenRoutine: (Long) -> Unit,
    onEnabledChange: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onRequestAdvance: () -> Unit,
    onStartNow: (Long) -> Unit,
    onDebugEndRoutine: () -> Unit,
    onSilentTestModeChange: (Boolean) -> Unit,
) {
    var routineToDelete by remember { mutableStateOf<RoutineListItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val runningMessage = stringResource(R.string.routine_running_locked)

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.routine_list_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRoutine) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.routine_list_add))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            // Extra bottom space so the last row isn't hidden behind the add button.
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.showPermissions) {
                item { PermissionsCard(uiState.permissions) }
            }
            if (uiState.scheduleFailed) {
                item {
                    Text(stringResource(R.string.routine_schedule_failed), color = MaterialTheme.colorScheme.error)
                }
            }
            if (uiState.routines.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.routine_list_empty),
                        modifier = Modifier.padding(vertical = 24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            items(uiState.routines, key = { it.routineId }) { routine ->
                RoutineRow(
                    routine = routine,
                    onClick = {
                        if (routine.isRunning) {
                            scope.launch { snackbarHostState.showSnackbar(runningMessage) }
                        } else {
                            onOpenRoutine(routine.routineId)
                        }
                    },
                    onEnabledChange = { onEnabledChange(routine.routineId, it) },
                    onDelete = { routineToDelete = routine },
                    onRequestAdvance = onRequestAdvance,
                    onStartNow = { onStartNow(routine.routineId) },
                    onDebugEndRoutine = onDebugEndRoutine,
                )
            }
            if (BuildConfig.DEBUG) {
                item {
                    SilentTestModeRow(enabled = uiState.silentTestMode, onEnabledChange = onSilentTestModeChange)
                }
            }
        }
    }

    routineToDelete?.let { routine ->
        DeleteRoutineDialog(
            routineName = routine.displayName(),
            onConfirm = {
                routineToDelete = null
                onDelete(routine.routineId)
            },
            onDismiss = { routineToDelete = null },
        )
    }
}

@Composable
private fun RoutineRow(
    routine: RoutineListItem,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onRequestAdvance: () -> Unit,
    onStartNow: () -> Unit,
    onDebugEndRoutine: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(formatTime(routine.startTimeMinutes), style = MaterialTheme.typography.headlineMedium)
                    Text(routine.displayName(), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        pluralStringResource(R.plurals.routine_task_count, routine.taskCount, routine.taskCount),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = routine.isEnabled, onCheckedChange = onEnabledChange, enabled = !routine.isRunning)
                IconButton(onClick = onDelete, enabled = !routine.isRunning) {
                    Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.routine_delete))
                }
            }

            val running = routine.running
            if (running != null) {
                RunningStatus(running, routine.taskCount)
            }
            val inCountdown = running?.countdownEndAtWallMs != null
            if (inCountdown || (BuildConfig.DEBUG && running == null)) {
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (running != null && inCountdown) {
                        FilledTonalButton(onClick = onRequestAdvance) {
                            Text(stringResource(if (running.isLastTask) R.string.routine_end else R.string.routine_next_task))
                        }
                        if (BuildConfig.DEBUG) {
                            OutlinedButton(onClick = onDebugEndRoutine) {
                                Text(stringResource(R.string.routine_end_debug))
                            }
                        }
                    }
                    if (BuildConfig.DEBUG && running == null) {
                        OutlinedButton(onClick = onStartNow) {
                            Text(stringResource(R.string.routine_start_now_debug))
                        }
                    }
                }
            }
        }
    }
}

/** "Task 2 of 5 · Ringing", or the time left during a countdown, updated every second. */
@Composable
private fun RunningStatus(running: RunningTask, taskCount: Int) {
    val endAt = running.countdownEndAtWallMs
    val text = if (endAt == null) {
        stringResource(R.string.routine_running_ringing, running.taskNumber, taskCount)
    } else {
        val now by produceState(System.currentTimeMillis(), endAt) {
            while (true) {
                value = System.currentTimeMillis()
                delay(1_000 - value % 1_000)
            }
        }
        // Rounded up, so it shows 00:01 during the last second and never 00:00 before it ends.
        val secondsLeft = (endAt - now + 999) / 1_000
        stringResource(R.string.routine_running_countdown, running.taskNumber, taskCount, formatCountdown(secondsLeft))
    }
    Text(
        text,
        modifier = Modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SilentTestModeRow(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.silent_test_mode))
            Text(stringResource(R.string.silent_test_mode_hint), style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
private fun DeleteRoutineDialog(routineName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.routine_delete_title)) },
        text = { Text(stringResource(R.string.routine_delete_message, routineName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.routine_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun RoutineListItem.displayName(): String =
    name.ifBlank { stringResource(R.string.routine_default_name) }
