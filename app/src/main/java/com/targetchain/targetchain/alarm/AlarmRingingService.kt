package com.targetchain.targetchain.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.targetchain.targetchain.appContainer
import com.targetchain.targetchain.ui.alarm.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Foreground service that keeps the alarm ringing and shows the full-screen alarm notification. */
class AlarmRingingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: ExoPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, AlarmReceiver.NO_ALARM_ID)
            ?: AlarmReceiver.NO_ALARM_ID

        // Must be called promptly after startForegroundService, before any slow work.
        ServiceCompat.startForeground(
            this,
            AlarmNotifications.RINGING_NOTIFICATION_ID,
            AlarmNotifications.ringing(this, alarmId),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        isRinging = true

        if (player == null) {
            serviceScope.launch { ring(alarmId) }
            serviceScope.launch { keepAlarmScreenShown(alarmId) }
        }
        // If the process is killed while ringing, don't restart silently with a null intent.
        return START_NOT_STICKY
    }

    /**
     * The full-screen intent only opens the alarm screen when the phone is locked or the screen is
     * off; while in use it shows a heads-up instead. Home and Recents can't be blocked either. So
     * while ringing, reopen the screen whenever it isn't visible. Starting it from the background
     * needs "Display over other apps", and Android may block a start for ~5 s after Home or Recents,
     * which is why this retries instead of starting once.
     */
    private suspend fun keepAlarmScreenShown(alarmId: Long) {
        while (isRinging) {
            if (!AlarmActivity.isVisible && Settings.canDrawOverlays(this)) {
                startActivity(AlarmActivity.newIntent(this, alarmId))
            }
            delay(SCREEN_CHECK_INTERVAL_MS)
        }
    }

    private suspend fun ring(alarmId: Long) {
        val repository = appContainer.routineRepository
        val alarm = withContext(Dispatchers.IO) {
            runCatching { repository.getAlarm(alarmId) }.getOrNull()
        }
        startPlayback(alarm?.ringtoneUri?.toUri() ?: Settings.System.DEFAULT_ALARM_ALERT_URI)

        // The test alarm is one-shot. Chaining will replace this with "advance to the next alarm".
        if (alarm != null) {
            withContext(Dispatchers.IO) { repository.disableRoutine(alarm.routineId) }
        }
    }

    private fun startPlayback(uri: Uri) {
        val attributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_ALARM)
            .setContentType(C.AUDIO_CONTENT_TYPE_SONIFICATION)
            .build()
        // No audio focus handling: another app taking focus must not be able to pause the alarm.
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(attributes, /* handleAudioFocus = */ false)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                addListener(FallbackOnError(this, uri))
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                play()
            }
    }

    /** If the chosen tone can't be played, fall back once to the default ringtone. */
    private class FallbackOnError(private val player: ExoPlayer, private val failedUri: Uri) : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Cannot play $failedUri", error)
            if (failedUri == Settings.System.DEFAULT_RINGTONE_URI) return
            player.removeListener(this)
            player.setMediaItem(MediaItem.fromUri(Settings.System.DEFAULT_RINGTONE_URI))
            player.prepare()
            player.play()
        }
    }

    override fun onDestroy() {
        isRinging = false
        serviceScope.cancel()
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AlarmRingingService"
        private const val SCREEN_CHECK_INTERVAL_MS = 1_000L

        /** True from the moment an alarm starts ringing until it is stopped. */
        @Volatile
        var isRinging = false
            private set

        fun start(context: Context, alarmId: Long) {
            val intent = Intent(context, AlarmRingingService::class.java)
                .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            // Cleared here too, because stopService destroys the service asynchronously.
            isRinging = false
            context.stopService(Intent(context, AlarmRingingService::class.java))
        }
    }
}
