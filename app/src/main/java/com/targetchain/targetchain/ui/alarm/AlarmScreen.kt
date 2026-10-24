package com.targetchain.targetchain.ui.alarm

import android.media.AudioManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.targetchain.targetchain.R
import com.targetchain.targetchain.ui.theme.TargetChainTheme
import kotlinx.coroutines.delay

/** @param debugText extra info shown only in debug builds (e.g. the current alarm volume). */
@Composable
fun AlarmScreen(onStop: () -> Unit, modifier: Modifier = Modifier, debugText: String? = null) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.alarm_ringing_title),
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(48.dp))
            // Temporary: will be replaced by the alarm's challenges.
            Button(onClick = onStop) {
                Text(stringResource(R.string.alarm_stop_temporary))
            }
            if (debugText != null) {
                Spacer(Modifier.height(24.dp))
                Text(debugText, style = MaterialTheme.typography.bodySmall)
            }
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
        AlarmScreen(onStop = {}, debugText = "Alarm volume: 7/7")
    }
}
