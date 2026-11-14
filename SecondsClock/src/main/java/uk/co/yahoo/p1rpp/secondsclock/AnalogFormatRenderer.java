/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converts advanced-format lines into text rows and optional analog rows. */
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
        final AnalogFormat.Hands hands;

        Line(List<Part> parts, boolean emphasized, AnalogFormat.Hands hands) {
            this.parts = parts;
            this.emphasized = emphasized;
            this.hands = hands;
        }
    }

    static final class Result {
        final List<Line> lines;
        final boolean hasAnalog;
        final boolean hasSecondHand;

        Result(List<Line> lines, boolean hasAnalog, boolean hasSecondHand) {
            this.lines = lines;
            this.hasAnalog = hasAnalog;
            this.hasSecondHand = hasSecondHand;
        }
    }

    private AnalogFormatRenderer() {}

    static Result render(String format, Long nextAlarmTime,
                         boolean emphasizeMarkers) {
        List<FormatLineSplit.Line> formatLines = FormatLineSplit.splitLines(
                format, emphasizeMarkers);
        return renderLines(formatLines, nextAlarmTime);
    }

    static Result renderLines(List<FormatLineSplit.Line> formatLines,
                              Long nextAlarmTime) {
        List<Line> lines = new ArrayList<>();
        boolean hasAnalog = false;
        boolean hasSecondHand = false;
        for (FormatLineSplit.Line formatLine : formatLines) {
            AnalogFormat.Hands hands =
                    AnalogFormat.handsForClockLine(
                            formatLine.format, nextAlarmTime);
            if (hands != null) {
                lines.add(new Line(Collections.singletonList(Part.analog()),
                        formatLine.emphasized, hands));
                hasAnalog = true;
                hasSecondHand |= hands.second;
                continue;
            }

            DateCommandFormat.FormattedPattern converted =
                    DateCommandFormat.toTextClockPatternWithTimeRanges(
                            formatLine.format, nextAlarmTime);
            if (converted.pattern.isEmpty() && formatLine.format.contains("%<")) {
                continue;
            }
            List<Part> parts = new ArrayList<>();
            if (!converted.pattern.isEmpty()) {
                parts.add(Part.text(converted.pattern, converted.timeRanges));
            }
            lines.add(new Line(parts, formatLine.emphasized, null));
        }
        return new Result(lines, hasAnalog, hasSecondHand);
    }
}
