/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.security.SecureRandom;

/** Deterministic, high-contrast colors for advanced-format lines. */
final class LineColors {
    static final String ENABLED_KEY = "WlineColorsEnabled";
    static final String PROFILE_KEY = "WlineColorProfile";
    static final String SEED_KEY = "WlineColorSeed";
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

    static boolean automaticEnabled(SharedPreferences prefs) {
        return prefs.getBoolean(ENABLED_KEY, false);
    }

    static boolean shouldColor(SharedPreferences prefs, boolean marked) {
        return marked || automaticEnabled(prefs);
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
