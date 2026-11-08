/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** The old-style, dynamically laid-out screen for backup and preset transfers. */
public class SettingsManagerActivity extends Activity_common
        implements View.OnLongClickListener {
    static final String EXTRA_START_ACTION = "settingsStartAction";
    static final String ACTION_MANAGE = "manage";
    static final String ACTION_EXPORT = "export";
    static final String ACTION_IMPORT = "import";

    private static final int REQUEST_CREATE_BACKUP = 801;
    private static final int REQUEST_OPEN_BACKUP = 802;
    private static final String UPDATE_WIDGET_ACTION =
            "uk.co.yahoo.p1rpp.UpdateWidget";

    private String initialAction = ACTION_MANAGE;
    private boolean pickerStarted;
    private int restoredSourceKind;
    private String restoredSourceId;
    private int restoredDestinationKind;
    private String restoredDestinationId;
    private Selection sourceSelection;
    private Selection destinationSelection;
    private TextView statusText;
    private Button clearButton;
    private Button confirmButton;
    private Button newPresetButton;
    private LinearLayout currentRows;
    private LinearLayout presetRows;
    private List<SettingsPreset> presets = new ArrayList<>();

    private static final int HELP_ID = 12001;
    private static final int SYSTEM_CLOCK_HELP_ID = HELP_ID + 3;
    private static final int SYSTEM_CLOCK_OVERRIDE_ID = HELP_ID + 4;
    private static final int SYSTEM_CLOCK_RESET_ID = HELP_ID + 5;

    private static final class Selection {
        static final int COMPONENT = 1;
        static final int PRESET = 2;
        static final int NEW_PRESET = 3;

        final int kind;
        final String id;
        final String label;
        final String type;
        final String source;
        final int widgetId;
        final Map<String, Object> values;

        Selection(int kind, String id, String label, String type,
                  String source, int widgetId, Map<String, Object> values) {
            this.kind = kind;
            this.id = id;
            this.label = label;
            this.type = type;
            this.source = source;
            this.widgetId = widgetId;
            this.values = values;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_key = "";
        if (savedInstanceState != null) {
            pickerStarted = savedInstanceState.getBoolean("pickerStarted", false);
            initialAction = savedInstanceState.getString(
                    EXTRA_START_ACTION, ACTION_MANAGE);
            restoredSourceKind = savedInstanceState.getInt("sourceKind");
            restoredSourceId = savedInstanceState.getString("sourceId");
            restoredDestinationKind = savedInstanceState.getInt("destinationKind");
            restoredDestinationId = savedInstanceState.getString("destinationId");
        } else {
            initialAction = getIntent().getStringExtra(
                    EXTRA_START_ACTION) == null ? ACTION_MANAGE
                    : getIntent().getStringExtra(EXTRA_START_ACTION);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("pickerStarted", pickerStarted);
        outState.putString(EXTRA_START_ACTION, initialAction);
        if (sourceSelection != null) {
            outState.putInt("sourceKind", sourceSelection.kind);
            outState.putString("sourceId", sourceSelection.id);
        }
        if (destinationSelection != null) {
            outState.putInt("destinationKind", destinationSelection.kind);
            outState.putString("destinationId", destinationSelection.id);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void resume() {
        m_key = "";
        super.resume();
        removeAllViews(m_topLayout);
        loadPresets();
        restoreSelection();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(5), dp(2), dp(5), dp(3));

        TextView help = new TextView(this);
        help.setId(HELP_ID);
        help.setText(R.string.settings_manager_help);
        help.setGravity(Gravity.CENTER_HORIZONTAL);
        help.setOnLongClickListener(this);
        page.addView(help, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout panels = new LinearLayout(this);
        panels.setOrientation(m_orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        if (m_orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            panels.addView(makeCurrentPanel(), new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0F));
            panels.addView(makePresetPanel(), new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0F));
        } else {
            panels.addView(makeCurrentPanel(), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0F));
            panels.addView(makePresetPanel(), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0F));
        }
        page.addView(panels, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0F));

        page.addView(makeFooter(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        m_topLayout.addView(page);

        if (!pickerStarted && !ACTION_MANAGE.equals(initialAction)) {
            pickerStarted = true;
            m_topLayout.post(() -> launchPicker(initialAction));
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view.getId() == HELP_ID) {
            doToast(R.string.settings_manager_help);
            return true;
        }
        if (view.getId() == SYSTEM_CLOCK_HELP_ID
                || view.getId() == SYSTEM_CLOCK_OVERRIDE_ID
                || view.getId() == SYSTEM_CLOCK_RESET_ID) {
            doToast(R.string.system_clock_settings_help);
            return true;
        }
        return false;
    }

    private void addSystemClockControls(LinearLayout parent) {
        TextView heading = centredLabel(
                R.string.system_clock_settings_title, SYSTEM_CLOCK_HELP_ID);
        parent.addView(heading, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

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
        parent.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button override = new Button(this);
        override.setText(R.string.system_clock_override_button);
        override.setId(SYSTEM_CLOCK_OVERRIDE_ID);
        override.setAllCaps(false);
        override.setOnClickListener(view -> {
            try {
                SystemClockApp.openSelector(this, false);
            } catch (ActivityNotFoundException exception) {
                doToast(R.string.system_clock_selector_not_found);
            }
        });
        override.setOnLongClickListener(this);
        parent.addView(override, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button reset = new Button(this);
        reset.setText(R.string.system_clock_reset_button);
        reset.setId(SYSTEM_CLOCK_RESET_ID);
        reset.setAllCaps(false);
        reset.setEnabled(m_prefs.contains(SystemClockApp.OVERRIDE_KEY)
                || m_prefs.getBoolean(SystemClockApp.DONT_ASK_KEY, false));
        reset.setOnClickListener(view -> confirmResetSystemClockSettings());
        reset.setOnLongClickListener(this);
        parent.addView(reset, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
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

    private LinearLayout makeCurrentPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(3), dp(2), dp(3), dp(2));
        panel.addView(centredLabel(R.string.current_settings, HELP_ID + 1));

        ScrollView scroll = makeScrollView();
        currentRows = new LinearLayout(this);
        currentRows.setOrientation(LinearLayout.VERTICAL);
        addComponentRow(currentRows, globalsSelection(), false);
        addSystemClockControls(currentRows);
        addComponentRow(currentRows, clockSelection(), false);

        List<SettingsStore.WidgetRecord> widgets = SettingsStore.activeWidgets(
                m_prefs, appWidgetManager, secondsClockWidget);
        for (int i = 0; i < widgets.size(); ++i) {
            SettingsStore.WidgetRecord widget = widgets.get(i);
            String label = widget.name.isEmpty()
                    ? getString(R.string.unnamed_widget, i + 1) : widget.name;
            addComponentRow(currentRows, widgetSelection(widget, label), true);
        }
        scroll.addView(currentRows);
        panel.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0F));
        return panel;
    }

    private LinearLayout makePresetPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(3), dp(2), dp(3), dp(2));
        panel.addView(centredLabel(R.string.presets, HELP_ID + 2));

        ScrollView scroll = makeScrollView();
        presetRows = new LinearLayout(this);
        presetRows.setOrientation(LinearLayout.VERTICAL);
        if (presets.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_presets);
            empty.setPadding(dp(8), dp(8), dp(8), dp(8));
            presetRows.addView(empty);
        } else {
            List<SettingsPreset> ordered = new ArrayList<>(presets);
            Collections.sort(ordered, Comparator.comparing(
                    preset -> preset.name.toLowerCase(java.util.Locale.ROOT)));
            for (SettingsPreset preset : ordered) {
                addPresetRow(presetRows, preset);
            }
        }
        scroll.addView(presetRows);
        panel.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0F));

        newPresetButton = new Button(this);
        newPresetButton.setText(R.string.new_preset);
        newPresetButton.setAllCaps(false);
        newPresetButton.setEnabled(sourceSelection != null
                && destinationSelection == null);
        newPresetButton.setOnClickListener(v -> promptForNewPreset());
        panel.addView(newPresetButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return panel;
    }

    private LinearLayout makeFooter() {
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        statusText = new TextView(this);
        statusText.setText(statusMessage());
        statusText.setPadding(dp(4), dp(5), dp(4), dp(5));
        footer.addView(statusText, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0F));

        clearButton = new Button(this);
        clearButton.setText("×");
        clearButton.setContentDescription(getString(R.string.clear_selection));
        clearButton.setAllCaps(false);
        clearButton.setVisibility(sourceSelection == null
                ? View.INVISIBLE : View.VISIBLE);
        clearButton.setOnClickListener(v -> {
            sourceSelection = null;
            destinationSelection = null;
            renderAgain();
        });
        footer.addView(clearButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        confirmButton = new Button(this);
        confirmButton.setText("✓");
        confirmButton.setContentDescription(getString(R.string.confirm_transfer));
        confirmButton.setAllCaps(false);
        confirmButton.setVisibility(destinationSelection == null
                ? View.INVISIBLE : View.VISIBLE);
        confirmButton.setOnClickListener(v -> executeTransfer());
        footer.addView(confirmButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return footer;
    }

    private ScrollView makeScrollView() {
        ScrollView scroll = new ScrollView(this);
        scroll.setScrollbarFadingEnabled(false);
        return scroll;
    }

    private void addComponentRow(LinearLayout parent,
                                 Selection selection, boolean isWidget) {
        boolean selected = isSelected(selection);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        CheckBox check = new CheckBox(this);
        check.setText(selection.label);
        check.setAllCaps(false);
        check.setChecked(selected);
        check.setOnClickListener(v -> onItemTapped(selection));
        row.addView(check, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0F));
        row.setOnClickListener(v -> onItemTapped(selection));

        if (isWidget) {
            Button rename = smallButton(R.string.rename,
                    v -> promptForWidgetName(selection.widgetId));
            row.addView(rename);
        }
        Button reset = smallButton(R.string.reset,
                v -> resetComponent(selection));
        row.addView(reset);
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addPresetRow(LinearLayout parent, SettingsPreset preset) {
        Selection selection = presetSelection(preset);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        CheckBox check = new CheckBox(this);
        check.setText(preset.name + " · " + getString(
                preset.type.equals(SettingsPreset.GLOBALS)
                        ? R.string.preset_type_globals
                        : R.string.preset_type_display));
        check.setAllCaps(false);
        check.setChecked(isSelected(selection));
        check.setOnClickListener(v -> onItemTapped(selection));
        row.addView(check, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0F));
        row.setOnClickListener(v -> onItemTapped(selection));
        row.addView(smallButton(R.string.rename,
                v -> promptForPresetName(preset)));
        row.addView(smallButton(R.string.delete,
                v -> confirmDeletePreset(preset)));
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private Button smallButton(int label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(12.0F);
        button.setMinWidth(0);
        button.setPadding(dp(3), 0, dp(3), 0);
        button.setOnClickListener(listener);
        return button;
    }

    private Selection globalsSelection() {
        return new Selection(Selection.COMPONENT, "globals",
                getString(R.string.global_settings), SettingsPreset.GLOBALS,
                SettingsTransferRules.GLOBALS, -1,
                SettingsStore.globalValues(m_prefs.getAll()));
    }

    private Selection clockSelection() {
        return new Selection(Selection.COMPONENT, "clock",
                getString(R.string.fullscreen_clock), SettingsPreset.DISPLAY,
                SettingsTransferRules.CLOCK, -1,
                SettingsStore.clockValues(m_prefs.getAll()));
    }

    private Selection widgetSelection(SettingsStore.WidgetRecord widget,
                                      String label) {
        return new Selection(Selection.COMPONENT,
                "widget:" + widget.id, label, SettingsPreset.DISPLAY,
                SettingsTransferRules.WIDGET, widget.id, widget.values);
    }

    private Selection presetSelection(SettingsPreset preset) {
        return new Selection(Selection.PRESET, preset.id, preset.name,
                preset.type, preset.source, -1,
                new java.util.LinkedHashMap<>(preset.values));
    }

    private void onItemTapped(Selection item) {
        if (sourceSelection == null) {
            if (item.kind == Selection.NEW_PRESET) return;
            sourceSelection = item;
        } else if (sameSelection(sourceSelection, item)
                || !canTransfer(sourceSelection, item)) {
            doToast(R.string.source_target_incompatible);
            // A CheckBox toggles itself before its click listener runs. Rebuild
            // so an incompatible tap never appears selected.
            renderAgain();
            return;
        } else {
            // Allow changing the destination before confirming the transfer.
            destinationSelection = item;
        }
        renderAgain();
    }

    private boolean canTransfer(Selection source, Selection destination) {
        if (destination.kind == Selection.PRESET) {
            return source.type.equals(destination.type);
        }
        if (destination.kind != Selection.COMPONENT) return false;
        if (source.type.equals(SettingsPreset.GLOBALS)) {
            return destination.source.equals(SettingsTransferRules.GLOBALS);
        }
        return source.type.equals(SettingsPreset.DISPLAY)
                && (destination.source.equals(SettingsTransferRules.CLOCK)
                    || destination.source.equals(SettingsTransferRules.WIDGET));
    }

    private boolean sameSelection(Selection first, Selection second) {
        return first.kind == second.kind && first.id.equals(second.id);
    }

    private boolean isSelected(Selection item) {
        return (sourceSelection != null && sameSelection(sourceSelection, item))
                || (destinationSelection != null
                    && sameSelection(destinationSelection, item));
    }

    private String statusMessage() {
        if (sourceSelection == null) return getString(R.string.tap_what_to_copy);
        if (destinationSelection == null) return getString(R.string.tap_where_to_copy);
        return getString(R.string.confirm_copy,
                sourceSelection.label, destinationSelection.label);
    }

    private void executeTransfer() {
        if (sourceSelection == null || destinationSelection == null) return;
        try {
            if (destinationSelection.kind == Selection.PRESET) {
                SettingsPreset target = SettingsStore.findPreset(
                        presets, destinationSelection.id);
                if (target == null) return;
                SettingsPreset replacement = new SettingsPreset(
                        java.util.UUID.randomUUID().toString(), target.name,
                        sourceSelection.type, sourceSelection.source,
                        sourceSelection.values);
                SettingsStore.AddResult added = SettingsStore.addPreset(
                        presets, replacement);
                if (added.duplicate != null) {
                    showDuplicateMessage(replacement.name, added.duplicate.name);
                    return;
                }
                SettingsStore.writePresets(m_prefs, presets);
            } else {
                SettingsPreset source = new SettingsPreset(
                        java.util.UUID.randomUUID().toString(),
                        sourceSelection.label, sourceSelection.type,
                        sourceSelection.source, sourceSelection.values);
                String target = destinationSelection.source;
                SettingsStore.applyPresetToComponent(m_prefs, presets,
                        source, target, destinationSelection.widgetId);
                if (target.equals(SettingsTransferRules.WIDGET)) updateWidgets();
            }
            sourceSelection = null;
            destinationSelection = null;
            loadPresets();
            renderAgain();
            doToast(R.string.copy_complete);
        } catch (JSONException exception) {
            doToast(getString(R.string.file_load_failed, exception.getMessage()));
        }
    }

    private void promptForNewPreset() {
        if (sourceSelection == null || destinationSelection != null) return;
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint(R.string.preset_name_hint);
        name.setText(SettingsStore.suggestedPresetName(
                sourceSelection.source, sourceSelection.label));
        new AlertDialog.Builder(this)
                .setTitle(R.string.new_preset)
                .setView(name)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String presetName = name.getText().toString().trim();
                    if (presetName.isEmpty()) {
                        doToast(R.string.preset_name_required);
                        return;
                    }
                    SettingsPreset candidate = new SettingsPreset(
                            java.util.UUID.randomUUID().toString(), presetName,
                            sourceSelection.type, sourceSelection.source,
                            sourceSelection.values);
                    SettingsStore.AddResult added = SettingsStore.addPreset(
                            presets, candidate);
                    if (added.duplicate != null) {
                        showDuplicateMessage(candidate.name, added.duplicate.name);
                    } else {
                        sourceSelection = null;
                        destinationSelection = null;
                        savePresetsAndRefresh();
                        doToast(R.string.preset_saved);
                    }
                }).show();
    }

    private void promptForWidgetName(int widgetId) {
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint(R.string.widget_name_hint);
        name.setText(SettingsStore.widgetName(m_prefs, widgetId));
        name.setSelection(0, name.length());
        new AlertDialog.Builder(this)
                .setTitle(R.string.rename_widget)
                .setView(name)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String newName = name.getText().toString().trim();
                    if (!SettingsStore.isWidgetNameAvailable(m_prefs,
                            appWidgetManager, secondsClockWidget,
                            widgetId, newName)) {
                        doToast(R.string.widget_name_in_use);
                        return;
                    }
                    SettingsStore.renameWidget(m_prefs, widgetId, newName);
                    sourceSelection = null;
                    destinationSelection = null;
                    renderAgain();
                }).show();
    }

    private void promptForPresetName(SettingsPreset preset) {
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint(R.string.preset_name_hint);
        name.setText(preset.name);
        new AlertDialog.Builder(this)
                .setTitle(R.string.rename)
                .setView(name)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (SettingsStore.renamePreset(presets, preset.id,
                            name.getText().toString())) {
                        sourceSelection = null;
                        destinationSelection = null;
                        savePresetsAndRefresh();
                    }
                }).show();
    }

    private void confirmDeletePreset(SettingsPreset preset) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_preset)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    SettingsStore.removePreset(presets, preset.id);
                    if (sourceSelection != null
                            && sourceSelection.kind == Selection.PRESET
                            && sourceSelection.id.equals(preset.id)) {
                        sourceSelection = null;
                        destinationSelection = null;
                    }
                    if (destinationSelection != null
                            && destinationSelection.kind == Selection.PRESET
                            && destinationSelection.id.equals(preset.id)) {
                        destinationSelection = null;
                    }
                    savePresetsAndRefresh();
                    doToast(R.string.preset_deleted);
                }).show();
    }

    private void resetComponent(Selection component) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.reset_component)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.reset, (dialog, which) -> {
                    resetComponentNow(component);
                }).show();
    }

    private void resetComponentNow(Selection component) {
        try {
            switch (component.source) {
                case SettingsTransferRules.GLOBALS:
                    SettingsStore.resetGlobals(m_prefs, presets);
                    break;
                case SettingsTransferRules.CLOCK:
                    SettingsStore.resetClock(m_prefs, presets);
                    break;
                case SettingsTransferRules.WIDGET:
                    String widgetName = SettingsStore.widgetName(
                            m_prefs, component.widgetId);
                    SettingsStore.resetWidget(m_prefs, presets,
                            component.widgetId,
                            widgetName.isEmpty() ? component.label : widgetName);
                    updateWidgets();
                    break;
            }
            sourceSelection = null;
            destinationSelection = null;
            loadPresets();
            renderAgain();
            doToast(R.string.component_reset);
        } catch (JSONException exception) {
            doToast(getString(R.string.file_load_failed, exception.getMessage()));
        }
    }

    private void savePresetsAndRefresh() {
        try {
            SettingsStore.writePresets(m_prefs, presets);
            loadPresets();
            renderAgain();
        } catch (JSONException exception) {
            doToast(getString(R.string.file_load_failed, exception.getMessage()));
        }
    }

    private void showDuplicateMessage(String incoming, String existing) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.duplicate_presets_title)
                .setMessage(getString(R.string.duplicate_preset_report,
                        incoming, existing))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void loadPresets() {
        try {
            presets = SettingsStore.readPresets(m_prefs);
        } catch (JSONException exception) {
            presets = new ArrayList<>();
            doToast(getString(R.string.file_load_failed, exception.getMessage()));
        }
    }

    private void restoreSelection() {
        if (sourceSelection == null && restoredSourceId != null) {
            sourceSelection = selectionForId(restoredSourceKind, restoredSourceId);
        }
        if (destinationSelection == null && restoredDestinationId != null) {
            destinationSelection = selectionForId(
                    restoredDestinationKind, restoredDestinationId);
        }
        restoredSourceId = null;
        restoredDestinationId = null;
    }

    private Selection selectionForId(int kind, String id) {
        if (kind == Selection.PRESET) {
            SettingsPreset preset = SettingsStore.findPreset(presets, id);
            return preset == null ? null : presetSelection(preset);
        }
        if (kind != Selection.COMPONENT) return null;
        if (id.equals("globals")) return globalsSelection();
        if (id.equals("clock")) return clockSelection();
        if (!id.startsWith("widget:")) return null;
        try {
            int widgetId = Integer.parseInt(id.substring("widget:".length()));
            List<SettingsStore.WidgetRecord> widgets = SettingsStore.activeWidgets(
                    m_prefs, appWidgetManager, secondsClockWidget);
            for (int i = 0; i < widgets.size(); ++i) {
                SettingsStore.WidgetRecord widget = widgets.get(i);
                if (widget.id != widgetId) continue;
                String label = widget.name.isEmpty()
                        ? getString(R.string.unnamed_widget, i + 1) : widget.name;
                return widgetSelection(widget, label);
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }

    private void renderAgain() {
        if (m_width > 0 && m_height > 0) resume();
    }

    private void launchPicker(String action) {
        Intent intent;
        if (ACTION_EXPORT.equals(action)) {
            intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.backup_file_name));
            try {
                startActivityForResult(intent, REQUEST_CREATE_BACKUP);
            } catch (ActivityNotFoundException exception) {
                pickerStarted = false;
                doToast(R.string.file_save_failed);
            }
        } else if (ACTION_IMPORT.equals(action)) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            try {
                startActivityForResult(intent, REQUEST_OPEN_BACKUP);
            } catch (ActivityNotFoundException exception) {
                pickerStarted = false;
                doToast(getString(R.string.file_load_failed,
                        exception.getMessage()));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_CREATE_BACKUP) exportBackup(uri);
        else if (requestCode == REQUEST_OPEN_BACKUP) importBackup(uri);
    }

    private void exportBackup(Uri uri) {
        try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
            if (stream == null) throw new IOException("Could not open the selected file");
            JSONObject backup = SettingsStore.createBackup(
                    m_prefs, appWidgetManager, secondsClockWidget);
            SettingsStore.writeBackup(stream, backup);
            doToast(R.string.file_exported);
        } catch (Exception exception) {
            doToast(R.string.file_save_failed);
        }
    }

    private void importBackup(Uri uri) {
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new IOException("Could not open the selected file");
            JSONObject backup = SettingsStore.readBackup(stream);
            SettingsStore.ImportResult result = SettingsStore.importBackup(
                    m_prefs, appWidgetManager, secondsClockWidget, backup);
            if (result.appliedWidgets > 0) updateWidgets();
            loadPresets();
            renderAgain();
            doToast(getString(R.string.file_imported,
                    result.appliedWidgets, result.storedUnmatchedWidgets));
            if (!result.duplicates.isEmpty()) {
                showDuplicateMessages(result.duplicates);
            }
        } catch (Exception exception) {
            doToast(getString(R.string.file_load_failed,
                    exception.getMessage() == null
                            ? exception.getClass().getSimpleName()
                            : exception.getMessage()));
        }
    }

    private void showDuplicateMessages(
            List<SettingsStore.DuplicatePreset> duplicates) {
        StringBuilder text = new StringBuilder();
        for (SettingsStore.DuplicatePreset duplicate : duplicates) {
            if (text.length() > 0) text.append('\n');
            text.append(getString(R.string.duplicate_preset_report,
                    duplicate.incomingName, duplicate.existingName));
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.duplicate_presets_title)
                .setMessage(text.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void updateWidgets() {
        int[] ids = appWidgetManager.getAppWidgetIds(secondsClockWidget);
        if (ids.length == 0) return;
        Intent intent = new Intent(UPDATE_WIDGET_ACTION);
        intent.setClass(this, SecondsClockWidget.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5F);
    }
}
