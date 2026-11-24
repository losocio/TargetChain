# Architecture

State at the end of step 3 (completed on 2026-11-24).

## Layers
Compose screen → ViewModel (single immutable `StateFlow` UI state, events from the screen) → Repository → Room. Dependencies are created once in `AppContainer` (manual DI) and reached through `context.appContainer`. ViewModels that schedule alarms also use `AlarmScheduler` directly.

## Package structure (`com.targetchain.targetchain`)
```
TargetChainApp.kt            Application; creates AppContainer (applicationScope, AlarmStateStore, RoutineRepository,
                             PasswordRepository, AlarmScheduler, RingtoneCatalog) and the notification channel
MainActivity.kt              launcher activity; hosts TargetChainNavHost; autofill disabled; not resizable
data/
  model/  Routine.kt, Alarm.kt, Challenge.kt, ChallengeType.kt, SavedPassword.kt   Room entities
          Challenge.kt also has ChallengeConfig (a challenge in the editor, before it's saved)
          SavedPassword.kt also has PasswordSummary (id + name, what the UI sees)
          RoutineWithAlarms.kt   routine + its alarms (@Relation); firstAlarm
  db/     AppDatabase.kt     Room database (version 2, destructive migration during development)
          RoutineDao.kt      routines, alarms and challenges; saveSingleAlarmRoutine (@Transaction)
          PasswordDao.kt     saved passwords; which alarms use one
  RoutineRepository.kt       data access used by the ViewModels, receivers and service; refuses to save,
                             disable or delete a running routine
  PasswordRepository.kt      create (hash), rename, delete (refused while in use), check a typed password
challenge/                   pure logic (unit-tested)
  PasswordHasher.kt          normalization (trim) + PBKDF2 hash with salt + comparison
  PasswordRules.kt           password name and new password validation
  TextInsertion.kt           insertedCharCount(before, after): rejects pasting in the challenge field
  ChallengeProgress.kt       current challenge / finished; List.moved for reordering
alarm/
  NextTrigger.kt             pure function nextTriggerTime(now, minutesOfDay) (unit-tested)
  AlarmScheduler.kt          setAlarmClock / cancel with PendingIntents to AlarmReceiver
  AlarmReceiver.kt           fired at the alarm time; starts AlarmRingingService
  RescheduleReceiver.kt      reschedules enabled routines after reboot, app update, time/time zone change,
                             exact alarm permission granted
  AlarmRingingService.kt     foreground service (mediaPlayback): notification, ExoPlayer tone,
                             keeps the alarm screen shown; completed challenges (in memory)
  AlarmNotifications.kt      notification channel + ringing notification with full-screen intent
  AlarmStateStore.kt         SharedPreferences: ringing alarm id, running routine id (also as a flow),
                             alarm volume before ringing, silent test mode (debug)
ringtone/
  RingtoneCatalog.kt         preinstalled alarm tones (RingtoneManager TYPE_ALARM) and tone titles
  RingtonePreviewPlayer.kt   ExoPlayer preview on the alarm stream (muted in silent test mode)
permissions/
  AlarmPermissions.kt        PermissionStatus + checks and settings intents
ui/
  theme/                     Material 3 theme (template)
  navigation/TargetChainNavHost.kt   Navigation Compose host, string routes, nested alarm edit graph
  alarmlist/   AlarmListScreen.kt, AlarmListViewModel.kt, AlarmListUiState.kt
               list of alarms: switch, delete, add; running alarm locked; permissions card;
               silent test mode switch (debug)
  alarmedit/   AlarmEditScreen.kt, AlarmEditViewModel.kt, AlarmEditUiState.kt
               name, time, ringtone, challenges (draft shared by the whole edit graph);
               Save (needs a challenge) enables and schedules
  ringtone/    RingtonePickerScreen.kt, RingtonePickerViewModel.kt, RingtonePickerUiState.kt
               tone list with preview
  challenges/  ChallengeListScreen.kt (order, delete, up/down), ChallengeTypeScreen.kt,
               ChallengeLabels.kt (available types, labels)
  password/    PasswordChallengeScreen.kt + ViewModel (choose, rename, delete saved passwords),
               NewPasswordScreen.kt + ViewModel, PasswordErrors.kt
  permissions/PermissionsCard.kt   permission rows with Grant buttons
  common/      TimeFormatting.kt (formatTime), PasswordInput.kt (password keyboard options,
               TypedByHandOnly input transformation, NoTextToolbar)
  alarm/       AlarmActivity.kt, AlarmScreen.kt, AlarmViewModel.kt, AlarmUiState.kt
               alarm screen over the lock screen with the challenges one by one; Stop (debug only)
```
Tests: `app/src/test/.../alarm/NextTriggerTest.kt`, `challenge/PasswordHasherTest.kt`, `challenge/ChallengeLogicTest.kt`.

## Navigation
```
alarms ──(+ / tap a row, not while running)──> alarm/{routineId}   nested graph (-1 = new alarm),
                                                                   one AlarmEditViewModel for all its screens
  alarmEditor (start)
    ├─(Ringtone)──> ringtone?selectedUri={uri}      OK: onRingtonePicked on the shared ViewModel, pop
    └─(Challenges)──> challenges                    list; delete; up/down; "Add challenge"
                        └──> challengeTypes         one button per available type (only Password)
                               └──> passwordChallenge   choose / rename / delete saved passwords
                                      ├──> newPassword  creates it; sets "createdPassword" on the
                                      │                 previous entry, which selects it; pops
                                      └─ Add: adds the challenge at the end of the draft and pops
                                              back to challenges (skipping challengeTypes)
```
Navigation callbacks run only while their destination is resumed (`ifResumed`), so double taps don't navigate twice. The editor closes (pops the whole graph, discarding the draft) on Back or when its state reports `isSaved`.

