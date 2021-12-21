/*
 * Copyright © 2021. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This class handles the settings for SecondsClockWidget.
 * It also shows what the widget will look like in 1x1 size
 */

package uk.co.yahoo.p1rpp.secondsclock;

import static android.text.InputType.TYPE_CLASS_NUMBER;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

public class WidgetConfigureActivity extends Activity {

    private AppWidgetManager appWidgetManager;
    private ComponentName secondsClockWidget;
    private int maxHeight = 121;
    private int minWidth = 93;
    private boolean recursive = false;
    private EditText alphaValue;
    private SeekBar alphaSlider;
    private EditText redValue;
    private SeekBar redSlider;
    private EditText greenValue;
    private SeekBar greenSlider;
    private EditText blueValue;
    private SeekBar blueSlider;
    private int currentView;
    static final int SETWIDGETBACKGROUNDCOLOUR = 0;
    static final int SETWIDGETTEXTCOLOUR = 1;
    static final int CONFIGUREWIDGET = 2;
    private int m_bgcolour;
    private TextClock demo;
    private SharedPreferences prefs;
    private int m_fgcolour;
    private int showTime;
    private int showWeekDay;
    private int showShortDate;
    private int showMonthDay;
    private int showMonth;
    private int showYear;
    private DisplayMetrics metrics;
    private BitmapWrapper m_activeWrapper = null;
    private Bitmap m_bitmap;
    private int multiplier;
    private Configuration config;
    private TextView helptext;
    private ImageView m_colourmap;
    private FrameLayout topLayout;
    private LinearLayout demobox;
    private LinearLayout demoboxbox;
    private LinearLayout.LayoutParams lpWrapMatch;
    private CheckBox showTimeCheckBox;
    private CheckBox showSecondsCheckBox;
    private CheckBox showShortWeekDayCheckBox;
    private CheckBox showLongWeekDayCheckBox;
    private CheckBox showShortDateCheckBox;
    private CheckBox showMonthDayCheckBox;
    private CheckBox showShortMonthCheckBox;
    private CheckBox showLongMonthCheckBox;
    private CheckBox showYearCheckBox;
    private Button bgButton;
    private Button fgButton;
    private LinearLayout.LayoutParams lpMatchWrap;
    private ViewGroup.LayoutParams lpWrapWrap;
    private boolean colourmaplongclicked = false;
    private WidgetConfigureActivity ac;
    private SeekBar hueSlider;
    private LinearLayout.LayoutParams lpMMWeight;
    private LinearLayout.LayoutParams lpMatchMatch;
    private Button okButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.generic_layout);
    }

    private void updateWidget() {
        int[] widgetIds = appWidgetManager.getAppWidgetIds(secondsClockWidget);
        if (widgetIds.length > 0) {
            Bundle newOptions =
                appWidgetManager.getAppWidgetOptions(widgetIds[0]);
            maxHeight = newOptions.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight);
            minWidth = newOptions.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidth);
        }
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.setClassName("uk.co.yahoo.p1rpp.secondsclock",
            "uk.co.yahoo.p1rpp.secondsclock.SecondsClockWidget");
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        sendBroadcast(intent);
    }

    @SuppressLint({"SetTextI18n", "ApplySharedPref"})
    private void setColour(int colour) {
        if (!recursive) {
            recursive = true;
            int val = (colour >> 24) & 0xFF;
            alphaValue.setText(Integer.toString(val));
            alphaSlider.setProgress(val);
            val = (colour >> 16) & 0xFF;
            redValue.setText(Integer.toString(val));
            redSlider.setProgress(val);
            val = (colour >> 8) & 0xFF;
            greenValue.setText(Integer.toString(val));
            greenSlider.setProgress(val);
            val = colour & 0xFF;
            blueValue.setText(Integer.toString(val));
            blueSlider.setProgress(val);
            switch (currentView) {
                case SETWIDGETBACKGROUNDCOLOUR:
                    m_bgcolour = colour;
                    demo.setBackgroundColor(colour);
                    prefs.edit().putInt("Wbgcolour", colour).commit();
                    alphaSlider.setBackground(new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[] {0xFF000000, m_bgcolour | 0xFF000000}));
                    ColorStateList cl = ColorStateList.valueOf(m_fgcolour);
                    alphaSlider.setProgressTintList(cl);
                    alphaSlider.setThumbTintList(cl);
                    break;
                case SETWIDGETTEXTCOLOUR:
                    m_fgcolour = colour;
                    demo.setTextColor(colour);
                    prefs.edit().putInt("Wfgcolour", colour).commit();
                    break;
                case CONFIGUREWIDGET:
            }
            updateDemo();
            updateWidget();
            recursive = false;
        }
    }

    void updateDemo() {
        Formatter f = new Formatter();
        f.set(this, minWidth, maxHeight,
            showTime, showWeekDay, showShortDate,
            showMonthDay, showMonth, showYear);
        int width = (int)(minWidth * metrics.density);
        int height = (int)(maxHeight * metrics.density);
        demo.setMinimumWidth(width);
        demo.setMaxWidth(width);
        demo.setMinimumHeight(height);
        demo.setMaxHeight(height);
        demo.setFormat12Hour(f.time12 + f.rest);
        demo.setFormat24Hour(f.time24 + f.rest);
        demo.setLines(f.lines);
        demo.setAutoSizeTextTypeWithDefaults(
            TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
    }

    private void fillBitmapInBackground(int colour, boolean firstTime) {
        FillBitmap task;
        if (m_activeWrapper != null) {
            task = m_activeWrapper.m_active;
            if (task != null) {
                task.cancel(true);
            }
        }
        m_activeWrapper = new BitmapWrapper(
            colour, m_bitmap, multiplier, this, firstTime);
        task = new FillBitmap();
        m_activeWrapper.m_active = task;
        task.execute(m_activeWrapper);
    }

    void setMultiplier() {
        int size;
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            size = metrics.heightPixels;
            int sb = 72;
            Resources res = getResources();
            int resourceId = res.getIdentifier(
                "status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                sb = res.getDimensionPixelSize(resourceId);
            }
            int hh = helptext.getBottom();
            size -= sb + hh;
        } else {
            size = metrics.widthPixels;
        }
        if (size < 512) {
            multiplier = 1;
        } else if (size < 1024) {
            multiplier = 2;
        } else {
            multiplier = 4;
        }
    }

    void redisplayBitmap() {
        m_colourmap.invalidate();
    }

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

    @SuppressLint("SetTextI18n")
    void doMainLayout() {
        removeAllViews(topLayout);
        demobox.addView(demo);
        demoboxbox.addView(demobox);
        LinearLayout l1 = new LinearLayout(this);
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            l1.setOrientation(LinearLayout.HORIZONTAL);
            demoboxbox.setLayoutParams(lpWrapMatch);
            demoboxbox.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setGravity(Gravity.CENTER_VERTICAL);
            l1.addView(demoboxbox);
            LinearLayout l2 = new LinearLayout(this);
            l2.setOrientation(LinearLayout.VERTICAL);
            l2.addView(helptext);
            LinearLayout l3 = new LinearLayout(this);
            l3.setOrientation(LinearLayout.HORIZONTAL);
            l3.addView(showTimeCheckBox);
            l3.addView(showSecondsCheckBox);
            l2.addView(l3);
            LinearLayout l4 = new LinearLayout(this);
            l4.setOrientation(LinearLayout.HORIZONTAL);
            l4.addView(showShortWeekDayCheckBox);
            l4.addView(showLongWeekDayCheckBox);
            l2.addView(l4);
            LinearLayout l5 = new LinearLayout(this);
            l5.setOrientation(LinearLayout.HORIZONTAL);
            l5.addView(showShortDateCheckBox);
            l5.addView(showMonthDayCheckBox);
            l2.addView(l5);
            LinearLayout l6 = new LinearLayout(this);
            l6.setOrientation(LinearLayout.HORIZONTAL);
            l6.addView(showShortMonthCheckBox);
            l6.addView(showLongMonthCheckBox);
            l2.addView(l6);
            l2.addView(showYearCheckBox);
            LinearLayout l7 = new LinearLayout(this);
            l7.setOrientation(LinearLayout.HORIZONTAL);
            l7.addView(bgButton);
            l7.addView(fgButton);
            l2.addView(l7);
            l1.addView(l2);
        } else { // assume Portrait
            l1.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setLayoutParams(lpMatchWrap);
            demoboxbox.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setGravity(Gravity.CENTER_HORIZONTAL);
            l1.addView(demoboxbox);
            l1.addView(helptext);
            helptext.setGravity(Gravity.START);
            l1.addView(showTimeCheckBox);
            showTimeCheckBox.setGravity(Gravity.START);
            l1.addView(showSecondsCheckBox);
            showSecondsCheckBox.setGravity(Gravity.START);
            l1.addView(showShortWeekDayCheckBox);
            showShortWeekDayCheckBox.setGravity(Gravity.START);
            l1.addView(showLongWeekDayCheckBox);
            showLongWeekDayCheckBox.setGravity(Gravity.START);
            l1.addView(showShortDateCheckBox);
            showShortDateCheckBox.setGravity(Gravity.START);
            l1.addView(showMonthDayCheckBox);
            showMonthDayCheckBox.setGravity(Gravity.START);
            l1.addView(showShortMonthCheckBox);
            showShortMonthCheckBox.setGravity(Gravity.START);
            l1.addView(showLongMonthCheckBox);
            showLongMonthCheckBox.setGravity(Gravity.START);
            l1.addView(showYearCheckBox);
            showYearCheckBox.setGravity(Gravity.START);
            LinearLayout l2 = new LinearLayout(this);
            l2.setLayoutParams(lpMatchWrap);
            l2.setOrientation(LinearLayout.VERTICAL);
            l2.setGravity(Gravity.CENTER_HORIZONTAL);
            l2.addView(bgButton, lpWrapWrap);
            l2.addView(fgButton, lpWrapWrap);
            l1.addView(l2);
            /* debugging
            Button testButton = new Button(this);
            testButton.setText("Test going to system clock app");
            testButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        startActivity(new Intent(
                            "android.intent.action.SHOW_TIMERS"));
                    } catch (ActivityNotFoundException ignore) {
                        Toast.makeText(ac, "ActivityNotFoundException",
                            Toast.LENGTH_LONG).show();
                    }
                }
            });
            l1.addView(testButton);
             */ // end of debugging code
        }
        Button b = new Button(this);
        b.setText("clock");
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ac, ClockActivity.class);
                startActivity(intent);
            }
        });
        LinearLayout l18 = new LinearLayout(this);
        l18.setLayoutParams(lpMatchWrap);
        l18.setOrientation(LinearLayout.VERTICAL);
        l18.setGravity(Gravity.CENTER_HORIZONTAL);
        l18.addView(b, lpWrapWrap);
        l1.addView(l18);
        topLayout.addView(l1);
    }

    @SuppressLint({"ClickableViewAccessibility"})
    void doChooserLayout() {
        setMultiplier();
        /* These have to be created to order because their sizes can depend
         * on whether the device is in portrait or landscape orientation.
         */
        int size = 256 * multiplier;
        m_bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
            fillBitmapInBackground(m_bgcolour, true);
            setColour(m_bgcolour);
        } else {
            fillBitmapInBackground(m_fgcolour, true);
            setColour(m_fgcolour);
        }
        m_colourmap = new ImageView(this);
        m_colourmap.setAdjustViewBounds(true);
        m_colourmap.setMaxWidth(size);
        m_colourmap.setMaxHeight(size);
        m_colourmap.setImageBitmap(m_bitmap);
        m_colourmap.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (  (event.getAction() == MotionEvent.ACTION_UP)
                    && !colourmaplongclicked)
                {
                    int colour = m_bitmap.getPixel(
                        (int)event.getX(), (int)event.getY());
                    if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
                        // don't change opacity from colourmap touch
                        setColour((m_bgcolour & 0xFF000000) | (colour & 0xFFFFFF));
                    } else {
                        setColour(colour);
                    }
                    return false;
                } else {
                    colourmaplongclicked = false;
                    return false;
                }
            }
        });
        m_colourmap.setLongClickable(true);
        m_colourmap.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                colourmaplongclicked = true;
                Toast.makeText(ac, getString((
                    currentView == SETWIDGETTEXTCOLOUR)
                        ? R.string.fghelp : R.string.bghelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        int colour = (currentView == SETWIDGETTEXTCOLOUR)
            ? m_fgcolour : m_bgcolour;
        int pad =(int)(5 * metrics.density);
        removeAllViews(topLayout);
        LinearLayout l1 = new LinearLayout(this);
        demobox.addView(demo);
        demoboxbox.addView(demobox);
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            l1.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout l2 = new LinearLayout(this);
            l2.setOrientation(LinearLayout.HORIZONTAL);
            l2.setLayoutParams(lpWrapMatch);
            l2.setGravity(Gravity.CENTER_VERTICAL);
            l2.setPadding(pad, pad, pad, pad);
            l2.addView(m_colourmap);
            l1.addView(l2);
            LinearLayout l3 = new LinearLayout(this);
            l3.setOrientation(LinearLayout.VERTICAL);
            l3.setLayoutParams(lpMatchMatch);
            l3.setPadding(pad, 0, pad, 0);
            demoboxbox.setLayoutParams(lpMatchWrap);
            demoboxbox.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setGravity(Gravity.CENTER_HORIZONTAL);
            l3.addView(demoboxbox);
            l3.addView(helptext);
            LinearLayout l4 = new LinearLayout(this);
            l4.setOrientation(LinearLayout.VERTICAL);
            l4.setLayoutParams(lpMatchWrap);
            l4.addView(hueSlider);
            l3.addView(l4);
            LinearLayout l5 = new LinearLayout(this);
            l5.setOrientation(LinearLayout.HORIZONTAL);
            l5.setLayoutParams(lpMatchWrap);
            LinearLayout l6 = new LinearLayout(this);
            l6.setLayoutParams(lpMMWeight);
            l6.setOrientation(LinearLayout.VERTICAL);
            l6.setGravity(Gravity.CENTER_VERTICAL);
            l6.addView(redSlider);
            l5.addView(l6);
            LinearLayout l7 = new LinearLayout(this);
            l7.setLayoutParams(lpWrapWrap);
            l7.addView(redValue);
            l5.addView(l7);
            l3.addView(l5);
            LinearLayout l8 = new LinearLayout(this);
            l8.setOrientation(LinearLayout.HORIZONTAL);
            l8.setLayoutParams(lpMatchWrap);
            LinearLayout l9 = new LinearLayout(this);
            l9.setLayoutParams(lpMMWeight);
            l9.setOrientation(LinearLayout.VERTICAL);
            l9.setGravity(Gravity.CENTER_VERTICAL);
            l9.addView(greenSlider);
            l8.addView(l9);
            LinearLayout l10 = new LinearLayout(this);
            l10.setLayoutParams(lpWrapWrap);
            l10.addView(greenValue);
            l8.addView(l10);
            l3.addView(l8);
            LinearLayout l11 = new LinearLayout(this);
            l11.setOrientation(LinearLayout.HORIZONTAL);
            l11.setLayoutParams(lpMatchWrap);
            LinearLayout l12 = new LinearLayout(this);
            l12.setLayoutParams(lpMMWeight);
            l12.setOrientation(LinearLayout.VERTICAL);
            l12.setGravity(Gravity.CENTER_VERTICAL);
            l12.addView(blueSlider);
            l11.addView(l12);
            LinearLayout l13 = new LinearLayout(this);
            l13.setLayoutParams(lpWrapWrap);
            l13.addView(blueValue);
            l11.addView(l13);
            l3.addView(l11);
            if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
                LinearLayout l14 = new LinearLayout(this);
                l14.setOrientation(LinearLayout.HORIZONTAL);
                l14.setLayoutParams(lpMatchWrap);
                LinearLayout l15 = new LinearLayout(this);
                l15.setLayoutParams(lpMMWeight);
                l15.setOrientation(LinearLayout.VERTICAL);
                l15.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout l16 = new LinearLayout(this);
                l16.setBackgroundResource(R.drawable.background);
                l16.setOrientation(LinearLayout.VERTICAL);
                l16.setLayoutParams(lpMatchWrap);
                l16.addView(alphaSlider);
                l15.addView(l16);
                l14.addView(l15);
                LinearLayout l17 = new LinearLayout(this);
                l17.setLayoutParams(lpWrapWrap);
                l17.addView(alphaValue);
                l14.addView(l17);
                l3.addView(l14);
                // This seems to be needed to make the background pattern visible
                alphaValue.setText(alphaValue.getText());
            }
            LinearLayout l18 = new LinearLayout(this);
            l18.setLayoutParams(lpMatchWrap);
            l18.setOrientation(LinearLayout.VERTICAL);
            l18.setGravity(Gravity.CENTER_HORIZONTAL);
            l18.addView(okButton, lpWrapWrap);
            l3.addView(l18);
            l1.addView(l3);
        } else { // assume Portrait
            l1.setOrientation(LinearLayout.VERTICAL);
            l1.setPadding(pad, 0, pad, 0);
            demoboxbox.setLayoutParams(lpMatchWrap);
            demoboxbox.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setGravity(Gravity.CENTER_HORIZONTAL);
            l1.addView(demoboxbox);
            l1.addView(helptext);
            LinearLayout l2 = new LinearLayout(this);
            l2.setOrientation(LinearLayout.VERTICAL);
            l2.setLayoutParams(lpMatchWrap);
            l2.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout l3 = new LinearLayout(this);
            l3.setOrientation(LinearLayout.VERTICAL);
            l3.setLayoutParams(lpWrapWrap);
            l3.addView(m_colourmap);
            l2.addView(l3);
            l1.addView(l2);
            LinearLayout l4 = new LinearLayout(this);
            l4.setOrientation(LinearLayout.VERTICAL);
            l4.setLayoutParams(lpMatchWrap);
            l4.setPadding(0, pad, pad, pad);
            l4.addView(hueSlider);
            l1.addView(l4);
            LinearLayout l5 = new LinearLayout(this);
            l5.setOrientation(LinearLayout.HORIZONTAL);
            l5.setLayoutParams(lpMatchWrap);
            LinearLayout l6 = new LinearLayout(this);
            l6.setLayoutParams(lpMMWeight);
            l6.setOrientation(LinearLayout.VERTICAL);
            l6.setGravity(Gravity.CENTER_VERTICAL);
            l6.addView(redSlider);
            l5.addView(l6);
            LinearLayout l7 = new LinearLayout(this);
            l7.setLayoutParams(lpWrapWrap);
            l7.addView(redValue);
            l5.addView(l7);
            l1.addView(l5);
            LinearLayout l8= new LinearLayout(this);
            l8.setOrientation(LinearLayout.HORIZONTAL);
            l8.setLayoutParams(lpMatchWrap);
            LinearLayout l9 = new LinearLayout(this);
            l9.setLayoutParams(lpMMWeight);
            l9.setOrientation(LinearLayout.VERTICAL);
            l9.setGravity(Gravity.CENTER_VERTICAL);
            l9.addView(greenSlider);
            l8.addView(l9);
            LinearLayout l10 = new LinearLayout(this);
            l10.setLayoutParams(lpWrapWrap);
            l10.addView(greenValue);
            l8.addView(l10);
            l1.addView(l8);
            LinearLayout l11 = new LinearLayout(this);
            l11.setOrientation(LinearLayout.HORIZONTAL);
            l11.setLayoutParams(lpMatchWrap);
            LinearLayout l12 = new LinearLayout(this);
            l12.setLayoutParams(lpMMWeight);
            l12.setOrientation(LinearLayout.VERTICAL);
            l12.setGravity(Gravity.CENTER_VERTICAL);
            l12.addView(blueSlider);
            l11.addView(l12);
            LinearLayout l13 = new LinearLayout(this);
            l13.setLayoutParams(lpWrapWrap);
            l13.addView(blueValue);
            l11.addView(l13);
            l1.addView(l11);
            if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
                LinearLayout l14 = new LinearLayout(this);
                l14.setOrientation(LinearLayout.HORIZONTAL);
                l14.setLayoutParams(lpMatchWrap);
                l14.setPadding(0, pad, 0, pad);
                LinearLayout l15 = new LinearLayout(this);
                l15.setLayoutParams(lpMMWeight);
                l15.setOrientation(LinearLayout.VERTICAL);
                l15.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout l16 = new LinearLayout(this);
                l16.setBackgroundResource(R.drawable.background);
                l16.setOrientation(LinearLayout.VERTICAL);
                l16.setLayoutParams(lpMatchWrap);
                l16.addView(alphaSlider);
                l15.addView(l16);
                l14.addView(l15);
                LinearLayout l17 = new LinearLayout(this);
                l17.setLayoutParams(lpWrapWrap);
                l17.addView(alphaValue);
                l14.addView(l17);
                l1.addView(l14);
                // This seems to be needed to make the background pattern visible
                alphaValue.setText(alphaValue.getText());
            }
            LinearLayout l17 = new LinearLayout(this);
            l17.setLayoutParams(lpMatchWrap);
            l17.setOrientation(LinearLayout.VERTICAL);
            l17.setGravity(Gravity.CENTER_HORIZONTAL);
            l17.addView(okButton, lpWrapWrap);
            l1.addView(l17);
        }
        topLayout.addView(l1);
    }

    int safeParseInt(String s) {
        if ((s == null) || s.isEmpty()) { return 0; }
        return Integer.parseInt(s);
    }

    @SuppressLint({"ApplySharedPref", "SetTextI18n"})
    private void handleHueChanged(int val, int oldColour) {
        int r = 0;
        int g = 0;
        int b = 0;
        if (val < 256) { r = 255; g = val; }
        else if (val < 511) { r = 510 - val; g = 255; }
        else if (val < 766) { g = 255; b = val - 510; }
        else if (val < 1021) { g = 1020 - val; b = 255; }
        else if (val < 1275) { b = 255; r = val - 1020; }
        else { b = 1530 - val; r = 255; }
        int whiteness = 255;
        int blackness = 0;
        int col = (oldColour >> 16) & 0xFF;
        if (whiteness > col) { whiteness = col; }
        if (blackness < col) { blackness = col; }
        col = (oldColour >> 8) & 0xFF;
        if (whiteness > col) { whiteness = col; }
        if (blackness < col) { blackness = col; }
        col = oldColour & 0xFF;
        if (whiteness > col) { whiteness = col; }
        if (blackness < col) { blackness = col; }
        r = whiteness + r * (blackness - whiteness) / 255;
        if (r > 255) { r = 255; } // safety check
        g = whiteness + g * (blackness - whiteness) / 255;
        if (g > 255) { g = 255; } // safety check
        b = whiteness + b * (blackness - whiteness) / 255;
        if (b > 255) { b = 255; } // safety check
        redSlider.setProgress(r);
        redValue.setText(Integer.toString(r));
        greenSlider.setProgress(g);
        greenValue.setText(Integer.toString(g));
        blueSlider.setProgress(b);
        blueValue.setText(Integer.toString(b));
        col = (oldColour & 0xFF000000) + (((r << 8) + g) << 8) + b;
        fillBitmapInBackground(col, false);
        if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
            m_bgcolour = col;
            demo.setBackgroundColor(m_bgcolour);
            prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
            alphaSlider.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {
                    m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
        } else {
            m_fgcolour = col;
            demo.setTextColor(m_fgcolour);
            ColorStateList cl = ColorStateList.valueOf(m_fgcolour);
            alphaSlider.setProgressTintList(cl);
            alphaSlider.setThumbTintList(cl);
            prefs.edit().putInt("Wfgcolour", m_fgcolour).commit();
        }
        updateWidget();
    }

    void setHueSlider(int colour) {
        int r = (colour >> 16) & 0xFF;
        int g = (colour >> 8) & 0xFF;
        int b = colour & 0xFF;
        int hue = 0;
        if (r >= g) {
            if (g >= b) {
                if (r > b) {
                    // r >= g >= b, not both ==
                    hue = (g - b) * 255 / (r - b);
                } // else r == g == b, hue is indeterminate, 0 will do
            } else if (r >= b) {
                // r >= b > g
                hue = 1530 - (b - g) * 255 / (r - g);
            } else {
                // b > r >= g
                hue = 1020 + (r - g) * 255 / (b - g);
            }
        } else if (r >= b) {
            // g > r >= b
            hue = 510 - (r - b) * 255 / (g - b);
        } else if (g >= b) {
            // g >= b > r
            hue = 510 + (b - r) * 255 / (g - r);
        } else {
            // b > g > r
            hue = 1020 - (g - r) * 255 / (b - r);
        }
        hueSlider.setProgress(hue);
    }

