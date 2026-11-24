package com.targetchain.targetchain.ui.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.targetchain.targetchain.appContainer
import com.targetchain.targetchain.challenge.NewPasswordError
import com.targetchain.targetchain.challenge.PasswordNameError
import com.targetchain.targetchain.challenge.validateNewPassword
import com.targetchain.targetchain.data.PasswordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The password text itself stays in the screen's fields; it only reaches this class to be hashed. */
data class NewPasswordUiState(
    val name: String = "",
    val nameError: PasswordNameError? = null,
    val passwordError: NewPasswordError? = null,
    val isCreating: Boolean = false,
    /** Set once created; the screen goes back with this password selected. */
    val createdId: Long? = null,
)

class NewPasswordViewModel(private val repository: PasswordRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NewPasswordUiState())
    val uiState: StateFlow<NewPasswordUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, nameError = null) }

    fun onPasswordChange() = _uiState.update { it.copy(passwordError = null) }

    fun onCreate(password: String, repeated: String) {
        val state = _uiState.value
        if (state.isCreating || state.createdId != null) return
        _uiState.update { it.copy(isCreating = true) }
        viewModelScope.launch {
            val nameError = repository.validateName(state.name)
            val passwordError = validateNewPassword(password, repeated)
            val createdId = if (nameError == null && passwordError == null) {
                repository.create(state.name, password)
            } else {
                null
            }
            _uiState.update {
                it.copy(
                    isCreating = false,
                    createdId = createdId,
                    nameError = nameError
                        // Only fails if the name was taken in the meantime.
                        ?: if (createdId == null && passwordError == null) PasswordNameError.DUPLICATE else null,
                    passwordError = passwordError,
                )
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                NewPasswordViewModel(this[APPLICATION_KEY]!!.appContainer.passwordRepository)
            }
        }
    }
}
