package uk.co.yahoo.p1rpp.secondsclock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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

public class ClockActivity extends Activity implements SensorEventListener {

    private Calendar m_now;
    private TextView m_timeView;
    private String m_timeFormat;
    private TextView m_smallSeconds;
    private final Handler m_handler = new Handler();
    private int m_showSeconds;
    private SensorManager sensorManager;
    private Sensor luxSensor = null;
    private SharedPreferences m_prefs;
    private Configuration m_config;
    private TextView m_date;

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
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void updateTime() {
        m_now = Calendar.getInstance(); // new one in case time zone changed
        m_timeView.setText(DateFormat.format(m_timeFormat, m_now));
        if (m_smallSeconds != null) {
            m_smallSeconds.setText(DateFormat.format("ss", m_now));
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
        super.onResume();
        m_prefs = getSharedPreferences("SecondsClock", Context.MODE_PRIVATE);
        m_showSeconds = m_prefs.getInt(("CshowSeconds"), 2);
        int showWeekDay = m_prefs.getInt(
            "WshowWeekDay",2); // long format
        int showShortDate = m_prefs.getInt("WshowShortDate",0);
        int showMonthDay = m_prefs.getInt("WshowMonthDay",1);
        int showMonth = m_prefs.getInt("WshowMonth", 2); // long format
        int showYear = m_prefs.getInt("WshowYear", 1);
        Resources res = getResources();
        m_config = res.getConfiguration();
        FrameLayout topLayout = findViewById(R.id.genericlayout);
        m_timeView = new TextView(this);
        m_timeView.setAutoSizeTextTypeUniformWithConfiguration(
            10, 3000,
            3, TypedValue.COMPLEX_UNIT_PX);
        m_timeView.setGravity(Gravity.CENTER_HORIZONTAL);
        int nlines = 0;
        View v1;
        boolean is24 = DateFormat.is24HourFormat(this);
        switch (m_showSeconds) {
            case 0: // no seconds
                m_timeFormat = is24 ? "HH:mm" : "h:mm a";
                m_smallSeconds = null;
                nlines = 1;
                v1 = m_timeView;
                break;
            case 1: // small seconds
                m_timeFormat = is24 ? "HH:mm" : "h:mm a";
                m_smallSeconds = new TextView(this);
                m_smallSeconds.setAutoSizeTextTypeWithDefaults(
                    TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
                LinearLayout ll = new LinearLayout(this);
                if (m_config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                {
                    ll.setOrientation(LinearLayout.HORIZONTAL);
                    nlines = 1;
                } else {
                    ll.setOrientation(LinearLayout.VERTICAL);
                    nlines = 2;
                }
                ll.addView(m_timeView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, 0.67F));
                ll.addView(m_smallSeconds, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, 0.33F));
                v1 = ll;
                break;
            default: // large seconds
                if (m_config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                {
                    m_timeFormat = is24 ? "HH:mm:ss" : "h:mm:ss' 'a";
                    nlines = 1;
                } else { // ORIENTATION_PORTRAIT
                    m_timeFormat = is24 ? "HH:mm'\n'ss" : "h:mm:ss'\n'a";
                    nlines = 2;
                }
                m_smallSeconds = null;
                m_timeView.setLines(nlines);
                v1 = m_timeView;
        }
        if (showWeekDay + showShortDate + showMonthDay + showMonth + showYear == 0)
        {
            m_date = null;
            topLayout.addView(v1, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            // just for testing
            m_date = null;
            topLayout.addView(v1, new LinearLayout.LayoutParams(
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
