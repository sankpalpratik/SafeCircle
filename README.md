# SafeCircle

Offline-first emergency safety app for a small trusted group of Android users.

> CI validation branch.

## What is implemented

- Large SOS button with a 5-second cancellation window.
- Up to 3 emergency contacts.
- SMS SOS with last-known location when cellular service and SMS permission are available.
- Direct emergency-services call button, defaulting to **999**.
- Nearby device-to-device SOS transport using Google Nearby Connections. This can work without internet when another SafeCircle phone is nearby.
- Shared circle code to avoid treating unrelated nearby SafeCircle devices as trusted circle members.
- Local-only settings and contact storage. No backend or account is required.

## Important limitations

SafeCircle is a supplemental safety tool, not a replacement for Android Emergency SOS or emergency services.

A normal Android app cannot guarantee communication when there is no internet **and** no cellular service. Nearby Connections can reach another compatible phone that is physically nearby, while SMS requires cellular service. Satellite emergency communication remains device/platform dependent.

Android background execution, Bluetooth permissions, battery optimization, OEM behavior, SMS permissions, and Google Play policy can affect reliability. The app should be tested on the exact phones it will be used on.

## Stack

- Kotlin
- Jetpack Compose + Material 3
- Google Play services Location
- Google Nearby Connections
- SharedPreferences for local configuration

## Running

Open the project in Android Studio, allow Gradle to sync, and run the `app` configuration on an Android 8.0+ device. For realistic testing, use three physical Android phones because nearby discovery and SMS behavior cannot be meaningfully validated on a single emulator.

Before real-world use, test:

1. SOS with internet available.
2. SOS with mobile data disabled but cellular service available.
3. SOS with internet and cellular data unavailable while the other SafeCircle phones are nearby.
4. Location permission denied.
5. Bluetooth/Nearby permissions denied.
6. Battery saver enabled.
7. Screen locked.
8. A false-trigger cancellation.
9. Emergency call behavior.

Never use a real emergency number for testing. Use the app's non-emergency test path or your carrier/device test facilities.
