package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DateCommandFormatTest {
    @Test
    public void timeRangesIncludeClockSeparatorsButExcludeDateText() {
        DateCommandFormat.FormattedPattern formatted =
                DateCommandFormat.toTextClockPatternWithTimeRanges(
                        "%A %d%n%H:%M:%S %p", null);

        assertEquals(1, formatted.timeRanges.size());
        DateCommandFormat.PatternRange time = formatted.timeRanges.get(0);
        String timePattern = formatted.pattern.substring(time.start, time.end);
        assertTrue(timePattern.contains("HH"));
        assertTrue(timePattern.contains("mm"));
        assertTrue(timePattern.contains("ss"));
        assertTrue(timePattern.contains("a"));
        assertFalse(timePattern.contains("EEEE"));
        assertFalse(timePattern.contains("dd"));
    }

    @Test
    public void localizedDateTimeStylesOnlyItsTimePart() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            DateCommandFormat.FormattedPattern formatted =
                    DateCommandFormat.toTextClockPatternWithTimeRanges("%c", null);

            assertEquals(1, formatted.timeRanges.size());
            DateCommandFormat.PatternRange time = formatted.timeRanges.get(0);
            String timePattern = formatted.pattern.substring(time.start, time.end);
            assertTrue(timePattern.contains("h"));
            assertTrue(timePattern.contains("mm"));
            assertFalse(timePattern.contains("MMM"));
            assertFalse(timePattern.contains("yyyy"));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void alarmBlockTimeRangeMapsToTheRenderedAlarmTime() {
        long alarmTime = 1_700_000_000_000L;
        DateCommandFormat.FormattedPattern formatted =
                DateCommandFormat.toTextClockPatternWithTimeRanges(
                        "%<Alarm %H:%M%>", alarmTime);

        assertEquals(1, formatted.timeRanges.size());
        DateCommandFormat.PatternRange time = formatted.timeRanges.get(0);
        String expectedTime = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(alarmTime));
        assertEquals(expectedTime,
                formatted.pattern.substring(time.start, time.end));
    }
}
