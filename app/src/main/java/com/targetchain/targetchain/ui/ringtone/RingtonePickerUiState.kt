package com.targetchain.targetchain.ui.ringtone

import com.targetchain.targetchain.ringtone.RingtoneOption

data class RingtonePickerUiState(
    val isLoading: Boolean = true,
    /** The default tone first, then the device's alarm tones. */
    val ringtones: List<RingtoneOption> = emptyList(),
    /** Null means the system default alarm tone. */
    val selectedUri: String? = null,
)
