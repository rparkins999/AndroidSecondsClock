# SecondsClock

SecondsClock is an Android clock that runs as a home-screen widget or a full-screen night clock.
You can add multiple widgets and configure each one independently.

This is version 2.6.0.

## Choose a setup

### Easy setup: Checkbox mode

Choose what to show with checkboxes. SecondsClock formats the date and time for your device's
locale and arranges the selected information automatically. You don't need to know format tokens.

### Custom setup: Advanced mode

Use Advanced mode when you want specific text or a particular layout. Type plain text and insert
date/time tokens. Tokens are case-sensitive. Press Enter or use `%n` to start a line. Text scales
to fit, but line breaks stay where you put them, so a resized widget may need adjustment.

The options differ slightly between home-screen widgets and the full-screen clock; details follow.

## Home-screen widgets

### Checkbox mode

Choose whether to show the time, seconds, weekday, date, and next alarm. You can use a numeric date
or choose the day, month, and year separately. Time uses the device's 12- or 24-hour preference.
The layout adapts to the widget's size. The **Show next alarm (any alarm app)** option displays the
next alarm Android reports, not necessarily one from the default clock app.

### Advanced mode

Enter a custom format using date/time tokens. **Sync** shares one format between widget shapes.
Otherwise, the **Horizontal** and **Vertical** profiles are selected from the widget's reported
dimensions. New profiles start from Sync.

Common tokens:

| Token | Displays |
| --- | --- |
| `%H` | 24-hour clock hour |
| `%I` | 12-hour clock hour |
| `%M`, `%S`, `%p` | Minute, second, AM/PM |
| `%a`, `%A` | Short or full weekday |
| `%b`, `%B` | Short or full month name |
| `%d`, `%e` | Zero-padded or unpadded day |
| `%m`, `%Y` | Month number or year |
| `%F` | ISO date |
| `%x`, `%X` | Localized date or time |
| `%n`, `%%` | New line or literal percent sign |

#### Optional alarm text

Use `%<` and `%>` to mark an alarm block. Inside it, `%|` separates the text shown when an alarm
is set from an optional fallback shown when none is set:

```text
%<Alarm: %H:%M%|%nNO ALARM%>
```

When Android reports a next alarm-clock alarm from any app, the text before `%|` appears and its
tokens use the alarm time. Without an alarm, the text after `%|` appears and its tokens use the
current time. Without `%|`, the block is hidden when no alarm is set; the fallback can also be
empty. Android does not expose the alarm's label.

### Appearance and touch

You can change the widget's text colour, background colour, and background opacity. Widget touch
actions can open the system clock, SecondsClock, its settings, or the full-screen clock. You can
place multiple widgets with different settings on the same or different home-screen pages.

If a widget's touch action doesn't open its configuration page, open SecondsClock from its app icon
(usually in the apps list), then choose **Configure existing widget**. Touch the widget you want to
configure; the next widget you touch will open its configuration page regardless of its assigned
touch action.

Opening the system clock may not work on every device. The system clock app is OEM-specific, and
Android doesn't define a standard intent to launch it. SecondsClock tries several possible intents,
but they may not find the clock app on every device. If the system-clock action doesn't work, report
the device and, if possible, the name of its clock app so another intent can be considered.

### Seconds and battery

A widget showing seconds updates every second while it is visible, which uses more battery. It
doesn't update while hidden.

### Widget orientation

Some launchers don't notify widgets when the screen rotates. On those devices, a widget's layout may
not adapt to landscape until the launcher updates it or the widget is resized.

## Full-screen night clock

The full-screen clock's background is always black, but its text colour is configurable
in both Light and Dark appearance.

### Checkbox mode

Choose the time, seconds size, weekday, date, and next-alarm options. The clock arranges the
selected information automatically. The seconds size can be adjusted separately. Its checkbox-mode
alarm is shown as `ALM: Mon, 3:30`.

### Advanced mode

The same date/time tokens and optional alarm block are available. **Sync** shares a format across
screen orientations; otherwise, **Portrait** and **Landscape** profiles can differ. Include `%S`
to show seconds; the separate seconds-size control applies only in Checkbox mode.

