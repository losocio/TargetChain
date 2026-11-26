package com.targetchain.targetchain.ui.routineedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.targetchain.targetchain.BuildConfig
import com.targetchain.targetchain.R
import com.targetchain.targetchain.data.model.TaskDraft
import com.targetchain.targetchain.ui.common.formatDuration
import com.targetchain.targetchain.ui.common.formatTime
import com.targetchain.targetchain.ui.common.formatTimeOfDaySeconds

@Composable
fun RoutineEditScreen(
    viewModel: RoutineEditViewModel,
    onOpenTask: () -> Unit,
    onClose: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onClose()
    }

    RoutineEditContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onTimeChange = viewModel::onTimeChange,
        onSetNextMinute = viewModel::onSetNextMinute,
        onSetAllDurationsDebug = viewModel::onSetAllDurationsDebug,
        onOpenTask = { index ->
            viewModel.onOpenTask(index)
            onOpenTask()
        },
        onAddTask = {
            viewModel.onAddTask()
            onOpenTask()
        },
        onMoveTask = viewModel::onTaskMoved,
        onDeleteTask = viewModel::onTaskDeleted,
        onSave = viewModel::onSave,
        onBack = onClose,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineEditContent(
    uiState: RoutineEditUiState,
    onNameChange: (String) -> Unit,
    onTimeChange: (Int) -> Unit,
    onSetNextMinute: () -> Unit,
    onSetAllDurationsDebug: () -> Unit,
    onOpenTask: (Int) -> Unit,
    onAddTask: () -> Unit,
    onMoveTask: (from: Int, to: Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (uiState.isNew) R.string.routine_edit_title_new else R.string.routine_edit_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.navigate_back))
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = uiState.canSave) {
                        Text(stringResource(R.string.routine_edit_save))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.isRoutineRunning) {
                Text(stringResource(R.string.routine_edit_running), color = MaterialTheme.colorScheme.error)
            }
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.routine_edit_name)) },
                placeholder = { Text(stringResource(R.string.routine_default_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
            )

            Card(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.routine_edit_start_time), style = MaterialTheme.typography.labelLarge)
                    Text(formatTime(uiState.startTimeMinutes), style = MaterialTheme.typography.displayMedium)
                }
            }
            if (BuildConfig.DEBUG) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onSetNextMinute) {
                        Text(stringResource(R.string.routine_edit_next_minute))
                    }
                    OutlinedButton(onClick = onSetAllDurationsDebug) {
                        Text(stringResource(R.string.routine_edit_all_debug_duration))
                    }
                }
            }

            Text(stringResource(R.string.routine_edit_tasks), style = MaterialTheme.typography.titleMedium)
            uiState.tasks.forEachIndexed { index, task ->
                TaskRow(
                    number = index + 1,
                    task = task,
                    canMoveUp = index > 0,
                    canMoveDown = index < uiState.tasks.lastIndex,
                    canDelete = uiState.tasks.size > 1,
                    onClick = { onOpenTask(index) },
                    onMoveUp = { onMoveTask(index, index - 1) },
                    onMoveDown = { onMoveTask(index, index + 1) },
                    onDelete = { onDeleteTask(index) },
                )
            }
            Button(onClick = onAddTask) {
                Text(stringResource(R.string.routine_edit_add_task))
            }

            val end = uiState.earliestEnd
            Text(
                when {
                    end == null -> stringResource(R.string.routine_edit_earliest_end_unknown)
                    end.daysLater == 0 -> stringResource(R.string.routine_edit_earliest_end, formatTimeOfDaySeconds(end.secondsOfDay))
                    else -> stringResource(R.string.routine_edit_earliest_end_days, formatTimeOfDaySeconds(end.secondsOfDay), end.daysLater)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!uiState.allTasksComplete) {
                Text(
                    stringResource(R.string.routine_edit_incomplete),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialMinutesOfDay = uiState.startTimeMinutes,
            onConfirm = { minutes ->
                showTimePicker = false
                onTimeChange(minutes)
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun TaskRow(
    number: Int,
    task: TaskDraft,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$number. " + task.name.ifBlank { stringResource(R.string.task_default_name, number) },
                    style = MaterialTheme.typography.titleMedium,
                )
                val duration = task.durationSeconds
                Text(
                    if (duration == null) stringResource(R.string.task_no_duration) else formatDuration(duration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (duration == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (task.challenges.isEmpty()) {
                        stringResource(R.string.task_no_challenges)
                    } else {
                        pluralStringResource(R.plurals.task_edit_challenge_count, task.challenges.size, task.challenges.size)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (task.challenges.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(painterResource(R.drawable.ic_arrow_upward), contentDescription = stringResource(R.string.task_move_up))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(painterResource(R.drawable.ic_arrow_downward), contentDescription = stringResource(R.string.task_move_down))
            }
            IconButton(onClick = onDelete, enabled = canDelete) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.task_delete))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initialMinutesOfDay: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initialMinutesOfDay / 60,
        initialMinute = initialMinutesOfDay % 60,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
        text = { TimePicker(state = state) },
    )
}
