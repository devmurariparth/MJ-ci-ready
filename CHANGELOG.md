# Changelog

## MJ 3.0.3 — 2026-11-11

- Added regression tests for Gujarati, Hindi, English and Roman-script language detection, chat prompt roles, and multilingual task classification; CI now runs unit tests, Android lint and APK assembly.
- CHAT mode no longer runs local device-action shortcuts.
- Invalid or incomplete AI actions receive clearer retry feedback; music and flashlight actions require valid arguments.
- Accessibility text taps now resolve duplicate text nodes to a single clickable target and refuse ambiguous matches.
- Device action exceptions are returned to the agent as recoverable failures, while coroutine cancellation is preserved.
- Model loading now preserves and restores backups after interrupted imports; a backup is deleted only after a replacement model loads successfully.
- Timer parsing uses bounded Long arithmetic to avoid overflow from unusually large spoken durations.
- Kept the floating MJ orb within screen bounds and hardened text-to-speech initialization.
- Preserved text drafts when the user tries to submit while MJ is already busy.
- Bumped versionName to 3.0.3 and versionCode to 8.

## MJ 3.0.2 — 2026-11-10

- WhatsApp messages now open as prefilled drafts. MJ does not tap Send automatically; the user reviews and sends the message.
- Model import now stages the selected file and preserves the existing model as a backup. If the new model cannot be loaded, the previous model file is restored.
- The GitHub Actions build generates and commits Gradle 9.1.0 wrapper files, then validates the wrapper before lint and APK assembly.
- Version bumped to versionName 3.0.2 and versionCode 7.

## MJ 3.0.1

- Added local media volume and transport controls.
- Added Gujarati, Hindi and English fast-path routing for app, call, alarm, timer and media commands.
- Added the Liquid-style Compose interface and floating Accessibility orb.
