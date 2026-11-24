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

## Step 3 (started on 2026-11-23)

Completed on 2026-11-24.

Current step: 3 (challenge system and password challenge). Step 2 completed on 2026-10-24. Update this line when a roadmap step is completed.

Goal of this step: the first step where an alarm is dismissed by completing challenges instead of the temporary button.

Configuration screens:
- Challenge list: the alarm editor has a "Challenges" button (showing how many there are) that opens a separate screen with the alarm's challenges in order. A new alarm starts empty with only an "Add challenge" button. Challenges can be deleted and reordered.
- Type selector: "Add challenge" opens a screen with one button per available challenge type. For now only Password; QR and NFC are not shown, not even disabled.
- Type configuration: tapping a type opens its configuration screen. Confirming adds the challenge at the end of the list and goes back to it.
- Challenge changes are part of editing the alarm: saved with "Save" and discarded when leaving with Back, like name, time and ringtone.
- An alarm can't be saved without at least one challenge. The temporary "Stop" button stays only in debug builds.

Password challenge configuration:
- Saved passwords: the app keeps a list of reusable passwords, each with a name that identifies it (not empty, not repeated). The Password configuration shows that list by name and a "New password" button. Several alarms can use the same password.
- New password: name and password, with the password typed twice. While creating it, the password is visible and can be pasted from the clipboard. Once created, it is selected.
- After creation the password is never shown again in the app, only its name. The creation screen briefly explains that it must be written down outside the phone, near the activity the alarm marks (e.g. on paper on the desk, or in a file on the work computer).
- Passwords can be renamed and deleted.
- The challenge can't be created while no password is selected, including when none exists yet.

During a challenge (any type):
- When the alarm rings, its challenges appear one by one in the configured order, and the alarm only stops after the last one. The tone keeps ringing during the challenges.
- The challenge screen can't be left until completed, same as the alarm screen: Back and volume down blocked, reopened after Home, Recents or screen off, keeps ringing after a reboot or if the service is killed. New challenge types must get all this with nothing extra; if it's a separate screen, the per-second check must cover it too.
- After a reboot or a killed service, the alarm's challenges start from zero, even if some were already completed.

While a routine is running:
- At no point of a routine's execution (while an alarm rings, while a challenge is being completed and, from step 4, during the countdown between alarms) can its alarms or challenges be edited or deleted, nor a password used by an alarm of that routine be deleted.

Password challenge while ringing:
- A wrong password doesn't count: a warning is shown, the field is cleared and the tone keeps ringing.
- It's always typed by hand. Nothing can be pasted: not from the field's menu, the Samsung keyboard clipboard, autofill or suggestions.
- The keyboard must not learn or suggest the password, also when creating it.

- Out of scope: QR/NFC (step 5), chaining and countdown (step 4), random challenge order (TFG).
- Must work on the Samsung with Android 13. Show me the plan before writing code.
