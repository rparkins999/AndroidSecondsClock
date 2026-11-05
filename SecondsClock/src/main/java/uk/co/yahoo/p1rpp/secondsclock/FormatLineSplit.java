/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import java.util.ArrayList;
import java.util.List;

/** Splits a full-screen format into lines and removes %L layout markers. */
final class FormatLineSplit {
    private static final String ALARM_START = "%<";
    private static final String ALARM_END = "%>";
    private static final String EMPHASIS = "%L";

    static final class Line {
        final String format;
        final boolean emphasized;

        Line(String format, boolean emphasized) {
            this.format = format;
            this.emphasized = emphasized;
        }
    }

    private FormatLineSplit() {}

    static List<Line> splitLines(String format) {
        List<Line> lines = new ArrayList<>();
        if (format == null) {
            lines.add(new Line("", false));
            return lines;
        }

        StringBuilder line = new StringBuilder();
        boolean emphasized = false;
        boolean insideAlarm = false;
        for (int index = 0; index < format.length();) {
            if (format.charAt(index) == '\n') {
                addLine(lines, line, emphasized, insideAlarm);
                line.setLength(0);
                if (insideAlarm) line.append(ALARM_START);
                emphasized = false;
                ++index;
                continue;
            }

            if (format.charAt(index) != '%' || index + 1 >= format.length()) {
                line.append(format.charAt(index++));
                continue;
            }

            if (format.startsWith(ALARM_START, index)) {
                if (insideAlarm || format.indexOf(ALARM_END,
                        index + ALARM_START.length()) < 0) {
                    line.append(ALARM_START);
                } else {
                    line.append(ALARM_START);
                    insideAlarm = true;
                }
                index += ALARM_START.length();
                continue;
            }
            if (format.startsWith(ALARM_END, index)) {
                line.append(ALARM_END);
                insideAlarm = false;
                index += ALARM_END.length();
                continue;
            }

            char conversion = format.charAt(index + 1);
            if (conversion == '%') {
                line.append("%%");
                index += 2;
            } else if (conversion == 'n') {
                addLine(lines, line, emphasized, insideAlarm);
                line.setLength(0);
                if (insideAlarm) line.append(ALARM_START);
                emphasized = false;
                index += 2;
            } else if (format.startsWith(EMPHASIS, index)) {
                emphasized = true;
                index += EMPHASIS.length();
            } else if (conversion == ':' && index + 2 < format.length()
                    && format.charAt(index + 2) == 'z') {
                line.append("%:z");
                index += 3;
            } else {
                line.append(format, index, index + 2);
                index += 2;
            }
        }
        addLine(lines, line, emphasized, insideAlarm);
        return lines;
    }

    static String stripEmphasisMarkers(String format) {
        if (format == null || format.isEmpty()) return "";
        StringBuilder result = new StringBuilder(format.length());
        for (int index = 0; index < format.length();) {
            if (format.startsWith("%%", index)) {
                result.append("%%");
                index += 2;
            } else if (format.startsWith(EMPHASIS, index)) {
                index += EMPHASIS.length();
            } else {
                result.append(format.charAt(index++));
            }
        }
        return result.toString();
    }

    private static void addLine(
            List<Line> lines, StringBuilder format,
            boolean emphasized, boolean insideAlarm) {
        String value = format.toString();
        if (insideAlarm) value += ALARM_END;
        lines.add(new Line(value, emphasized));
    }
}
