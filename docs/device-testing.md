# Device testing

Manual tests on a physical device requested for each step.

## Step 1

### After the first implementation (test alarm)
- [x] Grant notifications from the home screen and check that all permissions show OK.
- [x] Schedule "Next minute", lock the phone with the screen off → the screen turns on, the alarm appears over the lock screen and the tone rings. The Stop button stops it.
- [x] Same with the phone unlocked and another app open → heads-up notification with the tone ringing.
- [x] With silent mode / Do Not Disturb → it should still ring (`USAGE_ALARM`).
- [x] Schedule +5 min, reboot, unlock → it rings at its time.
- [x] Settings > Battery > Background usage limits: test with the app in "Sleeping apps" and in "Never sleeping apps".
- [x] Change the time zone with an alarm scheduled → it is rescheduled to the correct local time.

### After the battery button fix
- [x] Tap Grant on "Unrestricted battery" → Battery → Unrestricted, go back: the row should show OK.

### After the pop-up and "can't leave" fixes
- [x] Grant "Display over other apps" and the battery setting: both rows should show OK.
- [x] Schedule "Next minute" and keep using the phone in another app: the alarm screen should pop up by itself, not just the notification.
- [x] While ringing:
  - Back: nothing happens.
  - Home: the alarm screen comes back (it may take up to ~5 s).
  - Recents, then another app: it comes back.
- [x] Stop button: the alarm stops and the screen stays closed.
- [x] Phone locked with the screen off: still works.
- [x] Power button while ringing: the screen turns back on with the alarm (expected).
- [x] Turn "Display over other apps" off and repeat the pop-up test above ("Next minute" while using another app): you should get the old notification at the top.

### After the per-second reopen fix
- [x] Press Recents many times in a row, and also Recents → Home → Recents: the alarm must always come back, within about 5 s at most.
- [x] From Recents, open another app: the alarm comes back.
- [x] Open the notification panel and tap another app's notification: the alarm comes back.
- [x] Repeat what already worked (screen off, Home, another app, Back) to confirm nothing broke.
- [x] Press Stop: the alarm stops and the screen doesn't reopen.

### After the volume, reboot and killed-service measures (with silent test mode on)
- [x] Turn on "Silent test mode (debug)" on the home screen; the alarm screen shows "Alarm volume: x/y".
- [x] When it starts ringing, the alarm volume goes to max.
- [x] While ringing, press volume down, use the panel slider, and press volume with the screen off: the shown alarm volume goes back to max within about 1 s.
- [x] After Stop, the alarm volume goes back to the previous level.
- [x] Reboot the phone while it rings, then unlock: it rings again a few seconds later.
- [x] Stop the alarm, then reboot: it doesn't ring again.
- [x] Kill the app process while ringing (`adb shell am crash com.targetchain.targetchain`): it rings again within about 60 s.
- [x] Repeat the earlier tests (Back, Home, Recents, screen off, Stop) to check nothing broke.
- [x] Once, outside the library, with silent test mode off: the tone plays through the speaker at max volume.

## Step 2

### Alarm list and editor
- [x] Open the app: the alarm list appears. The permissions card is shown only while some permission is missing. If an old "test" alarm appears, delete it.
- [x] Tap +: "New alarm" editor with 07:00 and "Default alarm tone". Set a name, pick a time, Save: the alarm appears in the list with its switch on.
- [x] Tap the alarm: the editor shows its name, time and tone. Change them and Save: the list shows the changes (ordered by time).
- [x] Open an alarm, change something and press Back (or the arrow): nothing is saved.
- [x] Tap Save or + several times quickly: only one alarm is created and the app doesn't end on an empty screen.
- [x] Delete an alarm: a confirmation is shown; Cancel keeps it, Delete removes it.

### Ringtone picker
- [x] In the editor, tap Ringtone: the list shows "Default alarm tone" and Samsung's alarm tones, with the current one selected.
- [x] Tap a tone: it plays and gets selected. Tap another: the first one stops and the new one plays.
- [x] Press Back while a tone plays: it stops and the editor keeps the old tone. Repeat with OK: it stops and the editor shows the new tone.
- [x] While a tone plays, press Home: it stops.
- [x] With silent test mode on, the preview makes no sound. With it off, it plays at the current alarm volume.

### Scheduling (with silent test mode on)
- [x] Create an alarm with "Next minute (debug)" and Save: it rings at that minute, with everything from step 1 working (full screen, Back/Home/Recents, volume). Press Stop: its switch turns off.
- [x] Turn its switch off before it rings: it doesn't ring. Turn it on again: it rings at its time.
- [x] Delete an enabled alarm before it rings: it doesn't ring.
- [x] Edit an enabled alarm to a new time: it rings at the new time, not the old one.
- [x] Two alarms at different minutes: both ring, each at its time.
- [x] Choose a tone that isn't the default, then once outside the library with silent test mode off: the alarm rings with that tone.
- [x] Reboot with an alarm enabled, then unlock: it still rings at its time.
- [x] While an alarm rings, if you manage to reach the list (e.g. with "Display over other apps" off, from the notification): turning its switch off, editing it or deleting it doesn't stop it; only Stop does.