@Override
    @SuppressLint({"ApplySharedPref", "RtlHardcoded", "SetTextI18n"})
    protected void onResume() {
        super.onResume();
        appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        secondsClockWidget = new ComponentName(
            getApplicationContext(), SecondsClockWidget.class);
        alphaValue = new EditText(this);
        alphaSlider = new LongClickableSeekBar(this);
        redValue = new EditText(this);
        redSlider = new LongClickableSeekBar(this);
        greenValue = new EditText(this);
        greenSlider = new LongClickableSeekBar(this);
        blueValue = new EditText(this);
        blueSlider = new LongClickableSeekBar(this);
        demo = new TextClock(this);
        prefs = getSharedPreferences("SecondsClock", Context.MODE_PRIVATE);
        currentView = prefs.getInt("Wview", CONFIGUREWIDGET);
        m_bgcolour = prefs.getInt("Wbgcolour", 0x00000000);
        m_fgcolour = prefs.getInt("Wfgcolour",0xFFFFFFFF);
        showTime = prefs.getInt("WshowTime", 2); // include seconds
        showWeekDay = prefs.getInt("WshowWeekDay",2); // long format
        showShortDate = prefs.getInt("WshowShortDate",0);
        showMonthDay = prefs.getInt("WshowMonthDay",1);
        showMonth = prefs.getInt("WshowMonth", 2); // long format
        showYear = prefs.getInt("WshowYear", 1);
        Resources res = getResources();
        metrics = res.getDisplayMetrics();
        config = res.getConfiguration();
        helptext = new TextView(this);

        // The 1.3 is a fudge factor = I don't know why it is needed.
        int numberWidth = (int)(alphaValue.getPaint().measureText("000") * 1.3);
        demo.setGravity(Gravity.CENTER_HORIZONTAL);
        topLayout = findViewById(R.id.genericlayout);
        demobox = new LinearLayout(this);
        demoboxbox = new LinearLayout(this);
        lpWrapMatch =  new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT);
        showTimeCheckBox = new CheckBox(this);
        showSecondsCheckBox = new CheckBox(this);
        showShortWeekDayCheckBox = new CheckBox(this);
        showLongWeekDayCheckBox = new CheckBox(this);
        showShortDateCheckBox = new CheckBox(this);
        showMonthDayCheckBox = new CheckBox(this);
        showShortMonthCheckBox = new CheckBox(this);
        showLongMonthCheckBox = new CheckBox(this);
        showYearCheckBox = new CheckBox(this);
        bgButton = new Button(this);
        fgButton = new Button(this);
        lpMatchWrap = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lpWrapWrap = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        ac = this;
        hueSlider = new LongClickableSeekBar(this);
        lpMMWeight = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT);
        lpMMWeight.weight = 1.0F;
        lpMatchMatch = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT);
        okButton = new Button(this);

        alphaSlider.setMax(255);
        alphaSlider.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener()
            {
                @Override
                public void onProgressChanged(
                    SeekBar seekBar, int progress, boolean fromUser)
                {
                    if (!recursive) {
                        recursive = true;
                        int val = seekBar.getProgress();
                        alphaValue.setText(Integer.toString(val));
                        m_bgcolour = (val << 24) + (m_bgcolour & 0xFFFFFF);
                        demo.setBackgroundColor(m_bgcolour);
                        prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                        alphaSlider.setBackground(new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[] {
                                m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
                        updateWidget();
                        recursive = false;
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        alphaSlider.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.setalphasliderhelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        alphaSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
        ColorStateList cl = ColorStateList.valueOf(m_fgcolour);
        alphaSlider.setProgressTintList(cl);
        alphaSlider.setThumbTintList(cl);
        alphaValue.setInputType(TYPE_CLASS_NUMBER);
        alphaValue.setText("0");
        alphaValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    recursive = true;
                    int val = safeParseInt(s.toString());
                    if (val > 255) {
                        alphaValue.setText("255");
                        val = 255;
                    }
                    alphaSlider.setProgress(val);
                    m_bgcolour = (val << 24) + (m_bgcolour & 0xFFFFFF);
                    prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                    demo.setBackgroundColor(m_bgcolour);
                    alphaSlider.setBackground(new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[] {
                            m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
                    updateWidget();
                    recursive = false;
                }
            }
        });
        alphaValue.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.alphavaluehelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        alphaValue.setWidth(numberWidth);
        redSlider.setMax(255);
        redSlider.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener()
            {
                @Override
                public void onProgressChanged(
                    SeekBar seekBar, int progress, boolean fromUser)
                {
                    if (!recursive) {
                        recursive = true;
                        int val = seekBar.getProgress();
                        redValue.setText(Integer.toString(val));
                        if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
                            m_bgcolour = (val << 16) + (m_bgcolour & 0xFF00FFFF);
                            fillBitmapInBackground(m_bgcolour, false);
                            prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                            demo.setBackgroundColor(m_bgcolour);
                            alphaSlider.setBackground(new GradientDrawable(
                                GradientDrawable.Orientation.LEFT_RIGHT,
                                new int[] {
                                    m_bgcolour & 0xFFFFFF,
                                    m_bgcolour | 0xFF000000}));
                            setHueSlider(m_bgcolour);
                        } else {
                            m_fgcolour = (val << 16) + (m_fgcolour & 0xFF00FFFF);
                            fillBitmapInBackground(m_fgcolour, false);
                            prefs.edit().putInt("Wfgcolour", m_fgcolour).commit();
                            demo.setTextColor(m_fgcolour);
                            ColorStateList cl = ColorStateList.valueOf(m_fgcolour);
                            alphaSlider.setProgressTintList(cl);
                            alphaSlider.setThumbTintList(cl);
                            setHueSlider(m_fgcolour);
                        }
                        updateWidget();
                        recursive = false;
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        redSlider.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.setredsliderhelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        redSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {0xFF000000, 0xFFFF0000}));
        cl = ColorStateList.valueOf(0xFFFFFFFF);
        redSlider.setProgressTintList(cl);
        redSlider.setThumbTintList(cl);
        redValue.setInputType(TYPE_CLASS_NUMBER);
        redValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    recursive = true;
                    int val = safeParseInt(s.toString());
                    if (val > 255) {
                        redValue.setText("255");
                        val = 255;
                    }
                    redSlider.setProgress(val);
                    if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
                        m_bgcolour = (val << 16) + (m_bgcolour & 0xFF00FFFF);
                        fillBitmapInBackground(m_bgcolour, false);
                        prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                        demo.setBackgroundColor(m_bgcolour);
                        alphaSlider.setBackground(new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[] {
                                m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
                        setHueSlider(m_bgcolour);
                    } else {
                        m_fgcolour = (val << 16) + (m_fgcolour & 0xFF00FFFF);
                        fillBitmapInBackground(m_fgcolour, false);
                        prefs.edit().putInt("Wfgcolour", m_fgcolour).commit();
                        demo.setTextColor(m_fgcolour);
                        ColorStateList cl = ColorStateList.valueOf(m_fgcolour);
                        alphaSlider.setProgressTintList(cl);
                        alphaSlider.setThumbTintList(cl);
                        setHueSlider(m_fgcolour);
                    }
                    updateWidget();
                    recursive = false;
                }
            }
        });
        redValue.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.redvaluehelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        redValue.setWidth(numberWidth);
        greenSlider.setMax(255);
        greenSlider.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener()
            {
                @Override
                public void onProgressChanged(
                    SeekBar seekBar, int progress, boolean fromUser)
                {
                    if (!recursive) {
                        recursive = true;
                        int val = seekBar.getProgress();
                        greenValue.setText(Integer.toString(val));
                        if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
                            m_bgcolour = (val << 8) + (m_bgcolour & 0xFFFF00FF);
                            fillBitmapInBackground(m_bgcolour, false);
                            prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                            demo.setBackgroundColor(m_bgcolour);
                            alphaSlider.setBackground(new GradientDrawable(
                                GradientDrawable.Orientation.LEFT_RIGHT,
                                new int[] {
                                    m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
                            setHueSlider(m_bgcolour);
                        } else {
                            m_fgcolour = (val << 8) + (m_fgcolour & 0xFFFF00FF);
                            fillBitmapInBackground(m_fgcolour, false);
                            prefs.edit().putInt("Wfgcolour", m_fgcolour).commit();
                            demo.setTextColor(m_fgcolour);
                            ColorStateList cl = ColorStateList.valueOf(m_fgcolour);
                            alphaSlider.setProgressTintList(cl);
                            alphaSlider.setThumbTintList(cl);
                            setHueSlider(m_fgcolour);
                        }
                        updateWidget();
                        recursive = false;
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        greenSlider.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.setgreensliderhelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        greenSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {0xFF000000, 0xFF00FF00}));
        greenSlider.setProgressTintList(cl);
        greenSlider.setThumbTintList(cl);
        greenValue.setInputType(TYPE_CLASS_NUMBER);
        greenValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    recursive = true;
                    int val = safeParseInt(s.toString());
                    if (val > 255) {
                        greenValue.setText("255");
                        val = 255;
                    }
                    greenSlider.setProgress(val);
                    if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
                        m_bgcolour = (val << 8) + (m_bgcolour & 0xFFFF00FF);
                        fillBitmapInBackground(m_bgcolour, false);
                        prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                        demo.setBackgroundColor(m_bgcolour);
                        alphaSlider.setBackground(new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[] {
                                m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
                        setHueSlider(m_bgcolour);
                    } else {
                        m_fgcolour = (val << 8) + (m_fgcolour & 0xFFFF00FF);
                        fillBitmapInBackground(m_fgcolour, false);
                        prefs.edit().putInt("Wfgcolour", m_fgcolour).commit();
                        demo.setTextColor(m_fgcolour);
                        ColorStateList cl = ColorStateList.valueOf(m_fgcolour);
                        alphaSlider.setProgressTintList(cl);
                        alphaSlider.setThumbTintList(cl);
                        setHueSlider(m_fgcolour);
                    }
                    updateWidget();
                    recursive = false;
                }
            }
        });
        greenValue.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.greenvaluehelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        greenValue.setWidth(numberWidth);
        blueSlider.setMax(255);
        blueSlider.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener()
            {
                @Override
                public void onProgressChanged(
                    SeekBar seekBar, int progress, boolean fromUser)
                {
                    if (!recursive) {
                        recursive = true;
                        int val = seekBar.getProgress();
                        blueValue.setText(Integer.toString(val));
                        int colour;
                        if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
                            m_bgcolour = val + (m_bgcolour & 0xFFFFFF00);
                            fillBitmapInBackground(m_bgcolour, false);
                            prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                            demo.setBackgroundColor(m_bgcolour);
                            alphaSlider.setBackground(new GradientDrawable(
                                GradientDrawable.Orientation.LEFT_RIGHT,
                                new int[]{
                                    m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
                            setHueSlider(m_bgcolour);
                        } else {
                            m_fgcolour = val + (m_fgcolour & 0xFFFFFF00);
                            fillBitmapInBackground(m_fgcolour, false);
                            prefs.edit().putInt("Wfgcolour", m_fgcolour).commit();
                            demo.setTextColor(m_fgcolour);
                            fillBitmapInBackground(m_fgcolour, false);
                            ColorStateList cl = ColorStateList.valueOf(m_fgcolour);
                            alphaSlider.setProgressTintList(cl);
                            alphaSlider.setThumbTintList(cl);
                            setHueSlider(m_bgcolour);
                        }
                        updateWidget();
                        recursive = false;
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        blueSlider.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.setbluesliderhelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        blueSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {0xFF000000, 0xFF0000FF}));
        blueSlider.setProgressTintList(cl);
        blueSlider.setThumbTintList(cl);
        blueValue.setInputType(TYPE_CLASS_NUMBER);
        blueValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    recursive = true;
                    int val = safeParseInt(s.toString());
                    if (val > 255) {
                        blueValue.setText("255");
                        val = 255;
                    }
                    blueSlider.setProgress(val);
                    if (currentView == SETWIDGETBACKGROUNDCOLOUR) {
                        m_bgcolour = val + (m_bgcolour & 0xFFFFFF00);
                        fillBitmapInBackground(m_bgcolour, false);
                        prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                        demo.setBackgroundColor(m_bgcolour);
                        alphaSlider.setBackground(new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[] {
                                m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
                        setHueSlider(m_bgcolour);
                    } else {
                        m_fgcolour = val + (m_fgcolour & 0xFFFFFF00);
                        fillBitmapInBackground(m_fgcolour, false);
                        prefs.edit().putInt("Wfgcolour", m_fgcolour).commit();
                        demo.setTextColor(m_fgcolour);
                        ColorStateList cl = ColorStateList.valueOf(m_fgcolour);
                        alphaSlider.setProgressTintList(cl);
                        alphaSlider.setThumbTintList(cl);
                        setHueSlider(m_bgcolour);
                    }
                    updateWidget();
                    recursive = false;
                }
            }
        });
        blueValue.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.bluevaluehelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        blueValue.setWidth(numberWidth);
        demo.setBackgroundColor(m_bgcolour);
        demo.setTextColor(m_fgcolour);
        demo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.demohelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        String s = "";
        try
        {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName (), 0);
            s = getString(R.string.app_name) + " " + pi.versionName
                + " built " + getString(R.string.build_time) + "\n";
        } catch (PackageManager.NameNotFoundException ignore) {}
        helptext.setText(s + getString(R.string.longpresslabel));
        helptext.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                switch(currentView) {
                    case SETWIDGETBACKGROUNDCOLOUR:
                        Toast.makeText(ac, getString(R.string.backgroundcolourhelp),
                            Toast.LENGTH_LONG).show();
                        break;
                    case SETWIDGETTEXTCOLOUR:
                        Toast.makeText(ac, getString(R.string.textcolourhelphelp),
                            Toast.LENGTH_LONG).show();
                        break;
                    case CONFIGUREWIDGET:
                        Toast.makeText(ac, getString(R.string.editwidgethelp),
                            Toast.LENGTH_LONG).show();
                    break;
                }
                return true;
            }
        });
        demobox.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        demobox.setBackgroundResource(R.drawable.background);
        demoboxbox.setPadding(10, 0, 10, 0);
        if (showShortDate != 0) {
            showMonthDayCheckBox.setVisibility(View.GONE);
            showShortMonthCheckBox.setVisibility(View.GONE);
            showLongMonthCheckBox.setVisibility(View.GONE);
            showYearCheckBox.setVisibility(View.GONE);
        }
        showTimeCheckBox.setText(R.string.show_time);
        showTimeCheckBox.setChecked(showTime != 0);
        showTimeCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showTime = 1;
                        showSecondsCheckBox.setChecked(false);
                        showSecondsCheckBox.setVisibility(View.VISIBLE);
                    } else {
                        showTime = 0;
                        if (config.orientation !=
                            Configuration.ORIENTATION_LANDSCAPE)
                        {
                            showSecondsCheckBox.setVisibility(View.GONE);
                        } else {
                            showSecondsCheckBox.setVisibility(View.INVISIBLE);
                        }
                    }
                    prefs.edit().putInt("WshowTime", showTime).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showTimeCheckBox.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.showtimehelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        showSecondsCheckBox.setText(R.string.show_seconds);
        showSecondsCheckBox.setChecked(showTime == 2);
        showSecondsCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showTime = 2;
                    } else {
                        showTime = 1;
                    }
                    prefs.edit().putInt("WshowTime", showTime).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showSecondsCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showsecondshelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        showShortWeekDayCheckBox.setText(R.string.show_short_weekday);
        showShortWeekDayCheckBox.setChecked(showWeekDay == 1);
        showShortWeekDayCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showLongWeekDayCheckBox.setChecked(false);
                        showWeekDay = 1;
                    } else {
                        showWeekDay = 0;
                    }
                    prefs.edit().putInt("WshowWeekDay", showWeekDay).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showShortWeekDayCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showshortweekdayhelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        showLongWeekDayCheckBox.setText(R.string.show_long_weekday);
        showLongWeekDayCheckBox.setChecked(showWeekDay == 2);
        showLongWeekDayCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showShortWeekDayCheckBox.setChecked(false);
                        showWeekDay = 2;
                    } else {
                        showWeekDay = 0;
                    }
                    prefs.edit().putInt("WshowWeekDay", showWeekDay).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showLongWeekDayCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showlongweekdayhelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        showShortDateCheckBox.setText(R.string.show_short_date);
        showShortDateCheckBox.setChecked(showShortDate != 0);
        showShortDateCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showShortDate = 1;
                        showMonthDayCheckBox.setChecked(false);
                        showShortMonthCheckBox.setChecked(false);
                        showLongMonthCheckBox.setChecked(false);
                        showYearCheckBox.setChecked(false);
                        showYearCheckBox.setVisibility(View.GONE);
                        showMonthDayCheckBox.setVisibility(View.GONE);
                        showShortMonthCheckBox.setVisibility(View.GONE);
                        showLongMonthCheckBox.setVisibility(View.GONE);
                        showYearCheckBox.setVisibility(View.GONE);
                    } else {
                        showShortDate = 0;
                        showMonthDayCheckBox.setVisibility(View.VISIBLE);
                        showShortMonthCheckBox.setVisibility(View.VISIBLE);
                        showLongMonthCheckBox.setVisibility(View.VISIBLE);
                        showYearCheckBox.setVisibility(View.VISIBLE);
                    }
                    prefs.edit().putInt(
                        "WshowShortDate", showShortDate).commit();
                    prefs.edit().putInt("WshowMonthDay", showMonthDay).commit();
                    prefs.edit().putInt("WshowMonth", showMonth).commit();
                    prefs.edit().putInt("WshowYear", showYear).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showShortDateCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showshortdatehelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        showMonthDayCheckBox.setText(R.string.show_month_day);
        showMonthDayCheckBox.setChecked(showMonthDay != 0);
        showMonthDayCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showMonthDay = 1;
                    } else {
                        showMonthDay = 0;
                    }
                    prefs.edit().putInt("WshowMonthDay", showMonthDay).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showMonthDayCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showmonthdayhelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        showShortMonthCheckBox.setText(R.string.show_short_month);
        showShortMonthCheckBox.setChecked(showMonth == 1);
        showShortMonthCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showLongMonthCheckBox.setChecked(false);
                        showMonth = 1;
                    } else {
                        showMonth = 0;
                    }
                    prefs.edit().putInt("WshowMonth", showMonth).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showShortMonthCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showshortmonthhelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        showLongMonthCheckBox.setText(R.string.show_long_month);
        showLongMonthCheckBox.setChecked(showMonth == 2);
        showLongMonthCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showShortMonthCheckBox.setChecked(false);
                        showMonth = 2;
                    } else {
                        showMonth = 0;
                    }
                    prefs.edit().putInt("WshowMonth", showMonth).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showLongMonthCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showlongmonthhelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        showYearCheckBox.setText(R.string.show_year);
        showYearCheckBox.setChecked(showYear != 0);
        showYearCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showYear = 1;
                    } else {
                        showYear = 0;
                    }
                    prefs.edit().putInt("WshowYear", showYear).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showYearCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(ac, getString(R.string.showyearhelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        bgButton.setText(R.string.set_bg_colour);
        bgButton.setAllCaps(false);
        bgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentView = SETWIDGETBACKGROUNDCOLOUR;
                prefs.edit().putInt("Wview", currentView).commit();
                doChooserLayout();
            }
        });
        bgButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.setbgcolourhelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        fgButton.setAllCaps(false);
        fgButton.setText(R.string.set_fg_colour);
        fgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentView = SETWIDGETTEXTCOLOUR;
                prefs.edit().putInt("Wview", currentView).commit();
                doChooserLayout();
            }
        });
        fgButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.setfgcolourhelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        hueSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, new int[]
            {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00,
                0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000}));
        hueSlider.setProgressTintList(ColorStateList.valueOf(0xFFFFFFFF));
        hueSlider.setThumbTintList(ColorStateList.valueOf(0xFFFFFFFF));
        hueSlider.setMax(1530);
        hueSlider.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener()
            {
                @Override
                public void onProgressChanged(
                    SeekBar seekBar, int progress, boolean fromUser)
                {
                    if (!recursive) {
                        recursive = true;
                        handleHueChanged(seekBar.getProgress(),
                            (currentView == SETWIDGETBACKGROUNDCOLOUR)
                                ? m_bgcolour : m_fgcolour);
                        updateWidget();
                        recursive = false;
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        hueSlider.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.sethuesliderhelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        okButton.setText(R.string.done);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentView = CONFIGUREWIDGET;
                prefs.edit().putInt("Wview", currentView).commit();
                doMainLayout();
            }
        });
        okButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(ac, getString(R.string.donehelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        updateWidget();
        updateDemo();

        switch (currentView) {
            case SETWIDGETBACKGROUNDCOLOUR:
            case SETWIDGETTEXTCOLOUR:
                doChooserLayout();
                break;
            case CONFIGUREWIDGET:
                doMainLayout();
                break;
        }
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onBackPressed() {
        switch (currentView) {
            case SETWIDGETBACKGROUNDCOLOUR:
            case SETWIDGETTEXTCOLOUR:
                currentView = CONFIGUREWIDGET;
                prefs.edit().putInt("Wview", currentView).commit();
                doMainLayout();
                break;
            case CONFIGUREWIDGET:
                super.onBackPressed();
                break;
        }
    }
}
