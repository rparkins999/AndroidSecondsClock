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
        addMenuButtonGroup(lmain,
                new int[] {
                    R.string.gosysclock,
                    R.string.runnightclock,
                    R.string.confignightclock
                },
                new int[] {
                    GO_SYSTEM_CLOCK,
                    GO_NIGHT_CLOCK,
                    CONFIGURE_NIGHT_CLOCK
                }, false);

        addMenuSection(lmain, R.string.main_section_widgets);
        String m_widgetIds = m_prefs.getString("widgetIds", "");
        boolean hasExistingWidgets = m_widgetIds.length() > 0;
        int widgetButtonCount = 1 + (hasExistingWidgets ? 1 : 0)
                + (widgetID != -1 ? 1 : 0);
        int[] widgetLabels = new int[widgetButtonCount];
        int[] widgetIds = new int[widgetButtonCount];
        int widgetButtonIndex = 0;
        widgetLabels[widgetButtonIndex] = R.string.confignewwidget;
        widgetIds[widgetButtonIndex++] = CONFIGURE_NEW_WIDGET;
        if (hasExistingWidgets) {
            widgetLabels[widgetButtonIndex] = R.string.configoldwwidget;
            widgetIds[widgetButtonIndex++] = CONFIGURE_EXISTING_WIDGET;
        }
        if (widgetID != -1) {
            widgetLabels[widgetButtonIndex] = R.string.configwidget;
            widgetIds[widgetButtonIndex] = CONFIGURE_THIS_WIDGET;
        }
        addMenuButtonGroup(lmain, widgetLabels, widgetIds, true);

        addMenuSection(lmain, R.string.main_section_settings);
        addMenuButtonGroup(lmain,
                new int[] {
                    R.string.export_settings,
                    R.string.import_settings,
                    R.string.global_settings_button,
                    R.string.settings_manager
                },
                new int[] {
                    EXPORT_SETTINGS,
                    IMPORT_SETTINGS,
                    GLOBAL_SETTINGS,
                    MANAGE_SETTINGS
                }, false);

        addMenuSection(lmain, R.string.main_section_app);
        addMenuButtonGroup(lmain, new int[] { R.string.exit },
                new int[] { GO_EXIT }, false);

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

    private void addMenuButtonGroup(LinearLayout parent,
                                   int[] labels, int[] ids,
                                   boolean reverseFill) {
        if (labels.length != ids.length) {
            throw new IllegalArgumentException("Menu labels and IDs must match");
        }
        Button[] buttons = new Button[labels.length];
        for (int i = 0; i < labels.length; ++i) {
            buttons[i] = new Button(this);
            buttons[i].setText(labels[i]);
            buttons[i].setId(ids[i]);
            buttons[i].setAllCaps(false);
            buttons[i].setOnClickListener(this);
            buttons[i].setOnLongClickListener(this);
        }
        MenuButtonGroup group = new MenuButtonGroup(buttons, reverseFill);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        parent.addView(group, params);
    }

    private final class MenuButtonGroup extends LinearLayout {
        private final Button[] buttons;
        private final boolean reverseFill;
        private final int gap;
        private int formattedWidth;

        MenuButtonGroup(Button[] buttons, boolean reverseFill) {
            super(MainActivity.this);
            this.buttons = buttons;
            this.reverseFill = reverseFill;
            this.gap = Math.round(4 * m_density);
            setOrientation(LinearLayout.VERTICAL);
            setBaselineAligned(false);
            formattedWidth = -1;
            addOnLayoutChangeListener((view, left, top, right, bottom,
                                       oldLeft, oldTop, oldRight, oldBottom) -> {
                int width = right - left;
                if (width > 0 && width != formattedWidth) {
                    formatButtons(width);
                    formattedWidth = width;
                }
            });
        }

        private void formatButtons(int width) {
            int[] naturalWidths = new int[buttons.length];
            int unspecified = View.MeasureSpec.makeMeasureSpec(
                    0, View.MeasureSpec.UNSPECIFIED);
            for (int i = 0; i < buttons.length; ++i) {
                buttons[i].measure(unspecified, unspecified);
                naturalWidths[i] = buttons[i].getMeasuredWidth();
            }
            int[][] rows = MenuButtonLayoutFormatter.format(
                    naturalWidths, width, gap, reverseFill);

            for (Button button : buttons) {
                if (button.getParent() instanceof ViewGroup) {
                    ((ViewGroup) button.getParent()).removeView(button);
                }
            }
            removeAllViews();
            for (int[] rowButtons : rows) {
                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setBaselineAligned(false);
                addView(row, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                for (int buttonIndex : rowButtons) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0F);
                    params.setMargins(gap, 0, gap, 0);
                    row.addView(buttons[buttonIndex], params);
                }
            }
        }
    }
}
