/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Reads, writes, exports and applies portable SecondsClock settings. */
final class SettingsStore {
    static final String BACKUP_FORMAT = "uk.co.yahoo.p1rpp.secondsclock.settings";
    static final int BACKUP_VERSION = 1;
    static final String PRESETS_KEY = "settingsPresets";

    private static final String[] WIDGET_SETTING_SUFFIXES = {
            "bgcolour", "fgcolour", "touchaction", "formatSyncEnabled",
            "textStyleEmoji", "advancedFormatMode", "showTime",
            "showWeekDay", "showShortDate", "showMonthDay", "showMonth",
            "showYear", "showNextAlarm", "format", "formatHorizontal",
            "formatVertical", "formatProfile"};
    private static final String[] CLOCK_SETTING_SUFFIXES = {
            "showWeekDay", "showShortDate", "showMonthDay", "showMonth",
            "showYear", "secondsSize", "advancedFormatMode", "format",
            "formatPortrait", "formatLandscape", "formatSyncEnabled",
            "formatProfile", "textStyleEmoji", "fgcolour", "minBright",
            "alpha", "threshold", "onlyalpha", "7seg", "forcevertical",
            "showNextAlarm", "advancedLargeTime"};
    private static final String[] STRING_SETTINGS = {
            "format", "formatHorizontal", "formatVertical",
            "formatPortrait", "formatLandscape"};
    private static final String[] BOOLEAN_SETTINGS = {
            "formatSyncEnabled", "textStyleEmoji", "advancedFormatMode",
            "showNextAlarm", "7seg", "forcevertical", "advancedLargeTime"};

    private SettingsStore() {}

    static final class WidgetRecord {
        final int id;
        final String name;
        final Map<String, Object> values;

        WidgetRecord(int id, String name, Map<String, Object> values) {
            this.id = id;
            this.name = name;
            this.values = values;
        }
    }

    static final class AddResult {
        final SettingsPreset added;
        final SettingsPreset duplicate;

        AddResult(SettingsPreset added, SettingsPreset duplicate) {
            this.added = added;
            this.duplicate = duplicate;
        }
    }

    static final class ImportResult {
        int appliedWidgets;
        int storedUnmatchedWidgets;
        final List<DuplicatePreset> duplicates = new ArrayList<>();
    }

    static final class DuplicatePreset {
        final String incomingName;
        final String existingName;

        DuplicatePreset(String incomingName, String existingName) {
            this.incomingName = incomingName;
            this.existingName = existingName;
        }
    }

    static List<WidgetRecord> activeWidgets(
            SharedPreferences prefs, AppWidgetManager manager,
            ComponentName widgetComponent) {
        int[] ids = manager.getAppWidgetIds(widgetComponent);
        Arrays.sort(ids);
        List<WidgetRecord> result = new ArrayList<>();
        for (int id : ids) {
            result.add(new WidgetRecord(id, widgetName(prefs, id),
                    widgetValues(prefs.getAll(), id)));
        }
        return result;
    }

    static String widgetName(SharedPreferences prefs, int widgetId) {
        try {
            String name = prefs.getString(widgetPrefix(widgetId) + "name", "");
            return name == null ? "" : name.trim();
        } catch (ClassCastException ignored) {
            return "";
        }
    }

    static boolean isWidgetNameAvailable(
            SharedPreferences prefs, AppWidgetManager manager,
            ComponentName widgetComponent, int widgetId, String candidate) {
        String normalized = normalizeName(candidate);
        if (normalized.isEmpty()) return true;
        for (int otherId : manager.getAppWidgetIds(widgetComponent)) {
            if (otherId == widgetId) continue;
            if (normalized.equals(normalizeName(widgetName(prefs, otherId)))) {
                return false;
            }
        }
        return true;
    }

