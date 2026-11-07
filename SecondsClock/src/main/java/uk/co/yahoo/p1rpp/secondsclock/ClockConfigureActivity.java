/*
 * Copyright © 2024. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This class handles the settings for the full screen clock.
 * It also shows a half-size demonstration of what it will look like.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import static android.text.InputType.TYPE_CLASS_NUMBER;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/*************************************************************
 * Revised dimming logic
 * Settable silders:-
 * Minimum brightness
 * Minimum Opacity
 * Lux threshold bvelow which to dim
 *
 * Display while setting dim parameters:-
 * Lux level
 * Computed brightness
 * Computed opacity
 *
 * Apply opacity to demo widget, but not to controls
 * Apply brightness to demo widget and controls
 *
 */

public class ClockConfigureActivity extends ConfigureActivity
    implements View.OnClickListener
{
    private static final int DEMOCLOCK = SETTEXTCOLOUR + 1;
    private static final int SSLABEL = DEMOCLOCK + 1;
    private static final int NOSECONDS = SSLABEL + 1;
    private static final int SMALLSECONDS = NOSECONDS + 1;
    private static final int LARGESECONDS = SMALLSECONDS + 1;
    private static final int SECONDSSIZER = LARGESECONDS + 1;
    private static final int MINBRIGHTLABEL = SECONDSSIZER + 1;
    private static final int MINBRIGHTSLIDER = MINBRIGHTLABEL + 1;
    private static final int MINBRIGHTVALUE = MINBRIGHTSLIDER + 1;
    private static final int ALPHALABEL = MINBRIGHTVALUE + 1;
    private static final int THRESHOLDLABEL = ALPHALABEL + 1;
    private static final int THRESHOLDSLIDER = THRESHOLDLABEL + 1;
    private static final int THRESHOLDVALUE = THRESHOLDSLIDER + 1;
    private static final int SEVENSEGMENTS = THRESHOLDVALUE + 1;
    private static final int FORCEVERTICAL = SEVENSEGMENTS + 1;
    private static final int DATABUTTON = FORCEVERTICAL + 1;
    private static final int DISPLAYBUTTON = DATABUTTON + 1;
    private static final int DIMBUTTON = DISPLAYBUTTON + 1;
    private static final int CURRENTLUX = DIMBUTTON + 1;
    private static final int CURRENTBRIGHT = CURRENTLUX + 1;
    private static final int CURRENTALPHA = CURRENTBRIGHT + 1;
    private static final int ONLYOPACITY = CURRENTALPHA + 1;
    private static final int FORMATMODE = ONLYOPACITY + 1;
    private static final int FORMATPROFILE_SYNC = FORMATMODE + 1;
    private static final int FORMATPROFILE_PORTRAIT = FORMATPROFILE_SYNC + 1;
    private static final int FORMATPROFILE_LANDSCAPE = FORMATPROFILE_PORTRAIT + 1;
    private static final int FORMATFIELD = FORMATPROFILE_LANDSCAPE + 1;
    private static final int SHOW_NEXT_ALARM = FORMATFIELD + 1;
    private static final int TEXT_STYLE_EMOJI = SHOW_NEXT_ALARM + 1;

    // Conversion between threshold slider and numeric value
    private int m_fgcolour;
    private Slider m_secondsSizer;
    private Slider m_minBrightSlider;
    private EditText m_minBrightValue;
    private Slider m_alphaSlider;
    private EditText m_alphaValue;
    private Slider m_thresholdSlider;
    private EditText m_thresholdValue;
    private CheckBox m_onlyopacity;
    private ClockView m_clockView;
    private Button m_textColour;
    private Button m_data;
    private Button m_display;
    private TextView m_currentLux;
    private TextView m_currentBright;
    private TextView m_currentOpacity;
    private boolean m_advancedFormatMode;
    private boolean m_changingFormatInput;
    private int m_formatProfile = FormatProfiles.SYNC;
    private EditText m_formatInput;
    private TextView m_formatLabel;
    private Button m_formatModeButton;
    private Button m_syncFormatButton;
    private Button m_portraitFormatButton;
    private Button m_landscapeFormatButton;

    public void updateTexts(float lux, int bright, int alpha) {
        if (m_currentView == DISPLAYBUTTON) {
            m_currentLux.setText(getString(R.string.currentlux, (int) lux));
            if (bright < 0) {
                int settingsBrightness = Settings.System.getInt(
                        getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, 255);
                m_currentBright.setText(getString(R.string.systembright,
                        settingsBrightness));
            } else {
                m_currentBright.setText(getString(R.string.currentbright,
                        bright));
            }
            m_currentOpacity.setText(getString(R.string.currentopacity, alpha));
        }
    }

    private String clockFormatPreferenceForProfile(int profile) {
        if (profile == FormatProfiles.PORTRAIT) return "CformatPortrait";
        if (profile == FormatProfiles.LANDSCAPE) return "CformatLandscape";
        return "Cformat";
    }

    private String getClockFormatForProfile(int profile) {
        String sync = m_prefs.getString(
                "Cformat", Formatter.fromClockPreferences(this, m_prefs, "C"));
        if (profile == FormatProfiles.PORTRAIT) {
            return m_prefs.getString("CformatPortrait", sync);
        }
        if (profile == FormatProfiles.LANDSCAPE) {
            return m_prefs.getString("CformatLandscape", sync);
        }
        return sync;
    }

    private void initializeClockAdvancedFormat() {
        if (!m_prefs.contains("Cformat")) {
            m_prefs.edit().putString("Cformat",
                    Formatter.fromClockPreferences(this, m_prefs, "C")).apply();
        }
        if (!m_prefs.contains("CformatSyncEnabled")) {
            m_prefs.edit().putBoolean("CformatSyncEnabled", true).apply();
        }
    }

    private void selectClockFormatProfile(int profile) {
        m_formatProfile = profile;
        m_prefs.edit()
                .putInt("CformatProfile", profile)
                .putBoolean("CformatSyncEnabled", profile == FormatProfiles.SYNC)
                .apply();
        if (m_formatInput != null) {
            m_changingFormatInput = true;
            m_formatInput.setText(getClockFormatForProfile(profile));
            m_formatInput.setSelection(m_formatInput.length());
            m_changingFormatInput = false;
        }
        updateClockFormatLabel();
        m_clockView.updateLayout();
    }

    private void updateClockFormatLabel() {
        if (m_syncFormatButton != null) {
            FormatProfileSelection.apply(
                    m_syncFormatButton, m_formatProfile == FormatProfiles.SYNC);
            FormatProfileSelection.apply(
                    m_portraitFormatButton,
                    m_formatProfile == FormatProfiles.PORTRAIT);
            FormatProfileSelection.apply(
                    m_landscapeFormatButton,
                    m_formatProfile == FormatProfiles.LANDSCAPE);
        }
        if (m_formatLabel == null) return;
        int profileLabel = m_formatProfile == FormatProfiles.PORTRAIT
                ? R.string.format_profile_portrait
                : m_formatProfile == FormatProfiles.LANDSCAPE
                        ? R.string.format_profile_landscape
                        : R.string.format_profile_sync;
        m_formatLabel.setText(getString(R.string.clock_format_label)
                + " (" + getString(profileLabel) + ")");
    }

    private Button makeClockProfileButton(int id, int label) {
        Button button = new Button(this);
        button.setId(id);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(this);
        return button;
    }

    private LinearLayout makeClockFormatProfiles() {
        LinearLayout profiles = new LinearLayout(this);
        profiles.setOrientation(LinearLayout.HORIZONTAL);
        m_syncFormatButton = makeClockProfileButton(
                FORMATPROFILE_SYNC, R.string.format_profile_sync);
        m_portraitFormatButton = makeClockProfileButton(
                FORMATPROFILE_PORTRAIT, R.string.format_profile_portrait);
        m_landscapeFormatButton = makeClockProfileButton(
                FORMATPROFILE_LANDSCAPE, R.string.format_profile_landscape);
        LinearLayout orientationProfiles = new LinearLayout(this);
        orientationProfiles.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams portraitParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0F);
        portraitParams.rightMargin = -(int)(4 * m_density);
        orientationProfiles.addView(m_portraitFormatButton, portraitParams);
        orientationProfiles.addView(m_landscapeFormatButton,
                new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0F));
        LinearLayout.LayoutParams syncParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0F);
        syncParams.rightMargin = (int)(4 * m_density);
        profiles.addView(m_syncFormatButton, syncParams);
        profiles.addView(orientationProfiles, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 2.0F));
        updateClockFormatLabel();
        return profiles;
    }

    private LinearLayout makeClockFormatEditor() {
        LinearLayout editor = new LinearLayout(this);
        editor.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(this);
        m_formatLabel = label;
        updateClockFormatLabel();
        editor.addView(label, lpMatchWrap);
        TextView profileHint = new TextView(this);
        profileHint.setText(R.string.clock_format_profile_hint);
        editor.addView(profileHint, lpMatchWrap);
        editor.addView(makeClockFormatProfiles(), lpMatchWrap);
        TextView hint = new TextView(this);
        hint.setText(R.string.clock_format_input_hint);
        editor.addView(hint, lpMatchWrap);
        editor.addView(m_formatInput, lpMatchWrap);
        CheckBox textStyleEmoji = new CheckBox(this);
        textStyleEmoji.setId(TEXT_STYLE_EMOJI);
        textStyleEmoji.setText(R.string.text_style_emoji);
        textStyleEmoji.setChecked(m_prefs.getBoolean("CtextStyleEmoji", false));
        textStyleEmoji.setOnCheckedChangeListener(this);
        editor.addView(textStyleEmoji, lpWrapWrap);
        editor.addView(makeClockFormatGuide(), lpMatchWrap);
        return editor;
    }

    private LinearLayout makeClockFormatGuide() {
        LinearLayout guide = new LinearLayout(this);
        guide.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(6 * m_density);
        guide.setPadding(padding, padding, padding, padding);
        TextView title = new TextView(this);
        title.setText(R.string.clock_format_guide_title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        guide.addView(title, lpMatchWrap);
        TextView instructions = new TextView(this);
        instructions.setText(R.string.clock_format_guide_hint);
        guide.addView(instructions, lpMatchWrap);
        addClockTokenSection(guide, R.string.widget_format_time_section);
        addClockTokenRow(guide,
                R.string.format_token_hour24, "%H",
                R.string.format_token_hour12, "%I");
        addClockTokenRow(guide,
                R.string.format_token_minute, "%M",
                R.string.format_token_second, "%S");
        addClockTokenRow(guide,
                R.string.format_token_ampm, "%p",
                R.string.format_token_local_time, "%X");
        addClockTokenSection(guide, R.string.widget_format_date_section);
        addClockTokenRow(guide,
                R.string.format_token_short_weekday, "%a",
                R.string.format_token_full_weekday, "%A");
        addClockTokenRow(guide,
                R.string.format_token_short_month, "%b",
                R.string.format_token_full_month, "%B");
        addClockTokenRow(guide,
                R.string.format_token_day, "%d",
                R.string.format_token_day_unpadded, "%e");
        addClockTokenRow(guide,
                R.string.format_token_month, "%m",
                R.string.format_token_year, "%Y");
        addClockTokenRow(guide,
                R.string.format_token_iso_date, "%F",
                R.string.format_token_local_date, "%x");
        addClockTokenRow(guide,
                R.string.format_token_newline, "%n",
                R.string.format_token_percent, "%%");
        addClockTokenSection(guide, R.string.widget_format_other_section);
        addClockTokenRow(guide, R.string.format_token_large_line, "%L", 2);
        TextView landscapeHint = new TextView(this);
        landscapeHint.setText(R.string.clock_format_landscape_emphasis_hint);
        guide.addView(landscapeHint, lpMatchWrap);
        addClockTokenSection(guide, R.string.widget_format_alarm_section);
        addClockTokenRow(guide, R.string.format_token_alarm, "%<%>", 2);
        addClockTokenRow(guide, R.string.format_token_alarm_alternative, "%|", 2);
        TextView alarmHint = new TextView(this);
        alarmHint.setText(R.string.widget_format_alarm_hint);
        guide.addView(alarmHint, lpMatchWrap);
        return guide;
    }

    private void addClockTokenSection(LinearLayout guide, int labelResource) {
        TextView label = new TextView(this);
        label.setText(labelResource);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setPadding(0, (int)(8 * m_density), 0, 0);
        guide.addView(label, lpMatchWrap);
    }

    private void addClockTokenRow(
            LinearLayout guide, int firstLabel, String firstToken,
            int secondLabel, String secondToken) {
        addClockTokenRow(guide, firstLabel, firstToken,
                secondLabel, secondToken, firstToken.length());
    }

    private void addClockTokenRow(
            LinearLayout guide, int firstLabel, String firstToken,
            int firstCursorOffset) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(makeClockTokenButton(firstLabel, firstToken, firstCursorOffset),
                new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0F));
        guide.addView(row, lpMatchWrap);
    }

    private void addClockTokenRow(
            LinearLayout guide, int firstLabel, String firstToken,
            int secondLabel, String secondToken, int firstCursorOffset) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(makeClockTokenButton(
                firstLabel, firstToken, firstCursorOffset),
                new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0F));
        if (secondToken != null) {
            row.addView(makeClockTokenButton(
                    secondLabel, secondToken, secondToken.length()),
                    new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0F));
        }
        guide.addView(row, lpMatchWrap);
    }

    private Button makeClockTokenButton(int label, String token, int cursorOffset) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        button.setOnClickListener(v -> {
            Editable text = m_formatInput.getText();
            int start = m_formatInput.getSelectionStart();
            int end = m_formatInput.getSelectionEnd();
            if (start < 0 || end < 0) start = end = text.length();
            int replaceStart = Math.min(start, end);
            int replaceEnd = Math.max(start, end);
            text.replace(replaceStart, replaceEnd, token);
            m_formatInput.requestFocus();
            m_formatInput.setSelection(replaceStart + cursorOffset);
        });
        return button;
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case LONGPRESSHELP: doToast(R.string.clockconfighelp); return true;
            case DEMOCLOCK:
                doToast(getString(R.string.clockdemo,
                    (m_orientation == Configuration.ORIENTATION_LANDSCAPE)
                        ? getString(R.string.landscape) : getString(R.string.portrait)));
                return true;
            case SSLABEL:
            case SECONDSSIZER:doToast(
                (m_orientation == Configuration.ORIENTATION_LANDSCAPE)
                    ? getString(R.string.sshelpland) : getString(R.string.sshelpport));
                return true;
            case NOSECONDS: doToast(R.string.nosecondshelp); return true;
            case SMALLSECONDS: doToast(R.string.smallsecondshelp); return true;
            case LARGESECONDS: doToast(R.string.largesecondshelp); return true;
            case MINBRIGHTLABEL:
            case MINBRIGHTSLIDER: doToast(R.string.minbrightsliderhelp); return true;
            case MINBRIGHTVALUE: doToast(R.string.minbrightvaluehelp); return true;
            case ALPHALABEL:
            case ALPHASLIDER: doToast(R.string.minalphasliderhelp); return true;
            case ALPHAVALUE: doToast(R.string.minalphavaluehelp); return true;
            case THRESHOLDLABEL:
            case THRESHOLDSLIDER: doToast(R.string.thresholdsliderhelp); return true;
            case THRESHOLDVALUE: doToast(R.string.thresholdvaluehelp); return true;
            case SEVENSEGMENTS: doToast(R.string.sevensegmentshelp); return true;
            case FORCEVERTICAL: doToast(R.string.forceverticalhelp); return true;
            case SETTEXTCOLOUR: doToast(R.string.setclockcolourhelp); return true;
            case DATABUTTON: doToast(R.string.setdatahelp); return true;
            case FORMATMODE: doToast(R.string.clock_format_caption); return true;
            case SHOW_NEXT_ALARM: doToast(R.string.next_alarm_source_help); return true;
            case DISPLAYBUTTON: doToast(R.string.setdisplayhelp); return true;
            case DIMBUTTON: doToast(R.string.setdimminghelp); return true;
            case CURRENTLUX: doToast(R.string.luxhelp); return true;
            case CURRENTBRIGHT:
                Window w = getWindow();
                WindowManager.LayoutParams lp = w.getAttributes();
                if (lp.screenBrightness >= 0) {
                    doToast(R.string.brighthelp);
                } else {
                    doToast(R.string.systembrighthelp);
                }
                return true;
            case CURRENTALPHA: doToast(R.string.opacityhelp); return true;
            case ONLYOPACITY: doToast(R.string.onlyopacityhelp); return true;
        }
        return super.onLongClick(v);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onValueChanged(Slider slider, int value) {
        switch (slider.getId()) {
            case HUESLIDER:
                m_fgcolour = 0xFF000000 | hueChanged();
                m_prefs.edit().putInt("Cfgcolour", m_fgcolour).commit();
                m_clockView.adjustColour();
                break;
            case SATURATIONSLIDER:
                m_fgcolour = 0xFF000000 | saturationChanged();
                m_prefs.edit().putInt("Cfgcolour", m_fgcolour).commit();
                m_clockView.adjustColour();
                break;
            case VALUESLIDER:
                m_fgcolour = 0xFF000000 | valueChanged();
                m_prefs.edit().putInt("Cfgcolour", m_fgcolour).commit();
                m_clockView.adjustColour();
                break;
            case REDSLIDER:
                m_fgcolour = redSliderChanged(value, m_fgcolour, "Cfgcolour");
                m_clockView.adjustColour();
                break;
            case GREENSLIDER:
                m_fgcolour = greenSliderChanged(
                    value, m_fgcolour, "Cfgcolour");
                m_clockView.adjustColour();
                break;
            case BLUESLIDER:
                m_fgcolour = blueSliderChanged(value, m_fgcolour, "Cfgcolour");
                m_clockView.adjustColour();
                break;
            case SECONDSSIZER:
                m_prefs.edit().putInt("CsecondsSize", value).commit();
                m_clockView.updateLayout();
                break;
            case MINBRIGHTSLIDER:
                if (!recursive) {
                    recursive = true;
                    m_minBrightValue.setText(String.valueOf(value));
                    recursive = false;
                }
                fixTintList(m_minBrightSlider, (value * 255) / 100);
                m_prefs.edit().putInt("CminBright", value).commit();
                m_clockView.adjustColour();
                break;
            case ALPHASLIDER:
                if (!recursive) {
                    recursive = true;
                    m_alphaValue.setText(String.valueOf(value));
                    recursive = false;
                }
                fixTintList(m_alphaSlider, value);
                m_prefs.edit().putInt("Calpha", value).commit();
                m_clockView.adjustColour();
                break;
            case THRESHOLDSLIDER:
                if (!recursive) {
                    recursive = true;
                    m_thresholdValue.setText(String.valueOf(value));
                    recursive = false;
                }
                fixTintList(m_thresholdSlider, value);
                m_prefs.edit().putInt("Cthreshold", value).commit();
                m_clockView.adjustColour();
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case SETTEXTCOLOUR: setCurrentView(SETTEXTCOLOUR); break;
            case DATABUTTON: setCurrentView(DATABUTTON); break;
            case FORMATMODE:
                m_advancedFormatMode = !m_advancedFormatMode;
                if (m_advancedFormatMode) initializeClockAdvancedFormat();
                m_prefs.edit().putBoolean(
                        "CadvancedFormatMode", m_advancedFormatMode).apply();
                m_clockView.updateLayout();
                setCurrentView(DATABUTTON);
                break;
            case FORMATPROFILE_SYNC:
                selectClockFormatProfile(FormatProfiles.SYNC);
                break;
            case FORMATPROFILE_PORTRAIT:
                selectClockFormatProfile(FormatProfiles.PORTRAIT);
                break;
            case FORMATPROFILE_LANDSCAPE:
                selectClockFormatProfile(FormatProfiles.LANDSCAPE);
                break;
            case DIMBUTTON:
            case DISPLAYBUTTON: setCurrentView(DISPLAYBUTTON); break;
            case DEMOCLOCK:
                Intent intent = new Intent(m_activity, ClockActivity.class);
                startActivity(intent);
                break;
            case DONEBUTTON:
                if (m_currentView == DATABUTTON) {
                    finish();
                } else {
                    setCurrentView(DATABUTTON);
                }
        }
    }

    protected void updateFromCheckBox() {
        m_clockView.updateLayout();
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
           case SEVENSEGMENTS:
                m_prefs.edit().putBoolean("C7seg", isChecked).commit();
                m_clockView.updateLayout();
                break;
            case FORCEVERTICAL:
                m_prefs.edit().putBoolean("Cforcevertical", isChecked).commit();
                m_clockView.updateLayout();
                break;
            case SHOW_NEXT_ALARM:
                m_prefs.edit().putBoolean("CshowNextAlarm", isChecked).commit();
                m_clockView.updateLayout();
                break;
            case TEXT_STYLE_EMOJI:
                m_prefs.edit().putBoolean("CtextStyleEmoji", isChecked).apply();
                m_clockView.updateLayout();
                break;
            default: super.onCheckedChanged(buttonView, isChecked);
        }
    }

    private GridLayout doCLockView() {
        GridLayout gl = new GridLayout(this);
        gl.setBackgroundColor(m_background);
        // clockview is in top left for both layouts
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
            GridLayout.spec(0, 1),
            GridLayout.spec(0, 1)
        );
        layoutParams.width = m_width / 2;
        layoutParams.height = m_height / 2;
        gl.addView(m_clockView, -1, layoutParams);
        return gl;
    }

    @SuppressLint({"SetTextI18n"})
    private LinearLayout doSmallSeconds() {
        // common code for small seconds label
        LinearLayout lSmallSeconds = new LinearLayout(this);
        // default is HORIZONTAL
        lSmallSeconds.setOnLongClickListener(this);
        lSmallSeconds.setId(SMALLSECONDS);
        LinearLayout.LayoutParams l3pars = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        l3pars.gravity = Gravity.CENTER_VERTICAL;
        TextView tv = new TextView(this);
        tv.setText("00:00");
        lSmallSeconds.addView(tv, l3pars);
        tv = new TextView(this);
        tv.setText("00");
        tv.setScaleX(0.6F);
        tv.setScaleY(0.6F);
        lSmallSeconds.addView(tv, l3pars);
        return lSmallSeconds;
    }

    @SuppressLint({"RtlHardcoded", "SetTextI18n"})
    private void doDataLayout() {
        GridLayout gl = doCLockView();
        ScrollView scrollView = new ScrollView(this);
        scrollView.setScrollbarFadingEnabled(false);
        scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        LinearLayout lboxes = new LinearLayout(this);
        lboxes.setOrientation(LinearLayout.VERTICAL);
        m_formatModeButton.setText(m_advancedFormatMode
                ? R.string.checkbox_mode : R.string.advanced_format_mode);
        if (m_advancedFormatMode) {
            lboxes.addView(makeClockFormatEditor(), lpMatchWrap);
        } else {
            lboxes.addView(showLongWeekDayCheckBox);
            lboxes.addView(showShortWeekDayCheckBox);
            lboxes.addView(showShortDateCheckBox);
            lboxes.addView(showMonthDayCheckBox);
            lboxes.addView(showLongMonthCheckBox);
            lboxes.addView(showShortMonthCheckBox);
            lboxes.addView(showYearCheckBox);
            CheckBox showNextAlarm = new CheckBox(this);
            showNextAlarm.setId(SHOW_NEXT_ALARM);
            showNextAlarm.setText(R.string.show_next_alarm_any_app);
            showNextAlarm.setChecked(m_prefs.getBoolean("CshowNextAlarm", false));
            showNextAlarm.setOnLongClickListener(this);
            showNextAlarm.setOnCheckedChangeListener(this);
            lboxes.addView(showNextAlarm);
        }
        CheckBox cb = new CheckBox(this);
        cb.setId(SEVENSEGMENTS);
        cb.setText(R.string.sevensegments);
        cb.setChecked(m_prefs.getBoolean("C7seg", false));
        cb.setOnLongClickListener(this);
        cb.setOnCheckedChangeListener(this);
        lboxes.addView(cb);
        LinearLayout lControls = new LinearLayout(this);
        lControls.setOrientation(LinearLayout.VERTICAL);
        m_helptext.setText(R.string.longpresshoriz);
        lControls.addView(m_helptext);
        LinearLayout lButtons = new LinearLayout(this);
        lButtons.setOrientation(LinearLayout.VERTICAL);
        lButtons.setGravity(Gravity.CENTER_HORIZONTAL);
        lButtons.addView(m_textColour, lpWrapWrap);
        lButtons.addView(m_display, lpWrapWrap);
        lButtons.addView(m_formatModeButton, lpWrapWrap);
        lButtons.addView(m_okButton, lpWrapWrap);
        if (m_orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Buttons in bottom left
            m_display.setId(DISPLAYBUTTON);
            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(1, 1),
                GridLayout.spec(0, 1)
            );
            layoutParams.width = m_width / 2;
            gl.addView(lButtons, -1, layoutParams);
            lControls.addView(lboxes);
            scrollView.addView(lControls);
            // remaining controls in right half
            layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(0, 2, 1f),
                GridLayout.spec(1, 1, 1f)
            );
            layoutParams.width = 0;
            layoutParams.height = 0;
            gl.addView(scrollView, -1, layoutParams);
        } else { // assume PORTRAIT
             // remaining controls in bottom half
            if (!m_advancedFormatMode) {
                cb = new CheckBox(this);
                cb.setId(FORCEVERTICAL);
                cb.setText(R.string.forcevertical);
                cb.setChecked(m_prefs.getBoolean("Cforcevertical", false));
                cb.setOnLongClickListener(this);
                cb.setOnCheckedChangeListener(this);
                lboxes.addView(cb);
            }
            lControls.addView(lboxes);
            m_display.setId(DIMBUTTON);
            scrollView.addView(lControls);
            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(1, 1, 1f),
                GridLayout.spec(0, 2, 1f)
            );
            layoutParams.width = 0;
            layoutParams.height = 0;
            gl.addView(scrollView, -1, layoutParams);
            layoutParams = new GridLayout.LayoutParams(
                    GridLayout.spec(0, 1, 1f),
                    GridLayout.spec(1, 1, 1F)
            );
            layoutParams.width = m_width / 2;
            layoutParams.height = 0;
            gl.addView(lButtons, -1, layoutParams);
        }
        m_topLayout.addView(gl);
    }

    private void doDisplayLayout() {
        GridLayout gl = doCLockView();
        ScrollView scrollView = new ScrollView(this);
        scrollView.setScrollbarFadingEnabled(false);
        scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        LinearLayout lControls = new LinearLayout(this);
        lControls.setOrientation(LinearLayout.VERTICAL);
        m_helptext.setText(R.string.longpresshoriz);
        lControls.addView(m_helptext);
        RelativeLayout lssb = new RelativeLayout(this);
        if (m_advancedFormatMode) {
            TextView secondsHint = new TextView(this);
            secondsHint.setText(R.string.clock_format_seconds_help);
            lssb.addView(secondsHint);
        } else {
            RelativeLayout.LayoutParams rlp1 =
                    new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            rlp1.addRule(RelativeLayout.CENTER_HORIZONTAL);
            lssb.addView(textLabel(R.string.secondssizelabel, SSLABEL), rlp1);
            RelativeLayout.LayoutParams rlp2 =
                    new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp2.addRule(RelativeLayout.BELOW, SSLABEL);
            rlp2.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lssb.addView(textLabel("00:00", NOSECONDS), rlp2);
            RelativeLayout.LayoutParams rlp3 =
                    new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp3.addRule(RelativeLayout.BELOW, SSLABEL);
            rlp3.addRule(RelativeLayout.CENTER_HORIZONTAL);
            lssb.addView(doSmallSeconds(), rlp3);
            RelativeLayout.LayoutParams rlp4 =
                    new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp4.addRule(RelativeLayout.BELOW, SSLABEL);
            rlp4.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            lssb.addView(textLabel("00:00:00", LARGESECONDS), rlp4);
            RelativeLayout.LayoutParams rlp5 =
                    new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
            rlp5.addRule(RelativeLayout.BELOW, LARGESECONDS);
            rlp5.addRule(RelativeLayout.BELOW, SMALLSECONDS);
            rlp5.addRule(RelativeLayout.BELOW, NOSECONDS);
            m_secondsSizer.setDirection(Slider.SLIDER_RIGHTWARDS);
            lssb.addView(m_secondsSizer, rlp5);
        }
        lControls.addView(lssb);
        LinearLayout lButtons = new LinearLayout(this);
        lButtons.setOrientation(LinearLayout.VERTICAL);
        lButtons.setGravity(Gravity.CENTER_HORIZONTAL);
        lButtons.addView(m_textColour, lpWrapWrap);
        lButtons.addView(m_data, lpWrapWrap);
        lButtons.addView(m_okButton, lpWrapWrap);
        LinearLayout lDimming = new LinearLayout(this);
        lDimming.setOrientation(LinearLayout.VERTICAL);
        lDimming.addView(centredLabel(R.string.minbrightlabel, MINBRIGHTLABEL));
        LinearLayout lBright4 = new LinearLayout(this);
        // default orientation is HORIZONTAL
        LinearLayout lBright5 = new LinearLayout(this);
        lBright5.setOrientation(LinearLayout.VERTICAL);
        lBright5.setLayoutParams(lpMMWeight);
        lBright5.setGravity(Gravity.CENTER_VERTICAL); //FIXME may not need this
        int fgColour = m_prefs.getInt("Cfgcolour", 0xFFFFFFFF);
        m_minBrightSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {0xFF000000,  0xFF000000 | (fgColour & 0xFFFFFF)}));
        lBright5.addView(m_minBrightSlider);
        lBright4.addView(lBright5);
        LinearLayout lBright6 = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lBright6.setLayoutParams(lpWrapWrap);
        lBright6.addView(m_minBrightValue);
        lBright4.addView(lBright6);
        lDimming.addView(lBright4);
        lDimming.addView(centredLabel(R.string.minalphalabel, ALPHALABEL));
        LinearLayout lAlpha1 = new LinearLayout(this);
        // default orientation is HORIZONTAL
        LinearLayout lAlpha2 = new LinearLayout(this);
        lAlpha2.setOrientation(LinearLayout.VERTICAL);
        lAlpha2.setLayoutParams(lpMMWeight);
        lAlpha2.setGravity(Gravity.CENTER_VERTICAL); //FIXME may not need this
        m_alphaSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {0xFF000000,  0xFF000000 | (fgColour & 0xFFFFFF)}));
        lAlpha2.addView(m_alphaSlider);
        lAlpha1.addView(lAlpha2);
        LinearLayout lAlpha3 = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lAlpha3.setLayoutParams(lpWrapWrap);
        lAlpha3.addView(m_alphaValue);
        lAlpha1.addView(lAlpha3);
        lDimming.addView(lAlpha1);
        lDimming.addView(centredLabel(R.string.thresholdlabel, THRESHOLDLABEL));
        LinearLayout lThreshold1 = new LinearLayout(this);
        // default orientation is HORIZONTAL
        LinearLayout lThreshold2 = new LinearLayout(this);
        lThreshold2.setOrientation(LinearLayout.VERTICAL);
        lThreshold2.setLayoutParams(lpMMWeight);
        lThreshold2.setGravity(Gravity.CENTER_VERTICAL); //FIXME may not need this
        lThreshold2.addView(m_thresholdSlider);
        lThreshold1.addView(lThreshold2);
        LinearLayout lThreshold3 = new LinearLayout(this);
        // default orientation is HORIZONTAL
        lThreshold3.setLayoutParams(lpWrapWrap);
        lThreshold3.addView(m_thresholdValue);
        lThreshold1.addView(lThreshold3);
        lDimming.addView(lThreshold1);
        lDimming.addView(m_onlyopacity);
        lDimming.addView(m_currentLux);
        lDimming.addView(m_currentBright);
        lDimming.addView(m_currentOpacity);
        lControls.addView(lDimming);
        scrollView.addView(lControls);

        if (m_orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Buttons in bottom left
            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(1, 1),
                GridLayout.spec(0, 1)
            );
            layoutParams.width = m_width / 2;
            layoutParams.height = m_height / 2;
            gl.addView(lButtons, -1, layoutParams);
            // Controls in right half
            layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(0, 2),
                GridLayout.spec(1, 1)
            );
            layoutParams.width = m_width / 2;
            layoutParams.height = m_height;
            gl.addView(scrollView, -1, layoutParams);
        } else { // PORTRAIT
            // Buttons at top right
            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(0, 1),
                GridLayout.spec(1, 1, 1F)
            );
            layoutParams.width = m_width / 2;
            layoutParams.height = m_height / 2;
            gl.addView(lButtons, -1, layoutParams);
            // Controls in bottom half
            layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(1, 1, 1f),
                GridLayout.spec(0, 2, 1f)
            );
            layoutParams.width = m_width;
            layoutParams.height = m_height / 2;
            gl.addView(scrollView, -1, layoutParams);
        }
        m_topLayout.addView(gl);
    }

    @SuppressLint("SetTextI18n")
    protected void doChooserLayout() {
        m_ColourType = "full screen clock";
        GridLayout gl = doCLockView();
        ScrollView scrollView = new ScrollView(this);
        scrollView.setScrollbarFadingEnabled(false);
        scrollView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        LinearLayout lButtons = new LinearLayout(this);
        lButtons.setOrientation(LinearLayout.VERTICAL);
        lButtons.setGravity(Gravity.CENTER_HORIZONTAL);
        if (m_orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LinearLayout lChooser = new LinearLayout(this);
            lChooser.setOrientation(LinearLayout.VERTICAL);
            m_helptext.setText(R.string.longpresshoriz);
            lChooser.addView(m_helptext);
            lChooser.addView(makeChooser());
            m_display.setId(DISPLAYBUTTON);
            lButtons.addView(m_data, lpWrapWrap);
            lButtons.addView(m_display, lpWrapWrap);
            lButtons.addView(m_okButton, lpWrapWrap);
            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(1, 1),
                GridLayout.spec(0, 1)
            );
            layoutParams.width = m_width / 2;
            layoutParams.height = m_height / 2;
            gl.addView(lButtons, -1, layoutParams);
            scrollView.addView(lChooser);
            layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(0, 2),
                GridLayout.spec(1, 1)
            );
            layoutParams.width = m_width / 2;
            layoutParams.height = m_height;
            gl.addView(scrollView, -1, layoutParams);
        } else { // assume PORTRAIT
            m_display.setId(DIMBUTTON);
            m_helptext.setText(R.string.longpressvert);
            lButtons.addView(m_helptext, lpWrapWrap);
            lButtons.addView(m_data, lpWrapWrap);
            lButtons.addView(m_display, lpWrapWrap);
            lButtons.addView(m_okButton, lpWrapWrap);
            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(0, 1),
                GridLayout.spec(1, 1)
            );
            layoutParams.width = m_width / 2;
            layoutParams.height = m_height / 2;
            gl.addView(lButtons, -1, layoutParams);
            layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(1, 1),
                GridLayout.spec(0, 2)
            );
            layoutParams.width = m_width;
            layoutParams.height = m_height / 2;
            scrollView.addView(makeChooser());
            gl.addView(scrollView, -1, layoutParams);
        }
        m_topLayout.addView(gl);
        m_clockView.setColour(m_fgcolour);
        rgbChanged(m_fgcolour);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_CorW = "clock";
        m_clockView = new ClockView(this);
        m_clockView.setId(DEMOCLOCK);
        m_clockView.setOnClickListener(this);
        m_clockView.setOnLongClickListener(this);
    }


    @SuppressLint("ApplySharedPref")
    @Override
    protected void setCurrentView(int viewnum) {
        if (viewnum == CONFIGURE) { m_currentView = DATABUTTON; }
        else { m_currentView = viewnum; }
        m_prefs.edit().putInt("Cview", m_currentView).commit();
        removeAllViews(m_topLayout);
        switch(m_currentView) {
            default:
            case DATABUTTON: doDataLayout(); break;
            case SETTEXTCOLOUR: doChooserLayout(); break;
            case DISPLAYBUTTON: doDisplayLayout(); break;
        }
        m_clockView.adjustColour();
    }

    @Override
    protected void resume() {
        m_key = "C";
        super.resume();
        m_advancedFormatMode = m_prefs.getBoolean("CadvancedFormatMode", false);
        m_formatProfile = m_prefs.getInt("CformatProfile", FormatProfiles.SYNC);
        if (m_formatProfile < FormatProfiles.SYNC
                || m_formatProfile > FormatProfiles.LANDSCAPE) {
            m_formatProfile = FormatProfiles.SYNC;
        }
        if (m_advancedFormatMode) initializeClockAdvancedFormat();
        m_formatInput = new EditText(this);
        m_formatInput.setId(FORMATFIELD);
        m_formatInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        m_formatInput.setSingleLine(false);
        m_formatInput.setMinLines(3);
        m_formatInput.setMaxLines(6);
        m_formatInput.setGravity(Gravity.TOP | Gravity.START);
        m_formatInput.setVerticalScrollBarEnabled(true);
        m_formatInput.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        m_formatInput.setHint(R.string.widget_format_hint);
        m_formatInput.setText(getClockFormatForProfile(m_formatProfile));
        m_formatInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (m_changingFormatInput) return;
                m_prefs.edit().putString(
                        clockFormatPreferenceForProfile(m_formatProfile),
                        s.toString()).apply();
                m_clockView.updateLayout();
            }
        });
        m_formatModeButton = new Button(this);
        m_formatModeButton.setId(FORMATMODE);
        m_formatModeButton.setAllCaps(false);
        m_formatModeButton.setOnClickListener(this);
        m_formatModeButton.setOnLongClickListener(this);
        /* This is in fact the height of the actual view, but the ClockView
         * can't read its own height before it has been laid out, and in PORTRAIT
         * orientation we need it before then to assign space to its children.
         */
        m_clockView.setHeight(m_height / 2);
        m_secondsSizer = new Slider(this);
        m_secondsSizer.setMax(255);
        m_secondsSizer.setId(SECONDSSIZER);
        m_secondsSizer.setOnChangeListener(this);
        m_secondsSizer.setOnLongClickListener(this);
        m_secondsSizer.setValue(m_prefs.getInt("CsecondsSize", 255));
        m_secondsSizer.setThumbTintList(
                ColorStateList.valueOf(m_foreground));
        m_secondsSizer.setTrackTintList(
                ColorStateList.valueOf(m_foreground));
        int value = m_prefs.getInt("CminBright", 255);
        m_minBrightSlider = new Slider(this);
        m_minBrightSlider.setMax(100);
        m_minBrightSlider.setId(MINBRIGHTSLIDER);
        fixTintList(m_minBrightSlider, value);
        m_minBrightSlider.setOnChangeListener(this);
        m_minBrightSlider.setOnLongClickListener(this);
        m_minBrightSlider.setValue(value);
        m_minBrightValue = new EditText(this);
        m_minBrightValue.setId(MINBRIGHTVALUE);
        m_minBrightValue.setOnLongClickListener(this);
        m_minBrightValue.setInputType(TYPE_CLASS_NUMBER);
        m_minBrightValue.setWidth(m_numberWidth);
        m_minBrightValue.setText(String.valueOf(value));
        m_minBrightValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            {}
            @SuppressLint({"ApplySharedPref", "SetTextI18n"})
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    int value = safeParseInt(s.toString());
                    if (value > 100) {
                        m_minBrightValue.setText("100");
                        return;
                    }
                    m_minBrightSlider.setValue(value);
                    fixTintList(m_minBrightSlider, (value * 255) / 100);
                    m_prefs.edit().putInt("CminBright", value).commit();
                    m_clockView.adjustColour();
                    m_minBrightValue.setSelection(
                        m_minBrightValue.getText().length());
                }
            }
        });
        value = m_prefs.getInt("Calpha", 255);
        m_alphaSlider = new Slider(this);
        m_alphaSlider.setMax(255);
        m_alphaSlider.setId(ALPHASLIDER);
        fixTintList(m_alphaSlider, value);
        m_alphaSlider.setOnChangeListener(this);
        m_alphaSlider.setOnLongClickListener(this);
        m_alphaSlider.setValue(value);
        m_alphaValue = new EditText(this);
        m_alphaValue.setId(ALPHAVALUE);
        m_alphaValue.setOnLongClickListener(this);
        m_alphaValue.setInputType(TYPE_CLASS_NUMBER);
        m_alphaValue.setWidth(m_numberWidth);
        m_alphaValue.setText(String.valueOf(value));
        m_alphaValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            {}
            @SuppressLint({"ApplySharedPref", "SetTextI18n"})
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    int value = safeParseInt(s.toString());
                    if (value > 255) {
                        m_alphaValue.setText("255");
                        return;
                    }
                    m_alphaSlider.setValue(value);
                    fixTintList(m_alphaSlider, value);
                    m_prefs.edit().putInt("Calpha", value).commit();
                    m_clockView.adjustColour();
                    m_alphaValue.setSelection(
                        m_alphaValue.getText().length());
                }
            }
        });
        value = m_prefs.getInt("Cthreshold", 255);
        m_thresholdSlider = new Slider(this);
        m_thresholdSlider.setMax(255);
        m_thresholdSlider.setId(THRESHOLDSLIDER);
        fixTintList(m_thresholdSlider, value);
        m_thresholdSlider.setOnChangeListener(this);
        m_thresholdSlider.setOnLongClickListener(this);
        m_thresholdSlider.setValue(value);
        m_thresholdSlider.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] { 0xFF000000,  0xFFFFFFFF }));
        m_thresholdValue = new EditText(this);
        m_thresholdValue.setId(THRESHOLDVALUE);
        m_thresholdValue.setOnLongClickListener(this);
        m_thresholdValue.setInputType(TYPE_CLASS_NUMBER);
        m_thresholdValue.setWidth(m_numberWidth);
        m_thresholdValue.setText(String.valueOf(value));
        m_thresholdValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            {}
            @SuppressLint({"ApplySharedPref", "SetTextI18n"})
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    int value = safeParseInt(s.toString());
                    if (value > 255) {
                        m_thresholdValue.setText("255");
                        return;
                    }
                    m_thresholdSlider.setValue(value);
                    fixTintList(m_thresholdSlider, value);
                    m_prefs.edit().putInt("Cthreshold", value).commit();
                    m_clockView.adjustColour();
                    m_thresholdValue.setSelection(
                        m_thresholdValue.getText().length());
                }
            }
        });
        value = m_prefs.getInt("Conlyalpha", 0);
        m_onlyopacity = new CheckBox(this);
        m_onlyopacity.setId(ONLYOPACITY);
        m_onlyopacity.setText(R.string.onlyopacitylabel);
        m_onlyopacity.setChecked(value != 0);
        m_onlyopacity.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @SuppressLint({"ApplySharedPref"})
                    @Override
                    public void onCheckedChanged(
                            CompoundButton buttonView, boolean isChecked) {
                        m_prefs.edit().putInt("Conlyalpha",
                                isChecked ? 1 : 0).commit();
                        m_clockView.adjustColour();
                    }
                });
        m_onlyopacity.setOnLongClickListener(this);
        m_currentLux = new TextView(this);
        m_currentLux.setId(CURRENTLUX);
        m_currentLux.setOnLongClickListener(this);
        m_currentBright = new TextView(this);
        m_currentBright.setId(CURRENTBRIGHT);
        m_currentBright.setOnLongClickListener(this);
        m_currentOpacity = new TextView(this);
        m_currentOpacity.setId(CURRENTALPHA);
        m_currentOpacity.setOnLongClickListener(this);
        saturationValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            {}
            @SuppressLint({"ApplySharedPref", "SetTextI18n"})
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    int colour = fixSaturation(s.toString());
                    m_fgcolour = colour | (m_fgcolour & 0xFF000000);
                    m_prefs.edit().putInt(m_key + "fgcolour", m_fgcolour).commit();
                    m_clockView.adjustColour();
                    saturationValue.setSelection(
                        saturationValue.getText().length());
                }
            }
        });
        valueValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after)
            {}
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count)
            {}
            @SuppressLint({"ApplySharedPref", "SetTextI18n"})
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    int colour = fixValue(s.toString());
                    m_fgcolour = colour | (m_fgcolour & 0xFF000000);
                    m_prefs.edit().putInt(m_key + "fgcolour", m_fgcolour).commit();
                    m_clockView.adjustColour();
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
            @SuppressLint({"ApplySharedPref", "SetTextI18n"})
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    int value = safeParseInt(s.toString());
                    if (value > 255) {
                        redValue.setText("255");
                        return;
                    }
                    redSlider.setValue(value);
                    m_fgcolour = (value << 16) + (m_fgcolour & 0xFF00FFFF);
                    m_prefs.edit().putInt(m_key + "fgcolour", m_fgcolour).commit();
                    m_clockView.adjustColour();
                    rgbChanged();
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
            @SuppressLint({"ApplySharedPref", "SetTextI18n"})
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    int val = safeParseInt(s.toString());
                    if (val > 255) {
                        greenValue.setText("255");
                        return;
                    }
                    greenSlider.setValue(val);
                    m_fgcolour = (val << 8) + (m_fgcolour & 0xFFFF00FF);
                    m_prefs.edit().putInt(m_key + "fgcolour", m_fgcolour).commit();
                    m_clockView.adjustColour();
                    rgbChanged();
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
            @SuppressLint({"ApplySharedPref", "SetTextI18n"})
            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive) {
                    int val = safeParseInt(s.toString());
                    if (val > 255) {
                        blueValue.setText("255");
                        return;
                    }
                    blueSlider.setValue(val);
                    m_fgcolour = val + (m_fgcolour & 0xFFFFFF00);
                    m_prefs.edit().putInt(m_key + "fgcolour", m_fgcolour).commit();
                    rgbChanged();
                    m_clockView.adjustColour();
                    blueValue.setSelection(blueValue.getText().length());
                }
            }
        });
        m_data = new Button(this);
        m_data.setText(R.string.setdata);
        m_data.setAllCaps(false);
        m_data.setId(DATABUTTON);
        m_data.setOnClickListener(this);
        m_data.setOnLongClickListener(this);
        m_textColour = new Button(this);
        m_textColour.setText(R.string.setclockcolour);
        m_textColour.setAllCaps(false);
        m_textColour.setId(SETTEXTCOLOUR);
        m_textColour.setOnClickListener(this);
        m_textColour.setOnLongClickListener(this);
        m_display = new Button(this);
        m_display.setText(R.string.setdisplay);
        m_display.setAllCaps(false);
        m_display.setOnClickListener(this);
        m_display.setOnLongClickListener(this);
        m_fgcolour = m_prefs.getInt(m_key + "fgcolour", 0xFFFFFFFF);
        getDatePrefs();
        setCurrentView(m_prefs.getInt("Cview", DATABUTTON));
        m_clockView.adjustColour();
    }
}
