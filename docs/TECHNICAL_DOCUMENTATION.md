# Technical Documentation

## Architecture

The app has three runtime pieces:

- `MainActivity`: permission/status UI and manual start/stop controls.
- `BlackOverlayService`: foreground service that owns the overlay window and notification.
- `UnlockActivity`: authentication screen launched by long pressing the overlay.
- `BlackOverlayTileService`: optional Quick Settings tile for fast start/stop access.

The overlay lifetime is tied to `BlackOverlayService`. Starting the service creates the foreground notification and adds the overlay view. Stopping the service removes the view in `onDestroy()`.

## Runtime Flow

1. User opens `MainActivity`.
2. `MainActivity` checks `Settings.canDrawOverlays()`.
3. If overlay permission is missing, `MainActivity` opens `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`.
4. User taps **Start black overlay**.
5. `MainActivity` requests notification permission on Android 13+ if needed.
6. `MainActivity` starts `BlackOverlayService` with `ContextCompat.startForegroundService()`.
7. `BlackOverlayService` calls `startForeground()` and creates the black overlay.
8. The overlay consumes touch events.
9. A long press launches `UnlockActivity`.
10. `UnlockActivity` shows AndroidX `BiometricPrompt`.
11. Successful authentication stops `BlackOverlayService`.
12. `BlackOverlayService.onDestroy()` removes the overlay.

Cancel, error, and failed authentication paths do not stop the service, so the overlay remains active.

Quick Settings flow:

1. User adds the **Black Overlay** tile from Android Quick Settings edit mode.
2. If overlay permission is granted, tapping the tile starts or stops `BlackOverlayService`.
3. If overlay permission is missing, tapping the tile opens `MainActivity`.

## Project Structure

```text
.
+-- settings.gradle.kts
+-- build.gradle.kts
+-- gradle.properties
+-- README.md
+-- docs/
|   +-- SETUP_AND_DEBUGGING.md
|   +-- TECHNICAL_DOCUMENTATION.md
+-- app/
    +-- build.gradle.kts
    +-- proguard-rules.pro
    +-- src/main/
        +-- AndroidManifest.xml
        +-- java/com/andres/blackoverlay/
        |   +-- MainActivity.kt
        |   +-- BlackOverlayService.kt
        |   +-- BlackOverlayTileService.kt
        |   +-- UnlockActivity.kt
        +-- res/
            +-- layout/activity_main.xml
            +-- drawable/
            +-- mipmap-anydpi-v26/
            +-- values/colors.xml
            +-- values/strings.xml
            +-- values/themes.xml
```

## File Map

`settings.gradle.kts`

- Defines plugin repositories and includes the `:app` module.

`build.gradle.kts`

- Root Gradle configuration.
- Keeps Android and Kotlin plugin versions centralized.

`gradle.properties`

- Enables AndroidX with `android.useAndroidX=true`.
- Suppresses the AGP 8.7.3 compile SDK 36 compatibility warning with `android.suppressUnsupportedCompileSdk=36`.
- Sets JVM memory and UTF-8 encoding for Gradle.

`app/build.gradle.kts`

- Android application module configuration.
- Sets namespace and application id to `com.andres.blackoverlay`.
- Uses `minSdk 28`, `compileSdk 36`, and `targetSdk 36`.
- Adds AndroidX Core, AppCompat, and stable AndroidX Biometric dependencies.
- Uses Java/Kotlin 17 bytecode settings.

`app/src/main/AndroidManifest.xml`

- Declares overlay, foreground service, special-use foreground service, and notification permissions.
- Registers `MainActivity`, `UnlockActivity`, and `BlackOverlayService`.
- Disables app backup because the app has no user data that should be backed up.
- Uses project-owned adaptive launcher icons instead of platform drawable icons.
- Marks the service with `android:foregroundServiceType="specialUse"`.
- Adds `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` for target SDK 36 foreground service validation.
- Registers `BlackOverlayTileService` with `android.permission.BIND_QUICK_SETTINGS_TILE`.

`MainActivity.kt`

- Displays overlay and notification permission status.
- Opens Android overlay permission settings.
- Requests notification permission on Android 13+.
- Starts and stops `BlackOverlayService`.
- Disables the start button until overlay permission is granted.

`BlackOverlayService.kt`

- Creates the foreground notification channel.
- Starts as a foreground service.
- Creates one fullscreen black overlay view via `WindowManager`.
- Uses `TYPE_APPLICATION_OVERLAY`.
- Consumes all touches with an `OnTouchListener`.
- Uses `FLAG_NOT_FOCUSABLE` so the app overlay does not take key/input focus from the system.
- Detects a long press with `GestureDetector`.
- Launches `UnlockActivity`.
- Removes the overlay safely when the service stops.
- Stores simple local overlay-active state for Quick Settings tile display.

`BlackOverlayTileService.kt`

- Provides a Quick Settings tile named **Black Overlay**.
- Starts the foreground service when the tile is inactive.
- Stops the foreground service when the tile is active.
- Opens `MainActivity` if overlay permission has not been granted.

`UnlockActivity.kt`

