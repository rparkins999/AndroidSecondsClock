/*
 * Copyright © 2022. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This class handles the settings for SecondsClockWidget.
 * It also shows what the widget will look like in 1x1 size.
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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

public class WidgetConfigureActivity extends ConfigureActivity
{
    private static final int SETBACKGROUNDCOLOUR = SETTEXTCOLOUR + 1;

    private int widgetId;
    private int maxHeight = 121;
    private int minWidth = 93;
    private int m_bgcolour;
    private TextClock demo;
    private int m_fgcolour;
    private int showTime;
    private LinearLayout demobox;
    private LinearLayout demoboxbox;
    private CheckBox showTimeCheckBox;
    private CheckBox showSecondsCheckBox;
    private Button bgButton;
    private Button fgButton;
    private Button chooseButton;
    private CheckBox sysClockCheckBox;
    private CheckBox configThisCheckBox;
    private CheckBox configNightClockCheckBox;
    private CheckBox runNightClockCheckBox;
    private CheckBox chooseCheckBox;

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case LONGPRESSHELP:
                switch(m_currentView) {
                    case CONFIGURE: doToast(R.string.widgetconfighelp); return true;
                    case SETBACKGROUNDCOLOUR: doToast(R.string.bgcolourhelp); return true;
                    case SETTEXTCOLOUR:
                        doToast(R.string.fgcolourhelp, m_CorW); return true;
                    case CHOOSE_ACTION: doToast(R.string.actionhelp); return true;
                }
                break;
            case ALPHASLIDER: doToast(R.string.alphasliderhelp); return true;
            case ALPHAVALUE: doToast(R.string.alphavaluehelp); return true;
            case WIDGETDEMO:
                doToast(R.string.demohelp);
                doToast(R.string.demohelp);
                return true;
            case SHOWTIME: doToast(R.string.showtimehelp); return true;
            case SHOWSECONDS: doToast(R.string.showsecondshelp); return true;
            case DONEBUTTON: doToast(R.string.donehelp); return true;
            case SETTEXTCOLOUR: doToast(R.string.setfgcolourhelp); return true;
            case SETBACKGROUNDCOLOUR: doToast(R.string.setbgcolourhelp); return true;
            case GO_SYSTEM_CLOCK:
                doToast(R.string.touchrunnightclock);
                return true;
            case CONFIGURE_EXISTING_WIDGET:
                doToast(R.string.helpconfigwidget);
                return true;
            case CONFIGURE_NIGHT_CLOCK:
                doToast(R.string.touchconfignightclock);
                return true;
            case GO_NIGHT_CLOCK:
                doToast(R.string.touchrunnightclock);
                return true;
            case CHOOSE_ACTION:
                if (m_currentView == CHOOSE_ACTION) {
                    doToast(R.string.touchchooseaction); return true;
                } else {
                    doToast(R.string.setactionhelp); return true;
                }
        }
        return super.onLongClick(v);
    }
    @SuppressLint("ApplySharedPref")
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            switch (buttonView.getId()) {
                case GO_SYSTEM_CLOCK:
                    configThisCheckBox.setChecked(false);
                    configNightClockCheckBox.setChecked(false);
                    runNightClockCheckBox.setChecked(false);
                    chooseCheckBox.setChecked(false);
                    m_prefs.edit().putInt(
                        m_key + "touchaction", GO_SYSTEM_CLOCK).commit();
                    break;
                case CONFIGURE_EXISTING_WIDGET:
                    sysClockCheckBox.setChecked(false);
                    configNightClockCheckBox.setChecked(false);
                    runNightClockCheckBox.setChecked(false);
                    chooseCheckBox.setChecked(false);
                    m_prefs.edit().putInt(m_key + "touchaction",
                        CONFIGURE_EXISTING_WIDGET).commit();
                    break;
                case CONFIGURE_NIGHT_CLOCK:
                    sysClockCheckBox.setChecked(false);
                    configThisCheckBox.setChecked(false);
                    runNightClockCheckBox.setChecked(false);
                    chooseCheckBox.setChecked(false);
                    m_prefs.edit().putInt(
                        m_key + "touchaction", CONFIGURE_NIGHT_CLOCK).commit();
                    break;
                case GO_NIGHT_CLOCK:
                    sysClockCheckBox.setChecked(false);
                    configThisCheckBox.setChecked(false);
                    configNightClockCheckBox.setChecked(false);
                    chooseCheckBox.setChecked(false);
                    m_prefs.edit().putInt(
                        m_key + "touchaction", GO_NIGHT_CLOCK).commit();
                    break;
                case CHOOSE_ACTION:
                    sysClockCheckBox.setChecked(false);
                    configThisCheckBox.setChecked(false);
                    configNightClockCheckBox.setChecked(false);
                    runNightClockCheckBox.setChecked(false);
                    m_prefs.edit().putInt(
                        m_key + "touchaction", CHOOSE_ACTION).commit();
                    break;
                default:
                    super.onCheckedChanged(buttonView, isChecked);
            }
        }
    }

    private void updateWidget() {
        int[] widgetIds;
        if (widgetId == -1) {
            widgetIds = appWidgetManager.getAppWidgetIds(secondsClockWidget);
        } else {
            widgetIds = new int[] {widgetId};
        }
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

    private void setAlphaSliderBackground() {
        alphaSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {
                m_bgcolour & 0xFFFFFF, m_bgcolour | 0xFF000000}));
    }

    private void setAlphaSliderTint() {
        ColorStateList cl = ColorStateList.valueOf(m_fgcolour);
        alphaSlider.setTrackTintList(cl);
        alphaSlider.setThumbTintList(cl);
    }

    @SuppressLint({"ApplySharedPref", "SetTextI18n"})
    private void updatehsv(int colour) {
        if (m_currentView == SETBACKGROUNDCOLOUR) {
            m_bgcolour = colour | (m_bgcolour & 0xFF000000);
            m_prefs.edit().putInt(m_key + "bgcolour", colour).commit();
            demo.setBackgroundColor(m_bgcolour);
            setAlphaSliderBackground();
        } else { // SETTEXTCOLOUR
            m_fgcolour = colour | (m_fgcolour & 0xFF000000);
            m_prefs.edit().putInt(m_key + "fgcolour", colour).commit();
            demo.setTextColor(m_fgcolour);
            setAlphaSliderTint();
        }
    }

    @SuppressLint({"ApplySharedPref"})
    @Override
    public void onValueChanged(Slider slider, int value) {
        switch(slider.getId()) {
            case HUESLIDER:
                updatehsv(hueChanged());
                break;
            case SATURATIONSLIDER:
                updatehsv(saturationChanged());
                break;
            case VALUESLIDER:
                updatehsv(valueChanged());
                fixTintList(slider, value);
                break;
            case REDSLIDER:
                if (m_currentView == SETBACKGROUNDCOLOUR) {
                    m_bgcolour = redSliderChanged(
                        value, m_bgcolour, m_key + "bgcolour");
                    demo.setBackgroundColor(m_bgcolour);
                    setAlphaSliderBackground();
                } else {
                    m_fgcolour = redSliderChanged(
                        value, m_fgcolour, m_key + "fgcolour");
                    demo.setTextColor(m_fgcolour);
                    setAlphaSliderTint();
                }
                break;
            case GREENSLIDER:
                if (m_currentView == SETBACKGROUNDCOLOUR) {
                    m_bgcolour = greenSliderChanged(
                        value, m_bgcolour, m_key + "bgcolour");
                    demo.setBackgroundColor(m_bgcolour);
                    setAlphaSliderBackground();
                } else {
                    m_fgcolour = greenSliderChanged(
                        value, m_fgcolour, m_key + "fgcolour");
                    demo.setTextColor(m_fgcolour);
                    setAlphaSliderTint();
                }
                break;
            case BLUESLIDER:
                if (m_currentView == SETBACKGROUNDCOLOUR) {
                    m_bgcolour = blueSliderChanged(
                        value, m_bgcolour, m_key + "bgcolour");
                    demo.setBackgroundColor(m_bgcolour);
                    setAlphaSliderBackground();
                } else {
                    m_fgcolour = blueSliderChanged(
                        value, m_fgcolour, m_key + "fgcolour");
                    demo.setTextColor(m_fgcolour);
                    setAlphaSliderTint();
                }
                break;
            case ALPHASLIDER:
                if (!recursive) {
                    recursive = true;
                    alphaValue.setText(String.valueOf(value));
                    recursive = false;
                }
                m_bgcolour = (value << 24) | (m_bgcolour & 0xFFFFFF);
                demo.setBackgroundColor(m_bgcolour);
                m_prefs.edit().putInt(m_key + "bgcolour", m_bgcolour).commit();
                setAlphaSliderBackground();
                break;
        }
        updateWidget();
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case SETTEXTCOLOUR: setCurrentView(SETTEXTCOLOUR); break;
            case SETBACKGROUNDCOLOUR: setCurrentView(SETBACKGROUNDCOLOUR); break;
            case CHOOSE_ACTION: setCurrentView(CHOOSE_ACTION); break;
            case DONEBUTTON:
                if (m_currentView == CONFIGURE) {
                    finish();
                } else {
                    setCurrentView(CONFIGURE);
                }
        }
    }

    void updateDemo() {
        Formatter f = new Formatter();
        f.set(this, minWidth, maxHeight,
            showTime, showWeekDay, showShortDate,
            showMonthDay, showMonth, showYear);
        int width = (int)(minWidth * m_density);
        int height = (int)(maxHeight * m_density);
        demo.setMinimumWidth(width);
        demo.setMaxWidth(width);
        demo.setMinimumHeight(height);
        demo.setMaxHeight(height);
        demo.setFormat12Hour(f.time12 + f.rest);
        demo.setFormat24Hour(f.time24 + f.rest);
        demo.setLines(f.lines);
        demo.setAutoSizeTextTypeUniformWithConfiguration(
            10, 3000,
            3, TypedValue.COMPLEX_UNIT_PX);
    }

    @SuppressLint("SetTextI18n")
    private void doMainLayout() {
        removeAllViews(m_topLayout);
        m_ColourType = "";
        demobox.addView(demo);
        demoboxbox.addView(demobox);
        ScrollView lscroll = new ScrollView(this);
        lscroll.setScrollbarFadingEnabled(false);
        LinearLayout l1 = new LinearLayout(this);
        l1.setBackgroundColor(0xFF000000);
        if (m_orientation == Configuration.ORIENTATION_LANDSCAPE) {
            l1.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout l2 = new LinearLayout(this);
            l2.setOrientation(LinearLayout.VERTICAL);
            m_helptext.setText(R.string.longpressvert);
            m_helptext.setGravity(Gravity.CENTER_HORIZONTAL);
            l2.addView(m_helptext);
            l2.setGravity(Gravity.CENTER_HORIZONTAL);
            demoboxbox.setLayoutParams(lpWrapMatch);
            demoboxbox.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setGravity(Gravity.CENTER_VERTICAL);
            l2.addView(demoboxbox);
            l2.addView(m_okButton, lpWrapWrap);
            l1.addView(l2);
            LinearLayout l3 = new LinearLayout(this);
            l3.setOrientation(LinearLayout.VERTICAL);
            LinearLayout l4 = new LinearLayout(this);
            // default orientation is HORIZONTAL
            l4.addView(showTimeCheckBox);
            l4.addView(showSecondsCheckBox);
            l3.addView(l4, lpWrapWrap);
            LinearLayout l5 = new LinearLayout(this);
            // default orientation is HORIZONTAL
            l5.addView(showShortWeekDayCheckBox);
            l5.addView(showLongWeekDayCheckBox);
            l3.addView(l5, lpWrapWrap);
            LinearLayout l6 = new LinearLayout(this);
            // default orientation is HORIZONTAL
            l6.addView(showShortDateCheckBox);
            l6.addView(showMonthDayCheckBox);
            l3.addView(l6, lpWrapWrap);
            LinearLayout l7 = new LinearLayout(this);
            // default orientation is HORIZONTAL
            l7.addView(showShortMonthCheckBox);
            l7.addView(showLongMonthCheckBox);
            l3.addView(l7, lpWrapWrap);
            l3.addView(showYearCheckBox);
            LinearLayout l8 = new LinearLayout(this);
            // default orientation is HORIZONTAL
            l8.addView(bgButton);
            l8.addView(fgButton);
            l3.addView(l8);
            l3.addView(chooseButton, lpWrapWrap);
            l1.addView(l3, lpWrapWrap);
        } else { // assume Portrait
            l1.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setLayoutParams(lpMatchWrap);
            demoboxbox.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setGravity(Gravity.CENTER_HORIZONTAL);
            l1.addView(demoboxbox);
            m_helptext.setText(R.string.longpresshoriz);
            m_helptext.setGravity(Gravity.NO_GRAVITY);
            l1.addView(m_helptext);
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
            l2.addView(chooseButton, lpWrapWrap);
            l2.addView(m_okButton, lpWrapWrap);
            l1.addView(l2);
        }
        lscroll.addView(l1);
        m_topLayout.addView(lscroll);
    }

    protected void doChooserLayout() {
        removeAllViews(m_topLayout);
        LinearLayout lc = makeChooser();
        int pad = (int)(5 * m_density);
        LinearLayout l1 = new LinearLayout(this);
        l1.setBackgroundColor(0xFF000000);
        demobox.addView(demo);
        demoboxbox.setLayoutParams(lpMatchWrap);
        demoboxbox.setOrientation(LinearLayout.VERTICAL);
        demoboxbox.setGravity(Gravity.CENTER_HORIZONTAL);
        demoboxbox.addView(demobox);
        if (m_orientation == Configuration.ORIENTATION_LANDSCAPE) {
            l1.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout l2 = new LinearLayout(this);
            // default orientation is HORIZONTAL
            l2.setLayoutParams(lpWrapMatch);
            l2.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout l3 = new LinearLayout(this);
            l3.setLayoutParams(lpWrapWrap);
            l3.setOrientation(LinearLayout.VERTICAL);
            l3.setGravity(Gravity.CENTER_HORIZONTAL);
            m_helptext.setText(R.string.longpressvert);
            m_helptext.setGravity(Gravity.CENTER_HORIZONTAL);
            l3.addView(m_helptext);
            l3.addView(demoboxbox);
            l3.addView(m_okButton, lpWrapWrap);
            l2.addView(l3);
            l1.addView(l2);
            LinearLayout l4 = new LinearLayout(this);
            l4.setOrientation(LinearLayout.VERTICAL);
            l4.setLayoutParams(lpMatchMatch);
            l4.setPadding(pad, 0, pad, 0);
            ScrollView l5 = new ScrollView(this);
            l5.setScrollbarFadingEnabled(false);
            LinearLayout l6 = new LinearLayout(this);
            l6.setOrientation(LinearLayout.VERTICAL);
            l6.addView(lc);
            if (m_currentView == SETBACKGROUNDCOLOUR) {
                LinearLayout l7 = new LinearLayout(this);
                // default orientation is HORIZONTAL
                l7.setLayoutParams(lpMatchWrap);
                LinearLayout l8 = new LinearLayout(this);
                // default orientation is HORIZONTAL
                l8.setLayoutParams(lpWrapMatch);
                l8.setGravity(Gravity.CENTER_VERTICAL);
                TextView tv = textLabel(R.string.alphalabel, ALPHASLIDER);
                tv.setWidth(m_width / 10);
                l8.addView(tv);
                l7.addView(l8);
                LinearLayout l9 = new LinearLayout(this);
                l9.setLayoutParams(lpMMWeight);
                l9.setOrientation(LinearLayout.VERTICAL);
                l9.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout l10 = new LinearLayout(this);
                l10.setBackgroundResource(R.drawable.background);
                l10.setOrientation(LinearLayout.VERTICAL);
                l10.setLayoutParams(lpMatchWrap);
                l10.addView(alphaSlider);
                l9.addView(l10);
                l7.addView(l9);
                LinearLayout l11 = new LinearLayout(this);
                // default orientation is HORIZONTAL
                l11.setLayoutParams(lpWrapWrap);
                l11.addView(alphaValue);
                l7.addView(l11);
                l6.addView(l7);
            }
            l5.addView(l6);
            l4.addView(l5);
            l1.addView(l4);
        } else { // assume Portrait
            l1.setOrientation(LinearLayout.VERTICAL);
            l1.setPadding(pad, 0, pad, 0);
            l1.addView(demoboxbox);
            m_helptext.setText(R.string.longpresshoriz);
            m_helptext.setGravity(Gravity.START);
            l1.addView(m_helptext);
            ScrollView l2 = new ScrollView(this);
            l2.setScrollbarFadingEnabled(false);
            LinearLayout l3 = new LinearLayout(this);
            l3.setOrientation(LinearLayout.VERTICAL);
            l3.setLayoutParams(lpMatchWrap);
            l3.setGravity(Gravity.CENTER_HORIZONTAL);
            l3.addView(lc);
            if (m_currentView == SETBACKGROUNDCOLOUR) {
                l3.addView(centredLabel(R.string.alphalabel, ALPHASLIDER));
                LinearLayout l4 = new LinearLayout(this);
                // default orientation is HORIZONTAL
                l4.setLayoutParams(lpMatchWrap);
                l4.setPadding(0, pad, 0, pad);
                LinearLayout l5 = new LinearLayout(this);
                l5.setLayoutParams(lpMMWeight);
                l5.setOrientation(LinearLayout.VERTICAL);
                l5.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout l6 = new LinearLayout(this);
                l6.setBackgroundResource(R.drawable.background);
                l6.setOrientation(LinearLayout.VERTICAL);
                l6.setLayoutParams(lpMatchWrap);
                l6.addView(alphaSlider);
                l5.addView(l6);
                l4.addView(l5);
                LinearLayout l7 = new LinearLayout(this);
                // default orientation is HORIZONTAL
                l7.setLayoutParams(lpWrapWrap);
                l7.addView(alphaValue);
                l4.addView(l7);
                l3.addView(l4);
            }
            LinearLayout l11 = new LinearLayout(this);
            l11.setLayoutParams(lpMatchWrap);
            l11.setOrientation(LinearLayout.VERTICAL);
            l11.setGravity(Gravity.CENTER_HORIZONTAL);
            l11.addView(m_okButton, lpWrapWrap);
            l3.addView(l11);
            l2.addView(l3);
            l1.addView(l2);
        }
        m_topLayout.addView(l1);
        switch (m_currentView) {
            case SETBACKGROUNDCOLOUR:
                m_ColourType = "widget background";
                demo.setBackgroundColor(m_bgcolour);
                int val = (m_bgcolour >> 24) & 0xFF;
                if (!recursive) {
                    recursive = true;
                    alphaValue.setText(String.valueOf(val));
                    recursive = false;
                }
                alphaSlider.setValue(val);
                setAlphaSliderBackground();
                setAlphaSliderTint();
                rgbChanged(m_bgcolour);
                break;
            case SETTEXTCOLOUR:
                m_ColourType = "widget text";
                demo.setTextColor(m_fgcolour);
                rgbChanged(m_fgcolour);
                break;
            case CONFIGURE:
        }
    }

    protected void doActionLayout() {
        removeAllViews(m_topLayout);
        demobox.addView(demo);
        demoboxbox.addView(demobox);
        LinearLayout lbuttons = new LinearLayout(this);
        lbuttons.setOrientation(LinearLayout.VERTICAL);
        lbuttons.addView(centredLabel(R.string.actionpage, LONGPRESSHELP));
        lbuttons.addView(sysClockCheckBox, lpWrapWrap);
        lbuttons.addView(configThisCheckBox, lpWrapWrap);
        lbuttons.addView(configNightClockCheckBox, lpWrapWrap);
        lbuttons.addView(runNightClockCheckBox, lpWrapWrap);
        lbuttons.addView(chooseCheckBox, lpWrapWrap);
        LinearLayout l1 = new LinearLayout(this);
        // default orientation is HORIZONTAL
        l1.setBackgroundColor(0xFF000000);
        if (m_orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LinearLayout l2 = new LinearLayout(this);
            l2.setOrientation(LinearLayout.VERTICAL);
            l2.setGravity(Gravity.CENTER_HORIZONTAL);
            m_helptext.setText(R.string.longpressvert);
            l2.addView(m_helptext);
            demoboxbox.setLayoutParams(lpWrapMatch);
            demoboxbox.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setGravity(Gravity.CENTER_VERTICAL);
            l2.addView(demoboxbox);
            l2.addView(m_okButton, lpWrapWrap);
            l1.addView(l2);
            l1.addView(lbuttons);
        } else {
            l1.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setLayoutParams(lpMatchWrap);
            demoboxbox.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setGravity(Gravity.CENTER_HORIZONTAL);
            l1.addView(demoboxbox);
            m_helptext.setText(R.string.longpresshoriz);
            m_helptext.setGravity(Gravity.NO_GRAVITY);
            l1.addView(m_helptext);
            lbuttons.addView(m_okButton, lpWrapWrap);
            l1.addView(lbuttons);
        }
        m_topLayout.addView(l1);
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
        m_currentView = viewnum;
        m_prefs.edit().putInt("Wview", m_currentView).commit();
        switch(m_currentView) {
            case CONFIGURE: doMainLayout(); break;
            case SETTEXTCOLOUR:
            case SETBACKGROUNDCOLOUR: doChooserLayout(); break;
            case CHOOSE_ACTION: doActionLayout(); break;
        }
    }

    @Override
    @SuppressLint({"ApplySharedPref", "RtlHardcoded", "SetTextI18n"})
    protected void resume() {
        Intent intent;
        int[] widgetIds
            = appWidgetManager.getAppWidgetIds(secondsClockWidget);
        if (widgetIds.length > 0) {
            // update widgets in case orientation has changed
            intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.setClassName("uk.co.yahoo.p1rpp.secondsclock",
                "uk.co.yahoo.p1rpp.secondsclock.SecondsClockWidget");
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
            sendBroadcast(intent);
        }
        intent = getIntent();
        if (intent.hasExtra("widgetID")) {
            widgetId = intent.getIntExtra("widgetID", 0);
            m_key = "W" + widgetId;
            Bundle newOptions =
                appWidgetManager.getAppWidgetOptions(widgetId);
            maxHeight = newOptions.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight);
            minWidth = newOptions.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidth);
        } else {
            widgetId = -1;
            m_key = "W";
            widgetIds =
               appWidgetManager.getAppWidgetIds(secondsClockWidget);
            if (widgetIds.length > 0) {
                Bundle newOptions =
                    appWidgetManager.getAppWidgetOptions(widgetIds[0]);
                maxHeight = newOptions.getInt(
                    AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight);
                minWidth = newOptions.getInt(
                    AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidth);
            } else {
                maxHeight = m_height / 24;
                minWidth = m_width / 18;
            }
        }
        super.resume();
        m_currentView = m_prefs.getInt("Wview", CONFIGURE);
        m_bgcolour = m_prefs.getInt(m_key + "bgcolour", 0x00000000);
        m_fgcolour = m_prefs.getInt(m_key + "fgcolour",0xFFFFFFFF);
        showTime = m_prefs.getInt(m_key + "showTime", 2); // include seconds
        demo = new TextClock(this);
        demo.setId(WIDGETDEMO);
        demo.setGravity(Gravity.CENTER_HORIZONTAL);
        demo.setBackgroundColor(m_bgcolour);
        demo.setTextColor(m_fgcolour);
        demo.setOnLongClickListener(this);
        demobox = new LinearLayout(this);
        demobox.setLayoutParams(lpWrapWrap);
        demobox.setBackgroundResource(R.drawable.background);
        demoboxbox = new LinearLayout(this);
        demoboxbox.setPadding(10, 0, 10, 0);
        showTimeCheckBox = new CheckBox(this);
        showTimeCheckBox.setId(SHOWTIME);
        showTimeCheckBox.setText(R.string.show_time);
        showTimeCheckBox.setChecked(showTime != 0);
        showTimeCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @SuppressLint({"ApplySharedPref"})
                @Override
                public void onCheckedChanged(
                    CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        showTime = 1;
                        showSecondsCheckBox.setChecked(false);
                        showSecondsCheckBox.setVisibility(View.VISIBLE);
                    } else {
                        showTime = 0;
                        if (m_orientation !=
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
        showTimeCheckBox.setOnLongClickListener(this);
        showSecondsCheckBox = new CheckBox(this);
        showSecondsCheckBox.setId(SHOWSECONDS);
        showSecondsCheckBox.setText(R.string.show_seconds);
        showSecondsCheckBox.setChecked(showTime == 2);
        showSecondsCheckBox.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @SuppressLint({"ApplySharedPref"})
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
        showSecondsCheckBox.setOnLongClickListener(this);
        bgButton = new Button(this);
        bgButton.setId(SETBACKGROUNDCOLOUR);
        bgButton.setText(R.string.setbgcolour);
        bgButton.setAllCaps(false);
        bgButton.setOnClickListener(this);
        bgButton.setOnLongClickListener(this);
        fgButton = new Button(this);
        fgButton.setId(SETTEXTCOLOUR);
        fgButton.setAllCaps(false);
        fgButton.setText(getString(R.string.setfgcolour, "widget"));
        fgButton.setOnClickListener(this);
        fgButton.setOnLongClickListener(this);
        chooseButton = new Button(this);
        chooseButton.setId(CHOOSE_ACTION);
        chooseButton.setAllCaps(false);
        chooseButton.setText(R.string.chooseaction);
        chooseButton.setOnClickListener(this);
        chooseButton.setOnLongClickListener(this);
        int action = m_prefs.getInt(m_key + "touchaction", CHOOSE_ACTION);
        sysClockCheckBox = new CheckBox(this);
        sysClockCheckBox.setId(GO_SYSTEM_CLOCK);
        sysClockCheckBox.setText(getString(R.string.gosysclock));
        sysClockCheckBox.setChecked(action == GO_SYSTEM_CLOCK);
        sysClockCheckBox.setOnCheckedChangeListener(this);
        sysClockCheckBox.setOnLongClickListener(this);
        configThisCheckBox = new CheckBox(this);
        configThisCheckBox.setId(CONFIGURE_EXISTING_WIDGET);
        configThisCheckBox.setText(getString(R.string.configwidget));
        configThisCheckBox.setChecked(action == CONFIGURE_EXISTING_WIDGET);
        configThisCheckBox.setOnCheckedChangeListener(this);
        configThisCheckBox.setOnLongClickListener(this);
        configNightClockCheckBox = new CheckBox(this);
        configNightClockCheckBox.setId(CONFIGURE_NIGHT_CLOCK);
        configNightClockCheckBox.setText(getString(R.string.confignightclock));
        configNightClockCheckBox.setChecked(action == CONFIGURE_NIGHT_CLOCK);
        configNightClockCheckBox.setOnCheckedChangeListener(this);
        configNightClockCheckBox.setOnLongClickListener(this);
        runNightClockCheckBox = new CheckBox(this);
        runNightClockCheckBox.setId(GO_NIGHT_CLOCK);
        runNightClockCheckBox.setText(getString(R.string.runnightclock));
        runNightClockCheckBox.setChecked(action == GO_NIGHT_CLOCK);
        runNightClockCheckBox.setOnCheckedChangeListener(this);
        runNightClockCheckBox.setOnLongClickListener(this);
        chooseCheckBox = new CheckBox(this);
        chooseCheckBox.setId(CHOOSE_ACTION);
        chooseCheckBox.setText(getString(R.string.chooseaction));
        chooseCheckBox.setChecked(action == CHOOSE_ACTION);
        chooseCheckBox.setOnCheckedChangeListener(this);
        chooseCheckBox.setOnLongClickListener(this);

        saturationValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            {}
            @SuppressLint({"ApplySharedPref"})
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    int colour = fixSaturation(s.toString());
                    if (m_currentView == SETBACKGROUNDCOLOUR) {
                        m_bgcolour = colour | (m_bgcolour & 0xFF000000);
                        m_prefs.edit().putInt(m_key + "bgcolour", m_bgcolour)
                               .commit();
                        demo.setBackgroundColor(m_bgcolour);
                        setAlphaSliderBackground();
                    } else {
                        m_fgcolour = colour | (m_fgcolour & 0xFF000000);
                        m_prefs.edit().putInt(m_key + "fgcolour", m_fgcolour)
                               .commit();
                        demo.setTextColor(m_fgcolour);
                        setAlphaSliderTint();
                    }
                    updateWidget();
                    saturationValue.setSelection(
                        saturationValue.getText().length());
                }
            }
        });
        valueValue.addTextChangedListener(new TextWatcher() {
            @SuppressLint({"ApplySharedPref"})
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            {}
            @SuppressLint({"ApplySharedPref"})
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    int colour = fixValue(s.toString());
                    if (m_currentView == SETBACKGROUNDCOLOUR) {
                        m_bgcolour = colour | (m_bgcolour & 0xFF000000);
                        m_prefs.edit().putInt(m_key + "bgcolour", m_bgcolour)
                               .commit();
                        demo.setBackgroundColor(m_bgcolour);
                        setAlphaSliderBackground();
                    } else {
                        m_fgcolour = colour | (m_fgcolour & 0xFF000000);
                        m_prefs.edit().putInt(m_key + "fgcolour", m_fgcolour)
                               .commit();
                        demo.setTextColor(m_fgcolour);
                        setAlphaSliderTint();
                    }
                    updateWidget();
                    valueValue.setSelection(valueValue.getText().length());
                }
            }
        });
        redValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            {}
            @SuppressLint({"ApplySharedPref"})
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    int val = safeParseInt(s.toString());
                    if (val > 255) {
                        redValue.setText("255");
                        return;
                    }
                    redSlider.setValue(val);
                    if (m_currentView == SETBACKGROUNDCOLOUR) {
                        m_bgcolour = (val << 16) | (m_bgcolour & 0xFF00FFFF);
                        m_prefs.edit().putInt(m_key + "bgcolour", m_bgcolour)
                               .commit();
                        demo.setBackgroundColor(m_bgcolour);
                        setAlphaSliderBackground();
                    } else {
                        m_fgcolour = (val << 16) | (m_fgcolour & 0xFF00FFFF);
                        m_prefs.edit().putInt(m_key + "fgcolour", m_fgcolour)
                               .commit();
                        demo.setTextColor(m_fgcolour);
                        setAlphaSliderTint();
                    }
                    rgbChanged();
                    updateWidget();
                    redValue.setSelection(redValue.getText().length());
                }
            }
        });
        greenValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            {}
            @SuppressLint({"ApplySharedPref"})
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    int val = safeParseInt(s.toString());
                    if (val > 255) {
                        greenValue.setText("255");
                        return;
                    }
                    greenSlider.setValue(val);
                    if (m_currentView == SETBACKGROUNDCOLOUR) {
                        m_bgcolour = (val << 8) | (m_bgcolour & 0xFFFF00FF);
                        m_prefs.edit().putInt(m_key + "bgcolour", m_bgcolour)
                               .commit();
                        demo.setBackgroundColor(m_bgcolour);
                        setAlphaSliderBackground();
                    } else {
                        m_fgcolour = (val << 8) | (m_fgcolour & 0xFFFF00FF);
                        m_prefs.edit().putInt(m_key + "fgcolour", m_fgcolour)
                               .commit();
                        demo.setTextColor(m_fgcolour);
                        setAlphaSliderTint();
                    }
                    rgbChanged();
                    updateWidget();
                    greenValue.setSelection(greenValue.getText().length());
                }
            }
        });
        blueValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            {}
            @SuppressLint({"ApplySharedPref"})
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    int val = safeParseInt(s.toString());
                    if (val > 255) {
                        blueValue.setText("255");
                        return;
                    }
                    blueSlider.setValue(val);
                    if (m_currentView == SETBACKGROUNDCOLOUR) {
                        m_bgcolour = val | (m_bgcolour & 0xFFFFFF00);
                        m_prefs.edit().putInt(m_key + "bgcolour", m_bgcolour)
                               .commit();
                        demo.setBackgroundColor(m_bgcolour);
                        setAlphaSliderBackground();
                    } else {
                        m_fgcolour = val | (m_fgcolour & 0xFFFFFF00);
                        m_prefs.edit().putInt(m_key + "fgcolour", m_fgcolour)
                               .commit();
                        demo.setTextColor(m_fgcolour);
                        setAlphaSliderTint();
                    }
                    rgbChanged();
                    updateWidget();
                    blueValue.setSelection(blueValue.getText().length());
                }
            }
        });
        alphaSlider = new Slider(this);
        alphaSlider.setId(ALPHASLIDER);
        alphaSlider.setOnLongClickListener(this);
        alphaSlider.setMax(255);
        alphaSlider.setOnChangeListener(this);
        alphaValue = new EditText(this);
        alphaValue.setId(ALPHAVALUE);
        alphaValue.setOnLongClickListener(this);
        alphaValue.setInputType(TYPE_CLASS_NUMBER);
        alphaValue.setWidth(m_numberWidth);
        setAlphaSliderBackground();
        setAlphaSliderTint();
        alphaValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            {}
            @SuppressLint({"ApplySharedPref"})
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    int val = safeParseInt(s.toString());
                    if (val > 255) {
                        alphaValue.setText("255");
                        return;
                    }
                    alphaSlider.setValue(val);
                    m_bgcolour = (val << 24) + (m_bgcolour & 0xFFFFFF);
                    m_prefs.edit().putInt(m_key + "bgcolour", m_bgcolour)
                           .commit();
                    demo.setBackgroundColor(m_bgcolour);
                    setAlphaSliderBackground();
                    updateWidget();
                    alphaValue.setSelection(alphaValue.getText().length());
                }
            }
        });
        updateDemo();
        setCurrentView(m_currentView);
    }
}