## Data model (Room)
- **`routines`** (`Routine`): `id`, `name`, `isEnabled`.
- **`alarms`** (`Alarm`): `id`, `routineId` (FK → routines, cascade delete, indexed), `position` (order in the chain), `startTimeMinutes: Int?` (time of day in minutes since midnight; used by the first alarm), `activityDurationMinutes: Int?` (countdown after its challenges; null = routine ends), `ringtoneUri: String?` (null = system default alarm tone).
- **`challenges`** (`Challenge`): `id`, `alarmId` (FK → alarms, cascade delete, indexed), `position` (order in which they're shown), `type: ChallengeType` (`PASSWORD`, `BARCODE`, `NFC`, stored as text), `passwordId: Long?` (FK → passwords, RESTRICT, indexed; used by `PASSWORD`), `expectedValue: String?` (for step 5: code or NFC tag id).
- **`passwords`** (`SavedPassword`): `id`, `name` (unique, `NOCASE`), `hash`, `salt` (PBKDF2-HMAC-SHA256, see `PasswordHasher`). Shared by all alarms; never shown after creation.

Until chaining (step 4), every routine has exactly one alarm, and the UI shows each routine as one alarm: `Routine.name` is the alarm name (blank = shown as "Alarm"), `Routine.isEnabled` is its switch, and the alarm at position 0 holds the time, the tone and the challenges. `RoutineDao.saveSingleAlarmRoutine` creates or updates the routine and alarm and replaces the alarm's challenges in one transaction. Deleting a routine deletes its alarm and challenges (cascade). A password can't be deleted while a challenge uses it. The list is ordered by time.

## Lifecycle of an alarm

| Stage | Status | How |
|---|---|---|
| Scheduled | Implemented (first alarm of a routine) | Editor Save (only with at least one challenge) → `AlarmEditViewModel.onSave` → `RoutineRepository.saveSingleAlarmRoutine` (enables the routine; refused if it's running) → `AlarmScheduler.schedule` → `setAlarmClock` at `nextTriggerTime`; if scheduling fails (no exact alarm permission) the routine is disabled again. List switch on → `AlarmListViewModel.onEnabledChange` schedules and then enables. Switch off or delete → `AlarmScheduler.cancel` + routine disabled or deleted. `RescheduleReceiver` schedules again the first alarm of every enabled routine after reboot, app update, time/time zone change or exact alarm permission granted. |
| Rings | Implemented | `AlarmReceiver` → `AlarmRingingService.start` → `startForeground` with the ringing notification (full-screen intent to `AlarmActivity`). The service saves `ringingAlarmId` and the current alarm volume in `AlarmStateStore`, resets the completed challenges to 0, reads the alarm from Room, sets `activeRoutineId` to its routine and plays its tone (`ringtoneUri`; null = default alarm tone) with ExoPlayer in a loop (fallback to default ringtone on error; muted in debug silent test mode). |
| Routine locked | Implemented | While `activeRoutineId` is set (persisted, survives a reboot), the list shows the alarm as "Running" with switch and delete disabled and doesn't open the editor; `RoutineRepository` refuses to save, disable or delete it; the editor shows a message and disables Save. Cleared when the alarm stops; from step 4 it will stay set during the countdown. |
| Anti-evasion while ringing | Implemented | Every second the service reopens `AlarmActivity` if it isn't visible (needs "Display over other apps") and puts the alarm volume back to max. `AlarmActivity` ignores Back and volume down. A watchdog alarm (`setAlarmClock`, every 60 s) starts the service again if the process was killed; the service also restarts with `START_REDELIVER_INTENT`. After a reboot, `RescheduleReceiver` schedules the alarm that was still ringing a few seconds later. Since the challenges are part of `AlarmActivity`, all of this covers them. `AlarmActivity` is not resizable, so it isn't shown in a pop-up window or split screen (another app's pop-up over it is a known limitation). |
| Challenge | Implemented (password) | `AlarmViewModel` loads the alarm's challenges in order and combines them with `AlarmRingingService.completedChallenges` (in memory: a reboot or kill starts over from the first). The screen shows "Challenge i of n" and the current one. Password: typed by hand only (`TypedByHandOnly`, no paste menu, no autofill); `PasswordRepository.matches` checks the hash off the main thread; wrong → message, field cleared, tone keeps ringing; right → `completeChallenge(index)`. After the last one, `AlarmActivity` calls `AlarmRingingService.stop`, which clears `ringingAlarmId` and `activeRoutineId`, cancels the watchdog, restores the previous alarm volume, stops the service and disables the routine. Debug builds (or an alarm without challenges) also show a Stop button. |
| Countdown | Not implemented | `activityDurationMinutes` is stored but not used. |
| Next alarm | Not implemented | Alarms are one-shot: when one is stopped, its routine is disabled (its switch turns off), so it isn't rescheduled. |
