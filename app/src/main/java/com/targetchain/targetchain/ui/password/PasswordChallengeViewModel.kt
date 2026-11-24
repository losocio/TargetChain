package com.targetchain.targetchain.ui.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.targetchain.targetchain.appContainer
import com.targetchain.targetchain.challenge.PasswordNameError
import com.targetchain.targetchain.data.PasswordRepository
import com.targetchain.targetchain.data.model.PasswordSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PasswordChallengeUiState(
    val isLoading: Boolean = true,
    val passwords: List<PasswordSummary> = emptyList(),
    val selectedId: Long? = null,
    val renameDialog: RenameDialogState? = null,
    val deleteDialog: DeleteDialogState? = null,
) {
    /** The challenge can only be created with an existing password selected. */
    val canAdd: Boolean
        get() = selectedId != null && passwords.any { it.id == selectedId }
}

data class RenameDialogState(
    val passwordId: Long,
    val name: String,
    val error: PasswordNameError? = null,
)

/**
 * @property usedByAlarms names of the saved alarms that use the password.
 * @property usedInDraft the alarm being edited uses it (not saved yet).
 */
data class DeleteDialogState(
    val passwordId: Long,
    val name: String,
    val usedByAlarms: List<String>,
    val usedInDraft: Boolean,
) {
    val canDelete: Boolean
        get() = usedByAlarms.isEmpty() && !usedInDraft
}

/**
 * Configuration of a password challenge: choose one of the saved passwords. Renaming and deleting
 * passwords apply immediately (they are shared by all alarms).
 */
class PasswordChallengeViewModel(private val repository: PasswordRepository) : ViewModel() {

    private val localState = MutableStateFlow(PasswordChallengeUiState())

    val uiState: StateFlow<PasswordChallengeUiState> = combine(
        localState,
        repository.observePasswords(),
    ) { state, passwords ->
        state.copy(isLoading = false, passwords = passwords)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), localState.value)

    fun onSelect(passwordId: Long) = localState.update { it.copy(selectedId = passwordId) }

    fun onRenameClick(password: PasswordSummary) =
        localState.update { it.copy(renameDialog = RenameDialogState(password.id, password.name)) }

    fun onRenameNameChange(name: String) =
        localState.update { it.copy(renameDialog = it.renameDialog?.copy(name = name, error = null)) }

    fun onRenameConfirm() {
        val dialog = localState.value.renameDialog ?: return
        viewModelScope.launch {
            val error = repository.rename(dialog.passwordId, dialog.name)
            localState.update {
                it.copy(renameDialog = if (error == null) null else it.renameDialog?.copy(error = error))
            }
        }
    }

    /** [usedInDraft]: the alarm being edited has a challenge with this password. */
    fun onDeleteClick(password: PasswordSummary, usedInDraft: Boolean) {
        viewModelScope.launch {
            val usedBy = repository.getAlarmNamesUsing(password.id)
            localState.update {
                it.copy(deleteDialog = DeleteDialogState(password.id, password.name, usedBy, usedInDraft))
            }
        }
    }

    fun onDeleteConfirm() {
        val dialog = localState.value.deleteDialog ?: return
        if (!dialog.canDelete) return
        viewModelScope.launch {
            val deleted = repository.delete(dialog.passwordId)
            localState.update {
                it.copy(
                    deleteDialog = null,
                    selectedId = if (deleted && it.selectedId == dialog.passwordId) null else it.selectedId,
                )
            }
        }
    }

    fun onDialogDismiss() = localState.update { it.copy(renameDialog = null, deleteDialog = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                PasswordChallengeViewModel(this[APPLICATION_KEY]!!.appContainer.passwordRepository)
            }
        }
    }
}
