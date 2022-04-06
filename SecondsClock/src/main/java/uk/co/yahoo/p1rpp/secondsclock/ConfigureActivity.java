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
    protected static final int CONFIGURE = 0; // m_currentView default value

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
    protected  void fixTintList(@NonNull Slider slider, int value) {
        ColorStateList cl = ColorStateList.valueOf(
            value < 128 ? 0xFFFFFFFF : 0xFF000000);
        slider.setTrackTintList(cl);
        slider.setThumbTintList(cl);
    }

    public abstract void onValueChanged(Slider slider, int value);

    public void onClick(View v) {
        if (v.getId() == DONEBUTTON) {
            setCurrentView(CONFIGURE);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) { m_currentView = CONFIGURE; }
        setContentView(R.layout.generic_layout);
    }

    protected LinearLayout makeChooser() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(centredLabel(R.string.huelabel, HUESLIDER));
        LinearLayout lhue = new LinearLayout(this);
        LinearLayout lshue = new LinearLayout(this);
        lshue.setOrientation(LinearLayout.VERTICAL);
        lshue.setLayoutParams(lpMMWeight);
        lshue.setGravity(Gravity.CENTER_VERTICAL);
        lshue.addView(hueSlider);
        lhue.addView(lshue);
        LinearLayout lvhue = new LinearLayout(this);
        lvhue.setLayoutParams(lpWrapWrap);
        lvhue.addView(hueSpacer);
        lhue.addView(lvhue);
        ll.addView(lhue);
        ll.addView(centredLabel(R.string.saturationlabel, SATURATIONSLIDER));
        LinearLayout lsat = new LinearLayout(this);
        LinearLayout lssat = new LinearLayout(this);
        lssat.setOrientation(LinearLayout.VERTICAL);
        lssat.setLayoutParams(lpMMWeight);
        lssat.setGravity(Gravity.CENTER_VERTICAL);
        lssat.addView(saturationSlider);
        lsat.addView(lssat);
        LinearLayout lvsat = new LinearLayout(this);
        lvsat.setLayoutParams(lpWrapWrap);
        lvsat.addView(saturationValue);
        lsat.addView(lvsat);
        ll.addView(lsat);
        ll.addView(centredLabel(R.string.valuelabel, VALUESLIDER));
        LinearLayout lval = new LinearLayout(this);
        LinearLayout lsval = new LinearLayout(this);
        lsval.setOrientation(LinearLayout.VERTICAL);
        lsval.setLayoutParams(lpMMWeight);
        lsval.setGravity(Gravity.CENTER_VERTICAL);
        lsval.addView(valueSlider);
        lval.addView(lsval);
        LinearLayout lvval = new LinearLayout(this);
        lvval.setLayoutParams(lpWrapWrap);
        lvval.addView(valueValue);
        lval.addView(lvval);
        ll.addView(lval);
        ll.addView(centredLabel(R.string.redlabel, REDSLIDER));
        LinearLayout lred = new LinearLayout(this);
        LinearLayout lsred = new LinearLayout(this);
        lsred.setOrientation(LinearLayout.VERTICAL);
        lsred.setLayoutParams(lpMMWeight);
        lsred.setGravity(Gravity.CENTER_VERTICAL);
        lsred.addView(redSlider);
        lred.addView(lsred);
        LinearLayout lvred = new LinearLayout(this);
        lvred.setLayoutParams(lpWrapWrap);
        lvred.addView(redValue);
        lred.addView(lvred);
        ll.addView(lred);
        ll.addView(centredLabel(R.string.greenlabel, GREENSLIDER));
        LinearLayout lgrn = new LinearLayout(this);
        LinearLayout lsgrn = new LinearLayout(this);
        lsgrn.setOrientation(LinearLayout.VERTICAL);
        lsgrn.setLayoutParams(lpMMWeight);
        lsgrn.setGravity(Gravity.CENTER_VERTICAL);
        lsgrn.addView(greenSlider);
        lgrn.addView(lsgrn);
        LinearLayout lvgrn = new LinearLayout(this);
        lvgrn.setLayoutParams(lpWrapWrap);
        lvgrn.addView(greenValue);
        lgrn.addView(lvgrn);
        ll.addView(lgrn);
        ll.addView(centredLabel(R.string.bluelabel, BLUESLIDER));
        LinearLayout lblu = new LinearLayout(this);
        LinearLayout lsblu = new LinearLayout(this);
        lsblu.setOrientation(LinearLayout.VERTICAL);
        lsblu.setLayoutParams(lpMMWeight);
        lsblu.setGravity(Gravity.CENTER_VERTICAL);
        lsblu.addView(blueSlider);
        lblu.addView(lsblu);
        LinearLayout lvblu = new LinearLayout(this);
        lvblu.setLayoutParams(lpWrapWrap);
        lvblu.addView(blueValue);
        lblu.addView(lvblu);
        ll.addView(lblu);
        return ll;
    }

    int safeParseInt(String s) {
        if ((s == null) || s.isEmpty()) { return 0; }
        return Integer.parseInt(s);
    }

    @Override
    protected void resume() {
        super.resume();
        m_helptext = new TextView(this);
        m_helptext.setId(LONGPRESSHELP);
        m_helptext.setText(R.string.longpresslabel);
        m_helptext.setOnLongClickListener(this);
        showShortWeekDayCheckBox = new CheckBox(this);
        showShortWeekDayCheckBox.setId(SHORTWEEKDAY);
        showShortWeekDayCheckBox.setChecked(showWeekDay == 1);
        showShortWeekDayCheckBox.setText(getString(R.string.show_short_weekday, m_CorW));
        showShortWeekDayCheckBox.setOnCheckedChangeListener(this);
        showShortWeekDayCheckBox.setOnLongClickListener(this);
        showLongWeekDayCheckBox = new CheckBox(this);
        showLongWeekDayCheckBox.setId(LONGWEEKDAY);
        showLongWeekDayCheckBox.setText(getString(R.string.show_long_weekday, m_CorW));
        showLongWeekDayCheckBox.setChecked(showWeekDay == 2);
        showLongWeekDayCheckBox.setOnCheckedChangeListener(this);
        showLongWeekDayCheckBox.setOnLongClickListener(this);
        showShortDateCheckBox = new CheckBox(this);
        showShortDateCheckBox.setId(SHORTDATE);
        showShortDateCheckBox.setChecked(showShortDate != 0);
        showShortDateCheckBox.setOnCheckedChangeListener(this);
        showShortDateCheckBox.setText(getString(R.string.show_short_date, m_CorW));
        showShortDateCheckBox.setOnLongClickListener(this);
        showMonthDayCheckBox = new CheckBox(this);
        showMonthDayCheckBox.setId(MONTHDAY);
        showMonthDayCheckBox.setChecked(showMonthDay != 0);
        showMonthDayCheckBox.setOnCheckedChangeListener(this);
        showMonthDayCheckBox.setText(getString(R.string.show_month_day, m_CorW));
        showMonthDayCheckBox.setOnLongClickListener(this);
        showShortMonthCheckBox = new CheckBox(this);
        showShortMonthCheckBox.setId(SHORTMONTH);
        showShortMonthCheckBox.setChecked(showMonth == 1);
        showShortMonthCheckBox.setOnCheckedChangeListener(this);
        showShortMonthCheckBox.setText(getString(R.string.show_short_month, m_CorW));
        showShortMonthCheckBox.setOnLongClickListener(this);
        showLongMonthCheckBox = new CheckBox(this);
        showLongMonthCheckBox.setId(LONGMONTH);
        showLongMonthCheckBox.setChecked(showMonth == 2);
        showLongMonthCheckBox.setOnCheckedChangeListener(this);
        showLongMonthCheckBox.setText(getString(R.string.show_long_month, m_CorW));
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
        m_numberWidth = (int)(saturationValue.getPaint().measureText("000") * 1.3);
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

    // set the value fields for saturation and value
    protected void setSatVal(int sat, int value) {
        fixTintList(saturationSlider, value);
        fixTintList(valueSlider, value);
        if (!recursive) {
            recursive = true;
            saturationValue.setText(String.valueOf(sat));
            valueValue.setText(String.valueOf(value));
            recursive = false;
        }
    }

    // set the sliders and value fields for red, green, and blue
    protected int setColour(int r, int g, int b) {
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

    // called to update red, green, and blue if hue, saturation or value has changed
    protected int hsvChanged() {
        int r;
        int g;
        int b;
        int hue = hueSlider.getValue();
        if (hue < 256) {
            r = 255;
            g = hue;
            b = 0;
        } else if (hue < 511) {
            r = 510 - hue;
            g = 255;
            b = 0;
        } else if (hue < 766) {
            r = 0;
            g = 255;
            b = hue - 510;
        } else if (hue < 1021) {
            r = 0;
            g = 1020 - hue;
            b = 255;
        } else if (hue < 1275) {
            r = hue - 1020;
            g = 0;
            b = 255;
        } else {
            r = 255;
            g = 0;
            b = 1530 - hue;
        }
        int saturation = saturationSlider.getValue();
        int msat = 255 - saturation;
        r = (r * saturation) / 255 + msat;
        g = (g * saturation) / 255 + msat;
        b = (b * saturation) / 255 + msat;
        int value = valueSlider.getValue();
        r = r * value / 255;
        g = g * value / 255;
        b = b * value / 255;
        setSatVal(saturation, value); // set the value fields
        return setColour(r, g, b); // set sliders and value fields
    }

    // called to update hue, saturation, and value if red, green, or blue has changed
    protected void rgbChanged(int r, int g, int b) {
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
        hueSlider.setValue(hue);
        int colour = (((r << 8) + g) << 8) + b;
        int value = r;
        if (value < g) { value = g; }
        if (value < b) { value = b; }
        valueSlider.setValue(value);
        valueSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {
                0xFF000000, colour | 0xFF000000}));
        int saturation;
        if (value == 0) { saturation = 0; }
        else {
            int least = r;
            if (least > g) { least = g; }
            if (least > b) { least = b; }
            saturation = (255 * least) / value;
        }
        saturationSlider.setValue(saturation);
        saturationSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {
                (value * 0x010101) | 0xFF000000, colour | 0xFF000000}));
        setSatVal(saturation, value);
    }

    // Called to set colour from preferences
    protected void rgbChanged(int colour) {
        int r = (colour >> 16) & 0xFF;
        int g = (colour >> 8) & 0xFF;
        int b = colour & 0xFF;
        setColour(r, g, b); // set red, green, and blue sliders and values
        rgbChanged(r, g, b); // set hue, saturation, and value
    }

    // Called when red, green, or blue has changed, slider and value already updated
    protected void rgbChanged() {
        int r = redSlider.getValue();
        int g = greenSlider.getValue();
        int b = blueSlider.getValue();
        rgbChanged(r, g, b);
    }

    @SuppressLint("ApplySharedPref")
    protected int redSliderChanged(int r, int colour, String key) {
        if (!recursive) {
            recursive = true;
            redValue.setText(String.valueOf(r));
            recursive = false;
        }
        colour = (r << 16) + (colour & 0xFF00FFFF);
        int g = (colour >> 8) & 0xFF;
        int b = colour & 0xFF;
        m_prefs.edit().putInt(key, colour).commit();
        rgbChanged(r, g, b); // set hue, saturation, and value
        return colour;
    }

    @SuppressLint("ApplySharedPref")
    protected int greenSliderChanged(int g, int colour, String key) {
        if (!recursive) {
            recursive = true;
            greenValue.setText(String.valueOf(g));
            recursive = false;
        }
        int r = (colour >> 16) & 0xFF;
        colour = (g << 8) + (colour & 0xFFFF00FF);
        int b = colour & 0xFF;
        m_prefs.edit().putInt(key, colour).commit();
        rgbChanged(r, g, b); // set hue, saturation, and value
        return colour;
    }

    @SuppressLint("ApplySharedPref")
    protected int blueSliderChanged(int b, int colour, String key) {
        if (!recursive) {
            recursive = true;
            blueValue.setText(String.valueOf(b));
            recursive = false;
        }
        int r = (colour >> 16) & 0xFF;
        int g = (colour >> 8) & 0xFF;
        colour = b + (colour & 0xFFFFFF00);
        m_prefs.edit().putInt(key, colour).commit();
        rgbChanged(r, g, b); // set hue, saturation, and value
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
