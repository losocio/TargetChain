# Architecture

State at the end of step 4 (alarm chaining, completed on 2026-11-26).

## Layers
Compose screen → ViewModel (single immutable `StateFlow` UI state, events from the screen) → Repository → Room. Dependencies are created once in `AppContainer` (manual DI) and reached through `context.appContainer`. ViewModels that schedule alarms also use `AlarmScheduler` directly; the ones that act on the running routine use `RoutineRunner`.

## Terminology
In the UI each link of a routine is a **task**. In the code a task is an `Alarm` entity (table `alarms`): the alarm that starts it, with its tone, challenges, name and duration. "Alarm id" and "task id" are the same thing.

## Package structure (`com.targetchain.targetchain`)
```
TargetChainApp.kt            Application; creates AppContainer (applicationScope, AlarmStateStore, RoutineRepository,
                             PasswordRepository, AlarmScheduler, RoutineRunner, RingtoneCatalog) and the notification channels
MainActivity.kt              launcher activity (singleTop); hosts TargetChainNavHost and the "Next task / End routine"
                             confirmation dialog, also opened from the countdown notification; autofill disabled; not resizable
data/
  model/  Routine.kt, Alarm.kt (a task), Challenge.kt, ChallengeType.kt, SavedPassword.kt   Room entities
          Challenge.kt also has ChallengeConfig (a challenge in the editor, before it's saved)
          TaskDraft.kt      a task in the editor (name, tone, duration, challenges), before it's saved
          SavedPassword.kt also has PasswordSummary (id + name, what the UI sees)
          RoutineWithAlarms.kt   routine + its tasks (@Relation); tasks (in order), firstAlarm
  db/     AppDatabase.kt     Room database (version 3, destructive migration during development)
          RoutineDao.kt      routines, tasks and challenges; saveRoutine (@Transaction, replaces all tasks)
          PasswordDao.kt     saved passwords; which routines use one
  RoutineRepository.kt       data access used by the ViewModels, receivers and services; refuses to save,
                             disable or delete a running routine
  PasswordRepository.kt      create (hash), rename, delete (refused while in use), check a typed password
chain/                       pure logic of routines (unit-tested)
  TaskDuration.kt            1 s..12 h range, H:M:S fields parsing, splitting, countdown text
  RoutineEnd.kt              earliest end (start time + all durations, may be days later)
  TaskRules.kt               a task needs a challenge and a duration
  RoutineRun.kt              RoutineRun / Countdown (the running routine's state), afterTask (next task or end),
                             decideAlarmFire (ring / skip because another routine runs / ignore),
                             countdownAction (what to schedule after a reboot or time change)
  CountdownTiming.kt         countdown end on the wall clock + monotonic anchor; time left; re-anchoring
challenge/                   pure logic of challenges (unit-tested)
  PasswordHasher.kt          normalization (trim) + PBKDF2 hash with salt + comparison
  PasswordRules.kt           password name and new password validation
  TextInsertion.kt           insertedCharCount(before, after): rejects pasting in the challenge field
  ChallengeProgress.kt       current challenge / finished; List.moved for reordering
alarm/
  NextTrigger.kt             pure function nextTriggerTime(now, minutesOfDay) (unit-tested)
  AlarmScheduler.kt          setAlarmClock for tasks (routine start, next task, ringing watchdog) with PendingIntents
                             to AlarmReceiver; exact alarms for the routine's end and the countdown watchdog
                             (to RoutineEventReceiver)
  AlarmReceiver.kt           a task's alarm fired; asks RoutineRunner (goAsync)
  RoutineEventReceiver.kt    last countdown ended, countdown watchdog, debug "End routine" from the notification
  RescheduleReceiver.kt      after reboot, app update, time/time zone change, exact alarm permission granted:
                             schedules the first task of enabled routines (not the running one) and resumes the
                             running routine
  RoutineRunner.kt           moves the running routine through its tasks (ring → countdown → next task → end);
                             persists every step in AlarmStateStore; serialized with a mutex
  AlarmRingingService.kt     foreground service (mediaPlayback): ringing notification, ExoPlayer tone,
                             keeps the alarm screen shown, forces the volume; completed challenges (in memory)
  RoutineCountdownService.kt foreground service (systemExempted) during a countdown: persistent notification with
                             a system chronometer, the next task and the Next task / End routine button
  AlarmNotifications.kt      channels (ringing, countdown, notices); ringing, countdown and "routine skipped"
                             notifications
  AlarmStateStore.kt         SharedPreferences: the running routine (RoutineRun, also as a flow), alarm volume
                             before ringing, silent test mode (debug)
ringtone/
  RingtoneCatalog.kt         preinstalled alarm tones (RingtoneManager TYPE_ALARM) and tone titles
  RingtonePreviewPlayer.kt   ExoPlayer preview on the alarm stream (muted in silent test mode)
permissions/
  AlarmPermissions.kt        PermissionStatus + checks and settings intents
ui/
  theme/                     Material 3 theme (template)
  navigation/TargetChainNavHost.kt   Navigation Compose host, string routes, nested routine edit graph
  routinelist/ RoutineListScreen.kt, RoutineListViewModel.kt, RoutineListUiState.kt
               routines: switch, delete, add; running routine locked, with its task and time left and the
               Next task / End routine button; permissions card; Start now, End routine and silent test mode (debug)
  routineedit/ RoutineEditScreen.kt, TaskEditScreen.kt, RoutineEditViewModel.kt, RoutineEditUiState.kt
               routine (name, start time, tasks: add, delete, up/down, earliest end) and task (name, tone,
               challenges, H:M:S duration); one draft shared by the whole edit graph; Save (every task needs
               a challenge and a duration) enables and schedules; Next minute, All to 3 s, 3 s (debug)
  routinerun/  AdvanceRoutineViewModel.kt, AdvanceRoutineDialog.kt   confirmation before Next task / End routine
  ringtone/    RingtonePickerScreen.kt, RingtonePickerViewModel.kt, RingtonePickerUiState.kt
               tone list with preview
  challenges/  ChallengeListScreen.kt (the editing task's challenges: order, delete, up/down),
               ChallengeTypeScreen.kt, ChallengeLabels.kt (available types, labels)
  password/    PasswordChallengeScreen.kt + ViewModel (choose, rename, delete saved passwords),
               NewPasswordScreen.kt + ViewModel, PasswordErrors.kt
  permissions/PermissionsCard.kt   permission rows with Grant buttons
  common/      TimeFormatting.kt, DurationFormatting.kt, PasswordInput.kt (password keyboard options,
               TypedByHandOnly input transformation, NoTextToolbar)
  alarm/       AlarmActivity.kt, AlarmScreen.kt, AlarmViewModel.kt, AlarmUiState.kt
               alarm screen over the lock screen: task, "Task i of n", routine, duration and the challenges
               one by one; Stop (debug only) counts as completing them
```
Tests: `app/src/test/.../alarm/NextTriggerTest.kt`, `challenge/PasswordHasherTest.kt`, `challenge/ChallengeLogicTest.kt`, `chain/ChainLogicTest.kt`.

