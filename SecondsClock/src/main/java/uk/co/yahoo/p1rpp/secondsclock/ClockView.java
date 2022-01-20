/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This is the view which shows the fullscreen clock.  It is used in ClockActivity,
 * and also in ClockConfigureActivity to show what the clock will look like.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.app.Activity;
import android.content.ContentResolver;
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
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

class ClockView extends LinearLayout implements SensorEventListener {

    // This is the ambient light level (in lux)
    // above which we display at the user's set brightness.
    private final static float MAXLIGHT = 255F;

    public String m_dateFormat = " ";
    public String m_hoursFormat = " ";
    public String m_monthFormat =  " ";
    public String m_timeFormat = " ";
    public String m_weekdayFormat = " ";

    private final TextView m_ampmView;
    private final Activity m_owner;
    private final TextView m_dateView;
    private final Handler m_handler = new Handler();
    private final TextView m_hoursView;
    private final TextView m_minutesView;
    private final TextView m_monthdayView;
    private final TextView m_monthView;
    private final TextView m_secondsView;
    private final SensorManager m_sensorManager;
    private final SharedPreferences m_prefs;
    private final TextView m_timeView;
    private final TextView m_weekdayView;
    private final TextView m_yearView;

    private int m_fgColour;
    private int m_height;
    public float m_lightLevel = MAXLIGHT;
    private Sensor m_lightSensor;
    private boolean m_trim = false; // chop off last char of time
    private boolean m_visible;

    // Set the colour of all of our display objects.
    private void setColour(int colour) {
        m_ampmView.setTextColor(colour);
        m_dateView.setTextColor(colour);
        m_hoursView.setTextColor(colour);
        m_minutesView.setTextColor(colour);
        m_monthdayView.setTextColor(colour);
        m_monthView.setTextColor(colour);
        m_secondsView.setTextColor(colour);
        m_timeView.setTextColor(colour);
        m_weekdayView.setTextColor(colour);
        m_yearView.setTextColor(colour);
    }

    /* This adjusts the colour and brightness of the clock display.
     * It can be called from our configuration screen
     * or from onSensorChanged when the ambient light level changes.
     *
     * The minimum brightness comes from our configuration screen and
     * the maximum brightness comes from the user's settings page. We vary
     * the actual brightness between them according to the ambient light level.
     *
     * We used a mixed algorithm, partly using the overall screen brightness
     * in order to save power on backlit displays, and partly using the opacity
     * of the actual TextViews in order to make use of high dynamic resolution
     * if the device supports it.
     *
     * On the configuration screen we can't do this because reducing the overall
     * screen brightness would dim the controls as well.
     */
    public void adjustColour() {
        int minbright= m_prefs.getInt("Cbrightness", 255);
        ContentResolver cr = m_owner.getContentResolver();
        int userbright = Settings.System.getInt(
            cr, Settings.System.SCREEN_BRIGHTNESS, 255);
        float alpha;
        if (m_owner instanceof ClockConfigureActivity) { // not full screen
            alpha = (255F * m_lightLevel + minbright * (255F - m_lightLevel))
                    / 255F;
        } else { // full screen
            if (minbright < userbright) {
                alpha = (userbright * m_lightLevel + minbright * (255F - m_lightLevel))
                        / 255F;
            } else { alpha = userbright; }
            alpha = (float)Math.sqrt(alpha * 255F);
            if (alpha > 255F) { alpha = 255F; }
            if (alpha < 1F) { alpha = 1F; }
            Window w = m_owner.getWindow();
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.screenBrightness = alpha / 255F;
            w.setAttributes(lp);
        }
        setColour(((int)alpha << 24) | (m_fgColour & 0xFFFFFF));
    }

