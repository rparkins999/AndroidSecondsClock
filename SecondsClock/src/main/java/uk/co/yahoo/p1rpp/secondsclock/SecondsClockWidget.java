/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This class implements the AppWidgetProvider for a home screen widget
 * which can display the time of day in locale-appropriate format,
 * with or without seconds.
 * It can also display the day of the week in short or long format,
 * a short date in local-appropriate format, the day of the month,
 * a long or short month name, and the year.
 * The short date cannot be displayed together with the day of the month,
 * month name, or year. Otherwise all combinations are possible.
 * The widget is fully programmable from its app settings page.
 *
 * Users should be aware that displaying seconds can increase battery drain.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.Objects;

/*
 * Implementation of App Widget functionality.
 */
public class SecondsClockWidget extends AppWidgetProvider {

    @SuppressLint("ApplySharedPref")
    void updateAppWidget(
        Context context, AppWidgetManager appWidgetManager, int appWidgetId)
    {
        int showTime;
        int showWeekDay;
        int showShortDate;
        int showMonthDay;
        int showMonth;
        int showYear;
        int bgcolour;
        int fgcolour;
        int touchaction;
        String key = "W" + appWidgetId;
        SharedPreferences prefs = context.getSharedPreferences(
            "SecondsClock", Context.MODE_PRIVATE);
        String widgetIds = prefs.getString("widgetIds", "");
        String thisWidget = "[" + String.valueOf(appWidgetId) + "]";
        if (Objects.requireNonNull(widgetIds).contains(thisWidget)) {
            // Get the preferences for this widget.
            showTime =
                prefs.getInt(key +" showTime", 2); // include seconds
            showWeekDay =
                prefs.getInt(key + " showWeekDay", 2); // long format
            showShortDate = prefs.getInt(key +" showShortDate", 0);
            showMonthDay =
                prefs.getInt(key + "showMonthDay", 1);
            showMonth =
                prefs.getInt(key + "showMonth", 2); // long format
            showYear = prefs.getInt(key + "showYear", 1);
            bgcolour = prefs.getInt(key + "bgcolour", 0x00000000);
            fgcolour = prefs.getInt(key + "fgcolour",0xFFFFFFFF);
            touchaction = prefs.getInt(key + "touchaction", 0);
        } else {
            // New widget, add to list and set its preferences
            // to the new widget defaults.
            showTime =
                prefs.getInt("WshowTime", 2); // include seconds
            showWeekDay =
                prefs.getInt("WshowWeekDay", 2); // long format
            showShortDate = prefs.getInt("WshowShortDate", 0);
            showMonthDay =
                prefs.getInt("WshowMonthDay", 1);
            showMonth = prefs.getInt("WshowMonth", 2); // long format
            showYear = prefs.getInt("WshowYear", 1);
            bgcolour = prefs.getInt("Wbgcolour", 0x00000000);
            fgcolour = prefs.getInt("Wfgcolour",0xFFFFFFFF);
            touchaction = prefs.getInt("Wtouchaction", 0);
            widgetIds += thisWidget;
            prefs.edit()
                 .putString("widgetIds", widgetIds)
                 .putInt(key + "showTime", showTime)
                 .putInt(key + "showWeekDay", showWeekDay)
                 .putInt(key + "showShortDate", showShortDate)
                 .putInt(key + "showMonthDay", showMonthDay)
                 .putInt(key + "showMonth", showMonth)
                 .putInt(key + "showYear", showYear)
                 .putInt(key + "bgcolour", bgcolour)
                 .putInt(key + "fgcolour", fgcolour)
                 .putInt(key + "touchaction", touchaction)
                 .commit();
        }

        Bundle newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minWidth = newOptions.getInt(
            AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 93);
        int maxHeight = newOptions.getInt(
            AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 121);
        /* debugging
        int maxWidth = newOptions.getInt(
            AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 93);
        int minHeight = newOptions.getInt(
            AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 121);
        String s = "Width min " + minWidth
                      + " max " + maxWidth
              + ", Height min " + minHeight
                      + " min " + maxHeight;
        Toast.makeText(context, s, Toast.LENGTH_LONG).show();
        // */

        Formatter f = new Formatter();
        // If showShortDate is not zero,
        // showMonthDay, showMonth, and showYear must all be zero.
        f.set(context, minWidth, maxHeight,
            showTime, showWeekDay, showShortDate,
            showMonthDay, showMonth, showYear);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews
            (context.getPackageName(), R.layout.seconds_clock_widget);
        views.setInt(R.id.appwidget_textclock,
            "setBackgroundColor", bgcolour);
        views.setTextColor(R.id.appwidget_textclock, fgcolour);
        views.setCharSequence(R.id.appwidget_textclock,
            "setFormat12Hour", f.time12 + f.rest);
        views.setCharSequence(R.id.appwidget_textclock,
            "setFormat24Hour", f.time24 + f.rest);
        views.setInt(R.id.appwidget_textclock,"setLines",f.lines);

        // For analogue clock,
        // views.setImageViewBitmap(int viewId, Bitmap bitmap)
        // but we need to find a way to force updates every second,
        // but only when the widget is visible.

        // Make a click on the widget go to the switch in MainActivity.
        Intent ai = new Intent(context,
            uk.co.yahoo.p1rpp.secondsclock.MainActivity.class);
        ai.putExtra("widgetID", appWidgetId);
        PendingIntent pi = PendingIntent.getActivity(
            context, appWidgetId, ai, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.appwidget_textclock, pi);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onAppWidgetOptionsChanged(
        Context context, AppWidgetManager appWidgetManager,
        int appWidgetId, Bundle newOptions)
    {
        super.onAppWidgetOptionsChanged(
            context, appWidgetManager, appWidgetId, newOptions);
        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    @Override
    public void onUpdate(Context context,
                         AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        SharedPreferences prefs = context.getSharedPreferences(
            "SecondsClock", Context.MODE_PRIVATE);
        String widgetIds = prefs.getString("widgetIds", "");
        for (int i: appWidgetIds) {
            String thisWidget = "[" + String.valueOf(i) + "]";
            if (Objects.requireNonNull(widgetIds).contains(thisWidget)) {
                widgetIds = widgetIds.replace(thisWidget, "");
            }
        }
        prefs.edit() .putString("widgetIds", widgetIds).commit();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        /* debugging
        Toast.makeText(context, intent.getAction(),
            Toast.LENGTH_LONG).show();
        // */
        super.onReceive(context, intent);
    }
}