## Step 3

The database version changed: the alarms created in step 2 are deleted when the new version is installed.

### Challenge configuration
- [x] Tap +: the editor shows "Challenges: None", a message asking for at least one challenge, and Save disabled.
- [x] Tap Challenges: the list is empty with only "Add challenge". Tap it: only the Password button is shown (no QR or NFC).
- [x] Tap Password: with no saved passwords, a message explains it and Add is disabled. Tap "New password".
- [x] New password: the explanation about writing it down outside the phone is shown. Try an empty name, an empty password and two different passwords: each shows its error. Paste a password from the clipboard: it's accepted, and the password is visible while typing.
- [x] Create it: you're back on the Password screen with it selected. Tap Add: back on the challenge list with "1. Password" and its name.
- [x] Add a second password challenge with another new password. Move them with the arrows: the order changes. Delete one: it disappears.
- [x] Back to the editor: "Challenges: 1 challenge". Save: the alarm appears in the list.
- [x] Open the alarm, add or delete challenges, then press Back in the editor: reopening it shows the old challenges.
- [x] Create a password with the same name as an existing one but different case (e.g. "desk" for "Desk"): refused. Rename a password (pencil): empty or repeated names are refused; a valid name is shown in the list and in the challenge list.
- [x] Try to delete a password used by a saved alarm: a dialog lists that alarm and nothing is deleted. Add a challenge with a new unused password and, before saving the alarm, try to delete it: refused too. Delete an unused password: it's removed.
- [x] While typing and creating passwords: Samsung Keyboard shows no suggestions, and afterwards it doesn't suggest the password in other apps (e.g. a notes app). No "save password?" prompt from Samsung Pass or Google appears.

### Challenges while ringing (with silent test mode on)
- [x] Alarm with two password challenges ("Next minute (debug)"): it rings and shows "Challenge 1 of 2" with the first password's name.
- [x] Type a wrong password and Check: "Wrong password" message, empty field, the alarm keeps ringing.
- [x] Type the right one (with spaces before or after: still accepted; with different case: refused): "Challenge 2 of 2" appears. Complete it: the alarm stops and its switch turns off.
- [x] Try to paste into the field: long press shows no Paste menu; pasting from the Samsung keyboard clipboard, choosing a suggestion or using voice typing doesn't insert anything. Typing letter by letter works.
- [x] With the keyboard open, repeat the step 1 checks: Back (first closes the keyboard, then does nothing), Home, Recents, volume down, screen off. The screen comes back on the same challenge.
- [x] With the phone locked when it rings: the keyboard works over the lock screen.
- [x] Complete the first challenge, then kill the app (`adb shell am crash com.targetchain.targetchain`): it rings again within about 60 s and starts again at "Challenge 1 of 2". Repeat with a reboot (unlock after rebooting).
- [x] Debug Stop button: still shown in debug builds and stops the alarm.
- [x] Samsung pop-up view or split screen: try to open another app over the alarm screen while it rings, and tell me what happens.

### Running routine locked
- [x] While an alarm rings, reach the list (e.g. with "Display over other apps" off, from the notification): it shows "Running", the switch and delete are disabled, and tapping it shows a message instead of opening the editor.
- [x] With "Display over other apps" off, open an alarm in the editor and let it ring (set it to the next minute, save, open it again before it rings): once it rings, the editor says it's running and Save is disabled.
- [x] While it rings, its password can't be deleted (it's in use). After completing the challenges, everything can be edited again.

### After making the alarm screen non-resizable (with silent test mode on)
For each case, set it up before the alarm rings ("Next minute (debug)") and tell me what happens:
- [x] TargetChain in a pop-up window, another app behind: the alarm should appear full screen, not inside the pop-up.
- [x] Split screen with TargetChain and another app: split screen should close and the alarm appear full screen.
- [x] Split screen with two other apps: tell me whether split screen closes and the alarm appears full screen.
- [x] Another app in a pop-up window, TargetChain behind: tell me whether the pop-up stays usable (expected: yes, known limitation).
- [x] While ringing, try to put the alarm screen in a pop-up window or split screen (from Recents or with the gesture): it isn't allowed.
- [x] Repeat the step 1 checks (Back, Home, Recents, screen off, volume down) and complete a challenge: nothing broke.

### After making the main screen non-resizable
- [x] Try to open TargetChain in a pop-up window or split screen (from Recents or with the gesture): it isn't allowed, or it opens full screen.
- [x] With silent test mode on, let an alarm ring ("Next minute (debug)") while using another app: the alarm appears full screen as before, and completing its challenges stops it.
- [x] Open the app normally: the list, the editor and the challenge screens work as before.

## Step 4

The database version changed: the alarms **and passwords** created in step 3 are deleted when the new version is installed. Silent test mode on for all of these unless a test says otherwise.

