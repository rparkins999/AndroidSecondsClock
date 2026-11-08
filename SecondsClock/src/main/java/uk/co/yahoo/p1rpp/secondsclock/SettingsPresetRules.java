/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Name and duplicate rules shared by backup import and the preset manager. */
final class SettingsPresetRules {
    private SettingsPresetRules() {}

    static SettingsPreset duplicateOf(
            SettingsPreset candidate, Collection<SettingsPreset> existing) {
        for (SettingsPreset preset : existing) {
            if (candidate.hasSameSettings(preset)) return preset;
        }
        return null;
    }

    /** Finds a free historical name such as name-001 or name-old-002. */
    static String availableName(String base, Collection<String> names) {
        String cleanBase = base == null || base.trim().isEmpty()
                ? "Preset" : base.trim();
        Set<String> used = new HashSet<>();
        for (String name : names) {
            if (name != null) used.add(name.toLowerCase(java.util.Locale.ROOT));
        }
        if (!used.contains(cleanBase.toLowerCase(java.util.Locale.ROOT))) {
            return cleanBase;
        }
        for (int i = 1; ; ++i) {
            String candidate = cleanBase + "-" + String.format(
                    java.util.Locale.ROOT, "%03d", i);
            if (!used.contains(candidate.toLowerCase(java.util.Locale.ROOT))) {
                return candidate;
            }
        }
    }

    /** A replacement snapshot uses name-old, then name-old-001, etc. */
    static String oldSettingsName(String base, Collection<String> names) {
        String oldBase = (base == null || base.trim().isEmpty()
                ? "Settings" : base.trim()) + "-old";
        Set<String> used = new HashSet<>();
        for (String name : names) {
            if (name != null) used.add(name.toLowerCase(java.util.Locale.ROOT));
        }
        if (!used.contains(oldBase.toLowerCase(java.util.Locale.ROOT))) {
            return oldBase;
        }
        for (int i = 1; ; ++i) {
            String candidate = oldBase + "-" + String.format(
                    java.util.Locale.ROOT, "%03d", i);
            if (!used.contains(candidate.toLowerCase(java.util.Locale.ROOT))) {
                return candidate;
            }
        }
    }
}
