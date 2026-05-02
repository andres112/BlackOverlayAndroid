# Technical Documentation

## Architecture

The app has three runtime pieces:

- `MainActivity`: permission/status UI and manual start/stop controls.
- `BlackOverlayService`: foreground service that owns the overlay window and notification.
- `UnlockActivity`: authentication screen launched by long pressing the overlay.

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

## Project Structure

```text
.
+-- settings.gradle.kts
+-- build.gradle.kts
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
        |   +-- UnlockActivity.kt
        +-- res/
            +-- layout/activity_main.xml
            +-- values/colors.xml
            +-- values/strings.xml
            +-- values/themes.xml
            +-- drawable/ic_launcher_background.xml
```

## File Map

`settings.gradle.kts`

- Defines plugin repositories and includes the `:app` module.

`build.gradle.kts`

- Root Gradle configuration.
- Keeps Android and Kotlin plugin versions centralized.

`app/build.gradle.kts`

- Android application module configuration.
- Sets namespace and application id to `com.andres.blackoverlay`.
- Uses `minSdk 28`, `compileSdk 36`, and `targetSdk 36`.
- Adds AndroidX Core, AppCompat, Material, and Biometric dependencies.
- Uses Java/Kotlin 17 bytecode settings.

`app/src/main/AndroidManifest.xml`

- Declares overlay, foreground service, special-use foreground service, and notification permissions.
- Registers `MainActivity`, `UnlockActivity`, and `BlackOverlayService`.
- Marks the service with `android:foregroundServiceType="specialUse"`.
- Adds `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` for target SDK 36 foreground service validation.

`MainActivity.kt`

- Displays overlay and notification permission status.
- Opens Android overlay permission settings.
- Requests notification permission on Android 13+.
- Starts and stops `BlackOverlayService`.

`BlackOverlayService.kt`

- Creates the foreground notification channel.
- Starts as a foreground service.
- Creates one fullscreen black overlay view via `WindowManager`.
- Uses `TYPE_APPLICATION_OVERLAY`.
- Consumes all touches with an `OnTouchListener`.
- Detects a long press with `GestureDetector`.
- Launches `UnlockActivity`.
- Removes the overlay safely when the service stops.

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

## Overlay Design

The service creates a plain `View` with an opaque black background. It uses:

- `WindowManager.LayoutParams.MATCH_PARENT` width and height
- `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`
- `WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN`
- `PixelFormat.OPAQUE`

The overlay consumes every touch event by returning `true` from its touch listener. This prevents touches from passing through to normal apps underneath the overlay.

The app intentionally does not try to block system navigation. Android reserves Home, Recents, power, and some system surfaces for the operating system.

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

Fallback on Windows PowerShell if the wrapper is unavailable and installed Gradle exists:

```powershell
gradle assembleDebug
```

Manual inspection checklist:

- Package name is consistently `com.andres.blackoverlay`.
- Manifest permissions match app behavior.
- `BlackOverlayService` uses `TYPE_APPLICATION_OVERLAY`.
- Overlay permission is checked before service start and inside service start.
- Notification permission is requested only on Android 13+.
- Unlock uses AndroidX `BiometricPrompt`.
- Service removes the overlay in `onDestroy()`.
- No Device Owner, Lock Task Mode, AccessibilityService, root, telemetry, ads, cloud service, or Play Store-specific behavior is present.
