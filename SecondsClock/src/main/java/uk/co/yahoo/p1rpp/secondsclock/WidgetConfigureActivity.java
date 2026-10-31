/*
 * Copyright © 2024. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This class handles the settings for SecondsClockWidget.
 * It also shows what the widget will look like in 1x1 size.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import static android.text.InputType.TYPE_CLASS_NUMBER;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
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
import android.view.inputmethod.EditorInfo;
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
    private static final int FORMATFIELD = SETBACKGROUNDCOLOUR + 1;
    private static final int FORMATHELP = FORMATFIELD + 1;
    private static final int FORMATMODE = FORMATHELP + 1;
    private static final int NEXTALARM = FORMATMODE + 1;
    private static final int FORMATPROFILE_SYNC = NEXTALARM + 1;
    private static final int FORMATPROFILE_HORIZONTAL = FORMATPROFILE_SYNC + 1;
    private static final int FORMATPROFILE_VERTICAL = FORMATPROFILE_HORIZONTAL + 1;

    private int widgetId;
    private int maxHeight = 121;
    private int minWidth = 93;
    private int m_bgcolour;
    private TextClock demo;
    private int m_fgcolour;
    private String widgetFormat;
    private int showTime;
    private int formatProfile = FormatProfiles.SYNC;
    private boolean advancedFormatMode;
    private boolean changingFormatInput;
    private boolean changingTimeChecks;
    private LinearLayout demobox;
    private LinearLayout demoboxbox;
    private EditText formatInput;
    private Button formatModeButton;
    private Button syncFormatButton;
    private Button horizontalFormatButton;
    private Button verticalFormatButton;
    private CheckBox showTimeCheckBox;
    private CheckBox showSecondsCheckBox;
    private CheckBox showNextAlarmCheckBox;
    private Button bgButton;
    private Button fgButton;
    private Button chooseButton;
    private CheckBox sysClockCheckBox;
    private CheckBox secondsClockCheckBox;
    private CheckBox configThisCheckBox;
    private CheckBox configNightClockCheckBox;
    private CheckBox runNightClockCheckBox;
    private CheckBox chooseCheckBox;

    private static class FormatToken {
        final int labelResource;
        final String insertion;
        final int cursorOffset;

        FormatToken(int labelResource, String insertion) {
            this(labelResource, insertion, insertion.length());
        }

        FormatToken(int labelResource, String insertion, int cursorOffset) {
            this.labelResource = labelResource;
            this.insertion = insertion;
            this.cursorOffset = cursorOffset;
        }
    }

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
            case FORMATHELP: doToast(R.string.widget_format_help); return true;
            case NEXTALARM: doToast(R.string.widget_alarm_source_help); return true;
            case FORMATMODE: doToast(R.string.widgetconfighelp); return true;
            case DONEBUTTON: doToast(R.string.donehelp); return true;
            case SETTEXTCOLOUR: doToast(R.string.setfgcolourhelp); return true;
            case SETBACKGROUNDCOLOUR: doToast(R.string.setbgcolourhelp); return true;
            case GO_SYSTEM_CLOCK:
                doToast(R.string.helptouchsysclock);
                return true;
            case GO_SECONDS_CLOCK:
                doToast(R.string.helptouchsecondsclock);
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

    private void fixCheckBoxes(int action) {
        if (!recursive) {
            recursive = true;
            sysClockCheckBox.setChecked(action == GO_SYSTEM_CLOCK);
            secondsClockCheckBox.setChecked(action == GO_SECONDS_CLOCK);
            configThisCheckBox.setChecked(action == CONFIGURE_EXISTING_WIDGET);
            configNightClockCheckBox.setChecked(action == CONFIGURE_NIGHT_CLOCK);
            runNightClockCheckBox.setChecked(action == GO_NIGHT_CLOCK);
            chooseCheckBox.setChecked(action == CHOOSE_ACTION);
            recursive = false;
        }
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == NEXTALARM) {
            m_prefs.edit().putBoolean(m_key + "showNextAlarm", isChecked).apply();
            updateFromCheckBox();
            return;
        }
        int action = buttonView.getId();
        switch (action) {
            case GO_SYSTEM_CLOCK:
            case GO_SECONDS_CLOCK:
            case CONFIGURE_EXISTING_WIDGET:
            case CONFIGURE_NIGHT_CLOCK:
            case GO_NIGHT_CLOCK:
            case CHOOSE_ACTION:
                fixCheckBoxes(action);
                m_prefs.edit().putInt(
                    m_key + "touchaction", action).commit();
                break;
            default:
                super.onCheckedChanged(buttonView, isChecked);
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
        Intent intent = new Intent("uk.co.yahoo.p1rpp.UpdateWidget");
        intent.setClass(this, SecondsClockWidget.class);
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
            m_prefs.edit().putInt(m_key + "fgcolour", m_fgcolour).commit();
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
            case FORMATMODE:
                advancedFormatMode = !advancedFormatMode;
                if (advancedFormatMode) initializeAdvancedFormats();
                m_prefs.edit().putBoolean(
                        m_key + "advancedFormatMode", advancedFormatMode).apply();
                updateWidget();
                updateDemo();
                setCurrentView(CONFIGURE);
                break;
            case FORMATPROFILE_SYNC:
            case FORMATPROFILE_HORIZONTAL:
            case FORMATPROFILE_VERTICAL:
                selectFormatProfile(v.getId());
                break;
            case DONEBUTTON:
                if (m_currentView == CONFIGURE) {
                    finish();
                } else {
                    setCurrentView(CONFIGURE);
                }
        }
    }

    private String formatPreferenceForProfile(int profile) {
        if (profile == FormatProfiles.HORIZONTAL) {
            return m_key + "formatHorizontal";
        }
        if (profile == FormatProfiles.VERTICAL) {
            return m_key + "formatVertical";
        }
        return m_key + "format";
    }

    private String getFormatForProfile(int profile) {
        String sync = m_prefs.getString(m_key + "format", widgetFormat);
        if (profile == FormatProfiles.HORIZONTAL) {
            return m_prefs.getString(m_key + "formatHorizontal", sync);
        }
        if (profile == FormatProfiles.VERTICAL) {
            return m_prefs.getString(m_key + "formatVertical", sync);
        }
        return sync;
    }

    private void initializeAdvancedFormats() {
        if (!m_prefs.contains(m_key + "format")) {
            widgetFormat = Formatter.fromLegacyPreferences(this, m_prefs, m_key);
            m_prefs.edit().putString(m_key + "format", widgetFormat).apply();
        } else {
            widgetFormat = m_prefs.getString(m_key + "format", "");
        }
        if (!m_prefs.contains(m_key + "formatSyncEnabled")) {
            m_prefs.edit().putBoolean(m_key + "formatSyncEnabled", true).apply();
        }
    }

    private void selectFormatProfile(int profile) {
        formatProfile = profile;
        boolean synchronizedFormats = profile == FormatProfiles.SYNC;
        m_prefs.edit()
                .putInt(m_key + "formatProfile", profile)
                .putBoolean(m_key + "formatSyncEnabled", synchronizedFormats)
                .apply();
        widgetFormat = getFormatForProfile(profile);
        if (formatInput != null) {
            changingFormatInput = true;
            formatInput.setText(widgetFormat);
            formatInput.setSelection(formatInput.length());
            changingFormatInput = false;
        }
        updateProfileButtonStates();
        updateWidget();
        updateDemo();
    }

    private void updateProfileButtonStates() {
        if (syncFormatButton != null) {
            boolean syncSelected = formatProfile == FormatProfiles.SYNC;
            boolean horizontalSelected =
                    formatProfile == FormatProfiles.HORIZONTAL;
            boolean verticalSelected = formatProfile == FormatProfiles.VERTICAL;
            syncFormatButton.setSelected(syncSelected);
            horizontalFormatButton.setSelected(horizontalSelected);
            verticalFormatButton.setSelected(verticalSelected);
            syncFormatButton.setAlpha(syncSelected ? 1.0F : 0.65F);
            horizontalFormatButton.setAlpha(horizontalSelected ? 1.0F : 0.65F);
            verticalFormatButton.setAlpha(verticalSelected ? 1.0F : 0.65F);
        }
    }

    private Button makeProfileButton(int id, int label) {
        Button button = new Button(this);
        button.setId(id);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(this);
        return button;
    }

    private LinearLayout makeFormatProfileSelector() {
        LinearLayout profiles = new LinearLayout(this);
        profiles.setOrientation(LinearLayout.HORIZONTAL);
        syncFormatButton = makeProfileButton(
                FORMATPROFILE_SYNC, R.string.format_profile_sync);
        horizontalFormatButton = makeProfileButton(
                FORMATPROFILE_HORIZONTAL, R.string.format_profile_horizontal);
        verticalFormatButton = makeProfileButton(
                FORMATPROFILE_VERTICAL, R.string.format_profile_vertical);
        LinearLayout.LayoutParams weight = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0F);
        profiles.addView(syncFormatButton, weight);
        profiles.addView(horizontalFormatButton, weight);
        profiles.addView(verticalFormatButton, weight);
        updateProfileButtonStates();
        return profiles;
    }

    void updateDemo() {
        Formatter f = new Formatter();
        Long nextAlarm = Formatter.getNextAlarmTime(this);
        if (advancedFormatMode) {
            f.set(getFormatForProfile(formatProfile), nextAlarm);
        } else {
            f.setCheckboxes(this, minWidth, maxHeight, showTime,
                    showWeekDay, showShortDate, showMonthDay, showMonth,
                    showYear, showNextAlarmCheckBox != null
                            && showNextAlarmCheckBox.isChecked(), nextAlarm);
        }
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
        LinearLayout settingsEditor = advancedFormatMode
                ? makeFormatEditor() : makeCheckboxEditor();
        formatModeButton.setText(advancedFormatMode
                ? R.string.checkbox_mode : R.string.advanced_format_mode);
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
            l1.addView(l2);
            LinearLayout l3 = new LinearLayout(this);
            l3.setOrientation(LinearLayout.VERTICAL);
            l3.addView(settingsEditor, lpMatchWrap);
            LinearLayout l8 = new LinearLayout(this);
            // default orientation is HORIZONTAL
            l8.addView(bgButton);
            l8.addView(fgButton);
            l3.addView(l8);
            l3.addView(chooseButton, lpWrapWrap);
            l3.addView(formatModeButton, lpWrapWrap);
            l3.addView(m_okButton, lpWrapWrap);
            l1.addView(l3, lpWrapWrap);
            LinearLayout page = new LinearLayout(this);
            page.setOrientation(LinearLayout.VERTICAL);
            page.addView(l1, lpMatchWrap);
            if (advancedFormatMode) page.addView(makeFormatGuide(), lpMatchWrap);
            lscroll.addView(page);
        } else { // assume Portrait
            l1.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setLayoutParams(lpMatchWrap);
            demoboxbox.setOrientation(LinearLayout.VERTICAL);
            demoboxbox.setGravity(Gravity.CENTER_HORIZONTAL);
            l1.addView(demoboxbox);
            m_helptext.setText(R.string.longpresshoriz);
            m_helptext.setGravity(Gravity.NO_GRAVITY);
            l1.addView(m_helptext);
            l1.addView(settingsEditor, lpMatchWrap);
            LinearLayout l2 = new LinearLayout(this);
            l2.setLayoutParams(lpMatchWrap);
            l2.setOrientation(LinearLayout.VERTICAL);
            l2.setGravity(Gravity.CENTER_HORIZONTAL);
            l2.addView(bgButton, lpWrapWrap);
            l2.addView(fgButton, lpWrapWrap);
            l2.addView(chooseButton, lpWrapWrap);
            l2.addView(formatModeButton, lpWrapWrap);
            l2.addView(m_okButton, lpWrapWrap);
            if (advancedFormatMode) l2.addView(makeFormatGuide(), lpMatchWrap);
            l1.addView(l2);
            lscroll.addView(l1);
        }
        m_topLayout.addView(lscroll);
    }

    private LinearLayout makeCheckboxEditor() {
        LinearLayout editor = new LinearLayout(this);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.addView(showTimeCheckBox, lpWrapWrap);
        editor.addView(showSecondsCheckBox, lpWrapWrap);
        editor.addView(showShortWeekDayCheckBox, lpWrapWrap);
        editor.addView(showLongWeekDayCheckBox, lpWrapWrap);
        editor.addView(showShortDateCheckBox, lpWrapWrap);
        editor.addView(showMonthDayCheckBox, lpWrapWrap);
        editor.addView(showShortMonthCheckBox, lpWrapWrap);
        editor.addView(showLongMonthCheckBox, lpWrapWrap);
        editor.addView(showYearCheckBox, lpWrapWrap);
        editor.addView(showNextAlarmCheckBox, lpWrapWrap);
        TextView alarmHelp = new TextView(this);
        alarmHelp.setText(R.string.widget_alarm_source_help);
        editor.addView(alarmHelp, lpMatchWrap);
        return editor;
    }

    private LinearLayout makeFormatEditor() {
        LinearLayout editor = new LinearLayout(this);
        editor.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(this);
        label.setText(R.string.widget_format_label);
        label.setId(FORMATHELP);
        label.setLabelFor(FORMATFIELD);
        label.setOnLongClickListener(this);
        editor.addView(label);
        TextView hint = new TextView(this);
        hint.setText(R.string.widget_format_caption);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        editor.addView(hint, lpMatchWrap);
        TextView profileHint = new TextView(this);
        profileHint.setText(R.string.widget_format_profile_hint);
        profileHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        editor.addView(profileHint, lpMatchWrap);
        editor.addView(makeFormatProfileSelector(), lpMatchWrap);
        editor.addView(formatInput, lpMatchWrap);
        return editor;
    }

    private LinearLayout makeFormatGuide() {
        LinearLayout guide = new LinearLayout(this);
        guide.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(6 * m_density);
        guide.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText(R.string.widget_format_guide_title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        guide.addView(title, lpMatchWrap);
        TextView instructions = new TextView(this);
        instructions.setText(R.string.widget_format_guide_hint);
        guide.addView(instructions, lpMatchWrap);

        addGuideSection(guide, R.string.widget_format_time_section);
        addFormatTokenRow(guide,
                new FormatToken(R.string.format_token_hour24, "%H"),
                new FormatToken(R.string.format_token_hour12, "%I"));
        addFormatTokenRow(guide,
                new FormatToken(R.string.format_token_minute, "%M"),
                new FormatToken(R.string.format_token_second, "%S"));
        addFormatTokenRow(guide,
                new FormatToken(R.string.format_token_ampm, "%p"),
                new FormatToken(R.string.format_token_local_time, "%X"));

        addGuideSection(guide, R.string.widget_format_date_section);
        addFormatTokenRow(guide,
                new FormatToken(R.string.format_token_short_weekday, "%a"),
                new FormatToken(R.string.format_token_full_weekday, "%A"));
        addFormatTokenRow(guide,
                new FormatToken(R.string.format_token_short_month, "%b"),
                new FormatToken(R.string.format_token_full_month, "%B"));
        addFormatTokenRow(guide,
                new FormatToken(R.string.format_token_day, "%d"),
                new FormatToken(R.string.format_token_day_unpadded, "%e"));
        addFormatTokenRow(guide,
                new FormatToken(R.string.format_token_month, "%m"),
                new FormatToken(R.string.format_token_year, "%Y"));
        addFormatTokenRow(guide,
                new FormatToken(R.string.format_token_iso_date, "%F"),
                new FormatToken(R.string.format_token_local_date, "%x"));
        addFormatTokenRow(guide,
                new FormatToken(R.string.format_token_local_time, "%X"),
                new FormatToken(R.string.format_token_newline, "%n"));

        addGuideSection(guide, R.string.widget_format_other_section);
        addFormatTokenRow(guide,
                new FormatToken(R.string.format_token_percent, "%%"), null);

        addGuideSection(guide, R.string.widget_format_alarm_section);
        addFormatTokenRow(guide,
                new FormatToken(R.string.format_token_alarm, "%<%>", 2), null);
        TextView alarmHint = new TextView(this);
        alarmHint.setText(R.string.widget_format_alarm_hint);
        guide.addView(alarmHint, lpMatchWrap);
        return guide;
    }

    private void addGuideSection(LinearLayout guide, int labelResource) {
        TextView label = new TextView(this);
        label.setText(labelResource);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setPadding(0, (int)(8 * m_density), 0, 0);
        guide.addView(label, lpMatchWrap);
    }

    private void addFormatTokenRow(
            LinearLayout guide, FormatToken first, FormatToken second) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(makeFormatTokenButton(first), new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0F));
        if (second != null) {
            row.addView(makeFormatTokenButton(second),
                    new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0F));
        }
        guide.addView(row, lpMatchWrap);
    }

    private Button makeFormatTokenButton(FormatToken token) {
        Button button = new Button(this);
        button.setText(token.labelResource);
        button.setAllCaps(false);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        button.setOnClickListener(v ->
                insertFormatToken(token.insertion, token.cursorOffset));
        return button;
    }

    private void insertFormatToken(String token, int cursorOffset) {
        Editable text = formatInput.getText();
        int selectionStart = formatInput.getSelectionStart();
        int selectionEnd = formatInput.getSelectionEnd();
        if (selectionStart < 0 || selectionEnd < 0) {
            selectionStart = text.length();
            selectionEnd = selectionStart;
        }
        int replaceStart = Math.min(selectionStart, selectionEnd);
        int replaceEnd = Math.max(selectionStart, selectionEnd);
        text.replace(replaceStart, replaceEnd, token);
        formatInput.requestFocus();
        formatInput.setSelection(replaceStart + cursorOffset);
    }

    protected void doChooserLayout() {
        removeAllViews(m_topLayout);
        LinearLayout lc = makeChooser();
        int pad = (int)(5 * m_density);
        LinearLayout l1 = new LinearLayout(this);
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
        lbuttons.addView(secondsClockCheckBox, lpWrapWrap);
        lbuttons.addView(configThisCheckBox, lpWrapWrap);
        lbuttons.addView(configNightClockCheckBox, lpWrapWrap);
        lbuttons.addView(runNightClockCheckBox, lpWrapWrap);
        lbuttons.addView(chooseCheckBox, lpWrapWrap);
        LinearLayout l1 = new LinearLayout(this);
        // default orientation is HORIZONTAL
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
            intent = new Intent("uk.co.yahoo.p1rpp.UpdateWidget");
            intent.setClass(this, SecondsClockWidget.class);
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
        showTime = m_prefs.getInt(m_key + "showTime", 2);
        advancedFormatMode = m_prefs.getBoolean(
                m_key + "advancedFormatMode", m_prefs.contains(m_key + "format"));
        formatProfile = m_prefs.getInt(
                m_key + "formatProfile", FormatProfiles.SYNC);
        if (formatProfile < FormatProfiles.SYNC
                || formatProfile > FormatProfiles.VERTICAL) {
            formatProfile = FormatProfiles.SYNC;
        }
        widgetFormat = m_prefs.getString(m_key + "format", null);
        if (widgetFormat == null) {
            widgetFormat = Formatter.fromLegacyPreferences(this, m_prefs, m_key);
        }
        if (advancedFormatMode) initializeAdvancedFormats();
        showTimeCheckBox = new CheckBox(this);
        showTimeCheckBox.setId(SHOWTIME);
        showTimeCheckBox.setText(R.string.show_time);
        showTimeCheckBox.setChecked(showTime > 0);
        showTimeCheckBox.setOnLongClickListener(this);
        showTimeCheckBox.setOnCheckedChangeListener((button, checked) -> {
            if (changingTimeChecks) return;
            if (checked) {
                showTime = showSecondsCheckBox.isChecked() ? 2 : 1;
            } else {
                showTime = 0;
                changingTimeChecks = true;
                showSecondsCheckBox.setChecked(false);
                changingTimeChecks = false;
            }
            showSecondsCheckBox.setVisibility(
                    checked ? View.VISIBLE : View.GONE);
            m_prefs.edit().putInt(m_key + "showTime", showTime).apply();
            updateFromCheckBox();
        });
        showSecondsCheckBox = new CheckBox(this);
        showSecondsCheckBox.setId(SHOWSECONDS);
        showSecondsCheckBox.setText(R.string.show_seconds);
        showSecondsCheckBox.setChecked(showTime == 2);
        showSecondsCheckBox.setVisibility(
                showTime > 0 ? View.VISIBLE : View.GONE);
        showSecondsCheckBox.setOnLongClickListener(this);
        showSecondsCheckBox.setOnCheckedChangeListener((button, checked) -> {
            if (changingTimeChecks) return;
            if (checked && !showTimeCheckBox.isChecked()) {
                changingTimeChecks = true;
                showTimeCheckBox.setChecked(true);
                changingTimeChecks = false;
            }
            showTime = checked ? 2 : (showTimeCheckBox.isChecked() ? 1 : 0);
            m_prefs.edit().putInt(m_key + "showTime", showTime).apply();
            updateFromCheckBox();
        });
        showNextAlarmCheckBox = new CheckBox(this);
        showNextAlarmCheckBox.setId(NEXTALARM);
        showNextAlarmCheckBox.setText(R.string.show_next_alarm_any_app);
        showNextAlarmCheckBox.setChecked(
                m_prefs.getBoolean(m_key + "showNextAlarm", false));
        showNextAlarmCheckBox.setOnCheckedChangeListener(this);
        showNextAlarmCheckBox.setOnLongClickListener(this);
        formatInput = new EditText(this);
        formatInput.setId(FORMATFIELD);
        formatInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        formatInput.setSingleLine(false);
        formatInput.setMinLines(4);
        formatInput.setMaxLines(6);
        formatInput.setGravity(Gravity.TOP | Gravity.START);
        formatInput.setVerticalScrollBarEnabled(true);
        formatInput.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        formatInput.setHint(R.string.widget_format_hint);
        formatInput.setText(getFormatForProfile(formatProfile));
        formatInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!recursive && !changingFormatInput) {
                    widgetFormat = s.toString();
                    m_prefs.edit().putString(
                            formatPreferenceForProfile(formatProfile),
                            widgetFormat).apply();
                    updateWidget();
                    updateDemo();
                }
            }
        });
        formatModeButton = new Button(this);
        formatModeButton.setId(FORMATMODE);
        formatModeButton.setAllCaps(false);
        formatModeButton.setOnClickListener(this);
        formatModeButton.setOnLongClickListener(this);
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
        chooseButton.setText(R.string.setaction);
        chooseButton.setOnClickListener(this);
        chooseButton.setOnLongClickListener(this);
        sysClockCheckBox = new CheckBox(this);
        sysClockCheckBox.setId(GO_SYSTEM_CLOCK);
        sysClockCheckBox.setText(getString(R.string.gosysclock));
        sysClockCheckBox.setOnCheckedChangeListener(this);
        sysClockCheckBox.setOnLongClickListener(this);
        secondsClockCheckBox = new CheckBox(this);
        secondsClockCheckBox.setId(GO_SECONDS_CLOCK);
        secondsClockCheckBox.setText(getString(R.string.gosecondsclock));
        secondsClockCheckBox.setOnCheckedChangeListener(this);
        secondsClockCheckBox.setOnLongClickListener(this);
        configThisCheckBox = new CheckBox(this);
        configThisCheckBox.setId(CONFIGURE_EXISTING_WIDGET);
        configThisCheckBox.setText(getString(R.string.configwidget));
        configThisCheckBox.setOnCheckedChangeListener(this);
        configThisCheckBox.setOnLongClickListener(this);
        configNightClockCheckBox = new CheckBox(this);
        configNightClockCheckBox.setId(CONFIGURE_NIGHT_CLOCK);
        configNightClockCheckBox.setText(getString(R.string.confignightclock));
        configNightClockCheckBox.setOnCheckedChangeListener(this);
        configNightClockCheckBox.setOnLongClickListener(this);
        runNightClockCheckBox = new CheckBox(this);
        runNightClockCheckBox.setId(GO_NIGHT_CLOCK);
        runNightClockCheckBox.setText(getString(R.string.runnightclock));
        runNightClockCheckBox.setOnCheckedChangeListener(this);
        runNightClockCheckBox.setOnLongClickListener(this);
        chooseCheckBox = new CheckBox(this);
        chooseCheckBox.setId(CHOOSE_ACTION);
        chooseCheckBox.setText(getString(R.string.chooseaction));
        chooseCheckBox.setOnCheckedChangeListener(this);
        chooseCheckBox.setOnLongClickListener(this);
        int action = m_prefs.getInt(m_key + "touchaction", CHOOSE_ACTION);
        fixCheckBoxes(action);

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
