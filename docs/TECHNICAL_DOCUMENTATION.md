# Technical Documentation

## Architecture

The app is intentionally small, but it separates runtime Android components from settings storage and main-screen state.

Runtime components:

- `MainActivity`: native Views screen for permissions, tap-count configuration, and manual start/stop controls.
- `BlackOverlayService`: foreground service that owns the overlay window and notification.
- `UnlockActivity`: authentication screen launched after the configured tap sequence.
- `BlackOverlayTileService`: optional Quick Settings tile for activation-only access.

Settings and state components:

- `OverlaySettings`: central constants for preference names, keys, defaults, and supported tap-count range.
- `OverlaySettingsRepository`: repository wrapper around `SharedPreferences`.
- `MainViewModel`: main-screen ViewModel that exposes settings to `MainActivity`.

The overlay lifetime is tied to `BlackOverlayService`. Starting the service creates the foreground notification and adds the overlay view. Stopping the service removes the view in `onDestroy()`.

## Patterns

The project uses a deliberately lightweight MVVM + Repository design.

MVVM is used only where it provides value today: `MainActivity` is the View, and `MainViewModel` is the ViewModel for main-screen settings state. `MainActivity` still owns Android framework orchestration such as permission requests, `Intent` launches, and button click wiring because those are view/controller concerns in this small native Views app. The ViewModel exposes the unlock tap-count options and persists user changes through the repository.

The Repository pattern is used for settings persistence. `OverlaySettingsRepository` is the only class that reads and writes the app's `SharedPreferences`; services, tile code, and activities consume intent-level methods such as `getUnlockTapCount()` or `setOverlayActive()`. This keeps preference keys and coercion rules out of runtime components and prevents future settings from spreading across the codebase.

`OverlaySettings` is the central configuration point for settings constants. To change the supported unlock tap-count range, update `MIN_UNLOCK_TAP_COUNT`, `MAX_UNLOCK_TAP_COUNT`, and optionally `DEFAULT_UNLOCK_TAP_COUNT` there. The dropdown options are generated from those constants, and the service reads the same persisted value through the repository.

## Runtime Flow

1. User opens `MainActivity`.
2. `MainActivity` checks `Settings.canDrawOverlays()`.
3. If overlay permission is missing, `MainActivity` opens `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`.
4. User taps **Start black overlay**.
5. `MainActivity` requests notification permission on Android 13+ if needed.
6. `MainActivity` starts `BlackOverlayService` with `ContextCompat.startForegroundService()`.
7. `BlackOverlayService` calls `startForeground()` and creates the black overlay.
8. The overlay consumes touch events.
9. Followed taps anywhere on the overlay are counted by `BlackOverlayService`.
10. When the configured tap count is reached, `BlackOverlayService` launches `UnlockActivity`.
11. `UnlockActivity` shows AndroidX `BiometricPrompt`.
12. Successful authentication stops `BlackOverlayService`.
13. `BlackOverlayService.onDestroy()` removes the overlay.

Cancel, error, and failed authentication paths do not stop the service, so the overlay remains active.

Quick Settings flow:

1. User taps the unchecked **Quick Settings tile** checkbox in `MainActivity`, or manually adds the tile from Android Quick Settings edit mode.
2. If overlay permission is granted and the overlay is inactive, tapping the tile starts `BlackOverlayService`.
3. If the overlay is already active, tapping the tile leaves it active.
4. If overlay permission is missing, tapping the tile opens `MainActivity`.

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
        |   +-- MainViewModel.kt
        |   +-- BlackOverlayService.kt
        |   +-- BlackOverlayTileService.kt
        |   +-- OverlaySettings.kt
        |   +-- OverlaySettingsRepository.kt
        |   +-- UnlockActivity.kt
        +-- res/
            +-- layout/activity_main.xml
            +-- layout/item_spinner_unlock_tap_count.xml
            +-- layout/item_spinner_unlock_tap_count_dropdown.xml
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
- Adds AndroidX Core, AppCompat, AndroidX Biometric, and AndroidX Lifecycle ViewModel dependencies.
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

- Displays overlay and notification permission state with switches.
- Displays known Quick Settings tile state with a checkbox.
- Hosts the main native Views screen.
- Uses `MainViewModel` for unlock tap-count settings.
- Populates a dropdown from the ViewModel's tap-count options.
- Opens Android overlay permission settings.
- Requests that Android add the app's Quick Settings tile on Android 13+ from the checkbox row.
- Requests notification permission on Android 13+.
- Starts and stops `BlackOverlayService`.
- Disables the start button until overlay permission is granted.

`MainViewModel.kt`

- Implements the ViewModel side of the main screen's MVVM split.
- Exposes unlock tap-count options generated from `OverlaySettings`.
- Reads and writes settings through `OverlaySettingsRepository`.
- Keeps settings persistence out of `MainActivity`.

`OverlaySettings.kt`

- Centralizes local settings names, keys, defaults, and allowed ranges.
- Defines unlock tap-count range constants: 3 minimum, 7 maximum, 3 default.
- Generates the dropdown option list from the configured range.

`OverlaySettingsRepository.kt`

