# Android and Samsung quirks

Permissions needed, odd behaviors of Android 12/13 (and newer versions taken into account) and Samsung, and how each one was solved. "Seen on device" means it happened in our tests on the Samsung (Android 13); the rest was taken into account from the platform rules.

## Permissions (step 1)

| Permission | Why | Version behavior and handling |
|---|---|---|
| `USE_EXACT_ALARM` (API 33+) | `setAlarmClock` | Granted at install and can't be revoked; intended for alarm clock apps. |
| `SCHEDULE_EXACT_ALARM` (`maxSdkVersion=32`) | `setAlarmClock` on Android 12 | Granted by the user. Checked with `canScheduleExactAlarms()`; the home screen links to `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. Not used on 13+ (on 14+ it's denied by default for new installs). |
| `POST_NOTIFICATIONS` | Ringing notification and its full-screen intent | Runtime permission on 13+. Requested with the system dialog; if denied, the app opens its notification settings (after two denials the dialog is no longer shown). |
| `USE_FULL_SCREEN_INTENT` | Open the alarm screen over the lock screen | Granted on 12/13. On 14+ checked with `canUseFullScreenIntent()`, with a link to `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`. |
| `SYSTEM_ALERT_WINDOW` | Start (and reopen) the alarm screen from the background while the phone is in use | Granted by the user in "Display over other apps" (`ACTION_MANAGE_OVERLAY_PERMISSION`), checked with `Settings.canDrawOverlays()`. |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Ringing service | A foreground service type is required on 14+; we use `mediaPlayback`. |
| `RECEIVE_BOOT_COMPLETED` | Reschedule after reboot | — |
| `WAKE_LOCK` | Keep the CPU awake while the tone plays (ExoPlayer `WAKE_MODE_LOCAL`) | — |

## Android behaviors

### Alarms are cleared on reboot
- **Behavior:** Android drops all scheduled alarms when the phone restarts. A time zone change also changes what "07:00" means.
- **Solution:** `RescheduleReceiver` reschedules the enabled routines on `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED` and `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`. It uses `goAsync()` to read Room.

### Direct Boot: no database before the first unlock
- **Behavior:** after a reboot, Room (credential-encrypted storage) isn't available until the user unlocks the phone for the first time, so alarms aren't rescheduled until then.
- **Solution:** not solved. If the phone reboots at night and isn't unlocked, the alarm won't ring. Fix would need a `directBootAware` receiver and device-protected storage.

### Foreground service must start promptly; background start allowed by `setAlarmClock`
- **Behavior:** after `startForegroundService`, the service must call `startForeground` within a few seconds or the app crashes. Starting a foreground service from the background is restricted, but alarms set with `setAlarmClock` are exempt.
- **Solution:** `AlarmRingingService` calls `startForeground` first, before reading Room or starting playback.

### Full-screen intent only pops up when locked or screen off (seen on device)
- **Behavior:** the full-screen intent opens the activity only when the phone is locked or the screen is off. While the phone is in use it shows a heads-up notification instead.
- **Solution:** with "Display over other apps", `AlarmRingingService` starts `AlarmActivity` directly. Without it, the heads-up notification is the fallback (tap to open).

### Home and Recents can't be blocked; background starts are delayed after them (seen on device)
- **Behavior:** apps can't intercept Home or Recents. After Home/Recents, Android blocks activity starts from the background for about 5 seconds, so a single start attempt can be lost.
- **Solution:** while ringing, the service checks every second and reopens the alarm screen if it isn't visible. The user may see the home screen for up to about 5 s.

### Notification panel and Quick settings can't be closed or blocked (seen on device)
- **Behavior:** the panels can be opened over the alarm screen. Since Android 12, apps can't close them (`ACTION_CLOSE_SYSTEM_DIALOGS` is blocked).
- **Solution:** accepted limitation. The alarm keeps ringing and the alarm screen stays underneath; anything opened from the panel that hides the alarm screen triggers the reopen.

### Turning the screen off while ringing (seen on device)
- **Behavior:** turning the screen off stops the alarm screen, so it is reopened and, with `turnScreenOn`, the screen turns on again.
- **Solution:** expected behavior, kept.

### No mediaPlayback foreground service from `BOOT_COMPLETED` on Android 15+
- **Behavior:** apps targeting Android 15+ can't start a `mediaPlayback` foreground service from a `BOOT_COMPLETED` receiver.
- **Solution:** to make an alarm that was ringing ring again after a reboot, `RescheduleReceiver` schedules it with `setAlarmClock` a few seconds later instead of starting the service. The alarm is allowed to start the service on every version. Not seen on device (the test phone runs Android 13).

### Restarting a killed foreground service
- **Behavior:** if the process is killed while ringing, a service restarted by the system (`START_REDELIVER_INTENT`) may be refused the foreground start from the background. `am kill` doesn't kill a process with a foreground service, and "Force stop" clears all the app's alarms, including the watchdog.
- **Solution:** `startForeground` is wrapped in a try/catch. A watchdog alarm (`setAlarmClock`, every 60 s while ringing) starts the service again, since alarms are allowed to start it. To test a kill: `adb shell am crash com.targetchain.targetchain`. Seen on device: works (2026-10-24).

### Lowering the volume can't be fully blocked
- **Behavior:** the volume keys only reach the activity while it has focus (not with the screen off), and the alarm volume can also be lowered from the panel slider.
- **Solution:** `AlarmActivity` ignores volume down. The service sets the alarm volume to max when ringing starts, puts it back every second if lowered, and restores the previous level on stop. `setStreamVolume` is wrapped in a try/catch in case Do Not Disturb refuses it. Seen on device: works (2026-10-24).

### PendingIntent mutability
- **Behavior:** since Android 12 every PendingIntent must declare `FLAG_IMMUTABLE` or `FLAG_MUTABLE`.
- **Solution:** all PendingIntents use `FLAG_IMMUTABLE`.

### Alarm audio
- **Behavior:** a normal media stream is silenced by silent mode and can be paused by other apps taking audio focus.
- **Solution:** ExoPlayer with `USAGE_ALARM` (alarm volume, rings in silent mode) and audio focus not handled, so other apps can't pause it. The notification channel has no sound of its own, so the tone isn't played twice.

### Ringtone list and tone URIs (step 2)
- **Behavior:** the preinstalled alarm tones depend on the manufacturer (Samsung ships its own). Tones are identified by a MediaStore URI (`content://media/internal/audio/media/<id>`), which may stop working if a system update renumbers or removes the tones.
- **Solution:** the list is read with `RingtoneManager` (`TYPE_ALARM`). System tones are in MediaStore's internal storage, so no storage permission is needed. If a saved tone can't be played, the ringing service falls back to the default ringtone (step 1), and the editor shows "Unknown tone". Seen on device: the list shows Samsung's alarm tones and the chosen tone rings (2026-10-24). A tone URI that stops working after an update hasn't been seen.

## Samsung

### Battery optimization list hides the app (seen on device)
- **Behavior:** `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` opens a list that only shows already unrestricted apps by default, so TargetChain didn't appear.
- **Solution:** the button opens the app's App info page, where Battery > Unrestricted can be chosen. It's optional: `setAlarmClock` works without the exemption.

### Sleeping apps and force stop
- **Behavior:** Samsung's "Sleeping apps" / "Deep sleeping apps" can restrict background work. "Force stop" clears all the app's scheduled alarms.
- **Solution:** the home screen hint tells the user to add TargetChain to "Never sleeping apps". Force stop can't be prevented.
