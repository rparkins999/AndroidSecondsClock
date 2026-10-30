/*
 * Copyright © 2022. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * Converts the widget's date-command-style format into a TextClock pattern.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.Context;
import android.content.SharedPreferences;

class Formatter {
    public String time12;
    public String time24;
    public String rest;
    public int lines;

    Formatter() {}

    /**
     * Builds a format matching the old checkbox configuration. This is used
     * for widgets that were configured before custom formats were introduced.
     */
    static String fromLegacyPreferences(
            Context context, SharedPreferences prefs, String key) {
        int showTime = prefs.getInt(key + "showTime", 2);
        int showWeekDay = prefs.getInt(key + "showWeekDay", 2);
        int showShortDate = prefs.getInt(key + "showShortDate", 0);
        int showMonthDay = prefs.getInt(key + "showMonthDay", 1);
        int showMonth = prefs.getInt(key + "showMonth", 2);
        int showYear = prefs.getInt(key + "showYear", 1);

        StringBuilder format = new StringBuilder();
        boolean anything = false;
        if (showTime > 0) {
            boolean is24Hour = android.text.format.DateFormat
                    .is24HourFormat(context);
            if (is24Hour) {
                format.append(showTime == 2 ? "%H:%M:%S" : "%H:%M");
            } else {
                format.append(showTime == 2 ? "%l:%M:%S %p" : "%l:%M %p");
            }
            anything = true;
        }
        if (showWeekDay > 0) {
            if (anything) format.append('\n');
            format.append(showWeekDay == 1 ? "%a" : "%A");
            anything = true;
        }
        if (showShortDate > 0) {
            if (anything) format.append('\n');
            format.append("%x");
        } else {
            if (showMonthDay > 0) {
                if (anything) format.append('\n');
                format.append("%e");
                anything = true;
            }
            if (showMonth > 0) {
                if (anything) format.append('\n');
                format.append(showMonth == 1 ? "%b" : "%B");
                anything = true;
            }
            if (showYear > 0) {
                if (anything) format.append('\n');
                format.append("%Y");
            }
        }
        return format.toString();
    }

    public void set(String format, Long nextAlarmTime) {
        String pattern = DateCommandFormat.toTextClockPattern(
                format, nextAlarmTime);
        time12 = pattern;
        time24 = pattern;
        rest = "";
        lines = 1;
        for (int i = 0; i < pattern.length(); ++i) {
            if (pattern.charAt(i) == '\n') ++lines;
        }
    }

    static Long getNextAlarmTime(Context context) {
        android.app.AlarmManager alarmManager =
                (android.app.AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE);
        if (alarmManager == null) return null;
        android.app.AlarmManager.AlarmClockInfo nextAlarm =
                alarmManager.getNextAlarmClock();
        return nextAlarm == null ? null : nextAlarm.getTriggerTime();
    }
}
