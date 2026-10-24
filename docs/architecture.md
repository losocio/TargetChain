# Architecture

State at the end of step 2.

## Layers
Compose screen → ViewModel (single immutable `StateFlow` UI state, events from the screen) → Repository → Room. Dependencies are created once in `AppContainer` (manual DI) and reached through `context.appContainer`. ViewModels that schedule alarms also use `AlarmScheduler` directly.

## Package structure (`com.targetchain.targetchain`)
```
TargetChainApp.kt            Application; creates AppContainer (applicationScope, RoutineRepository, AlarmScheduler,
                             AlarmStateStore, RingtoneCatalog) and the notification channel
MainActivity.kt              launcher activity; hosts TargetChainNavHost
data/
  model/  Routine.kt, Alarm.kt, Challenge.kt, ChallengeType.kt   Room entities
          RoutineWithAlarms.kt   routine + its alarms (@Relation); firstAlarm
  db/     AppDatabase.kt     Room database (version 1, destructive migration during development)
          RoutineDao.kt      queries for routines and alarms; saveSingleAlarmRoutine (@Transaction)
  RoutineRepository.kt       data access used by the ViewModels, receivers and service
alarm/
  NextTrigger.kt             pure function nextTriggerTime(now, minutesOfDay) (unit-tested)
  AlarmScheduler.kt          setAlarmClock / cancel with PendingIntents to AlarmReceiver
  AlarmReceiver.kt           fired at the alarm time; starts AlarmRingingService
  RescheduleReceiver.kt      reschedules enabled routines after reboot, app update, time/time zone change,
                             exact alarm permission granted
  AlarmRingingService.kt     foreground service (mediaPlayback): notification, ExoPlayer tone,
                             keeps the alarm screen shown
  AlarmNotifications.kt      notification channel + ringing notification with full-screen intent
  AlarmStateStore.kt         SharedPreferences: ringing alarm id, alarm volume before ringing,
                             silent test mode (debug)
ringtone/
  RingtoneCatalog.kt         preinstalled alarm tones (RingtoneManager TYPE_ALARM) and tone titles
  RingtonePreviewPlayer.kt   ExoPlayer preview on the alarm stream (muted in silent test mode)
permissions/
  AlarmPermissions.kt        PermissionStatus + checks and settings intents
ui/
  theme/                     Material 3 theme (template)
  navigation/TargetChainNavHost.kt   Navigation Compose host, string routes
  alarmlist/   AlarmListScreen.kt, AlarmListViewModel.kt, AlarmListUiState.kt
               list of alarms: switch, delete, add; permissions card; silent test mode switch (debug)
  alarmedit/   AlarmEditScreen.kt, AlarmEditViewModel.kt, AlarmEditUiState.kt
               name, time, ringtone; Save enables and schedules
  ringtone/    RingtonePickerScreen.kt, RingtonePickerViewModel.kt, RingtonePickerUiState.kt
               tone list with preview
  permissions/PermissionsCard.kt   permission rows with Grant buttons
  common/TimeFormatting.kt         formatTime(minutesOfDay)
  alarm/  AlarmActivity.kt, AlarmScreen.kt   alarm screen over the lock screen, temporary Stop button
```
Tests: `app/src/test/.../alarm/NextTriggerTest.kt`.

## Navigation
```
alarms ──(+ / tap a row)──> alarm/{routineId}        (-1 = new alarm)
                                 └──(Ringtone)──> ringtone?selectedUri={uri}
                                                    └─ OK: sets "pickedRingtone" on the editor's
                                                       back stack entry ("" = default tone) and pops
```
Navigation callbacks run only while their destination is resumed (`ifResumed`), so double taps don't navigate twice. The editor closes itself when its state reports `isSaved`.

## Data model (Room)
- **`routines`** (`Routine`): `id`, `name`, `isEnabled`.
- **`alarms`** (`Alarm`): `id`, `routineId` (FK → routines, cascade delete, indexed), `position` (order in the chain), `startTimeMinutes: Int?` (time of day in minutes since midnight; used by the first alarm), `activityDurationMinutes: Int?` (countdown after its challenges; null = routine ends), `ringtoneUri: String?` (null = system default alarm tone).
- **`challenges`** (`Challenge`): `id`, `alarmId` (FK → alarms, cascade delete, indexed), `position`, `type: ChallengeType` (`PASSWORD`, `BARCODE`, `NFC`, stored as text), `expectedValue: String?` (password, code or NFC tag id).

Until chaining (step 4), every routine has exactly one alarm, and the UI shows each routine as one alarm: `Routine.name` is the alarm name (blank = shown as "Alarm"), `Routine.isEnabled` is its switch, and the alarm at position 0 holds the time and the tone. `RoutineDao.saveSingleAlarmRoutine` creates or updates both in one transaction. Deleting a routine deletes its alarm and challenges (cascade). The list is ordered by time.

## Lifecycle of an alarm

| Stage | Status | How |
|---|---|---|
| Scheduled | Implemented (first alarm of a routine) | Editor Save → `AlarmEditViewModel.onSave` → `RoutineRepository.saveSingleAlarmRoutine` (enables the routine) → `AlarmScheduler.schedule` → `setAlarmClock` at `nextTriggerTime`; if scheduling fails (no exact alarm permission) the routine is disabled again. List switch on → `AlarmListViewModel.onEnabledChange` schedules and then enables. Switch off or delete → `AlarmScheduler.cancel` + routine disabled or deleted; this doesn't stop an alarm that is already ringing. `RescheduleReceiver` schedules again the first alarm of every enabled routine after reboot, app update, time/time zone change or exact alarm permission granted. |
| Rings | Implemented | `AlarmReceiver` → `AlarmRingingService.start` → `startForeground` with the ringing notification (full-screen intent to `AlarmActivity`). The service saves `ringingAlarmId` and the current alarm volume in `AlarmStateStore`, reads the alarm from Room and plays its tone (`ringtoneUri`, chosen in the editor; null = default alarm tone) with ExoPlayer in a loop (fallback to default ringtone on error; muted in debug silent test mode). |
| Anti-evasion while ringing | Implemented | Every second the service reopens `AlarmActivity` if it isn't visible (needs "Display over other apps") and puts the alarm volume back to max. `AlarmActivity` ignores Back and volume down. A watchdog alarm (`setAlarmClock`, every 60 s) starts the service again if the process was killed; the service also restarts with `START_REDELIVER_INTENT`. After a reboot, `RescheduleReceiver` schedules the alarm that was still ringing a few seconds later. |
| Challenge | Not implemented | `Challenge` entities exist but aren't used. The alarm screen has a temporary "Stop (temporary)" button: `AlarmRingingService.stop` clears `ringingAlarmId`, cancels the watchdog, restores the previous alarm volume, stops the service and disables the routine. |
| Countdown | Not implemented | `activityDurationMinutes` is stored but not used. |
| Next alarm | Not implemented | Alarms are one-shot: when one is stopped, its routine is disabled (its switch turns off), so it isn't rescheduled. |
