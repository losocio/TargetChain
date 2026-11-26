<!-- This file is the project's CLAUDE.md (the root CLAUDE.md only imports it). Add or change project instructions here, never in the root CLAUDE.md. -->
# TargetChain

Android alarm app where each alarm can only be dismissed by completing a challenge, and alarms are **chained** to guide the user through a daily routine. Each alarm marks the start of a task; once its challenge is completed, a countdown starts for the time assigned to that task, then the next alarm rings. The chain can't be disabled until the routine ends.

Context: university project (Proyectos IV, deadline **29 Nov 2026**) that continues as a thesis (TFG). Priority: a working, reliable app. Low-risk scope, no over-engineering.

Intermediate deadline: **25 Oct 2026**. I must submit whatever progress I have on the code by then. There are no specific targets; the goal is to show the professor my progress and which changes to the scope I'll need to make. Keep the project in a compiling state, and tell me about anything that proves harder than expected, since it may be a candidate for a scope change.

## Current status
Current step: 4 (alarm chaining), completed on 2026-11-26. It was the last roadmap step. Update this line when a roadmap step is completed.

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

## Tech stack
- Kotlin only (logic and UI). Gradle with Kotlin DSL.
- minSdk 31 (Android 12), Android only.
- UI: Jetpack Compose + Material 3, Navigation Compose.
- Architecture: MVVM with UDF (unidirectional data flow). The screen sends events to the ViewModel, the ViewModel exposes a single immutable UI state (StateFlow) that the screen observes. Layers: Compose screen -> ViewModel -> Repository -> Room. Keep it light, no extra layers or DI framework unless really needed.
- Room for alarms/chains, DataStore for settings, Kotlin Coroutines/Flow for async.
- Media3 ExoPlayer for ringtones.
- Phase 2 only, not added yet: CameraX + ML Kit (offline barcode/QR scanning).

## Features
Do NOT implement anything from Phase 2 unless I ask. Within Phase 1, follow the priority order.

This list is not fixed: features may be dropped, postponed or modified, and new ones may be added as the project evolves (e.g. if something turns out too complex or from real-world use). Only implement what I explicitly ask for, and assume the list may be out of date if I say otherwise.

### Phase 1: Proyectos IV, implement first
1. **Per-alarm config**: number, type and order of challenges; ringtone from a preinstalled list.
2. **Challenges**: hidden password. First get one working alarm dismissed by a challenge.
3. **Alarm chaining** (core feature, must be complete). While a routine is running, the chain and its alarms can't be disabled or edited until it ends. The phone is usable during the countdown; alarm-screen restrictions apply only while an alarm rings.
4. **Anti-evasion**: block leaving the alarm screen, block volume down while ringing. Built and tested together with each feature, not left for the end.

### Phase 2: TFG (later)
Everything not listed in Phase 1: extra alarm options (repeating routines, locking a routine some time before it starts, custom tones and random-order playlists, snooze, TTS, volume ramp, random challenge order), timer start, temporary mute and emergency exit, more challenges (QR/barcode scan, NFC tag, steps, heart rate, AI recognition, pose photo; none doable lying down), advanced anti-evasion (post-check that the user is still awake, blocking power-off/uninstall/force-stop/clearing app data, blocking other apps in pop-up or split-screen windows over the alarm screen, money deposit), guided setup.

## Roadmap (iterate, one step at a time)
Anti-evasion is not a separate step: each step includes the measures that apply to what it builds, implemented and tested on the device together with the feature (e.g. a new challenge screen can't be left or bypassed).
1. Alarm that rings reliably at a set time (screen off, after reboot) with a full-screen alarm screen and its anti-evasion measures.
2. Single-alarm screens: alarm list (create, edit, delete, enable/disable) and alarm editor (name, time, ringtone from a preinstalled list with preview). Each alarm is stored as a one-link routine; the chain editor comes with step 4.
3. Challenge configuration + hidden password challenge.
4. Alarm chaining: routine list and editor with ordered tasks (each with its alarm, challenges and duration), countdown between tasks with a foreground service, one routine running at a time.

## Working rules
- Before implementing anything non-trivial, propose a short plan and wait for my OK.
- Small, focused changes. One feature or fix per task. Keep the project compiling after each change.
- Don't add dependencies, permissions or new modules without asking first.
- Don't implement features outside the current roadmap step.
- Don't explain code in detail unless I ask. Summarize what changed in 2-3 lines.
- Alarm reliability is the top technical risk: use exact alarms, full-screen intents and foreground services properly, and tell me about any permission or manufacturer-specific behavior I must test on real devices.
- Prefer simple, readable code over clever code. Idiomatic Kotlin, meaningful names.
- Write tests only for pure logic (e.g. chain scheduling), not for UI.

## Conventions
- Write all code, comments and commit messages in English. UI texts go in `strings.xml`, never hardcoded.
- A task is done only when the project compiles, tests pass, and you tell me what to check by hand on a device.
- Git: never commit or push; I'll do it manually. Suggest a separate branch for risky changes.

## Commands
- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew test`
- Install on device: `./gradlew installDebug`
- Many features (alarms, NFC, camera) can't be validated on an emulator; I test only on physical devices.

## Docs (used later for the thesis)
Keep these files in `docs/` up to date as part of finishing each task. No need to ask before editing them.
- `steps-functionality.md`: one entry per roadmap step, each with the same text as its "Current status" section at that time. Completed steps stay listed; when a new step starts, append it at the end.
- `corrections.md`: errors or incorrect behaviors detected and the corrections applied.
- `ai-log.md`: one line per prompt I send, briefly describing what I asked.
- `decisions.md`: important decisions and why, plus any deviations from the plan or scope and their reasons. Propose an entry when we make a significant decision. Read it before proposing alternatives: it also lists dead ends (things we tried or discarded and why). Don't suggest those again.
- `architecture.md`: package structure, data model and the lifecycle of an alarm (scheduled, rings, challenge, countdown, next alarm). Update it when the structure or flow changes. Read it before changing related code.
- `android-quirks.md`: permissions needed, odd behaviors of Android 12/13 and Samsung (battery optimization, background restrictions), and how each one was solved. Read it before touching alarm code.
- `device-testing.md`: only the manual tests you ask me to run on a device for each step, grouped by step. Don't add anything else. Each test starts with an empty checkbox (`- [ ]`), not a number; I tick it (`- [x]`) when it passes.

## Project notes
- Test device: Samsung phone running Android 13. Keep in mind Samsung's battery optimization and background restrictions when touching alarm code.
