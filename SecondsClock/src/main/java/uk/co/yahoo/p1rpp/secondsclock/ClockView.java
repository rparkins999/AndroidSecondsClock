/*
 * Copyright © 2024. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This is the view which shows the fullscreen clock.
 * It is used in ClockActivity, and also at reduced size
 * in ClockConfigureActivity to show what the full screen clock will look like.
 */

//FIXME
/* more hysteresis on lux value
   move gradually from system brightness to mine
   see if can read system brightness when I'm setting it - yes
   if so, move gradually from my value ti system brightness
 */
// FIXME Add option to only use opacity

package uk.co.yahoo.p1rpp.secondsclock;

import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.util.IntProperty;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

@SuppressLint("ViewConstructor")
class ClockView extends LinearLayout implements SensorEventListener {
    public String m_dateFormat = " ";
    public String m_hoursFormat = " ";
    public String m_monthFormat = " ";
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
    private boolean m_visible;
    private final SensorManager m_sensorManager;
    private final SharedPreferences m_prefs;
    private final TextView m_timeView;
    private final TextView m_weekdayView;
    private final TextView m_yearView;
    private int m_fgcolour;
    private int m_height;
    private float m_smoothedLightLevel; // prevent hunting
    private Sensor m_lightSensor;

    // Interpolator used for brightness animations.
    private static final DecelerateInterpolator VALUE_ANIM_INTERPOLATOR =
            new DecelerateInterpolator();
    private int m_wanted;

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

    private void setLp (int value) {
        Window w = m_owner.getWindow();
        WindowManager.LayoutParams lp = w.getAttributes();
        int sb = Settings.System.getInt(
                m_owner.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, 255);
        if ((value == sb) && (value == m_wanted)) {
            value = -255;
        }
        lp.screenBrightness = value / 255F;
        w.setAttributes(lp);
        if (m_owner instanceof ClockConfigureActivity) {
            ((ClockConfigureActivity)m_owner).updateTexts(
                    m_smoothedLightLevel, value, 255);
        }
    }

    private Integer getLp() {
        Window w = m_owner.getWindow();
        WindowManager.LayoutParams lp = w.getAttributes();
        return (int)(lp.screenBrightness * 255F);
    }

    // called by animator
    private final IntProperty<ClockView> LP_VALUE =
            new IntProperty<ClockView>("brightness") {
                @Override
                public void setValue(ClockView object, int value) {
                    object.setLp(value);
                }
                @Override
                public Integer get(ClockView object) {
                    return object.getLp();
                }
            };

    private void setOpacity(int value) {
        m_fgcolour= (m_fgcolour & 0xFFFFFF) | value << 24;
        setColour(m_fgcolour);
        if (m_owner instanceof ClockConfigureActivity) {
            Window w = m_owner.getWindow();
            WindowManager.LayoutParams lp = w.getAttributes();
            int lb = (int)(lp.screenBrightness * 255);
            ((ClockConfigureActivity)m_owner).updateTexts(
                    m_smoothedLightLevel, lb, value);
        }
    }
    private Integer getOpacity() {
        return (m_fgcolour >> 24) & 255;
    }

    // called by animator
    private final IntProperty<ClockView> OPACITY_VALUE =
            new IntProperty<ClockView>("opacity" ) {
                @Override
                public void setValue(ClockView object, int value) {
                    object.setOpacity(value);
                }
                @Override
                public Integer get(ClockView object) {
                    return object.getOpacity();
                }
            };