### Routine and task editors
- [x] The list is titled "Routines". Tap +: the routine editor shows name, start time, one task ("1. Task 1", "No duration" and "No challenges" in red), "Add task", "Earliest end: set every task's duration", the message about challenges and durations, and Save disabled.
- [x] Duration fields: 13 hours, 60 minutes, 60 seconds or all zeros show the "Between 1 second and 12 hours" error and the task can't be saved; 12 : 0 : 0 is accepted; letters can't be typed. "3 s (debug)" sets 0 : 0 : 3.
- [x] Add two more tasks ("Add task" opens each new one). Each one starts with the default tone, no challenges and no duration, and its settings are independent of the others.
- [x] Move tasks with the arrows and delete one: the order and numbers change. With a single task left, its delete button is disabled.
- [x] "Earliest end" = start time + the sum of the durations; with a start time of 23:00 and durations adding up to more than 1 hour it shows "(+1 d)". "All to 3 s (debug)" sets every task to 3 s.
- [x] Save is only enabled when every task has a challenge and a duration. Save: the list row shows the time, the name and "3 tasks", with the switch on.
- [x] Open the routine, change a task (name, tone, challenges, duration) and the routine name, then press Back in the routine editor: reopening it shows nothing changed. Back from the task editor keeps the changes until you leave the routine editor.

### Chain on time (silent test mode on, short durations such as 20 s)
- [x] Routine with 3 tasks, "Next minute (debug)", save, lock the phone. Task 1 rings: the alarm screen shows "Task 1 of 3", the task name, the routine name and "Duration: …".
- [x] Complete its challenges: the alarm screen closes, the tone stops and the alarm volume goes back to what it was. A silent notification shows "Task 1 of 3 · name", the time left counting down, "Next: <task 2>" and a "Next task" button. No pop-up, the screen isn't reopened and the volume isn't forced: use other apps normally.
- [x] The list row shows "Task 1 of 3 · 00:15 left" counting down, with the switch and delete disabled; tapping it shows the message instead of opening the editor.
- [x] When the countdown ends, task 2 rings with its own tone and challenges (all the step 1 and 3 anti-evasion checks still apply: Back, Home, Recents, volume down, screen off).
- [x] Take a long time on task 2's challenge (e.g. 1 minute) with a 20 s duration: after completing it, the countdown starts at 20 s (the challenge time doesn't count).
- [x] During the last task's countdown the notification says "Last task" and the button is "End routine". When it ends: the notification disappears with no sound, the switch is off, and the routine can be edited again.

### Next task / End routine (silent test mode on, long durations such as 10 min)
- [x] During task 1's countdown, tap "Next task" in the list row: a dialog asks for confirmation. Cancel: nothing changes. Confirm: task 2 rings now with its challenges.
- [x] During task 2's countdown, tap "Next task" in the notification: the app opens with the same dialog; confirm: task 3 rings. Also try it with the phone locked (it asks to unlock first) and with the app closed.
- [x] During the last countdown, "End routine" (list or notification) with confirmation ends the routine: notification gone, switch off, editable again.
- [x] Open the dialog from the notification, then wait until the next task rings by itself: after completing it, the old dialog isn't shown any more.

### Reliability during the countdown (silent test mode on)
- [x] During a countdown of about 2 minutes, lock the phone and leave it with the screen off: the next task rings on time.
- [x] During a countdown, kill the app (`adb shell am crash com.targetchain.targetchain`): the next task still rings on time. Tell me whether the notification comes back (within about a minute) or stays gone until the next task.
- [x] During a countdown of about 5 minutes, reboot and unlock: the notification comes back with the right time left, and the next task rings on time.
- [x] During a countdown of about 1 minute, power off the phone for about 2 minutes, then turn it on and unlock: the next task rings within a few seconds. Same during the last task's countdown: the routine just ends (no sound), the switch is off.
- [x] During a countdown of about 10 minutes, turn off automatic date and time and move the clock 1 hour forward: the time left (notification and list) stays the same, and the next task rings when it should (not right away). Then move it 1 hour back: same. Then change the time zone: same. Turn automatic date and time back on.

### One routine at a time and locks (silent test mode on)
- [x] During routine A's countdown, let routine B's start time arrive (or tap B's "Start now (debug)"): B doesn't ring, its switch turns off and a notification says it didn't run because another routine was running. A continues normally.
- [x] Same while routine A's alarm is ringing, and during A's last task's countdown: B is skipped in both cases.
- [x] During a countdown, the running routine can't be edited, disabled or deleted (list), and a password it uses can't be deleted (Password screen of another routine). Other routines can be edited.

### Debug shortcuts
- [x] "Start now (debug)" in the list makes the routine's first task ring immediately.
- [x] "Stop (debug)" on the alarm screen starts that task's countdown instead of ending the routine.
- [x] "End routine (debug)" in the list row and in the notification during any countdown ends the routine at once, without confirmation.

### Final check with sound (outside the library, silent test mode off)
- [x] A 2-task routine: each task rings with its tone at max volume, and the countdown between them is silent.
