# Setup And Debugging

This guide starts from a fresh computer checkout and ends with the app running on a physical Samsung Galaxy S24+.

## Is It Safe?

This app uses normal Android app permissions and public APIs. It is a visual black overlay, not a real lock screen, and it does not take ownership of the phone.

It does not use:

- Device Owner
- Lock Task Mode
- AccessibilityService
- root access
- system-app permissions
- code that blocks Home, Recents, notification shade, power button, or system navigation

If something goes wrong during testing, use one of these recovery paths:

- Pull down notifications if Android allows it and tap the notification **Stop** action.
- Open the app again and tap **Stop overlay**.
- Force stop or uninstall the app from Android Settings.
- Reboot the phone.

Samsung/Android may suppress overlays on sensitive screens, including some permission, payment, system, and lock-related screens.

## Start From Scratch

1. Install Android Studio.
2. Install the Android SDK platform for API 36 if Android Studio prompts for it.
3. Clone this repository.
4. Open Android Studio.
5. Select **Open**.
6. Choose the repository root folder, `BlackOverlayAndroid`.
7. Wait for Gradle sync to finish.

The project root is the folder containing `settings.gradle.kts`.

## Enable Developer Options On Samsung

1. On the phone, open **Settings**.
2. Open **About phone**.
3. Open **Software information**.
4. Tap **Build number** 7 times.
5. Enter the device PIN, pattern, or password if prompted.

## Enable USB Debugging

1. Open **Settings**.
2. Open **Developer options**.
3. Enable **USB debugging**.
4. Connect the phone to the computer with USB.
5. Accept the phone prompt to trust the computer.

Check the connection:

```bash
adb devices
```

The device should appear as `device`, not `unauthorized`. If it is unauthorized, unlock the phone and accept the USB debugging prompt.

## Debug With Android Studio

1. Connect the Galaxy S24+ with USB.
2. In Android Studio, select the phone from the device picker near the Run button.
3. Select the `app` run configuration.
4. Click **Run** to install and launch the debug build.
5. Use **Logcat** in Android Studio to inspect runtime logs if the app fails to launch or a permission flow behaves unexpectedly.

For breakpoint debugging:

1. Open a Kotlin file, such as `MainActivity.kt`.
2. Click in the editor gutter to add a breakpoint.
3. Click **Debug** instead of **Run**.
4. Interact with the app on the phone until execution reaches the breakpoint.

## Install With Android Studio

1. Connect and authorize the phone.
2. Click **Run** in Android Studio.
3. Android Studio builds, installs, and launches the app automatically.

This is the easiest path while developing.

## Build A Debug APK

From the repository root:

```bash
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

On Windows PowerShell, if the Gradle wrapper is not available and installed Gradle is present:

```powershell
gradle assembleDebug
```

The APK path is:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install With adb

Build first, then install:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows PowerShell with installed Gradle but no wrapper:

```powershell
gradle assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Use `-r` to replace the existing debug install while keeping normal app data.

## Grant Overlay Permission

Android requires explicit user approval before an app can draw over other apps.

1. Open **Black Overlay**.
2. Tap **Request overlay permission**.
3. Samsung settings opens the app-specific overlay page.
4. Allow **Appear on top** for this app.
5. Return to the app.
6. Confirm the app shows overlay permission as granted.

If you need to find it manually:

1. Open **Settings**.
2. Open **Apps**.
3. Open the overflow menu or **Special app access**.
4. Open **Appear on top**.
5. Allow **Black Overlay**.

Samsung menu names can vary by One UI version.

## Grant Notification Permission

On Android 13 and newer, Android may ask whether this app can show notifications.

Allow notifications so the foreground service notification is visible. The service uses a persistent low-importance notification while the overlay is active.

If you denied it earlier:

1. Open **Settings**.
2. Open **Apps**.
3. Open **Black Overlay**.
4. Open **Notifications**.
5. Allow notifications.

## Test The App

1. Open the app.
2. Grant overlay permission.
3. Allow notifications if Android asks.
4. Tap **Start black overlay**.
5. Confirm the screen becomes black.
6. Long press the black screen.
7. Authenticate with biometric, PIN, pattern, or password.
8. Confirm the overlay disappears.

## Add The Quick Settings Tile

The app includes an optional Quick Settings tile for faster access from the pull-down top menu.

Android 13+:

1. Open **Black Overlay**.
2. Tap **Add Quick Settings tile**.
3. Accept Android's prompt to add the tile.
4. Swipe down from the top of the screen and tap **Black Overlay** to start or stop the overlay.

Manual method:

1. Swipe down from the top of the screen twice.
2. Tap the edit button for Quick Settings tiles.
3. Find **Black Overlay**.
4. Drag it into the active Quick Settings area.
5. Tap the tile to start or stop the overlay.

If overlay permission has not been granted yet, tapping the tile opens the app so you can grant permission first.

Emergency/manual test:

1. Start the overlay.
2. Use the foreground notification **Stop** action, or reopen the app and tap **Stop overlay**.
3. Confirm the black overlay is removed.

## Common Issues

`adb devices` shows no device:

- Try another USB cable or port.
- Unlock the phone.
- Re-enable USB debugging.
- Check that Samsung USB mode is not charge-only.

Android Studio cannot find SDK 36:

- Open **SDK Manager**.
- Install the Android SDK Platform for API 36.
- Sync Gradle again.

Command-line Gradle says `JAVA_HOME is not set`:

- Android Studio can build with its bundled runtime even when the terminal cannot.
- For terminal builds, install a JDK or point `JAVA_HOME` to Android Studio's bundled JBR/JDK.
- After changing `JAVA_HOME`, open a new terminal and run `.\gradlew.bat assembleDebug` again.

Gradle generated `build/reports/problems/problems-report.html`:

- This is a local build report.
- A deprecation warning in this report is not automatically a failed build.
- Check Android Studio's **Build** output for `BUILD SUCCESSFUL` or the first actual error.

Overlay does not appear:

- Confirm **Appear on top** is granted.
- Confirm the service notification appears.
- Try from a normal app screen, not a sensitive Android settings or security screen.

Overlay does not cover the navigation bar:

- Android reserves the navigation bar and gesture area for system control.
- This app does not block Home, Back, Recents, or system gestures.
- On Samsung, switching between gesture navigation and button navigation can change how much of the bottom system area Android allows third-party overlays to cover.

Unlock prompt closes without removing the overlay:

- Make sure the phone has a PIN, pattern, password, or biometric enrolled.
- Cancel/error paths intentionally leave the overlay active.

## Repository Hygiene

Do not commit local Android Studio or build output folders. The repository `.gitignore` excludes:

- `.idea/`
- `.gradle/`
- `build/`
- `app/build/`
- `local.properties`
- generated APK/AAB files
- signing keys and keystore property files
- generated logs and reports

Commit the Gradle wrapper files:

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
