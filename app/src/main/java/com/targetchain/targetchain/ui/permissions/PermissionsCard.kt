package com.targetchain.targetchain.ui.permissions

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.targetchain.targetchain.R
import com.targetchain.targetchain.permissions.AlarmPermissions
import com.targetchain.targetchain.permissions.PermissionStatus

/** Status of each permission the alarm needs, with a button to grant the missing ones. */
@Composable
fun PermissionsCard(permissions: PermissionStatus, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val openSettings = { intent: Intent -> context.startActivity(intent) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Denied twice: the system no longer shows the dialog, so go to settings.
        if (!granted) openSettings(AlarmPermissions.notificationSettingsIntent(context))
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.permissions_title), style = MaterialTheme.typography.titleMedium)
            PermissionRow(R.string.permission_notifications, permissions.notifications) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openSettings(AlarmPermissions.notificationSettingsIntent(context))
                }
            }
            PermissionRow(R.string.permission_exact_alarms, permissions.exactAlarms) {
                openSettings(AlarmPermissions.exactAlarmSettingsIntent(context))
            }
            PermissionRow(R.string.permission_full_screen, permissions.fullScreenIntent) {
                openSettings(AlarmPermissions.fullScreenIntentSettingsIntent(context))
            }
            PermissionRow(R.string.permission_display_over_apps, permissions.displayOverOtherApps) {
                openSettings(AlarmPermissions.displayOverOtherAppsSettingsIntent(context))
            }
            PermissionRow(R.string.permission_battery, permissions.ignoringBatteryOptimizations) {
                openSettings(AlarmPermissions.batteryOptimizationSettingsIntent(context))
            }
            Text(
                stringResource(R.string.permission_battery_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PermissionRow(labelRes: Int, granted: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(labelRes),
            modifier = Modifier.weight(1f),
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        if (granted) {
            Text(stringResource(R.string.permission_granted))
        } else {
            TextButton(onClick = onGrant) { Text(stringResource(R.string.permission_grant)) }
        }
    }
}
