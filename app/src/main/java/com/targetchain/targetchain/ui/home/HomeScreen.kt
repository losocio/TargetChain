package com.targetchain.targetchain.ui.home

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.targetchain.targetchain.R
import com.targetchain.targetchain.permissions.AlarmPermissions
import com.targetchain.targetchain.permissions.PermissionStatus
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Temporary debug screen: permission status and a single test alarm. */
@Composable
fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    HomeContent(
        uiState = uiState,
        onScheduleAt = viewModel::onScheduleAt,
        onScheduleInOneMinute = viewModel::onScheduleInOneMinute,
        onCancel = viewModel::onCancel,
        modifier = modifier,
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onScheduleAt: (Int) -> Unit,
    onScheduleInOneMinute: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PermissionsCard(uiState.permissions)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.test_alarm_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    uiState.scheduledTimeMinutes
                        ?.let { stringResource(R.string.test_alarm_scheduled_at, formatTime(it)) }
                        ?: stringResource(R.string.test_alarm_none)
                )
                if (uiState.scheduleFailed) {
                    Text(
                        stringResource(R.string.test_alarm_schedule_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showTimePicker = true }) {
                        Text(stringResource(R.string.test_alarm_pick_time))
                    }
                    Button(onClick = onScheduleInOneMinute) {
                        Text(stringResource(R.string.test_alarm_in_one_minute))
                    }
                }
                if (uiState.scheduledTimeMinutes != null) {
                    OutlinedButton(onClick = onCancel) {
                        Text(stringResource(R.string.test_alarm_cancel))
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onConfirm = { minutes ->
                showTimePicker = false
                onScheduleAt(minutes)
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun PermissionsCard(permissions: PermissionStatus) {
    val context = LocalContext.current
    val openSettings = { intent: Intent -> context.startActivity(intent) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Denied twice: the system no longer shows the dialog, so go to settings.
        if (!granted) openSettings(AlarmPermissions.notificationSettingsIntent(context))
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.permissions_title), style = MaterialTheme.typography.titleMedium)
            PermissionRow(R.string.permission_notifications, permissions.notifications) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openSettings(AlarmPermissions.notificationSettingsIntent(context))
                }
            }
            PermissionRow(R.string.permission_exact_alarms, permissions.exactAlarms) {
                openSettings(AlarmPermissions.exactAlarmSettingsIntent(context))
            }
            PermissionRow(R.string.permission_full_screen, permissions.fullScreenIntent) {
                openSettings(AlarmPermissions.fullScreenIntentSettingsIntent(context))
            }
            PermissionRow(R.string.permission_display_over_apps, permissions.displayOverOtherApps) {
                openSettings(AlarmPermissions.displayOverOtherAppsSettingsIntent(context))
            }
            PermissionRow(R.string.permission_battery, permissions.ignoringBatteryOptimizations) {
                openSettings(AlarmPermissions.batteryOptimizationSettingsIntent(context))
            }
            Text(
                stringResource(R.string.permission_battery_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PermissionRow(labelRes: Int, granted: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(labelRes),
            modifier = Modifier.weight(1f),
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        if (granted) {
            Text(stringResource(R.string.permission_granted))
        } else {
            TextButton(onClick = onGrant) { Text(stringResource(R.string.permission_grant)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val now = LocalTime.now()
    val state = rememberTimePickerState(initialHour = now.hour, initialMinute = now.minute)
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

private fun formatTime(minutesOfDay: Int): String =
    LocalTime.of(minutesOfDay / 60, minutesOfDay % 60)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
