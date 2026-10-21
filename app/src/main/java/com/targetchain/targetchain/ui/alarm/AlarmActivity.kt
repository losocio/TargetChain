package com.targetchain.targetchain.ui.alarm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.targetchain.targetchain.alarm.AlarmReceiver
import com.targetchain.targetchain.alarm.AlarmRingingService
import com.targetchain.targetchain.ui.theme.TargetChainTheme

/**
 * Full-screen alarm UI, shown over the lock screen by the ringing notification's full-screen
 * intent, or started directly by AlarmRingingService. The user can't leave it while the alarm rings.
 */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        enableEdgeToEdge()
        // Back does nothing: the alarm can only be stopped from this screen.
        onBackPressedDispatcher.addCallback(this) { }
        setContent {
            TargetChainTheme {
                AlarmScreen(onStop = ::stopAlarm)
            }
        }
    }

    // AlarmRingingService reopens this screen while the alarm rings and it isn't visible.
    override fun onStart() {
        super.onStart()
        isVisible = true
    }

    override fun onStop() {
        super.onStop()
        isVisible = false
    }

    private fun stopAlarm() {
        AlarmRingingService.stop(this)
        finish()
    }

    companion object {
        /** Whether the alarm screen is currently on screen. Read and written on the main thread. */
        var isVisible = false
            private set

        fun newIntent(context: Context, alarmId: Long): Intent =
            Intent(context, AlarmActivity::class.java)
                .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}
