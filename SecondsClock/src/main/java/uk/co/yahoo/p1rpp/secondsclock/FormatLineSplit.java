/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import java.util.ArrayList;
import java.util.List;

/** Splits a full-screen format into lines and removes %L layout markers. */
final class FormatLineSplit {
    private static final String ALARM_START = "%<";
    private static final String ALARM_SEPARATOR = "%|";
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
        return splitLines(format, true);
    }

    /** Splits format lines, optionally retaining the full-screen-only `%L` token. */
    static List<Line> splitLines(String format, boolean removeEmphasisMarkers) {
        List<Line> lines = new ArrayList<>();
        if (format == null) {
            lines.add(new Line("", false));
            return lines;
        }

        StringBuilder line = new StringBuilder();
        boolean emphasized = false;
        boolean insideAlarm = false;
        boolean insideAlarmFallback = false;
        for (int index = 0; index < format.length();) {
            if (format.charAt(index) == '\n') {
                addLine(lines, line, emphasized, insideAlarm);
                line.setLength(0);
                if (insideAlarm) {
                    line.append(ALARM_START);
                    if (insideAlarmFallback) line.append(ALARM_SEPARATOR);
                }
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
                    insideAlarmFallback = false;
                }
                index += ALARM_START.length();
                continue;
            }
            if (format.startsWith(ALARM_END, index)) {
                line.append(ALARM_END);
                insideAlarm = false;
                insideAlarmFallback = false;
                index += ALARM_END.length();
                continue;
            }

            if (insideAlarm && format.startsWith(ALARM_SEPARATOR, index)) {
                line.append(ALARM_SEPARATOR);
                insideAlarmFallback = true;
                index += ALARM_SEPARATOR.length();
                continue;
            }

            char conversion = format.charAt(index + 1);
            if (conversion == '%') {
                line.append("%%");
                index += 2;
            } else if (conversion == 'n') {
                addLine(lines, line, emphasized, insideAlarm);
                line.setLength(0);
                if (insideAlarm) {
                    line.append(ALARM_START);
                    if (insideAlarmFallback) line.append(ALARM_SEPARATOR);
                }
                emphasized = false;
                index += 2;
            } else if (format.startsWith(EMPHASIS, index)) {
                if (removeEmphasisMarkers) {
                    emphasized = true;
                } else {
                    line.append(EMPHASIS);
                }
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

    /** Keeps the old landscape first-line emphasis for formats without %L. */
    static List<Line> applyLegacyFirstLineEmphasis(
            List<Line> lines, boolean enabled) {
        if (!enabled || lines.size() < 2) return lines;
        for (Line line : lines) {
            if (line.emphasized) return lines;
        }

        boolean hasFollowingContent = false;
        for (int index = 1; index < lines.size(); ++index) {
            if (!lines.get(index).format.isEmpty()) {
                hasFollowingContent = true;
                break;
            }
        }
        if (!hasFollowingContent) return lines;

        List<Line> result = new ArrayList<>(lines);
        for (int index = 0; index < result.size(); ++index) {
            Line line = result.get(index);
            if (!line.format.isEmpty()) {
                result.set(index, new Line(line.format, true));
                break;
            }
        }
        return result;
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
