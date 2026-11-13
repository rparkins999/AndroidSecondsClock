/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

/** Parsing helpers for the advanced-format analog-clock placeholder. */
final class AnalogFormat {
    static final String TOKEN = "%@";
    static final char PLACEHOLDER = '\u0001';

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

    /** `%@` is not an existing conversion; doubled percent signs stay literal. */
    static boolean containsPlaceholder(String format) {
        if (format == null) return false;
        for (int index = 0; index < format.length();) {
            if (format.startsWith("%%", index)) {
                index += 2;
            } else if (format.startsWith(TOKEN, index)) {
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

    /**
     * With no hand selectors, `%@` shows the usual three hands. Once any of
     * `%H`, `%M`, or `%S` is present, their presence selects the visible hands.
     */
    static Hands handsFor(String format) {
        boolean hour = false;
        boolean minute = false;
        boolean second = false;
        boolean hasSelector = false;
        if (format != null) {
            for (int index = 0; index < format.length();) {
                if (format.startsWith("%%", index)) {
                    index += 2;
                } else if (format.charAt(index) == '%' && index + 1 < format.length()) {
                    char conversion = format.charAt(index + 1);
                    if (conversion == 'H' || conversion == 'M' || conversion == 'S') {
                        hasSelector = true;
                        if (conversion == 'H') hour = true;
                        if (conversion == 'M') minute = true;
                        if (conversion == 'S') second = true;
                    }
                    index += conversion == ':' && index + 2 < format.length() ? 3 : 2;
                } else {
                    ++index;
                }
            }
        }
        return hasSelector ? new Hands(hour, minute, second)
                : new Hands(true, true, true);
    }

    /** Removes `%H`, `%M`, and `%S` selectors without changing escaped text. */
    static String removeHandSelectors(String format) {
        if (format == null || !containsPlaceholder(format)) return format;
        StringBuilder result = new StringBuilder(format.length());
        for (int index = 0; index < format.length();) {
            if (format.startsWith("%%", index)) {
                result.append("%%");
                index += 2;
            } else if (format.charAt(index) == '%' && index + 1 < format.length()
                    && isHandSelector(format.charAt(index + 1))) {
                index += 2;
            } else {
                result.append(format.charAt(index++));
            }
        }
        return result.toString();
    }

    private static boolean isHandSelector(char conversion) {
        return conversion == 'H' || conversion == 'M' || conversion == 'S';
    }
}
