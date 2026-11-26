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

## Scope changes for the intermediate delivery (2026-10-25)

### Post-check, random challenge order and custom tones moved to the TFG (scope deviation, 2026-10-25)
- **Decision:** three features leave the Proyectos IV scope and move to Phase 2 (TFG):
  - post-check that the user is still awake after the challenge;
  - random order of challenges (challenges keep a fixed order);
  - custom tones and random-order playlists. The ringtone is chosen from the device's preinstalled alarm tones only. Roadmap step 6 is now multi-device testing and bug fixing.
- **Why:** they aren't part of the core to validate by 29 Nov (chaining with challenges and basic anti-evasion), and the project is about two weeks behind the original milestones. Post-check was already low priority and is meant for single-alarm chains.

## Tooling: Claude desktop project reads the docs (2026-11-22)

Not part of the app: how the separate Claude desktop project (research and brainstorming) gets context from the docs that Claude Code maintains.

### The desktop project is linked to `docs/` only
- **Decision:** link only the `docs/` folder to the Claude desktop project, not the repo root or the code folders.
- **Why:** the project needs to know how the app is built and what was decided, not to read the code. Linking the root would make all the code reachable, plus noise like `build/`, `.gradle/` and `.idea/` that can confuse the model.

### `docs/` is read-only for the desktop project
- **Decision:** Claude Code writes and maintains every file in `docs/`. The desktop project's instructions tell it to read these notes for context but never edit, rename, move or delete any file in `docs/`. If a note looks wrong or out of date, it tells me in the chat instead.
- **Why:** one writer per file. The two sides don't know what the other is doing, so if both edited the same file, one could overwrite the other's changes from a stale copy or leave contradicting entries. It's a written convention in each side's instructions, not a technical lock.

### Claude Code's instructions live in `docs/claude-code-instructions.md`
- **Decision:** the contents of the root `CLAUDE.md` moved to `docs/claude-code-instructions.md`. The root `CLAUDE.md` keeps only a short note and the import line `@docs/claude-code-instructions.md`, so Claude Code loads the same instructions as before. The desktop project's instructions explain that this file is effectively Claude Code's `CLAUDE.md`.
- **Why:** the desktop project can read Claude Code's rules and conventions through `docs/` without getting access to the code folders.
- **Discarded:**
  - Linking the root folder and telling the desktop project to read only `CLAUDE.md`: the code would still be reachable, kept out only by an instruction.
  - Uploading a copy of `CLAUDE.md` to the desktop project: it goes stale as soon as Claude Code updates the original.
  - A symlink or hard link inside `docs/`: folder access may not follow symlinks, and a hard link breaks silently when an editor saves by replacing the file.

### New instructions go in the docs file, never in the root `CLAUDE.md`
- **Decision:** both files carry a note. The root `CLAUDE.md` says that all project instructions live in `docs/claude-code-instructions.md` and that nothing should be added to the root file. The top of `docs/claude-code-instructions.md` says it is the project's `CLAUDE.md` and that instructions are added or changed there.
- **Why:** when Claude Code updates its instructions, it tends to edit the root `CLAUDE.md` directly. A rule added there would be invisible to the desktop project, and the two files would drift apart.

## Step 3: challenge system and password challenge (2026-11-23)

### Challenges are shown inside the alarm screen
- **Decision:** every challenge type is shown inside `AlarmActivity`, one at a time in the configured order, instead of in its own screen. The alarm stops only after the last one.
- **Why:** Back and volume-down blocking, reopening every second, the watchdog and ringing again after a reboot already apply to that screen, so every challenge type (QR and NFC in step 5 too) gets them with nothing extra. The camera and NFC also need an activity, which this one is.
- **Progress in memory only:** the number of completed challenges lives in `AlarmRingingService` (in memory), reset when an alarm starts ringing. After a reboot or a killed process the challenges start from zero, as the step requires; if Android only recreates the alarm screen, the progress is kept.
- **Safety net:** if an alarm rings with no challenges (only possible with corrupted data), the alarm screen shows a Stop button so the user is never trapped. Otherwise the Stop button exists only in debug builds.

