<!-- This file is the project's CLAUDE.md (the root CLAUDE.md only imports it). Add or change project instructions here, never in the root CLAUDE.md. -->
# TargetChain

Android alarm app where each alarm can only be dismissed by completing a challenge, and alarms are **chained** to guide the user through a daily routine. Each alarm marks the start of an activity; once its challenge is completed, a countdown starts for the time assigned to that activity, then the next alarm rings. The chain can't be disabled until the routine ends.

Context: university project (Proyectos IV, deadline **29 Nov 2026**) that continues as a thesis (TFG). Priority: a working, reliable app. Low-risk scope, no over-engineering.

Intermediate deadline: **25 Oct 2026**. I must submit whatever progress I have on the code by then. There are no specific targets; the goal is to show the professor my progress and which changes to the scope I'll need to make. Keep the project in a compiling state, and tell me about anything that proves harder than expected, since it may be a candidate for a scope change.

## Current status
Current step: 3 (challenge system and password challenge), completed on 2026-11-24. Step 4 goals pending. Update this line when a roadmap step is completed.

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

## Tech stack
- Kotlin only (logic and UI). Gradle with Kotlin DSL.
- minSdk 31 (Android 12), Android only.
- UI: Jetpack Compose + Material 3, Navigation Compose.
- Architecture: MVVM with UDF (unidirectional data flow). The screen sends events to the ViewModel, the ViewModel exposes a single immutable UI state (StateFlow) that the screen observes. Layers: Compose screen -> ViewModel -> Repository -> Room. Keep it light, no extra layers or DI framework unless really needed.
- Room for alarms/chains, DataStore for settings, Kotlin Coroutines/Flow for async.
- CameraX + ML Kit (offline barcode/QR scanning), Media3 ExoPlayer for ringtones.

## Features
Do NOT implement anything from Phase 2 unless I ask. Within Phase 1, follow the priority order.

This list is not fixed: features may be dropped, postponed or modified, and new ones may be added as the project evolves (e.g. if something turns out too complex or from real-world use). Only implement what I explicitly ask for, and assume the list may be out of date if I say otherwise.

### Phase 1: Proyectos IV, implement first
1. **Per-alarm config**: number, type and order of challenges; ringtone from a preinstalled list.
2. **Challenges**: hidden password, QR/barcode scan, NFC tag. First get one working alarm dismissed by a challenge.
3. **Alarm chaining** (core feature, must be complete). While a routine is running, the chain and its alarms can't be disabled or edited until it ends. The phone is usable during the countdown; alarm-screen restrictions apply only while an alarm rings.
4. **Anti-evasion**: block leaving the alarm screen, block volume down while ringing. Built and tested together with each feature, not left for the end.

### Phase 2: TFG (later)
Everything not listed in Phase 1: extra alarm options (custom tones and random-order playlists, snooze, TTS, volume ramp, random challenge order), timer start, temporary mute and emergency exit, more challenges (steps, heart rate, AI recognition, pose photo; none doable lying down), advanced anti-evasion (post-check that the user is still awake, blocking power-off/uninstall/force-stop, blocking other apps in pop-up or split-screen windows over the alarm screen, money deposit), guided setup.

## Roadmap (iterate, one step at a time)
Anti-evasion is not a separate step: each step includes the measures that apply to what it builds, implemented and tested on the device together with the feature (e.g. a new challenge screen can't be left or bypassed).
1. Alarm that rings reliably at a set time (screen off, after reboot) with a full-screen alarm screen and its anti-evasion measures.
2. Single-alarm screens: alarm list (create, edit, delete, enable/disable) and alarm editor (name, time, ringtone from a preinstalled list with preview). Each alarm is stored as a one-link routine; the chain editor comes with step 4.
3. Challenge configuration + hidden password challenge.
4. Alarm chaining.
5. QR/barcode challenge, NFC challenge.
6. Multi-device testing, bug fixing.

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