## Navigation
```
routines ──(+ / tap a row, not while running)──> routine/{routineId}   nested graph (-1 = new routine),
                                                                       one RoutineEditViewModel for all its screens
  routineEditor (start)       name, start time, tasks, Save
    └─(tap a task / Add task)──> task       the task chosen in the editor (editingTaskIndex); Back keeps the draft
          ├─(Ringtone)──> ringtone?selectedUri={uri}    OK: onRingtonePicked on the shared ViewModel, pop
          └─(Challenges)──> challenges                  the task's challenges; delete; up/down; "Add challenge"
                              └──> challengeTypes       one button per available type (only Password)
                                     └──> passwordChallenge   choose / rename / delete saved passwords
                                            ├──> newPassword  creates it; sets "createdPassword" on the
                                            │                 previous entry, which selects it; pops
                                            └─ Add: adds the challenge at the end of the task and pops
                                                    back to challenges (skipping challengeTypes)
```
Navigation callbacks run only while their destination is resumed (`ifResumed`), so double taps don't navigate twice. The editor closes (pops the whole graph, discarding the draft) on Back or when its state reports `isSaved`. The "Next task / End routine" dialog is drawn by `MainActivity` over any screen; the list row and the countdown notification (through an intent extra) ask for it.

## Data model (Room)
- **`routines`** (`Routine`): `id`, `name` (blank = shown as "Routine"), `isEnabled` (its switch).
- **`alarms`** (`Alarm`, a task): `id`, `routineId` (FK → routines, cascade delete, indexed), `position` (order in the routine), `name` (blank = shown as "Task n"), `startTimeMinutes: Int?` (routine start time, minutes since midnight; only on the first task), `durationSeconds: Int?` (countdown after its challenges, 1 s..12 h; required to save), `ringtoneUri: String?` (null = system default alarm tone).
- **`challenges`** (`Challenge`): `id`, `alarmId` (FK → alarms, cascade delete, indexed), `position` (order in which they're shown), `type: ChallengeType` (`PASSWORD`, `BARCODE`, `NFC`, stored as text), `passwordId: Long?` (FK → passwords, RESTRICT, indexed; used by `PASSWORD`), `expectedValue: String?` (unused; reserved for the QR/NFC challenges, moved to the TFG: code or NFC tag id).
- **`passwords`** (`SavedPassword`): `id`, `name` (unique, `NOCASE`), `hash`, `salt` (PBKDF2-HMAC-SHA256, see `PasswordHasher`). Shared by all routines; never shown after creation.

`RoutineDao.saveRoutine` creates or updates the routine and replaces all its tasks and their challenges in one transaction (the routine can't be running then). Deleting a routine deletes its tasks and challenges (cascade). A password can't be deleted while a challenge uses it. The list is ordered by start time. Version 3 (step 4) changed `alarms`; the migration is destructive (decided in step 4, see `decisions.md`).

## Running routine (persisted in `AlarmStateStore`)
`RoutineRun`: `routineId` (the running routine; while set it's locked), `ringingAlarmId` (task whose alarm rings) or `countdown` (`alarmId`, `nextAlarmId` (null = last task) and `CountdownTiming`: `endAtWallMs`, `anchorElapsedMs`, `remainingAtAnchorMs`, `bootCount`). Each change is one SharedPreferences `commit`, so a reboot or kill never finds it half written. Also `previousAlarmVolume` (restored when an alarm stops).

## Lifecycle of a routine

| Stage | How |
|---|---|
| Scheduled | Editor Save (every task with a challenge and a duration) → `RoutineEditViewModel.onSave` → `RoutineRepository.saveRoutine` (enables it; refused if running) → cancel the old first task's alarm → `AlarmScheduler.schedule(first task)` → `setAlarmClock` at `nextTriggerTime` with extras routine id, task id and `isRoutineStart`. If scheduling fails (no exact alarm permission) the routine is disabled again. List switch on/off schedules/cancels the first task. `RescheduleReceiver` schedules the first task of every enabled routine except the running one after reboot, app update, time/time zone change or exact alarm permission granted. |
| Start time arrives | `AlarmReceiver` → `RoutineRunner.onAlarmFired` → `decideAlarmFire`: nothing running → rings; another routine running (ringing or any countdown) → **skipped**: its switch turns off and a "didn't run" notification is shown; stale alarms are ignored. Debug "Start now" takes the same path. |
| Rings | `startRinging`: `RoutineRun(routineId, ringingAlarmId)` saved, countdown service stopped, `AlarmRingingService.start`. The service shows the ringing notification (full-screen intent to `AlarmActivity`), saves the alarm volume, resets the completed challenges, plays the task's tone in a loop. |
| Anti-evasion while ringing | Every second the service reopens `AlarmActivity` if it isn't visible (needs "Display over other apps") and puts the alarm volume back to max. `AlarmActivity` ignores Back and volume down, isn't resizable. A watchdog alarm (`setAlarmClock`, every 60 s) rings it again if the process was killed; after a reboot `RoutineRunner.resume` schedules the ringing task again a few seconds later. Challenges start from zero in both cases. |
| Challenge | `AlarmViewModel` loads the task (name, "Task i of n", routine, duration) and its challenges in order, combined with `AlarmRingingService.completedChallenges` (in memory). Password: typed by hand only; wrong → message, field cleared, tone keeps ringing. |
| Countdown starts | After the last challenge (or debug Stop), `AlarmActivity` (still visible) calls `RoutineRunner.completeRingingTask`: reads the tasks, `afterTask` gives the next one, `startCountdown(now, duration)`; one write replaces the ringing state with the countdown; the next task's alarm is scheduled with `setAlarmClock` at the countdown's end (or, for the last task, an exact alarm that ends the routine); the countdown watchdog is scheduled; `RoutineCountdownService` starts; the tone stops and the volume is restored. The countdown is measured from this moment, so the time spent on challenges doesn't count. |
| During the countdown | `RoutineCountdownService` (foreground, `systemExempted`) shows a silent persistent notification: "Task i of n · name", a system chronometer counting down to `endAtWallMs`, "Next: …" or "Last task", and a Next task / End routine button that opens the app with the confirmation dialog (debug: End routine directly). The list row shows the task and the time left. No screen reopening, no volume forcing. The routine stays locked (`routineId` set). If the service is killed the next alarm still rings (it's in AlarmManager); the countdown watchdog (exact alarm, every 60 s) starts the service again. |
| Next task early | Confirmed "Next task" → `RoutineRunner.advance`: cancels the next task's scheduled alarm and makes it ring now (`startRinging`). During the last task's countdown it ends the routine instead. |
| Next task on time | The next task's alarm fires → `decideAlarmFire` (it's the expected next task) → rings. |
| Time or time zone changed, reboot | `RescheduleReceiver` → `RoutineRunner.resume`: a countdown is re-anchored (`CountdownTiming.reanchored`: time left from the monotonic clock in the same boot, from the wall clock after a reboot) and its next alarm or end is scheduled again; a next alarm whose time has passed rings 3 s later; a last countdown that has ended ends the routine. The countdown service is started again (its notification shows the new end). |
| Routine ends | Last countdown's alarm → `RoutineEventReceiver` → `onCountdownEnded` → `endRoutine`: state cleared, pending alarms cancelled, countdown service stopped (notification gone), routine disabled (switch off). No sound or alert. Also from "End routine" (confirmed) and debug "End routine". |