### Challenge configuration shares the editor's ViewModel (nested navigation graph)
- **Decision:** the editor, ringtone picker, challenge list, type selector and password configuration form a nested navigation graph (`alarm/{routineId}`) that shares one `AlarmEditViewModel`, which holds the whole draft (name, time, tone and challenges). Save writes it in one Room transaction; leaving the editor discards it. The ringtone picker now hands its result to that ViewModel directly instead of through the back stack `SavedStateHandle`.
- **Why:** the step requires challenge changes to be saved and discarded like the rest of the alarm. Sharing the ViewModel is simpler than passing lists between screens.
- **Reordering with up/down arrows** instead of drag and drop: simpler, accessible, no library.
- **Editing an existing challenge isn't supported:** to change it, delete it and add it again (not in the step goals).

### Saved passwords: a shared, named library, stored as a salted hash
- **Decision:** passwords live in their own table (`passwords`: name unique ignoring case, hash, salt), shared by all alarms. A password challenge references one (`Challenge.passwordId`). Creating, renaming and deleting a password apply immediately, not as part of the alarm edit, because other alarms may use it.
- **Hash instead of plain text:** PBKDF2-HMAC-SHA256, 100,000 iterations, random 16-byte salt per password, `javax.crypto` (built into Android, no dependency). The app never needs to show a password again, so plain text has no use, and with a hash it can't be read from a backup or the database (`allowBackup` is on).
- **Comparison:** case-sensitive; leading and trailing spaces are ignored (trimmed when created and when checked), inner spaces are kept; a blank password isn't accepted. Spaces at the ends are invisible on paper and some keyboards add them. Because only the hash is stored, this normalization can't be changed later without creating the passwords again.
- **Names:** trimmed and compared ignoring case ("Desk" and "desk" are the same name).

### A password in use can't be deleted
- **Decision:** a password can't be deleted while a saved alarm or the alarm being edited uses it. The dialog lists the alarms that use it; their challenge must be removed first. The foreign key is `RESTRICT`, so the database refuses it even if the UI failed.
- **Why:** no alarm can end up without a valid challenge, and it also covers the rule that a password used by a running routine can't be deleted.
- **Discarded:** deleting the challenges in cascade (alarms could be left enabled with zero challenges) and leaving them pointing to nothing.
- **Known limitation:** if the alarm being edited still uses it in its saved version, it must be saved first (the dialog names it).

### Running routine is locked
- **Decision:** `AlarmStateStore.activeRoutineId` (persisted) is set when an alarm of the routine starts ringing and cleared when the routine ends (for now, when its alarm stops; from step 4 it will stay set during the countdown). While set, the list shows the alarm as "Running" with its switch and delete disabled, tapping it shows a message, and `RoutineRepository` refuses to save, disable or delete it (if it starts ringing while the editor is open, Save is refused).
- **Interpretation:** "no alarm or challenge can be edited" applies to the alarms of the running routine; other alarms stay editable.

### Password typed by hand only
- **Decision:** the challenge field (`OutlinedSecureTextField`) has an `InputTransformation` that rejects any change inserting more than one character at once, and an empty text toolbar so no "Paste" menu appears. Autofill is disabled for both activities (`IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS`), and the new password screen also cancels any autofill session when left, so password managers don't offer to save or fill it.
- **Keyboard learning:** both the challenge field and the creation fields use the password keyboard type with autocorrect off; Samsung Keyboard and Gboard don't learn or suggest in password fields. On creation the text is visible (`TextObfuscationMode.Visible`) and pasting is allowed.
- **What can't be blocked:** memorizing the password; writing it down on the phone (notes, photos) or photographing the creation screen with another device; a third-party keyboard, accessibility or automation app that types one character at a time; pasting a single character; a keyboard that ignores the password type.
- **Discarded by the user:** `FLAG_SECURE` on the new password screen (to block screenshots while the password is visible).

