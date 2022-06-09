/*
 * Copyright © 2022. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This class implements the format control for SecondsClockWidget.
 *
 * It is common code used both by the SecondsClockWidget itself
 * and by the settings page to show what the widget will look like.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
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
                    int maxHeight,
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
        boolean anything = false;
        StringBuilder sbrest = new StringBuilder();
        if (showTime > 0) {
            time12 = "h:mm a";
            time24 = "HH:mm";
            if (showTime == 2) { // show seconds
                int anydate = showWeekDay + showShortDate + showMonthDay
                    + showMonth + showYear;
                if ((minWidth >= maxHeight) || (anydate != 0)) {
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
                if ((showMonth + showYear == 3)  && (minWidth >= maxHeight)) {
                    sbrest.append(" ");
                } else {
                    sbrest.append("\n");
                    ++lines;
                }
            }
            if (showWeekDay == 1) {
                sbrest.append("c");
            } else {
                sbrest.append("cccc");
            }
            anything = true;
        }
        if (showShortDate > 0) {
            if (anything)
            {
                sbrest.append("\n");
                ++lines;
            }
            sbrest.append(shortDateFormat(context));
            anything = true;
        } else {
            if (showMonthDay > 0) {
                if (anything) {
                    if ((showWeekDay == 1) && (showMonth + showYear == 0)) {
                        sbrest.append(" ");
                    } else {
                        sbrest.append("\n");
                        ++lines;
                    }
                }
                sbrest.append("d");
                anything = true;
            }
            if (showMonth > 0) {
                if ((showMonthDay != 0) && (minWidth >= maxHeight)) {
                    sbrest.append(" ");
                } else if (anything) {
                    sbrest.append("\n");
                    ++lines;
                }
                if (showMonth == 1) {
                    sbrest.append("LLL");
                } else {
                    sbrest.append("LLLL");
                }
                anything = true;
            }
            if (showYear > 0) {
                if (   (showMonthDay + showMonth > 0)
                    && (showMonth < 2) && (minWidth >= maxHeight))
                {
                    sbrest.append(" ");
                } else if (anything) {
                    sbrest.append("\n");
                    ++lines;
                }
                sbrest.append("yyyy");
            }
        }
        rest = sbrest.toString();
    }
}
