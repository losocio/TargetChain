package com.targetchain.targetchain.ui.ringtone

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.targetchain.targetchain.BuildConfig
import com.targetchain.targetchain.appContainer
import com.targetchain.targetchain.ringtone.RingtoneCatalog
import com.targetchain.targetchain.ringtone.RingtonePreviewPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Lists the preinstalled alarm tones and previews the one the user taps. */
class RingtonePickerViewModel(
    savedStateHandle: SavedStateHandle,
    private val ringtoneCatalog: RingtoneCatalog,
    private val previewPlayer: RingtonePreviewPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RingtonePickerUiState(selectedUri = savedStateHandle.get<String>(ARG_SELECTED_URI))
    )
    val uiState: StateFlow<RingtonePickerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val ringtones = withContext(Dispatchers.IO) { ringtoneCatalog.alarmTones() }
            _uiState.update { it.copy(isLoading = false, ringtones = ringtones) }
        }
    }

    fun onSelect(uri: String?) {
        _uiState.update { it.copy(selectedUri = uri) }
        previewPlayer.play(uri)
    }

    /** Called when the screen is no longer visible (left, or the app went to the background). */
    fun onStopPreview() = previewPlayer.stop()

    override fun onCleared() {
        previewPlayer.release()
    }

    companion object {
        const val ARG_SELECTED_URI = "selectedUri"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val alarmStateStore = app.appContainer.alarmStateStore
                RingtonePickerViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    ringtoneCatalog = app.appContainer.ringtoneCatalog,
                    // Silent test mode (debug builds only) mutes the preview too.
                    previewPlayer = RingtonePreviewPlayer(app) {
                        BuildConfig.DEBUG && alarmStateStore.silentTestMode
                    },
                )
            }
        }
    }
}
