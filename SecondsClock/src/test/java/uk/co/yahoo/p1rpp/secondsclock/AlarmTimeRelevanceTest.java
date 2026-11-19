package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AlarmTimeRelevanceTest {
    private static final long NOW = 1_700_000_000_000L;

    @Test
    public void retainsFutureAndRecentPastAlarmTimes() {
        assertEquals(Long.valueOf(NOW + 1),
                AlarmTimeRelevance.withinMaxAge(NOW + 1, NOW));
        assertEquals(Long.valueOf(NOW - 1),
                AlarmTimeRelevance.withinMaxAge(NOW - 1, NOW));
    }

    @Test
    public void retainsAlarmAtMaximumAgeBoundary() {
        long boundary = NOW - AlarmTimeRelevance.MAX_AGE_MILLIS;

        assertEquals(Long.valueOf(boundary),
                AlarmTimeRelevance.withinMaxAge(boundary, NOW));
    }

    @Test
    public void ignoresAlarmOlderThanMaximumAge() {
        long staleAlarm = NOW - AlarmTimeRelevance.MAX_AGE_MILLIS - 1;

        assertNull(AlarmTimeRelevance.withinMaxAge(staleAlarm, NOW));
    }

    @Test
    public void keepsMissingAlarmMissing() {
        assertNull(AlarmTimeRelevance.withinMaxAge(null, NOW));
    }
}