    /* This adjusts the colour and brightness of the clock display.
     * It can be called from our configuration screen
     * or from onSensorChanged when the ambient light level changes.
     */
    @SuppressLint("ApplySharedPref")
    public void adjustColour() {
        // This is the ambient light level (in lux)
        // above which we display at system brightness.
        int threshold = m_prefs.getInt("Cthreshold", 0);
        // This is the minimum brightness at low lux levels.
        // The range is 0..100 because useful values are small.
        // Adjusting opacity may make the display dimmer.
        int minbright = m_prefs.getInt("CminBright", 0);
        // This is the system brightness, possibly modified by
        // the device's own auto-dimming
        int sb = Settings.System.getInt(
                m_owner.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, 255);
        /* If this is > 1.0, lux is above threshold and we should
         * let the system set the screen brightness:
         * otherwise it is the brightness value (0F .. 1F) that we want.
         * In either case we may be still animating to get there.
         */
        int alpha;
        int wb = getLp();
        int onlyalpha = m_prefs.getInt("Conlyalpha", 0);
        if (m_smoothedLightLevel >= threshold) {
            // revert to system brightness
            if (wb < 0) {
                // already using system brightness
                if (m_owner instanceof ClockConfigureActivity) {
                    ((ClockConfigureActivity) m_owner).updateTexts(
                            m_smoothedLightLevel, -255, 255);
                }
                return;
            } else if (wb == sb) {
                // Animation reached system brightness
                setLp(-255);
                if (m_owner instanceof ClockConfigureActivity) {
                    ((ClockConfigureActivity) m_owner).updateTexts(
                            m_smoothedLightLevel, -255, 255);
                }
                return;
            } else {
                // move smoothly to system brightness
                m_wanted = sb;
                alpha = 255;
            }
        } else {
            int bright = (int) ((m_smoothedLightLevel * sb) / threshold);
            // This is the minimum transparency at low light level
            // If it is 255, transparency is not used at all.
            int minalpha = m_prefs.getInt("Calpha", 0);
            if (onlyalpha != 0) {
                // only controlling opacity
                m_wanted = sb;
                alpha = minalpha + (bright * (255 - minalpha)) / 255;
            } else if ((minalpha == 255) || (bright >= minbright)) {
                if (bright > sb) {
                    // should be impossible
                    // but can happen because of rounding errors
                    m_wanted = sb;
                } else {
                    m_wanted = minbright + (
                            (bright * (sb - minbright)) / 255);
                }
                alpha = 255;
            } else {
                m_wanted = minbright;
                alpha = minalpha + (bright * (255 - minalpha)) / minbright;
            }
        }
        long diff1;
        if (wb < 0) {
            Window w = m_owner.getWindow();
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.screenBrightness = sb / 255F;
            w.setAttributes(lp);
            diff1 = m_wanted - sb;
        } else {
            diff1 = m_wanted - wb;
        }
        if (diff1 < 0) {
            diff1 = -diff1;
        }
        long diff2;
        diff2 = alpha - ((m_fgcolour >> 24) & 255);
        if (diff2 < 0) {
            diff2 = -diff2;
        }
        long difference;
        if (diff1 > diff2) {
            difference = 5 * diff1;
        } else {
            difference = 5 * diff2;
        }
        if (diff1 > 0) {
            if (diff1 > 10) {
                // Animator for smoothing changes in screen brightness
                ObjectAnimator m_brightnessAnimator = ObjectAnimator.ofInt(
                        this, LP_VALUE, getLp(), m_wanted);
                m_brightnessAnimator.setAutoCancel(true);
                m_brightnessAnimator.setDuration(difference);
                m_brightnessAnimator.setInterpolator(VALUE_ANIM_INTERPOLATOR);
                m_brightnessAnimator.start();
            } else {
                /* Small differences can occur if we are in the configure page
                 * and one of the sliders is being animated. In this case we
                 * want to go straight to the new value to avoid drag.
                 */
                setLp(m_wanted);
            }
        }
        if (diff2 > 0) {
            if (diff2 > 10) {
                // Animator for smoothing changes in screen brightness
                ObjectAnimator m_opacityAnimator = ObjectAnimator.ofInt(
                        this, OPACITY_VALUE, getOpacity(), alpha);
                m_opacityAnimator.setAutoCancel(true);
                m_opacityAnimator.setDuration(difference);
                m_opacityAnimator.setInterpolator(VALUE_ANIM_INTERPOLATOR);
                m_opacityAnimator.start();
            } else {
                setOpacity(alpha);
            }
        }
        if (diff1 + diff2 == 0) {
            if (m_owner instanceof ClockConfigureActivity) {
                ((ClockConfigureActivity) m_owner).updateTexts(
                        m_smoothedLightLevel, m_wanted, alpha);
            }
        }
    }

