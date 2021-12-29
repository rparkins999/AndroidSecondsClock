/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This is the activity which runs the fullscreen clock.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.Calendar;

public class ClockActivity extends Activity_common
    implements SensorEventListener, View.OnLongClickListener
{

    private Calendar m_now;
    private TextView m_timeView;
    private String m_timeFormat;
    private TextView m_SecondsView;
    private final Handler m_handler = new Handler();
    private int m_showSeconds;
    private SensorManager sensorManager;
    private Sensor luxSensor = null;
    private Configuration m_config;
    private TextView m_date;
    private FrameLayout m_topLayout;
    private int m_fgColour;

    /* We adjust the screen brightness to be the user's preferred brightness
     * reduced by the ratio of the current illumination
     * to the illumination of a typical brightly lit room (100 lux).
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float oldBrightness;
            try {
                oldBrightness = Settings.System.getInt(
                    getContentResolver(), Settings.System.SCREEN_BRIGHTNESS) / 255F;
            } catch (Settings.SettingNotFoundException e) {
                oldBrightness = 1.0F;
            }
            float br = event.values[0] / 100F;
            if (br > oldBrightness) { br = oldBrightness; }
            Window window = getWindow();
            if (window != null) {
                WindowManager.LayoutParams lp = window.getAttributes();
                lp.screenBrightness = br; // set screen brightness
                window.setAttributes(lp);
            }
            // This only works on HDR screens
            int colour = m_fgColour;
            if (br < 1F / 255F) {
                int opacity = (int)(br * 255);
                if (opacity == 0) { opacity = 1; }
                colour = (opacity << 24) | (colour & 0xFFFFFF);
            }
            m_topLayout.setForegroundTintList(ColorStateList.valueOf(colour));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    private void updateTime() {
        m_now = Calendar.getInstance(); // new one in case time zone changed
        m_timeView.setText(DateFormat.format(m_timeFormat, m_now));
        if (m_SecondsView != null) {
            m_SecondsView.setText(DateFormat.format("ss", m_now));
        }
    }

    private final Runnable m_ticker = new Runnable() {
        public void run() {
            m_handler.removeCallbacks(this);
            updateTime();
            long offset = m_now.getTimeInMillis();
            if (m_showSeconds > 0) {
                m_now.add(Calendar.SECOND, 1);
            } else {
                m_now.add(Calendar.MINUTE, 1);
                m_now.set(Calendar.SECOND, 0);
            }
            m_now.set(Calendar.MILLISECOND, 0);
            offset = m_now.getTimeInMillis() - offset;
            if (offset <= 0) {
                // should be impossible, but set 1 second just in case
                offset = 1000;
            }
            m_handler.postDelayed(this, offset);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.generic_layout);
        sensorManager =
            (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            luxSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }
    }

    @Override
    protected void onResume() {
        m_key = "C";
        super.onResume();
        m_showSeconds = m_prefs.getInt(("CshowSeconds"), 1);
        m_fgColour = m_prefs.getInt("CfgColour", 0xFFFFFFFF);
        Resources res = getResources();
        m_config = res.getConfiguration();
        m_topLayout = findViewById(R.id.genericlayout);
        removeAllViews(m_topLayout);
        m_topLayout.setBackgroundTintList(ColorStateList.valueOf(0xFF000000));
        m_timeView = new TextView(this);
        m_timeView.setAutoSizeTextTypeUniformWithConfiguration(
            10, 3000,
            3, TypedValue.COMPLEX_UNIT_PX);
        m_timeView.setGravity(Gravity.CENTER_HORIZONTAL);
        int nlines;
        View v1;
        boolean is24 = DateFormat.is24HourFormat(this);
        switch (m_showSeconds) {
            case 0: // no seconds
                if (m_config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                {
                    m_timeFormat = is24 ? "HH:mm" : "h:mm' 'a";
                    nlines = 1;
                } else { // ORIENTATION_PORTRAIT
                    if (is24) {
                        m_timeFormat = "HH'\n'mm";
                        nlines = 2;
                    } else {
                        m_timeFormat = "h'\n'mm'\n'a";
                        nlines = 3;
                    }
                }
                m_SecondsView = null;
                v1 = m_timeView;
                break;
            case 1: // small seconds
                if (m_config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                {
                    m_timeFormat = is24 ? "HH:mm" : "h:mm' 'a";
                    nlines = 1;
                } else { // ORIENTATION_PORTRAIT
                    if (is24) {
                        m_timeFormat = "HH'\n'mm";
                        nlines = 3;
                    } else {
                        m_timeFormat = "h'\n'mm'\n'a";
                        nlines = 4;
                    }
                }
                m_SecondsView = new TextView(this);
                m_SecondsView.setAutoSizeTextTypeWithDefaults(
                    TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
                LinearLayout ll = new LinearLayout(this);
                if (m_config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                {
                    ll.setOrientation(LinearLayout.HORIZONTAL);
                    m_SecondsView.setGravity(Gravity.CENTER);
                    nlines = 1;
                } else {
                    ll.setOrientation(LinearLayout.VERTICAL);
                    m_SecondsView.setGravity(Gravity.CENTER_HORIZONTAL);
                    nlines = 2;
                }
                ll.addView(m_timeView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, 0.2F));
                ll.addView(m_SecondsView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, 0.8F));
                v1 = ll;
                break;
            default: // large seconds
                if (m_config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                {
                    m_timeFormat = is24 ? "HH:mm:ss" : "h:mm:ss' 'a";
                    nlines = 1;
                } else { // ORIENTATION_PORTRAIT
                    if (is24) {
                        m_timeFormat = "HH'\n'mm'\n'ss";
                        nlines = 3;
                    } else {
                        m_timeFormat = "h'\n'mm'\n'ss'\n'a";
                        nlines = 4;
                    }
                }
                m_SecondsView = null;
                v1 = m_timeView;
        }
        m_timeView.setLines(nlines);
        if (showWeekDay + showShortDate + showMonthDay + showMonth + showYear == 0)
        {
            m_date = null;
            m_topLayout.addView(v1, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            // just for testing
            m_date = null;
            m_topLayout.addView(v1, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        }

        if (luxSensor != null) {
            sensorManager.registerListener(
                this, luxSensor, 1000000);
        }
        m_ticker.run();
    }

    /* Touching the screen makes the clock go away.
     * This is because the back button may be too dimmed down to be visible.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            finish();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onPause() {
        super.onPause();
        m_handler.removeCallbacks(m_ticker);
        sensorManager.unregisterListener(this);
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.screenBrightness = -1.0F; // restore user's screen brightness
            window.setAttributes(lp);
        }
    }
}
