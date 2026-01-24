# Testing Guide: Running on a Physical Device

This guide explains how to compile the Typing Toucan app and install it on your Android device for testing.

## 1. Prepare Your Android Device
Before you can install the app from your computer, you must enable "Developer Options" on your phone:

1.  Open **Settings** > **About Phone**.
2.  Tap **Build Number** seven times until you see the message "You are now a developer!"
3.  Go back to **Settings** > **System** > **Developer Options**.
4.  Enable **USB Debugging**.

## 2. Connect to Your Computer
1.  Connect your device to your Linux machine via a USB cable.
2.  A prompt may appear on your phone asking to "Allow USB debugging?". Check **Always allow from this computer** and tap **Allow**.
3.  Verify the connection by running this in your terminal:
    ```bash
    adb devices
    ```
    *If you don't have `adb` installed, run `sudo apt install adb` (Ubuntu/Debian).*

## 3. Build and Install Automatically
The easiest way to test is to let Gradle handle the installation. Ensure your device is connected and the screen is unlocked.

Run this command from the project root:
```bash
./gradlew :android:installDebug
```

Gradle will compile the code, generate a **Debug APK**, and push it directly to your device. The app should launch automatically once finished.

## 4. Manual Installation (The APK Method)
If you prefer to generate the file and install it manually:

1.  **Generate the APK**:
    ```bash
    ./gradlew :android:assembleDebug
    ```
2.  **Locate the file**: 
    The APK will be at `android/build/outputs/apk/debug/android-debug.apk`.
3.  **Install via ADB**:
    ```bash
    adb install android/build/outputs/apk/debug/android-debug.apk
    ```
    *Or simply transfer the file to your phone (via USB, Drive, etc.) and open it with a File Manager.*

## 5. Troubleshooting
- **SDK Not Found**: Ensure `ANDROID_HOME` is set or `local.properties` has the correct `sdk.dir`.
- **Permission Denied**: On some Linux distros, you might need to configure `udev` rules for your specific phone manufacturer if `adb devices` shows `?????? no permissions`.
- **Keyboard Input**: Since Typing Toucan is a typing game, you can test with the on-screen keyboard, but it is highly recommended to plug in a physical USB keyboard via an **OTG Adapter** for the best experience.