- Owns `SharedPreferences` access.
- Persists overlay active state for Quick Settings tile status.
- Persists unlock tap-count configuration.
- Persists whether Android has reported the Quick Settings tile as added or already present.
- Coerces stored tap-count values back into the supported range.

`BlackOverlayService.kt`

- Creates the foreground notification channel.
- Starts as a foreground service.
- Creates one fullscreen black overlay view via `WindowManager`.
- Uses `TYPE_APPLICATION_OVERLAY`.
- Consumes all touches with an `OnTouchListener`.
- Uses `FLAG_NOT_FOCUSABLE` so the app overlay does not take key/input focus from the system.
- Counts followed taps anywhere on the overlay.
- Reads the required tap count from `OverlaySettingsRepository`.
- Launches `UnlockActivity`.
- Removes the overlay safely when the service stops.
- Stores overlay-active state through `OverlaySettingsRepository`.

`BlackOverlayTileService.kt`

- Provides a Quick Settings tile named **Black Overlay**.
- Starts the foreground service when the tile is inactive.
- Leaves the foreground service running when the tile is active.
- Opens `MainActivity` if overlay permission has not been granted.
- Reads overlay-active state through `OverlaySettingsRepository`.

`UnlockActivity.kt`

- Builds an AndroidX `BiometricPrompt`.
- Allows `BIOMETRIC_WEAK | DEVICE_CREDENTIAL`.
- Stops `BlackOverlayService` only after successful authentication.
- Finishes without stopping the service on prompt cancellation or errors.

`app/src/main/res/layout/activity_main.xml`

- Native Android Views layout for the minimal UI.
- Contains overlay controls, setup switches, Quick Settings tile checkbox, unlock tap-count dropdown, and action buttons.

`app/src/main/res/layout/item_spinner_unlock_tap_count.xml`

- Selected-row layout for the unlock tap-count dropdown.

`app/src/main/res/layout/item_spinner_unlock_tap_count_dropdown.xml`

- Popup-row layout for the unlock tap-count dropdown.

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
- Activity-launch `PendingIntent` usage is immutable where required by Android APIs.
- The foreground service re-checks overlay permission before adding the window.
- The Quick Settings tile is activation-only and does not stop or unlock an active overlay.
- The foreground notification has no stop action, preventing notification-shade unlock bypass.
- Only successful biometric/device-credential authentication removes the overlay during normal locked operation.
- The overlay uses app-level `TYPE_APPLICATION_OVERLAY`; it does not request Device Owner, AccessibilityService, root, Lock Task Mode, or system-app privileges.
- Signing keys, local SDK paths, build outputs, IDE metadata, and generated reports are ignored by `.gitignore`.

## Overlay Design

The service creates a plain `View` with an opaque black background. It uses:

- `WindowManager.LayoutParams.MATCH_PARENT` width and height
- `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`
- `WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN`
- `WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS`
- `WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE`
- `PixelFormat.OPAQUE`

The overlay consumes every touch event by returning `true` from its touch listener. This prevents touches from passing through to normal apps underneath the overlay.

`BlackOverlayService` counts `ACTION_UP` events. If the pause between taps exceeds the configured timeout, the sequence resets. When the count reaches the configured value from `OverlaySettingsRepository`, the service launches `UnlockActivity`.

The app intentionally does not try to block system navigation. Android reserves Home, Back, Recents, power, and some system surfaces for the operating system, so Samsung/Android may still keep parts of the bottom navigation area interactive.

## Foreground Service Behavior

`BlackOverlayService` calls `startForeground()` before adding the overlay. The notification is:

- persistent
- low importance
- only alerts once
- visible while the overlay is active
- not equipped with a stop action

Repeated service starts are handled by checking whether `overlayView` is already non-null before adding a view. This avoids duplicate overlay windows.

The Quick Settings tile mirrors overlay-active state for display, but it does not stop an active overlay. This prevents unlocking or removing the overlay from the notification shade or pull-down controls.

## Authentication Behavior

`UnlockActivity` checks whether the device can authenticate with:

```kotlin
BIOMETRIC_WEAK or DEVICE_CREDENTIAL
```

This supports devices with weak biometrics, PIN, pattern, or password. If authentication is unavailable, the activity closes and leaves the overlay active.

Only `onAuthenticationSucceeded()` stops the service. `onAuthenticationError()` finishes the activity but keeps the overlay active. `onAuthenticationFailed()` leaves the prompt open where Android supports it.

The unlock prompt is reachable only after the configured tap sequence. The default is 3 taps, and the supported range is 3 through 7 taps. The range is defined in `OverlaySettings`, persisted through `OverlaySettingsRepository`, exposed to the UI through `MainViewModel`, and consumed by `BlackOverlayService`.

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
- Unlock tap-count range is centralized in `OverlaySettings`.
- Settings persistence goes through `OverlaySettingsRepository`.
- Main-screen settings state goes through `MainViewModel`.
- Quick Settings tile starts only and does not stop an active overlay.
- Foreground notification has no stop action.
- Service removes the overlay in `onDestroy()`.
- No Device Owner, Lock Task Mode, AccessibilityService, root, telemetry, ads, cloud service, or Play Store-specific behavior is present.
