package com.targetchain.targetchain.ui.alarmedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.targetchain.targetchain.BuildConfig
import com.targetchain.targetchain.R
import com.targetchain.targetchain.ui.common.formatTime

@Composable
fun AlarmEditScreen(
    viewModel: AlarmEditViewModel,
    onPickRingtone: (selectedUri: String?) -> Unit,
    onClose: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onClose()
    }

    AlarmEditContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onTimeChange = viewModel::onTimeChange,
        onSetNextMinute = viewModel::onSetNextMinute,
        onPickRingtone = { onPickRingtone(uiState.ringtoneUri) },
        onSave = viewModel::onSave,
        onBack = onClose,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditContent(
    uiState: AlarmEditUiState,
    onNameChange: (String) -> Unit,
    onTimeChange: (Int) -> Unit,
    onSetNextMinute: () -> Unit,
    onPickRingtone: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (uiState.isNew) R.string.alarm_edit_title_new else R.string.alarm_edit_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.navigate_back))
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !uiState.isLoading && !uiState.isSaving) {
                        Text(stringResource(R.string.alarm_edit_save))
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
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.alarm_edit_name)) },
                placeholder = { Text(stringResource(R.string.alarm_default_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
            )

            Card(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.alarm_edit_time), style = MaterialTheme.typography.labelLarge)
                    Text(formatTime(uiState.startTimeMinutes), style = MaterialTheme.typography.displayMedium)
                }
            }
            if (BuildConfig.DEBUG) {
                OutlinedButton(onClick = onSetNextMinute) {
                    Text(stringResource(R.string.alarm_edit_next_minute))
                }
            }

            Card(onClick = onPickRingtone, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.alarm_edit_ringtone), style = MaterialTheme.typography.labelLarge)
                    Text(uiState.ringtoneTitle, style = MaterialTheme.typography.bodyLarge)
                }
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
