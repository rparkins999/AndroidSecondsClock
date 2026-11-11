# Releasing SecondsClock

This checklist keeps version metadata, F-Droid changelogs, build artifacts, and signing steps
explicit. Ordinary Gradle configuration/builds do not edit files or stage Git changes.

## Prepare a release

1. Confirm the intended changes are merged and run the app's regression checks. Seconds updating is
   a core behavior: do not raise `targetSdk` without testing second-by-second updates on the supported
   device/launcher matrix.
2. Choose a new, higher Android `versionCode` and the user-facing `versionName`. Write release notes
   as plain text in a temporary file, for example `/tmp/87.txt`, with one `- ` bullet per item.
3. Run the explicit metadata command from the repository root:

   ```sh
   python3 scripts/release.py prepare \
     --version-code 87 \
     --version-name 2.7.0 \
     --changelog-file /tmp/87.txt
   ```

   This updates the two version constants in `SecondsClock/build.gradle`, the version sentence in
   `README.md`, and creates `fastlane/metadata/android/en-US/changelogs/87.txt`. It never stages
   files and refuses to reuse a version code or overwrite a changelog. Review and stage the changes
   yourself.
4. Verify the metadata and run CI-equivalent checks:

   ```sh
   python3 scripts/release.py check
   ./gradlew clean test lint assembleDebug assembleRelease
   ```

   F-Droid expects the lowercase `changelogs/` directory, unpadded Android version-code filenames,
   and changelogs of at most 500 characters. Keep the matching file in
   `fastlane/metadata/android/en-US/changelogs/`; do not use version names as filenames.

## Build and sign APKs

Contributor builds need no private signing credentials. Debug builds use Android's debug key; a
release build without a configured key is unsigned and is for CI/build verification only.

For a distributable APK, use the authorized release keystore and verify its certificate before
uploading. Keep the keystore and `keystore.properties` outside Git. The build accepts either
`-PkeystorePropertiesFile=/absolute/path/to/keystore.properties` or the
`SECONDSCLOCK_KEYSTORE_PROPERTIES` environment variable. For local convenience it also checks the
ignored `local/keystore.properties`, then the legacy `../Keys/keystore.properties` location.
Properties use `keyAlias`, `keyPassword`, `storeFile`, and `storePassword`. After building, inspect
the APK package/version and use the Android SDK's `apksigner verify --verbose --print-certs` to
confirm the expected signing certificate. Never publish an unsigned APK or a debug APK as the
release artifact.

Do not commit credentials, upload credentials, or generated APKs. After reviewing and committing the
release metadata, create the version tag and publish the verified APK and release notes through the
project's configured release process. Update F-Droid metadata as required by its normal submission
process.

## Clean-checkout build requirements

Use JDK 17, Android SDK platform 28, and the checked-in Gradle wrapper. The launcher-icon task uses
the checked-in PNG fallback when `rsvg-convert` is unavailable; installing the optional converter
regenerates the timestamped icon from its SVG source.
