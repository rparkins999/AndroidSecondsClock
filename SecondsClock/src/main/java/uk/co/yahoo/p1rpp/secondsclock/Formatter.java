/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This class implements the format control for SecondsClockWidget.
 *
 * It is common code used both by the SecondsClockWidget itself
 * and by the settings page to show what the widget will look like.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.Context;
import android.text.format.DateFormat;

class Formatter {
    public String time12;
    public String time24;
    public String rest;
    public int lines;

    Formatter() {}

    /* Android doesn't get this case right so we add some em/6 spaces */
    String shortDateFormat(Context context) {
        char[] order = DateFormat.getDateFormatOrder(context);
        StringBuilder result = new StringBuilder("\u2006");
        for (int i = 0; i < 3; ++i) {
            if (i != 0) { result.append("/"); }
            switch (order[i]) {
                case 'd': result.append("dd"); break;
                case 'M': result.append("MM"); break;
                case 'y': result.append("yy"); break;
            }
        }
        return result.append("\u2006").toString();
    }
    public void set(Context context,
                    int minWidth,
                    int minHeight,
                    int showTime, // 0 => none, 1 => hhmm, 2 => hhmmss
                    int showWeekDay, // 0 => none, 1 => short, 2 => long
                    int showShortDate, // 0 => no. 1 => yes
                    int showMonthDay, // 0 => no. 1 => yes
                    int showMonth, // 0 => none, 1 => short, 2 => long
                    int showYear) // 0 => no. 1 => yes
    {
        time12 = "";
        time24 = "";
        lines = 1;
        StringBuilder sbrest = new StringBuilder();
        if (showTime > 0) {
            if (showTime == 2) { // show seconds
                time12 = "h:mm";
                time24 = "HH:mm";
                int anydate = showWeekDay + showShortDate + showMonthDay
                    + showMonth + showYear;
                if (minWidth >= 2 * minHeight) {
                    sbrest.append(":ss");
                } else if (anydate == 0) {
                    sbrest.append("'\n'ss");
                    ++lines;
                } else /*if (anydate >= 2)*/ {
                    sbrest.append(":ss");
               /* } else {
                    // Android doesn't get this case right, so we add a newline
                    sbrest.append("ss'\n'");*/
                }
            } else {
                time12 = "h:mm";
                time24 = "HH:mm";
            }
            if (showWeekDay > 0) {
                if (minWidth < 2 * minHeight) {
                    sbrest.append("'\n'");
                    ++lines;
                } else {
                    sbrest.append(" ");
                }
                if (showWeekDay == 1) {
                    sbrest.append("c");
                } else {
                    sbrest.append("cccc");
                }
            }
            if (showShortDate > 0) {
                if (   (minWidth < 2 * minHeight)
                    || (showWeekDay > 0))
                {
                    sbrest.append("'\n'");
                    ++lines;
                } else {
                    sbrest.append(" ");
                }
                sbrest.append(shortDateFormat(context));
            }
        } else if (showWeekDay == 1) {
            sbrest.append("c");
        } else if (showWeekDay == 2) {
            sbrest.append("cccc");
        }
        if ((showShortDate > 0) && (showTime == 0)) {
            if (showWeekDay > 0) {
                if (   ((showWeekDay == 1) && (minWidth < 2 * minHeight))
                    || (minWidth < 3 * minHeight))
                {
                    sbrest.append(" ");
                } else {
                    sbrest.append("'\n'");
                    ++lines;
                }
            }
            sbrest.append(shortDateFormat(context));
        }
        if (   (showMonthDay + showMonth + showYear > 0)
            && (showTime + showWeekDay > 0))
        {
            sbrest.append("'\n'");
            ++lines;
        }
        if (showMonthDay > 0) {
            sbrest.append("d");
        }
        if (showMonth > 0) {
            if (showMonthDay > 0) {
                if (minWidth < 2 * minHeight) {
                    sbrest.append("'\n'");
                    ++lines;
                } else {
                    sbrest.append(" ");
                }
            }
            if (showMonth == 1) {
                sbrest.append("LLL");
            } else {
                sbrest.append("LLLL");
            }
        }
        if (showYear > 0) {
            if (showMonthDay + showMonth > 0) {
                if (   (minWidth >= 3 * minHeight)
                    || (   (minWidth >= 2 * minHeight)
                    && (showMonth < 2)))
                {
                    sbrest.append(" ");
                } else {
                    sbrest.append("'\n'");
                    ++lines;
                }
            }
            sbrest.append("yyyy");
        }
        rest = sbrest.toString();
    }
}
