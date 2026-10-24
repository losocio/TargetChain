package com.targetchain.targetchain.ui.alarmlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.targetchain.targetchain.BuildConfig
import com.targetchain.targetchain.R
import com.targetchain.targetchain.ui.common.formatTime
import com.targetchain.targetchain.ui.permissions.PermissionsCard

@Composable
fun AlarmListScreen(
    viewModel: AlarmListViewModel,
    onAddAlarm: () -> Unit,
    onOpenAlarm: (routineId: Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    AlarmListContent(
        uiState = uiState,
        onAddAlarm = onAddAlarm,
        onOpenAlarm = onOpenAlarm,
        onEnabledChange = viewModel::onEnabledChange,
        onDelete = viewModel::onDelete,
        onSilentTestModeChange = viewModel::onSilentTestModeChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmListContent(
    uiState: AlarmListUiState,
    onAddAlarm: () -> Unit,
    onOpenAlarm: (Long) -> Unit,
    onEnabledChange: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onSilentTestModeChange: (Boolean) -> Unit,
) {
    var alarmToDelete by remember { mutableStateOf<AlarmListItem?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.alarm_list_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAlarm) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.alarm_list_add))
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
                    Text(
                        stringResource(R.string.alarm_schedule_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (uiState.alarms.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.alarm_list_empty),
                        modifier = Modifier.padding(vertical = 24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            items(uiState.alarms, key = { it.routineId }) { alarm ->
                AlarmRow(
                    alarm = alarm,
                    onClick = { onOpenAlarm(alarm.routineId) },
                    onEnabledChange = { onEnabledChange(alarm.routineId, it) },
                    onDelete = { alarmToDelete = alarm },
                )
            }
            if (BuildConfig.DEBUG) {
                item {
                    SilentTestModeRow(
                        enabled = uiState.silentTestMode,
                        onEnabledChange = onSilentTestModeChange,
                    )
                }
            }
        }
    }

    alarmToDelete?.let { alarm ->
        DeleteAlarmDialog(
            alarmName = alarm.displayName(),
            onConfirm = {
                alarmToDelete = null
                onDelete(alarm.routineId)
            },
            onDismiss = { alarmToDelete = null },
        )
    }
}

@Composable
private fun AlarmRow(
    alarm: AlarmListItem,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formatTime(alarm.startTimeMinutes), style = MaterialTheme.typography.headlineMedium)
                Text(alarm.displayName(), style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = alarm.isEnabled, onCheckedChange = onEnabledChange)
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.alarm_delete))
            }
        }
    }
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
            Text(
                stringResource(R.string.silent_test_mode_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
private fun DeleteAlarmDialog(alarmName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.alarm_delete_title)) },
        text = { Text(stringResource(R.string.alarm_delete_message, alarmName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.alarm_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun AlarmListItem.displayName(): String =
    name.ifBlank { stringResource(R.string.alarm_default_name) }
