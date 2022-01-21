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
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

public class WidgetConfigureActivity extends ConfigureActivity {

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
    static final int CONFIGURE = 0;
    static final int SETTEXTCOLOUR = 1;
    static final int SETBACKGROUNDCOLOUR = 2;
    private int m_bgcolour;
    private TextClock demo;
    private int m_fgcolour;
    private int showTime;
    private LinearLayout demobox;
    private LinearLayout demoboxbox;
    private LinearLayout.LayoutParams lpWrapMatch;
    private CheckBox showTimeCheckBox;
    private CheckBox showSecondsCheckBox;
    private Button bgButton;
    private Button fgButton;
    private LinearLayout.LayoutParams lpMatchWrap;
    private ViewGroup.LayoutParams lpWrapWrap;
    private SeekBar hueSlider;
    private LinearLayout.LayoutParams lpMMWeight;
    private LinearLayout.LayoutParams lpMatchMatch;
    private Button okButton;

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == LONGPRESSHELP) {
            switch(currentView) {
                case SETBACKGROUNDCOLOUR:
                    Toast.makeText(m_activity, getString(R.string.backgroundcolourhelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                case SETTEXTCOLOUR:
                    Toast.makeText(m_activity, getString(R.string.textcolourhelphelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                case CONFIGURE:
                    Toast.makeText(m_activity, getString(R.string.widgetconfighelp),
                        Toast.LENGTH_LONG).show();
                    return true;
            }
        }
        return super.onLongClick(v);
    }

    void updateDemo() {
        Formatter f = new Formatter();
        f.set(this, minWidth, maxHeight,
            showTime, showWeekDay, showShortDate,
            showMonthDay, showMonth, showYear);
        int width = (int)(minWidth * m_metrics.density);
        int height = (int)(maxHeight * m_metrics.density);
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

    @SuppressLint("SetTextI18n")
    private void setSliders(int colour) {
        ColorStateList cl = ColorStateList.valueOf(m_fgcolour);
        alphaSlider.setProgressTintList(cl);
        alphaSlider.setThumbTintList(cl);
        int val = (colour >> 16) & 0xFF;
        redValue.setText(Integer.toString(val));
        redSlider.setProgress(val);
        val = (colour >> 8) & 0xFF;
        greenValue.setText(Integer.toString(val));
        greenSlider.setProgress(val);
        val = colour & 0xFF;
        blueValue.setText(Integer.toString(val));
        blueSlider.setProgress(val);
        setHueSlider(colour);
        updateDemo();
        updateWidget();
    }

    private void setFgColour(int colour) {
        demo.setTextColor(colour);
        setSliders(colour);
    }

    @SuppressLint("SetTextI18n")
    private void setBgColour(int colour) {
        demo.setBackgroundColor(colour);
        int val = (colour >> 24) & 0xFF;
        alphaValue.setText(Integer.toString(val));
        alphaSlider.setProgress(val);
        alphaSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{
                m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
        setSliders(colour);
    }

    @SuppressLint({ "ApplySharedPref"})
    private void setColour(int colour) {
        switch (currentView) {
            case SETBACKGROUNDCOLOUR:
                m_bgcolour = colour;
                m_prefs.edit().putInt("Wbgcolour", colour).commit();
                setBgColour(colour);
                break;
            case SETTEXTCOLOUR:
                m_fgcolour = colour;
                m_prefs.edit().putInt("Wfgcolour", colour).commit();
                setFgColour(colour);
                break;
            case CONFIGURE:
        }
    }

    private void setColour() {
        switch (currentView) {
            case SETBACKGROUNDCOLOUR:
                setBgColour(m_bgcolour);
                break;
            case SETTEXTCOLOUR:
                setFgColour(m_fgcolour);
                break;
            case CONFIGURE:
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void doMainLayout() {
        super.doMainLayout();
        demobox.addView(demo);
        demoboxbox.addView(demobox);
        LinearLayout l1 = new LinearLayout(this);
        if (m_config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            l1.setOrientation(LinearLayout.HORIZONTAL);
            demoboxbox.setLayoutParams(lpWrapMatch);
            demoboxbox.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setGravity(Gravity.CENTER_VERTICAL);
            l1.addView(demoboxbox);
            LinearLayout l2 = new LinearLayout(this);
            l2.setOrientation(LinearLayout.VERTICAL);
            l2.addView(m_helptext);
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
            l1.addView(m_helptext);
            m_helptext.setGravity(Gravity.START);
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
        }
        m_topLayout.addView(l1);
    }

    @SuppressLint({"ClickableViewAccessibility"})
    protected void doChooserLayout() {
        super.doChooserLayout();
        int pad =(int)(5 * m_metrics.density);
        LinearLayout l1 = new LinearLayout(this);
        demobox.addView(demo);
        demoboxbox.addView(demobox);
        if (m_config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            l1.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout l3 = new LinearLayout(this);
            l3.setOrientation(LinearLayout.VERTICAL);
            l3.setLayoutParams(lpMatchMatch);
            l3.setPadding(pad, 0, pad, 0);
            demoboxbox.setLayoutParams(lpMatchWrap);
            demoboxbox.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setGravity(Gravity.CENTER_HORIZONTAL);
            l3.addView(demoboxbox);
            l3.addView(m_helptext);
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
            if (currentView == SETBACKGROUNDCOLOUR) {
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
            l1.addView(m_helptext);
            LinearLayout l2 = new LinearLayout(this);
            l2.setOrientation(LinearLayout.VERTICAL);
            l2.setLayoutParams(lpMatchWrap);
            l2.setGravity(Gravity.CENTER_HORIZONTAL);
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
            if (currentView == SETBACKGROUNDCOLOUR) {
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
        m_topLayout.addView(l1);
        if (!recursive) {
            recursive = true;
            setColour();
            recursive = false;
        }
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
        if (!recursive) {
            recursive = true;
            setColour((oldColour & 0xFF000000) + (((r << 8) + g) << 8) + b);
            recursive = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_CorW = "widget";
    }

    @Override
    protected void updateFromCheckBox() {
        updateWidget();
        updateDemo();
    }

    @SuppressLint("ApplySharedPref")
    @Override
    protected void setCurrentView(int viewnum) {
        currentView = viewnum;
        m_prefs.edit().putInt("Wview", currentView).commit();
        if (currentView == CONFIGURE) {
            doMainLayout();
        } else {
            doChooserLayout();
        }
    }

    @Override
    @SuppressLint({"ApplySharedPref", "RtlHardcoded", "SetTextI18n"})
    protected void onResume() {
        Intent intent = getIntent();
        if (intent.hasExtra("widgetID")) {
            m_key = "W" +  intent.getIntExtra("widgetID", 0);
        } else {
            m_key = "W";
        }
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
        currentView = m_prefs.getInt("Wview", CONFIGURE);
        m_bgcolour = m_prefs.getInt("Wbgcolour", 0x00000000);
        m_fgcolour = m_prefs.getInt("Wfgcolour",0xFFFFFFFF);
        showTime = m_prefs.getInt("WshowTime", 2); // include seconds

        // The 1.3 is a fudge factor = I don't know why it is needed.
        int numberWidth = (int)(alphaValue.getPaint().measureText("000") * 1.3);
        demo.setGravity(Gravity.CENTER_HORIZONTAL);
        demobox = new LinearLayout(this);
        demoboxbox = new LinearLayout(this);
        lpWrapMatch =  new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT);
        showTimeCheckBox = new CheckBox(this);
        showSecondsCheckBox = new CheckBox(this);
        bgButton = new Button(this);
        fgButton = new Button(this);
        lpMatchWrap = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lpWrapWrap = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
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
                        m_prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
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
                Toast.makeText(m_activity, getString(R.string.setalphasliderhelp),
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
                    m_prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
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
                Toast.makeText(m_activity, getString(R.string.alphavaluehelp),
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
                        if (currentView == SETBACKGROUNDCOLOUR) {
                            m_bgcolour = (val << 16) + (m_bgcolour & 0xFF00FFFF);
                            m_prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                            demo.setBackgroundColor(m_bgcolour);
                            alphaSlider.setBackground(new GradientDrawable(
                                GradientDrawable.Orientation.LEFT_RIGHT,
                                new int[] {
                                    m_bgcolour & 0xFFFFFF,
                                    m_bgcolour | 0xFF000000}));
                            setHueSlider(m_bgcolour);
                        } else {
                            m_fgcolour = (val << 16) + (m_fgcolour & 0xFF00FFFF);
                            m_prefs.edit().putInt("Wfgcolour", m_fgcolour).commit();
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
                Toast.makeText(m_activity, getString(R.string.setredsliderhelp),
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
                    if (currentView == SETBACKGROUNDCOLOUR) {
                        m_bgcolour = (val << 16) + (m_bgcolour & 0xFF00FFFF);
                        m_prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                        demo.setBackgroundColor(m_bgcolour);
                        alphaSlider.setBackground(new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[] {
                                m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
                        setHueSlider(m_bgcolour);
                    } else {
                        m_fgcolour = (val << 16) + (m_fgcolour & 0xFF00FFFF);
                        m_prefs.edit().putInt("Wfgcolour", m_fgcolour).commit();
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
                Toast.makeText(m_activity, getString(R.string.redvaluehelp),
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
                        if (currentView == SETBACKGROUNDCOLOUR) {
                            m_bgcolour = (val << 8) + (m_bgcolour & 0xFFFF00FF);
                            m_prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                            demo.setBackgroundColor(m_bgcolour);
                            alphaSlider.setBackground(new GradientDrawable(
                                GradientDrawable.Orientation.LEFT_RIGHT,
                                new int[] {
                                    m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
                            setHueSlider(m_bgcolour);
                        } else {
                            m_fgcolour = (val << 8) + (m_fgcolour & 0xFFFF00FF);
                            m_prefs.edit().putInt("Wfgcolour", m_fgcolour).commit();
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
                Toast.makeText(m_activity, getString(R.string.setgreensliderhelp),
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
                    if (currentView == SETBACKGROUNDCOLOUR) {
                        m_bgcolour = (val << 8) + (m_bgcolour & 0xFFFF00FF);
                        m_prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                        demo.setBackgroundColor(m_bgcolour);
                        alphaSlider.setBackground(new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[] {
                                m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
                        setHueSlider(m_bgcolour);
                    } else {
                        m_fgcolour = (val << 8) + (m_fgcolour & 0xFFFF00FF);
                        m_prefs.edit().putInt("Wfgcolour", m_fgcolour).commit();
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
                Toast.makeText(m_activity, getString(R.string.greenvaluehelp),
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
                        if (currentView == SETBACKGROUNDCOLOUR) {
                            m_bgcolour = val + (m_bgcolour & 0xFFFFFF00);
                            m_prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                            demo.setBackgroundColor(m_bgcolour);
                            alphaSlider.setBackground(new GradientDrawable(
                                GradientDrawable.Orientation.LEFT_RIGHT,
                                new int[]{
                                    m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
                            setHueSlider(m_bgcolour);
                        } else {
                            m_fgcolour = val + (m_fgcolour & 0xFFFFFF00);
                            m_prefs.edit().putInt("Wfgcolour", m_fgcolour).commit();
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
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        blueSlider.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(m_activity, getString(R.string.setbluesliderhelp),
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
                    if (currentView == SETBACKGROUNDCOLOUR) {
                        m_bgcolour = val + (m_bgcolour & 0xFFFFFF00);
                        m_prefs.edit().putInt("Wbgcolour", m_bgcolour).commit();
                        demo.setBackgroundColor(m_bgcolour);
                        alphaSlider.setBackground(new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[] {
                                m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
                        setHueSlider(m_bgcolour);
                    } else {
                        m_fgcolour = val + (m_fgcolour & 0xFFFFFF00);
                        m_prefs.edit().putInt("Wfgcolour", m_fgcolour).commit();
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
                Toast.makeText(m_activity, getString(R.string.bluevaluehelp),
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
                Toast.makeText(m_activity, getString(R.string.demohelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        demobox.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        demobox.setBackgroundResource(R.drawable.background);
        demoboxbox.setPadding(10, 0, 10, 0);
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
                        if (m_config.orientation !=
                            Configuration.ORIENTATION_LANDSCAPE)
                        {
                            showSecondsCheckBox.setVisibility(View.GONE);
                        } else {
                            showSecondsCheckBox.setVisibility(View.INVISIBLE);
                        }
                    }
                    m_prefs.edit().putInt("WshowTime", showTime).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showTimeCheckBox.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(m_activity, getString(R.string.showtimehelp),
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
                    m_prefs.edit().putInt("WshowTime", showTime).commit();
                    updateWidget();
                    updateDemo();
                }
            });
        showSecondsCheckBox.setOnLongClickListener(
            new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(m_activity, getString(R.string.showsecondshelp),
                        Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        bgButton.setText(R.string.set_bg_colour);
        bgButton.setAllCaps(false);
        bgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentView(SETBACKGROUNDCOLOUR);
            }
        });
        bgButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(m_activity, getString(R.string.setbgcolourhelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        fgButton.setAllCaps(false);
        fgButton.setText(R.string.set_fg_colour);
        fgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentView(SETTEXTCOLOUR);
            }
        });
        fgButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(m_activity, getString(R.string.setfgcolourhelp),
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
                    handleHueChanged(seekBar.getProgress(),
                        (currentView == SETBACKGROUNDCOLOUR)
                            ? m_bgcolour : m_fgcolour);
                    updateWidget();
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        hueSlider.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(m_activity, getString(R.string.sethuesliderhelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        okButton.setText(R.string.done);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentView(CONFIGURE);
            }
        });
        okButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(m_activity, getString(R.string.donehelp),
                    Toast.LENGTH_LONG).show();
                return true;
            }
        });
        updateDemo();
        setCurrentView(currentView);
    }
}