- Builds an AndroidX `BiometricPrompt`.
- Allows `BIOMETRIC_WEAK | DEVICE_CREDENTIAL`.
- Stops `BlackOverlayService` only after successful authentication.
- Finishes without stopping the service on prompt cancellation or errors.

`app/src/main/res/layout/activity_main.xml`

- Native Android Views layout for the minimal UI.
- Contains permission status text and three buttons.

`app/src/main/res/values/strings.xml`

- User-visible strings for buttons, notification, and authentication prompt.

`app/src/main/res/values/colors.xml`

- Theme color resources.

`app/src/main/res/values/themes.xml`

- App theme used by activities.

`app/src/main/res/drawable/`

- Shape drawables for the native Views UI.
- Vector drawables for the launcher foreground and foreground-service notification icon.

`app/src/main/res/mipmap-anydpi-v26/`

- Adaptive launcher icon definitions for Android 8.0+.

## Permissions

`SYSTEM_ALERT_WINDOW`

- Required for `TYPE_APPLICATION_OVERLAY`.
- Must be granted manually by the user through Android settings.

`FOREGROUND_SERVICE`

- Required to run a foreground service.

`FOREGROUND_SERVICE_SPECIAL_USE`

- Used because this service does not fit a narrower foreground service type and targets SDK 36.

`POST_NOTIFICATIONS`

- Required on Android 13+ for notification runtime permission.
- Requested gracefully from `MainActivity`.

## Security And Privacy

- The app does not include telemetry, analytics, ads, cloud services, or remote backends.
- Activities and the foreground service are not exported except `MainActivity`, which is the launcher entry point.
- App backup is disabled in the manifest.
- Cleartext network traffic is disabled, and the app does not request internet access.
- Notification actions use immutable `PendingIntent` flags.
- The foreground service re-checks overlay permission before adding the window.
- The overlay uses app-level `TYPE_APPLICATION_OVERLAY`; it does not request Device Owner, AccessibilityService, root, Lock Task Mode, or system-app privileges.
- Signing keys, local SDK paths, build outputs, IDE metadata, and generated reports are ignored by `.gitignore`.

## Overlay Design

The service creates a plain `View` with an opaque black background. It uses:

- `WindowManager.LayoutParams.MATCH_PARENT` width and height
- `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`
- `WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN`
- `WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS`
- `WindowManager.LayoutParams.FLAG_FULLSCREEN`
- `WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE`
- `PixelFormat.OPAQUE`

The overlay consumes every touch event by returning `true` from its touch listener. This prevents touches from passing through to normal apps underneath the overlay.

The overlay also requests immersive fullscreen system UI flags so Android can lay it out behind the navigation area where allowed. The app intentionally does not try to block system navigation. Android reserves Home, Back, Recents, power, and some system surfaces for the operating system, so Samsung/Android may still keep parts of the bottom navigation area interactive.

## Foreground Service Behavior

`BlackOverlayService` calls `startForeground()` before adding the overlay. The notification is:

- persistent
- low importance
- only alerts once
- visible while the overlay is active
- equipped with a **Stop** action for emergency development testing

Repeated service starts are handled by checking whether `overlayView` is already non-null before adding a view. This avoids duplicate overlay windows.

## Authentication Behavior

`UnlockActivity` checks whether the device can authenticate with:

```kotlin
BIOMETRIC_WEAK or DEVICE_CREDENTIAL
```

This supports devices with weak biometrics, PIN, pattern, or password. If authentication is unavailable, the activity closes and leaves the overlay active.

Only `onAuthenticationSucceeded()` stops the service. `onAuthenticationError()` finishes the activity but keeps the overlay active. `onAuthenticationFailed()` leaves the prompt open where Android supports it.

## Brightness

The MVP does not control global brightness and does not request `WRITE_SETTINGS`.

Future optional brightness control would need a separate user-approved `WRITE_SETTINGS` flow. That should remain separate from the overlay MVP because it changes system settings rather than only drawing an app-owned overlay.

## Validation

Preferred build check:

```bash
./gradlew assembleDebug
```

Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

Fallback on Windows PowerShell if the wrapper is unavailable and installed Gradle exists:

```powershell
gradle assembleDebug
```

Android Studio and Gradle may generate local files during sync/build:

- `.gradle/`
- `.idea/`
- `build/`
- `app/build/`
- `local.properties`
- `build/reports/problems/problems-report.html`
- `app/build/outputs/logs/manifest-merger-debug-report.txt`

These are local environment or generated build artifacts. They are ignored by `.gitignore` and should not be committed.

Manual inspection checklist:

- Package name is consistently `com.andres.blackoverlay`.
- Manifest permissions match app behavior.
- `BlackOverlayService` uses `TYPE_APPLICATION_OVERLAY`.
- Overlay permission is checked before service start and inside service start.
- Notification permission is requested only on Android 13+.
- Unlock uses AndroidX `BiometricPrompt`.
- Service removes the overlay in `onDestroy()`.
- No Device Owner, Lock Task Mode, AccessibilityService, root, telemetry, ads, cloud service, or Play Store-specific behavior is present.
