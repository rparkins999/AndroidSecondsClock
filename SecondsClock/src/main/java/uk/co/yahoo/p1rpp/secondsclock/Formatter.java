/*
 * Copyright © 2022. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * Formats widget checkbox settings or custom date-command text.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;

import java.util.Locale;
import java.util.TimeZone;

class Formatter {
    public String time12;
    public String time24;
    public String rest;
    public int lines;

    Formatter() {}

    static String fromLegacyPreferences(
            Context context, SharedPreferences prefs, String key) {
        int showTime = prefs.getInt(key + "showTime", 2);
        int showWeekDay = prefs.getInt(key + "showWeekDay", 2);
        int showShortDate = prefs.getInt(key + "showShortDate", 0);
        int showMonthDay = prefs.getInt(key + "showMonthDay", 1);
        int showMonth = prefs.getInt(key + "showMonth", 2);
        int showYear = prefs.getInt(key + "showYear", 1);
        return makeDateCommandFormat(context, showTime == 2,
                showTime > 0, showWeekDay, showShortDate,
                showMonthDay, showMonth, showYear, false);
    }

    static String fromClockPreferences(
            Context context, SharedPreferences prefs, String key) {
        int secondsSize = prefs.getInt(key + "secondsSize", 255);
        int showWeekDay = prefs.getInt(key + "showWeekDay", 2);
        int showShortDate = prefs.getInt(key + "showShortDate", 0);
        int showMonthDay = prefs.getInt(key + "showMonthDay", 1);
        int showMonth = prefs.getInt(key + "showMonth", 2);
        int showYear = prefs.getInt(key + "showYear", 1);
        return makeDateCommandFormat(context, secondsSize > 0, true,
                showWeekDay, showShortDate, showMonthDay, showMonth, showYear, true);
    }

    private static String makeDateCommandFormat(
            Context context, boolean includeSeconds, boolean includeTime,
            int showWeekDay, int showShortDate, int showMonthDay,
            int showMonth, int showYear, boolean zeroPadDay) {
        StringBuilder format = new StringBuilder();
        if (includeTime) {
            boolean is24Hour = DateFormat.is24HourFormat(context);
            if (is24Hour) {
                format.append(includeSeconds ? "%H:%M:%S" : "%H:%M");
            } else {
                format.append(includeSeconds ? "%l:%M:%S %p" : "%l:%M %p");
            }
        }
        appendDateCommandFormat(format, showWeekDay, showShortDate,
                showMonthDay, showMonth, showYear, zeroPadDay);
        return format.toString();
    }

    private static void appendDateCommandFormat(
            StringBuilder format, int showWeekDay, int showShortDate,
            int showMonthDay, int showMonth, int showYear, boolean zeroPadDay) {
        if (showWeekDay > 0) {
            appendLine(format, showWeekDay == 1 ? "%a" : "%A");
        }
        if (showShortDate > 0) {
            appendLine(format, "%x");
        } else {
            if (showMonthDay > 0) appendLine(format, zeroPadDay ? "%d" : "%e");
            if (showMonth > 0) appendLine(format, showMonth == 1 ? "%b" : "%B");
            if (showYear > 0) appendLine(format, "%Y");
        }
    }

    private static void appendLine(StringBuilder format, String line) {
        if (format.length() > 0) format.append('\n');
        format.append(line);
    }

    /** Builds the adaptive layout used by the original checkbox interface. */
    void setCheckboxes(
            Context context, int minWidth, int maxHeight,
            int showTime, int showWeekDay, int showShortDate,
            int showMonthDay, int showMonth, int showYear,
            boolean showNextAlarm, Long nextAlarmTime) {
        time12 = "";
        time24 = "";
        lines = 1;
        boolean anything = false;
        StringBuilder restBuilder = new StringBuilder();
        if (showTime > 0) {
            time12 = "h:mm a";
            time24 = "HH:mm";
            if (showTime == 2) {
                int anydate = showWeekDay + showShortDate + showMonthDay
                        + showMonth + showYear;
                if (minWidth >= maxHeight || anydate != 0) {
                    time12 = "h:mm:ss a";
                    time24 = "HH:mm:ss";
                } else {
                    time12 = "h:mm'\n'ss a";
                    time24 = "HH:mm'\n'ss";
                    ++lines;
                }
            }
            anything = true;
        }
        if (showWeekDay > 0) {
            if (anything) {
                if (showMonth + showYear == 3 && minWidth >= maxHeight) {
                    restBuilder.append(" ");
                } else {
                    restBuilder.append("\n");
                    ++lines;
                }
            }
            restBuilder.append(showWeekDay == 1 ? "c" : "cccc");
            anything = true;
        }
        if (showShortDate > 0) {
            if (anything) {
                restBuilder.append("\n");
                ++lines;
            }
            restBuilder.append(shortDateFormat(context));
            anything = true;
        } else {
            if (showMonthDay > 0) {
                if (anything) {
                    if (showWeekDay == 1 && showMonth + showYear == 0) {
                        restBuilder.append(" ");
                    } else {
                        restBuilder.append("\n");
                        ++lines;
                    }
                }
                restBuilder.append("d");
                anything = true;
            }
            if (showMonth > 0) {
                if (showMonthDay != 0 && minWidth >= maxHeight) {
                    restBuilder.append(" ");
                } else if (anything) {
                    restBuilder.append("\n");
                    ++lines;
                }
                restBuilder.append(showMonth == 1 ? "LLL" : "LLLL");
                anything = true;
            }
            if (showYear > 0) {
                if ((showMonthDay + showMonth > 0) && showMonth < 2
                        && minWidth >= maxHeight) {
                    restBuilder.append(" ");
                } else if (anything) {
                    restBuilder.append("\n");
                    ++lines;
                }
                restBuilder.append("yyyy");
            }
        }

        if (showNextAlarm && nextAlarmTime != null) {
            boolean hadContent = time12.length() > 0 || restBuilder.length() > 0;
            String alarmText = AlarmDisplayFormat.format(
                    nextAlarmTime,
                    context.getString(R.string.clock_alarm_prefix),
                    Locale.getDefault(), TimeZone.getDefault());
            if (!alarmText.isEmpty()) {
                if (hadContent) restBuilder.append("\n");
                restBuilder.append(DateCommandFormat.literalPattern(alarmText));
                if (hadContent) ++lines;
            }
        }
        rest = restBuilder.toString();
    }

    String shortDateFormat(Context context) {
        char[] order = DateFormat.getDateFormatOrder(context);
        StringBuilder result = new StringBuilder("\u2006");
        for (int i = 0; i < 3; ++i) {
            if (i != 0) result.append("/");
            switch (order[i]) {
                case 'd': result.append("dd"); break;
                case 'M': result.append("MM"); break;
                case 'y': result.append("yy"); break;
            }
        }
        return result.append("\u2006").toString();
    }

    public void set(String format, Long nextAlarmTime) {
        String pattern = DateCommandFormat.toTextClockPattern(format, nextAlarmTime);
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
                (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return null;
        android.app.AlarmManager.AlarmClockInfo nextAlarm = alarmManager.getNextAlarmClock();
        return nextAlarm == null ? null : nextAlarm.getTriggerTime();
    }
}
