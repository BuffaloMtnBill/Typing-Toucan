# Android Release Roadmap: Typing-Toucan

This document outlines the steps required to transition the current desktop JVM project into a production-ready Android application and generate a distributable APK/Bundle.

---

## Phase 1: Project Restructuring (Multi-Module)
The current project is a flat JVM structure. Android requires a multi-module Gradle setup to separate platform-specific dependencies.

**Objective**: Create a clean separation between shared game logic (`core`) and platform launchers (`desktop`, `android`).

1.  **Create Root Project Structure**:
    *   Create `settings.gradle.kts` to define the modules: `include(":core", ":desktop", ":android")`.
    *   Move the current project root `build.gradle.kts` logic into submodules.
    *   **Subtask**: Create a `gradle.properties` file to manage library versions centrally (e.g., `gdxVersion=1.13.0`, `androidPluginVersion=8.2.0`).

2.  **Define `:core` Module**:
    *   Create `core/build.gradle.kts` (apply `java-library` plugin).
    *   Move all shared code (`src/main/kotlin/com/typingtoucan/`) excluding Launchers to `core/src/main/kotlin`.
    *   Move `assets` to `android/assets` (Android standard) and link them in Desktop via Gradle sourceSets.
    *   **Subtask**: Verify `core` has NO dependencies on `gdx-backend-lwjgl3` or `gdx-backend-android`.

3.  **Define `:desktop` Module**:
    *   Create `desktop/build.gradle.kts` (apply `application` plugin).
    *   Move `DesktopLauncher.kt` to `desktop/src/main/kotlin`.
    *   Add dependency on `project(":core")` and `gdx-backend-lwjgl3`.

4.  **Define `:android` Module**:
    *   Create `android/build.gradle.kts` (apply `com.android.application` and `kotlin-android` plugins).
    *   Move `AndroidLauncher.kt` to `android/src/main/java`.
    *   Add dependency on `project(":core")` and `gdx-backend-android`.
    *   **Subtask**: Configure `natives` dependencies properly for arm64-v8a and armeabi-v7a.

---

## Phase 2: Android Manifest & Configuration
**Objective**: ensure the app launches correctly on Android devices with appropriate permissions and settings.

1.  **Configure `AndroidManifest.xml`**:
    *   Create at `android/src/main/AndroidManifest.xml`.
    *   Set `package="com.typingtoucan.android"`.
    *   Set `<uses-feature android:name="android.hardware.touchscreen" android:required="false" />` (since we rely on keyboard).
    *   **Subtask**: Add `<uses-permission android:name="android.permission.INTERNET" />` if analytics/ads are planned (likely not for this version).

2.  **Activity Configuration**:
    *   Set `android:screenOrientation="sensorLandscape"` to force landscape mode.
    *   Set `android:configChanges="keyboard|keyboardHidden|orientation|screenSize"`.
    *   **Subtask**: Ensure `android:theme="@style/GdxTheme"` is used to hide the status bar (Immersive Mode).

3.  **SDK Management**:
    *   In `android/build.gradle.kts`:
        *   `compileSdk = 34`
        *   `minSdk = 21` (Android 5.0 Lollipop)
        *   `targetSdk = 34` (Android 14)
    *   **Subtask**: Verify JDK 17 compatibility (required for AGP 8.0+).

4.  **Icons & Branding**:
    *   Use Android Studio's **Image Asset Studio** (or manual generation) to create:
        *   `mipmap-mdpi` (48x48)
        *   `mipmap-hdpi` (72x72)
        *   `mipmap-xhdpi` (96x96)
        *   `mipmap-xxhdpi` (144x144)
        *   `mipmap-xxxhdpi` (192x192)
    *   **Subtask**: Create `ic_launcher_round` (adaptive icons) if targeting modern Android.

---

## Phase 3: Platform Optimizations
**Objective**: Fix mobile-specific bugs and performance issues.

1.  **Asset Management**:
    *   **Subtask**: Check strict path case-sensitivity. Desktop (Windows) is forgiving; Android (Linux-based) is not. Ensure `Assets/` vs `assets/` usage is consistent.
    *   **Subtask**: Use `TexturePacker` to create atlases for smaller sprites. Large background images (`background_panoramic.png` ~13MB) might crash low-end devices. Considerations: Split into tiles or reduce resolution for mobile.

2.  **Input Handling (The Toucan's Specialty)**:
    *   **Subtask**: Test external keyboard handling via OTG/Bluetooth.
    *   **Subtask**: Verify `KEYCODE_BACK` maps to `Input.Keys.ESCAPE` (already implemented, needs testing on device).
    *   **Subtask**: Handle `KEYCODE_ENTER` vs `KEYCODE_NUMPAD_ENTER`.

3.  **Coroutines & Threading on Android**:
    *   Ensure no blocking file I/O occurs on the main UI thread (Android triggers ANR - App Not Responding).
    *   **Subtask**: Verify `SoundManager` loading is async or fast enough not to stutter the splash screen.

---

## Phase 4: Build & SDK/APK Generation
**Objective**: Build the artifact for distribution.

1.  **Code Shrinking (R8)**:
    *   Enable `isMinifyEnabled = true` in `build.gradle.kts` (release build type).
    *   **Subtask**: Create `proguard-rules.pro` to keep LibGDX reflection entry points:
        ```proguard
        -keep class com.badlogic.gdx.backends.android.** { *; }
        -keep class com.typingtoucan.** { *; }
        ```

2.  **Signing Configuration**:
    *   Generate a keystore: `keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias`.
    *   Configure `signingConfigs` in `android/build.gradle.kts` to read from strictly LOCAL `local.properties` (never commit keys to git).

3.  **Generate Artifacts**:
    *   **Internal Test**: Run `./gradlew :android:assembleDebug` -> `android/build/outputs/apk/debug/android-debug.apk`.
    *   **Production**: Run `./gradlew :android:bundleRelease` -> `android/build/outputs/bundle/release/android-release.aab`.

---

## Phase 5: Google Play Console (Post-Build)
**Objective**: Publish the game.

1.  **Internal Testing Track**:
    *   Create app entry in Google Play Console.
    *   Upload `android-release.aab`.
    *   Add tester emails.
    *   **Subtask**: Download logic on test device using the Play Store link to verify "Google Play Signing" didn't break anything.

2.  **Store Listing**:
    *   **Title**: Typing Toucan
    *   **Short Description**: "Master typing with a Toucan!" (Max 80 chars)
    *   **Feature Graphic**: 1024x500 PNG.
    *   **Screenshots**: Landscape screenshots required.

3.  **Compliance**:
    *   **Privacy Policy**: Even if you don't collect data, you need a policy URL saying "No data collected".
    *   **Data Safety Form**: Fill out the form in Play Console (mostly "No" for offline games).

---
**Next Immediate Step**: Begin Phase 1 (Project Restructuring). This requires moving files and potentially breaking the build temporarily until the new structure is set up.
