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

## Step 4 (started on 2026-11-25)

Completed on 2026-11-26.

Current step: 4 (alarm chaining). Step 3 completed on 2026-11-24. Update this line when a roadmap step is completed.

Goal of this step: the app's core feature. A routine is an ordered list of tasks. Each task starts with an alarm that is dismissed by completing its challenges. Completing them starts a countdown with the time assigned to that task, and when it ends the next task's alarm rings. The routine ends when the last task's countdown ends.

Terminology: in the UI, each link of the routine is called a "Task": "Add task", "Task 2 of 5", etc. Each task has its alarm (tone and challenges) and its duration.

Screens:
- Routine list: the alarm list becomes the routine list. Each row shows the name, the start time and how many tasks it has, with its switch and delete button, as now. A running routine shows which task it's on and, during a countdown, how much time is left.
- Routine editor: the current alarm editor becomes the routine editor: name, start time (only the first task has a fixed time) and the ordered list of its tasks. Tasks can be added, deleted and reordered. A new routine starts with one task. The editor shows the earliest time the routine would end: the start time plus the sum of all its tasks' durations, not counting the time spent on challenges.
- Task editor: tapping a task opens its configuration: name, tone, challenges (the step 3 screens) and duration, which is the countdown that starts once its challenges are completed. Each task is configured independently. A new task starts with the default alarm tone, no challenges and no duration.
- Duration: every task has one, including the last. Minimum 1 second, maximum 12 hours. The plan says how it's entered.
- All changes, to the routine and to any of its tasks and challenges, are saved together with "Save" in the routine editor and discarded when leaving with Back, as in step 3.
- A routine can't be saved if any of its tasks has no challenge or no duration. Saving enables the routine and schedules its first task's alarm, as now.

Alarm screen (every task, including the first):
- Besides the challenges, it shows the name of the task that starts, its position in the routine ("Task 2 of 5"), the routine name and the task's duration.
- Every alarm works like the current one: full screen, all the anti-evasion measures of steps 1 and 3, and challenges one by one in order, starting from zero after a reboot or if the service is killed.

During the countdown:
- When the last challenge of a task is completed, the tone stops, the alarm screen closes and that task's countdown starts. The next task's alarm is scheduled from that moment, not at a fixed time, so the time spent on the challenge doesn't eat into the task's time.
- When the last task's countdown ends, the routine simply ends: the notification disappears and the routine is unlocked, with no sound or alert.
- A foreground service stays active for the whole countdown. Its persistent notification shows the time left, kept up to date, and which task is next (or that it's the last one). The plan says which foreground service type is used and whether it needs a new permission.
- Next task: if the user finishes early, a button in the notification and in the list row makes the next task's alarm ring now, with its challenges. It first asks for confirmation with a dialog; from the notification, it opens the app with that dialog. During the last task's countdown the button becomes "End routine" and, with the same confirmation, ends the routine now.
- The phone can be used normally: the alarm screen isn't reopened and the volume isn't forced.
- The next alarm rings at its time even if the phone is locked, the screen is off, the phone was rebooted, or the service or the app was killed. If its time has already passed when the phone restarts, it rings as soon as possible.
- Changing the phone's time or time zone during the countdown doesn't make it longer or shorter. The plan lists the options with their pros and cons; I choose before it's implemented.

One routine at a time:
- A routine may be scheduled to start at a time when another one could be running; this isn't checked on save.
- When a routine's start time arrives, the app checks whether another one is running (ringing or in any countdown, including its last task's). If so, the new one doesn't run: it doesn't ring or lock anything, its switch turns off and a notification says it didn't run because another routine was running.

While a routine is running:
- From the moment its first alarm rings until its last task's countdown ends, the routine and its tasks and challenges can't be disabled, deleted or edited, and a password it uses can't be deleted. This already works while an alarm rings; now it must also cover the countdowns.
- When it ends, the routine is unlocked and its switch turns off, like the current one-shot alarms.

Debug builds only, to test quickly:
- The alarm screen's Stop button counts as completing the challenges: the chain continues with that task's countdown.
- A "3 s" button where a task's duration is set, which sets it to 3 seconds (changed from 1 s on 2026-11-26: 1 second was too fast to check the screens).
- An "All to 3 s" button in the routine editor, which sets all its tasks' durations to 3 seconds.
- A "Start now" button in the list, which makes the routine's first task's alarm ring immediately.
- An "End routine" button during any countdown (in the list row or in the notification), which ends the routine and unlocks it.

- Out of scope: QR/NFC (TFG). Phase 2 (TFG): repeating the routine and other typical alarm features; locking the routine some time before it starts; timer instead of start time, temporary mute, emergency exit, and blocking power-off, force stop, clearing app data or uninstalling. Escaping a routine during the countdown with these methods is a known risk to be addressed in Phase 2; no need to analyze it now.
- Must work on the Samsung with Android 13. Show me the plan before writing code: screens and navigation; data model changes and whether a migration is needed (and whether saved alarms are lost); the routine lifecycle end to end, including what is stored on disk to survive a reboot; the pure logic covered by tests; the options for time changes; any dependency or permission needed; and anything harder than expected as a candidate to cut.
