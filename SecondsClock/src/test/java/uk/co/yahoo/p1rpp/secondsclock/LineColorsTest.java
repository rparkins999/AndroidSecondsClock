package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LineColorsTest {
    @Test
    public void seedAndProfileProduceDeterministicColors() {
        assertEquals(LineColors.colorForLine("clock", LineColors.NORMAL, 3),
                LineColors.colorForLine("clock", LineColors.NORMAL, 3));
        assertFalse(LineColors.colorForLine("clock", LineColors.NORMAL, 0)
                == LineColors.colorForLine("other", LineColors.NORMAL, 0));
        assertFalse(LineColors.colorForLine("clock", LineColors.NORMAL, 0)
                == LineColors.colorForLine("clock", LineColors.TRITANOPIA, 0));
        assertFalse(LineColors.colorForLine(
                "clock", LineColors.NORMAL, 1) == 0xFFFFFFFF);
    }

    @Test
    public void adjacentLinesUseDistinctColorsBeforeRepeatingPalette() {
        Set<Integer> colors = new HashSet<>();
        for (int line = 0; line < 8; ++line) {
            colors.add(LineColors.colorForLine(
                    "distinct-lines", LineColors.NORMAL, line));
        }
        assertEquals(8, colors.size());
        assertEquals(LineColors.colorForLine(
                        "distinct-lines", LineColors.NORMAL, 0),
                LineColors.colorForLine(
                        "distinct-lines", LineColors.NORMAL, 8));
    }

    @Test
    public void generatedSeedsAreShareableHexStrings() {
        String seed = LineColors.newSeed();

        assertTrue(seed.matches("[0-9a-f]{16}"));
    }

    @Test
    public void unsupportedProfileFallsBackToStandardPalette() {
        assertEquals(LineColors.colorForLine(
                        "clock", LineColors.NORMAL, 2),
                LineColors.colorForLine("clock", -1, 2));
        assertEquals(LineColors.colorForLine(
                        "clock", LineColors.NORMAL, 2),
                LineColors.colorForLine("clock", 99, 2));
    }

    @Test
    public void paletteColorsKeepTheConfiguredForegroundOpacity() {
        int palette = LineColors.colorForLine(
                "clock", LineColors.NORMAL, 2);
        int base = 0x55112233;

        assertEquals(0x55, (LineColors.withBaseAlpha(palette, base) >>> 24) & 0xFF);
        assertEquals(palette & 0x00FFFFFF,
                LineColors.withBaseAlpha(palette, base) & 0x00FFFFFF);
    }
}
