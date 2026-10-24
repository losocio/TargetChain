# Device testing

Manual tests on a physical device requested for each step.

## Step 1

### After the first implementation (test alarm)
1. Grant notifications from the home screen and check that all permissions show OK.
2. Schedule "Next minute", lock the phone with the screen off → the screen turns on, the alarm appears over the lock screen and the tone rings. The Stop button stops it.
3. Same with the phone unlocked and another app open → heads-up notification with the tone ringing.
4. With silent mode / Do Not Disturb → it should still ring (`USAGE_ALARM`).
5. Schedule +5 min, reboot, unlock → it rings at its time.
6. Settings > Battery > Background usage limits: test with the app in "Sleeping apps" and in "Never sleeping apps".
7. Change the time zone with an alarm scheduled → it is rescheduled to the correct local time.

### After the battery button fix
1. Tap Grant on "Unrestricted battery" → Battery → Unrestricted, go back: the row should show OK.

### After the pop-up and "can't leave" fixes
1. Grant "Display over other apps" and the battery setting: both rows should show OK.
2. Schedule "Next minute" and keep using the phone in another app: the alarm screen should pop up by itself, not just the notification.
3. While ringing:
   - Back: nothing happens.
   - Home: the alarm screen comes back (it may take up to ~5 s).
   - Recents, then another app: it comes back.
4. Stop button: the alarm stops and the screen stays closed.
5. Phone locked with the screen off: still works.
6. Power button while ringing: the screen turns back on with the alarm (expected).
7. Turn "Display over other apps" off and repeat step 2: you should get the old notification at the top.

### After the per-second reopen fix
1. Press Recents many times in a row, and also Recents → Home → Recents: the alarm must always come back, within about 5 s at most.
2. From Recents, open another app: the alarm comes back.
3. Open the notification panel and tap another app's notification: the alarm comes back.
4. Repeat what already worked (screen off, Home, another app, Back) to confirm nothing broke.
5. Press Stop: the alarm stops and the screen doesn't reopen.

### After the volume, reboot and killed-service measures (with silent test mode on)
1. Turn on "Silent test mode (debug)" on the home screen; the alarm screen shows "Alarm volume: x/y".
2. When it starts ringing, the alarm volume goes to max.
3. While ringing, press volume down, use the panel slider, and press volume with the screen off: the shown alarm volume goes back to max within about 1 s.
4. After Stop, the alarm volume goes back to the previous level.
5. Reboot the phone while it rings, then unlock: it rings again a few seconds later.
6. Stop the alarm, then reboot: it doesn't ring again.
7. Kill the app process while ringing (`adb shell am crash com.targetchain.targetchain`): it rings again within about 60 s.
8. Repeat the earlier tests (Back, Home, Recents, screen off, Stop) to check nothing broke.
9. Once, outside the library, with silent test mode off: the tone plays through the speaker at max volume.

## Step 2

### Alarm list and editor
1. Open the app: the alarm list appears. The permissions card is shown only while some permission is missing. If an old "test" alarm appears, delete it.
2. Tap +: "New alarm" editor with 07:00 and "Default alarm tone". Set a name, pick a time, Save: the alarm appears in the list with its switch on.
3. Tap the alarm: the editor shows its name, time and tone. Change them and Save: the list shows the changes (ordered by time).
4. Open an alarm, change something and press Back (or the arrow): nothing is saved.
5. Tap Save or + several times quickly: only one alarm is created and the app doesn't end on an empty screen.
6. Delete an alarm: a confirmation is shown; Cancel keeps it, Delete removes it.

### Ringtone picker
1. In the editor, tap Ringtone: the list shows "Default alarm tone" and Samsung's alarm tones, with the current one selected.
2. Tap a tone: it plays and gets selected. Tap another: the first one stops and the new one plays.
3. Press Back while a tone plays: it stops and the editor keeps the old tone. Repeat with OK: it stops and the editor shows the new tone.
4. While a tone plays, press Home: it stops.
5. With silent test mode on, the preview makes no sound. With it off, it plays at the current alarm volume.

### Scheduling (with silent test mode on)
1. Create an alarm with "Next minute (debug)" and Save: it rings at that minute, with everything from step 1 working (full screen, Back/Home/Recents, volume). Press Stop: its switch turns off.
2. Turn its switch off before it rings: it doesn't ring. Turn it on again: it rings at its time.
3. Delete an enabled alarm before it rings: it doesn't ring.
4. Edit an enabled alarm to a new time: it rings at the new time, not the old one.
5. Two alarms at different minutes: both ring, each at its time.
6. Choose a tone that isn't the default, then once outside the library with silent test mode off: the alarm rings with that tone.
7. Reboot with an alarm enabled, then unlock: it still rings at its time.
8. While an alarm rings, if you manage to reach the list (e.g. with "Display over other apps" off, from the notification): turning its switch off, editing it or deleting it doesn't stop it; only Stop does.
