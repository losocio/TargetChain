package com.targetchain.targetchain.ui.routinerun

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.targetchain.targetchain.R

/** Confirmation before making the next task's alarm ring now, or ending the routine now. */
@Composable
fun AdvanceRoutineDialog(dialog: AdvanceDialog, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val isEnd = dialog.nextTaskName == null
    val title: String
    val message: String
    if (isEnd) {
        title = stringResource(R.string.advance_end_title)
        message = stringResource(
            R.string.advance_end_message,
            dialog.routineName.ifBlank { stringResource(R.string.routine_default_name) },
        )
    } else {
        title = stringResource(R.string.advance_next_title)
        message = stringResource(
            R.string.advance_next_message,
            dialog.nextTaskName.orEmpty().ifBlank { stringResource(R.string.task_default_name, dialog.taskNumber) },
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(if (isEnd) R.string.routine_end else R.string.routine_next_task))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}
