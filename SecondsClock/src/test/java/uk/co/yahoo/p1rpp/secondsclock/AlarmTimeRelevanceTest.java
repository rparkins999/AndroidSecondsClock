package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertArrayEquals;

public class AlarmTimeRelevanceTest {
    private static final long NOW = 1_700_000_000_000L;

    @Test
    public void retainsFutureAndRecentPastAlarmTimes() {
        assertEquals(Long.valueOf(NOW + 1),
                AlarmTimeRelevance.withinMaxAge(NOW + 1, NOW, 24));
        assertEquals(Long.valueOf(NOW - 1),
                AlarmTimeRelevance.withinMaxAge(NOW - 1, NOW, 24));
    }

    @Test
    public void retainsAlarmAtSelectedMaximumAgeBoundary() {
        int maxAgeHours = 24;
        long boundary = NOW - maxAgeHours * 60L * 60L * 1000L;

        assertEquals(Long.valueOf(boundary),
                AlarmTimeRelevance.withinMaxAge(boundary, NOW, maxAgeHours));
    }

    @Test
    public void ignoresAlarmOlderThanSelectedMaximumAge() {
        int maxAgeHours = 6;
        long staleAlarm = NOW - maxAgeHours * 60L * 60L * 1000L - 1;

        assertNull(AlarmTimeRelevance.withinMaxAge(
                staleAlarm, NOW, maxAgeHours));
    }

    @Test
    public void keepsMissingAlarmMissing() {
        assertNull(AlarmTimeRelevance.withinMaxAge(null, NOW, 24));
    }

    @Test
    public void alwaysShowIsTheDefaultAndDoesNotFilterOldAlarms() {
        long oldAlarm = NOW - 365L * 24L * 60L * 60L * 1000L;

        assertEquals(AlarmTimeRelevance.ALWAYS_SHOW,
                AlarmTimeRelevance.DEFAULT_MAX_AGE_HOURS);
        assertEquals(Long.valueOf(oldAlarm), AlarmTimeRelevance.withinMaxAge(
                oldAlarm, NOW, AlarmTimeRelevance.DEFAULT_MAX_AGE_HOURS));
    }

    @Test
    public void offersSeveralMaximumAgesAndAlwaysShow() {
        assertArrayEquals(new int[] {0, 1, 6, 12, 24, 48, 168},
                AlarmTimeRelevance.MAX_AGE_HOURS_OPTIONS);
        assertEquals(4, AlarmTimeRelevance.optionIndex(24));
        assertEquals(6, AlarmTimeRelevance.optionIndex(168));
        assertEquals(0, AlarmTimeRelevance.optionIndex(2));
    }
}
