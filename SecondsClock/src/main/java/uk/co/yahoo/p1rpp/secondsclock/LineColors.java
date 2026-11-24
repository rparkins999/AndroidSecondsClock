/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.ForegroundColorSpan;

import java.security.SecureRandom;
import java.util.List;

/** Deterministic, high-contrast colors for clock and widget lines. */
final class LineColors {
    static final String ENABLED_SUFFIX = "lineColorsEnabled";
    static final String PROFILE_KEY = "WlineColorProfile";
    static final String SEED_KEY = "WlineColorSeed";
    private static final String WIDGET_DEFAULT_PREFIX = "W";
    static final String ACTION_CHANGED =
            "uk.co.yahoo.p1rpp.secondsclock.LINE_COLORS_CHANGED";
    static final int NORMAL = 0;
    static final int PROTANOPIA = 1;
    static final int DEUTERANOPIA = 2;
    static final int TRITANOPIA = 3;
    static final int MONOCHROME = 4;
    static final int PROFILE_COUNT = 5;
    static final String DEFAULT_SEED = "seconds-clock";

    // Each palette is an ordered cycle with well-separated neighbors. Seeds
    // rotate or reverse it while preserving the neighboring-color spacing.
    private static final int[][] PALETTES = {
            {0xFFD55E00, 0xFF009E73, 0xFFF0E442, 0xFF56B4E9,
                    0xFFCC79A7, 0xFFE69F00, 0xFF0072B2, 0xFFFFFFFF},
            {0xFF56B4E9, 0xFFF0E442, 0xFFCC79A7, 0xFF8DD3C7,
                    0xFFE69F00, 0xFFFFFFFF, 0xFF0072B2, 0xFFFFD8A8},
            {0xFF0072B2, 0xFFF0E442, 0xFFCC79A7, 0xFFE69F00,
                    0xFFFFFFFF, 0xFFF000A8, 0xFF56B4E9, 0xFFFFCB77},
            {0xFFD55E00, 0xFF009E73, 0xFFFF9DA7, 0xFF59A14F,
                    0xFFE31A1C, 0xFFFFFFFF, 0xFFE69F00, 0xFFCC79A7},
            {0xFF636363, 0xFFB6B6B6, 0xFF777777, 0xFFCCCCCC,
                    0xFF8B8B8B, 0xFFE2E2E2, 0xFFA0A0A0, 0xFFF9F9F9}
    };

    private static final SecureRandom RANDOM = new SecureRandom();

    private LineColors() {}

    static String enabledKey(String preferencePrefix) {
        return preferencePrefix + ENABLED_SUFFIX;
    }

    static boolean automaticEnabled(
            SharedPreferences prefs, String preferencePrefix) {
        String key = enabledKey(preferencePrefix);
        if (prefs.contains(key)) return prefs.getBoolean(key, false);

        // Older versions stored one shared switch in the widget-defaults
        // preference. Seed each clock/widget once so it becomes independent.
        boolean inherited = prefs.getBoolean(
                enabledKey(WIDGET_DEFAULT_PREFIX), false);
        prefs.edit().putBoolean(key, inherited).apply();
        return inherited;
    }

    static int checkboxLineColor(
            SharedPreferences prefs, String preferencePrefix,
            int lineIndex, int baseColor) {
        return automaticEnabled(prefs, preferencePrefix)
                ? withBaseAlpha(colorForLine(prefs, lineIndex), baseColor)
                : baseColor;
    }

    static CharSequence colorCheckboxFormat(
            CharSequence format, SharedPreferences prefs,
            String preferencePrefix, int firstLineIndex, int baseColor) {
        if (format == null || format.length() == 0) return format;

        List<CheckboxFormatLines.Line> lines =
                CheckboxFormatLines.split(format.toString(), format.toString());
        SpannableStringBuilder colored = new SpannableStringBuilder(format);
        int lineStart = 0;
        int lineIndex = 0;
        for (int index = 0; index <= format.length(); ++index) {
            if (index < format.length() && format.charAt(index) != '\n') continue;
            if (index > lineStart && lineIndex < lines.size()) {
                colored.setSpan(new ForegroundColorSpan(checkboxLineColor(
                                prefs, preferencePrefix,
                                firstLineIndex + lineIndex, baseColor)),
                        lineStart, index, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            ++lineIndex;
            lineStart = index + 1;
        }
        return colored;
    }

    static CharSequence colorFormattedText(
            CharSequence text, CharSequence format) {
        if (text == null || format == null || !(format instanceof Spanned)) {
            return text;
        }

        Spanned styledFormat = (Spanned) format;
        SpannableStringBuilder colored = new SpannableStringBuilder(text);
        int formatStart = 0;
        int textStart = 0;
        while (formatStart < format.length() && textStart < text.length()) {
            int formatEnd = lineEnd(format, formatStart);
            int textEnd = lineEnd(text, textStart);
            ForegroundColorSpan[] spans = styledFormat.getSpans(
                    formatStart, formatEnd, ForegroundColorSpan.class);
            if (formatEnd > formatStart && textEnd > textStart
                    && spans.length > 0) {
                colored.setSpan(new ForegroundColorSpan(
                                spans[0].getForegroundColor()),
                        textStart, textEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (formatEnd == format.length() || textEnd == text.length()) break;
            formatStart = formatEnd + 1;
            textStart = textEnd + 1;
        }
        return new SpannedString(colored);
    }

    private static int lineEnd(CharSequence text, int start) {
        int end = start;
        while (end < text.length() && text.charAt(end) != '\n') ++end;
        return end;
    }

    static int colorForLine(SharedPreferences prefs, int lineIndex) {
        String seed = prefs.getString(SEED_KEY, DEFAULT_SEED);
        int profile = prefs.getInt(PROFILE_KEY, NORMAL);
        return colorForLine(seed, profile, lineIndex);
    }

    static int colorForLine(String seed, int profile, int lineIndex) {
        int selectedProfile = profile >= 0 && profile < PROFILE_COUNT
                ? profile : NORMAL;
        int[] palette = PALETTES[selectedProfile];
        long seedHash = hashSeed(seed == null ? DEFAULT_SEED : seed);
        long defaultSeedHash = hashSeed(DEFAULT_SEED);
        long profileSalt = 0x9E3779B97F4A7C15L * selectedProfile;
        long profileHash = mix64(seedHash ^ profileSalt);
        long defaultProfileHash = mix64(
                defaultSeedHash ^ profileSalt);
        int offset = (int) Long.remainderUnsigned(
                profileHash - defaultProfileHash, palette.length);
        int stride = ((seedHash ^ defaultSeedHash) & 1L) == 0L
                ? 1 : palette.length - 1;
        int index = Math.floorMod(
                offset + Math.max(0, lineIndex) * stride, palette.length);
        return palette[index];
    }

    static int withBaseAlpha(int paletteColor, int baseColor) {
        return (paletteColor & 0x00FFFFFF) | (baseColor & 0xFF000000);
    }

    static String newSeed() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format(
                    java.util.Locale.ROOT, "%02x", value & 0xFF));
        }
        return result.toString();
    }

    static void notifyChanged(Context context) {
        Intent changed = new Intent(ACTION_CHANGED)
                .setPackage(context.getPackageName());
        context.sendBroadcast(changed);
        SecondsClockWidget.requestUpdateAll(context);
    }

    private static long hashSeed(String seed) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < seed.length(); ++i) {
            hash ^= seed.charAt(i);
            hash *= 0x100000001b3L;
        }
        return mix64(hash);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
