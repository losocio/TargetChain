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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.targetchain.targetchain.BuildConfig
import com.targetchain.targetchain.R

/** One task of the routine being edited: name, tone, challenges and duration. */
@Composable
fun TaskEditScreen(
    viewModel: RoutineEditViewModel,
    onPickRingtone: (selectedUri: String?) -> Unit,
    onOpenChallenges: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TaskEditContent(
        uiState = uiState,
        onNameChange = viewModel::onTaskNameChange,
        onPickRingtone = { onPickRingtone(uiState.editingTask.ringtoneUri) },
        onOpenChallenges = onOpenChallenges,
        onDurationChange = viewModel::onDurationFieldsChange,
        onSetDebugDuration = viewModel::onSetDurationDebug,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditContent(
    uiState: RoutineEditUiState,
    onNameChange: (String) -> Unit,
    onPickRingtone: () -> Unit,
    onOpenChallenges: () -> Unit,
    onDurationChange: (DurationFields) -> Unit,
    onSetDebugDuration: () -> Unit,
    onBack: () -> Unit,
) {
    val task = uiState.editingTask
    val defaultName = stringResource(R.string.task_default_name, uiState.editingTaskIndex + 1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(task.name.ifBlank { defaultName }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.navigate_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = task.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.task_edit_name)) },
                placeholder = { Text(defaultName) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
            )

            Card(onClick = onPickRingtone, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.task_edit_ringtone), style = MaterialTheme.typography.labelLarge)
                    Text(uiState.ringtoneTitles[task.ringtoneUri].orEmpty(), style = MaterialTheme.typography.bodyLarge)
                }
            }

            Card(onClick = onOpenChallenges, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.task_edit_challenges), style = MaterialTheme.typography.labelLarge)
                    Text(
                        if (task.challenges.isNotEmpty()) {
                            pluralStringResource(R.plurals.task_edit_challenge_count, task.challenges.size, task.challenges.size)
                        } else {
                            stringResource(R.string.task_edit_no_challenges)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (task.challenges.isEmpty()) {
                Text(
                    stringResource(R.string.task_edit_challenge_required),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            DurationInput(
                fields = uiState.durationFields,
                isValid = task.durationSeconds != null,
                onChange = onDurationChange,
            )
            if (BuildConfig.DEBUG) {
                OutlinedButton(onClick = onSetDebugDuration) {
                    Text(stringResource(R.string.task_edit_debug_duration))
                }
            }
        }
    }
}

/** Three numeric fields, hours : minutes : seconds. */
@Composable
private fun DurationInput(fields: DurationFields, isValid: Boolean, onChange: (DurationFields) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.task_edit_duration), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.task_edit_duration_hint), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val modifier = Modifier.weight(1f)
            DurationField(fields.hours, R.string.task_edit_hours, !isValid, modifier) { onChange(fields.copy(hours = it)) }
            DurationField(fields.minutes, R.string.task_edit_minutes, !isValid, modifier) { onChange(fields.copy(minutes = it)) }
            DurationField(fields.seconds, R.string.task_edit_seconds, !isValid, modifier) { onChange(fields.copy(seconds = it)) }
        }
        if (!isValid) {
            Text(
                stringResource(if (fields.isBlank) R.string.task_edit_duration_required else R.string.task_edit_duration_invalid),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun DurationField(
    value: String,
    labelRes: Int,
    isError: Boolean,
    modifier: Modifier,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        // Digits only, and short enough to stay a readable number.
        onValueChange = { text -> if (text.length <= 2 && text.all(Char::isDigit)) onValueChange(text) },
        modifier = modifier,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    )
}
