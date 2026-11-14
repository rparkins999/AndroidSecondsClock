/*

 * Copyright © 2024. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 *
 * This is the top level activity, launched when you touch tha app's icon.
 * It just offers you a choice of what to do next.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity_common
    implements View.OnClickListener, View.OnLongClickListener
{
    private int widgetID;
    private boolean widgetActionHandled;
    private boolean systemClockPromptPending;
    private boolean systemClockPromptPermissionFailure;
    private static final int CONFIGURE_THIS_WIDGET = GO_EXIT + 1;
    private static final int EXPORT_SETTINGS = CONFIGURE_THIS_WIDGET + 1;
    private static final int IMPORT_SETTINGS = EXPORT_SETTINGS + 1;
    private static final int MANAGE_SETTINGS = IMPORT_SETTINGS + 1;
    private static final int GLOBAL_SETTINGS = MANAGE_SETTINGS + 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            widgetActionHandled = savedInstanceState.getBoolean(
                    "widgetActionHandled", false);
            systemClockPromptPending = savedInstanceState.getBoolean(
                    "systemClockPromptPending", false);
            systemClockPromptPermissionFailure = savedInstanceState.getBoolean(
                    "systemClockPromptPermissionFailure", false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("widgetActionHandled", widgetActionHandled);
        outState.putBoolean("systemClockPromptPending", systemClockPromptPending);
        outState.putBoolean("systemClockPromptPermissionFailure",
                systemClockPromptPermissionFailure);
        super.onSaveInstanceState(outState);
    }

    private Intent systemClockIntent(String category) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                      | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        intent.addCategory(category);
        return intent;
    }

    private boolean tryLaunchSystemClock(Intent intent) {
        try {
            startActivity(intent);
            finish();
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    private boolean runKnownSystemClockAttempts() {
        String[] packages = {
            "com.android.deskclock",
            "com.sec.android.app.clockpackage"
        };
        String[] activities = {
            "com.android.deskclock.DeskClock",
            "com.sec.android.app.clockpackage.Clockpackage"
        };
        for (String category : new String[] {
                Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_DEFAULT }) {
            for (int i = 0; i < packages.length; ++i) {
                Intent intent = systemClockIntent(category)
                        .setClassName(packages[i], activities[i]);
                if (tryLaunchSystemClock(intent)) return true;
            }
        }
        return false;
    }

    private void launchSecondsClockFallback() {
        doToast(R.string.system_clock_seconds_fallback);
        startActivity(new Intent(this, ClockActivity.class));
        finish();
    }

    private void saveDontAskPreference(boolean dontAsk) {
        m_prefs.edit().putBoolean(
                SystemClockApp.DONT_ASK_KEY, dontAsk).apply();
    }

    private void promptForSystemClockApp(boolean permissionFailure) {
        systemClockPromptPending = true;
        systemClockPromptPermissionFailure = permissionFailure;
        CheckBox dontAsk = new CheckBox(this);
        dontAsk.setText(R.string.system_clock_dont_ask_again);
        int padding = Math.round(16 * getResources().getDisplayMetrics().density);
        dontAsk.setPadding(padding, 0, padding, 0);

        new AlertDialog.Builder(this)
                .setTitle(R.string.system_clock_not_found_title)
                .setMessage(permissionFailure
                        ? R.string.system_clock_permission_message
                        : R.string.system_clock_not_found_message)
                .setView(dontAsk)
                .setPositiveButton(R.string.system_clock_choose_app,
                        (dialog, which) -> {
                            systemClockPromptPending = false;
                            saveDontAskPreference(dontAsk.isChecked());
                            try {
                                SystemClockApp.openSelector(this, true);
                            } catch (ActivityNotFoundException exception) {
                                doToast(R.string.system_clock_selector_not_found);
                                launchSecondsClockFallback();
                            }
                        })
                .setNegativeButton(R.string.system_clock_not_now,
                        (dialog, which) -> {
                            systemClockPromptPending = false;
                            saveDontAskPreference(dontAsk.isChecked());
                            launchSecondsClockFallback();
                        })
                .setOnCancelListener(dialog -> systemClockPromptPending = false)
                .show();
    }

    private void finishSystemClockFailure(boolean permissionFailure) {
        if (m_prefs.getBoolean(SystemClockApp.DONT_ASK_KEY, false)) {
            if (permissionFailure) doToast(R.string.noclockpermission);
            launchSecondsClockFallback();
        } else {
            promptForSystemClockApp(permissionFailure);
        }
    }

    // Try a saved override first, then the existing best-effort discovery.
    private void goSystemClock() {
        ComponentName override = SystemClockApp.getOverride(m_prefs);
        if (override != null) {
            Intent intent = systemClockIntent(Intent.CATEGORY_LAUNCHER)
                    .setComponent(override);
            if (tryLaunchSystemClock(intent)) return;
            // A stale or unavailable component should not block automatic lookup.
            SystemClockApp.clearOverride(m_prefs);
        }

        if (runKnownSystemClockAttempts()) return;

        Intent intent = new Intent("android.intent.action.SHOW_TIMERS");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                      | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        try {
            startActivity(intent);
            finish();
        } catch (SecurityException ignore) {
            finishSystemClockFailure(true);
        } catch (Exception ignore) {
            finishSystemClockFailure(false);
        }
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case GO_SYSTEM_CLOCK:
                goSystemClock();
                break;
                // GO_SECONDS_CLOCK isn't a main menu button
            case CONFIGURE_NEW_WIDGET:
                m_prefs.edit().putInt("Wview", CONFIGURE).commit();
                startActivity(new Intent(
                    this, WidgetConfigureActivity.class));
                break;
            case CONFIGURE_THIS_WIDGET:
                m_prefs.edit().putInt("Wview", CONFIGURE).commit();
                startActivity(
                    new Intent(this, WidgetConfigureActivity.class)
                        .putExtra("widgetID", widgetID));
                finish();
                break;
            case EXPORT_SETTINGS:
                startSettingsActivity(SettingsManagerActivity.ACTION_EXPORT);
                break;
            case IMPORT_SETTINGS:
                startSettingsActivity(SettingsManagerActivity.ACTION_IMPORT);
                break;
            case MANAGE_SETTINGS:
                startSettingsActivity(SettingsManagerActivity.ACTION_MANAGE);
                break;
            case GLOBAL_SETTINGS:
                startActivity(new Intent(this, GlobalSettingsActivity.class));
                break;
            case CONFIGURE_EXISTING_WIDGET:
                m_prefs.edit().putInt("Wview", CONFIGURE)
                    .putInt("Wconfiguring", 1).commit();
                doToast(R.string.actionoldwidget);
                Intent homeIntent= new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(homeIntent);
                finish();
                break;
            case CONFIGURE_NIGHT_CLOCK:
                m_prefs.edit().putInt("Cview", CONFIGURE).commit();
                startActivity(new Intent(
                    this, ClockConfigureActivity.class));
                break;
            case GO_NIGHT_CLOCK:
                startActivity(new Intent(
                    this, ClockActivity.class));
                finish();
                break;
            case GO_EXIT:
                finish();
                break;
            default: v.performClick();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case LONGPRESSHELP:
                doToast(R.string.mainactivityhelp); return true;
            case GO_SYSTEM_CLOCK:
                doToast(R.string.helpgosysclock); return true;
            // GO_SECONDS_CLOCK isn't a main menu button
            case CONFIGURE_NEW_WIDGET:
                doToast(R.string.helpconfignewwidget); return true;
            case CONFIGURE_THIS_WIDGET:
                doToast(R.string.helpconfigthis); return true;
            case EXPORT_SETTINGS:
                doToast(R.string.export_settings_help); return true;
            case IMPORT_SETTINGS:
                doToast(R.string.import_settings_help); return true;
            case MANAGE_SETTINGS:
                doToast(R.string.settings_manager_help); return true;
            case GLOBAL_SETTINGS:
                doToast(R.string.global_settings_help); return true;
            case CONFIGURE_EXISTING_WIDGET:
                doToast(R.string.helpoldwidget); return true;
            case CONFIGURE_NIGHT_CLOCK:
                doToast(R.string.helpconfignightclock); return true;
            case GO_NIGHT_CLOCK:
                doToast(R.string.helprunnightclock); return true;
            case GO_EXIT:
                doToast(R.string.exithelp); return true;
            default:
                return false;
        }
    }

    private void startSettingsActivity(String action) {
        startActivity(new Intent(this, SettingsManagerActivity.class)
                .putExtra(SettingsManagerActivity.EXTRA_START_ACTION, action));
    }

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        boolean restoreClockPrompt = systemClockPromptPending;
        boolean permissionFailure = systemClockPromptPermissionFailure;
        if (intent.hasExtra("widgetID")) {
            // We got here because the user touched one of our widgets.
            widgetID = intent.getIntExtra("widgetID", -1);
            if (m_prefs.getInt("Wconfiguring", 0) != 0) {
                // We told the user to pick a widget, now configure it
                m_prefs.edit().putInt("Wview", CONFIGURE)
                    .putInt("Wconfiguring", 0).commit();
                startActivity(
                    new Intent(this, WidgetConfigureActivity.class)
                    .putExtra("widgetID", widgetID));
                finish();
            } else if (!widgetActionHandled) {
                // External clock apps and the app chooser can return here.
                // Do not repeat a widget action when this Activity resumes.
                widgetActionHandled = true;
                // Do the touch action for the widget that called us.
                String key = "W" + widgetID + "touchaction";
                switch (m_prefs.getInt(key, CHOOSE_ACTION)) {
                    case GO_SYSTEM_CLOCK:
                        goSystemClock();
                        break;
                    case CONFIGURE_NEW_WIDGET:
                        m_prefs.edit().putInt("Wview", CONFIGURE).commit();
                        startActivity(new Intent(
                            this, WidgetConfigureActivity.class));
                        finish();
                        break;
                    case CONFIGURE_EXISTING_WIDGET:
                        m_prefs.edit().putInt("Wview", CONFIGURE).commit();
                        startActivity(new Intent(
                            this, WidgetConfigureActivity.class)
                            .putExtra("widgetID", widgetID));
                        finish();
                        break;
                    case CONFIGURE_NIGHT_CLOCK:
                        startActivity(new Intent(
                            this, ClockConfigureActivity.class));
                        finish();
                        break;
                    case GO_NIGHT_CLOCK:
                        startActivity(new Intent(
                            this, ClockActivity.class));
                        finish();
                        break;
                    default:
                }
            }
        } else {
            m_prefs.edit().putInt("Wconfiguring", 0).commit();
            widgetID = -1;
            // Update our widgets in case we've been reinstalled
            int[] widgetIds
                = appWidgetManager.getAppWidgetIds(secondsClockWidget);
            if (widgetIds.length > 0) {
                intent = new Intent("uk.co.yahoo.p1rpp.UpdateWidget");
                intent.setClass(this, SecondsClockWidget.class);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
                sendBroadcast(intent);
            }
        }
        if (restoreClockPrompt) {
            promptForSystemClockApp(permissionFailure);
        }
        // CHOOSE_ACTION or GO_SECONDS_CLOCK or we got launched:
        // onResume will set up our screen.
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void resume() {
        m_key = ""; // date preferences not used in this activity
        super.resume();
        removeAllViews(m_topLayout);
        m_helptext = new TextView(this);
        ScrollView lscroll = new ScrollView(this);
        lscroll.setScrollbarFadingEnabled(false);
        LinearLayout lmain = new LinearLayout(this);
        lmain.setOrientation(LinearLayout.VERTICAL);
        lmain.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        String s = "";
        try
        {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName (), 0);
            s = getString(R.string.app_name) + " " + pi.versionName
                + " · " + getString(R.string.build_time) + "\n";
        } catch (PackageManager.NameNotFoundException ignore) {}
        m_helptext.setText(s + getString(R.string.longpresshoriz));
        m_helptext.setId(LONGPRESSHELP);
        m_helptext.setOnLongClickListener(this);
        lmain.addView(m_helptext);

        addMenuSection(lmain, R.string.main_section_clock);
        addMenuButtonRow(lmain, R.string.gosysclock, GO_SYSTEM_CLOCK,
                R.string.runnightclock, GO_NIGHT_CLOCK);
        addMenuButtonRow(lmain, R.string.confignightclock,
                CONFIGURE_NIGHT_CLOCK, 0, 0);

        addMenuSection(lmain, R.string.main_section_widgets);
        String m_widgetIds = m_prefs.getString("widgetIds", "");
        boolean hasExistingWidgets = m_widgetIds.length() > 0;
        int secondWidgetLabel = 0;
        int secondWidgetId = 0;
        if (widgetID != -1) {
            secondWidgetLabel = R.string.configwidget;
            secondWidgetId = CONFIGURE_THIS_WIDGET;
        } else if (hasExistingWidgets) {
            secondWidgetLabel = R.string.configoldwwidget;
            secondWidgetId = CONFIGURE_EXISTING_WIDGET;
        }
        addMenuButtonRow(lmain, R.string.confignewwidget,
                CONFIGURE_NEW_WIDGET, secondWidgetLabel, secondWidgetId);
        if (widgetID != -1 && hasExistingWidgets) {
            addMenuButtonRow(lmain, R.string.configoldwwidget,
                    CONFIGURE_EXISTING_WIDGET, 0, 0);
        }

        addMenuSection(lmain, R.string.main_section_settings);
        addMenuButtonRow(lmain, R.string.global_settings_button,
                GLOBAL_SETTINGS, R.string.settings_manager, MANAGE_SETTINGS);
        addMenuButtonRow(lmain, R.string.export_settings, EXPORT_SETTINGS,
                R.string.import_settings, IMPORT_SETTINGS);

        addMenuSection(lmain, R.string.main_section_app);
        addMenuButtonRow(lmain, R.string.exit, GO_EXIT, 0, 0);

        lscroll.addView(lmain);
        m_topLayout.addView(lscroll);
    }

    private void addMenuSection(LinearLayout parent, int titleResource) {
        TextView title = new TextView(this);
        title.setText(titleResource);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        int horizontalPadding = Math.round(8 * m_density);
        title.setPadding(horizontalPadding, Math.round(12 * m_density),
                horizontalPadding, Math.round(2 * m_density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Math.round(4 * m_density);
        parent.addView(title, params);
    }

    private void addMenuButtonRow(LinearLayout parent,
                                  int firstLabel, int firstId,
                                  int secondLabel, int secondId) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        parent.addView(row, rowParams);

        boolean hasSecondButton = secondId != 0;
        addMenuButton(row, firstLabel, firstId, !hasSecondButton);
        if (hasSecondButton) {
            addMenuButton(row, secondLabel, secondId, false);
        }
    }

    private void addMenuButton(LinearLayout row, int label, int id,
                               boolean fullWidth) {
        Button button = new Button(this);
        button.setText(label);
        button.setId(id);
        button.setAllCaps(false);
        button.setOnClickListener(this);
        button.setOnLongClickListener(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                fullWidth ? ViewGroup.LayoutParams.MATCH_PARENT : 0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                fullWidth ? 0.0F : 1.0F);
        int gap = Math.round(4 * m_density);
        params.setMargins(gap, 0, gap, 0);
        row.addView(button, params);
    }
}
