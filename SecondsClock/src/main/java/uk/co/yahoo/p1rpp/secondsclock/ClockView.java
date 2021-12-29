/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This is the view which shows the fullscreen clock.  It is used in ClockActivity,
 * and also in ClockConfigureActivity to show what the clock will look like.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

class ClockView extends LinearLayout implements SensorEventListener {

    public String m_dateFormat;
    public String m_timeFormat;

    private final Context m_context;
    private final TextView m_dateView;
    private final Handler m_handler = new Handler();
    private final TextView m_secondsView;
    private final SensorManager m_sensorManager;
    private final SharedPreferences m_prefs;
    private final TextView m_timeView;

    private int m_fgColour;
    private boolean m_haveDate;
    private boolean m_haveSeconds;
    private Sensor m_lightSensor;
    private boolean m_visible;

    /* We set the foreground opacity of this view as the ratio of the current illumination
     * to the illumination of a typical brightly lit room (100 lux).
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            int opacity = (int) (event.values[0] * (255F / 100F));
            if (opacity < 1) {
                opacity = 1;
            } else if (opacity > 255) {
                opacity = 255;
            }
            setForegroundTintList(ColorStateList.valueOf(
                (opacity << 24) | (m_fgColour & 0xFFFFFF)));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private Calendar updateTime() {
        Calendar next = Calendar.getInstance(); // new one in case time zone changed
        m_timeView.setText(DateFormat.format(m_timeFormat, next));
        if (m_haveSeconds) {
            m_secondsView.setText(DateFormat.format("ss", next));
        }
        if (m_haveDate) {
            m_dateView.setText(DateFormat.format(m_dateFormat, next));
        }
        return next;
    }

    private final Runnable m_ticker = new Runnable() {
        public void run() {
            m_handler.removeCallbacks(this);
            Calendar next = updateTime();
            long now = next.getTimeInMillis();
            int secondsSize = m_prefs.getInt("CsecondsSize", 0);
            if (secondsSize > 0) {
                next.add(Calendar.SECOND, 1);
            } else {
                next.add(Calendar.MINUTE, 1);
                next.set(Calendar.SECOND, 0);
            }
            next.set(Calendar.MILLISECOND, 0);
            long offset = next.getTimeInMillis() - now;
            if (offset <= 0) {
                // should be impossible, but set 1 second just in case
                offset = 1000;
            }
            m_handler.postDelayed(this, offset);
        }
    };

    // recursive version
    void removeAllViews(View v) {
        if (v instanceof ViewGroup) {
            int n = ((ViewGroup)v).getChildCount();
            for ( int i = 0; i < n; ++i) {
                removeAllViews(((ViewGroup) v).getChildAt(i));
            }
            ((ViewGroup)v).removeAllViews();
        }
    }

    private String shortDateFormat() {
        char[] order = DateFormat.getDateFormatOrder(m_context);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 3; ++i) {
            if (i != 0) { result.append("/"); }
            switch (order[i]) {
                case 'd': result.append("dd"); break;
                case 'M': result.append("MM"); break;
                case 'y': result.append("yy"); break;
            }
        }
        return result.toString();
    }

    public void updateLayout() {
        removeAllViews(this);
        Configuration config = getResources().getConfiguration();
        boolean is24 = DateFormat.is24HourFormat(m_context);
        int secondsSize = m_prefs.getInt("CsecondsSize", 255);
        int showMonth = m_prefs.getInt("CshowMonth", 2); // long format
        int showMonthDay = m_prefs.getInt("CshowMonthDay",1);
        int showShortDate = m_prefs.getInt("CshowShortDate",0);
        int showWeekDay = m_prefs.getInt(
            "CshowWeekDay",2); // long format
        int showYear = m_prefs.getInt("CshowYear", 1);
        m_fgColour = m_prefs.getInt("CfgColour", 0xFFFFFFFF);
        Typeface font;
        if (m_prefs.getBoolean("C7seg", false)) {
            font = Typeface.createFromAsset(
                m_context.getAssets(), "DSEG7Classic-BoldItalic.ttf");
        } else {
            font = Typeface.DEFAULT;
        }
        m_timeView.setTypeface(font);
        m_secondsView.setTypeface(font);
        m_haveDate =
            showMonth + showMonthDay + showShortDate + showWeekDay + showYear > 0;
        setForegroundTintList(ColorStateList.valueOf(m_fgColour));
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            m_secondsView.setGravity(Gravity.CENTER);
            if (is24) {
                if (secondsSize == 255) {
                    m_haveSeconds = false;
                    m_timeFormat = "HH:mm:ss";
                } else {
                    m_timeFormat = "HH:mm";
                    m_haveSeconds = secondsSize > 0;
                }
            } else {
                if (secondsSize == 255) {
                    m_haveSeconds = false;
                    m_timeFormat = "h:mm:ss' 'A";
                } else {
                    m_timeFormat = "h:mm' 'A";
                    m_haveSeconds = secondsSize > 0;
                }
            }
            m_timeView.setLines(1);
            if (m_haveDate) {
                boolean first = true;
                StringBuilder sb = new StringBuilder();
                if (showWeekDay > 0) {
                    first = false;
                    if (showWeekDay == 1) {
                        sb.append("c");
                    } else {
                        sb.append("cccc");
                    }
                }
                if (showShortDate > 0) {
                    if (!first) { sb.append(" "); }
                    sb.append(shortDateFormat());
                } else {
                    if (showMonthDay > 0) {
                        if (first) { first = false; }
                        else { sb.append(" "); }
                        sb.append("d");
                    }
                    if (showMonth == 1) {
                        if (first) { first = false; }
                        else { sb.append(" "); }
                        sb.append("LLL");
                    } else if (showMonth == 2) {
                        if (first) { first = false; }
                        else { sb.append(" "); }
                        sb.append("LLLL");
                    }
                    if (showYear > 0) {
                        if (!first) { sb.append(" "); }
                        sb.append("yyyy");
                    }
                }
                m_dateFormat = sb.toString();
                m_dateView.setLines(1);
                m_ticker.run();
                if (m_haveSeconds) {
                    float timeWidth = m_timeView.getPaint().measureText(
                        String.valueOf(m_timeView.getText()));
                    float secondsWidth =
                        m_timeView.getPaint().measureText("00") * secondsSize / 255F;
                    LinearLayout ll = new LinearLayout(m_context);
                    ll.setOrientation(HORIZONTAL);
                    ll.addView(m_timeView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        secondsWidth / (timeWidth + secondsWidth)));
                    ll.addView(m_secondsView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        timeWidth / (timeWidth + secondsWidth)));
                    addView(ll, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT, 0.15F));
                    addView(m_dateView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT, 0.85F));
                } else {
                    addView(m_timeView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT, 0.15F));
                    addView(m_dateView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT, 0.85F));
                }
            } else {
                m_ticker.run();
                if (m_haveSeconds) {
                    setOrientation(HORIZONTAL);
                    addView(m_timeView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        secondsSize / 255F));
                    addView(m_secondsView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1F - secondsSize / 255F));
                } else {
                    addView(m_timeView);
                }
            }
        } else { // assume PORTRAIT
            m_secondsView.setGravity(Gravity.CENTER_HORIZONTAL);
            int nlines = 1;
            if (is24) {
                if (secondsSize == 255) {
                    nlines = 3;
                    m_haveSeconds = false;
                    m_timeFormat = "HH`\n`mm`\n`ss";
                } else {
                    nlines = 2;
                    m_timeFormat = "HH`\n`mm";
                    m_haveSeconds = secondsSize > 0;
                }
            } else {
                if (secondsSize == 255) {
                    nlines = 4;
                    m_haveSeconds = false;
                    m_timeFormat = "h`\n`mm`\n`ss'\n'A";
                } else {
                    nlines = 3;
                    m_timeFormat = "h`\n`mm`\n'A";
                    m_haveSeconds = secondsSize > 0;
                }
            }
            m_timeView.setLines(nlines);
            if (m_haveDate) {
                nlines = 0;
                StringBuilder sb = new StringBuilder();
                if (showWeekDay > 0) {
                    nlines = 1;
                    if (showWeekDay == 1) {
                        sb.append("c");
                    } else {
                        sb.append("cccc");
                    }
                }
                if (showShortDate > 0) {
                    if (nlines != 0) { sb.append("'\n'"); }
                    ++nlines;
                    sb.append(shortDateFormat());
                } else {
                    if (showMonthDay > 0) {
                        if (nlines != 0) { sb.append("'\n'"); }
                        ++nlines;
                        sb.append("d");
                    }
                    if (showMonth == 1) {
                        if (showMonthDay > 0) {
                            sb.append(" ");
                        } else {
                            if (nlines != 0) { sb.append("'\n'"); }
                            ++nlines;
                        }
                        sb.append("LLL");
                    } else if (showMonth == 2) {
                        if (nlines != 0) { sb.append("'\n'"); }
                        ++nlines;
                        sb.append("LLLL");
                    }
                    if (showYear > 0) {
                        if (nlines != 0) { sb.append("'\n'"); }
                        ++nlines;
                        sb.append("yyyy");
                    }
                }
                m_dateFormat = sb.toString();
                m_dateView.setLines(nlines);
                m_ticker.run();
                if (m_haveSeconds) {
                    addView(m_timeView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0.2F * secondsSize / 255F));
                    addView(m_secondsView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0.2F * (1F - secondsSize / 255F)));
                    addView(m_dateView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT, 0.8F));
                } else {
                    addView(m_timeView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT, 0.2F));
                    addView(m_dateView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT, 0.8F));
                }
            } else {
                m_ticker.run();
                if (m_haveSeconds) {
                    addView(m_timeView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        secondsSize / 255F));
                    addView(m_secondsView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1F - secondsSize / 255F));
                } else {
                    addView(m_timeView);
                }
            }
        }
    }

    public ClockView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(0xFF000000);
        m_context = context;
        m_dateView = new TextView(context);
        m_dateView.setAutoSizeTextTypeUniformWithConfiguration(
            10, 3000,
            3, TypedValue.COMPLEX_UNIT_PX);
        m_dateView.setGravity(Gravity.CENTER);
        m_prefs = context.getSharedPreferences("SecondsClock", Context.MODE_PRIVATE);
        m_secondsView = new TextView(context);
        m_secondsView.setAutoSizeTextTypeUniformWithConfiguration(
            10, 3000,
            3, TypedValue.COMPLEX_UNIT_PX);
        m_secondsView.setLines(1);
        m_sensorManager =
            (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (m_sensorManager != null) {
            m_lightSensor = m_sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }
        m_timeView = new TextView(context);
        m_timeView.setAutoSizeTextTypeUniformWithConfiguration(
            10, 3000,
            3, TypedValue.COMPLEX_UNIT_PX);
        m_timeView.setGravity(Gravity.CENTER);
        m_timeView.setIncludeFontPadding(false);
        m_visible = false;
        updateLayout();
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        if (isVisible != m_visible) {
            m_visible = isVisible;
            if (isVisible) {
                updateLayout();
                if (m_lightSensor != null) {
                    m_sensorManager.registerListener(
                        this, m_lightSensor, 1000000);
                }
                m_ticker.run();
            } else {
                m_handler.removeCallbacks(m_ticker);
                m_sensorManager.unregisterListener(this);
            }
        }
    }
}
