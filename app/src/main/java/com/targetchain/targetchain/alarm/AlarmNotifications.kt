package com.targetchain.targetchain.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.targetchain.targetchain.BuildConfig
import com.targetchain.targetchain.MainActivity
import com.targetchain.targetchain.R
import com.targetchain.targetchain.ui.alarm.AlarmActivity

object AlarmNotifications {

    const val RINGING_NOTIFICATION_ID = 1
    const val COUNTDOWN_NOTIFICATION_ID = 2
    private const val SKIPPED_NOTIFICATION_ID_BASE = 1_000

    private const val CHANNEL_ID = "ringing_alarms"
    private const val COUNTDOWN_CHANNEL_ID = "routine_countdown"
    private const val NOTICES_CHANNEL_ID = "routine_notices"

    fun createChannels(context: Context) {
        val ringing = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_alarms),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            // The service plays the tone itself.
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        // Silent and without pop-up: the phone is used normally during a countdown.
        val countdown = NotificationChannel(
            COUNTDOWN_CHANNEL_ID,
            context.getString(R.string.notification_channel_countdown),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val notices = NotificationChannel(
            NOTICES_CHANNEL_ID,
            context.getString(R.string.notification_channel_notices),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannels(listOf(ringing, countdown, notices))
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

    /**
     * What the countdown notification shows. Task names are already resolved for display (a blank
     * name is shown as "Task <number>").
     *
     * @property nextTaskName null when it's the last task.
     */
    data class CountdownInfo(
        val routineName: String,
        val taskNumber: Int,
        val taskCount: Int,
        val taskName: String,
        val nextTaskName: String?,
    )

    /**
     * Persistent notification of a task's countdown. The time left is a system chronometer counting
     * down to [endAtWallMs], which updates itself every second. [info] is null until the routine is
     * read from the database.
     */
    fun countdown(context: Context, endAtWallMs: Long, info: CountdownInfo?): Notification {
        val openApp = PendingIntent.getActivity(
            context,
            0,
            MainActivity.newIntent(context, confirmAdvance = false),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val confirmAdvance = PendingIntent.getActivity(
            context,
            1,
            MainActivity.newIntent(context, confirmAdvance = true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val isLastTask = info != null && info.nextTaskName == null
        val builder = NotificationCompat.Builder(context, COUNTDOWN_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openApp)
            .setWhen(endAtWallMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                0,
                context.getString(if (isLastTask) R.string.routine_end else R.string.routine_next_task),
                confirmAdvance,
            )
        if (info == null) {
            builder.setContentTitle(context.getString(R.string.notification_countdown_title_loading))
        } else {
            builder
                .setContentTitle(
                    context.getString(R.string.notification_countdown_title, info.taskNumber, info.taskCount, info.taskName)
                )
                .setContentText(
                    info.nextTaskName?.let { context.getString(R.string.notification_countdown_next, it) }
                        ?: context.getString(R.string.notification_countdown_last)
                )
                .setSubText(info.routineName)
        }
        if (BuildConfig.DEBUG) {
            val endRoutine = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, RoutineEventReceiver::class.java).setAction(RoutineEventReceiver.ACTION_DEBUG_END_ROUTINE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(0, context.getString(R.string.routine_end_debug), endRoutine)
        }
        return builder.build()
    }

    /** A routine's start time arrived while another routine was running, so it didn't run. */
    fun showRoutineSkipped(context: Context, routineId: Long, routineName: String) {
        val name = routineName.ifBlank { context.getString(R.string.routine_default_name) }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            MainActivity.newIntent(context, confirmAdvance = false),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, NOTICES_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notification_skipped_title, name))
            .setContentText(context.getString(R.string.notification_skipped_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notification_skipped_text)))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        // Without the notifications permission it can't be shown; the switch still turns off.
        runCatching {
            context.getSystemService(NotificationManager::class.java)
                .notify(SKIPPED_NOTIFICATION_ID_BASE + routineId.toInt(), notification)
        }
    }
}
