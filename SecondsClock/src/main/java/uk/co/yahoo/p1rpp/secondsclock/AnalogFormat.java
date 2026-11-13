/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

/** Parsing helpers for advanced-format analog-clock lines. */
final class AnalogFormat {
    static final String CLOCK_TOKEN = "%C";
    static final String LEGACY_CLOCK_TOKEN = "%@";

    static final class Hands {
        final boolean hour;
        final boolean minute;
        final boolean second;

        Hands(boolean hour, boolean minute, boolean second) {
            this.hour = hour;
            this.minute = minute;
            this.second = second;
        }
    }

    private AnalogFormat() {}

    /** Finds an unescaped current or legacy analog-clock marker. */
    static boolean containsClock(String format) {
        if (format == null) return false;
        for (int index = 0; index < format.length();) {
            if (format.startsWith("%%", index)) {
                index += 2;
            } else if (format.startsWith(CLOCK_TOKEN, index)
                    || format.startsWith(LEGACY_CLOCK_TOKEN, index)) {
                return true;
            } else if (format.charAt(index) == '%' && index + 1 < format.length()) {
                index += format.charAt(index + 1) == ':' && index + 2 < format.length()
                        ? 3 : 2;
            } else {
                ++index;
            }
        }
        return false;
    }

    /** Returns this line's hand settings, or null if it has no clock marker. */
    static Hands handsForClockLine(String format) {
        return handsForClockLine(format, null);
    }

    /** Returns hand settings for the clock marker selected by any alarm block. */
    static Hands handsForClockLine(String format, Long nextAlarmTime) {
        String selectedFormat = DateCommandFormat.selectAlarmBranch(
                format, nextAlarmTime);
        if (!containsClock(selectedFormat)) return null;

        boolean hour = true;
        boolean minute = true;
        boolean second = true;

        for (int index = 0; index < selectedFormat.length();) {
            if (selectedFormat.startsWith("%%", index)) {
                index += 2;
            } else if (selectedFormat.charAt(index) == '%'
                    && index + 1 < selectedFormat.length()) {
                char conversion = selectedFormat.charAt(index + 1);
                if (conversion == 'H') hour = false;
                if (conversion == 'M') minute = false;
                if (conversion == 'S') second = false;
                index += conversion == ':' && index + 2 < selectedFormat.length()
                        ? 3 : 2;
            } else {
                ++index;
            }
        }
        return new Hands(hour, minute, second);
    }
}
