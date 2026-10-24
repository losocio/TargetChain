# Decision log

Important decisions and why, deviations from the plan or scope and their reasons, and dead ends (things tried or discarded). Newest step last.

## Step 1: project setup, data model, reliable test alarm (2026-10-21)

### Exact alarms with `setAlarmClock` + `USE_EXACT_ALARM`
- **Decision:** schedule with `AlarmManager.setAlarmClock`. Declare `USE_EXACT_ALARM` (API 33+) and `SCHEDULE_EXACT_ALARM` only up to API 32.
- **Why:** `setAlarmClock` is the most reliable alarm type: it ignores Doze and battery restrictions and lets us start a foreground service from the background. `USE_EXACT_ALARM` is meant for alarm clock apps, which is our case, and avoids depending on a permission the user must grant (details in `android-quirks.md`).

### Ringing in a `mediaPlayback` foreground service
- **Decision:** `AlarmReceiver` starts `AlarmRingingService`, a foreground service of type `mediaPlayback` that shows the alarm notification and plays the tone with Media3 ExoPlayer (`USAGE_ALARM`, looping, `WAKE_MODE_LOCAL`).
- **Why:** a BroadcastReceiver only lives for a few seconds; an alarm can ring for minutes. `mediaPlayback` matches what the service does.
- **Audio focus is not handled on purpose.** Otherwise another app (e.g. a music app) could pause the alarm by taking focus. Downside: music already playing keeps playing over the alarm.
- **Fallback tone:** if the chosen tone fails, play the default ringtone once, so the alarm never stays silent.

### Manual dependency injection
- **Decision:** a small `AppContainer` created in `TargetChainApp` holds the database, repository and scheduler. No Hilt/Koin.
- **Why:** the project is small. A DI framework adds a dependency, annotation processing and concepts with no real benefit at this size.

### Data model: time in minutes, duration per alarm
- **Decision:** `Routine` → ordered `Alarm`s (`position`) → ordered `Challenge`s. Only the first alarm has a time of day (`startTimeMinutes`, minutes since midnight). Each alarm has `activityDurationMinutes`: the countdown to the next alarm, started once its challenges are done (null = the routine ends).
- **Why:** this matches the chain model: the first alarm starts the routine, the following ones are relative to the previous activity. Storing the time as an `Int` avoids TypeConverters. The next-trigger computation is a pure, unit-tested function (`nextTriggerTime`).
- **During development:** Room uses `fallbackToDestructiveMigration` (no real user data yet). It must be replaced by real migrations before the app is used for real.

### Rescheduling after reboot only after the first unlock (scope deviation)
- **Decision:** `RescheduleReceiver` reschedules enabled routines on `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED` and when the exact alarm permission is granted. Direct Boot support is postponed.
- **Why:** the step goal asks for rescheduling after reboot; it works once the phone is unlocked after rebooting. Supporting alarms before the first unlock (Direct Boot) needs a `directBootAware` receiver and device-protected storage. Candidate for a scope change (see `android-quirks.md`).

### Alarm screen pops up and is reopened while ringing
- **Decision:** request "Display over other apps" (`SYSTEM_ALERT_WINDOW`). While ringing, `AlarmRingingService` checks every second whether `AlarmActivity` is visible and reopens it if not. Back is disabled on the alarm screen.
- **Why:** the full-screen intent alone doesn't open the screen while the phone is in use, and Home/Recents can't be blocked (see `android-quirks.md`). Starting an activity from the background is only allowed with this permission, and retrying covers starts that Android blocks for a few seconds.

### Anti-evasion built with each feature instead of at the end (plan deviation, 2026-10-23)
- **Decision:** anti-evasion is no longer a separate roadmap step (the old step 6 was removed; the last step is now 6). Each step includes the anti-evasion measures that apply to what it builds, implemented and tested on the device together with the feature.
- **Why:** to check from the start that each screen can't be bypassed, instead of finding out late.

### Step 1 redefined: one alarm with its anti-evasion (plan deviation, 2026-10-23)
- **Decision:** step 1 now focuses only on one alarm that rings at a set time, covers the whole screen and has the anti-evasion measures that apply to it. On top of what's done (Back blocked, reopening after Home/Recents), they are:
  - blocking lowering the volume while ringing;
  - keeping it ringing after a reboot;
  - keeping it ringing if the service is killed.

  Architecture for settings, navigation and the rest is added only when a step needs it: Navigation Compose and DataStore, listed in the stack, are not added yet.
- **Why:** consequence of building anti-evasion with each feature. The alarm screen is what step 1 builds, so its measures belong here. Not adding architecture before it's needed keeps the scope low-risk.
- **Data model kept:** the Room entities (Routine → Alarm → Challenge) and the repository were already implemented in the original step 1 and are kept. They are what lets the alarm be rescheduled after a reboot.

