# AI log

One line per prompt sent to the AI assistant. Slash commands (`/effort`, `/btw`, `/rename`) and plan approvals are not prompts and aren't listed.

## Step 1
1. Start development: plan the minimal end-to-end skeleton of step 1 (project setup with Compose/Room/MVVM, Room entities, one test alarm with `setAlarmClock` and a full-screen alarm screen, Android 12+ permissions, rescheduling after reboot, working on Samsung Android 13), showing the plan and package structure before writing code.
2. Explain what was done, file by file, in simple language: what each file is, why it's needed and the key Android/Kotlin concepts, ordered by the app's flow, plus a summary of how they connect and the permissions added.
3. Ask why `MainActivity.kt` shows errors.
4. Ask to read `MainActivity.kt` again (when rejecting the proposed plan).
5. Explain the step again, file by file and line by line, for a complete beginner, ordered by the app's flow, with a summary and the permission list.
6. Fix three issues: the app isn't in the list opened by "Unrestricted battery", the alarm shows a notification instead of popping up full screen, and the alarm screen can be left with Home.
7. Approve the plan for the pop-up and reopening (including the `SYSTEM_ALERT_WINDOW` permission and blocking Back).
8. Report device test results: everything works except Recents pressed several times (escapes), and the notification panel and Quick settings (the screen doesn't come back).
9. Approve the fix that reopens the alarm screen every second from the service.
10. Report that everything works and ask for the commit.
11. Ask to write the `decisions.md` entries, update `CLAUDE.md`, and amend the commit to include the files left out.
12. Ask whether `.idea/deviceManager.xml` is also IDE configuration.
13. Ask to exclude `.idea/deploymentTargetSelector.xml` and `.idea/deviceManager.xml` from the repository.
14. Update the seven notes in `docs/` with what happened in step 1 (documentation only).
15. Update the notes after the CLAUDE.md change (anti-evasion is built and tested with each step; old step 6 removed), add it as a plan deviation, and point out which step 1 parts should already have anti-evasion measures (without implementing them).
16. Ask whether the anti volume-down measures can be implemented and tested without making noise (working in a library), without changing code.
17. Redefine step 1 (only an alarm that rings at a set time, full screen, with its anti-evasion measures; architecture for settings etc. added when needed) and describe the silent testing discussion.
18. Ask not to change the code yet.
19. List what needs to be finished for step 1 (the resulting plan was approved and implemented).
20. Explain test 6 of the step 1 device checks (killed app) in more detail.
21. Report that `adb` isn't recognized in PowerShell.
22. Report that all new checks and all earlier step 1 checks passed; ask not to commit anything.

## Step 2
1. Start step 2 with a reduced scope (alarm list and editor with name, time and ringtone with preview; enable/disable schedules/cancels; each alarm stored as a one-link routine; Navigation Compose + MVVM/UDF; no challenges or chaining): update `CLAUDE.md`, `steps-functionality.md` and `decisions.md` first, then show the plan (screens, navigation, data model changes, source of the preinstalled tones) before writing code.
2. Finish whatever was left unfinished after an API error interrupted the session (the docs updates after the code).
3. Report that step 1 had been committed and pushed manually, and that all the step 2 device tests in `device-testing.md` pass.
4. Decide to commit steps 1 and 2 together in a single commit.
