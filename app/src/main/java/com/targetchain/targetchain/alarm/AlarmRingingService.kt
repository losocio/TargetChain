package com.targetchain.targetchain.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
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
import com.targetchain.targetchain.BuildConfig
import com.targetchain.targetchain.appContainer
import com.targetchain.targetchain.ui.alarm.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground service that keeps the alarm ringing and shows the full-screen alarm notification.
 * The alarm keeps ringing until it is stopped from the alarm screen: the ringing state is persisted,
 * so a reboot (RescheduleReceiver) or a killed process (watchdog alarm) makes it ring again.
 */
class AlarmRingingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: ExoPlayer? = null
    private val audioManager by lazy { getSystemService(AudioManager::class.java) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, AlarmReceiver.NO_ALARM_ID)
            ?: AlarmReceiver.NO_ALARM_ID

        // Must be called promptly after startForegroundService, before any slow work.
        try {
            ServiceCompat.startForeground(
                this,
                AlarmNotifications.RINGING_NOTIFICATION_ID,
                AlarmNotifications.ringing(this, alarmId),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } catch (e: IllegalStateException) {
            // The system may refuse a foreground start when it restarts this service after a kill.
            // The watchdog alarm (which is allowed to start it) will make the alarm ring again.
            Log.w(TAG, "Foreground start refused", e)
            stopSelf()
            return START_NOT_STICKY
        }
        isRinging = true

        val container = appContainer
        container.alarmScheduler.scheduleWatchdog(alarmId)
        if (player == null) {
            container.alarmStateStore.ringingAlarmId = alarmId
            // Don't overwrite it if we are ringing again after a kill: the volume is already at max.
            if (container.alarmStateStore.previousAlarmVolume == null) {
                container.alarmStateStore.previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            }
            serviceScope.launch { ring(alarmId) }
            serviceScope.launch { guardWhileRinging(alarmId) }
        }
        // If the process is killed while ringing, restart with the same alarm.
        return START_REDELIVER_INTENT
    }

    /**
     * Anti-evasion checks, every second while ringing:
     * - The full-screen intent only opens the alarm screen when the phone is locked or the screen
     *   is off, and Home/Recents can't be blocked, so reopen the screen whenever it isn't visible.
     *   Starting it from the background needs "Display over other apps", and Android may block a
     *   start for ~5 s after Home or Recents, which is why this retries instead of starting once.
     * - Put the alarm volume back to max if it was lowered (panel slider, keys with the screen off).
     */
    private suspend fun guardWhileRinging(alarmId: Long) {
        while (isRinging) {
            if (!AlarmActivity.isVisible && Settings.canDrawOverlays(this)) {
                startActivity(AlarmActivity.newIntent(this, alarmId))
            }
            forceMaxAlarmVolume()
            delay(GUARD_INTERVAL_MS)
        }
    }

    private fun forceMaxAlarmVolume() {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) < max) {
            setAlarmVolume(this, max)
        }
    }

    private suspend fun ring(alarmId: Long) {
        val alarm = withContext(Dispatchers.IO) {
            runCatching { appContainer.routineRepository.getAlarm(alarmId) }.getOrNull()
        }
        startPlayback(alarm?.ringtoneUri?.toUri() ?: Settings.System.DEFAULT_ALARM_ALERT_URI)
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
                // Debug builds only: mute the player itself; the alarm stream volume is still enforced.
                if (BuildConfig.DEBUG && appContainer.alarmStateStore.silentTestMode) volume = 0f
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
        private const val GUARD_INTERVAL_MS = 1_000L

        /** True from the moment an alarm starts ringing in this process until it is stopped. */
        @Volatile
        var isRinging = false
            private set

        fun start(context: Context, alarmId: Long) {
            val intent = Intent(context, AlarmRingingService::class.java)
                .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            context.startForegroundService(intent)
        }

        /** Stops the ringing alarm for good: it won't ring again after a reboot or a kill. */
        fun stop(context: Context) {
            // Cleared here too, because stopService destroys the service asynchronously.
            isRinging = false
            val container = context.appContainer
            val store = container.alarmStateStore
            val alarmId = store.ringingAlarmId
            store.ringingAlarmId = null
            container.alarmScheduler.cancelWatchdog()
            store.previousAlarmVolume?.let { setAlarmVolume(context, it) }
            store.previousAlarmVolume = null
            context.stopService(Intent(context, AlarmRingingService::class.java))

            // Alarms are one-shot: its switch turns off. Chaining will replace this with
            // "advance to the next alarm".
            if (alarmId != null) {
                container.applicationScope.launch {
                    val repository = container.routineRepository
                    repository.getAlarm(alarmId)?.let { repository.disableRoutine(it.routineId) }
                }
            }
        }

        private fun setAlarmVolume(context: Context, volume: Int) {
            // May be refused in some Do Not Disturb configurations; the alarm keeps ringing anyway.
            runCatching {
                context.getSystemService(AudioManager::class.java)
                    .setStreamVolume(AudioManager.STREAM_ALARM, volume, 0)
            }.onFailure { Log.w(TAG, "Cannot set alarm volume", it) }
        }
    }
}