### Debug-only silent mode for testing without sound (2026-10-23)
- **Decision:** add a switch on the debug screen, only in the development build, that plays the alarm with the player at volume 0 (or a silent audio file). Everything else works as usual: the alarm screen, reopening, and watching the system alarm volume. To see the volume protection working without hearing it, the debug alarm screen shows the current alarm volume (the system volume slider can also be watched).
- **Why:** development happens in a library where no noise can be made. The volume protection acts on the system alarm volume, which is independent of the player's volume, so it can be tested silently.
- **Discarded:**
  - Headphones: for safety, Android and Samsung usually play alarms through the speaker too.
  - Setting the alarm volume to 0 before testing: that's exactly what the protection must prevent.
- **Limits:** the switch must not reach the final version, or must be clearly marked as a testing option. One final test with real sound is still needed, outside the library, to check that the tone plays through the speaker at the right volume.

### Step 1 anti-evasion: volume, reboot and killed service (2026-10-23)
- **Decision:**
  - While ringing, the alarm volume is forced to **max** and the previous level is restored when the alarm is stopped.
  - The alarm only counts as finished when it's stopped, not when it starts ringing.
  - After a reboot, an alarm that was still ringing rings again.
  - A watchdog alarm every 60 s brings it back if the process was killed.
- **Why:** forcing max can't be evaded by leaving the volume very low beforehand (the user chose this over keeping the level at ring start). The watchdog uses `setAlarmClock` because alarms may start the foreground service from the background, while a system restart of the service may be refused.

### SharedPreferences instead of DataStore for the alarm state (stack deviation, 2026-10-23)
- **Decision:** `AlarmStateStore` keeps the ringing alarm id, the previous alarm volume and the silent test mode in SharedPreferences.
- **Why:** it's built into Android (no new dependency) and its synchronous reads/writes are simple in receivers and services; the state must be on disk before a possible reboot or kill (writes use `commit`). The stack lists DataStore for settings; it can be added when a step needs real user settings.

### Dead ends
- **Lock task / screen pinning to block Home:** needs a device-owner (managed) device, or shows a confirmation that the user can dismiss and later unpin. Not usable.
- **Closing the notification panel (`ACTION_CLOSE_SYSTEM_DIALOGS`):** blocked for apps since Android 12. Discarded.
- **Reopening the alarm screen once from `AlarmActivity.onStop()`:** failed when Recents was pressed several times (see `corrections.md`). Replaced by the per-second check in the service.
- **`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` for the battery button:** the app isn't listed on Samsung (see `corrections.md`). Replaced by the App info page.

## Step 2: alarm list and alarm editor (2026-10-24)

### Step 2 reduced to single alarms instead of routines (scope deviation, 2026-10-24)
- **Decision:** step 2 builds screens for individual alarms (list and editor: name, time, ringtone with preview, enable/disable, delete) instead of screens to create and edit routines. No routine screens or placeholders are created; each screen is added when its logic exists.
- **Why:** a routine editor without chaining (step 4) would be a screen with no logic behind it. Building only what works end to end keeps every screen testable on the device and the scope low-risk.
- **Data model:** each alarm is stored internally as a routine with a single link (one `Routine` with one `Alarm`), reusing the existing Room model. The chain editor arrives with step 4, which only has to extend the alarm editor to add more alarms to the routine.

### Navigation Compose with string routes (2026-10-24)
- **Decision:** add `androidx.navigation:navigation-compose` (2.10.2) with string routes (`alarms`, `alarm/{routineId}`, `ringtone?selectedUri=…`). The picked ringtone goes back to the editor through the editor's back stack entry `SavedStateHandle`.
- **Why:** it's the navigation library listed in the stack. Type-safe routes would also need the kotlinx-serialization plugin and library, which isn't worth it for three screens.

### Preinstalled tones are the device's system alarm tones (2026-10-24)
- **Decision:** the ringtone list comes from `RingtoneManager` (`TYPE_ALARM`), with "Default alarm tone" (`ringtoneUri = null`) first. The preview plays with Media3 ExoPlayer on the alarm stream at its current volume (not forced to max), once, and stops when the picker is left or the app goes to the background. Silent test mode (debug) mutes the preview too.
- **Why:** no audio files, no new dependency and no permission. The preview on the alarm stream sounds like the alarm will.
- **Discarded for now:** bundling tones in `res/raw`. The list would be the same on every phone, but it needs freely licensed audio and makes the APK bigger. It fits better with custom tones (step 6).

### Alarms are one-shot; saving enables the alarm (2026-10-24)
- **Decision:** when a ringing alarm is stopped, its switch turns off (same as the step 1 test alarm). Saving in the editor always enables the alarm and schedules it.
- **Why:** repeating on certain days isn't in the step goals; it would be a scope change. Enabling on save is what alarm apps usually do.

### Other step 2 choices (2026-10-24)
- **Icons as vector drawables** (`ic_add`, `ic_delete`, `ic_arrow_back`, Material icons, Apache 2.0) instead of adding the `material-icons` dependency.
- **The debug home screen is gone.** The alarm list replaces the test alarm. The permissions card is shown at the top of the list while any permission is missing (including the optional battery one), and the silent test mode switch is at the bottom (debug only). The editor has a "Next minute" button in debug builds only, to test quickly.
- **No schema change:** the alarm name is `Routine.name`. Step 4 will probably need an activity name per alarm (`Alarm.name`), which will be a migration.
