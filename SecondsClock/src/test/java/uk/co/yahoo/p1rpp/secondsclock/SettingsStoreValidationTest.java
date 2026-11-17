package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SettingsStoreValidationTest {
    @Test
    public void acceptsKnownSettingsWithTheirPersistedTypes() {
        Map<String, Object> widget = new LinkedHashMap<>();
        widget.put("fgcolour", 0xFF112233);
        widget.put("format", "%H:%M");
        widget.put("advancedFormatMode", true);
        widget.put("showNextAlarm", false);
        widget.put("analogMode", true);

        assertNull(SettingsStore.invalidSetting(
                widget, SettingsTransferRules.WIDGET));
        assertNull(SettingsStore.invalidSetting(
                Collections.singletonMap("minBright", 30),
                SettingsTransferRules.CLOCK));
        assertNull(SettingsStore.invalidSetting(
                Collections.singletonMap("analogMode", true),
                SettingsTransferRules.CLOCK));
        Map<String, Object> globalColors = new LinkedHashMap<>();
        globalColors.put("lineColorsEnabled", true);
        globalColors.put("lineColorProfile", LineColors.DEUTERANOPIA);
        globalColors.put("lineColorSeed", "shared seed");
        assertNull(SettingsStore.invalidSetting(
                globalColors, SettingsTransferRules.GLOBALS));
    }

    @Test
    public void rejectsUnknownSettingKeysAndWrongValueTypes() {
        assertEquals("bgcolour", SettingsStore.invalidSetting(
                Collections.singletonMap("bgcolour", "black"),
                SettingsTransferRules.WIDGET));
        assertEquals("formatSyncEnabled", SettingsStore.invalidSetting(
                Collections.singletonMap("formatSyncEnabled", 1),
                SettingsTransferRules.CLOCK));
        assertEquals("futureSetting", SettingsStore.invalidSetting(
                Collections.singletonMap("futureSetting", 1),
                SettingsTransferRules.WIDGET));
        assertEquals("analogMode", SettingsStore.invalidSetting(
                Collections.singletonMap("analogMode", 1),
                SettingsTransferRules.WIDGET));
        assertNull(SettingsStore.invalidSetting(Collections.singletonMap(
                AlarmTimeRelevance.MAX_AGE_KEY, 24),
                SettingsTransferRules.GLOBALS));
        assertEquals(AlarmTimeRelevance.MAX_AGE_KEY,
                SettingsStore.invalidSetting(Collections.singletonMap(
                        AlarmTimeRelevance.MAX_AGE_KEY, 3),
                        SettingsTransferRules.GLOBALS));
        assertEquals(AlarmTimeRelevance.MAX_AGE_KEY,
                SettingsStore.invalidSetting(Collections.singletonMap(
                        AlarmTimeRelevance.MAX_AGE_KEY, 24),
                        SettingsTransferRules.WIDGET));
    }

    @Test
    public void alarmMaxAgeIsIncludedOnlyInActiveGlobalBackups() {
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put(AlarmTimeRelevance.MAX_AGE_KEY, 24);

        assertEquals(24, SettingsStore.backupGlobalValues(stored)
                .get(AlarmTimeRelevance.MAX_AGE_KEY));
        assertEquals(AlarmTimeRelevance.DEFAULT_MAX_AGE_HOURS,
                SettingsStore.backupGlobalValues(Collections.emptyMap())
                        .get(AlarmTimeRelevance.MAX_AGE_KEY));
        assertNull(SettingsStore.globalValues(stored)
                .get(AlarmTimeRelevance.MAX_AGE_KEY));
        assertEquals("lineColorsEnabled", SettingsStore.invalidSetting(
                Collections.singletonMap("lineColorsEnabled", 1),
                SettingsTransferRules.GLOBALS));
        assertEquals("lineColorProfile", SettingsStore.invalidSetting(
                Collections.singletonMap("lineColorProfile", 99),
                SettingsTransferRules.GLOBALS));
    }

    @Test
    public void analogModeDefaultsToDigitalAndSurvivesComponentSettingsReads() {
        assertEquals(false, SettingsStore.globalValues(Collections.emptyMap())
                .get("analogMode"));
        assertEquals(false, SettingsStore.widgetValues(
                Collections.emptyMap(), 17).get("analogMode"));
        assertEquals(false, SettingsStore.clockValues(Collections.emptyMap())
                .get("analogMode"));

        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("W17analogMode", true);
        assertEquals(true, SettingsStore.widgetValues(stored, 17)
                .get("analogMode"));
        assertEquals(true, SettingsStore.globalValues(
                Collections.singletonMap("WanalogMode", true))
                .get("analogMode"));
        assertEquals(true, SettingsStore.clockValues(
                Collections.singletonMap("CanalogMode", true))
                .get("analogMode"));
    }

    @Test
    public void rejectsSettingsFromAnUnknownComponentType() {
        assertEquals("component type", SettingsStore.invalidSetting(
                Collections.emptyMap(), "other"));
    }

    @Test
    public void colorPreferencesAreExportedAsGlobalSettingsOnly() {
        Map<String, Object> defaults = SettingsStore.globalValues(
                Collections.emptyMap());
        assertEquals(false, defaults.get("lineColorsEnabled"));
        assertEquals(LineColors.NORMAL, defaults.get("lineColorProfile"));
        assertEquals(LineColors.DEFAULT_SEED, defaults.get("lineColorSeed"));
        assertEquals(true, SettingsStore.globalValues(
                Collections.singletonMap(LineColors.ENABLED_KEY, true))
                .get("lineColorsEnabled"));
        assertEquals(false, SettingsStore.widgetValues(
                Collections.emptyMap(), 8).containsKey("lineColorsEnabled"));
    }
}
