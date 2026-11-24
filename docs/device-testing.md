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
