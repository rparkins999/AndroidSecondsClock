SecondsClock
------------

This app provides a seconds clock for Android.

This is version 2.4.

It can be used to create home screen widgets and as a full screen night clock.

If you report a problem or request a change, please say whether you are referring to the widget
or the full screen clock. Although some of the setup code is common to both, the mechanism used
to display them is quite different, so the things that are easy or hard to change are also different.

Each widget (there can be more than one), and the night clock, can be configured to display any combination of the following:-

1. the time in 12-or 24-hour format in accordance with the device's preference, with or without seconds,

2. the day of the week in short or long locale-appropriate format.

3. the date in numeric locale-appropriate format, or any combination of the following:-

    1. the day of the month,
    2. the month name in short or long locale-appropriate format,
    3. the (Gregorian) year number

Displaying the seconds in a widget will increase battery drain because the widget has to update itself
every second, but it doesn't update when the widget isn't visible. The night clock is intended
to be run with the device connected to a charger and will keep the screen on if it is.

The time, day of the week, and long date, will all fit into a 1x1 widget with a fairly small text size.
If the widget is given more space or either the widget or the night clock is configured to display less
information, it will reformat itself so as to make effective use of the available space
and use a larger text size if possible.

On some devices tthe widget doesn't make as good a job of reformatting itself in landscape orientation,
because Android doesn't require a home screen launcher to notify the widget provider
when the screen is rotated between portrait and landscape orientation.
If the widget provider does get notified (as it does on my Samsung Galaxy S21)
it will reformat the widget.

You can also configure the text colour of both the widget and the night clock,
and the colour and transparency of the widget's background.
The night clock's background is always black.
The default for the widget of white text on a transparent background works well
unless you have a very light or very complex wallpaper on your home screen.
If you have complex wallpaper you probably want an opaque backgrpund.

You can also configure what happens when you touch a widget.

You can have multiple widgets with different configurations on the same
or different pages of your home screen.

For the night clock you can also configure the the relative size of the seconds
and the dimming in low ambient light levels.

Currently it has a problem running on some versions of Android 12 (API 31).
Its toasts get truncated to two lines. Since I use toasts to display help information,
this makes it a bit harder to use, but it should be fairly apparent how the configurators work.

Truncating toasts is documented behaviour for applications which *target* Android 12,
but SecondsClock doesn't do so, so IMHO it's a bug in Android, not in SecondsClock.
If I find a workaround, I'll apply it,
but for the time being I'm not willing to rewrite all my help information.

It works on Android 8.0 API 26 or later.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/uk.co.yahoo.p1rpp.secondsclock/)

# Dimming
The full screen night clock can dim itself in low light levels. This is a bit complicated
because on at least some devices the actual brightness of the display is not set
directly by the software brightness control. Instead a complex algorithm is used
controlled by some OEM resources which a non-system app like SecondsClock is not able to read.
The situation is further complicated by the auto-dimming option on some devices
which adjusts the display brightness according to the ambient light level.
Reading the system display brightness can give you the value before or after
auto-dimming adjustment depending on the device and the OS version.
Setting a zero screen brightness does not necessarily make the screen dark enough
for a bedside night clock in a dark bedroom. So SecondsClock can use the opacity
of the clock itself to dim it further, right down to zero intensity if you want.

There are four controls available to help you to get it right for your device (and your eyesight).
The lux threshold determines when SecondsClock tries to dim the screen.
The maximum value is 255, which corresponds to a room reasonably well lit by artificial lighting.
Although the actual measured lux value can go up to several thousands in direct sunlight,
in practice you aren't likely to want to dim the clock display unless the ambient light level
is less than in a well-lit room.

If the ambient lux level is above the threshold SecondsClock doesn't dim the screen,
and if the ambient lux level goes below the threshold it smoothly reduces its idea
of what brightness you want (the wanted brightness) from the system display brightness
down to zero when the lux level is zero.
This automatically disables auto-dimming. If you see a step change in screen brightness
as the ambient lux level drops past the threshold, this is either
because your device reports the system display brightness before auto-dimming adjustment,
or possibly because setting the screen brightness in software doesn't give the same effect
as setting the system display brightness manually to the same value using the settings page.
There isn't much that I can do about that because SecondsClock can't find the parameters
of the device's dimming logic.

Two other controls are the minimum brightness and the minimum opacity.
The minimum brightness range only goes up to 100 (rather than 255)
because larger values aren't likely to be useful
and you need plenty of resolution at the low end of the scale.

If the minimum opacity is set to 255, the range of wanted brightness
from zero to the system brightness is mapped linearly onto the range
from the minimum brightness to the system brightness,
and the result used to set the screen brightness.
SecondsClock won't make the screen brightness bigger than the system brightness.
This option works well unless setting the screen brightness to zero
results in a display which is too bright for a dark room with zero lux.

If the minimum opacity is less than 255, it uses a different algorithm.
The range of wanted brightness from the minimum brightness to the system brightness
is mapped linearly onto the range from zero to the system brightness,
and the result used to set the screen brightness.
If the wanted brightness is less than the minimum brightness,
the screen brightness is zet to zero and the range of wanted brightness
from zero to the minimum brightness is mapped onto the range of opacity values
from the minimum opacity to 255,
and the result is used to set the opacity of the clock display.
This will allow the display to be dimmed right down to invisible (zero opacity).
The opacity of the status bar isn't modified,
but the status bar is quite small and doesn't emit much light.

There is also a check box to only use the opacity and not interfere withe the screen brightne at all.
This works well with an OLED display, but doesn't save any power with a backlit display.
I haven't found a way of getting Android to tell me which kind of display it has.

# Permissions
SecondsClock requests SET_ALARM permission although it doesn't actually set any alarms.
For some strange reason on some phones this permission is required
to go to the phone's built-in clock app when you touch the widget.
If you deny SecondsClock SET_ALARM permission,
the app can't go to the device's clock app if your device requires SET_ALARM permission to do so.
Currently Android automatically grants SET_ALARM permission for any app which requests it,
so you shouldn't see a problem unless you explicitly deny SET_ALARM permission.

# Licensing
One file (Slider.java) is licensed under the Apache license 2.0 because it is derived from an Android source file which uses that licence. Everything else was written by me and is licensed under GPL V3 or later. If you're puzzled by the apparently inconsistent spelling of "licen{s|c}e", in the UK English spelling the verb is license, but the noun is licence. There is also a noun license in UK English, but it means something different.

# Building
Android now requires all applications to be signed. If you build your own version,
you need to sign it - I'm not giving you my signing key.
`build.gradle` expects to find a `keystore.properties` file in the
subdirectory `keys` of the parent directory of the project's top level directory
(the one you cloned into). If you keep yours somewhere else, edit `build.gradle`.
The `keystore.properties` file should look something like this:-

`keyAlias=`<i>your user name</i><br>
`keyPassword=`<i>paasword for your key</i><br>
`storeFile=`<i>full pathname of key file</i><br>
`storePassword=`<i>password for your keystore (may be different)</i>

You should also be aware that if you debug your own version with Androis Studio, it will sign it with a
debug key, and Android will not let you install a version of an app signed with a different key unless
you delete the old version first. Unfortunatley this throws away all the stored peferences.
