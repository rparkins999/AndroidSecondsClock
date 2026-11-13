/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import java.util.ArrayList;
import java.util.List;

/** Converts advanced formats containing `%@` into text and analog render parts. */
final class AnalogFormatRenderer {
    static final class Part {
        final boolean analog;
        final String pattern;
        final List<DateCommandFormat.PatternRange> timeRanges;

        private Part(boolean analog, String pattern,
                     List<DateCommandFormat.PatternRange> timeRanges) {
            this.analog = analog;
            this.pattern = pattern;
            this.timeRanges = timeRanges;
        }

        static Part analog() {
            return new Part(true, "", new ArrayList<DateCommandFormat.PatternRange>());
        }

        static Part text(String pattern,
                         List<DateCommandFormat.PatternRange> timeRanges) {
            return new Part(false, pattern, timeRanges);
        }
    }

    static final class Line {
        final List<Part> parts;
        final boolean emphasized;

        Line(List<Part> parts, boolean emphasized) {
            this.parts = parts;
            this.emphasized = emphasized;
        }
    }

    static final class Result {
        final AnalogFormat.Hands hands;
        final List<Line> lines;
        final boolean hasAnalog;

        Result(AnalogFormat.Hands hands, List<Line> lines, boolean hasAnalog) {
            this.hands = hands;
            this.lines = lines;
            this.hasAnalog = hasAnalog;
        }
    }

    private AnalogFormatRenderer() {}

    static Result render(String format, Long nextAlarmTime,
                         boolean emphasizeMarkers) {
        boolean hasPlaceholder = AnalogFormat.containsPlaceholder(format);
        AnalogFormat.Hands hands = AnalogFormat.handsFor(format);
        String prepared = AnalogFormat.removeHandSelectors(format);
        List<FormatLineSplit.Line> formatLines = FormatLineSplit.splitLines(
                prepared, emphasizeMarkers);
        return renderLines(formatLines, hasPlaceholder, hands, nextAlarmTime);
    }

    static Result renderLines(List<FormatLineSplit.Line> formatLines,
                              boolean hasPlaceholder, AnalogFormat.Hands hands,
                              Long nextAlarmTime) {
        List<Line> lines = new ArrayList<>();
        boolean hasAnalog = false;
        for (FormatLineSplit.Line formatLine : formatLines) {
            DateCommandFormat.FormattedPattern converted =
                    DateCommandFormat.toTextClockPatternWithTimeRanges(
                            formatLine.format, nextAlarmTime);
            if (converted.pattern.isEmpty() && formatLine.format.contains("%<")) {
                continue;
            }
            List<Part> parts = splitPattern(converted);
            for (Part part : parts) hasAnalog |= part.analog;
            lines.add(new Line(parts, formatLine.emphasized));
        }
        return new Result(hands, lines, hasPlaceholder && hasAnalog);
    }

    private static List<Part> splitPattern(
            DateCommandFormat.FormattedPattern converted) {
        List<Part> result = new ArrayList<>();
        String pattern = converted.pattern;
        int start = 0;
        for (int index = 0; index < pattern.length(); ++index) {
            if (pattern.charAt(index) != AnalogFormat.PLACEHOLDER) continue;
            addTextPart(result, converted, start, index);
            result.add(Part.analog());
            start = index + 1;
        }
        addTextPart(result, converted, start, pattern.length());
        return result;
    }

    private static void addTextPart(List<Part> result,
                                    DateCommandFormat.FormattedPattern converted,
                                    int start, int end) {
        if (start >= end) return;
        List<DateCommandFormat.PatternRange> ranges = new ArrayList<>();
        for (DateCommandFormat.PatternRange range : converted.timeRanges) {
            int overlapStart = Math.max(start, range.start);
            int overlapEnd = Math.min(end, range.end);
            if (overlapStart < overlapEnd) {
                ranges.add(new DateCommandFormat.PatternRange(
                        overlapStart - start, overlapEnd - start));
            }
        }
        result.add(Part.text(converted.pattern.substring(start, end), ranges));
    }
}
