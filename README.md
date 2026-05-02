# BlackOverlayAndroid

BlackOverlayAndroid is a small native Android app for personal sideloading on a Samsung Galaxy S24+ running Android 16. It shows a fullscreen black overlay over other apps, consumes touches, and removes the overlay only after Android biometric or device-credential authentication.

This is a visual overlay, not a real lock screen. It does not block Home, Recents, the notification shade, the power menu, system navigation, or sensitive system screens.

## Start Here

- New setup, Android Studio debugging, and physical-device install steps: [docs/SETUP_AND_DEBUGGING.md](docs/SETUP_AND_DEBUGGING.md)
- Project architecture, file map, and implementation details: [docs/TECHNICAL_DOCUMENTATION.md](docs/TECHNICAL_DOCUMENTATION.md)

## What The App Does

1. `MainActivity` asks for overlay permission and starts/stops the overlay service.
2. `BlackOverlayService` runs as a foreground service and adds a black `TYPE_APPLICATION_OVERLAY` window.
3. A long press on the black screen opens `UnlockActivity`.
4. `UnlockActivity` shows AndroidX `BiometricPrompt` with biometric or device credential unlock.
5. Successful authentication stops the service and removes the overlay.

## Safety

This app should not break or permanently lock a physical device. It uses normal Android app APIs and does not request Device Owner, AccessibilityService, root, Lock Task Mode, or system-app privileges.

Important limitations:

- The overlay is removable by Android system behavior, app uninstall, force stop, or the notification emergency stop action.
- Samsung/Android may hide third-party overlays on sensitive system screens.
- This MVP does not change global brightness and does not request `WRITE_SETTINGS`.

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
