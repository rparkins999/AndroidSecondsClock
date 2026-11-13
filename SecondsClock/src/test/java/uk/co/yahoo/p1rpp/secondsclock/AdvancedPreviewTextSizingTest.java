package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AdvancedPreviewTextSizingTest {
    @Test
    public void clockRowReducesTextSizeCeilingForRemainingRows() {
        int withoutClock = AdvancedPreviewTextSizing.maximumTextSizePx(
                180.0F, 280.0F, 300.0F);
        int withClock = AdvancedPreviewTextSizing.maximumTextSizePx(
                60.0F, 280.0F, 300.0F);

        assertTrue(withClock < withoutClock);
    }

    @Test
    public void textSizeCeilingRespectsAvailableWidth() {
        int maxTextSize = AdvancedPreviewTextSizing.maximumTextSizePx(
                180.0F, 140.0F, 400.0F);

        assertTrue(maxTextSize * 4.0F <= 140.0F * 0.98F);
    }

    @Test
    public void emphasizedLineIsClampedToItsRowAndWidgetWidth() {
        int maxTextSize = AdvancedPreviewTextSizing.maximumTextSizePx(
                80.0F, 300.0F, 400.0F);

        assertTrue(maxTextSize <= 80.0F * 0.85F);
        assertTrue(maxTextSize * 4.0F <= 300.0F * 0.98F);
    }

    @Test
    public void advancedLinesKeepTheirIndependentLayoutWeights() {
        assertTrue(AdvancedPreviewTextSizing.rowWeight(false, false, false, false)
                < AdvancedPreviewTextSizing.rowWeight(false, false, true, false));
        assertTrue(AdvancedPreviewTextSizing.rowWeight(false, false, true, false)
                < AdvancedPreviewTextSizing.rowWeight(false, false, true, true));
        assertTrue(AdvancedPreviewTextSizing.rowWeight(false, false, true, false)
                > AdvancedPreviewTextSizing.rowWeight(false, false, false, false));
        assertTrue(AdvancedPreviewTextSizing.rowWeight(true, false, false, true)
                > AdvancedPreviewTextSizing.rowWeight(false, false, true, true));
    }

    @Test
    public void normalWidgetKeepsItsExistingVerticalLayout() {
        float totalWeight = 5.1F;

        assertTrue(Math.abs(AdvancedPreviewTextSizing.layoutWeightSum(
                totalWeight, 300.0F, 500.0F, true, true)
                - totalWeight) < 0.001F);
    }

    @Test
    public void tallWidgetOffsetsCompactContentByHalfTheFreeHeight() {
        float totalWeight = 5.1F;
        float width = 300.0F;
        float height = 900.0F;
        float topOffset = AdvancedPreviewTextSizing.topOffsetPx(
                width, height, true, true);
        float weightSum = AdvancedPreviewTextSizing.layoutWeightSum(
                totalWeight, width, height, true, true);
        float rowAreaHeight = height - topOffset;
        float occupiedHeight = rowAreaHeight * totalWeight / weightSum;
        float bottomFreeHeight = height - topOffset - occupiedHeight;

        assertTrue(weightSum > totalWeight);
        assertTrue(occupiedHeight <= width * 2.0F + 0.001F);
        assertTrue(Math.abs(topOffset - bottomFreeHeight) < 0.001F);
    }

    @Test
    public void wrappingTextKeepsAllAvailableHeight() {
        float totalWeight = 5.1F;

        assertTrue(Math.abs(AdvancedPreviewTextSizing.layoutWeightSum(
                totalWeight, 300.0F, 900.0F, false, false)
                - totalWeight) < 0.001F);
    }

    @Test
    public void spareHeightOffsetsTextOnlyBlockByHalfTheBottomSpace() {
        float totalWeight = 2.0F;
        float width = 300.0F;
        float height = 450.0F;
        float topOffset = AdvancedPreviewTextSizing.topOffsetPx(
                width, height, true, false);
        float weightSum = AdvancedPreviewTextSizing.layoutWeightSum(
                totalWeight, width, height, true, false);
        float rowAreaHeight = height - topOffset;
        float occupiedHeight = rowAreaHeight * totalWeight / weightSum;
        float bottomFreeHeight = height - topOffset - occupiedHeight;

        assertTrue(weightSum > totalWeight);
        assertTrue(Math.abs(occupiedHeight - width) < 0.001F);
        assertTrue(Math.abs(topOffset - bottomFreeHeight) < 0.001F);
    }

}
