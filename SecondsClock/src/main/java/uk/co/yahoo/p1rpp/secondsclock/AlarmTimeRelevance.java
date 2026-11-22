/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.SharedPreferences;

/** Filters stale timestamps returned by alarm-clock applications. */
final class AlarmTimeRelevance {
    static final String MAX_AGE_KEY = "alarmMaxAgeHours";
    static final int ALWAYS_SHOW = 0;
    static final int DEFAULT_MAX_AGE_HOURS = ALWAYS_SHOW;
    static final int[] MAX_AGE_HOURS_OPTIONS = {ALWAYS_SHOW, 1, 6, 12, 24, 48, 168};
    private static final long HOUR_MILLIS = 60L * 60L * 1000L;

    private AlarmTimeRelevance() {}

    static int maxAgeHours(SharedPreferences preferences) {
        try {
            int maxAgeHours = preferences.getInt(MAX_AGE_KEY, DEFAULT_MAX_AGE_HOURS);
            return isSupportedMaxAgeHours(maxAgeHours)
                    ? maxAgeHours : DEFAULT_MAX_AGE_HOURS;
        } catch (ClassCastException ignored) {
            return DEFAULT_MAX_AGE_HOURS;
        }
    }

    static boolean isSupportedMaxAgeHours(int maxAgeHours) {
        for (int option : MAX_AGE_HOURS_OPTIONS) {
            if (option == maxAgeHours) return true;
        }
        return false;
    }

    static int optionIndex(int maxAgeHours) {
        for (int index = 0; index < MAX_AGE_HOURS_OPTIONS.length; ++index) {
            if (MAX_AGE_HOURS_OPTIONS[index] == maxAgeHours) return index;
        }
        return 0;
    }

    static Long withinMaxAge(
            Long alarmTimeMillis, long nowMillis, int maxAgeHours) {
        if (alarmTimeMillis == null) return null;
        Long expiryTimeMillis = expiryTimeMillis(alarmTimeMillis, maxAgeHours);
        return expiryTimeMillis != null && nowMillis >= expiryTimeMillis
                ? null : alarmTimeMillis;
    }

    /**
     * Returns the first millisecond at which an alarm exceeds its maximum age.
     * A null result means there is no configured cutoff (or no alarm).
     */
    static Long expiryTimeMillis(Long alarmTimeMillis, int maxAgeHours) {
        if (alarmTimeMillis == null || maxAgeHours <= ALWAYS_SHOW) return null;
        long maxAgeMillis = maxAgeHours * HOUR_MILLIS;
        if (alarmTimeMillis > Long.MAX_VALUE - maxAgeMillis - 1) return null;
        return alarmTimeMillis + maxAgeMillis + 1;
    }
}
