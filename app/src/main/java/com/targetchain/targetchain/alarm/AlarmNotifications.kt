package com.targetchain.targetchain.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.targetchain.targetchain.R
import com.targetchain.targetchain.ui.alarm.AlarmActivity

object AlarmNotifications {

    const val RINGING_NOTIFICATION_ID = 1
    private const val CHANNEL_ID = "ringing_alarms"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_alarms),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            // The service plays the tone itself.
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun ringing(context: Context, alarmId: Long): Notification {
        val alarmScreenIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            AlarmActivity.newIntent(context, alarmId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notification_ringing_title))
            .setContentText(context.getString(R.string.notification_ringing_text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(alarmScreenIntent)
            .setFullScreenIntent(alarmScreenIntent, true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