    // This gets the current ambient light level in lux and limits it to MAXLIGHT,
    // the light level in a typical brightly lit room.
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lightLevel = event.values[0];
            m_smoothedLightLevel =
                    (7F * m_smoothedLightLevel + lightLevel) / 8F;
            // Only call adjustColour() for significant changes
            float ratio = lightLevel / m_prefs.getFloat("Csmoothed", 0);
            if ((ratio < 0.99) || (ratio > 1.01)) {
                m_prefs.edit().putFloat(
                        "Csmoothed", m_smoothedLightLevel).commit();
                adjustColour();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    /* This gets called by updateLayout in AM/PM mode as well as
     * updateTime, because switching from 9:59 to 10:00
     * or from 12:59 to 1:00 can change
     * the length of the text in m_timeView,
     * which can affect the layout.
     */
    private void fixLengths(Calendar next) {
        CharSequence ampm = DateFormat.format("a", next);
        CharSequence time = DateFormat.format(m_timeFormat, next);
        // Remove this bit of code if you're happy with the default for en_GB
        // of lower-case am/pm, which I don't like.
        if (Locale.getDefault().getDisplayName().startsWith("English")) {
            m_ampmView.setAllCaps(true);
            m_timeView.setAllCaps(true);
        } else {
            m_ampmView.setAllCaps(false);
            m_timeView.setAllCaps(false);
        }
        m_ampmView.setText(ampm);
        m_timeView.setText(time);
    }

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
        int timeLength = String.valueOf(m_timeView.getText()).length();
        fixLengths(next);
        if (timeLength != String.valueOf(m_timeView.getText()).length()) {
            /* fixLengths changed the length, so we need to relayout.
             * updateLayout does more than we need, but it's too
             * complicated to extract the part that we want,
             * and this only happens four times per 24 hours.
             */
            updateLayout();
        }
        m_dateView.setText(DateFormat.format(m_dateFormat, next));
        m_hoursView.setText(DateFormat.format(m_hoursFormat, next));
        m_minutesView.setText(DateFormat.format("mm", next));
        m_monthdayView.setText(DateFormat.format("dd", next));
        m_monthView.setText(DateFormat.format(m_monthFormat, next));
        m_secondsView.setText(DateFormat.format("ss", next));
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
            /* The 7 segment "M" didn't look very nice, so this
             * version of the 7 segment font has a nicer "M" which
             * can't be produced on a 7 segemnt display, but we
             * don't *really* have one.
             */
            font = Typeface.createFromAsset(
            m_owner.getAssets(), "DSEG7Classic-BoldItalic.ttf");
            // The seven segment font has no line spacing....
            lsp = 1.1F;
        } else {
            font = Typeface.DEFAULT;
        }
        m_ampmView.setTypeface(font);
        m_hoursView.setTypeface(font);
        m_minutesView.setTypeface(font);
        m_secondsView.setTypeface(font);
        m_timeView.setTypeface(font);
        boolean m_haveDate =
            showMonth + showMonthDay + showShortDate + showWeekDay + showYear > 0;
        setForegroundTintList(ColorStateList.valueOf(m_fgcolour));
        LinearLayout ll = new LinearLayout(m_owner);
        float timeWidth;
        float secondsWidth;
        float ampmWidth;
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setOrientation(VERTICAL);
            ll.setOrientation(HORIZONTAL);
            if (m_secondsSize == 255) { // full sixe seconds
                if (is24) {
                    m_timeFormat = "HH:mm:ss";
                } else {
                    m_timeFormat = "h:mm:ss a";
                }
                ll.addView(m_timeView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
            } else { // small seconds or none
                if (is24) {
                    m_timeFormat = "HH:mm";
                    ampmWidth = 0;
                } else {
                    m_timeFormat = "h:mm";
                    // only needed in AM/PM mode
                    fixLengths(Calendar.getInstance());
                    ampmWidth = m_timeView.getPaint().measureText(
                            String.valueOf(m_ampmView.getText()));
                }
                timeWidth = m_timeView.getPaint().measureText(
                        String.valueOf(m_timeView.getText()));
                secondsWidth = m_timeView.getPaint().measureText("00")
                        * m_secondsSize / 255F;
                ll.addView(m_timeView, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT,
                        timeWidth));
                ll.addView(m_secondsView, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT,
                        secondsWidth));
                if (ampmWidth != 0) {
                    ll.addView(m_ampmView, new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.MATCH_PARENT,
                            ampmWidth));
                }
            }
            addView(ll, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.85F));
            if (m_haveDate) {
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
                    if (!first) {
                        sb.append(" ");
                    }
                    sb.append(shortDateFormat());
                } else {
                    if (showMonthDay > 0) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(" ");
                        }
                        sb.append("d");
                    }
                    if (showMonth == 1) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(" ");
                        }
                        sb.append("LLL");
                    } else if (showMonth == 2) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(" ");
                        }
                        sb.append("LLLL");
                    }
                    if (showYear > 0) {
                        if (!first) {
                            sb.append(" ");
                        }
                        sb.append("yyyy");
                    }
                }
                m_dateFormat = sb.toString();
                addView(m_dateView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.15F));
            }
        } else { // assume PORTRAIT
            setOrientation(VERTICAL);
            float space = 0;
            boolean horizontalTime =
                (   (showMonth == 2) || (showWeekDay == 2) || (showShortDate > 0))
                 && !(m_prefs.getBoolean("Cforcevertical", false));
            // first work out how much space we need
            if (horizontalTime)
            {
                ll.setOrientation(HORIZONTAL);
                if (m_secondsSize == 255) { // full size seconds
                    if (is24) {
                        m_timeFormat = "HH:mm:ss";
                    } else {
                        m_timeFormat = "h:mm:ss a";
                        // only needed in AM/PM mode
                        fixLengths(Calendar.getInstance());
                    }
                    ll.addView(m_timeView, new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
                } else { // small seconds or none
                    if (is24) {
                        m_timeFormat = "HH:mm";
                        ampmWidth = 0;
                    } else {
                        m_timeFormat = "h:mm";
                        // only needed in AM/PM mode
                        fixLengths(Calendar.getInstance());
                        ampmWidth = m_timeView.getPaint().measureText(
                                String.valueOf(m_ampmView.getText()));
                    }
                    timeWidth = m_timeView.getPaint().measureText(
                            String.valueOf(m_timeView.getText()));
                    secondsWidth = m_timeView.getPaint().measureText("00")
                            * m_secondsSize / 255F;
                    ll.addView(m_timeView, new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.MATCH_PARENT,
                            timeWidth));
                    ll.addView(m_secondsView, new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.MATCH_PARENT,
                            secondsWidth));
                    if (ampmWidth != 0) {
                        ll.addView(m_ampmView, new LinearLayout.LayoutParams(
                                0, ViewGroup.LayoutParams.MATCH_PARENT,
                                ampmWidth));
                    }
                }
                space += lsp;
            } else { // not horizontalTime
                if (is24) {
                    m_hoursFormat = "HH";
                    space += 2F + lsp; // hours 0-24
                } else {
                    m_hoursFormat = "KK";  // hours 0-12
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
            if (horizontalTime) {
                addView(ll, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) (lsp * m_height / space)));
            } else { // time in vertical format
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
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) (m_height / space)));
                }
            }
            if (showWeekDay > 0) {
                addView(m_weekdayView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (m_height / space)));
            }
            if (showShortDate > 0) {
                addView(m_dateView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (m_height / space)));
            } else {
                if (showMonthDay > 0) {
                    addView(m_monthdayView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) (m_height / space)));
                }
                if (showMonth > 0) {
                    addView(m_monthView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) (m_height / space)));
                }
                if (showYear > 0) {
                    addView(m_yearView, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) (m_height / space)));
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
        setOrientation(VERTICAL);
        updateLayout();
    }

    // This arranges for the seconds to tick only if they are
    // being shown and this ClockView is visible.
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
