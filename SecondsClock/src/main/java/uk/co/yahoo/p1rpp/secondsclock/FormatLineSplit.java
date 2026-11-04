/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

/** Splits an advanced format after its first explicit line break. */
final class FormatLineSplit {
    private static final String ALARM_START = "%<";
    private static final String ALARM_END = "%>";

    private FormatLineSplit() {}

    /** Returns the first line and remainder, or a null remainder if unsplit. */
    static String[] splitAtFirstLine(String format) {
        if (format == null) return new String[] {"", null};

        boolean insideAlarm = false;
        for (int index = 0; index < format.length();) {
            if (format.charAt(index) == '\n') {
                return split(format, index, index + 1, insideAlarm);
            }
            if (format.charAt(index) != '%' || index + 1 >= format.length()) {
                ++index;
                continue;
            }

            char conversion = format.charAt(index + 1);
            if (format.startsWith(ALARM_START, index)) {
                insideAlarm = format.indexOf(ALARM_END,
                        index + ALARM_START.length()) >= 0;
                index += ALARM_START.length();
            } else if (format.startsWith(ALARM_END, index)) {
                insideAlarm = false;
                index += ALARM_END.length();
            } else if (conversion == '%') {
                index += 2;
            } else if (conversion == 'n') {
                return split(format, index, index + 2, insideAlarm);
            } else if (conversion == ':' && index + 2 < format.length()
                    && format.charAt(index + 2) == 'z') {
                index += 3;
            } else {
                index += 2;
            }
        }
        return new String[] {format, null};
    }

    private static String[] split(
            String format, int start, int end, boolean insideAlarm) {
        String first = format.substring(0, start);
        String rest = format.substring(end);
        if (insideAlarm) {
            first += ALARM_END;
            rest = ALARM_START + rest;
        }
        return new String[] {first, rest};
    }
}
