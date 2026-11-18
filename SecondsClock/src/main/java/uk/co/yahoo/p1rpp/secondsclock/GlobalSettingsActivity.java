/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** App-wide preferences that do not belong to an individual clock or widget. */
public class GlobalSettingsActivity extends Activity_common
        implements View.OnClickListener {
    private static final int SYSTEM_CLOCK_HELP_ID = 13001;
    private static final int SYSTEM_CLOCK_OVERRIDE_ID = SYSTEM_CLOCK_HELP_ID + 1;
    private static final int SYSTEM_CLOCK_RESET_ID = SYSTEM_CLOCK_HELP_ID + 2;
    private static final int NEW_WIDGET_DEFAULTS_ID = SYSTEM_CLOCK_HELP_ID + 3;
    private static final int DONE_ID = SYSTEM_CLOCK_HELP_ID + 4;
    private static final int LINE_COLOR_HELP_ID = DONE_ID + 1;
    private static final int LINE_COLOR_PROFILE_ID = DONE_ID + 2;
    private static final int LINE_COLOR_COPY_ID = DONE_ID + 3;
    private static final int LINE_COLOR_REGENERATE_ID = DONE_ID + 4;
    private EditText lineColorSeedInput;
    private boolean lineColorSeedDirty;

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_key = "";
    }

    @Override
    protected void resume() {
        m_key = "";
        super.resume();
        removeAllViews(m_topLayout);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(12);
        page.setPadding(padding, dp(4), padding, padding);

        TextView title = new TextView(this);
        title.setText(R.string.global_settings_button);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        page.addView(title, matchWrap());

        TextView help = new TextView(this);
        help.setText(R.string.global_settings_help);
        help.setGravity(Gravity.CENTER_HORIZONTAL);
        page.addView(help, matchWrap());

        addHeading(page, R.string.system_clock_settings_title,
                SYSTEM_CLOCK_HELP_ID);
        addSystemClockStatus(page);
        addButton(page, R.string.system_clock_override_button,
                SYSTEM_CLOCK_OVERRIDE_ID, R.string.system_clock_settings_help);
        Button reset = addButton(page, R.string.system_clock_reset_button,
                SYSTEM_CLOCK_RESET_ID, R.string.system_clock_settings_help);
        reset.setEnabled(m_prefs.contains(SystemClockApp.OVERRIDE_KEY)
                || m_prefs.getBoolean(SystemClockApp.DONT_ASK_KEY, false));

        addLineColorSettings(page);

        addHeading(page, R.string.new_widget_defaults_title, 0);
        TextView defaultsHelp = new TextView(this);
        defaultsHelp.setText(R.string.new_widget_defaults_help);
        defaultsHelp.setGravity(Gravity.CENTER_HORIZONTAL);
        page.addView(defaultsHelp, matchWrap());
        addButton(page, R.string.configure_new_widget_defaults,
                NEW_WIDGET_DEFAULTS_ID, R.string.helpconfignewwidget);

        addButton(page, R.string.global_settings_done, DONE_ID, 0);

        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        m_topLayout.addView(scroll, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onPause() {
        saveLineColorSeed();
        super.onPause();
    }

    @Override
    public boolean onLongClick(View view) {
        if (view.getId() == SYSTEM_CLOCK_HELP_ID
                || view.getId() == SYSTEM_CLOCK_OVERRIDE_ID
                || view.getId() == SYSTEM_CLOCK_RESET_ID) {
            doToast(R.string.system_clock_settings_help);
            return true;
        }
        if (view.getId() == NEW_WIDGET_DEFAULTS_ID) {
            doToast(R.string.helpconfignewwidget);
            return true;
        }
        if (view.getId() == LINE_COLOR_HELP_ID
                || view.getId() == LINE_COLOR_PROFILE_ID
                || view.getId() == LINE_COLOR_COPY_ID
                || view.getId() == LINE_COLOR_REGENERATE_ID) {
            doToast(R.string.line_color_settings_help);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case SYSTEM_CLOCK_OVERRIDE_ID:
                try {
                    SystemClockApp.openSelector(this, false);
                } catch (ActivityNotFoundException exception) {
                    doToast(R.string.system_clock_selector_not_found);
                }
                break;
            case SYSTEM_CLOCK_RESET_ID:
                confirmResetSystemClockSettings();
                break;
            case NEW_WIDGET_DEFAULTS_ID:
                m_prefs.edit().putInt("Wview", CONFIGURE).apply();
                startActivity(new Intent(this, WidgetConfigureActivity.class));
                break;
            case LINE_COLOR_PROFILE_ID:
                saveLineColorSeed();
                chooseLineColorProfile();
                break;
            case LINE_COLOR_COPY_ID:
                saveLineColorSeed();
                copyLineColorSeed();
                break;
            case LINE_COLOR_REGENERATE_ID:
                if (lineColorSeedInput != null) {
                    lineColorSeedInput.setText(LineColors.newSeed());
                }
                saveLineColorSeed();
                break;
            case DONE_ID:
                finish();
                break;
        }
    }

    private void addSystemClockStatus(LinearLayout parent) {
        TextView status = new TextView(this);
        status.setGravity(Gravity.CENTER_HORIZONTAL);
        if (SystemClockApp.getOverride(m_prefs) == null) {
            status.setText(R.string.system_clock_automatic_status);
        } else {
            String label = SystemClockApp.getOverrideLabel(m_prefs);
            status.setText(label.isEmpty()
                    ? getString(R.string.system_clock_override_unavailable_label)
                    : getString(R.string.system_clock_override_status, label));
        }
        parent.addView(status, matchWrap());
    }

    private void addLineColorSettings(LinearLayout page) {
        addHeading(page, R.string.line_color_settings_title,
                LINE_COLOR_HELP_ID);
        TextView help = new TextView(this);
        help.setText(R.string.line_color_settings_help);
        help.setGravity(Gravity.CENTER_HORIZONTAL);
        page.addView(help, matchWrap());

        CheckBox automatic = new CheckBox(this);
        automatic.setText(R.string.line_color_automatic);
        automatic.setChecked(m_prefs.getBoolean(
                LineColors.ENABLED_KEY, false));
        automatic.setOnCheckedChangeListener((button, checked) -> {
            m_prefs.edit().putBoolean(LineColors.ENABLED_KEY, checked).apply();
            LineColors.notifyChanged(this);
        });
        automatic.setOnLongClickListener(view -> {
            doToast(R.string.line_color_settings_help);
            return true;
        });
        page.addView(automatic, matchWrap());

        Button profile = addButton(page, R.string.line_color_profile_button,
                LINE_COLOR_PROFILE_ID, R.string.line_color_settings_help);
        profile.setText(lineColorProfileButtonText());

        TextView seedLabel = new TextView(this);
        seedLabel.setText(R.string.line_color_seed_label);
        seedLabel.setPadding(0, dp(6), 0, 0);
        page.addView(seedLabel, matchWrap());
        lineColorSeedInput = new EditText(this);
        lineColorSeedInput.setSingleLine(true);
        lineColorSeedInput.setText(m_prefs.getString(
                LineColors.SEED_KEY, LineColors.DEFAULT_SEED));
        lineColorSeedInput.setHint(R.string.line_color_seed_hint);
        lineColorSeedInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(
                    CharSequence text, int start, int count, int after) {}
            @Override public void onTextChanged(
                    CharSequence text, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable text) {
                m_prefs.edit().putString(
                        LineColors.SEED_KEY, text.toString()).apply();
                lineColorSeedDirty = true;
            }
        });
        lineColorSeedInput.setOnFocusChangeListener((view, focused) -> {
            if (!focused) saveLineColorSeed();
        });
        page.addView(lineColorSeedInput, matchWrap());

        LinearLayout seedButtons = new LinearLayout(this);
        seedButtons.setOrientation(LinearLayout.HORIZONTAL);
        Button copy = makeButton(R.string.line_color_copy_seed,
                LINE_COLOR_COPY_ID, R.string.line_color_settings_help);
        Button regenerate = makeButton(R.string.line_color_regenerate_seed,
                LINE_COLOR_REGENERATE_ID, R.string.line_color_settings_help);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0F);
        seedButtons.addView(copy, half);
        seedButtons.addView(regenerate, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0F));
        page.addView(seedButtons, matchWrap());
    }

    private Button makeButton(int label, int id, int helpResource) {
        Button button = new Button(this);
        button.setText(label);
        button.setId(id);
        button.setAllCaps(false);
        button.setOnClickListener(this);
        button.setOnLongClickListener(view -> {
            doToast(helpResource);
            return true;
        });
        return button;
    }

    private String lineColorProfileButtonText() {
        String[] profiles = getResources().getStringArray(
                R.array.line_color_profiles);
        int selected = m_prefs.getInt(LineColors.PROFILE_KEY, LineColors.NORMAL);
        if (selected < 0 || selected >= profiles.length) selected = LineColors.NORMAL;
        return getString(R.string.line_color_profile_button, profiles[selected]);
    }

    private void chooseLineColorProfile() {
        String[] profiles = getResources().getStringArray(
                R.array.line_color_profiles);
        int selected = m_prefs.getInt(LineColors.PROFILE_KEY, LineColors.NORMAL);
        if (selected < 0 || selected >= profiles.length) selected = LineColors.NORMAL;
        new AlertDialog.Builder(this)
                .setTitle(R.string.line_color_profile_title)
                .setSingleChoiceItems(R.array.line_color_profiles, selected,
                        (dialog, which) -> {
                            m_prefs.edit().putInt(
                                    LineColors.PROFILE_KEY, which).apply();
                            LineColors.notifyChanged(this);
                            dialog.dismiss();
                            renderAgain();
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void saveLineColorSeed() {
        if (lineColorSeedInput == null) return;
        m_prefs.edit().putString(LineColors.SEED_KEY,
                lineColorSeedInput.getText().toString()).apply();
        if (lineColorSeedDirty) {
            lineColorSeedDirty = false;
            LineColors.notifyChanged(this);
        }
    }

    private void copyLineColorSeed() {
        String seed = lineColorSeedInput == null
                ? m_prefs.getString(LineColors.SEED_KEY, LineColors.DEFAULT_SEED)
                : lineColorSeedInput.getText().toString();
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(
                    getString(R.string.line_color_seed_label), seed));
            doToast(R.string.line_color_seed_copied);
        }
    }

    private void addHeading(LinearLayout parent, int label, int id) {
        TextView heading = centredLabel(label, id);
        heading.setTextSize(18);
        heading.setPadding(0, dp(12), 0, dp(4));
        parent.addView(heading, matchWrap());
    }

    private Button addButton(LinearLayout parent, int label, int id,
                             int helpResource) {
        Button button = new Button(this);
        button.setText(label);
        button.setId(id);
        button.setAllCaps(false);
        button.setOnClickListener(this);
        if (helpResource != 0) {
            button.setOnLongClickListener(view -> {
                doToast(helpResource);
                return true;
            });
        }
        parent.addView(button, matchWrap());
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void confirmResetSystemClockSettings() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.system_clock_reset_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.system_clock_reset_button,
                        (dialog, which) -> {
                            SystemClockApp.reset(m_prefs);
                            renderAgain();
                            doToast(R.string.system_clock_reset_done);
                        })
                .show();
    }

    private void renderAgain() {
        if (m_width > 0 && m_height > 0) resume();
    }
}
