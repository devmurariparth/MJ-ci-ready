# MJ — Android AI assistant

MJ is an Android assistant project with Gujarati, Hindi and English chat/voice input, an optional on-device MediaPipe model, and phone actions through Android intents and the Accessibility service.

## Build setup (updated for Java 25)

This project now uses a toolchain combination that can run with the Java 25 Gradle JVM selected in your Android Studio:

- Gradle wrapper: **9.1.0** (supports running Gradle on Java 25)
- Android Gradle Plugin: **9.0.1** (requires Gradle 9.1.0 and JDK 17 or newer)
- Kotlin: **AGP 9 built-in Kotlin 2.2.10**
- Compose compiler plugin: **2.2.10**
- App Java bytecode target: **17**

The old `gradle/gradle-daemon-jvm.properties` file that forced JDK 21 has been removed. Gradle will use the JVM selected by Android Studio. Do not downgrade the Gradle wrapper to 8.9 for this project.

### Open and sync

1. Extract the ZIP.
2. In Android Studio, open the **inner `MJ` folder** (the folder containing `settings.gradle.kts`, not the outer ZIP extraction folder).
3. Keep the Android Studio Gradle JDK set to the installed JDK 25 that was already selected, or another JDK 17+ supported by Gradle 9.1.
4. Select **File → Sync Project with Gradle Files**.
5. After sync succeeds, select **Build → Make Project**.

Android Studio may offer to install missing Android SDK components (the project currently uses compileSdk 35). Allow the SDK installation if prompted.

## Setup on phone

1. Complete Gradle sync and build.
2. Connect an Android phone with USB debugging enabled and run the `app` configuration.
3. In MJ's Setup card, grant the requested microphone, contacts, phone and camera permissions.
4. Import a compatible MediaPipe `.task` LLM model. The file must be a valid MediaPipe LLM task bundle; renaming a regular model file to `.task` does not convert it.
5. Enable MJ's Accessibility service to use screen reading, taps, typing and the floating orb. Android may require **App info → ⋮ → Allow restricted settings** for sideloaded apps.

## Example commands

- `YouTube ખોલ` / `YouTube खोलो` / `open YouTube`
- `Rahul ne call kar` / `राहुल को कॉल करो`
- `WhatsApp par Rahul ne hu aavu chu moklo`
- `સવારે 6:45 નો અલાર્મ મૂક` / `5 मिनट का टाइमर लगाओ`
- `ટોર્ચ ચાલુ કર` / `torch off`
- `Play Believer on SimpMusic` / `SimpMusic ખોલ`
- `volume 60%` / `Awaj ochho kar` / `આવાજ વધારો`
- `next song` / `pause music` / `resume music`
- `set alarm for 6:45 AM` / `5 minutes timer`
- `cricket score google par shodho`

## Notes and limitations

- Voice activation is tap-to-talk; a background always-listening “Hey MJ” hotword service is not included in this project.
- Gujarati/Hindi speech recognition and text-to-speech availability depends on language packs and the Android speech/TTS service installed on the phone.
- WhatsApp opens the matched chat and attempts to press Send through Accessibility. When the Send button cannot be identified, MJ leaves the message ready for the user to send manually. Ambiguous contact matches are not guessed.
- A compatible on-device model is required for free-form chat and multi-step agent reasoning. App opening, calling, alarm, timer, flashlight, music search, volume and media-key fast-path commands can work without the model.
- SimpMusic is handled through the standard Android media-search intent when supported; otherwise MJ opens SimpMusic. This does not guarantee that a track starts automatically.
