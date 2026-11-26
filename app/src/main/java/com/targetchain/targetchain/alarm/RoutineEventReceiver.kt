package com.targetchain.targetchain.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.targetchain.targetchain.BuildConfig
import com.targetchain.targetchain.appContainer
import kotlinx.coroutines.launch

/** Countdown events: the last task's countdown ended, the countdown watchdog, debug "End routine". */
class RoutineEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val runner = context.appContainer.routineRunner
        when (intent.action) {
            ACTION_COUNTDOWN_WATCHDOG -> runner.onCountdownWatchdog()
            ACTION_COUNTDOWN_ENDED -> runAsync(context) { runner.onCountdownEnded() }
            ACTION_DEBUG_END_ROUTINE -> if (BuildConfig.DEBUG) runAsync(context) { runner.endRoutine() }
        }
    }

    private fun runAsync(context: Context, block: suspend () -> Unit) {
        val pendingResult = goAsync()
        context.appContainer.applicationScope.launch {
            try {
                block()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COUNTDOWN_ENDED = "com.targetchain.targetchain.action.COUNTDOWN_ENDED"
        const val ACTION_COUNTDOWN_WATCHDOG = "com.targetchain.targetchain.action.COUNTDOWN_WATCHDOG"
        const val ACTION_DEBUG_END_ROUTINE = "com.targetchain.targetchain.action.DEBUG_END_ROUTINE"
    }
}
