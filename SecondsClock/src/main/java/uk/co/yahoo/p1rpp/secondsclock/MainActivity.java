/*
 * Copyright © 2022. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This is the top level activity, launched when you touch tha app's icon.
 * It just offers you a choice of what to do next.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity_common
    implements View.OnClickListener, View.OnLongClickListener
{
    private int widgetID;
    private static final int CONFIGURE_THIS_WIDGET = GO_EXIT + 1;

    // Use best efforts to find the system clock app.
    private void goSystemClock() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                      | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        // first try some well-known clock app names
        try {
            intent.setClassName("com.android.deskclock",
                "com.android.deskclock.DeskClock");
            startActivity(intent);
            finish();
            return;
        } catch (Exception ignore) {}
        try {
            intent.setClassName("com.sec.android.app.clockpackage",
                "com.sec.android.app.clockpackage.Clockpackage");
            startActivity(intent);
            finish();
            return;
        } catch (Exception ignore) {}
        intent.removeCategory(Intent.CATEGORY_LAUNCHER);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        try {
            intent.setClassName("com.android.deskclock",
                "com.android.deskclock.DeskClock");
            startActivity(intent);
            finish();
            return;
        } catch (Exception ignore) {}
        try {
            intent.setClassName("com.sec.android.app.clockpackage",
                "com.sec.android.app.clockpackage.Clockpackage");
            startActivity(intent);
            finish();
            return;
        } catch (Exception ignore) {}
        // If that didn't work, try a well-known clock action.
        intent = new Intent("android.intent.action.SHOW_TIMERS");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                      | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        try {
            startActivity(intent);
            finish();
        } catch (SecurityException ignore) {
            // we needed permission.SET_ALARM and we don't have it.
            doToast(R.string.noclockpermission);
        } catch (Exception ignore) {
            doToast(R.string.nosysclock);
        }
        // We don't call finish here as we do if we can launch the system
        // clock app. If we can't find it we stay in this activity
        // so that the user can do something else.
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case GO_SYSTEM_CLOCK:
                goSystemClock();
                break;
                // GO_SECONDS_CLOCK isn't a main menu button
            case CONFIGURE_NEW_WIDGET:
                m_prefs.edit().putInt("Wview", CONFIGURE).commit();
                startActivity(new Intent(
                    this, WidgetConfigureActivity.class));
                break;
            case CONFIGURE_THIS_WIDGET:
                m_prefs.edit().putInt("Wview", CONFIGURE).commit();
                startActivity(
                    new Intent(this, WidgetConfigureActivity.class)
                        .putExtra("widgetID", widgetID));
                finish();
                break;
            case CONFIGURE_EXISTING_WIDGET:
                m_prefs.edit().putInt("Wview", CONFIGURE)
                    .putInt("Wconfiguring", 1).commit();
                doToast(R.string.actionoldwidget);
                Intent homeIntent= new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(homeIntent);
                finish();
                break;
            case CONFIGURE_NIGHT_CLOCK:
                m_prefs.edit().putInt("Cview", CONFIGURE).commit();
                startActivity(new Intent(
                    this, ClockConfigureActivity.class));
                break;
            case GO_NIGHT_CLOCK:
                startActivity(new Intent(
                    this, ClockActivity.class));
                finish();
                break;
            case GO_EXIT:
                finish();
                break;
            default: v.performClick();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case LONGPRESSHELP:
                doToast(R.string.mainactivityhelp); return true;
            case GO_SYSTEM_CLOCK:
                doToast(R.string.helpgosysclock); return true;
            // GO_SECONDS_CLOCK isn't a main menu button
            case CONFIGURE_NEW_WIDGET:
                doToast(R.string.helpconfignewwidget); return true;
            case CONFIGURE_THIS_WIDGET:
                doToast(R.string.helpconfigthis); return true;
            case CONFIGURE_EXISTING_WIDGET:
                doToast(R.string.helpoldwidget); return true;
            case CONFIGURE_NIGHT_CLOCK:
                doToast(R.string.helpconfignightclock); return true;
            case GO_NIGHT_CLOCK:
                doToast(R.string.helprunnightclock); return true;
            case GO_EXIT:
                doToast(R.string.exithelp); return true;
            default:
                return false;
        }
    }

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (intent.hasExtra("widgetID")) {
            // We got here because the user touched one of our widgets.
            widgetID = intent.getIntExtra("widgetID", -1);
            if (m_prefs.getInt("Wconfiguring", 0) != 0) {
                // We told the user to pick a widget, now configure it
                m_prefs.edit().putInt("Wview", CONFIGURE)
                    .putInt("Wconfiguring", 0).commit();
                startActivity(
                    new Intent(this, WidgetConfigureActivity.class)
                    .putExtra("widgetID", widgetID));
                finish();
            } else {
                // Do the touch action for the widget that called us.
                String key = "W" + widgetID + "touchaction";
                switch (m_prefs.getInt(key, CHOOSE_ACTION)) {
                    case GO_SYSTEM_CLOCK:
                        goSystemClock();
                        break;
                    case CONFIGURE_NEW_WIDGET:
                        m_prefs.edit().putInt("Wview", CONFIGURE).commit();
                        startActivity(new Intent(
                            this, WidgetConfigureActivity.class));
                        finish();
                        break;
                    case CONFIGURE_EXISTING_WIDGET:
                        m_prefs.edit().putInt("Wview", CONFIGURE).commit();
                        startActivity(new Intent(
                            this, WidgetConfigureActivity.class)
                            .putExtra("widgetID", widgetID));
                        finish();
                        break;
                    case CONFIGURE_NIGHT_CLOCK:
                        startActivity(new Intent(
                            this, ClockConfigureActivity.class));
                        finish();
                        break;
                    case GO_NIGHT_CLOCK:
                        startActivity(new Intent(
                            this, ClockActivity.class));
                        finish();
                        break;
                    default:
                }
            }
        } else {
            m_prefs.edit().putInt("Wconfiguring", 0).commit();
            widgetID = -1;
            // Update our widgets in case we've been reinstalled
            int[] widgetIds
                = appWidgetManager.getAppWidgetIds(secondsClockWidget);
            if (widgetIds.length > 0) {
                intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.setClassName("uk.co.yahoo.p1rpp.secondsclock",
                    "uk.co.yahoo.p1rpp.secondsclock.SecondsClockWidget");
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
                sendBroadcast(intent);
            }
        }
        // CHOOSE_ACTION or GO_SECONDS_CLOCK or we got launched:
        // onResume will set up our screen.
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void resume() {
        m_key = ""; // date preferences not used in this activity
        super.resume();
        removeAllViews(m_topLayout);
        m_helptext = new TextView(this);
        ScrollView lscroll = new ScrollView(this);
        lscroll.setScrollbarFadingEnabled(false);
        LinearLayout lmain = new LinearLayout(this);
        lmain.setOrientation(LinearLayout.VERTICAL);
        lmain.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        LinearLayout.LayoutParams centreButton = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        centreButton.gravity = Gravity.CENTER_HORIZONTAL;
        String s = "";
        try
        {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName (), 0);
            s = getString(R.string.app_name) + " " + pi.versionName
                + " built " + getString(R.string.build_time) + "\n";
        } catch (PackageManager.NameNotFoundException ignore) {}
        m_helptext.setText(s + getString(R.string.longpresshoriz));
        m_helptext.setId(LONGPRESSHELP);
        m_helptext.setOnLongClickListener(this);
        lmain.addView(m_helptext);
        Button b = new Button(this);
        b.setText(R.string.gosysclock);
        b.setId(GO_SYSTEM_CLOCK);
        b.setAllCaps(false);
        b.setOnClickListener(this);
        b.setOnLongClickListener(this);
        lmain.addView(b, centreButton);
        b = new Button(this);
        b.setText(R.string.confignewwidget);
        b.setId(CONFIGURE_NEW_WIDGET);
        b.setAllCaps(false);
        b.setOnClickListener(this);
        b.setOnLongClickListener(this);
        lmain.addView(b, centreButton);
        if (widgetID != -1) {
            b = new Button(this);
            b.setText(R.string.configwidget);
            b.setId(CONFIGURE_THIS_WIDGET);
            b.setAllCaps(false);
            b.setOnClickListener(this);
            b.setOnLongClickListener(this);
            lmain.addView(b, centreButton);
        }
        String m_widgetIds = m_prefs.getString("widgetIds", "");
        if (m_widgetIds.length() > 0) {
            b = new Button(this);
            b.setText(R.string.configoldwwidget);
            b.setId(CONFIGURE_EXISTING_WIDGET);
            b.setAllCaps(false);
            b.setOnClickListener(this);
            b.setOnLongClickListener(this);
            lmain.addView(b, centreButton);
        }
        b = new Button(this);
        b.setText(R.string.confignightclock);
        b.setId(CONFIGURE_NIGHT_CLOCK);
        b.setAllCaps(false);
        b.setOnClickListener(this);
        b.setOnLongClickListener(this);
        lmain.addView(b, centreButton);
        b = new Button(this);
        b.setText(R.string.runnightclock);
        b.setId(GO_NIGHT_CLOCK);
        b.setAllCaps(false);
        b.setOnClickListener(this);
        b.setOnLongClickListener(this);
        lmain.addView(b, centreButton);
        b = new Button(this);
        b.setText(R.string.exit);
        b.setId(GO_EXIT);
        b.setAllCaps(false);
        b.setOnClickListener(this);
        b.setOnLongClickListener(this);
        lmain.addView(b, centreButton);
        lscroll.addView(lmain);
        m_topLayout.addView(lscroll);
    }
}
