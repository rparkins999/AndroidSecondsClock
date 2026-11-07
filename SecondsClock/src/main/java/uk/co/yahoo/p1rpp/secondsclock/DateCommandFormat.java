/*
 * Copyright © 2026. Released under GPL V3 or later.
 *
 * Translates common GNU date-style conversions to Android TextClock patterns.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class DateCommandFormat {
    private static final String ALARM_START = "%<";
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

        StringBuilder result = new StringBuilder();
        Date now = new Date();
        int index = 0;
        while (index < format.length()) {
            if (format.startsWith(ALARM_START, index)) {
                int contentStart = index + ALARM_START.length();
                int end = format.indexOf(ALARM_END, contentStart);
                if (end >= 0) {
                    if (nextAlarmTime != null) {
                        String alarmPattern = toDatePattern(
                                format.substring(contentStart, end),
                                new Date(nextAlarmTime));
                        String alarmText = new SimpleDateFormat(
                                alarmPattern, Locale.getDefault())
                                .format(new Date(nextAlarmTime));
                        appendLiteral(result, alarmText);
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
            appendPattern(result, toDatePattern(
                    format.substring(index, segmentEnd), now));
            index = segmentEnd;
        }
        return result.toString();
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

    private static void appendPattern(StringBuilder pattern, String part) {
        if (pattern.length() > 0 && part.length() > 0
                && pattern.charAt(pattern.length() - 1) == '\''
                && part.charAt(0) == '\'') {
            // Merge adjacent quoted literals. SimpleDateFormat treats adjacent
            // quote delimiters as a literal apostrophe instead of a boundary.
            pattern.setLength(pattern.length() - 1);
            pattern.append(part, 1, part.length());
        } else {
            pattern.append(part);
        }
    }

    private static String toDatePattern(String format, Date referenceDate) {
        StringBuilder pattern = new StringBuilder();
        StringBuilder literal = new StringBuilder();
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
                if (patternConversion.startsWith("\u0000")) {
                    appendLiteral(pattern, patternConversion.substring(1));
                } else {
                    pattern.append(patternConversion);
                }
            }
            index = end;
        }
        flushLiteral(pattern, literal);
        return pattern.toString();
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
        if (text.isEmpty()) return;
        if (pattern.length() > 0
                && pattern.charAt(pattern.length() - 1) == '\'') {
            // Extend the previous quoted literal. Adjacent quoted sections
            // are parsed as an escaped apostrophe by SimpleDateFormat.
            pattern.setLength(pattern.length() - 1);
        } else {
            pattern.append('\'');
        }
        for (int i = 0; i < text.length(); ++i) {
            char current = text.charAt(i);
            pattern.append(current);
            if (current == '\'') pattern.append('\'');
        }
        pattern.append('\'');
    }

    static String literalPattern(String text) {
        StringBuilder pattern = new StringBuilder();
        appendLiteral(pattern, text);
        return pattern.toString();
    }
}