    static void renameWidget(SharedPreferences prefs, int widgetId, String name) {
        SharedPreferences.Editor editor = prefs.edit();
        String key = widgetPrefix(widgetId) + "name";
        if (name == null || name.trim().isEmpty()) editor.remove(key);
        else editor.putString(key, name.trim());
        editor.apply();
    }

    static List<SettingsPreset> readPresets(SharedPreferences prefs)
            throws JSONException {
        String stored = prefs.getString(PRESETS_KEY, "[]");
        JSONArray array = new JSONArray(stored);
        List<SettingsPreset> result = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            result.add(readPreset(array.getJSONObject(i)));
        }
        return result;
    }

    static void writePresets(SharedPreferences prefs,
                             Collection<SettingsPreset> presets)
            throws JSONException {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PRESETS_KEY, presetsToJson(presets).toString());
        editor.apply();
    }

    static JSONObject createBackup(SharedPreferences prefs,
                                   AppWidgetManager manager,
                                   ComponentName widgetComponent)
            throws JSONException {
        Map<String, ?> all = prefs.getAll();
        JSONObject backup = new JSONObject();
        backup.put("format", BACKUP_FORMAT);
        backup.put("version", BACKUP_VERSION);

        JSONObject active = new JSONObject();
        active.put("globals", settingsJson(globalValues(all)));
        active.put("clock", settingsJson(clockValues(all)));
        JSONArray widgets = new JSONArray();
        for (WidgetRecord widget : activeWidgets(prefs, manager, widgetComponent)) {
            JSONObject record = new JSONObject();
            record.put("name", widget.name);
            record.put("values", settingsJson(widget.values));
            widgets.put(record);
        }
        active.put("widgets", widgets);
        backup.put("active", active);
        backup.put("presets", presetsToJson(readPresets(prefs)));
        return backup;
    }

    static void writeBackup(OutputStream stream, JSONObject backup)
            throws IOException, JSONException {
        Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
        writer.write(backup.toString(2));
        writer.flush();
    }

    static JSONObject readBackup(InputStream stream) throws IOException, JSONException {
        StringBuilder json = new StringBuilder();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        char[] buffer = new char[4096];
        int count;
        int total = 0;
        while ((count = reader.read(buffer)) != -1) {
            total += count;
            if (total > 5 * 1024 * 1024) {
                throw new IOException("Settings file is too large");
            }
            json.append(buffer, 0, count);
        }
        JSONObject result = new JSONObject(json.toString());
        validateBackup(result);
        return result;
    }

    static ImportResult importBackup(
            SharedPreferences prefs, AppWidgetManager manager,
            ComponentName widgetComponent, JSONObject backup)
            throws JSONException {
        // Decode and validate every imported section before staging any changes.
        validateBackup(backup);
        JSONObject active = backup.getJSONObject("active");
        Map<String, Object> importedGlobals = readComponentSettings(
                active.getJSONObject("globals"), SettingsTransferRules.GLOBALS);
        Map<String, Object> importedClock = readComponentSettings(
                active.getJSONObject("clock"), SettingsTransferRules.CLOCK);
        addWidgetDefaults(importedGlobals);
        addClockDefaults(importedClock);
        List<ImportedWidget> importedWidgets = readWidgets(
                active.getJSONArray("widgets"));
        List<SettingsPreset> importedPresets = new ArrayList<>();
        JSONArray presetArray = backup.getJSONArray("presets");
        for (int i = 0; i < presetArray.length(); ++i) {
            importedPresets.add(readPreset(presetArray.getJSONObject(i)));
        }

        List<SettingsPreset> presets = readPresets(prefs);
        List<WidgetRecord> currentWidgets = activeWidgets(
                prefs, manager, widgetComponent);
        Map<String, Integer> sourceNameCounts = new HashMap<>();
        for (ImportedWidget widget : importedWidgets) {
            String normalized = normalizeName(widget.name);
            if (!normalized.isEmpty()) {
                sourceNameCounts.put(normalized,
                        sourceNameCounts.containsKey(normalized)
                                ? sourceNameCounts.get(normalized) + 1 : 1);
            }
        }
        Map<String, Integer> targetNameCounts = new HashMap<>();
        for (WidgetRecord widget : currentWidgets) {
            String normalized = normalizeName(widget.name);
            if (!normalized.isEmpty()) {
                targetNameCounts.put(normalized,
                        targetNameCounts.containsKey(normalized)
                                ? targetNameCounts.get(normalized) + 1 : 1);
            }
        }
        Map<Integer, ImportedWidget> matchedWidgets = new LinkedHashMap<>();
        for (ImportedWidget incoming : importedWidgets) {
            String normalized = normalizeName(incoming.name);
            Integer sourceCount = sourceNameCounts.get(normalized);
            Integer targetCount = targetNameCounts.get(normalized);
            if (normalized.isEmpty() || sourceCount == null || sourceCount != 1
                    || targetCount == null || targetCount != 1) continue;
            for (WidgetRecord target : currentWidgets) {
                if (normalizeName(target.name).equals(normalized)) {
                    matchedWidgets.put(target.id, incoming);
                    break;
                }
            }
        }

        ImportResult result = new ImportResult();
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> oldValues = prefs.getAll();

        addOldSettingsPreset(presets, "GLOBALS", SettingsPreset.GLOBALS,
                SettingsTransferRules.GLOBALS, globalValues(oldValues));
        addOldSettingsPreset(presets, "Full-screen clock", SettingsPreset.DISPLAY,
                SettingsTransferRules.CLOCK, clockValues(oldValues));
        for (WidgetRecord widget : currentWidgets) {
            if (matchedWidgets.containsKey(widget.id)) {
                addOldSettingsPreset(presets,
                        widgetPresetBaseName(widget), SettingsPreset.DISPLAY,
                        SettingsTransferRules.WIDGET, widget.values);
            }
        }

        for (SettingsPreset preset : importedPresets) {
            addImportedPreset(presets, preset, result);
        }
        addImportedPreset(presets, new SettingsPreset(
                newId(), "GLOBALS", SettingsPreset.GLOBALS,
                SettingsTransferRules.GLOBALS, importedGlobals), result);
        addImportedPreset(presets, new SettingsPreset(
                newId(), "Full-screen clock", SettingsPreset.DISPLAY,
                SettingsTransferRules.CLOCK, importedClock), result);
        int unnamed = 1;
        for (ImportedWidget widget : importedWidgets) {
            String presetName = widget.name.trim();
            if (presetName.isEmpty()) {
                presetName = String.format(Locale.ROOT, "Imported widget %03d", unnamed++);
            }
            boolean added = addImportedPreset(presets, new SettingsPreset(
                    newId(), presetName, SettingsPreset.DISPLAY,
                    SettingsTransferRules.WIDGET, widget.values), result);
            boolean matched = false;
            for (ImportedWidget matchedWidget : matchedWidgets.values()) {
                if (matchedWidget == widget) {
                    matched = true;
                    break;
                }
            }
            if (!matched && added) ++result.storedUnmatchedWidgets;
        }

        replacePrefix(editor, "W", true);
        putValues(editor, "W", importedGlobals);
        replacePrefix(editor, "C", false);
        putValues(editor, "C", importedClock);
        for (Map.Entry<Integer, ImportedWidget> entry : matchedWidgets.entrySet()) {
            replaceWidgetValues(editor, entry.getKey(),
                    entry.getValue().values);
            ++result.appliedWidgets;
        }
        editor.putString(PRESETS_KEY, presetsToJson(presets).toString());
        if (!editor.commit()) throw new JSONException("Could not save imported settings");
        return result;
    }

    static AddResult addPreset(List<SettingsPreset> presets,
                               SettingsPreset candidate) {
        SettingsPreset duplicate = SettingsPresetRules.duplicateOf(candidate, presets);
        if (duplicate != null) return new AddResult(null, duplicate);
        archiveSameNamePresets(presets, candidate.name);
        String id = candidate.id.isEmpty() || hasPresetId(presets, candidate.id)
                ? newId() : candidate.id;
        SettingsPreset added = candidate.withId(id);
        presets.add(added);
        return new AddResult(added, null);
    }

    static SettingsPreset addOldSettingsPreset(
            List<SettingsPreset> presets, String baseName,
            String type, String source, Map<String, ?> values) {
        if (values.isEmpty()) return null;
        SettingsPreset candidate = new SettingsPreset(newId(),
                SettingsPresetRules.oldSettingsName(baseName, presetNames(presets)),
                type, source, values);
        SettingsPreset duplicate = SettingsPresetRules.duplicateOf(candidate, presets);
        if (duplicate != null) return duplicate;
        presets.add(candidate);
        return candidate;
    }

    static boolean renamePreset(List<SettingsPreset> presets,
                                String id, String requestedName) {
        String clean = requestedName == null ? "" : requestedName.trim();
        if (clean.isEmpty()) return false;
        for (int i = 0; i < presets.size(); ++i) {
            SettingsPreset preset = presets.get(i);
            if (!preset.id.equals(id)) continue;
            List<String> otherNames = new ArrayList<>(presetNames(presets));
            otherNames.remove(preset.name);
            String unique = SettingsPresetRules.availableName(clean, otherNames);
            presets.set(i, preset.withName(unique));
            return true;
        }
        return false;
    }

    static SettingsPreset findPreset(List<SettingsPreset> presets, String id) {
        for (SettingsPreset preset : presets) {
            if (preset.id.equals(id)) return preset;
        }
        return null;
    }

    static void removePreset(List<SettingsPreset> presets, String id) {
        for (int i = presets.size() - 1; i >= 0; --i) {
            if (presets.get(i).id.equals(id)) presets.remove(i);
        }
    }

    static Map<String, Object> globalValues(Map<String, ?> all) {
        return settingsForSuffixes(all, "W", WIDGET_SETTING_SUFFIXES);
    }

    static Map<String, Object> clockValues(Map<String, ?> all) {
        return settingsForSuffixes(all, "C", CLOCK_SETTING_SUFFIXES);
    }

    static Map<String, Object> widgetValues(Map<String, ?> all, int widgetId) {
        return settingsForSuffixes(
                all, widgetPrefix(widgetId), WIDGET_SETTING_SUFFIXES);
    }

    private static Map<String, Object> settingsForSuffixes(
            Map<String, ?> all, String prefix, String[] suffixes) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String suffix : suffixes) {
            Object value = all.get(prefix + suffix);
            if (isPortableValue(value)) {
                result.put(suffix, value);
            }
        }
        if (prefix.equals("C")) addClockDefaults(result);
        else addWidgetDefaults(result);
        return result;
    }

    private static void addWidgetDefaults(Map<String, Object> values) {
        putDefault(values, "bgcolour", 0x00000000);
        putDefault(values, "fgcolour", 0xFFFFFFFF);
        putDefault(values, "touchaction", 0);
        putDefault(values, "formatSyncEnabled", true);
        putDefault(values, "textStyleEmoji", false);
        putDefault(values, "advancedFormatMode", values.containsKey("format"));
        putDefault(values, "showTime", 2);
        putDefault(values, "showWeekDay", 2);
        putDefault(values, "showShortDate", 0);
        putDefault(values, "showMonthDay", 1);
        putDefault(values, "showMonth", 2);
        putDefault(values, "showYear", 1);
        putDefault(values, "showNextAlarm", false);
        putDefault(values, "formatProfile", 0);
    }

    private static void addClockDefaults(Map<String, Object> values) {
        putDefault(values, "showWeekDay", 2);
        putDefault(values, "showShortDate", 0);
        putDefault(values, "showMonthDay", 1);
        putDefault(values, "showMonth", 2);
        putDefault(values, "showYear", 1);
        putDefault(values, "secondsSize", 255);
        putDefault(values, "advancedFormatMode", false);
        putDefault(values, "formatSyncEnabled", true);
        putDefault(values, "formatProfile", 0);
        putDefault(values, "textStyleEmoji", false);
        putDefault(values, "fgcolour", 0xFFFFFFFF);
        // Preserve the effective defaults for these optional preferences too.
        putDefault(values, "onlyalpha", 0);
        putDefault(values, "7seg", false);
        putDefault(values, "forcevertical", false);
        putDefault(values, "showNextAlarm", false);
        putDefault(values, "advancedLargeTime", true);
    }

    private static void putDefault(Map<String, Object> values,
                                   String key, Object defaultValue) {
        if (!values.containsKey(key)) values.put(key, defaultValue);
    }

    static String widgetPrefix(int id) {
        return "W" + id;
    }

    static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    static void resetGlobals(SharedPreferences prefs,
                             List<SettingsPreset> presets)
            throws JSONException {
        Map<String, ?> all = prefs.getAll();
        addOldSettingsPreset(presets, "GLOBALS", SettingsPreset.GLOBALS,
                SettingsTransferRules.GLOBALS, globalValues(all));
        SharedPreferences.Editor editor = prefs.edit();
        removePrefix(editor, "W", true);
        savePresetsInEditor(editor, presets);
    }

    static void resetClock(SharedPreferences prefs,
                           List<SettingsPreset> presets)
            throws JSONException {
        Map<String, ?> all = prefs.getAll();
        addOldSettingsPreset(presets, "Full-screen clock", SettingsPreset.DISPLAY,
                SettingsTransferRules.CLOCK, clockValues(all));
        SharedPreferences.Editor editor = prefs.edit();
        removePrefix(editor, "C", false);
        savePresetsInEditor(editor, presets);
    }

    static void resetWidget(SharedPreferences prefs,
                            List<SettingsPreset> presets, int widgetId,
                            String widgetName) throws JSONException {
        Map<String, ?> all = prefs.getAll();
        Map<String, Object> defaults = globalValues(all);
        addOldSettingsPreset(presets,
                widgetName == null || widgetName.isEmpty() ? "Widget" : widgetName,
                SettingsPreset.DISPLAY, SettingsTransferRules.WIDGET,
                widgetValues(all, widgetId));
        SharedPreferences.Editor editor = prefs.edit();
        replaceWidgetValues(editor, widgetId, defaults);
        savePresetsInEditor(editor, presets);
    }

    static void applyPresetToComponent(
            SharedPreferences prefs, List<SettingsPreset> presets,
            SettingsPreset preset, String target, int widgetId)
            throws JSONException {
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> all = prefs.getAll();
        if (preset.type.equals(SettingsPreset.GLOBALS)
                && target.equals(SettingsTransferRules.GLOBALS)) {
            addOldSettingsPreset(presets, "GLOBALS", SettingsPreset.GLOBALS,
                    SettingsTransferRules.GLOBALS, globalValues(all));
            removePrefix(editor, "W", true);
            putValues(editor, "W", preset.values);
        } else if (preset.type.equals(SettingsPreset.DISPLAY)
                && target.equals(SettingsTransferRules.CLOCK)) {
            Map<String, Object> mapped = SettingsTransferRules.forTarget(
                    preset.source, SettingsTransferRules.CLOCK, preset.values);
            if (preset.source.equals(SettingsTransferRules.CLOCK)) {
                addOldSettingsPreset(presets, "Full-screen clock",
                        SettingsPreset.DISPLAY, SettingsTransferRules.CLOCK,
                        clockValues(all));
                removePrefix(editor, "C", false);
            } else {
                addOldSettingsPreset(presets, "Full-screen clock",
                        SettingsPreset.DISPLAY, SettingsTransferRules.CLOCK,
                        clockValues(all));
            }
            putValues(editor, "C", mapped);
        } else if (preset.type.equals(SettingsPreset.DISPLAY)
                && target.equals(SettingsTransferRules.WIDGET)) {
            Map<String, Object> oldWidget = widgetValues(all, widgetId);
            String name = widgetName(prefs, widgetId);
            addOldSettingsPreset(presets,
                    name.isEmpty() ? "Widget" : name,
                    SettingsPreset.DISPLAY, SettingsTransferRules.WIDGET,
                    oldWidget);
            Map<String, Object> mapped = SettingsTransferRules.forTarget(
                    preset.source, SettingsTransferRules.WIDGET, preset.values);
            if (preset.source.equals(SettingsTransferRules.WIDGET)) {
                replaceWidgetValues(editor, widgetId, mapped);
            } else {
                putValues(editor, widgetPrefix(widgetId), mapped);
            }
        }
        editor.putString(PRESETS_KEY, presetsToJson(presets).toString());
        editor.apply();
    }

    static String suggestedPresetName(String source, String componentName) {
        if (source.equals(SettingsTransferRules.GLOBALS)) return "GLOBALS";
        if (componentName != null && !componentName.trim().isEmpty()) {
            return componentName.trim();
        }
        return "Full-screen clock";
    }

    static JSONArray presetsToJson(Collection<SettingsPreset> presets)
            throws JSONException {
        JSONArray array = new JSONArray();
        for (SettingsPreset preset : presets) array.put(presetToJson(preset));
        return array;
    }

    private static void validateBackup(JSONObject backup) throws JSONException {
        if (!BACKUP_FORMAT.equals(backup.getString("format"))) {
            throw new JSONException("This is not a SecondsClock settings file");
        }
        int version = backup.getInt("version");
        if (version < 1 || version > BACKUP_VERSION) {
            throw new JSONException("Unsupported settings file version");
        }
        JSONObject active = backup.getJSONObject("active");
        readComponentSettings(active.getJSONObject("globals"),
                SettingsTransferRules.GLOBALS);
        readComponentSettings(active.getJSONObject("clock"),
                SettingsTransferRules.CLOCK);
        readWidgets(active.getJSONArray("widgets"));
        JSONArray presets = backup.getJSONArray("presets");
        for (int i = 0; i < presets.length(); ++i) {
            readPreset(presets.getJSONObject(i));
        }
    }

    private static List<ImportedWidget> readWidgets(JSONArray array)
            throws JSONException {
        List<ImportedWidget> result = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            JSONObject record = array.getJSONObject(i);
            Map<String, Object> values = readComponentSettings(
                    record.getJSONObject("values"), SettingsTransferRules.WIDGET);
            Object encodedName = record.opt("name");
            if (encodedName != null && !(encodedName instanceof String)) {
                throw new JSONException("Invalid widget name");
            }
            addWidgetDefaults(values);
            result.add(new ImportedWidget(
                    encodedName == null ? "" : (String) encodedName,
                    values));
        }
        return result;
    }

    private static JSONObject settingsJson(Map<String, ?> values)
            throws JSONException {
        JSONObject result = new JSONObject();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            Object value = entry.getValue();
            JSONObject typedValue = new JSONObject();
            if (value instanceof String) typedValue.put("type", "string");
            else if (value instanceof Boolean) typedValue.put("type", "boolean");
            else if (value instanceof Integer) typedValue.put("type", "int");
            else if (value instanceof Long) typedValue.put("type", "long");
            else if (value instanceof Float) typedValue.put("type", "float");
            else continue;
            typedValue.put("value", value);
            result.put(entry.getKey(), typedValue);
        }
        return result;
    }

    private static Map<String, Object> readSettings(JSONObject object)
            throws JSONException {
        Map<String, Object> result = new LinkedHashMap<>();
        JSONArray names = object.names();
        if (names == null) return result;
        for (int i = 0; i < names.length(); ++i) {
            String key = names.getString(i);
            if (key.isEmpty()) throw new JSONException("Empty setting key");
            JSONObject encoded = object.getJSONObject(key);
            String type = encoded.getString("type");
            Object value;
            switch (type) {
                case "string": value = encoded.getString("value"); break;
                case "boolean": value = encoded.getBoolean("value"); break;
                case "int": value = encoded.getInt("value"); break;
                case "long": value = encoded.getLong("value"); break;
                case "float": value = (float) encoded.getDouble("value"); break;
                default: throw new JSONException("Unknown setting value type");
            }
            result.put(key, value);
        }
        return result;
    }

    private static JSONObject presetToJson(SettingsPreset preset)
            throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", preset.id);
        object.put("name", preset.name);
        object.put("type", preset.type);
        object.put("source", preset.source);
        object.put("values", settingsJson(preset.values));
        return object;
    }

    private static SettingsPreset readPreset(JSONObject object)
            throws JSONException {
        String type = object.getString("type");
        if (!type.equals(SettingsPreset.GLOBALS)
                && !type.equals(SettingsPreset.DISPLAY)) {
            throw new JSONException("Unknown preset type");
        }
        String source = object.optString("source", "");
        if (type.equals(SettingsPreset.GLOBALS)) {
            source = SettingsTransferRules.GLOBALS;
        } else if (!source.equals(SettingsTransferRules.CLOCK)
                && !source.equals(SettingsTransferRules.WIDGET)) {
            throw new JSONException("Unknown display preset source");
        }
        String name = object.getString("name").trim();
        if (name.isEmpty()) throw new JSONException("Preset name is empty");
        Map<String, Object> values = readComponentSettings(
                object.getJSONObject("values"), source);
        if (type.equals(SettingsPreset.GLOBALS)
                || source.equals(SettingsTransferRules.WIDGET)) {
            addWidgetDefaults(values);
        } else {
            addClockDefaults(values);
        }
        return new SettingsPreset(object.optString("id", newId()),
                name, type, source, values);
    }

    private static Map<String, Object> readComponentSettings(
            JSONObject object, String source) throws JSONException {
        Map<String, Object> values = readSettings(object);
        String invalidSetting = invalidSetting(values, source);
        if (invalidSetting != null) {
            throw new JSONException("Invalid setting: " + invalidSetting);
        }
        return values;
    }

    /** Returns the first key with an unsupported component or value type. */
    static String invalidSetting(Map<String, ?> values, String source) {
        String[] allowed;
        if (SettingsTransferRules.GLOBALS.equals(source)
                || SettingsTransferRules.WIDGET.equals(source)) {
            allowed = WIDGET_SETTING_SUFFIXES;
        } else if (SettingsTransferRules.CLOCK.equals(source)) {
            allowed = CLOCK_SETTING_SUFFIXES;
        } else {
            return "component type";
        }

        for (Map.Entry<String, ?> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (!contains(allowed, key)) return key;
            if (contains(STRING_SETTINGS, key)) {
                if (!(value instanceof String)) return key;
            } else if (contains(BOOLEAN_SETTINGS, key)) {
                if (!(value instanceof Boolean)) return key;
            } else if (!(value instanceof Integer)) {
                // All other persisted settings are integer preferences.
                return key;
            }
        }
        return null;
    }

    private static boolean contains(String[] values, String candidate) {
        for (String value : values) if (value.equals(candidate)) return true;
        return false;
    }

    private static boolean addImportedPreset(
            List<SettingsPreset> presets, SettingsPreset candidate,
            ImportResult result) {
        AddResult add = addPreset(presets, candidate);
        if (add.duplicate != null) {
            result.duplicates.add(new DuplicatePreset(
                    candidate.name, add.duplicate.name));
            return false;
        }
        return true;
    }

    private static void archiveSameNamePresets(
            List<SettingsPreset> presets, String name) {
        String normalized = normalizeName(name);
        if (normalized.isEmpty()) return;
        List<Integer> matches = new ArrayList<>();
        for (int i = 0; i < presets.size(); ++i) {
            if (normalizeName(presets.get(i).name).equals(normalized)) {
                matches.add(i);
            }
        }
        for (int index : matches) {
            Set<String> used = new HashSet<>();
            for (int i = 0; i < presets.size(); ++i) {
                if (i != index) used.add(normalizeName(presets.get(i).name));
            }
            String archivedName = nextVersionName(name, used);
            presets.set(index, presets.get(index).withName(archivedName));
        }
    }

    private static String nextVersionName(String base, Set<String> used) {
        for (int i = 1; ; ++i) {
            String candidate = base + "-" + String.format(Locale.ROOT, "%03d", i);
            if (!used.contains(normalizeName(candidate))) return candidate;
        }
    }

    private static List<String> presetNames(Collection<SettingsPreset> presets) {
        List<String> result = new ArrayList<>();
        for (SettingsPreset preset : presets) result.add(preset.name);
        return result;
    }

    private static boolean hasPresetId(Collection<SettingsPreset> presets, String id) {
        for (SettingsPreset preset : presets) {
            if (preset.id.equals(id)) return true;
        }
        return false;
    }

    private static void replaceWidgetValues(
            SharedPreferences.Editor editor, int widgetId,
            Map<String, ?> values) {
        String prefix = widgetPrefix(widgetId);
        for (String suffix : WIDGET_SETTING_SUFFIXES) {
            editor.remove(prefix + suffix);
        }
        putValues(editor, prefix, values);
    }

    private static void replacePrefix(SharedPreferences.Editor editor,
                                      String prefix,
                                      boolean rootWidgetDefaults) {
        removePrefix(editor, prefix, rootWidgetDefaults);
    }

    private static void removePrefix(SharedPreferences.Editor editor,
                                     String prefix,
                                     boolean rootWidgetDefaults) {
        String[] suffixes = rootWidgetDefaults
                ? WIDGET_SETTING_SUFFIXES : CLOCK_SETTING_SUFFIXES;
        for (String suffix : suffixes) editor.remove(prefix + suffix);
    }

    private static void putValues(SharedPreferences.Editor editor,
                                  String prefix, Map<String, ?> values) {
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (!isAllowedSuffix(prefix, entry.getKey())) continue;
            String key = prefix + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) editor.putString(key, (String) value);
            else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
            else if (value instanceof Integer) editor.putInt(key, (Integer) value);
            else if (value instanceof Long) editor.putLong(key, (Long) value);
            else if (value instanceof Float) editor.putFloat(key, (Float) value);
        }
    }

    private static void savePresetsInEditor(
            SharedPreferences.Editor editor, List<SettingsPreset> presets)
            throws JSONException {
        editor.putString(PRESETS_KEY, presetsToJson(presets).toString());
        if (!editor.commit()) throw new JSONException("Could not save settings");
    }

    private static boolean isAllowedSuffix(String prefix, String suffix) {
        String[] allowed;
        if (prefix.equals("W")) {
            allowed = WIDGET_SETTING_SUFFIXES;
        } else if (prefix.equals("C")) {
            allowed = CLOCK_SETTING_SUFFIXES;
        } else if (prefix.startsWith("W") && prefix.length() > 1
                && Character.isDigit(prefix.charAt(1))) {
            allowed = WIDGET_SETTING_SUFFIXES;
        } else {
            return false;
        }
        for (String key : allowed) if (key.equals(suffix)) return true;
        return false;
    }

    private static boolean isPortableValue(Object value) {
        return value instanceof String || value instanceof Boolean
                || value instanceof Integer || value instanceof Long
                || value instanceof Float;
    }

    private static String widgetPresetBaseName(WidgetRecord widget) {
        return widget.name.isEmpty() ? "Widget" : widget.name;
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    private static final class ImportedWidget {
        final String name;
        final Map<String, Object> values;

        ImportedWidget(String name, Map<String, Object> values) {
            this.name = name == null ? "" : name.trim();
            this.values = values;
        }
    }
}
