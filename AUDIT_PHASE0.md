# MJ Phase 0 Repository Audit

Date: 2026-11-12
Target repository: `devmurariparth/MJ-ci-ready`
Base branch: `main`
Audited base commit: `1665eb7326cada35e0ac05aa3f5db010a7d7c10e`
Current release: `v3.0.3` (`versionCode 8`, package `com.mj.agent`)

## Repository identity

Confirmed this is the requested CI-ready Android MJ repository. It contains the Android app, Gradle wrapper, Kotlin sources, unit tests, and GitHub Actions. The similarly named `pikachu` repository is not the target for this work.

## Baseline CI evidence

Latest observed Actions run: [43051467175](https://github.com/devmurariparth/MJ-ci-ready/actions/runs/43051467175), triggered by commit `1665eb7`, completed successfully on 2026-11-11. Its workflow steps show unit tests, lint, APK assembly, APK verification, artifact upload, and release publication completed. This proves CI success at that commit; it is not a claim of exhaustive real-device testing.

## Architecture inventory

- Android app: Kotlin + Jetpack Compose; minSdk 26, compileSdk 35, targetSdk 34.
- App entry: `MainActivity.kt`, `MJApp.kt`, `UI.kt`.
- Orchestration/state: `MJBrain.kt` (chat, AUTO/CHAT/AGENT modes, agent loop, model import).
- Routing: `Router.kt` multilingual fast-path intent parser.
- Agent: `AgentPrompt.kt` JSON action prompt and argument validation; `Act.kt` action/result types; `Executor.kt` action dispatch.
- Device actions: `Device.kt`, `Apps.kt`, `Contacts.kt`.
- Accessibility: `MJAccessibilityService.kt`, `Overlay.kt`.
- Voice/TTS: `Voice.kt`, `Speaker.kt`.
- Local LLM: `LlmEngine.kt` using MediaPipe Tasks GenAI; imports a user-provided compatible `.task` bundle.
- Tests: currently one test file, `LanguageAndRoutingTest.kt`, with language detection, prompt history, and classifier checks.
- CI: `.github/workflows/build-apk.yml` is the current release workflow; a second legacy `android-apk.yml` workflow duplicates the build trigger.

## Findings / gaps

1. **No Windows implementation in this repository.** The repository is Android-only. Windows needs a separate project (e.g. `MJ.Windows`) and shared protocol/contracts; don't pretend cross-platform code already exists.
2. **No Gemini/OpenAI provider in the inspected source.** The current local LLM engine is MediaPipe. Cloud provider integration is a future, optional module; secrets must not be committed.
3. **Model distribution is manual.** The app expects a compatible MediaPipe `model.task` imported by the user. A generic GGUF/Qwen/Ollama model cannot be renamed to `.task`; runtime/model format compatibility must be respected.
4. **Automated test coverage is narrow.** Existing tests mainly cover language detection and routing classification. Action argument validation, model JSON parsing, permission/policy decisions, cancellation/recovery, and device workflows need more tests.
5. **Two workflows trigger builds on main.** `android-apk.yml` is legacy and doesn't explicitly set Gradle 9.1.0, while current AGP is 9.0.1. Keep one authoritative workflow to avoid duplicate or incompatible runs.
6. **Permissions/setup UX can be refined.** `refreshSetup` currently treats microphone, contacts, and camera as one aggregate `permsOk`; Camera is only needed for flashlight, and Contacts only for contact lookup. Use feature-specific permission state.
7. **Accessibility automation is inherently conditional.** It requires the user to enable the service, compatible app accessibility nodes, and current screen state; not every app/UI can be automated reliably.
8. **No hotword service.** README explicitly states voice activation is tap-to-talk; always-listening “Hey MJ” is not implemented.
9. **Release is debug-signed.** The current APK is a test artifact, not a Play Store production-signed build; signing/update compatibility must be handled separately.
10. **No Windows/Android end-to-end device test evidence** was found in the repo/last workflow. CI success must not be conflated with real-device feature validation.

## Phase 0 hardening applied on audit branch

- Retain a reproducible audit report.
- Remove the duplicate legacy Android APK workflow; use `build-apk.yml` as the sole authoritative Android CI/release workflow.
- Configure the authoritative workflow for PR validation (unit tests + lint + APK assembly), while explicitly skipping release publication and wrapper pushes for PR merge refs.
- Validate supported Settings pages in the agent parser before execution (`wifi`, `bluetooth`, `display`, `main` only).
- Expand routing/parser regression tests for question-shaped commands, explicit torch state, invalid action arguments/settings, and braces inside JSON string values.
- Do not change `main` directly; changes are isolated in `phase0-audit-hardening`.

## Prior-upgrade completion checkpoint (Phase 0.1)

**Completed in this branch:** repository audit, duplicate workflow cleanup, PR-safe CI workflow configuration, parser guard for Settings pages, and expanded regression tests.

**Still pending before Phase 1:** fresh CI execution for this branch (workflow changes live in the PR and require the PR to be merged or otherwise run by GitHub Actions); Android real-device validation; Windows companion implementation; Gemini/OpenAI provider adapter and secure key setup; and optional always-on hotword feasibility/implementation. These are not claimed as complete. Hotword requires an explicit foreground/background microphone design and Android restrictions; do not promise reliable lock-screen wake detection without device testing.

## Phase 1 priorities

1. Add pure unit tests for `AgentPrompt.parse/validArguments` and `Router.quick` safety/ambiguity cases.
2. Introduce typed action contracts and central policy/permission checks before execution.
3. Add per-action result verification semantics (started vs completed vs externally confirmed).
4. Improve feature-specific permission status and accessibility stale-node safeguards.
5. Add model capability/format/size checks and memory/latency diagnostics.
6. Establish a Windows companion project and shared command/result schema only after Android contracts are stable.

## Acceptance gates

- `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` passes in CI.
- Every agent action has validated arguments and an explicit policy decision.
- Failure/cancellation is propagated; no false “success” claims.
- Sensitive/external actions remain user-confirmed.
- Android and Windows status are reported separately with actual test evidence.
