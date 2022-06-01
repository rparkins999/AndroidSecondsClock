/*
 * Copyright © 2022. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This is common logic for WidgetConfigureActivity and ClockConfigureActivity.
 * It doesn't need to be in the manifest because it is an abstract class
 * and no instance of it can ever be created.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import static android.text.InputType.TYPE_CLASS_NUMBER;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

abstract class ConfigureActivity extends Activity_common
    implements View.OnLongClickListener, CompoundButton.OnCheckedChangeListener,
    Slider.OnValueChangeListener, View.OnClickListener
{

    // base list of View IDs
    protected static final int LONGPRESSHELP = VIEWIDBASE;
    protected static final int SHOWTIME = LONGPRESSHELP + 1;
    protected static final int SHOWSECONDS = SHOWTIME + 1;
    protected static final int SHORTWEEKDAY = SHOWSECONDS + 1;
    protected static final int LONGWEEKDAY = SHORTWEEKDAY + 1;
    protected static final int SHORTDATE = LONGWEEKDAY + 1;
    protected static final int MONTHDAY = SHORTDATE + 1;
    protected static final int SHORTMONTH = MONTHDAY + 1;
    protected static final int LONGMONTH = SHORTMONTH + 1;
    protected static final int SHOWYEAR = LONGMONTH + 1;
    protected static final int SEVENSEGMENTS = SHOWYEAR + 1;
    protected static final int VERTICALTIME = SEVENSEGMENTS + 1;
    protected static final int HUESLIDER = VERTICALTIME + 1;
    protected static final int SATURATIONSLIDER = HUESLIDER + 1;
    protected static final int SATURATIONVALUE = SATURATIONSLIDER + 1;
    protected static final int VALUESLIDER = SATURATIONVALUE + 1;
    protected static final int VALUEVALUE = VALUESLIDER + 1;
    protected static final int REDSLIDER = VALUEVALUE + 1;
    protected static final int REDVALUE = REDSLIDER + 1;
    protected static final int GREENSLIDER = REDVALUE + 1;
    protected static final int GREENVALUE = GREENSLIDER + 1;
    protected static final int BLUESLIDER = GREENVALUE + 1;
    protected static final int BLUEVALUE = BLUESLIDER + 1;
    protected static final int ALPHASLIDER = BLUEVALUE + 1;
    protected static final int ALPHAVALUE = ALPHASLIDER + 1;
    protected static final int DONEBUTTON = ALPHAVALUE + 1;
    protected static final int WIDGETDEMO = DONEBUTTON + 1;
    protected static final int SETTEXTCOLOUR = WIDGETDEMO + 1;

    // common variables for widget and clock configue activities
    protected String m_CorW; // "clock" or "widget"
    protected int m_currentView;
    protected int m_numberWidth;
    protected boolean recursive = false;
    protected CheckBox showShortWeekDayCheckBox;
    protected CheckBox showLongWeekDayCheckBox;
    protected CheckBox showShortDateCheckBox;
    protected CheckBox showMonthDayCheckBox;
    protected CheckBox showShortMonthCheckBox;
    protected CheckBox showLongMonthCheckBox;
    protected CheckBox showYearCheckBox;
    protected Slider hueSlider;
    protected EditText hueSpacer;
    protected Slider saturationSlider;
    protected EditText saturationValue;
    protected Slider valueSlider;
    protected EditText valueValue; // two different meanings of value
    protected Slider redSlider;
    protected EditText redValue;
    protected Slider greenSlider;
    protected EditText greenValue;
    protected Slider blueSlider;
    protected EditText blueValue;
    protected Slider alphaSlider;
    protected EditText alphaValue;
    protected Button m_okButton;
    protected LinearLayout.LayoutParams lpMatchMatch;
    protected LinearLayout.LayoutParams lpMMWeight;
    protected LinearLayout.LayoutParams lpMatchWrap;
    protected LinearLayout.LayoutParams lpWrapMatch;
    protected ViewGroup.LayoutParams lpWrapWrap;

    // Common long click handling to display help toasts
    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case SHORTWEEKDAY:
                doToast(R.string.showshortweekdayhelp, m_CorW); return true;
            case LONGWEEKDAY:
                doToast(R.string.showlongweekdayhelp, m_CorW); return true;
            case SHORTDATE:
                doToast(R.string.showshortdatehelp, m_CorW); return true;
            case MONTHDAY:
                doToast(R.string.showmonthdayhelp, m_CorW); return true;
            case SHORTMONTH:
                doToast(R.string.showshortmonthhelp, m_CorW); return true;
            case LONGMONTH:
                doToast(R.string.showlongmonthhelp, m_CorW); return true;
            case SHOWYEAR:
                doToast(R.string.showyearhelp, m_CorW); return true;
            case SETTEXTCOLOUR:
                doToast(R.string.textcolourhelp, m_CorW); return true;
            case HUESLIDER:
                doToast(R.string.huesliderhelp); return true;
            case SATURATIONSLIDER:
                doToast(R.string.saturationsliderhelp); return true;
            case SATURATIONVALUE:
                doToast(R.string.saturationvaluehelp); return true;
            case VALUESLIDER:
                doToast(R.string.valuesliderhelp); return true;
            case VALUEVALUE:
                doToast(R.string.valuevaluehelp); return true;
            case REDSLIDER:
                doToast(R.string.redsliderhelp); return true;
            case REDVALUE:
                doToast(R.string.redvaluehelp); return true;
            case GREENSLIDER:
                doToast(R.string.greensliderhelp); return true;
            case GREENVALUE:
                doToast(R.string.greenvaluehelp); return true;
            case BLUESLIDER:
                doToast(R.string.bluesliderhelp); return true;
            case BLUEVALUE:
                doToast(R.string.bluevaluehelp); return true;
            case DONEBUTTON:
                doToast(R.string.donehelp); return true;
        }
        return false;
    }

    protected abstract void updateFromCheckBox();

    @SuppressLint("ApplySharedPref")
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case SHORTWEEKDAY:
                if (isChecked) {
                    showLongWeekDayCheckBox.setChecked(false);
                    showWeekDay = 1;
                } else {
                    showWeekDay = 0;
                }
                m_prefs.edit().putInt(m_key + "showWeekDay", showWeekDay).commit();
                updateFromCheckBox();
                break;
            case LONGWEEKDAY:
                if (isChecked) {
                    showShortWeekDayCheckBox.setChecked(false);
                    showWeekDay = 2;
                } else {
                    showWeekDay = 0;
                }
                m_prefs.edit().putInt(m_key + "showWeekDay", showWeekDay).commit();
                updateFromCheckBox();
                break;
            case SHORTDATE:
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
                m_prefs.edit().putInt(m_key + "showShortDate", showShortDate).commit();
                m_prefs.edit().putInt(m_key + "showMonthDay", showMonthDay).commit();
                m_prefs.edit().putInt(m_key + "showMonth", showMonth).commit();
                m_prefs.edit().putInt(m_key + "showYear", showYear).commit();
                updateFromCheckBox();
                break;
            case MONTHDAY:
                showMonthDay = isChecked ? 1: 0;
                m_prefs.edit().putInt(m_key + "showMonthDay", showMonthDay).commit();
                updateFromCheckBox();
                break;
            case SHORTMONTH:
                if (isChecked) {
                    showLongMonthCheckBox.setChecked(false);
                    showMonth = 1;
                } else {
                    showMonth = 0;
                }
                m_prefs.edit().putInt(m_key + "showMonth", showMonth).commit();
                updateFromCheckBox();
                break;
            case LONGMONTH:
                if (isChecked) {
                    showShortMonthCheckBox.setChecked(false);
                    showMonth = 2;
                } else {
                    showMonth = 0;
                }
                m_prefs.edit().putInt(m_key + "showMonth", showMonth).commit();
                updateFromCheckBox();
                break;
            case SHOWYEAR:
                showYear = isChecked ? 1: 0;
                m_prefs.edit().putInt(m_key + "showYear", showYear).commit();
                updateFromCheckBox();
                break;
        }
    }

    // Adjust colour of track and slider to ensure contrast with background
    protected void fixTintList(@NonNull Slider slider, int value) {
        ColorStateList cl = ColorStateList.valueOf(
            value < 128 ? 0xFFFFFFFF : 0xFF000000);
        slider.setTrackTintList(cl);
        slider.setThumbTintList(cl);
    }

    public abstract void onValueChanged(Slider slider, int value);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) { m_currentView = CONFIGURE; }
        setContentView(R.layout.generic_layout);
    }

    protected LinearLayout makeChooser() {
        LinearLayout lhue = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lhue.setLayoutParams(lpMatchWrap);
        LinearLayout lshue = new LinearLayout(this);
        lshue.setOrientation(LinearLayout.VERTICAL);
        lshue.setLayoutParams(lpMMWeight);
        lshue.setGravity(Gravity.CENTER_VERTICAL);
        lshue.addView(hueSlider);
        LinearLayout lvhue = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lvhue.setLayoutParams(lpWrapWrap);
        lvhue.addView(hueSpacer);
        LinearLayout lsat = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lsat.setLayoutParams(lpMatchWrap);
        LinearLayout lssat = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lssat.setOrientation(LinearLayout.VERTICAL);
        lssat.setLayoutParams(lpMMWeight);
        lssat.setGravity(Gravity.CENTER_VERTICAL);
        lssat.addView(saturationSlider);
        LinearLayout lvsat = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lvsat.setLayoutParams(lpWrapWrap);
        lvsat.addView(saturationValue);
        LinearLayout lval = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lval.setLayoutParams(lpMatchWrap);
        LinearLayout lsval = new LinearLayout(this);
        lsval.setOrientation(LinearLayout.VERTICAL);
        lsval.setLayoutParams(lpMMWeight);
        lsval.setGravity(Gravity.CENTER_VERTICAL);
        lsval.addView(valueSlider);
        LinearLayout lvval = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lvval.setLayoutParams(lpWrapWrap);
        lvval.addView(valueValue);
        LinearLayout lred = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lred.setLayoutParams(lpMatchWrap);
        LinearLayout lsred = new LinearLayout(this);
        lsred.setOrientation(LinearLayout.VERTICAL);
        lsred.setLayoutParams(lpMMWeight);
        lsred.setGravity(Gravity.CENTER_VERTICAL);
        lsred.addView(redSlider);
        LinearLayout lvred = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lvred.setLayoutParams(lpWrapWrap);
        lvred.addView(redValue);
        LinearLayout lgrn = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lgrn.setLayoutParams(lpMatchWrap);
        LinearLayout lsgrn = new LinearLayout(this);
        lsgrn.setOrientation(LinearLayout.VERTICAL);
        lsgrn.setLayoutParams(lpMMWeight);
        lsgrn.setGravity(Gravity.CENTER_VERTICAL);
        lsgrn.addView(greenSlider);
        LinearLayout lvgrn = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lvgrn.setLayoutParams(lpWrapWrap);
        lvgrn.addView(greenValue);
        LinearLayout lblu = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lblu.setLayoutParams(lpMatchWrap);
        LinearLayout lsblu = new LinearLayout(this);
        lsblu.setOrientation(LinearLayout.VERTICAL);
        lsblu.setLayoutParams(lpMMWeight);
        lsblu.setGravity(Gravity.CENTER_VERTICAL);
        lsblu.addView(blueSlider);
        LinearLayout lvblu = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lvblu.setLayoutParams(lpWrapWrap);
        lvblu.addView(blueValue);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        if (m_orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LinearLayout lthue = new LinearLayout(this);
            // default orientation is HORIZONTAL
            lthue.setLayoutParams(lpWrapMatch);
            lthue.setGravity(Gravity.CENTER_VERTICAL);
            TextView tv = textLabel(R.string.huelabel, HUESLIDER);
            tv.setWidth(m_width / 10);
            lthue.addView(tv);
            lhue.addView(lthue);
            lhue.addView(lshue);
            lhue.addView(lvhue);
            ll.addView(lhue);
            LinearLayout ltsat = new LinearLayout(this);
            // default orientation is HORIZONTAL
            ltsat.setLayoutParams(lpWrapMatch);
            ltsat.setGravity(Gravity.CENTER_VERTICAL);
            tv = textLabel(R.string.saturationlabel, SATURATIONSLIDER);
            tv.setWidth(m_width / 10);
            ltsat.addView(tv);
            lsat.addView(ltsat);
            lsat.addView(lssat);
            lsat.addView(lvsat);
            ll.addView(lsat);
            LinearLayout ltval = new LinearLayout(this);
            // default orientation is HORIZONTAL
            ltval.setLayoutParams(lpWrapMatch);
            ltval.setGravity(Gravity.CENTER_VERTICAL);
            tv = textLabel(R.string.valuelabel, VALUESLIDER);
            tv.setWidth(m_width / 10);
            ltval.addView(tv);
            lval.addView(ltval);
            lval.addView(lsval);
            lval.addView(lvval);
            ll.addView(lval);
            LinearLayout ltred = new LinearLayout(this);
            // default orientation is HORIZONTAL
            ltred.setLayoutParams(lpWrapMatch);
            ltred.setGravity(Gravity.CENTER_VERTICAL);
            tv = textLabel(R.string.redlabel, REDSLIDER);
            tv.setWidth(m_width / 10);
            ltred.addView(tv);
            lred.addView(ltred);
            lred.addView(lsred);
            lred.addView(lvred);
            ll.addView(lred);
            LinearLayout ltgrn = new LinearLayout(this);
            // default orientation is HORIZONTAL
            ltgrn.setLayoutParams(lpWrapMatch);
            ltgrn.setGravity(Gravity.CENTER_VERTICAL);
            tv = textLabel(R.string.greenlabel, GREENSLIDER);
            tv.setWidth(m_width / 10);
            ltgrn.addView(tv);
            lgrn.addView(ltgrn);
            lgrn.addView(lsgrn);
            lgrn.addView(lvgrn);
            ll.addView(lgrn);
            LinearLayout ltblu = new LinearLayout(this);
            // default orientation is HORIZONTAL
            ltblu.setLayoutParams(lpWrapMatch);
            ltblu.setGravity(Gravity.CENTER_VERTICAL);
            tv = textLabel(R.string.bluelabel, BLUESLIDER);
            tv.setWidth(m_width / 10);
            ltblu.addView(tv);
            lblu.addView(ltblu);
            lblu.addView(lsblu);
            lblu.addView(lvblu);
            ll.addView(lblu);
        } else {
            ll.addView(centredLabel(R.string.huelabel, HUESLIDER));
            lhue.addView(lshue);
            lhue.addView(lvhue);
            ll.addView(lhue);
            ll.addView(centredLabel(R.string.saturationlabel, SATURATIONSLIDER));
            lsat.addView(lssat);
            lsat.addView(lvsat);
            ll.addView(lsat);
            ll.addView(centredLabel(R.string.valuelabel, VALUESLIDER));
            lval.addView(lsval);
            lval.addView(lvval);
            ll.addView(lval);
            ll.addView(centredLabel(R.string.redlabel, REDSLIDER));
            lred.addView(lsred);
            lred.addView(lvred);
            ll.addView(lred);
            ll.addView(centredLabel(R.string.greenlabel, GREENSLIDER));
            lgrn.addView(lsgrn);
            lgrn.addView(lvgrn);
            ll.addView(lgrn);
            ll.addView(centredLabel(R.string.bluelabel, BLUESLIDER));
            lblu.addView(lsblu);
            lblu.addView(lvblu);
            ll.addView(lblu);
        }
        return ll;
    }

    protected int safeParseInt(String s) {
        if ((s == null) || s.isEmpty()) { return 0; }
        return Integer.parseInt(s);
    }

    @Override
    protected void resume() {
        super.resume();
        m_helptext = new TextView(this);
        m_helptext.setId(LONGPRESSHELP);
        m_helptext.setOnLongClickListener(this);
        showShortWeekDayCheckBox = new CheckBox(this);
        showShortWeekDayCheckBox.setId(SHORTWEEKDAY);
        showShortWeekDayCheckBox.setChecked(showWeekDay == 1);
        showShortWeekDayCheckBox.setText(
            getString(R.string.show_short_weekday, m_CorW));
        showShortWeekDayCheckBox.setOnCheckedChangeListener(this);
        showShortWeekDayCheckBox.setOnLongClickListener(this);
        showLongWeekDayCheckBox = new CheckBox(this);
        showLongWeekDayCheckBox.setId(LONGWEEKDAY);
        showLongWeekDayCheckBox.setText(
            getString(R.string.show_long_weekday, m_CorW));
        showLongWeekDayCheckBox.setChecked(showWeekDay == 2);
        showLongWeekDayCheckBox.setOnCheckedChangeListener(this);
        showLongWeekDayCheckBox.setOnLongClickListener(this);
        showShortDateCheckBox = new CheckBox(this);
        showShortDateCheckBox.setId(SHORTDATE);
        showShortDateCheckBox.setChecked(showShortDate != 0);
        showShortDateCheckBox.setOnCheckedChangeListener(this);
        showShortDateCheckBox.setText(
            getString(R.string.show_short_date, m_CorW));
        showShortDateCheckBox.setOnLongClickListener(this);
        showMonthDayCheckBox = new CheckBox(this);
        showMonthDayCheckBox.setId(MONTHDAY);
        showMonthDayCheckBox.setChecked(showMonthDay != 0);
        showMonthDayCheckBox.setOnCheckedChangeListener(this);
        showMonthDayCheckBox.setText(
            getString(R.string.show_month_day, m_CorW));
        showMonthDayCheckBox.setOnLongClickListener(this);
        showShortMonthCheckBox = new CheckBox(this);
        showShortMonthCheckBox.setId(SHORTMONTH);
        showShortMonthCheckBox.setChecked(showMonth == 1);
        showShortMonthCheckBox.setOnCheckedChangeListener(this);
        showShortMonthCheckBox.setText(
            getString(R.string.show_short_month, m_CorW));
        showShortMonthCheckBox.setOnLongClickListener(this);
        showLongMonthCheckBox = new CheckBox(this);
        showLongMonthCheckBox.setId(LONGMONTH);
        showLongMonthCheckBox.setChecked(showMonth == 2);
        showLongMonthCheckBox.setOnCheckedChangeListener(this);
        showLongMonthCheckBox.setText(
            getString(R.string.show_long_month, m_CorW));
        showLongMonthCheckBox.setOnLongClickListener(this);
        showYearCheckBox = new CheckBox(this);
        showYearCheckBox.setId(SHOWYEAR);
        showYearCheckBox.setChecked(showYear != 0);
        showYearCheckBox.setOnCheckedChangeListener(this);
        showYearCheckBox.setText(getString(R.string.show_year, m_CorW));
        showYearCheckBox.setOnLongClickListener(this);
        if (showShortDate != 0) {
            showMonthDayCheckBox.setVisibility(View.GONE);
            showShortMonthCheckBox.setVisibility(View.GONE);
            showLongMonthCheckBox.setVisibility(View.GONE);
            showYearCheckBox.setVisibility(View.GONE);
        }
        hueSlider = new Slider(this);
        hueSlider.setId(HUESLIDER);
        hueSlider.setOnLongClickListener(this);
        hueSlider.setMax(1530);
        hueSlider.setOnChangeListener(this);
        hueSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, new int[]
            {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00,
                0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000}));
        hueSlider.setTrackTintList(ColorStateList.valueOf(0xFFFFFFFF));
        hueSlider.setThumbTintList(ColorStateList.valueOf(0xFFFFFFFF));
        hueSpacer = new EditText(this);
        hueSpacer.setWidth(m_numberWidth);
        hueSpacer.setVisibility(View.INVISIBLE);
        saturationSlider = new Slider(this);
        saturationSlider.setId(SATURATIONSLIDER);
        saturationSlider.setOnLongClickListener(this);
        saturationSlider.setMax(255);
        saturationSlider.setOnChangeListener(this);
        saturationValue = new EditText(this);
        saturationValue.setId(SATURATIONVALUE);
        saturationValue.setOnLongClickListener(this);
        saturationValue.setInputType(TYPE_CLASS_NUMBER);
        // The 1.3 is a fudge factor = I don't know why it is needed.
        m_numberWidth =
            (int)(saturationValue.getPaint().measureText("000") * 1.3);
        saturationValue.setWidth(m_numberWidth);
        valueSlider = new Slider(this);
        valueSlider.setId(VALUESLIDER);
        valueSlider.setOnLongClickListener(this);
        valueSlider.setMax(255);
        valueSlider.setOnChangeListener(this);
        valueValue = new EditText(this);
        valueValue.setId(VALUEVALUE);
        valueValue.setOnLongClickListener(this);
        valueValue.setInputType(TYPE_CLASS_NUMBER);
        valueValue.setWidth(m_numberWidth);
        redSlider = new Slider(this);
        redSlider.setId(REDSLIDER);
        redSlider.setOnLongClickListener(this);
        redSlider.setMax(255);
        redSlider.setOnChangeListener(this);
        redSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {0xFF000000, 0xFFFF0000}));
        ColorStateList cl = ColorStateList.valueOf(0xFFFFFFFF);
        redSlider.setTrackTintList(cl);
        redSlider.setThumbTintList(cl);
        redValue = new EditText(this);
        redValue.setId(REDVALUE);
        redValue.setOnLongClickListener(this);
        redValue.setInputType(TYPE_CLASS_NUMBER);
        redValue.setWidth(m_numberWidth);
        greenSlider = new Slider(this);
        greenSlider.setId(GREENSLIDER);
        greenSlider.setOnLongClickListener(this);
        greenSlider.setMax(255);
        greenSlider.setOnChangeListener(this);
        greenSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {0xFF000000, 0xFF00FF00}));
        greenSlider.setTrackTintList(cl);
        greenSlider.setThumbTintList(cl);
        greenValue = new EditText(this);
        greenValue.setId(GREENVALUE);
        greenValue.setOnLongClickListener(this);
        greenValue.setInputType(TYPE_CLASS_NUMBER);
        greenValue.setWidth(m_numberWidth);
        blueSlider = new Slider(this);
        blueSlider.setId(BLUESLIDER);
        blueSlider.setOnLongClickListener(this);
        blueSlider.setMax(255);
        blueSlider.setOnChangeListener(this);
        blueSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {0xFF000000, 0xFF0000FF}));
        blueSlider.setTrackTintList(cl);
        blueSlider.setThumbTintList(cl);
        blueValue = new EditText(this);
        blueValue.setId(BLUEVALUE);
        blueValue.setOnLongClickListener(this);
        blueValue.setInputType(TYPE_CLASS_NUMBER);
        blueValue.setInputType(TYPE_CLASS_NUMBER);
        blueValue.setWidth(m_numberWidth);
        m_okButton = new Button(this);
        m_okButton.setId(DONEBUTTON);
        m_okButton.setText(R.string.done);
        m_okButton.setOnClickListener(this);
        m_okButton.setOnLongClickListener(this);
        lpMatchMatch = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT);
        lpMMWeight = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT);
        lpMMWeight.weight = 1.0F;
        lpMatchWrap = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lpWrapMatch = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT);
        lpWrapWrap = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    // set the thumb and track tints for saturation and value
    private void setTints(int saturation, int value) {
        fixTintList(saturationSlider, 255- saturation);
        fixTintList(valueSlider, value);
    }

    // set the backgrounds for the saturation and value sliders
    private void setBackgrounds(int r, int g, int b) {
        int most = r;
        if (most < g) { most = g; }
        if (most < b) { most = b; }
        int x;
        if (most == 0) {
            x = 0;
        } else {
            x = (  (  (((r * 255) / most) << 8)
                        + ((g * 255) / most)) << 8)
                     + ((b * 255) / most);
        }
        valueSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {0xFF000000, x | 0xFF000000}));
        int least = r;
        if (least > g) { least = g; }
        if (least > b) { least = b; }
        int msat = 255 - most;
        x = ((((r + msat) << 8) + g + msat) << 8) + b + msat;
        int y = ((((r - least) << 8) + g - least) << 8) + b - least;
        saturationSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {x | 0xFF000000, y | 0xFF000000}));
    }

    // set the sliders and value fields for red, green, and blue
    private int setColour(int r, int g, int b) {
        setBackgrounds(r, g, b);
        redSlider.setValue(r);
        greenSlider.setValue(g);
        blueSlider.setValue(b);
        if (!recursive) {
            recursive = true;
            redValue.setText(String.valueOf(r));
            greenValue.setText(String.valueOf(g));
            blueValue.setText(String.valueOf(b));
            recursive = false;
        }
        return (((r << 8) + g) << 8) + b;
    }

    // called to update red, green, and blue if hue has changed
    protected int hueChanged() {
        int r;
        int g;
        int b;
        int hue = hueSlider.getValue();
        int saturation = saturationSlider.getValue();
        int value = valueSlider.getValue();
        int chroma = saturation * value / 255;
        int white = value - chroma;
        if (hue < 255) {
            r = chroma + white;
            g = chroma * hue / 255 + white;
            b = white;
        } else if (hue < 511) {
            r = chroma * (510 - hue) / 255 + white;
            g = chroma + white;
            b = white;
        } else if (hue < 766) {
            r = white;
            g = chroma + white;
            b = chroma * (hue - 510) / 255 + white;
        } else if (hue < 1021) {
            r = white;
            g = chroma * (1020 - hue) / 255 + white;
            b = chroma + white;
        } else if (hue < 1275) {
            r = chroma * (hue - 1020) / 255 + white;
            g = white;
            b = chroma + white;
        } else {
            r = chroma + white;
            g = white;
            b = chroma * (1530 - hue) / 255 + white;
        }
        return setColour(r, g, b);
    }

    // called to update red, green, and blue if saturation has changed
    protected int saturationChanged() {
        int r = redSlider.getValue();
        int g = greenSlider.getValue();
        int b = blueSlider.getValue();
        int saturation = saturationSlider.getValue();
        int value = valueSlider.getValue();
        if (saturation + value < 255) {
            value = 255 - saturation;
            valueSlider.setValue(value);
        }
        int most = r;
        if (most < g) { most = g; }
        if (most < b) { most = b; }
        int least = r;
        if (least > g) { least = g; }
        if (least > b) { least = b; }
        int delta = 255 - saturation - least;
        if (delta > 255 - most) { delta = 255 - most; }
        r = r + delta;
        g = g + delta;
        b = b + delta;
        setTints(saturation, value); // set the value fields
        if (!recursive) {
            recursive = true;
            saturationValue.setText(String.valueOf(saturation));
            valueValue.setText(String.valueOf(value));
            recursive = false;
        }
        return setColour(r, g, b); // set sliders and value fields
    }

    // called when saturation text value has changed
    @SuppressLint("SetTextI18n")
    protected int fixSaturation(String s) {
        int saturation = safeParseInt(s);
        if (saturation > 255) {
            saturationValue.setText("255"); // makes recursive call
            return 255;
        }
        int value = valueSlider.getValue();
        if (saturation + value < 255) {
            value = 255 - saturation;
            valueValue.setText(String.valueOf(value));
        }
        saturationSlider.setValue(saturation);
        return saturationChanged();
    }

    // called to update red, green, and blue if value has changed
    protected int valueChanged() {
        int r = redSlider.getValue();
        int g = greenSlider.getValue();
        int b = blueSlider.getValue();
        int saturation = saturationSlider.getValue();
        int value = valueSlider.getValue();
        if (saturation + value < 255) {
            saturation = 255 - value;
            saturationSlider.setValue(saturation);
        }
        int most = r;
        if (most < g) { most = g; }
        if (most < b) { most = b; }
        if (most == 0) {
            r = value;
            g = value;
            b = value;
        } else {
            r = r * value / most;
            g = g * value / most;
            b = b * value / most;
        }
        setTints(saturation, value);
        if (!recursive) {
            recursive = true;
            valueValue.setText(String.valueOf(value));
            saturationValue.setText(String.valueOf(saturation));
            recursive = false;
        }
        return setColour(r, g, b); // set sliders and value fields
    }

    // called when value text value has changed
    @SuppressLint("SetTextI18n")
    protected int fixValue(String s) {
        int value = safeParseInt(s.toString());
        if (value > 255) {
            valueValue.setText("255"); // makes recursive call
            return 255;
        }
        int saturation = saturationSlider.getValue();
        if (saturation + value < 255) {
            saturation = 255 - value;
            saturationValue.setText(String.valueOf(saturation));
        } else
        valueSlider.setValue(value);
        return valueChanged();
    }

    // called to update hue, saturation, and value
    // if red, green, or blue has changed
    private void rgbChanged(int r, int g, int b) {
        // This calculation is copied from Wikipedia, with acknowlegement
        int value = r;
        if (g > value) { value = g; }
        if (b > value) { value = b; }
        int least = r;
        if (g < least) { least = g; }
        if (b < least) { least = b; }
        int chroma = value - least;
        int hue;
        if (chroma == 0) { hue = 0; }
        else if (value == r) {
             hue = 255 * (g - b) / chroma + ((g > b)  ? 0 : 1530);
        } else if (value == g) { hue = 510 + 255 * (b - r) / chroma; }
        else { hue = 1020 + 255 * (r - g) / chroma; }
        hueSlider.setValue(hue);
        int saturation = value == 0 ? 0 : 255 * chroma / value;
        saturationSlider.setValue(saturation);
        saturationValue.setText(String.valueOf(saturation));
        valueSlider.setValue(value);
        valueValue.setText(String.valueOf(value));
        setBackgrounds(r, g, b);
        setTints(saturation, value);
    }

    // Called to set colour from preferences
    protected void rgbChanged(int colour) {
        int r = (colour >> 16) & 0xFF;
        int g = (colour >> 8) & 0xFF;
        int b = colour & 0xFF;
        setColour(r, g, b); // set red, green, and blue sliders and values
        rgbChanged(r, g, b); // set hue, saturation, and value
    }

    // Called when red, green, or blue has changed,
    // slider and value already updated
    protected void rgbChanged() {
        int r = redSlider.getValue();
        int g = greenSlider.getValue();
        int b = blueSlider.getValue();
        rgbChanged(r, g, b);
    }

    @SuppressLint("ApplySharedPref")
    protected int redSliderChanged(int r, int colour, String key) {
        if (r > 255) { r = 255; } else if (r < 0) { r = 0; }
        colour = (r << 16) | (colour & 0xFF00FFFF);
        int g = (colour >> 8) & 0xFF;
        int b = colour & 0xFF;
        m_prefs.edit().putInt(key, colour).commit();
        if (!recursive) {
            recursive = true;
            redValue.setText(String.valueOf(r));
            rgbChanged(r, g, b); // set hue, saturation, and value
            recursive = false;
        }
        return colour;
    }

    @SuppressLint("ApplySharedPref")
    protected int greenSliderChanged(int g, int colour, String key) {
        if (g > 255) { g = 255; } else if (g < 0) { g = 0; }
        int r = (colour >> 16) & 0xFF;
        colour = (g << 8) | (colour & 0xFFFF00FF);
        int b = colour & 0xFF;
        m_prefs.edit().putInt(key, colour).commit();
        if (!recursive) {
            recursive = true;
            greenValue.setText(String.valueOf(g));
            rgbChanged(r, g, b); // set hue, saturation, and value
            recursive = false;
        }
        return colour;
    }

    @SuppressLint("ApplySharedPref")
    protected int blueSliderChanged(int b, int colour, String key) {
        if (b > 255) { b = 255; } else if (b < 0) { b = 0; }
        int r = (colour >> 16) & 0xFF;
        int g = (colour >> 8) & 0xFF;
        colour = b | (colour & 0xFFFFFF00);
        m_prefs.edit().putInt(key, colour).commit();
        if (!recursive) {
            recursive = true;
            blueValue.setText(String.valueOf(b));
            rgbChanged(r, g, b); // set hue, saturation, and value
            recursive = false;
        }
        return colour;
    }

    protected abstract void setCurrentView(int viewnum);

    @Override
    public void onBackPressed() {
        if (m_currentView == CONFIGURE) {
            super.onBackPressed();
        } else {
            setCurrentView(CONFIGURE);
        }
    }
}