### Alarm screen not resizable; other windows over it are a known limitation (scope deviation, 2026-11-24)
- **Problem (seen on device):** with Samsung pop-up view or split screen set up before the alarm rang, another app stays usable next to or over the alarm screen, and the per-second check doesn't react because the alarm screen still counts as visible. The other window could be used to read a password stored on the phone or to force stop the app from Settings.
- **Decision:** `AlarmActivity` is declared `resizeableActivity="false"`, so on phones Android shouldn't show it in a pop-up window or in half of a split screen, only full screen. One manifest line, no permission. Verified on device (2026-11-24): split screen (with TargetChain or with two other apps) closes and the alarm appears full screen; the alarm screen can't be put in a pop-up window or split screen while ringing. With TargetChain in a pop-up window, the alarm appears full screen over the other app, but the pop-up with TargetChain stays open. Another app's pop-up window stays usable over the alarm.
- **Also `MainActivity` (2026-11-24):** declared `resizeableActivity="false"` too, so TargetChain can't be opened in a pop-up window or split screen at all. Otherwise its own pop-up stayed over the alarm, and its permission buttons open Settings, where the app can be force stopped. An alarm app doesn't need multi-window.
- **Known limitation, moved to Phase 2 (TFG):** another app's pop-up window over the alarm stays open, listed with the advanced anti-evasion measures next to blocking force stop.
- **Discarded for now:** a full-screen overlay window (`TYPE_APPLICATION_OVERLAY`, with the existing "Display over other apps" permission) over every app while ringing. It would cover pop-ups and split screen, but the challenge and keyboard would have to move into the overlay, overlays are hidden over the lock screen (so both would be needed), and Back blocking and the step 1 and 3 tests would have to be redone. Too risky before the 29 Nov deadline with step 4 still pending; it's an option for the TFG.

## Scope changes for the final delivery (2026-11-25)

### QR/barcode and NFC challenges moved to the TFG; multi-device testing step removed (scope deviation, 2026-11-25)
- **Decision:**
  - The QR/barcode and NFC challenges leave the Proyectos IV scope and move to Phase 2 (TFG), listed with the other extra challenges. In Proyectos IV the only challenge type is the hidden password.
  - Roadmap step 5 (QR/barcode and NFC challenges) and step 6 (multi-device testing and bug fixing) are removed. The roadmap now ends with step 4 (alarm chaining).
  - CameraX and ML Kit, listed in the stack for QR scanning, aren't added in Phase 1.
- **Why:** the deadline is 29 Nov and step 4, the core feature that must be complete, hasn't started yet. The remaining time goes to chaining. QR and NFC would also need new dependencies, camera and NFC permissions and their own anti-evasion tests on the device. Testing stays on the Samsung with Android 13, done with each step as before.
- **What's kept for the TFG:** the challenge system is already generic. `ChallengeType` keeps `BARCODE` and `NFC` and `Challenge.expectedValue` stays in the data model (unused), and new types are shown inside `AlarmActivity`, so they inherit its anti-evasion measures with nothing extra (see step 3).

## Step 4: alarm chaining (2026-11-25)

### Countdown not changed by the phone's time: wall clock + monotonic anchor (option 2 of 4, chosen by the user)
- **Decision:** the next task's alarm stays on `setAlarmClock` (wall clock). Next to the countdown's end (epoch ms, which a time zone change doesn't affect) we keep `elapsedRealtime` and the time left at that moment, plus the boot count. On `TIME_SET` / `TIMEZONE_CHANGED` (already received by `RescheduleReceiver`) the time left is recomputed from the monotonic clock, and the end is moved and scheduled again. After a reboot `elapsedRealtime` starts over, so the wall clock is trusted and a new anchor is taken. Pure logic in `chain/CountdownTiming.kt`, unit-tested.
- **Why:** it keeps the alarm type already proven on the Samsung and covers moving the clock forward and back (moving it back would otherwise delay the next alarm, a way to escape).
- **Known limit:** a time change while the phone is off (or before the first unlock after a reboot) isn't detected.
- **Discarded:** (1) wall clock only: no work, but moving the clock shortens or lengthens the countdown; (3) `ELAPSED_REALTIME_WAKEUP` alarms: immune by design, but they drop `setAlarmClock`, which is the most reliable type against Doze and Samsung's battery management, and would need the step 1 reliability tests redone; (4) blocking the time change: not possible for an app.

### Countdown foreground service of type `systemExempted`
- **Decision:** `RoutineCountdownService` uses the `systemExempted` type with the normal (install-time) permission `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`. The type is passed on API 34+ only; on 12/13 types aren't enforced.
- **Why:** Android reserves it for apps that hold `USE_EXACT_ALARM` and keep a foreground service for their alarms, which is our case. It has no time limit and may start after a reboot.
- **Discarded:** `dataSync` (6 h per day on Android 15+, and a task can last 12 h), `shortService` (3 min), `mediaPlayback` (nothing plays, and Android 15+ forbids it from `BOOT_COMPLETED`). Fallback if it fails on a device: `specialUse`.
- **The service only shows the countdown.** The next task's alarm (or the routine's end) is always in AlarmManager, so it rings even if the service is killed. A watchdog (exact alarm every 60 s) brings the notification back.
- **Time left in the notification:** a system chronometer counting down (`setChronometerCountDown`), which updates itself every second without waking the app. After a time change the notification is posted again with the new end.

