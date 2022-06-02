This app provides a seconds clock for Android.

It can be used as a home screen widget, or as a full screen night clock.

Both the widget and the night clock can be configured to display any combination of the following:-

1. the time in 12-or 24-hour format in accordance with the device's preference, with or without seconds,

2. the day of the week in short or long locale-appropriate format.

3. the date in numeric locale-appropriate format, or any combination of the following:-

    1. the day of the month,
    2. the month name in short or long locale-appropraite format,
    3. the (Gregorian) year number

Displaying the seconds will increase battery drain because the widget has to update itself every second, but it doesn't update when the widget isn't visible. The night clock is intended to be run with the device connected to a charger.

The time, day of the week, and long date will all fit into a 1x1 widget with a fairly small text size, but if you display less information or make the widget bigger, both the widget and the night clock will reformat themselves so as to make more effective use of the available space and use a larger text size.

On some devices it doesn't make as good a job of reformatting the widget in landscape orientation, because Android doesn't require a home screen launcher to notify the widget provider when the screen is rotated between portrait and landscape orientation. If the widget provider does get notified (as it does on my Samsung Galaxy S21) it will reformat the widget.

You can also configure the text colour and the colour and transparency of the widget's background. The default of white text on a transparent background works well unless you have a very light or very complex wallpaper on your home screen.

Displaying the seconds will increase battery drain because the widget has to update itself every second, but it doesn't update when the widget isn't visible. The night clock is intended to be run with the device connected to a charger.

You can also configure what happens when you touch a widget.

You can have multiple widgets which can have different configurations.

For the night clock you can also configure the the relative size of the seconds and the dimming in low ambient light levels.

This is version 2.0.

Currently it has a problem running on some versions of Android 12 (API 31). Its toasts get truncated to two lines. Since I use toasts to display help information, this makes it a bit harder to use, but it should be fairly apparent how the configurators work.

Truncating toasts is documented behaviour for applications which *target* Android 12, but SecondsClock doesn't do so, so IMHO it's a bug in Android, not in SecondsClock. If I find a workaround, I'll apply it, but for the time being I'm not willing to rewrite all my help information.

It works on Android 8.0 API 26 or later.

# Permissions
SecondsClock requests SET_ALARM although it doesn't actually set any alarms. For some strange reason on some phones this permission is required to go to the phone's built-in clock app when you touch the widget. If you deny SecondsClock SET_ALARM permission, the app can't go to the device's clock app if your device requires SET_ALARM permission to do so. Currently Android automatically grants SET_ALARM permission for any app which requests it, so you shouldn't see a problem unless you deliberately remove it.