    // This gets the current ambient light level in lux and limits it to MAXLIGHT,
    // the light level in a typical brightly lit room.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            m_lightLevel = Math.min(MAXLIGHT, event.values[0]);
            adjustColour();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private Calendar updateTime() {
        Calendar next = Calendar.getInstance(); // new one in case time zone changed
        CharSequence ampm = DateFormat.format("a", next);
        CharSequence time = DateFormat.format(m_timeFormat, next);
        // Remove this bit of code if you're happy with the default for en_GB
        // of lower-case am/pm, which I don't like.
        if (Locale.getDefault().getDisplayName().equals("English (United Kingdom)")) {
            if (m_trim) {
                // We're in AM/PM mode with the seven segment font,
                // which doesn't have a decent "M", so we remove it.
                time = time.subSequence(0, time.length() - 1);
                ampm = ampm.subSequence(0, ampm.length() - 1);
            }
            m_ampmView.setAllCaps(true);
            m_timeView.setAllCaps(true);
        } else {
            m_ampmView.setAllCaps(false);
            m_timeView.setAllCaps(false);
        }
        m_ampmView.setText(ampm);
        m_dateView.setText(DateFormat.format(m_dateFormat, next));
        m_hoursView.setText(DateFormat.format(m_hoursFormat, next));
        m_minutesView.setText(DateFormat.format("mm", next));
        m_monthdayView.setText(DateFormat.format("dd", next));
        m_monthView.setText(DateFormat.format(m_monthFormat, next));
        m_secondsView.setText(DateFormat.format("ss", next));
        m_timeView.setText(time);
        m_weekdayView.setText(DateFormat.format(m_weekdayFormat, next));
        m_yearView.setText(DateFormat.format("yyyy", next));
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

    public void setHeight (int height) { m_height = height; }

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
        char[] order = DateFormat.getDateFormatOrder(m_owner);
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
        boolean is24 = DateFormat.is24HourFormat(m_owner);
        int m_secondsSize = m_prefs.getInt("CsecondsSize", 255);
        int showMonth = m_prefs.getInt("CshowMonth", 2); // long format
        int showMonthDay = m_prefs.getInt("CshowMonthDay",1);
        int showShortDate = m_prefs.getInt("CshowShortDate",0);
        int showWeekDay = m_prefs.getInt(
            "CshowWeekDay",2); // long format
        int showYear = m_prefs.getInt("CshowYear", 1);
        m_fgColour = m_prefs.getInt("CfgColour", 0xFFFFFFFF);
        Typeface font;
        float lsp = 1.0F;
        if (m_prefs.getBoolean("C7seg", false)) {
            font = Typeface.createFromAsset(
            m_owner.getAssets(), "DSEG7Classic-BoldItalic.ttf");
            // The seven segment font has no line spacing....
            lsp = 1.1F;
            m_trim = !is24;
        } else {
            font = Typeface.DEFAULT;
            m_trim = false;
        }
        m_ampmView.setTypeface(font);
        m_hoursView.setTypeface(font);
        m_minutesView.setTypeface(font);
        m_secondsView.setTypeface(font);
        m_secondsView.setGravity(Gravity.CENTER);
        m_timeView.setTypeface(font);
        boolean m_haveDate =
            showMonth + showMonthDay + showShortDate + showWeekDay + showYear > 0;
        setForegroundTintList(ColorStateList.valueOf(m_fgColour));
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            boolean haveSeconds;
            if (m_secondsSize == 255) {
                haveSeconds = false;
                if (is24) {
                    m_timeFormat = "HH:mm:ss";
                } else {
                    m_timeFormat = "h:mm:ss a";
                }
            } else {
                haveSeconds = m_secondsSize > 0;
                if (is24) {
                    m_timeFormat = "HH:mm";
                } else {
                    m_timeFormat = "h:mm a";
                }
            }
            if (m_haveDate) {
                setOrientation(VERTICAL);
                boolean first = true;
                StringBuilder sb = new StringBuilder();
                if (showWeekDay > 0) {
                    first = false;
                    if (showWeekDay == 1) {
                        sb.append("EEE");
                    } else {
                        sb.append("EEEE");
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
                if (haveSeconds) {
                    float timeWidth = m_timeView.getPaint().measureText(
                        String.valueOf(m_timeView.getText()));
                    float secondsWidth =
                        m_timeView.getPaint().measureText("00") * m_secondsSize / 255F;
                    LinearLayout ll = new LinearLayout(m_owner);
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
                if (haveSeconds) {
                    setOrientation(HORIZONTAL);
                    float timeWidth = m_timeView.getPaint().measureText(
                        String.valueOf(m_timeView.getText()));
                    float secondsWidth =
                        m_timeView.getPaint().measureText("00") * m_secondsSize / 255F;
                    addView(m_timeView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        secondsWidth / (timeWidth + secondsWidth)));
                    addView(m_secondsView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        timeWidth / (timeWidth + secondsWidth)));
                } else {
                    addView(m_timeView, new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                }
            }
        } else { // assume PORTRAIT
            setOrientation(VERTICAL);
            float space = 0;
            boolean horizontalTime =
                (   (showMonth == 2) || (showWeekDay == 2) || (showShortDate > 0))
                 && !(m_prefs.getBoolean("Cforcevertical", false));
            // first work out how much space we need
            boolean haveSeconds = false;
            if (horizontalTime)
            {
                if (m_secondsSize == 255) {
                    if (is24) {
                        m_timeFormat = "HH:mm:ss";
                    } else {
                        m_timeFormat = "h:mm:ss a";
                    }
                } else {
                    haveSeconds = m_secondsSize > 0;
                    if (is24) {
                        m_timeFormat = "HH:mm";
                    } else {
                        m_timeFormat = "h:mm a";
                    }
                }
                space += lsp;
            } else {
                if (is24) {
                    m_hoursFormat = "HH";
                    space += 2F + lsp;  // hours + minutes (fixed format)
                } else {
                    m_hoursFormat = "KK";  // hours + minutes (fixed format) + AM/PM
                    space += 3F * lsp;
                }
            }
            if (m_secondsSize > 0) { space += (lsp * m_secondsSize) / 255F; }
            if (showWeekDay == 1) { space += 1F; m_weekdayFormat = "EEE"; }
            else if (showWeekDay == 2) { space += 1F; m_weekdayFormat = "EEEE"; }
            if (showMonthDay != 0) { space += 1F; }
            if (showMonth == 1) { space += 1F; m_monthFormat = "LLL"; }
            else if (showMonth == 2) { space += 1F; m_monthFormat = "LLLL"; }
            if (showYear != 0) { space += 1F; }
            // now add the views
            LinearLayout.LayoutParams lpmm = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int)(m_height / space));
            if (horizontalTime) {
                if (haveSeconds) {
                    float timeWidth = m_timeView.getPaint().measureText(
                        String.valueOf(m_timeView.getText()));
                    float secondsWidth =
                        m_timeView.getPaint().measureText("00") * m_secondsSize / 255F;
                    LinearLayout ll = new LinearLayout(m_owner);
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
                        (int) (lsp * m_height / space)));
                } else {
                    addView(m_timeView, new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) (lsp * m_height / space)));
                }
            } else {
                addView(m_hoursView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, (int) (m_height / space)));
                if (lsp > 1F) {
                    addView(new View(m_owner), new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) (((lsp - 1F) * m_height) / space)));
                }
                addView(m_minutesView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, (int) (m_height / space)));
                if (m_secondsSize > 0) {
                    if (lsp > 1F) {
                        addView(new View(m_owner), new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            (int) (((lsp - 1F) * m_height) / space)));
                    }
                    addView(m_secondsView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) ((m_height * m_secondsSize) / (space * 255F))));
                }
                if (!is24) {
                    if (lsp > 1F) {
                        addView(new View(m_owner), new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            (int) (((lsp - 1F) * m_height) / space)));
                    }
                    addView(m_ampmView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (int) (m_height / space)));
                }
            }
            if (showWeekDay > 0) {
                addView(m_weekdayView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, (int) (m_height / space)));
            }
            if (showShortDate > 0) {
                addView(m_dateView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, (int) (m_height / space)));
            } else {
                if (showMonthDay > 0) {
                    addView(m_monthdayView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (int) (m_height / space)));
                }
                if (showMonth > 0) {
                    addView(m_monthView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (int) (m_height / space)));
                }
                if (showYear > 0) {
                    addView(m_yearView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (int) (m_height / space)));
                }
            }
        }
        m_ticker.run();
    }

    private TextView createInstance() {
        TextView tv = new TextView(m_owner);
        tv.setAutoSizeTextTypeUniformWithConfiguration(
            10, 3000,
            3, TypedValue.COMPLEX_UNIT_PX);
        tv.setGravity(Gravity.CENTER);
        tv.setLines(1);
        return tv;
    }


    public ClockView(Activity context) {
        super(context);
        setOrientation(VERTICAL);
        m_owner = context;
        m_ampmView = createInstance();
        m_dateView = createInstance();
        m_hoursView = createInstance();
        m_minutesView = createInstance();
        m_monthdayView = createInstance();
        m_monthView = createInstance();
        m_secondsView = createInstance();
        m_timeView = createInstance();
        m_weekdayView = createInstance();
        m_yearView = createInstance();
        m_prefs = context.getSharedPreferences("SecondsClock", Context.MODE_PRIVATE);
        m_sensorManager =
            (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (m_sensorManager != null) {
            m_lightSensor = m_sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }
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