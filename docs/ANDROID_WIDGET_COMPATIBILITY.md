# Android widget compatibility checks

SecondsClock's seconds display is a home-screen widget contract, not just a time-formatting
contract. Keep `targetSdk` unchanged until the visible-widget checks below have been run on the
supported launcher/device matrix.

## Automated GitHub Actions check

`.github/workflows/widget-compatibility.yml` runs the instrumentation test on the API 26 minimum
and stable API 37.0 (Android 17) x86_64 Google APIs emulator images using GitHub-hosted Ubuntu 24.04
runners. The Android SDK package ID is `android-37.0`; API 37.2 is currently QPR2 beta and is
intentionally excluded. The workflow does not edit the existing CI workflow or change `targetSdk`.

The test installs the real SecondsClock APK and binds its declared `SecondsClockWidget` provider to
an Android `AppWidgetHost`. It receives the provider's serialized `RemoteViews` through the platform
AppWidget service, attaches the returned `AppWidgetHostView` to a visible test-host Activity, and
checks that its actual `TextClock` has a seconds format and advances through at least three displayed
values within 12 seconds. This is intentionally stronger than inflating a `TextClock` in a unit test.

The harness uses a test-owned widget host, not the emulator's home-screen launcher. It therefore
checks framework binding, provider delivery, `RemoteViews` application, and visible `TextClock`
second ticks, but it does **not** claim to verify launcher-specific visibility callbacks, launcher
refresh policies, or OEM behavior. It does not automate the app's configuration screen.

## Manual home-screen launcher matrix

Run this check before changing `targetSdk`, on API 26 and API 37.0 (the newest stable Android system
image selected for the CI runner), plus representative physical/OEM launchers when possible:

1. Install the same APK without changing its target SDK. Add a SecondsClock widget from the system
   widget picker and leave it visible on the home screen.
2. Configure it to show the time and seconds. Confirm the seconds visibly change on successive
   seconds for at least one minute; record the Android version, launcher name/version, APK build,
   and result.
3. Put the widget on a different home-screen page (not visible), then return to its page. Confirm
   it resumes changing seconds while visible and remains current after returning from the hidden
   page.
4. Turn the screen off for roughly one minute, turn it back on, and confirm the displayed time is
   current and continues to tick. Note whether it stops updating while the screen is off.
5. Repeat after resizing the widget and after relaunching SecondsClock. Record any configuration,
   rendering, or update failures.

Treat the CI test and manual launcher observations as separate evidence. The CI test is not a
substitute for launcher coverage, and passing it alone is not sufficient evidence for raising
`targetSdk`.
