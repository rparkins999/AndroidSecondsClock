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

        assertNull(SettingsStore.invalidSetting(
                widget, SettingsTransferRules.WIDGET));
        assertNull(SettingsStore.invalidSetting(
                Collections.singletonMap("minBright", 30),
                SettingsTransferRules.CLOCK));
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
    }

    @Test
    public void rejectsSettingsFromAnUnknownComponentType() {
        assertEquals("component type", SettingsStore.invalidSetting(
                Collections.emptyMap(), "other"));
    }
}
