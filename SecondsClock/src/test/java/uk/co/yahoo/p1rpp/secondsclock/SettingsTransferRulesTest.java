package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SettingsTransferRulesTest {
    @Test
    public void clockDisplayPresetMapsToWidgetAndRetainsOnlyApplicableValues() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("fgcolour", 0xFF112233);
        source.put("format", "%H:%M");
        source.put("formatPortrait", "%H\n%M");
        source.put("formatLandscape", "%H:%M");
        source.put("formatProfile", 1); // portrait
        source.put("showNextAlarm", true);
        source.put("secondsSize", 255);
        source.put("minBright", 30); // full-screen-only setting

        Map<String, Object> result = SettingsTransferRules.forTarget(
                SettingsTransferRules.CLOCK, SettingsTransferRules.WIDGET, source);

        assertEquals(0xFF112233, result.get("fgcolour"));
        assertEquals("%H\n%M", result.get("formatVertical"));
        assertEquals("%H:%M", result.get("formatHorizontal"));
        assertEquals(2, result.get("formatProfile")); // vertical
        assertEquals(2, result.get("showTime"));
        assertTrue((Boolean) result.get("showNextAlarm"));
        assertFalse(result.containsKey("minBright"));
    }

    @Test
    public void widgetDisplayPresetMapsToClockWithoutErasingClockOnlySettings() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("formatVertical", "%H\n%M");
        source.put("formatHorizontal", "%H:%M");
        source.put("formatProfile", 1); // horizontal
        source.put("showTime", 1);
        source.put("showWeekDay", 2);
        source.put("touchaction", 9000); // widget-only setting

        Map<String, Object> result = SettingsTransferRules.forTarget(
                SettingsTransferRules.WIDGET, SettingsTransferRules.CLOCK, source);

        assertEquals("%H\n%M", result.get("formatPortrait"));
        assertEquals("%H:%M", result.get("formatLandscape"));
        assertEquals(2, result.get("formatProfile")); // landscape
        assertEquals(0, result.get("secondsSize"));
        assertEquals(2, result.get("showWeekDay"));
        assertFalse(result.containsKey("touchaction"));
    }

    @Test
    public void globalPresetsCannotBeTransferredToDisplayTargets() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("showYear", 1);

        assertTrue(SettingsTransferRules.forTarget(
                SettingsTransferRules.GLOBALS,
                SettingsTransferRules.CLOCK, source).isEmpty());
        assertTrue(SettingsTransferRules.forTarget(
                SettingsTransferRules.CLOCK,
                SettingsTransferRules.GLOBALS, source).isEmpty());
    }

    @Test
    public void crossTypeTransferIgnoresInactiveCheckboxTimeValueInAdvancedMode() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("advancedFormatMode", true);
        source.put("secondsSize", 255);

        Map<String, Object> result = SettingsTransferRules.forTarget(
                SettingsTransferRules.CLOCK, SettingsTransferRules.WIDGET, source);

        assertTrue((Boolean) result.get("advancedFormatMode"));
        assertFalse(result.containsKey("showTime"));
    }

    @Test
    public void crossTypeTransferUsesSourceDefaultsInsteadOfKeepingTargetValues() {
        Map<String, Object> result = SettingsTransferRules.forTarget(
                SettingsTransferRules.CLOCK, SettingsTransferRules.WIDGET,
                new LinkedHashMap<>());

        assertEquals(false, result.get("showNextAlarm"));
        assertEquals(false, result.get("advancedFormatMode"));
        assertEquals(2, result.get("showTime"));
        assertEquals(0xFFFFFFFF, result.get("fgcolour"));
    }

    @Test
    public void analogModeTransfersBetweenWidgetAndFullScreenSettings() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("analogMode", true);

        Map<String, Object> widget = SettingsTransferRules.forTarget(
                SettingsTransferRules.WIDGET, SettingsTransferRules.WIDGET, source);
        Map<String, Object> clock = SettingsTransferRules.forTarget(
                SettingsTransferRules.WIDGET, SettingsTransferRules.CLOCK, source);

        assertTrue((Boolean) widget.get("analogMode"));
        assertTrue((Boolean) clock.get("analogMode"));
    }
}
