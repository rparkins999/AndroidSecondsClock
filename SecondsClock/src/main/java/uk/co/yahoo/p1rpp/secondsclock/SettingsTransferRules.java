/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import java.util.LinkedHashMap;
import java.util.Map;

/** Maps settings shared by the full-screen clock and home-screen widgets. */
final class SettingsTransferRules {
    static final String GLOBALS = "globals";
    static final String CLOCK = "clock";
    static final String WIDGET = "widget";

    private SettingsTransferRules() {}

    static Map<String, Object> forTarget(
            String source, String target, Map<String, ?> values) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (source.equals(target)) {
            result.putAll(values);
            return result;
        }
        if (source.equals(GLOBALS) || target.equals(GLOBALS)) return result;

        putEffective(values, result, "showWeekDay", 2);
        putEffective(values, result, "showShortDate", 0);
        putEffective(values, result, "showMonthDay", 1);
        putEffective(values, result, "showMonth", 2);
        putEffective(values, result, "showYear", 1);
        putEffective(values, result, "analogMode", false);
        putEffective(values, result, "showNextAlarm", false);
        putEffective(values, result, "formatSyncEnabled", true);
        putEffective(values, result, "textStyleEmoji", false);
        putEffective(values, result, "fgcolour", 0xFFFFFFFF);
        result.put("advancedFormatMode", isAdvancedFormat(source, values));
        putEffective(values, result, "formatProfile", 0);
        copy(values, result, "format", "format");

        if (source.equals(CLOCK)) {
            copy(values, result, "formatPortrait", "formatVertical");
            copy(values, result, "formatLandscape", "formatHorizontal");
            Object secondsSize = values.get("secondsSize");
            if (!isAdvancedFormat(source, values)) {
                int size = secondsSize instanceof Number
                        ? ((Number) secondsSize).intValue() : 255;
                result.put("showTime", size == 0 ? 1 : 2);
            }
        } else if (source.equals(WIDGET)) {
            copy(values, result, "formatVertical", "formatPortrait");
            copy(values, result, "formatHorizontal", "formatLandscape");
            Object showTime = values.get("showTime");
            if (!isAdvancedFormat(source, values)) {
                int time = showTime instanceof Number
                        ? ((Number) showTime).intValue() : 2;
                if (time > 0) result.put("secondsSize", time == 1 ? 0 : 255);
            }
        }

        // Widget profiles use 1=horizontal, 2=vertical; clock profiles use
        // 1=portrait, 2=landscape. Those orientations are the opposite pair.
        Object profile = result.get("formatProfile");
        if (profile instanceof Number) {
            int value = ((Number) profile).intValue();
            if (value == 1) result.put("formatProfile", 2);
            else if (value == 2) result.put("formatProfile", 1);
        }
        return result;
    }

    private static void copy(Map<String, ?> from, Map<String, Object> to,
                             String fromKey, String toKey) {
        if (from.containsKey(fromKey)) to.put(toKey, from.get(fromKey));
    }

    private static boolean isAdvancedFormat(String source, Map<String, ?> values) {
        Object advanced = values.get("advancedFormatMode");
        if (advanced instanceof Boolean) return (Boolean) advanced;
        return source.equals(WIDGET) && values.containsKey("format");
    }

    private static void putEffective(Map<String, ?> source,
                                     Map<String, Object> target,
                                     String key, Object defaultValue) {
        target.put(key, source.containsKey(key) ? source.get(key) : defaultValue);
    }
}
