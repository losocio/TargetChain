package com.targetchain.targetchain.ui.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.targetchain.targetchain.R
import com.targetchain.targetchain.data.model.PasswordSummary

/**
 * Password challenge configuration: saved passwords by name (never the password itself), with
 * rename and delete, and "New password".
 *
 * @param usedPasswordIds passwords used by the alarm being edited, which can't be deleted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordChallengeScreen(
    uiState: PasswordChallengeUiState,
    usedPasswordIds: Set<Long>,
    onSelect: (Long) -> Unit,
    onNewPassword: () -> Unit,
    onRenameClick: (PasswordSummary) -> Unit,
    onRenameNameChange: (String) -> Unit,
    onRenameConfirm: () -> Unit,
    onDeleteClick: (PasswordSummary, usedInDraft: Boolean) -> Unit,
    onDeleteConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.password_challenge_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.navigate_back))
                    }
                },
                actions = {
                    TextButton(onClick = onAdd, enabled = uiState.canAdd) {
                        Text(stringResource(R.string.password_challenge_add))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    stringResource(
                        if (uiState.passwords.isEmpty() && !uiState.isLoading) R.string.password_challenge_empty
                        else R.string.password_challenge_hint
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            items(uiState.passwords, key = { it.id }) { password ->
                PasswordRow(
                    name = password.name,
                    selected = password.id == uiState.selectedId,
                    onSelect = { onSelect(password.id) },
                    onRename = { onRenameClick(password) },
                    onDelete = { onDeleteClick(password, password.id in usedPasswordIds) },
                )
            }
            item {
                OutlinedButton(onClick = onNewPassword, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.password_new))
                }
            }
        }
    }

    uiState.renameDialog?.let { dialog ->
        RenamePasswordDialog(
            dialog = dialog,
            onNameChange = onRenameNameChange,
            onConfirm = onRenameConfirm,
            onDismiss = onDialogDismiss,
        )
    }
    uiState.deleteDialog?.let { dialog ->
        DeletePasswordDialog(dialog = dialog, onConfirm = onDeleteConfirm, onDismiss = onDialogDismiss)
    }
}

@Composable
private fun PasswordRow(
    name: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
                .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onRename) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.password_rename))
            }
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.password_delete))
            }
        }
    }
}

@Composable
private fun RenamePasswordDialog(
    dialog: RenameDialogState,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.password_rename_title)) },
        text = {
            OutlinedTextField(
                value = dialog.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.password_name)) },
                singleLine = true,
                isError = dialog.error != null,
                supportingText = dialog.error?.let { error -> { Text(stringResource(error.messageRes())) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.dialog_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun DeletePasswordDialog(dialog: DeleteDialogState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    if (dialog.canDelete) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.password_delete_title)) },
            text = { Text(stringResource(R.string.password_delete_message, dialog.name)) },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(stringResource(R.string.password_delete)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    } else {
        val defaultName = stringResource(R.string.alarm_default_name)
        val alarms = dialog.usedByAlarms.joinToString { it.ifBlank { defaultName } }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.password_in_use_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dialog.usedByAlarms.isNotEmpty()) {
                        Text(stringResource(R.string.password_in_use_alarms, dialog.name, alarms))
                    }
                    if (dialog.usedInDraft) {
                        Text(stringResource(R.string.password_in_use_draft, dialog.name))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_ok)) }
            },
        )
    }
}
