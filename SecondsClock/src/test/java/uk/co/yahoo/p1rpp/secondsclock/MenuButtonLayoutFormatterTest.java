package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MenuButtonLayoutFormatterTest {
    private static final int WIDTH = 300;
    private static final int MARGIN = 4;

    @Test
    public void keepsAllButtonsTogetherWhenEqualSlotsFit() {
        assertRows(new int[] { 80, 90, 85 }, false,
                new int[] { 0, 1, 2 });
    }

    @Test
    public void normalThreeButtonGroupKeepsFirstButtonAboveFittingPair() {
        assertRows(new int[] { 110, 60, 80 }, false,
                new int[] { 0 }, new int[] { 1, 2 });
    }

    @Test
    public void normalThreeButtonGroupUsesThreeRowsWhenPairDoesNotFit() {
        assertRows(new int[] { 110, 100, 150 }, false,
                new int[] { 0 }, new int[] { 1 }, new int[] { 2 });
    }

    @Test
    public void reverseThreeButtonGroupFillsTopRowWithFirstPair() {
        assertRows(new int[] { 90, 100, 100 }, true,
                new int[] { 0, 1 }, new int[] { 2 });
    }

    @Test
    public void fourButtonGroupBalancesIntoTwoRows() {
        assertRows(new int[] { 100, 110, 100, 110 }, false,
                new int[] { 0, 1 }, new int[] { 2, 3 });
    }

    @Test
    public void fourButtonGroupNeverFallsBackToOnePlusThree() {
        assertRows(new int[] { 160, 160, 160, 160 }, false,
                new int[] { 0 }, new int[] { 1 }, new int[] { 2 },
                new int[] { 3 });
    }

    @Test
    public void largerGroupsKeepRowsBalancedInEitherDirection() {
        assertRows(new int[] { 90, 90, 90, 90, 90 }, false,
                new int[] { 0, 1 }, new int[] { 2, 3, 4 });
        assertRows(new int[] { 90, 90, 90, 90, 90 }, true,
                new int[] { 0, 1, 2 }, new int[] { 3, 4 });
    }

    private static void assertRows(int[] buttonWidths, boolean reverseFill,
                                   int[]... expectedRows) {
        int[][] actualRows = MenuButtonLayoutFormatter.format(
                buttonWidths, WIDTH, MARGIN, reverseFill);
        assertEquals(expectedRows.length, actualRows.length);
        for (int i = 0; i < expectedRows.length; ++i) {
            assertArrayEquals(expectedRows[i], actualRows[i]);
        }
    }
}
