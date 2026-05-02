# BlackOverlayAndroid

## Project purpose
This app provides a **visual full-screen black overlay** for personal use on Android devices. It runs a foreground service and places a touch-consuming `TYPE_APPLICATION_OVERLAY` window above apps.

## Why this is an overlay and not a real lock screen
This app is **not** a system lock screen. It cannot and does not block Home, Recents, notification shade, power menu, or other privileged system UI. It only draws a black overlay over normal app content.

## Known limitations
- Some sensitive system screens may not allow third-party overlays.
- OEM behavior (including Samsung) may vary across One UI updates.
- If notification permission is denied on newer Android versions, foreground service notification behavior may be restricted.
- This MVP does not change global brightness.
- TODO (future): optional brightness control via `WRITE_SETTINGS` (not requested in this MVP).

## Open in Android Studio
1. Clone this repository.
2. Open Android Studio.
3. Choose **Open** and select the repository root (`BlackOverlayAndroid`).
4. Let Gradle sync complete.

## Samsung Galaxy S24+ setup (Android 16)
### Enable Developer Options
1. Open **Settings** > **About phone** > **Software information**.
2. Tap **Build number** 7 times.
3. Enter device PIN/password when prompted.

### Enable USB debugging
1. Open **Settings** > **Developer options**.
2. Enable **USB debugging**.

### Connect phone to Android Studio
1. Connect with USB.
2. Accept the phone prompt to trust the computer.
3. Verify with `adb devices`.

## Grant overlay permission (Appear on top)
1. Open the app.
2. Tap **Request overlay permission**.
3. In Samsung settings, allow **Appear on top** for this app.

## Grant notification permission (if prompted)
- Allow notifications when Android asks, so the foreground service notification remains visible and compliant.

## Build debug APK
```bash
./gradlew assembleDebug
```

## Install with adb
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Install directly from Android Studio
1. Select your Galaxy S24+ as the deployment target.
2. Click **Run**.

## Manual test flow
1. Open app.
2. Grant overlay permission.
3. Start overlay.
4. Long press black screen.
5. Authenticate (biometric or device credential).
6. Confirm overlay disappears.
