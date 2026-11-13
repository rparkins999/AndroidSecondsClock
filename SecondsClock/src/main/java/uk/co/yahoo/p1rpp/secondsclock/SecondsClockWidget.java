/*
 * Copyright © 2024. Richard P. Parkins, M. A.
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
 * The analog display uses the platform clock view's timekeeping in the launcher.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Objects;

/*
 * Implementation of App Widget functionality.
 */
public class SecondsClockWidget extends AppWidgetProvider {

    @SuppressLint("ApplySharedPref")
    void updateAppWidget(
        Context context, AppWidgetManager appWidgetManager, int appWidgetId)
    {
        int bgcolour;
        int fgcolour;
        int touchaction;
        boolean analogMode;
        String key = "W" + appWidgetId;
        SharedPreferences prefs = context.getSharedPreferences(
            "SecondsClock", Context.MODE_PRIVATE);
        String widgetIds = prefs.getString("widgetIds", "");
        String thisWidget = "[" + String.valueOf(appWidgetId) + "]";
        if (Objects.requireNonNull(widgetIds).contains(thisWidget)) {
            bgcolour = prefs.getInt(key + "bgcolour", 0x00000000);
            fgcolour = prefs.getInt(key + "fgcolour",0xFFFFFFFF);
            touchaction = prefs.getInt(key + "touchaction", 0);
            analogMode = prefs.getBoolean(key + "analogMode", false);
        } else {
            // New widget: copy the new-widget defaults into its own preferences.
            bgcolour = prefs.getInt("Wbgcolour", 0x00000000);
            fgcolour = prefs.getInt("Wfgcolour",0xFFFFFFFF);
            touchaction = prefs.getInt("Wtouchaction", 0);
            analogMode = prefs.getBoolean("WanalogMode", false);
            widgetIds += thisWidget;
            boolean defaultAdvancedFormatMode = prefs.getBoolean(
                    "WadvancedFormatMode", prefs.contains("Wformat"));
            String defaultFormat = prefs.getString("Wformat", null);
            if (defaultAdvancedFormatMode && defaultFormat == null) {
                defaultFormat = Formatter.fromLegacyPreferences(
                        context, prefs, "W");
            }
            SharedPreferences.Editor edit = prefs.edit()
                    .putString("widgetIds", widgetIds)
                    .putBoolean(key + "formatSyncEnabled",
                            prefs.getBoolean("WformatSyncEnabled", true))
                    .putBoolean(key + "textStyleEmoji",
                            prefs.getBoolean("WtextStyleEmoji", false))
                    .putBoolean(key + "advancedFormatMode",
                            defaultAdvancedFormatMode)
                    .putInt(key + "showTime", prefs.getInt("WshowTime", 2))
                    .putInt(key + "showWeekDay", prefs.getInt("WshowWeekDay", 2))
                    .putInt(key + "showShortDate", prefs.getInt("WshowShortDate", 0))
                    .putInt(key + "showMonthDay", prefs.getInt("WshowMonthDay", 1))
                    .putInt(key + "showMonth", prefs.getInt("WshowMonth", 2))
                    .putInt(key + "showYear", prefs.getInt("WshowYear", 1))
                    .putBoolean(key + "showNextAlarm",
                            prefs.getBoolean("WshowNextAlarm", false))
                    .putInt(key + "bgcolour", bgcolour)
                    .putInt(key + "fgcolour", fgcolour)
                    .putInt(key + "touchaction", touchaction)
                    .putBoolean(key + "analogMode", analogMode);
            if (defaultFormat != null) {
                edit.putString(key + "format", defaultFormat);
            }
            if (prefs.contains("WformatHorizontal")) {
                edit.putString(key + "formatHorizontal",
                        prefs.getString("WformatHorizontal", ""));
            }
            if (prefs.contains("WformatVertical")) {
                edit.putString(key + "formatVertical",
                        prefs.getString("WformatVertical", ""));
            }
            if (prefs.contains("WformatProfile")) {
                edit.putInt(key + "formatProfile",
                        prefs.getInt("WformatProfile", FormatProfiles.SYNC));
            }
            edit.commit();
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

        // Construct the RemoteViews object. Checkbox analog mode keeps the
        // date and alarm text below the dial; advanced mode uses its own
        // inline placeholder instead of hiding the digital-format settings.
        RemoteViews views = new RemoteViews(
                context.getPackageName(), R.layout.seconds_clock_widget);
        views.setInt(R.id.appwidget_root, "setBackgroundColor", bgcolour);
        views.setTextColor(R.id.appwidget_textclock, fgcolour);
        views.setTextColor(R.id.appwidget_analog_dateclock, fgcolour);

        Long nextAlarmTime = Formatter.getNextAlarmTime(context);
        Formatter f = new Formatter();
        boolean advancedFormatMode = prefs.getBoolean(
                key + "advancedFormatMode", prefs.contains(key + "format"));
        String format = null;
        if (advancedFormatMode) {
            String syncFormat = prefs.getString(key + "format", "");
            format = FormatProfiles.widgetFormat(
                    syncFormat,
                    prefs.getString(key + "formatHorizontal", null),
                    prefs.getString(key + "formatVertical", null),
                    prefs.getBoolean(key + "formatSyncEnabled", true),
                    FormatProfiles.isHorizontalWidget(minWidth, maxHeight));
        }
        boolean advancedAnalog = advancedFormatMode
                && AnalogFormat.containsPlaceholder(format);
        boolean checkboxAnalog = analogMode && !advancedFormatMode;
        views.setViewVisibility(R.id.appwidget_analogclock,
                checkboxAnalog ? android.view.View.VISIBLE : android.view.View.GONE);
        views.setViewVisibility(R.id.appwidget_analog_dateclock,
                checkboxAnalog ? android.view.View.VISIBLE : android.view.View.GONE);
        views.setViewVisibility(R.id.appwidget_format_container,
                advancedAnalog ? android.view.View.VISIBLE : android.view.View.GONE);
        views.setViewVisibility(R.id.appwidget_textclock,
                checkboxAnalog || advancedAnalog
                        ? android.view.View.GONE : android.view.View.VISIBLE);

        if (advancedAnalog) {
            AnalogFormatRenderer.Result rendered = AnalogFormatRenderer.render(
                    format, nextAlarmTime, false);
            addAdvancedAnalogFormat(context, views, rendered, fgcolour);
        } else if (checkboxAnalog) {
            boolean showTime = prefs.getInt(key + "showTime", 2) > 0;
            boolean showSeconds = prefs.getInt(key + "showTime", 2) == 2;
            views.setImageViewBitmap(R.id.appwidget_analog_dial,
                    AnalogClockFace.createBitmap(fgcolour));
            setAnalogHands(views, context, showTime, showTime,
                    showSeconds, R.id.appwidget_analog_hands,
                    R.id.appwidget_analog_hours_only,
                    R.id.appwidget_analog_minutes_only,
                    R.id.appwidget_analog_no_hands,
                    R.id.appwidget_second_hand_flipper);
            f.setCheckboxes(context, minWidth, maxHeight, 0,
                    prefs.getInt(key + "showWeekDay", 2),
                    prefs.getInt(key + "showShortDate", 0),
                    prefs.getInt(key + "showMonthDay", 1),
                    prefs.getInt(key + "showMonth", 2),
                    prefs.getInt(key + "showYear", 1),
                    prefs.getBoolean(key + "showNextAlarm", false),
                    nextAlarmTime);
            views.setCharSequence(R.id.appwidget_analog_dateclock,
                    "setFormat12Hour", f.time12 + f.rest);
            views.setCharSequence(R.id.appwidget_analog_dateclock,
                    "setFormat24Hour", f.time24 + f.rest);
            views.setInt(R.id.appwidget_analog_dateclock, "setLines", f.lines);
            views.setViewVisibility(R.id.appwidget_analog_dateclock,
                    f.rest.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
        } else {
            if (advancedFormatMode) {
                f.set(format, nextAlarmTime,
                        prefs.getBoolean(key + "textStyleEmoji", false));
            } else {
                f.setCheckboxes(context, minWidth, maxHeight,
                        prefs.getInt(key + "showTime", 2),
                        prefs.getInt(key + "showWeekDay", 2),
                        prefs.getInt(key + "showShortDate", 0),
                        prefs.getInt(key + "showMonthDay", 1),
                        prefs.getInt(key + "showMonth", 2),
                        prefs.getInt(key + "showYear", 1),
                        prefs.getBoolean(key + "showNextAlarm", false),
                        nextAlarmTime);
            }
            views.setCharSequence(R.id.appwidget_textclock,
                    "setFormat12Hour", f.time12 + f.rest);
            views.setCharSequence(R.id.appwidget_textclock,
                    "setFormat24Hour", f.time24 + f.rest);
            views.setInt(R.id.appwidget_textclock, "setLines", f.lines);
        }

        // Make a click anywhere on the widget go to the switch in MainActivity.
        Intent ai = new Intent(context,
            uk.co.yahoo.p1rpp.secondsclock.MainActivity.class);
        ai.putExtra("widgetID", appWidgetId);
        PendingIntent pi = PendingIntent.getActivity(
            context, appWidgetId, ai, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.appwidget_root, pi);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static boolean setRemoteViewIcon(
            RemoteViews views, int viewId, String methodName, Icon icon) {
        try {
            RemoteViews.class.getMethod(
                    "setIcon", int.class, String.class, Icon.class)
                    .invoke(views, viewId, methodName, icon);
            return true;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private static void addAdvancedAnalogFormat(
            Context context, RemoteViews views,
            AnalogFormatRenderer.Result rendered, int dialColor) {
        views.removeAllViews(R.id.appwidget_format_container);
        for (AnalogFormatRenderer.Line line : rendered.lines) {
            RemoteViews row = new RemoteViews(
                    context.getPackageName(), R.layout.widget_format_line);
            boolean added = false;
            for (AnalogFormatRenderer.Part part : line.parts) {
                if (part.analog) {
                    RemoteViews analog = new RemoteViews(
                            context.getPackageName(), R.layout.analog_clock_widget_inline);
                    analog.setImageViewBitmap(R.id.appwidget_analog_dial,
                            AnalogClockFace.createBitmap(dialColor));
                    setAnalogHands(analog, context,
                            rendered.hands.hour, rendered.hands.minute,
                            rendered.hands.second,
                            R.id.appwidget_analog_hands,
                            R.id.appwidget_analog_hours_only,
                            R.id.appwidget_analog_minutes_only,
                            R.id.appwidget_analog_no_hands,
                            R.id.appwidget_second_hand_flipper);
                    row.addView(R.id.widget_format_line, analog);
                    added = true;
                } else if (!part.pattern.isEmpty()) {
                    RemoteViews text = new RemoteViews(
                            context.getPackageName(), R.layout.widget_format_segment);
                    text.setTextColor(R.id.widget_format_segment_clock, dialColor);
                    text.setCharSequence(R.id.widget_format_segment_clock,
                            "setFormat12Hour", part.pattern);
                    text.setCharSequence(R.id.widget_format_segment_clock,
                            "setFormat24Hour", part.pattern);
                    row.addView(R.id.widget_format_line, text);
                    added = true;
                }
            }
            if (!added) {
                RemoteViews blank = new RemoteViews(
                        context.getPackageName(), R.layout.widget_format_segment);
                blank.setCharSequence(R.id.widget_format_segment_clock,
                        "setFormat12Hour", "''");
                blank.setCharSequence(R.id.widget_format_segment_clock,
                        "setFormat24Hour", "''");
                row.addView(R.id.widget_format_line, blank);
            }
            views.addView(R.id.appwidget_format_container, row);
        }
    }

    private static void setAnalogHands(
            RemoteViews views, Context context, boolean hour, boolean minute,
            boolean second, int bothId, int hourId, int minuteId, int noneId,
            int secondFlipperId) {
        int selectedId = hour
                ? minute ? bothId : hourId
                : minute ? minuteId : noneId;
        int[] clockIds = {bothId, hourId, minuteId, noneId};
        for (int clockId : clockIds) {
            views.setViewVisibility(clockId, clockId == selectedId
                    ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        boolean nativeSecondHand = second && Build.VERSION.SDK_INT >= 37
                && setRemoteViewIcon(views, selectedId, "setSecondHand",
                        Icon.createWithResource(
                                context, R.drawable.analog_hand_second));
        views.setViewVisibility(secondFlipperId,
                second && !nativeSecondHand
                        ? android.view.View.VISIBLE : android.view.View.GONE);
        if (second && !nativeSecondHand) {
            views.setInt(secondFlipperId, "setDisplayedChild",
                    Calendar.getInstance().get(Calendar.SECOND));
        }
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
        SharedPreferences.Editor edit = prefs.edit();
        for (int i: appWidgetIds) {
            String thisWidget = "[" + String.valueOf(i) + "]";
            if (Objects.requireNonNull(widgetIds).contains(thisWidget)) {
                widgetIds = widgetIds.replace(thisWidget, "");
            }
            // Widget IDs can be reused; don't carry a deleted widget's name
            // over to a newly created widget with the same ID.
            edit.remove("W" + i + "name");
        }
        edit.putString("widgetIds", widgetIds).commit();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        /* debugging
        Toast.makeText(context, intent.getAction(),
            Toast.LENGTH_LONG).show();
        // */
        String action = intent.getAction();
        if (Objects.equals(action, "uk.co.yahoo.p1rpp.UpdateWidget")) {
            AppWidgetManager appWidgetManager =
                    AppWidgetManager.getInstance(context);
            onUpdate(context, appWidgetManager,
            intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS));
        } else if (Objects.equals(action,
                    AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
                || Objects.equals(action, Intent.ACTION_TIME_CHANGED)
                || Objects.equals(action, Intent.ACTION_DATE_CHANGED)
                || Objects.equals(action, Intent.ACTION_TIMEZONE_CHANGED)) {
            AppWidgetManager appWidgetManager =
                    AppWidgetManager.getInstance(context);
            int[] widgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, SecondsClockWidget.class));
            onUpdate(context, appWidgetManager, widgetIds);
        } else {
            super.onReceive(context, intent);
        }
    }
}
