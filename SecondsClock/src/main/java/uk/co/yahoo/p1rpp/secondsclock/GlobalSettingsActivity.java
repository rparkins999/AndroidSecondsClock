/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

        addHeading(page, R.string.new_widget_defaults_title, 0);
        TextView defaultsHelp = new TextView(this);
        defaultsHelp.setText(R.string.new_widget_defaults_help);
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
