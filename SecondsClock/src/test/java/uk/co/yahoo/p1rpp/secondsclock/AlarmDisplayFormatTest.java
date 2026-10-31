package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import java.time.Instant;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class AlarmDisplayFormatTest {
    @Test
    public void formatsShortWeekdayAndTwelveHourTime() {
        long mondayAtThreeThirty = Instant.parse("2024-01-01T03:30:00Z").toEpochMilli();
        assertEquals("ALM: Mon, 3:30", AlarmDisplayFormat.format(
                mondayAtThreeThirty, "ALM:", Locale.US, TimeZone.getTimeZone("UTC")));
    }

    @Test
    public void omitsAlarmTextWhenNoAlarmIsSet() {
        assertEquals("", AlarmDisplayFormat.format(
                null, "ALM:", Locale.US, TimeZone.getTimeZone("UTC")));
    }
}
