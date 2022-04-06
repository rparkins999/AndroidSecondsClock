/*
 * Copyright © 2022. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This is the view which shows the fullscreen clock.
 * It is used in ClockActivity, and also at reduced size
 * in ClockConfigureActivity to show what the full screen clock will look like.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.ArraySet;
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
import java.util.Set;

@SuppressLint("ViewConstructor")
class ClockView extends LinearLayout implements SensorEventListener {
    /* Setting a zero window brightness does not make the clock completely
     * invisible, so for requested brightness values less than this threshold
     * we set the window brightness to zero and reduce the opacity.
     */
    private static final int ZEROBRIGHT = 25;

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

    private int m_fgcolour;
    private int m_height;
    private float m_lightLevel;
    private float m_smoothedLightLevel; // prevent hunting
    private Sensor m_lightSensor;
    private boolean m_trim = false; // chop off last char of time
    private boolean m_visible;

    // Set the colour of all of our display objects.
    public void setColour(int colour) {
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

    /* When the clock view is used as a democlock in the configuration screen,
     * with a particular lux threshold setting, a particular minimum
     * brightness setting, a particular actual lux level, and a particular
     * global brightness setting, we want to display it as bright as it will
     * appear on the real full screen clock. This is tricky because
     * on the real full screen clock we do it by setting the application screen
     * brightness, but we can't do that in the configuration screen because
     * that would change the brightness of the controls as well. So we do it
     * by adjusting the opacity of the democlock so that the apparent
     * brightness of the democlock is the same as what the brightness of the
     * full screen clock would be.
     * Unfortunately the relationship between opacity and brightness is horribly
     * non-linear and also dependent on internal resources set by the device
     * manufacturer and not readable by a user app (so much for open source!).
     * Also zero screen brightness isn't invisible as zero opacity is,
     * so I reduce the opacity as well for low values of brightness:
     * this makes it even more nonlinear.
     * To try and get it right we use a map which gives the required opacity
     * from the global brightness setting and the application screen brightness
     * that we have calculated from the lux threshold setting,
     * the current actual lux level, the minimum brightness level setting,
     * and the current global brightness. The global brightness setting
     * appears both in the calculation and the map because we don't set
     * the application screen brightness to be more than that.
     * This map can be set in a calibration screen using flicker photometry:
     * default values are used if the user doesn't set the map.
     * We can't store a map in the SharedPreferences directly, so we store it as
     * a StringSet where each string contains three comma-separated values,
     * the global brightness setting, the application screen brightness,
     * and the required opacity for those values. We interpolate between the
     * closest entries in the map.
     * If the you don't bother to calibrate, it will still work,
     * but it will be harder to set the minimum brightness
     * to get the behaviour that you want.
     */
    private int getOpacity(int globright, int appbright) {
        /* These describe the map entry with the highest global brightness which
         * is less than or equal to globright and the highest application
         * screen brightness which is less than or equal to appbright.
         */
        int globlolo = 0;
        int applolo = 0;
        int oplolo = 0;
        /* These describe the map entry with the highest global brightness which
         * is less than or equal to globright and the lowest application screen
         * brightness which is greater than or equal to appbright.
         */
        int globlohi = 0;
        int applohi = 255;
        int oplohi = 255;
        /* These describe the map entry with the lowest global brightness which
         * is greater than or equal to globright and the highest application
         * screen brightness which is less than or equal to appbright.
         */
        int globhilo = 255;
        int apphilo = 0;
        int ophilo = 0;
        /* These describe the map entry with the lowest global brightness which
         * is greater than or equal to globright and the lowest application
         * screen brightness which is greater than or equal to appbright.
         */
        int globhihi = 255;
        int apphihi = 255;
        int ophihi = 255;
        // Go through the map to see if we can find anything closer than
        // the default values set above.
        Set<String> as = m_prefs.getStringSet("brightmap", null);
        if (as != null) {
            for (String s : as) { // iterate over the map
                try {
                    String[] t = s.split(",");
                    if (t.length == 3) { // ignore invalid map entry
                        int glob = Integer.parseInt(t[0]);
                        int app = Integer.parseInt(t[1]);
                        int op = Integer.parseInt(t[2]);
                        if (   (glob <= globright)
                            && (glob >= globlolo)
                            && (app <= appbright)
                            && (app >= applolo))
                        {
                            globlolo = glob;
                            applolo = app;
                            oplolo = op;
                            continue;
                        }
                        if (   (glob <= globright)
                            && (glob >= globlohi)
                            && (app >= appbright)
                            && (app <= applohi))
                        {
                            globlohi = glob;
                            applohi = app;
                            oplohi = op;
                            continue;
                        }
                        if (   (glob >= globright)
                            && (glob <= globhilo)
                            && (app <= appbright)
                            && (app >= apphilo))
                        {
                            globhilo = glob;
                            apphilo = app;
                            ophilo = op;
                            continue;
                        }
                        if (   (glob >= globright)
                            && (glob <= globhihi)
                            && (app >= appbright)
                            && (app <= apphihi))
                        {
                            globhihi = glob;
                            apphihi = app;
                            ophihi = op;
                        }
                    }
                } catch (NumberFormatException ignore) {}  // ignore invalid
            }
        }
        // first interpolate along application screen brightness
        int globlo =
            (globlolo * (applohi - appbright) + globlohi * (appbright - applolo))
            / (applohi - applolo);
        int oplo =
            (oplolo * (applohi - appbright) + oplohi * (appbright - applolo))
            / (applohi - applolo);
        int globhi =
            (globhilo * (apphihi - appbright) + globhihi * (appbright - apphilo))
            / (apphihi - apphilo);
        int ophi =
            (ophilo * (apphihi - appbright) + ophihi * (appbright - apphilo))
            / (apphihi - apphilo);
        // now interpolate along global brightness
        return (oplo * (globhi - globright) + ophi * (globright - globlo))
                / (globhi - globlo);
    }

    /* This adjusts the colour and brightness of the clock display.
     * It can be called from our configuration screen
     * or from onSensorChanged when the ambient light level changes.
     */
    public void adjustColour() {
        // This is the ambient light level (in lux)
        // above which we display at the user's set brightness.
        float threshold = m_prefs.getInt("Cthreshold", 0);
        // This is the minimum brightness when the lux is zero.
        int minbright= m_prefs.getInt("Cbrightness", 255);
        // This is a smoothed ambueint light level to avoid hunting
        m_smoothedLightLevel = (7F * m_smoothedLightLevel + m_lightLevel) / 8F;
        ContentResolver cr = m_owner.getContentResolver();
        int globright = Settings.System.getInt(
            cr, Settings.System.SCREEN_BRIGHTNESS, 255);
        float brightness;
        if (   (m_smoothedLightLevel >= threshold)
            || (minbright >= globright))
        {
            brightness = globright;
        } else {
            brightness = minbright +
                (m_smoothedLightLevel / threshold) * (globright - minbright);
        }
        int alpha;
        if (m_owner instanceof ClockConfigureActivity) {
            ((ClockConfigureActivity) m_owner).m_CurrentLux.setText(
                m_owner.getString(R.string.currentlux, (int)m_lightLevel));
            // This is the demo: fake brightness change by adjusting opacity
            alpha = getOpacity(globright, (int)brightness);
            m_fgcolour = m_prefs.getInt("Cfgcolour", 0xFFFFFFFF);
            setColour((alpha << 24) | (m_fgcolour & 0xFFFFFF));
        } else if (m_owner instanceof Calibrate) {
            // This is the calibrator, just update the current lux
            ((Calibrate) m_owner).m_CurrentLux.setText(
                m_owner.getString(R.string.currentlux, (int)m_lightLevel));
        } else {
            // This is the real full screen clock,
            // adjust the real screen brightness
            Window w = m_owner.getWindow();
            WindowManager.LayoutParams lp = w.getAttributes();
            if (brightness >= globright)
            {
                lp.screenBrightness = -1.0F;
                alpha = 255;
            } else {
                lp.screenBrightness = brightness / 255F;
                if (brightness >= 16) {
                    alpha = 255;
                } else {
                    alpha = (int)(255 * brightness / 16);
                }
            }
            setColour((alpha << 24) | (m_fgcolour & 0xFFFFFF));
            w.setAttributes(lp);
        }
    }

    // This gets the current ambient light level in lux and limits it to MAXLIGHT,
    // the light level in a typical brightly lit room.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            m_lightLevel = event.values[0];
            adjustColour();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private Calendar updateTime() {
        Calendar next = Calendar.getInstance(); // new one in case time zone changed
        /* Once per minute, we check if we're connected to a charger:
         * if so, we keep the screen on.
         */
        if ((m_owner instanceof ClockActivity) && next.get(Calendar.SECOND) == 0)
        {
            // ACTION_BATTERY_CHANGED is a sticky intent
            Intent battery = m_owner.registerReceiver(
                null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (   (battery != null) // it can't be null, but lint moans if we don't check
                && (battery.getIntExtra(
				        BatteryManager.EXTRA_PLUGGED, 0) != 0)) {
                m_owner.getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
            } else {
                m_owner.getWindow().clearFlags(FLAG_KEEP_SCREEN_ON);
            }
        }
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
            adjustColour();
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
        m_fgcolour = m_prefs.getInt("Cfgcolour", 0xFFFFFFFF);
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
        setForegroundTintList(ColorStateList.valueOf(m_fgcolour));
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
        setBackgroundColor(0xFF000000);
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
        m_lightLevel = m_prefs.getInt("Cthreshold", 0);
        m_smoothedLightLevel = m_lightLevel;
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
