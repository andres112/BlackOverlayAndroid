# Repository Guidelines

## Project

BlackOverlayAndroid is a minimal native Android app for personal sideloading on a Samsung Galaxy S24+ running Android 16. The package name is `com.andres.blackoverlay`.

The app intentionally implements a visual black overlay, not a real lock screen. Do not add code that tries to block Home, Recents, notification shade, power button, system navigation, Device Owner behavior, Lock Task Mode, root behavior, AccessibilityService behavior, telemetry, ads, cloud services, or Play Store-specific distribution features.

## Stack

- Kotlin
- Native Android Views, not Compose
- Gradle Kotlin DSL
- minSdk 28
- compileSdk 36
- targetSdk 36
- AndroidX Biometric
- AndroidX Lifecycle ViewModel
- Foreground service
- `WindowManager` with `TYPE_APPLICATION_OVERLAY`

## Structure

- Root Gradle files: `settings.gradle.kts`, `build.gradle.kts`
- App Gradle file: `app/build.gradle.kts`
- Manifest: `app/src/main/AndroidManifest.xml`
- Kotlin sources: `app/src/main/java/com/andres/blackoverlay/`
- Native Views layouts: `app/src/main/res/layout/`
- Values resources: `app/src/main/res/values/`
- Technical docs: `docs/`

## Implementation Notes

- Keep the app small and dependency-light.
- Use native Android Views and XML layouts.
- Keep overlay permission handling graceful via `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`.
- Keep notification permission handling graceful on Android 13+.
- The foreground service should remain robust against repeated starts and duplicate overlay views.
- Overlay views must consume touches and use `TYPE_APPLICATION_OVERLAY`.
- Overlay unlock should be triggered by followed taps anywhere on the overlay. The supported tap-count range must stay centralized in `OverlaySettings`.
- Unlock should require AndroidX `BiometricPrompt` with `BIOMETRIC_WEAK | DEVICE_CREDENTIAL`.
- Quick Settings tile behavior is activation-only. Do not allow the tile or foreground notification to stop/unlock an active overlay.
- Keep settings persistence behind `OverlaySettingsRepository`; do not spread `SharedPreferences` keys through activities, services, or tiles.
- Keep main-screen settings UI behind `MainViewModel` so `MainActivity` remains mostly view binding, permission handling, and Android intent orchestration.
- Do not request `WRITE_SETTINGS` unless explicitly implementing future brightness control.
- For target SDK 36 foreground service compatibility, preserve the special-use foreground service declaration unless replacing it with a better documented Android 16-compatible type.

## Verification

Preferred checks:

```bash
./gradlew assembleDebug
```

On Windows PowerShell, if the Gradle wrapper is unavailable, use an installed Gradle only if present:

```powershell
gradle assembleDebug
```

If Gradle or Android SDK components are unavailable in the local environment, inspect imports, manifest declarations, resource references, and Gradle configuration manually and report the limitation.

## Git Hygiene

- Do not revert user changes.
- Keep edits scoped to the Android MVP unless the user requests otherwise.
- Avoid unrelated formatting churn.
