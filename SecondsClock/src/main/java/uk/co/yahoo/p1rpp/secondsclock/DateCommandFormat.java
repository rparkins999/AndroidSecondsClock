/*
 * Copyright © 2026. Released under GPL V3 or later.
 *
 * Translates common GNU date-style conversions to Android TextClock patterns.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import java.text.DateFormat;
import java.text.AttributedCharacterIterator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class DateCommandFormat {
    private static final String ALARM_START = "%<";
    private static final String ALARM_SEPARATOR = "%|";
    private static final String ALARM_END = "%>";

    private DateCommandFormat() {}

    static String toTextClockPattern(String format, Long nextAlarmTime) {
        return toTextClockPattern(format, nextAlarmTime, false);
    }

    static String toTextClockPattern(
            String format, Long nextAlarmTime, boolean preferTextStyleEmoji) {
        if (format == null || format.isEmpty()) return "";
        if (preferTextStyleEmoji) {
            format = TextStyleEmoji.preferTextPresentation(format);
        }
        return toTextClockPatternWithTimeRanges(format, nextAlarmTime).pattern;
    }

    static final class PatternRange {
        final int start;
        final int end;

        PatternRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    static final class FormattedPattern {
        final String pattern;
        final List<PatternRange> timeRanges;

        FormattedPattern(String pattern, List<PatternRange> timeRanges) {
            this.pattern = pattern;
            this.timeRanges = timeRanges;
        }
    }

    /** Builds the TextClock pattern and identifies only the parts displaying time. */
    static FormattedPattern toTextClockPatternWithTimeRanges(
            String format, Long nextAlarmTime) {
        if (format == null || format.isEmpty()) {
            return new FormattedPattern("", new ArrayList<PatternRange>());
        }

        StringBuilder result = new StringBuilder();
        List<PatternRange> timeRanges = new ArrayList<>();
        Date now = new Date();
        int index = 0;
        while (index < format.length()) {
            if (format.startsWith(ALARM_START, index)) {
                int contentStart = index + ALARM_START.length();
                int end = format.indexOf(ALARM_END, contentStart);
                if (end >= 0) {
                    int separator = alarmSeparatorIndex(format, contentStart, end);
                    int selectedStart;
                    int selectedEnd;
                    Date referenceDate;
                    if (nextAlarmTime != null) {
                        selectedStart = contentStart;
                        selectedEnd = separator < 0 ? end : separator;
                        referenceDate = new Date(nextAlarmTime);
                    } else if (separator >= 0) {
                        selectedStart = separator + ALARM_SEPARATOR.length();
                        selectedEnd = end;
                        referenceDate = now;
                    } else {
                        selectedStart = end;
                        selectedEnd = end;
                        referenceDate = now;
                    }
                    if (selectedStart < selectedEnd) {
                        FormattedPattern selectedFormat = toDatePatternWithTimeRanges(
                                format.substring(selectedStart, selectedEnd),
                                referenceDate);
                        if (nextAlarmTime == null) {
                            appendPattern(result, timeRanges, selectedFormat);
                        } else {
                            AttributedCharacterIterator alarmText =
                                    new SimpleDateFormat(selectedFormat.pattern,
                                            Locale.getDefault())
                                            .formatToCharacterIterator(referenceDate);
                            int[] outputPositions = appendLiteralWithMapping(
                                    result, iteratorText(alarmText));
                            for (PatternRange range : attributedTimeRanges(alarmText)) {
                                timeRanges.add(new PatternRange(
                                        outputPositions[range.start],
                                        outputPositions[range.end]));
                            }
                        }
                    }
                    index = end + ALARM_END.length();
                    continue;
                }
                // An unclosed tag is treated as ordinary text so a typo does
                // not make the rest of the widget disappear.
                appendLiteral(result, ALARM_START);
                index = contentStart;
                continue;
            }

            if (format.startsWith(ALARM_END, index)) {
                appendLiteral(result, ALARM_END);
                index += ALARM_END.length();
                continue;
            }

            int nextTag = nextTagIndex(format, index);
            int segmentEnd = nextTag < 0 ? format.length() : nextTag;
            appendPattern(result, timeRanges, toDatePatternWithTimeRanges(
                    format.substring(index, segmentEnd), now));
            index = segmentEnd;
        }
        String pattern = result.toString();
        return new FormattedPattern(
                pattern, mergeTimeRanges(pattern, timeRanges, true));
    }

    private static int alarmSeparatorIndex(String format, int start, int end) {
        for (int index = start; index < end;) {
            if (format.startsWith("%%", index)) {
                index += 2;
            } else if (format.startsWith(ALARM_SEPARATOR, index)) {
                return index;
            } else {
                ++index;
            }
        }
        return -1;
    }

    private static int nextTagIndex(String format, int fromIndex) {
        int result = -1;
        String[] tags = {ALARM_START, ALARM_END};
        for (String tag : tags) {
            int found = format.indexOf(tag, fromIndex);
            if (found >= 0 && (result < 0 || found < result)) result = found;
        }
        return result;
    }

    private static FormattedPattern toDatePatternWithTimeRanges(
            String format, Date referenceDate) {
        StringBuilder pattern = new StringBuilder();
        StringBuilder literal = new StringBuilder();
        List<PatternRange> timeRanges = new ArrayList<>();
        for (int index = 0; index < format.length();) {
            char current = format.charAt(index);
            if (current != '%') {
                literal.append(current);
                ++index;
                continue;
            }

            if (index + 1 >= format.length()) {
                literal.append('%');
                ++index;
                continue;
            }

            char conversion = format.charAt(index + 1);
            int end = index + 2;
            String patternConversion;
            if (conversion == ':' && end < format.length()
                    && format.charAt(end) == 'z') {
                patternConversion = "XXX";
                ++end;
            } else {
                patternConversion = conversionPattern(
                        conversion, referenceDate);
            }

            if (patternConversion == null) {
                literal.append(format, index, end);
            } else {
                flushLiteral(pattern, literal);
                int conversionStart = pattern.length();
                if (patternConversion.startsWith("\u0000")) {
                    appendLiteral(pattern, patternConversion.substring(1));
                } else {
                    pattern.append(patternConversion);
                }
                if (isTimeConversion(conversion)) {
                    for (PatternRange range : timePatternRanges(patternConversion)) {
                        timeRanges.add(new PatternRange(
                                conversionStart + range.start,
                                conversionStart + range.end));
                    }
                }
            }
            index = end;
        }
        flushLiteral(pattern, literal);
        String result = pattern.toString();
        return new FormattedPattern(
                result, mergeTimeRanges(result, timeRanges, true));
    }

    private static boolean isTimeConversion(char conversion) {
        switch (conversion) {
            case 'H':
            case 'I':
            case 'k':
            case 'l':
            case 'M':
            case 'p':
            case 'r':
            case 'R':
            case 'S':
            case 'T':
            case 'X':
            case 'c':
                return true;
            default:
                return false;
        }
    }

    private static List<PatternRange> timePatternRanges(String pattern) {
        List<PatternRange> ranges = new ArrayList<>();
        boolean quoted = false;
        for (int index = 0; index < pattern.length();) {
            char current = pattern.charAt(index);
            if (current == '\'') {
                if (index + 1 < pattern.length()
                        && pattern.charAt(index + 1) == '\'') {
                    index += 2;
                } else {
                    quoted = !quoted;
                    ++index;
                }
                continue;
            }
            if (!quoted && isTimePatternField(current)) {
                ranges.add(new PatternRange(index, index + 1));
            }
            ++index;
        }
        return mergeTimeRanges(pattern, ranges, true);
    }

    private static boolean isTimePatternField(char value) {
        return value == 'H' || value == 'h' || value == 'K' || value == 'k'
                || value == 'm' || value == 's' || value == 'S' || value == 'a';
    }

    private static void appendPattern(
            StringBuilder output, List<PatternRange> outputTimeRanges,
            FormattedPattern addition) {
        int offset = output.length();
        int rangeOffset = 0;
        if (output.length() > 0 && addition.pattern.length() > 0
                && output.charAt(output.length() - 1) == '\''
                && addition.pattern.charAt(0) == '\'') {
            // Join adjacent quoted literals; otherwise SimpleDateFormat treats
            // the two quote delimiters as a literal apostrophe.
            output.setLength(output.length() - 1);
            output.append(addition.pattern, 1, addition.pattern.length());
            rangeOffset = -2;
        } else {
            output.append(addition.pattern);
        }
        for (PatternRange range : addition.timeRanges) {
            outputTimeRanges.add(new PatternRange(
                    offset + range.start + rangeOffset,
                    offset + range.end + rangeOffset));
        }
    }

    private static List<PatternRange> attributedTimeRanges(
            AttributedCharacterIterator iterator) {
        List<PatternRange> ranges = new ArrayList<>();
        int begin = iterator.getBeginIndex();
        int end = iterator.getEndIndex();
        int runStart = -1;
        for (int index = begin; index < end; ++index) {
            iterator.setIndex(index);
            boolean isTime = hasTimeField(iterator.getAttributes().keySet());
            if (isTime && runStart < 0) runStart = index - begin;
            if (!isTime && runStart >= 0) {
                ranges.add(new PatternRange(runStart, index - begin));
                runStart = -1;
            }
        }
        if (runStart >= 0) ranges.add(new PatternRange(runStart, end - begin));
        String text = iteratorText(iterator);
        return mergeTimeRanges(text, ranges, false);
    }

    private static boolean hasTimeField(
            java.util.Set<AttributedCharacterIterator.Attribute> attributes) {
        return attributes.contains(DateFormat.Field.HOUR_OF_DAY0)
                || attributes.contains(DateFormat.Field.HOUR_OF_DAY1)
                || attributes.contains(DateFormat.Field.HOUR0)
                || attributes.contains(DateFormat.Field.HOUR1)
                || attributes.contains(DateFormat.Field.MINUTE)
                || attributes.contains(DateFormat.Field.SECOND)
                || attributes.contains(DateFormat.Field.MILLISECOND)
                || attributes.contains(DateFormat.Field.AM_PM);
    }

    private static String iteratorText(AttributedCharacterIterator iterator) {
        StringBuilder result = new StringBuilder(
                iterator.getEndIndex() - iterator.getBeginIndex());
        for (char value = iterator.first(); value != AttributedCharacterIterator.DONE;
             value = iterator.next()) {
            result.append(value);
        }
        return result.toString();
    }

    private static List<PatternRange> mergeTimeRanges(
            String text, List<PatternRange> ranges, boolean pattern) {
        if (ranges.isEmpty()) return ranges;
        List<PatternRange> sorted = new ArrayList<>(ranges);
        Collections.sort(sorted, (left, right) -> left.start - right.start);
        List<PatternRange> merged = new ArrayList<>();
        PatternRange current = sorted.get(0);
        for (int index = 1; index < sorted.size(); ++index) {
            PatternRange next = sorted.get(index);
            if (next.start <= current.end
                    || isSeparatorGap(text, current.end, next.start, pattern)) {
                current = new PatternRange(current.start,
                        Math.max(current.end, next.end));
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private static boolean isSeparatorGap(
            String text, int start, int end, boolean pattern) {
        if (start >= end) return true;
        boolean quoted = false;
        for (int index = start; index < end; ++index) {
            char value = text.charAt(index);
            if (pattern && value == '\'') {
                if (index + 1 < end && text.charAt(index + 1) == '\'') {
                    ++index;
                } else {
                    quoted = !quoted;
                }
                continue;
            }
            if (Character.isLetterOrDigit(value)) return false;
        }
        return !quoted;
    }

    /**
     * Returns a SimpleDateFormat pattern. A leading NUL marks output that must
     * be inserted literally (for example, GNU date's %n newline).
     */
    private static String conversionPattern(char conversion, Date referenceDate) {
        switch (conversion) {
            case '%': return "\u0000%";
            case 'a': return "EEE";
            case 'A': return "EEEE";
            case 'b':
            case 'h': return "MMM";
            case 'B': return "MMMM";
            case 'c': return localizedPattern(DateFormat.getDateTimeInstance(
                    DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.getDefault()));
            case 'C':
                String year = new SimpleDateFormat("yyyy", Locale.getDefault())
                        .format(referenceDate);
                return "\u0000" + year.substring(0, year.length() - 2);
            case 'd': return "dd";
            case 'D': return "MM/dd/yy";
            case 'e': return "d";
            case 'F': return "yyyy-MM-dd";
            case 'H': return "HH";
            case 'I': return "hh";
            case 'j': return "DDD";
            case 'k': return "H";
            case 'l': return "h";
            case 'm': return "MM";
            case 'M': return "mm";
            case 'n': return "\u0000\n";
            case 'p': return "a";
            case 'r': return "hh:mm:ss a";
            case 'R': return "HH:mm";
            case 'S': return "ss";
            case 't': return "\u0000\t";
            case 'T': return "HH:mm:ss";
            case 'u': return "u";
            case 'x': return localizedPattern(
                    DateFormat.getDateInstance(DateFormat.SHORT,
                            Locale.getDefault()));
            case 'X': return localizedPattern(
                    DateFormat.getTimeInstance(DateFormat.MEDIUM,
                            Locale.getDefault()));
            case 'y': return "yy";
            case 'Y': return "yyyy";
            case 'z': return "Z";
            case 'Z': return "z";
            default: return null;
        }
    }

    private static String localizedPattern(DateFormat format) {
        if (format instanceof SimpleDateFormat) {
            return ((SimpleDateFormat) format).toPattern();
        }
        return "";
    }

    private static void flushLiteral(
            StringBuilder pattern, StringBuilder literal) {
        if (literal.length() == 0) return;
        appendLiteral(pattern, literal.toString());
        literal.setLength(0);
    }

    private static void appendLiteral(StringBuilder pattern, String text) {
        appendLiteralWithMapping(pattern, text);
    }

    private static int[] appendLiteralWithMapping(
            StringBuilder pattern, String text) {
        int[] outputPositions = new int[text.length() + 1];
        if (text.isEmpty()) {
            outputPositions[0] = pattern.length();
            return outputPositions;
        }
        if (pattern.length() > 0
                && pattern.charAt(pattern.length() - 1) == '\'') {
            // Extend the previous quoted literal. Adjacent quoted sections
            // are parsed as an escaped apostrophe by SimpleDateFormat.
            pattern.setLength(pattern.length() - 1);
        } else {
            pattern.append('\'');
        }
        for (int i = 0; i < text.length(); ++i) {
            outputPositions[i] = pattern.length();
            char current = text.charAt(i);
            pattern.append(current);
            if (current == '\'') pattern.append('\'');
        }
        outputPositions[text.length()] = pattern.length();
        pattern.append('\'');
        return outputPositions;
    }

    static String literalPattern(String text) {
        StringBuilder pattern = new StringBuilder();
        appendLiteral(pattern, text);
        return pattern.toString();
    }
}