### Destructive migration to database version 3 (chosen by the user)
- **Decision:** the tasks need a name and a duration in seconds (`alarms.name`, `alarms.durationSeconds` instead of `activityDurationMinutes`). Room keeps `fallbackToDestructiveMigration`: installing step 4 deletes the saved alarms **and passwords**.
- **Why:** saves the time of a manual migration before the deadline; there's no real user data yet. Real migrations are still needed before the app is used for real (see step 1).
- **Old scheduled alarms:** alarms scheduled by step 3 have no routine id in their intent and are ignored when they fire.

### A task is an `Alarm` in the code
- **Decision:** the UI says "Task"; the code keeps the `Alarm` entity and table (each row is a task: its alarm, tone, challenges, name and duration). The list and editor packages were renamed (`routinelist`, `routineedit`).
- **Why:** renaming the entity would touch almost every file (scheduler, receivers, services, alarm screen) four days before the deadline, for no functional gain. `architecture.md` explains the mapping.

### Routine editor: one shared draft, task editor on its own screen
- **Decision:** the routine editor, the task editor and the step 3 screens share one `RoutineEditViewModel` (nested graph), which holds the whole routine. The task editor and the challenge screens act on the task chosen in the routine editor (`editingTaskIndex`). Save replaces all the routine's tasks and challenges in one transaction. A routine always keeps at least one task; tasks are reordered with up/down arrows, as challenges.
- **Duration entered in three numeric fields** (hours : minutes : seconds), chosen by the user over a timer-style keypad. Empty fields count as 0; invalid values leave the task without duration, so it can't be saved.
- **Earliest end** = start time + all durations; shown with "+n d" when it's on a later day.

### Running routine: one state, one runner
- **Decision:** `RoutineRunner` is the only place that moves a routine forward (ring → countdown → next task → end). The state (`RoutineRun`: routine, ringing task or countdown) is saved in one SharedPreferences commit before acting, so a reboot or kill continues from it. The ringing service no longer writes this state; it only plays the tone and runs the anti-evasion checks.
- **Countdown started from the alarm screen:** the alarm screen calls the runner while it's still visible and closes afterwards, because Android only lets a foreground service start from the foreground (or from an alarm).
- **Which alarm rings is decided in one pure function** (`decideAlarmFire`): routine start with nothing running → rings; routine start while another runs (ringing or any countdown) → skipped (switch off + notification); the running routine's expected next task or ringing task → rings; anything else (stale alarms, a watchdog after the alarm stopped) → ignored.
- **Lock:** `routineId` stays set during every countdown, so the step 3 locks (list, editor, repository) cover the countdowns with no extra code. Passwords used by any saved routine already can't be deleted.
- **"Next task" from the notification** opens `MainActivity` with an extra; the dialog is drawn by `MainActivity` over any screen. The dialog only applies to the countdown it was asked for: if the next task rings meanwhile, it disappears.

### Debug shortcuts (debug builds only)
- Stop on the alarm screen counts as completing the challenges. "3 s" (task editor), "All to 3 s" (routine editor; both were 1 s at first, changed on 2026-11-26 because 1 second was too fast to check that each screen is right), "Start now" (list; goes through the same check as a real start, so it can also test the "another routine is running" case), "End routine" (list row and notification) during any countdown.
