/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This is the top level activity, launched when you touch tha app's icon.
 * It just offers you a choice of what to do next.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class MainActivity extends Activity_common
    implements View.OnClickListener, View.OnLongClickListener
{
    private static boolean m_configuring;
    private LinearLayout m_topLayout;
    private String m_widgetIds = "";

    // View IDs for buttons, to switch on click or long click:
    // also used as widget touch preference values, except that
    // CONFIGURE_NEW_WIDGET isn't a widget touch preference, and
    // CHOOSE_ACTION isn't a button, as we're already doing it if we get here.
    private static final int LONGPRESSHELP = 9091;
    private static final int GO_SYSTEM_CLOCK = LONGPRESSHELP + 1;
    private static final int CONFIGURE_NEW_WIDGET = GO_SYSTEM_CLOCK + 1;
    private static final int CONFIGURE_EXISTING_WIDGET = CONFIGURE_NEW_WIDGET + 1;
    private static final int CONFIGURE_NIGHT_CLOCK = CONFIGURE_EXISTING_WIDGET + 1;
    private static final int GO_NIGHT_CLOCK = CONFIGURE_NIGHT_CLOCK + 1;
    private static final int CHOOSE_ACTION = GO_NIGHT_CLOCK + 1;

    // Use best efforts to find the system clock app.
    private void goSystemClock() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                      | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
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
        // If that didn't work, try a well-known clock action.
        intent.setAction("android.intent.action.SHOW_TIMERS");
        intent.removeCategory(Intent.CATEGORY_DEFAULT);
        try {
            startActivity(intent);
            finish();
            return;
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case GO_SYSTEM_CLOCK:
                goSystemClock();
                break;
            case CONFIGURE_NEW_WIDGET:
                startActivity(new Intent(
                    this, WidgetConfigureActivity.class));
                break;
            case CONFIGURE_EXISTING_WIDGET:
                m_configuring = true;
                doToast(R.string.actionoldwidget);
                break;
            case CONFIGURE_NIGHT_CLOCK:
                startActivity(new Intent(
                    this, ClockConfigureActivity.class));
                break;
            case GO_NIGHT_CLOCK:
                startActivity(new Intent(
                    this, ClockActivity.class));
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
                doToast(R.string.helpsysclock); return true;
            case CONFIGURE_NEW_WIDGET:
                doToast(R.string.helpnewwidget); return true;
            case CONFIGURE_EXISTING_WIDGET:
                doToast(R.string.helpoldwidget); return true;
            case CONFIGURE_NIGHT_CLOCK:
                doToast(R.string.helpnightclock); return true;
            case GO_NIGHT_CLOCK:
                doToast(R.string.helprunclock); return true;
            default:
                return false;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            m_configuring = false;
        } else {
            m_widgetIds = m_prefs.getString("widgetIds", "");
            m_configuring = savedInstanceState.getBoolean("m_configuring");
        }
        LayoutInflater inflater =
            (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams")
        ViewGroup vg =
            (ViewGroup)inflater.inflate(R.layout.generic_layout, null);
        setContentView(vg);
        m_topLayout = new LinearLayout(this);
        m_topLayout.setOrientation(LinearLayout.VERTICAL);
        m_topLayout.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        vg.addView(m_topLayout);
        m_topLayout.setBackgroundColor(0xFF000000);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (intent.hasExtra("widgetID")) {
            // We got here because the user touched one of our widgets.
            int widgetID = intent.getIntExtra("widgetID", 0);
            if (m_configuring) {
                // We told the user to pick a widget, now configure it
                m_configuring = false;
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
                        startActivity(new Intent(
                            this, WidgetConfigureActivity.class));
                        finish();
                        break;
                    case CONFIGURE_EXISTING_WIDGET:
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
        }
        // CHOOSE_ACTION or we got launched: onResume will set up our screen.
    }

    @Override
    protected void onResume() {
        m_key = ""; // date preferences not used in this activity
        super.onResume();
        removeAllViews(m_topLayout);
        m_helptext = new TextView(this);
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
        m_helptext.setText(s + getString(R.string.longpresslabel));
        m_helptext.setId(LONGPRESSHELP);
        m_helptext.setOnLongClickListener(this);
        m_topLayout.addView(m_helptext);
        Button b = new Button(this);
        b.setText(R.string.gosysclock);
        b.setId(GO_SYSTEM_CLOCK);
        b.setAllCaps(false);
        b.setOnClickListener(this);
        b.setOnLongClickListener(this);
        m_topLayout.addView(b, centreButton);
        b = new Button(this);
        b.setText(R.string.confignewwidget);
        b.setId(CONFIGURE_NEW_WIDGET);
        b.setAllCaps(false);
        b.setOnClickListener(this);
        b.setOnLongClickListener(this);
        m_topLayout.addView(b, centreButton);
        if (m_widgetIds.length() > 0) {
            b = new Button(this);
            b.setText(R.string.configoldwwidget);
            b.setId(CONFIGURE_EXISTING_WIDGET);
            b.setAllCaps(false);
            b.setOnClickListener(this);
            b.setOnLongClickListener(this);
            m_topLayout.addView(b, centreButton);
        }
        b = new Button(this);
        b.setText(R.string.confignightclock);
        b.setId(CONFIGURE_NIGHT_CLOCK);
        b.setAllCaps(false);
        b.setOnClickListener(this);
        b.setOnLongClickListener(this);
        m_topLayout.addView(b, centreButton);
        b = new Button(this);
        b.setText(R.string.runnightclock);
        b.setId(GO_NIGHT_CLOCK);
        b.setAllCaps(false);
        b.setOnClickListener(this);
        b.setOnLongClickListener(this);
        m_topLayout.addView(b, centreButton);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // We haven't changed m_widgetIds, but this is a new bundle.
        outState.putBoolean("m_configuring", m_configuring);
    }
}
