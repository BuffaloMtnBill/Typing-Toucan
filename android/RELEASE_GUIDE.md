# Release Guide: Typing Toucan

This guide outlines the steps to prepare and build a production release of the Typing Toucan Android app.

## 1. Generate a Signing Key
You need a keystore file to sign your release builds. Run the following command in your terminal:

```bash
keytool -genkey -v -keystore android/release.keystore -alias typingtoucan -keyalg RSA -keysize 2048 -validity 10000
```
Follow the prompts to set your passwords and organization details.

## 2. Configure Environment Variables
To keep your passwords secure, it is recommended to use environment variables rather than hardcoding them in `build.gradle.kts`.

Set the following variables in your shell (e.g., in `.bashrc` or `.zshrc`):
```bash
export RELEASE_STORE_PASSWORD=your_keystore_password
export RELEASE_KEY_ALIAS=typingtoucan
export RELEASE_KEY_PASSWORD=your_key_password
```

## 3. Build the Release Bundle (AAB)
Google Play requires the Android App Bundle format (.aab).

```bash
./gradlew :android:bundleRelease
```
The output will be found at `android/build/outputs/bundle/release/android-release.aab`.

## 4. Testing the Release Build
Before publishing, you can generate a release APK from the bundle or build it directly:

```bash
./gradlew :android:assembleRelease
```
The output will be at `android/build/outputs/apk/release/android-release-unsigned.apk` (unless signed).
