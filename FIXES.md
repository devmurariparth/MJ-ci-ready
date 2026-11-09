# MJ project repair notes

## Build configuration repaired for the Java runtime selected in Android Studio

- Updated the Gradle wrapper from 8.9 to 9.1.0. Gradle 9.1.0 adds support for running Gradle on Java 25.
- Updated Android Gradle Plugin from 8.7.2 to 9.0.1, which uses Gradle 9.1.0 as its default/minimum version.
- Migrated the app to AGP 9 built-in Kotlin and removed the now-unnecessary `org.jetbrains.kotlin.android` plugin.
- Updated the Compose compiler plugin to 2.2.10, matching AGP 9's built-in Kotlin version.
- Removed the old `kotlinOptions` DSL; Java compile compatibility remains set to Java 17, which AGP 9 uses as the default Kotlin JVM target for the app.
- Removed `gradle/gradle-daemon-jvm.properties`, which forced JDK 21 and caused the latest “Cannot find a Java installation ... Java 21” failure. Gradle now uses the JVM selected in Android Studio (Java 25 is supported by Gradle 9.1.0).

## App-level changes already present in this project

- Added Camera permission and optional flash-hardware declaration for flashlight control.
- Moved Accessibility service operations back to Android's main thread.
- Improved contact matching so ambiguous partial-name matches are not silently selected.
- Added safer speech-recognizer and text-to-speech error handling.
- Improved model import staging, minimum-size validation, progress updates, and protection against concurrent model loading/importing.
- Added MediaPipe response cancellation handling and made the model path consistent with the importer.
- Improved floating-orb click handling and localized permission/import status text.

## Validation status

- Gradle/AGP/Kotlin/Compose version alignment was checked against the official Android and Gradle compatibility documentation.
- XML resources and ZIP structure were validated.
- A full Android build was not run in this environment because the Android SDK and Gradle distribution could not be downloaded here. The first full sync/build must be confirmed in the user's Android Studio.

## Verification pass (CI-ready)

- Added `.github/workflows/build-apk.yml` (repo root): JDK 17, Android SDK (platform 35, build-tools 36.0.0), Gradle 9.1.0 via `gradle/actions/setup-gradle` (no local wrapper needed), runs `gradle assembleDebug`, uploads `app/build/outputs/apk/debug/*.apk` as artifact `MJ-debug-APK`.
- `gradle.properties`: Gradle JVM heap raised from 2 GB to 4 GB (AGP 9 + Compose can run out of memory at 2 GB).
- `LlmEngine.kt`: `cancelGenerateResponseAsync` is now invoked via reflection so the build cannot fail if the MediaPipe version lacks that method (could not be verified offline).
- `MJBrain.kt`: a model timeout (`TimeoutCancellationException`) now shows the "could not finish" message instead of "Stopped".
- No secrets, signing keys or API keys exist in the project. The output is a DEBUG APK signed with the Android debug key, not a production/Play Store build.
- Local build was NOT run: this environment has no Android SDK and no access to Google Maven / Gradle downloads.
