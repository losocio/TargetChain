package com.targetchain.targetchain.ringtone

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Plays a ringtone once, on the alarm stream at its current volume, so the preview sounds like the
 * alarm will. Create, use and release it on the main thread.
 */
class RingtonePreviewPlayer(context: Context, private val isMuted: () -> Boolean) {

    private val player = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_ALARM)
                .setContentType(C.AUDIO_CONTENT_TYPE_SONIFICATION)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .build()
        .apply {
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.w(TAG, "Cannot preview ringtone", error)
                }
            })
        }

    /** Plays [uri], or the default alarm tone when it is null. Replaces the current preview. */
    fun play(uri: String?) {
        player.setMediaItem(MediaItem.fromUri(uri?.toUri() ?: Settings.System.DEFAULT_ALARM_ALERT_URI))
        player.volume = if (isMuted()) 0f else 1f
        player.prepare()
        player.play()
    }

    fun stop() = player.stop()

    fun release() = player.release()

    private companion object {
        const val TAG = "RingtonePreviewPlayer"
    }
}
