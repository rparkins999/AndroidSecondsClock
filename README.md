# SecondsClock

SecondsClock is an Android clock that runs as a home-screen widget or a full-screen night clock.
You can add multiple widgets and configure each one independently.

This is version 2.6.

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
The layout adapts to the widget's size.

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

### Optional alarm text

Use `%<` and `%>` to mark an alarm block. Inside it, `%|` separates the text shown when an alarm
is set from an optional fallback shown when none is set:

```text
%<Alarm: %H:%M%|%nNO ALARM%>
```

When Android reports a next alarm-clock alarm from any app, the text before `%|` appears and its
tokens use the alarm time. Without an alarm, the text after `%|` appears and its tokens use the
current time. Without `%|`, the block is hidden when no alarm is set; the fallback can also be
empty. Android does not expose the alarm's label.

The checkbox **Show next alarm (any alarm app)** option displays the next alarm Android reports,
not necessarily one from the default clock app.

## Full-screen night clock

### Checkbox mode

Choose the time, seconds size, weekday, date, and next-alarm options. The clock arranges the
selected information automatically. Its checkbox-mode alarm is shown as `ALM: Mon, 3:30`.

### Advanced mode

The same date/time tokens and optional alarm block are available. **Sync** shares a format across
screen orientations; otherwise, **Portrait** and **Landscape** profiles can differ. Include `%S`
to show seconds; the separate seconds-size control applies only in Checkbox mode.

To enlarge a line, put `%L` at its start. The marker is hidden. Emphasized lines share extra height
and scale to fit; unmarked lines retain space for details. A real newline and `%n` both split lines.
The advanced editor also offers a text-style emoji preference; monochrome rendering depends on the
device's fonts. The seven-segment font applies to time fields, not the alarm label or weekday.

## Other settings

You can change the widget's text colour, background colour, and background opacity. The full-screen
clock's background is always black, but its text colour is configurable. Widget touch actions can
open the system clock, SecondsClock, its settings, or the full-screen clock. You can place multiple
widgets with different settings on the same or different home-screen pages.

### Seconds and battery

A widget showing seconds updates every second while it is visible, which uses more battery. It
doesn't update while hidden. The full-screen night clock keeps the screen on, so it is intended to
be used while the device is charging. Its seconds size can be adjusted separately.

### Widget orientation

Some launchers don't notify widgets when the screen rotates. On those devices, a widget's layout may
not adapt to landscape until the launcher updates it or the widget is resized.

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
  the requested brightness falls toward zero as the measured light reaches zero. The threshold's
  maximum is 255 lux, about a brightly lit room; direct sunlight can be much higher.
- **Minimum brightness:** The control ranges up to 100 to give more precision at low brightness.
- **Minimum opacity:** At 255, the clock stays fully opaque and dimming uses screen brightness down
  to the minimum brightness. Below 255, screen brightness can reach zero and opacity fades from
  fully visible to the selected minimum. Set it to zero to let the clock disappear completely.
- **Use only opacity:** Leaves system screen brightness alone and dims the clock by opacity. This
  can save power on OLED screens, but not on backlit displays. Android doesn't report which display
  type is in use.

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

Release APKs must be signed with your own key; the project doesn't include a signing key. The build
looks for `keys/keystore.properties` in the parent directory of the project. If you store it
elsewhere, update `build.gradle`. It contains:

```properties
keyAlias=your-user-name
keyPassword=your-key-password
storeFile=/path/to/your/keystore
storePassword=your-keystore-password
```

Debug builds use a debug key. Android won't install an APK over an installed version signed with a
different key. Uninstalling the existing app first allows installation, but deletes its saved
settings.
