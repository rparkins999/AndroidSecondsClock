# SecondsClock

A customizable Android clock for home-screen widgets and a full-screen night clock. Choose digital
or analog displays, configure date/time layouts and alarms, and adjust the full-screen clock for
low-light use. Each widget can have its own layout and settings.

This is version 2.7.1.

## Choose a setup

- **Checkbox mode** automatically formats the selected date and time for your device's locale.
- **Advanced mode** lets you type text and insert date/time tokens. Tokens are case-sensitive; press
  Enter or use `%n` for a new line. Text scales to fit, but line breaks stay fixed, so resizing a
  widget may need adjustment.

## Home-screen widgets

### Checkbox mode

Choose the time, seconds, weekday, next alarm, and either a numeric date or separate day, month,
and year. Time follows the device's 12- or 24-hour setting. Digital is the default; **Analog clock**
replaces it with a 12-numbered dial, keeps other selected details below it, and preserves your digital
settings. **Show seconds** controls the second hand. **Show next alarm (any alarm app)** uses the next
alarm Android reports, not necessarily one from its default clock app. Dial markings follow the
widget text colour; Android 16 and later also tint the hands. On Android 17 the platform synchronizes
the second hand when the widget reappears; some earlier launchers may resume it where it stopped.
The widget schedules no repeating app alarms.

### Advanced mode

**Sync** shares one format across widget shapes. Otherwise, **Horizontal** and **Vertical** profiles
are selected from the widget's dimensions. New profiles start from Sync.

Common tokens:

| Token | Displays |
| --- | --- |
| `%H`, `%I` | 24- or 12-hour clock hour |
| `%M`, `%S`, `%p` | Minute, second, AM/PM |
| `%a`, `%A` | Short or full weekday |
| `%b`, `%B` | Short or full month name |
| `%d`, `%e` | Zero-padded or unpadded day |
| `%m`, `%Y` | Month number, year |
| `%F`, `%x`, `%X`, `%R` | ISO date, localized date or time, 24-hour `HH:mm` |
| `%n`, `%%` | New line, literal percent sign |
| `%C` | Full-row analog clock |
| `%Q` | Give this advanced-format line a seeded palette color |

Put `%C` on its own line for a full-row analog dial; that line renders no text. On it, `%H`, `%M`,
and `%S` suppress the matching hands instead of printing time fields. Elsewhere they print normally.
A real newline or `%n` ends the clock line.

**Global Settings → Advanced line colors** can color every advanced-format line automatically, or
use `%Q` to color only selected lines. The option supports standard, red-green color-vision,
blue-yellow color-vision, and monochrome palettes. Colors are deterministic for a given editable
seed; copy or regenerate it to share or change the palette assignment. Colors are designed for black
backgrounds, so set the clock/widget background to black for the clearest result. Android 16 and
later tint analog widget hands to match their line; on Android 8–15, the platform-provided hands
remain white, while the dial face still uses the line color. `%R` remains the existing 24-hour time
token; it is not used for color.

To show optional alarm text, wrap it in `%<` and `%>`. Use `%|` to add fallback text for when no
alarm is set:

```text
%<Alarm: %H:%M%|%nNO ALARM%>
```

When set, alarm tokens use the alarm time; fallback tokens use the current time. Without `%|`, the
block is hidden when no alarm is set. Android doesn't provide the alarm's label.
By default, reported alarm times are always shown. In **Global Settings**, choose **Always show** or
ignore past alarm times after 1, 6, 12, 24, or 48 hours, or 7 days.

### Appearance and actions

Set text and background colours and background opacity. Widget actions can open the system clock,
SecondsClock, its settings, or the full-screen clock. If a widget action prevents configuration, open
SecondsClock and choose **Configure existing widget**; the next widget you tap opens its settings.

System-clock apps vary by device. SecondsClock tries automatic detection first, then lets you save an
installed app as an override. A saved app is tried before detection. Choose **Not now** to open the
full-screen clock, or **Don't ask me again** to suppress later prompts. **Reset** in **Global
Settings** clears the override and prompt preference, restoring automatic detection and prompts.
Widgets showing seconds update every second while visible and use more battery; they don't update
while hidden. Some launchers may not refresh a widget's layout immediately after screen rotation.

## Full-screen night clock

The background is black; text colour can be changed in Light or Dark appearance. In Checkbox mode,
choose **Analog clock** instead of digital; selected date and alarm details remain below the dial.
Digital remains the default, and the seconds-size control determines whether the second hand is shown.
Checkbox alarm text looks like `ALM: Mon, 3:30`.

Advanced mode uses the same tokens and alarm block as widgets, with **Portrait** and **Landscape**
profiles or **Sync**. Put `%C` on its own line for a dial. Use `%L` at the start of a line to enlarge
it; the marker is hidden. Older multiline Landscape formats without `%L` still emphasize the first
non-empty line. The text-style emoji option preserves joined sequences; appearance depends on device
fonts. Seven-segment styling applies to time fields.

The clock keeps the screen on while charging, in both Checkbox and Advanced modes.

## Settings, backup, and presets

The main menu groups widget actions and settings. **Global Settings** controls defaults for new
widgets, advanced line colors, and the system-clock app override. **Settings Manager** copies
settings between defaults, the full-screen clock, widgets, and presets. You can save
components as presets, apply presets to
compatible components, and rename or delete presets; resetting a component first saves its current
settings as a preset.

Export and import use Android's document picker and a JSON backup. Backups include new-widget
defaults, full-screen and widget settings, and presets. Import restores them, matches widgets by name,
and saves unmatched widget settings as presets; existing settings are preserved as presets, and
duplicate presets are reported. Give widgets unique names on both installations for reliable matching.
Android won't install an APK signed with a different key over the installed app. Switching keys may
require uninstalling, which deletes saved settings, so export first.

## Night-clock dimming

The ambient-light threshold sets when dimming begins. By default, screen brightness dims to
**Minimum brightness**, then opacity fades the clock toward **Minimum opacity** (zero can hide it).
**Use only opacity** keeps brightness at Android's reported level and dims the text instead; it can
interact with device auto-dimming. OEM brightness curves may leave the screen brighter than expected
or cause a visible step, so settings may need adjustment. Opacity dimming can save power on OLED
screens, but not on backlit displays.

## Install

SecondsClock requires Android 8.0 (API 26) or later.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/uk.co.yahoo.p1rpp.secondsclock/)

The parallel-install build uses package ID `io.github.marvin1099.secondsclock`, so it can sit
alongside the original `uk.co.yahoo.p1rpp.secondsclock` app. It keeps its own settings and widgets;
see the [release guide](docs/RELEASING.md#parallel-install-package-id) for how to select it.

SecondsClock requests Android's `SET_ALARM` permission but doesn't set alarms. Some phones require
it to open their built-in clock app; denying it may disable that widget action. Other widget actions
continue to work.

## Licensing

`Slider.java` is licensed under Apache License 2.0. The rest of SecondsClock is licensed under GPL v3
or later.

## Building

See [the release guide](docs/RELEASING.md) for build, signing, and release instructions.
