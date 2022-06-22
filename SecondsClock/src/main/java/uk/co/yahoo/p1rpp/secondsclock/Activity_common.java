/*
 * Copyright © 2022. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This contains a few bits of shared code for all the app's Activities
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

public abstract class Activity_common extends Activity implements View.OnLongClickListener {

    protected static final int CONFIGURE = 0; // m_currentView default value

    // View IDs for top level menu buttons, to switch on click or long click:
    // also used as widget touch preference values, except that
    // CONFIGURE_NEW_WIDGET isn't a widget touch preference, and
    // GO_SECONDS_CLOCK isn't a button because we're alread there,
    // and CHOOSE_ACTION is both a button in the widget configuration page
    // and a possible action when teh widget is touched.
    protected static final int LONGPRESSHELP = 9091;
    protected static final int GO_SYSTEM_CLOCK = LONGPRESSHELP + 1;
    protected static final int GO_SECONDS_CLOCK = GO_SYSTEM_CLOCK + 1;
    protected static final int CONFIGURE_NEW_WIDGET = GO_SECONDS_CLOCK + 1;
    protected static final int CONFIGURE_EXISTING_WIDGET =
        CONFIGURE_NEW_WIDGET + 1;
    protected static final int CONFIGURE_NIGHT_CLOCK =
        CONFIGURE_EXISTING_WIDGET + 1;
    protected static final int GO_NIGHT_CLOCK = CONFIGURE_NIGHT_CLOCK + 1;
    protected static final int CHOOSE_ACTION = GO_NIGHT_CLOCK + 1;
    protected static final int GO_EXIT = CHOOSE_ACTION + 1;

    protected AppWidgetManager appWidgetManager;
    protected ComponentName secondsClockWidget;
    protected TextView m_helptext;
    protected String m_key = null;
    protected FrameLayout m_topLayout;
    protected Activity m_activity = this;
    protected SharedPreferences m_prefs;
    protected int showWeekDay;
    protected int showShortDate;
    protected int showMonthDay;
    protected int showMonth;
    protected int showYear;
    protected float m_density;
    protected int m_width = 0;
    protected int m_height = 0;
    protected int m_orientation;

    // These are the foreground and background colours of the UI,
    // not the widget or the clock, whose colours are user-configurable.
    protected int m_background;
    protected int m_foreground;

    protected TextView textLabel(CharSequence cs, int id) {
        TextView tv = new TextView(this);
        tv.setText(cs);
        tv.setId(id);
        tv.setOnLongClickListener(this);
        return tv;
    }

    protected TextView textLabel(int resid, int id) {
        return textLabel(getString(resid), id);
    }

    protected TextView centredLabel(int resid, int id) {
        TextView result = textLabel(getString(resid), id);
        result.setGravity(Gravity.CENTER_HORIZONTAL);
        result.setPadding(0, (int)(10 * m_density), 0, 0);
        return result;
    }

    protected void doToast(String s) {
        Toast.makeText(m_activity, s, Toast.LENGTH_LONG).show();
    }

    protected void doToast(int resId) {
        Toast.makeText(m_activity, getString(resId), Toast.LENGTH_LONG).show();
    }

    protected void doToast(int resId, String arg) {
        Toast.makeText(m_activity, getString(resId, arg), Toast.LENGTH_LONG).show();
    }

    // recursive version
    protected void removeAllViews(View v) {
        if (v instanceof ViewGroup) {
            int n = ((ViewGroup)v).getChildCount();
            for ( int i = 0; i < n; ++i) {
                removeAllViews(((ViewGroup)v).getChildAt(i));
            }
            ((ViewGroup)v).removeAllViews();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_prefs = getSharedPreferences("SecondsClock", Context.MODE_PRIVATE);
        setContentView(R.layout.generic_layout);
    }

    protected void getDatePrefs() {
    }

    @Override
    protected void onStart() {
        super.onStart();
        appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        secondsClockWidget = new ComponentName(
            getApplicationContext(), SecondsClockWidget.class);
    }

    // This is the real resume after we've found our dimensions
    protected void resume() {
        Resources res = getResources();
        DisplayMetrics metrics = res.getDisplayMetrics();
        m_density = metrics.density;
        Configuration config = res.getConfiguration();
        m_orientation = config.orientation;
        switch (config.uiMode & Configuration.UI_MODE_NIGHT_MASK) {
            case Configuration.UI_MODE_NIGHT_NO:
                m_background = 0xFFFFFFFF;
                m_foreground = 0xFF000000;
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                m_background = 0xFF000000;
                m_foreground = 0xFFFFFFFF;
            default:
        }
        // m_key if needed is set before derived class calls super.resume()
        if (m_key == null) { return; }
        showWeekDay = m_prefs.getInt(
            m_key + "showWeekDay",2); // long format
        showShortDate = m_prefs.getInt(m_key + "showShortDate",0);
        showMonthDay = m_prefs.getInt(m_key + "showMonthDay",1);
        showMonth = m_prefs.getInt(m_key + "showMonth", 2); // long format
        showYear = m_prefs.getInt(m_key + "showYear", 1);
        m_topLayout = findViewById(R.id.genericlayout);
        m_topLayout.setBackgroundColor(m_background);
        m_topLayout.setForegroundTintList(ColorStateList.valueOf(m_foreground));
    }

    public void setWidthAndHeight(int width, int height) {
        m_width = width;
        m_height = height;
        resume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_topLayout = findViewById(R.id.genericlayout);
        removeAllViews(m_topLayout);
        SizingView sizingView = new SizingView(this);
        m_topLayout.addView(sizingView);
        /* When the SizingView gets asked to lay itself out,
         * it will post a call to setWidthAndHeight, which will call resume().
         */
    }
}
