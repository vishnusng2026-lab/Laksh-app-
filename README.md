# Laksh

Reminders that call you. Laksh rings your phone with a full-screen incoming-call screen at the time you choose, even on the lock screen. Everything is stored on the phone. The app has no internet permission and uploads nothing.

## Build it

1. Install Android Studio (current stable version).
2. Choose File > Open and select this `LakshApp` folder. Let Gradle sync finish (it downloads what it needs).
3. Plug in your phone with USB debugging on, or start an emulator, then press Run.

To get an installable APK instead: Build > Build APK(s). The file lands in `app/build/outputs/apk/debug/app-debug.apk`. Copy it to your phone and open it.

## First run on the phone

1. Open Laksh and tap **Allow notifications**.
2. On Android 14 and newer, tap **Allow full-screen calls** and switch it on. Without it, the call shows as a heads-up notification instead of taking over the screen.
3. Schedule a call, then tap **Test call** on it and lock the phone. The call screen rings in 5 seconds.

## Keep calls reliable

Some phone brands (Xiaomi, Oppo, Vivo, Realme, OnePlus, Samsung) stop background apps aggressively. Set Laksh to "Unrestricted" or "No restrictions" under Settings > Apps > Laksh > Battery, and turn on "Autostart" if your phone has that option.

## How it works

- `Scheduler` sets a real system alarm with `setAlarmClock`, which fires on time even in battery saver and Doze. A small alarm icon may appear in the status bar.
- `AlarmReceiver` checks that you are not on a real phone call or video call. If you are, it waits a minute and tries again.
- `CallService` plays your ringtone, vibrates, and shows the full-screen call notification.
- `CallActivity` is the call screen. Answer plays your voice note, or reads the task aloud. Then you can mark it done or snooze 5 minutes.
- Declined or missed calls ring again after 5 minutes, up to 3 more times.
- `BootReceiver` puts all alarms back after a restart.

## Limits to know

- The ringtone follows your phone's ringer mode. In silent mode the phone still vibrates.
- Android does not let normal apps use the real dialer screen, so this is a look-alike call screen drawn by Laksh.
- The app was written without access to an Android build environment, so run it once on your phone before depending on it.

## Build the APK without Android Studio (GitHub)

1. Create a free account at github.com and make a new repository.
2. Upload everything in this folder. The hidden `.github` folder must be included. If it is missing, choose Add file > Create new file, name it `.github/workflows/build.yml`, and paste in the contents of `github-build-workflow.yml`.
3. Open the Actions tab and wait for "Build Laksh APK" to finish (about 5 minutes).
4. Open the finished run, download `Laksh-APK`, unzip it, and send `app-debug.apk` to your phone.
