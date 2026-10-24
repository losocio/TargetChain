# Steps and functionality

One entry per roadmap step, with the text of its "Current status" section in CLAUDE.md at that time.

## Step 1 (original definition, until 2026-10-23)

Current step: 1 (project setup, data model, alarm that rings reliably). Update this line when a roadmap step is completed.

Goal of this step: a minimal end-to-end skeleton.
- Project set up per this file (Compose, Room, MVVM).
- Room entities only (Routine -> ordered Alarms -> Challenges), no editing UI yet.
- One test alarm via `AlarmManager.setAlarmClock` that opens a full-screen alarm screen over the lock screen, with a ringtone and a temporary stop button.
- Android 12+ permissions (exact alarms, notifications, full-screen intent) and rescheduling after reboot.
- Must work on the Samsung with Android 13. Show me the plan and package structure before writing code.

## Step 1 (redefined on 2026-10-23)

Completed on 2026-10-24.

Current step: 1 (alarm that rings at a set time, full screen, with its anti-evasion measures). Update this line when a roadmap step is completed.

Goal of this step: one alarm that rings at a set time, covers the whole screen and can't be evaded.
- One test alarm via `AlarmManager.setAlarmClock` that rings at an exact time (also with the screen off or locked, and after reboot) and opens a full-screen alarm screen with a ringtone and a temporary stop button.
- The Android 12+ permissions it needs (exact alarms, notifications, full-screen intent, display over other apps).
- Anti-evasion for the alarm screen: Back blocked and the screen reopened after Home/Recents (done); block lowering the volume while ringing; keep ringing after a reboot or if the ringing service is killed.
- Debug-only silent mode, to test the alarm without sound.
- Architecture for settings, navigation and the rest is added only when a step needs it. The Room data model already in the code is kept.
- Must work on the Samsung with Android 13. Show me the plan before writing code.

## Step 2 (started on 2026-10-24)

Completed on 2026-10-24.

Current step: 2 (alarm list and alarm editor, single alarms only). Step 1 completed on 2026-10-24. Update this line when a roadmap step is completed.

Goal of this step: create, edit, delete and enable/disable single alarms. Reduced scope compared to the original roadmap: no routine screens and no placeholders; each screen is added when its logic exists.
- Alarm list screen: see all alarms, create a new one, open one to edit it, delete it, and enable/disable it with a switch.
- Alarm editor screen: name, time and ringtone.
- Ringtone selection from a preinstalled list, with preview (it plays when chosen and stops when leaving).
- Enabling an alarm schedules it with the step 1 system; disabling it cancels it.
- Internally, each alarm is stored as a one-link routine, reusing the existing Room model, so step 4 (chaining) only has to extend the editor to add more alarms.
- Navigation Compose and MVVM with UDF, as described in this file.
- Out of scope: challenge configuration (step 3) and anything about chaining (step 4).
- Must work on the Samsung with Android 13. Show me the plan before writing code.
