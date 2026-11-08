package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SettingsPresetRulesTest {
    private SettingsPreset preset(String id, String name, String type,
                                  String key, Object value) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(key, value);
        return new SettingsPreset(id, name, type, "widget", values);
    }

    @Test
    public void sameValuesAreDuplicatesRegardlessOfName() {
        SettingsPreset existing = preset("one", "Desk", SettingsPreset.DISPLAY,
                "fgcolour", 0xFFFFFFFF);
        SettingsPreset candidate = preset("two", "Office", SettingsPreset.DISPLAY,
                "fgcolour", 0xFFFFFFFF);

        assertNotNull(SettingsPresetRules.duplicateOf(
                candidate, Collections.singletonList(existing)));
    }

    @Test
    public void differentSettingsOrTypesAreNotDuplicates() {
        SettingsPreset existing = preset("one", "Desk", SettingsPreset.DISPLAY,
                "fgcolour", 0xFFFFFFFF);
        SettingsPreset differentValue = preset("two", "Desk", SettingsPreset.DISPLAY,
                "fgcolour", 0xFF000000);
        SettingsPreset differentType = preset("three", "Desk", SettingsPreset.GLOBALS,
                "fgcolour", 0xFFFFFFFF);

        assertNull(SettingsPresetRules.duplicateOf(
                differentValue, Collections.singletonList(existing)));
        assertNull(SettingsPresetRules.duplicateOf(
                differentType, Collections.singletonList(existing)));
    }

    @Test
    public void newVersionsUseTheNextFreeThreeDigitSuffix() {
        assertEquals("Desk-003", SettingsPresetRules.availableName("Desk",
                Arrays.asList("Desk", "Desk-001", "Desk-002")));
    }

    @Test
    public void replacementSnapshotsUseOldSuffixAndNextFreeNumber() {
        assertEquals("Desk-old-002", SettingsPresetRules.oldSettingsName("Desk",
                Arrays.asList("Desk-old", "Desk-old-001")));
    }

    @Test
    public void namesAreComparedWithoutCase() {
        assertEquals("desk-001", SettingsPresetRules.availableName("desk",
                Collections.singletonList("Desk")));
    }
}
