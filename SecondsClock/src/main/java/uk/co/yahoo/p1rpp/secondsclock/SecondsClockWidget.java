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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;

/*
 * Implementation of App Widget functionality.
 */
public class SecondsClockWidget extends AppWidgetProvider {

    void updateAppWidget(
        Context context, AppWidgetManager appWidgetManager, int appWidgetId)
    {
        SharedPreferences prefs = context.getSharedPreferences(
            "SecondsClock", Context.MODE_PRIVATE);
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
        // if showShortDate is not zero,
        // showMonthDay, showMonth, and showYear must all be zero
        // include seconds
        int showTime =
            prefs.getInt("WshowTime", 2); // include seconds
        int showWeekDay =
            prefs.getInt("WshowWeekDay", 2); // long format
        int showShortDate = prefs.getInt("WshowShortDate", 0);
        int showMonthDay =
            prefs.getInt("WshowMonthDay", 1); // long format
        int showMonth = prefs.getInt("WshowMonth", 2); // long format
        int showYear = prefs.getInt("WshowYear", 1);
        int bgcolour = prefs.getInt("Wbgcolour", 0x00000000);
        int fgcolour = prefs.getInt("Wfgcolour",0xFFFFFFFF);
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

        // for analogue clock,
        // views.setImageViewBitmap(int viewId, Bitmap bitmap)
        // but we need to find a way to force updates every second

        // Make a click on the widget go to the clock app
        Intent ai = new Intent("android.intent.action.SHOW_TIMERS");
        PendingIntent pi = PendingIntent.getActivity(
            context, 0, ai, PendingIntent.FLAG_IMMUTABLE);
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

    @Override
    public void onReceive(Context context, Intent intent) {
        /* debugging
        Toast.makeText(context, intent.getAction(),
            Toast.LENGTH_LONG).show();
        // */
        super.onReceive(context, intent);
    }
}
