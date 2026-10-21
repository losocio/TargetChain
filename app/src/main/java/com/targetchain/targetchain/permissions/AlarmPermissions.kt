package com.targetchain.targetchain.permissions

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri

data class PermissionStatus(
    val notifications: Boolean = false,
    val exactAlarms: Boolean = false,
    val fullScreenIntent: Boolean = false,
    val displayOverOtherApps: Boolean = false,
    val ignoringBatteryOptimizations: Boolean = false,
) {
    /** Battery optimization is informational: setAlarmClock works without the exemption. */
    val allRequiredGranted: Boolean
        get() = notifications && exactAlarms && fullScreenIntent && displayOverOtherApps
}

/** Checks the permissions the alarm needs on Android 12+ and builds the intents to grant them. */
object AlarmPermissions {

    fun status(context: Context): PermissionStatus = PermissionStatus(
        notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        exactAlarms = context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
        fullScreenIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        } else {
            true
        },
        displayOverOtherApps = Settings.canDrawOverlays(context),
        ignoringBatteryOptimizations = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName),
    )

    fun notificationSettingsIntent(context: Context) =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun exactAlarmSettingsIntent(context: Context) =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context))

    fun fullScreenIntentSettingsIntent(context: Context) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(context))
        } else {
            notificationSettingsIntent(context)
        }

    fun displayOverOtherAppsSettingsIntent(context: Context) =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri(context))

    /**
     * App info page, where Battery > Unrestricted can be chosen. The generic optimization list
     * (ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) only shows already unrestricted apps by default.
     */
    fun batteryOptimizationSettingsIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))

    private fun packageUri(context: Context) = "package:${context.packageName}".toUri()
}
