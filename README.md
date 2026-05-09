# BlackOverlayAndroid

BlackOverlayAndroid is a small native Android app for personal sideloading on a Samsung Galaxy S24+ running Android 16. It shows a fullscreen black overlay over other apps, consumes touches, and removes the overlay only after a configured tap sequence followed by Android biometric or device-credential authentication.

This is a visual overlay, not a real lock screen. It does not block Home, Recents, the notification shade, the power menu, system navigation, or sensitive system screens.

## Start Here

- New setup, Android Studio debugging, and physical-device install steps: [docs/SETUP_AND_DEBUGGING.md](docs/SETUP_AND_DEBUGGING.md)
- Project architecture, file map, and implementation details: [docs/TECHNICAL_DOCUMENTATION.md](docs/TECHNICAL_DOCUMENTATION.md)

## What The App Does

1. `MainActivity` asks for overlay permission, configures unlock tap count, and starts/stops the overlay service.
2. `BlackOverlayService` runs as a foreground service and adds a black `TYPE_APPLICATION_OVERLAY` window.
3. Repeated taps anywhere on the black screen open `UnlockActivity`; the required tap count is configurable from 3 to 7.
4. `UnlockActivity` shows AndroidX `BiometricPrompt` with biometric or device credential unlock.
5. Successful authentication stops the service and removes the overlay.
6. An optional Quick Settings tile can start the overlay from the pull-down top menu. It does not stop or unlock an active overlay.

## Safety

This app should not break or permanently lock a physical device. It uses normal Android app APIs and does not request Device Owner, AccessibilityService, root, Lock Task Mode, or system-app privileges.

Important limitations:

- The overlay is removable by Android system behavior, app uninstall, force stop, successful biometric/device-credential authentication, or the app's manual emergency/development stop button.
- Samsung/Android may hide third-party overlays on sensitive system screens.
- This MVP does not change global brightness and does not request `WRITE_SETTINGS`.

## Architecture

The app uses native Android Views with a small MVVM split for settings:

- `MainActivity` binds the UI and handles Android permission/intent flows.
- `MainViewModel` exposes settings state to the main screen.
- `OverlaySettingsRepository` owns local persistence.
- `OverlaySettings` centralizes settings keys and allowed unlock tap-count constants.

## Quick Build

```bash
./gradlew assembleDebug
```

On Windows PowerShell with the wrapper:

```powershell
.\gradlew.bat assembleDebug
```

On Windows PowerShell, if the Gradle wrapper is unavailable and Gradle is installed:

```powershell
gradle assembleDebug
```

Install the debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
