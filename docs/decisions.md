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
