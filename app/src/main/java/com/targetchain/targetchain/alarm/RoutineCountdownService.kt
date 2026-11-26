package com.targetchain.targetchain.alarm

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.targetchain.targetchain.R
import com.targetchain.targetchain.appContainer
import com.targetchain.targetchain.data.model.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that stays active during a task's countdown, with a persistent notification
 * showing the time left and the next task. It only shows the countdown: the next task's alarm (or
 * the routine's end) is scheduled in AlarmManager by [RoutineRunner], so it happens even if this
 * service is killed. A watchdog alarm brings the notification back in that case.
 *
 * Type systemExempted: reserved for alarm apps (USE_EXACT_ALARM) that keep a foreground service for
 * their alarms. It has no time limit (a task can last 12 h) and may start after a reboot.
 */
class RoutineCountdownService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val countdown = appContainer.alarmStateStore.run.countdown
        val endAt = countdown?.timing?.endAtWallMs ?: System.currentTimeMillis()
        // Must be called promptly after startForegroundService, before reading the database.
        try {
            val notification = AlarmNotifications.countdown(this, endAt, info = null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    AlarmNotifications.COUNTDOWN_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED,
                )
            } else {
                startForeground(AlarmNotifications.COUNTDOWN_NOTIFICATION_ID, notification)
            }
        } catch (e: IllegalStateException) {
            // E.g. a restart refused after a kill; the countdown watchdog tries again.
            Log.w(TAG, "Foreground start refused", e)
            stopSelf()
            return START_NOT_STICKY
        }
        if (countdown == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        isRunning = true
        serviceScope.launch { showDetails(countdown.alarmId, countdown.nextAlarmId, endAt) }
        return START_STICKY
    }

    private suspend fun showDetails(alarmId: Long, nextAlarmId: Long?, endAt: Long) {
        val repository = appContainer.routineRepository
        val info = runCatching {
            val alarm = repository.getAlarm(alarmId) ?: return
            val routine = repository.getRoutine(alarm.routineId) ?: return
            val tasks = repository.getTasks(alarm.routineId)
            fun displayName(task: Alarm) =
                task.name.ifBlank { getString(R.string.task_default_name, tasks.indexOf(task) + 1) }
            AlarmNotifications.CountdownInfo(
                routineName = routine.routine.name.ifBlank { getString(R.string.routine_default_name) },
                taskNumber = tasks.indexOfFirst { it.id == alarmId } + 1,
                taskCount = tasks.size,
                taskName = displayName(tasks.first { it.id == alarmId }),
                nextTaskName = tasks.firstOrNull { it.id == nextAlarmId }?.let(::displayName),
            )
        }.getOrNull() ?: return
        // Ignore it if the countdown changed meanwhile.
        if (appContainer.alarmStateStore.run.countdown?.alarmId != alarmId) return
        runCatching {
            getSystemService(NotificationManager::class.java).notify(
                AlarmNotifications.COUNTDOWN_NOTIFICATION_ID,
                AlarmNotifications.countdown(this, endAt, info),
            )
        }
    }

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RoutineCountdownService"

        /** True while the service is running in this process. */
        @Volatile
        var isRunning = false
            private set

        /**
         * Starts the service, or refreshes its notification if it's running. A start the system
         * refuses (background restrictions) is only logged: the next alarm doesn't depend on it.
         */
        fun startSafely(context: Context) {
            runCatching {
                context.startForegroundService(Intent(context, RoutineCountdownService::class.java))
            }.onFailure { Log.w(TAG, "Cannot start the countdown service", it) }
        }

        fun stop(context: Context) {
            isRunning = false
            context.stopService(Intent(context, RoutineCountdownService::class.java))
        }
    }
}
