package com.targetchain.targetchain.ringtone

import android.content.ContentUris
import android.content.Context
import android.media.RingtoneManager
import android.util.Log
import androidx.core.net.toUri
import com.targetchain.targetchain.R

/** A ringtone the user can choose. A null [uri] means the system default alarm tone. */
data class RingtoneOption(val uri: String?, val title: String)

/**
 * Preinstalled alarm tones, read with RingtoneManager. System tones live in MediaStore's internal
 * storage, so no storage permission is needed. Queries a content provider: call off the main thread.
 */
class RingtoneCatalog(private val context: Context) {

    /** The default tone first, then the device's alarm tones. */
    fun alarmTones(): List<RingtoneOption> = listOf(defaultOption()) + systemAlarmTones()

    fun title(uri: String?): String {
        if (uri == null) return context.getString(R.string.ringtone_default)
        return runCatching { RingtoneManager.getRingtone(context, uri.toUri())?.getTitle(context) }
            .getOrNull()
            ?: context.getString(R.string.ringtone_unknown)
    }

    private fun defaultOption() = RingtoneOption(uri = null, title = context.getString(R.string.ringtone_default))

    private fun systemAlarmTones(): List<RingtoneOption> {
        val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
        val cursor = runCatching { manager.cursor }
            .onFailure { Log.w(TAG, "Cannot list alarm tones", it) }
            .getOrNull()
            ?: return emptyList()
        return cursor.use {
            buildList {
                while (cursor.moveToNext()) {
                    val uri = ContentUris.withAppendedId(
                        cursor.getString(RingtoneManager.URI_COLUMN_INDEX).toUri(),
                        cursor.getLong(RingtoneManager.ID_COLUMN_INDEX),
                    )
                    add(RingtoneOption(uri.toString(), cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)))
                }
            }
        }
    }

    private companion object {
        const val TAG = "RingtoneCatalog"
    }
}
