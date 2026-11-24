package com.targetchain.targetchain.ui.alarm

import android.media.AudioManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.targetchain.targetchain.R
import com.targetchain.targetchain.data.model.ChallengeType
import com.targetchain.targetchain.ui.common.NoTextToolbar
import com.targetchain.targetchain.ui.common.PasswordKeyboardOptions
import com.targetchain.targetchain.ui.common.TypedByHandOnly
import com.targetchain.targetchain.ui.theme.TargetChainTheme
import kotlinx.coroutines.delay

/**
 * Alarm screen: the alarm's challenges one by one. Every challenge type is shown inside this
 * screen, so the alarm screen's anti-evasion measures apply to all of them.
 *
 * @param showStopButton the direct Stop button: debug builds, or an alarm without challenges.
 * @param debugText extra info shown only in debug builds (e.g. the current alarm volume).
 */
@Composable
fun AlarmScreen(
    uiState: AlarmUiState,
    onPasswordSubmit: (String) -> Unit,
    onStop: () -> Unit,
    showStopButton: Boolean,
    modifier: Modifier = Modifier,
    debugText: String? = null,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.alarm_ringing_title),
                style = MaterialTheme.typography.displaySmall,
            )
            if (uiState.alarmName.isNotBlank()) {
                Text(uiState.alarmName, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(32.dp))

            val challenge = uiState.currentChallenge
            val index = uiState.progress.currentIndex
            if (challenge != null && index != null) {
                Text(
                    stringResource(R.string.alarm_challenge_progress, index + 1, uiState.challenges.size),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(16.dp))
                // A fresh input for every challenge.
                key(index) {
                    when (challenge.type) {
                        ChallengeType.PASSWORD -> PasswordChallenge(
                            passwordName = challenge.passwordName,
                            isChecking = uiState.isCheckingPassword,
                            showWrongPassword = uiState.showWrongPassword,
                            onSubmit = onPasswordSubmit,
                        )
                        // Not configurable yet (step 5).
                        ChallengeType.BARCODE, ChallengeType.NFC -> Unit
                    }
                }
            }

            if (showStopButton) {
                Spacer(Modifier.height(32.dp))
                OutlinedButton(onClick = onStop) {
                    Text(stringResource(R.string.alarm_stop_debug))
                }
            }
            if (debugText != null) {
                Spacer(Modifier.height(24.dp))
                Text(debugText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * The password must be typed by hand: no paste menu, and any change that inserts more than one
 * character at once is rejected. The field is cleared after every attempt.
 */
@Composable
private fun PasswordChallenge(
    passwordName: String?,
    isChecking: Boolean,
    showWrongPassword: Boolean,
    onSubmit: (String) -> Unit,
) {
    val state = rememberTextFieldState()
    fun submit() {
        if (isChecking) return
        val typed = state.text.toString()
        state.clearText()
        onSubmit(typed)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.alarm_password_prompt, passwordName ?: stringResource(R.string.challenge_type_password)),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(8.dp))
        CompositionLocalProvider(LocalTextToolbar provides NoTextToolbar) {
            OutlinedSecureTextField(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.alarm_password_label)) },
                isError = showWrongPassword,
                supportingText = if (showWrongPassword) {
                    { Text(stringResource(R.string.alarm_password_wrong)) }
                } else {
                    null
                },
                inputTransformation = TypedByHandOnly,
                keyboardOptions = PasswordKeyboardOptions,
                onKeyboardAction = { submit() },
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = ::submit, enabled = !isChecking) {
            Text(stringResource(R.string.alarm_password_check))
        }
    }
}

data class AlarmVolume(val current: Int, val max: Int)

/** Current alarm stream volume, refreshed every second. */
@Composable
fun rememberAlarmVolume(): AlarmVolume {
    val audioManager = LocalContext.current.getSystemService(AudioManager::class.java)
    fun read() = AlarmVolume(
        current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM),
        max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
    )
    val volume by produceState(read()) {
        while (true) {
            delay(1_000)
            value = read()
        }
    }
    return volume
}

@Preview
@Composable
private fun AlarmScreenPreview() {
    TargetChainTheme {
        AlarmScreen(
            uiState = AlarmUiState(
                isLoading = false,
                alarmName = "Work",
                challenges = listOf(AlarmChallenge(ChallengeType.PASSWORD, 1, "Desk")),
                showWrongPassword = true,
            ),
            onPasswordSubmit = {},
            onStop = {},
            showStopButton = true,
            debugText = "Alarm volume: 7/7",
        )
    }
}
