# Corrections

Errors or incorrect behaviors detected, and the correction applied.

## Step 1

### Compile error in `MainActivity.kt`
- **Problem:** the import `androidx.lifecycle.viewmodel.compose.viewModel` had a stray `/` (`viewmodel.co/mpose.viewModel`). The import was invalid and `viewModel(...)` became an unresolved reference.
- **Correction:** the `/` was removed (the file was already fixed when it was read again). No other code change was needed.

### "Unrestricted battery" button: the app wasn't in the list (Samsung)
- **Problem:** the button opened `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`. On Samsung that list only shows apps that are already unrestricted by default, so TargetChain didn't appear.
- **Correction:** the button now opens the app's App info page (`ACTION_APPLICATION_DETAILS_SETTINGS`), where Battery > Unrestricted can be chosen. The hint text explains it.

### Alarm showed a heads-up notification instead of the full-screen screen
- **Problem:** while the phone was in use (home screen or another app), the alarm only showed a notification at the top; the alarm screen opened only after tapping it.
- **Correction:** added the "Display over other apps" permission (`SYSTEM_ALERT_WINDOW`) and a row for it on the home screen. With it granted, `AlarmRingingService` starts `AlarmActivity` directly.

### The alarm screen could be left with Home (and Back)
- **Problem:** pressing Home left the alarm screen while the alarm kept ringing. Back also closed the screen while ringing.
- **Correction:** Back is disabled on the alarm screen (`onBackPressedDispatcher` callback that does nothing). Home can't be blocked, so `AlarmActivity.onStop()` reopened the screen while the alarm was still ringing.

### Pressing Recents several times escaped the alarm screen
- **Problem:** the screen came back after one Recents press, but after several attempts it stayed closed. Reopening happened only once, in `onStop()`; if Android blocked that start (it blocks background starts for about 5 s after Home/Recents), nothing retried.
- **Correction:** `AlarmActivity` only records whether it's visible (`onStart`/`onStop`). While ringing, `AlarmRingingService` checks every second and reopens the screen if it isn't visible, so a blocked start is retried. This check also opens the screen when the alarm starts.

### Rebooting while ringing silenced the alarm for good
- **Problem (found when reviewing step 1 for anti-evasion):** the test routine was disabled as soon as the alarm *started* ringing, so rebooting the phone while it rang stopped it and it never rang again.
- **Correction:** the routine is disabled only when the alarm is stopped. The ringing alarm id is saved in `AlarmStateStore`, and after a reboot `RescheduleReceiver` makes it ring again. Verified on device (2026-10-24).

### A killed ringing service never came back
- **Problem (same review):** the service used `START_NOT_STICKY`, so if Android killed it, the alarm went silent.
- **Correction:** `START_REDELIVER_INTENT` plus a watchdog alarm every 60 s while ringing. Verified on device (2026-10-24).