To enlarge a line, put `%L` at its start. The marker is hidden. Emphasized lines share extra height
and scale to fit; unmarked lines retain space for details. A real newline and `%n` both split lines.
For compatibility, multiline Landscape formats without a `%L` marker emphasize the first non-empty
line by default.
The advanced editor also offers a text-style emoji preference. Joined emoji and flag sequences
are preserved; monochrome rendering depends on the device's fonts. The seven-segment font applies
to time fields, not the alarm label or weekday.

### Charging

The full-screen night clock keeps the screen on while the device is charging in both Checkbox and
Advanced modes.

## Settings, backup, and presets

From the main menu, **Export settings** saves the current settings and preset library to a JSON
file using Android's document picker. **Import settings** restores a file: settings for matching
widgets are updated, while unmatched widget settings are saved as presets. Give widgets unique
names in their configuration so backups can match them after reinstalling or moving to another
installation. Export before uninstalling an APK signed with a different key, because uninstalling
deletes the app's saved settings.

**Settings Manager** copies settings between new-widget defaults, the full-screen clock, individual
widgets, and presets. You can save a selected component as a preset, apply a preset to a compatible
component, and rename or delete presets. Resetting a component saves its previous custom settings as
a preset first.

## Installation and compatibility

SecondsClock works on Android 8.0 (API 26) or later. On some Android 12 devices, help toasts may be
truncated to two lines, making the built-in guidance harder to read.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/uk.co.yahoo.p1rpp.secondsclock/)

## Launcher icon

The editable artwork is `SecondsClock/seconds-clock-icon.svg`. When `rsvg-convert` is available on
`PATH`, Gradle creates a timestamped launcher icon for debug and release APKs. Otherwise, builds
use the checked-in `SecondsClock/src/main/res/drawable/ic_launcher.png`. The 144×144 F-Droid metadata
icon is a static rasterization of the SVG.

## Night clock dimming

SecondsClock can dim the full-screen clock in low ambient light. Display brightness varies by
device: an OEM may apply its own brightness curve or auto-dimming, and Android may report brightness
before or after that adjustment. As a result, setting software brightness to zero may still leave
the display too bright for a dark room. SecondsClock can also reduce clock opacity, down to invisible.

### Dimming controls

- **Lux threshold:** Above this ambient-light level, SecondsClock doesn't dim the clock. Below it,
  the dimming target decreases as ambient light falls. Unless **Use only opacity** is selected,
  screen brightness bottoms out at the minimum-brightness setting; opacity can provide further
  dimming. The threshold's maximum is 255 lux, about a brightly lit room; direct sunlight can be
  much higher.
- **Minimum brightness:** The control ranges up to 100 to give more precision at low brightness.
- **Minimum opacity:** At 255, opacity stays unchanged and screen brightness dims only as far as the
  minimum-brightness setting. Below 255, once brightness reaches that minimum, the clock stays at
  minimum brightness while opacity fades toward the selected minimum. Set it to zero to let the
  clock disappear completely.
- **Use only opacity:** Dims the clock using opacity while setting its window brightness to Android's
  reported system brightness. Depending on the device, this can interact with or override
  auto-dimming. Opacity dimming can save power on OLED screens, but not on backlit displays.

Device-specific brightness processing can cause a visible step when dimming begins. SecondsClock
can't read each device's OEM brightness parameters, so the exact result may need tuning on your
device.

## Permissions

SecondsClock requests Android's `SET_ALARM` permission, but it doesn't set alarms. Some phones
require this permission to open the built-in clock app when you tap a widget. Denying it prevents
that action on affected devices; other widget actions continue to work.

## Licensing

`Slider.java` is licensed under Apache License 2.0 because it derives from an Android source file.
The rest of SecondsClock is licensed under GPL v3 or later.

## Building

Debug and release APKs use the configured signing key; the project doesn't include one. The build
looks for `Keys/keystore.properties` (capital K) in the parent directory of the project. If you store
it elsewhere, update `build.gradle`. The file contains:

```properties
keyAlias=your-user-name
keyPassword=your-key-password
storeFile=/path/to/your/keystore
storePassword=your-keystore-password
```

Debug APKs have a `.debug` application ID suffix but use the same configured key. Android won't
install an APK over an installed version signed with a different key. Uninstalling the existing app
first allows installation, but deletes its saved settings.
