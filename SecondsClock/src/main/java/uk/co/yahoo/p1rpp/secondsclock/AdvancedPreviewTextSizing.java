package uk.co.yahoo.p1rpp.secondsclock;

/** Sizes advanced-format text rows to the space available in their frame. */
final class AdvancedPreviewTextSizing {
    private static final float MAX_ANALOG_CONTENT_HEIGHT_TO_WIDTH_RATIO = 2.0F;
    private static final float MAX_TEXT_CONTENT_HEIGHT_TO_WIDTH_RATIO = 1.0F;
    private static final float MAX_ROW_HEIGHT_RATIO = 0.85F;
    private static final float MAX_ROW_WIDTH_RATIO = 0.98F;
    private static final int MIN_MAX_TEXT_SIZE_PX = 8;

    private AdvancedPreviewTextSizing() {}

    static int maximumTextSizePx(
            float rowHeightPx, float rowWidthPx, float textWidthAt100Px) {
        float heightLimit = rowHeightPx * MAX_ROW_HEIGHT_RATIO;
        float widthLimit = textWidthAt100Px > 0.0F
                ? rowWidthPx * MAX_ROW_WIDTH_RATIO * 100.0F / textWidthAt100Px
                : heightLimit;
        return Math.max(MIN_MAX_TEXT_SIZE_PX,
                (int) Math.floor(Math.min(heightLimit, widthLimit)));
    }

    static float rowWeight(
            boolean analog, boolean longText, boolean emphasized,
            boolean formatHasAnalog) {
        if (analog) return 2.5F;
        if (emphasized) return formatHasAnalog ? 1.6F : 1.2F;
        return longText ? 1.5F : 0.5F;
    }

    /** Keeps excess height split evenly above and below compact content. */
    static float layoutWeightSum(
            float totalWeight, float widthPx, float heightPx,
            boolean allowTallCompaction, boolean formatHasAnalog) {
        if (!allowTallCompaction || totalWeight <= 0.0F
                || widthPx <= 0.0F || heightPx <= 0.0F) {
            return totalWeight;
        }
        float contentHeight = contentHeightPx(
                widthPx, heightPx, allowTallCompaction, formatHasAnalog);
        float topOffset = (heightPx - contentHeight) / 2.0F;
        float rowAreaHeight = heightPx - topOffset;
        return totalWeight * rowAreaHeight / contentHeight;
    }

    static float topOffsetPx(
            float widthPx, float heightPx,
            boolean allowTallCompaction, boolean formatHasAnalog) {
        float contentHeight = contentHeightPx(
                widthPx, heightPx, allowTallCompaction, formatHasAnalog);
        return Math.max(0.0F, (heightPx - contentHeight) / 2.0F);
    }

    private static float contentHeightPx(
            float widthPx, float heightPx,
            boolean allowTallCompaction, boolean formatHasAnalog) {
        if (!allowTallCompaction || widthPx <= 0.0F || heightPx <= 0.0F) {
            return heightPx;
        }
        float maxContentRatio = formatHasAnalog
                ? MAX_ANALOG_CONTENT_HEIGHT_TO_WIDTH_RATIO
                : MAX_TEXT_CONTENT_HEIGHT_TO_WIDTH_RATIO;
        return Math.min(heightPx, widthPx * maxContentRatio);
    }

    static float rowHeightPx(float totalHeightPx, float rowWeight, float weightSum) {
        return weightSum > 0.0F
                ? totalHeightPx * rowWeight / weightSum : 0.0F;
    }

}
