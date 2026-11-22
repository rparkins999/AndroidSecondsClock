# Changelog

Release history, newest first. The number in parentheses is the Android version code.

## 2.7.1 (88)

- Align the full-screen clock menu buttons when text wraps.

## 2.7.0 (87)

- Improve live colour preview and dimming in the full-screen clock.
- Add a system-clock app override, chooser, and full-screen fallback when no compatible app is available.
- Improve launcher-icon generation and refresh the static fallback artwork.
- Add optional analog home-screen widgets with advanced analog formats.
- Add Global Settings and reorganize main-menu actions.

## 2.6.0 (86)

- Add Advanced date/time formats for widgets (Horizontal and Vertical profiles) and the
  full-screen clock (Portrait and Landscape profiles).
- Add optional alarm blocks and `%|` no-alarm alternatives for custom formats.
- Add `%L` line emphasis and improve fitting for marked Landscape clock lines.
- Add text-style emoji and preserve joined emoji and flag sequences.
- Fix alarm-literal quoting and seconds-ticker defaults, and align ticks for pre-epoch times.
- Improve format-editor hints and shorten in-app settings and help text.
- Generate timestamped launcher icons from SVG when a rasterizer is available, with a static PNG
  fallback.
- Keep the full-screen clock awake while charging in both modes and check charging when it opens.
- Apply the configured full-screen text colour in Light appearance.
- Add Settings Manager presets, settings transfers, and JSON import/export; imports match named
  widgets and save unmatched widget settings as presets.

## 2.5 (9)

- Make the launcher icon show its build date and time while home-screen widgets continue to show the current date and time.
- Improve full-screen dimming near the ambient-light threshold and include other bug fixes.

## 2.4 (8)

- Fix a bug in `updatehsv()` that could make widget text invisible.

## 2.3 (7)

- Fix issues #3, #4, #5, and #6.

## 2.2 (6)

- Improve the full-screen clock's dimming algorithm.

## 2.1 (5)

- Make the first F-Droid release, with improved full-screen dimming and Fastlane metadata.

## 2.0 (4)

- Add multiple home-screen widgets and a full-screen night clock.

## 1.0 (2)

- Publish the first release with a signed APK.
