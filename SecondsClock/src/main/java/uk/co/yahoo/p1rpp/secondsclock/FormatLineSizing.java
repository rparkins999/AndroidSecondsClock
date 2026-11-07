package uk.co.yahoo.p1rpp.secondsclock;

/** Calculates relative font scales for emphasized clock-format lines. */
final class FormatLineSizing {
    private static final float WIDTH_TARGET_RATIO = 1.7F;

    private FormatLineSizing() {}

    static float relativeScale(float lineWidth, float widestLine) {
        if (!(lineWidth > 0.0F) || !(widestLine > 0.0F)) return 1.0F;
        return WIDTH_TARGET_RATIO * widestLine / lineWidth;
    }
}
