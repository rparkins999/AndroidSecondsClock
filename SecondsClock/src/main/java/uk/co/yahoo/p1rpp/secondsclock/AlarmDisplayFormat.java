/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class AlarmDisplayFormat {
    private AlarmDisplayFormat() {}

    static String format(
            Long triggerTime, String prefix, Locale locale, TimeZone timeZone) {
        if (triggerTime == null) return "";
        SimpleDateFormat timeFormat = new SimpleDateFormat("EEE, h:mm", locale);
        timeFormat.setTimeZone(timeZone);
        return prefix + " " + timeFormat.format(new Date(triggerTime));
    }
}
