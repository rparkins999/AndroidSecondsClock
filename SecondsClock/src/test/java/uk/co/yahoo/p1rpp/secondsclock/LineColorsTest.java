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
    public void nonAdjacentPaletteColorsRemainPerceptuallySeparated() {
        for (int profile = 0; profile < LineColors.PROFILE_COUNT; ++profile) {
            int[] colors = new int[8];
            for (int line = 0; line < colors.length; ++line) {
                colors[line] = LineColors.colorForLine(
                        "palette-spacing", profile, line);
            }

            double minimumDistance = Double.MAX_VALUE;
            for (int left = 0; left < colors.length; ++left) {
                for (int right = left + 1; right < colors.length; ++right) {
                    minimumDistance = Math.min(minimumDistance,
                            perceptualDistance(colors[left], colors[right]));
                }
            }

            double requiredDistance = profile == LineColors.MONOCHROME
                    ? 7.5 : 20.0;
            assertTrue("profile " + profile + " has colors that are too close: "
                            + minimumDistance,
                    minimumDistance >= requiredDistance);
        }
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

    private static double perceptualDistance(int first, int second) {
        double[] firstLab = toLab(first);
        double[] secondLab = toLab(second);
        double lightness = firstLab[0] - secondLab[0];
        double a = firstLab[1] - secondLab[1];
        double b = firstLab[2] - secondLab[2];
        return Math.sqrt(lightness * lightness + a * a + b * b);
    }

    private static double[] toLab(int color) {
        double red = linear((color >>> 16) & 0xFF);
        double green = linear((color >>> 8) & 0xFF);
        double blue = linear(color & 0xFF);
        double x = (0.4124564 * red + 0.3575761 * green + 0.1804375 * blue)
                / 0.95047;
        double y = 0.2126729 * red + 0.7151522 * green + 0.0721750 * blue;
        double z = (0.0193339 * red + 0.1191920 * green + 0.9503041 * blue)
                / 1.08883;
        double fx = labPivot(x);
        double fy = labPivot(y);
        double fz = labPivot(z);
        return new double[] {
                116.0 * fy - 16.0,
                500.0 * (fx - fy),
                200.0 * (fy - fz)
        };
    }

    private static double linear(int channel) {
        double value = channel / 255.0;
        return value <= 0.04045 ? value / 12.92
                : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static double labPivot(double value) {
        return value > 0.008856
                ? Math.cbrt(value) : 7.787 * value + 16.0 / 116.0;
    }
}
