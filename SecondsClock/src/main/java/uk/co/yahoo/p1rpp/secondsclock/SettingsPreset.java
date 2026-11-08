/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** A saved settings group. Presets contain values, not Android widget IDs. */
final class SettingsPreset {
    static final String GLOBALS = "globals";
    static final String DISPLAY = "display";

    final String id;
    final String name;
    final String type;
    final String source;
    final Map<String, Object> values;

    SettingsPreset(String id, String name, String type, String source,
                   Map<String, ?> values) {
        this.id = id == null ? "" : id;
        this.name = name == null ? "" : name.trim();
        this.type = type == null ? "" : type;
        this.source = source == null ? "" : source;
        this.values = Collections.unmodifiableMap(
                new LinkedHashMap<String, Object>(values));
    }

    boolean hasSameSettings(SettingsPreset other) {
        return other != null
                && type.equals(other.type)
                && values.equals(other.values);
    }

    SettingsPreset withName(String newName) {
        return new SettingsPreset(id, newName, type, source, values);
    }

    SettingsPreset withId(String newId) {
        return new SettingsPreset(newId, name, type, source, values);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SettingsPreset)) return false;
        SettingsPreset preset = (SettingsPreset) other;
        return Objects.equals(id, preset.id)
                && Objects.equals(name, preset.name)
                && Objects.equals(type, preset.type)
                && Objects.equals(source, preset.source)
                && Objects.equals(values, preset.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type, source, values);
    }
}
