# TargetChain

Android alarm app where each alarm can only be dismissed by completing a challenge, and alarms are **chained** to guide the user through a daily routine. Each alarm marks the start of an activity; once its challenge is completed, a countdown starts for the time assigned to that activity, then the next alarm rings. The chain can't be disabled until the routine ends.

Context: university project (Proyectos IV, deadline **29 Nov 2026**) that continues as a thesis (TFG). Priority: a working, reliable app. Low-risk scope, no over-engineering.

Intermediate deadline: **25 Oct 2026**. I must submit whatever progress I have on the code by then. There are no specific targets; the goal is to show the professor my progress and which changes to the scope I'll need to make. Keep the project in a compiling state, and tell me about anything that proves harder than expected, since it may be a candidate for a scope change.

## Current status
Current step: 2 (alarm list and alarm editor, single alarms only), completed on 2026-10-24. Step 3 goals pending. Update this line when a roadmap step is completed.

Goal of this step: create, edit, delete and enable/disable single alarms. Reduced scope compared to the original roadmap: no routine screens and no placeholders; each screen is added when its logic exists.
- Alarm list screen: see all alarms, create a new one, open one to edit it, delete it, and enable/disable it with a switch.
- Alarm editor screen: name, time and ringtone.
- Ringtone selection from a preinstalled list, with preview (it plays when chosen and stops when leaving).
- Enabling an alarm schedules it with the step 1 system; disabling it cancels it.
- Internally, each alarm is stored as a one-link routine, reusing the existing Room model, so step 4 (chaining) only has to extend the editor to add more alarms.
- Navigation Compose and MVVM with UDF, as described in this file.
- Out of scope: challenge configuration (step 3) and anything about chaining (step 4).
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
Everything not listed in Phase 1: extra alarm options (custom tones and random-order playlists, snooze, TTS, volume ramp, random challenge order), timer start, temporary mute and emergency exit, more challenges (steps, heart rate, AI recognition, pose photo; none doable lying down), advanced anti-evasion (post-check that the user is still awake, blocking power-off/uninstall/force-stop, money deposit), guided setup.

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
- `device-testing.md`: only the manual tests you ask me to run on a device for each step, grouped by step. Don't add anything else.

## Project notes
- Test device: Samsung phone running Android 13. Keep in mind Samsung's battery optimization and background restrictions when touching alarm code.
