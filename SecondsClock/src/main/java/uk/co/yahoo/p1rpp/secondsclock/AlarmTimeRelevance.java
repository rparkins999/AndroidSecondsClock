/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

/** Filters stale timestamps returned by alarm-clock applications. */
final class AlarmTimeRelevance {
    static final long MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L;

    private AlarmTimeRelevance() {}

    static Long withinMaxAge(Long alarmTimeMillis, long nowMillis) {
        if (alarmTimeMillis == null) return null;
        return alarmTimeMillis < nowMillis - MAX_AGE_MILLIS
                ? null : alarmTimeMillis;
    }
}
