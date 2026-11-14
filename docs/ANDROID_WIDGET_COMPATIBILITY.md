# Android widget compatibility checks

SecondsClock's seconds display is a home-screen widget contract, not just a time-formatting
contract. Keep `targetSdk` unchanged until the visible-widget checks below have been run on the
supported launcher/device matrix.

## Automated GitHub Actions check

`.github/workflows/widget-compatibility.yml` runs the instrumentation test on API 26 and the
current stable package (API 37.2) for pull requests and pushes to `master`, using GitHub-hosted
Ubuntu 24.04 runners. To check all supported/prerelease images, open **Actions → Android widget
compatibility → Run workflow**, select the branch, enable **Run the full Android prerelease
compatibility matrix**, and start the run. The full matrix adds the Android 17 base stable image
(API 37.0), Android 17 QPR2 Beta 3, and the latest listed Canary image (`android-canary-20261104`).
The debug APK is uploaded from the API 37.2 stable job after its test passes. Emulator test jobs
have a 20-minute timeout so a stalled instrumentation run does not hang indefinitely.
These package/API mappings were checked against the [official Android SDK repository metadata](https://dl.google.com/android/repository/repository2-3.xml)
on 2026-11-13: `android-37.2` is stable (extension 24), `android-37.2-beta3` reports API 37.1
(codename `DEV`, extension 23), and `android-canary-20261104` reports API 37.2 (codename `CANARY`,
extension 24). The current stable, beta, and Canary Google APIs x86_64 images are published with
16 KB page size; the workflow checks the exact image path in each generated AVD. `targetSdk` remains
26. The beta and Canary images cannot resolve a named `pixel_2` hardware profile, so those AVDs use
the emulator's generic default profile instead. `avdmanager` records their generated targets as
`android-37.2-beta3` and `android-canary-20261104`, respectively, rather than the `DEV`/`CANARY`
codenames reported by the SDK metadata.

The test installs the real SecondsClock APK and binds its declared `SecondsClockWidget` provider to
an Android `AppWidgetHost`. It receives the provider's serialized `RemoteViews` through the platform
AppWidget service, attaches the returned `AppWidgetHostView` to a visible test-host Activity, waits
for the provider's seconds-formatted update (rather than asserting against the initial placeholder),
and checks that the actual `TextClock` advances through at least three displayed values within 12
seconds. Before instrumentation runs, the workflow grants its debug host widget-bind authorization
through Android's shell command so the test exercises real framework binding and `RemoteViews`
delivery without depending on the version-specific Settings consent screen. This test-only setup
does not change the APK's `targetSdk` (still 26) or production permissions. The host Activity is
included only in the debug variant, not release APKs. This is intentionally stronger than inflating
a `TextClock` in a unit test.

The harness uses a test-owned widget host, not the emulator's home-screen launcher. It therefore
checks framework binding, provider delivery, `RemoteViews` application, and visible `TextClock`
second ticks, but it does **not** claim to verify launcher-specific visibility callbacks, launcher
refresh policies, or OEM behavior. It does not automate the app's configuration screen.

## Manual home-screen launcher matrix

Run this check before changing `targetSdk`, on API 26 and the current stable API 37.2 image, plus the
local Pixel_10 (Android 17) and representative physical/OEM launchers when possible. Include a beta
or Canary image when testing prerelease compatibility:

1. Install the same APK without changing its target SDK. Add a SecondsClock widget from the system
   widget picker and leave it visible on the home screen.
2. Configure it to show the time and seconds. Confirm the seconds visibly change on successive
   seconds for at least one minute. Open the full-screen clock and confirm its seconds also advance.
   Record the Android version, launcher name/version, APK build, and result.
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
