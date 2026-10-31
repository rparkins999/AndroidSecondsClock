/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

final class FormatProfiles {
    static final int SYNC = 0;
    static final int HORIZONTAL = 1;
    static final int VERTICAL = 2;
    static final int PORTRAIT = 1;
    static final int LANDSCAPE = 2;

    private FormatProfiles() {}

    static boolean isHorizontalWidget(int widthDp, int heightDp) {
        return widthDp >= heightDp;
    }

    static String widgetFormat(
            String synced, String horizontal, String vertical,
            boolean useSynced, boolean isHorizontal) {
        if (useSynced) return emptyIfNull(synced);
        String selected = isHorizontal ? horizontal : vertical;
        return selected == null ? emptyIfNull(synced) : selected;
    }

    static String clockFormat(
            String synced, String portrait, String landscape,
            boolean useSynced, boolean isLandscape) {
        if (useSynced) return emptyIfNull(synced);
        String selected = isLandscape ? landscape : portrait;
        return selected == null ? emptyIfNull(synced) : selected;
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
