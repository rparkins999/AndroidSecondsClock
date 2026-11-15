/*
 * Copyright © 2024. Richard P. Parkins, M. A.
 * Released under GPL V3 or later
 */

package uk.co.yahoo.p1rpp.secondsclock;

final class MenuButtonLayoutFormatter {
    private MenuButtonLayoutFormatter() {}

    static int[][] format(int[] buttonWidths, int availableWidth,
                          int horizontalMargin, boolean reverseFill) {
        int count = buttonWidths.length;
        if (count == 0) return new int[0][];

        for (int rowCount = 1; rowCount <= count; ++rowCount) {
            int[][] rows = partition(count, rowCount, reverseFill);
            if (fitsRows(buttonWidths, rows, availableWidth, horizontalMargin)) {
                return rows;
            }
        }

        // Single-button rows are the safest fallback if even balanced rows fail.
        return partition(count, count, reverseFill);
    }

    private static int[][] partition(int buttonCount, int rowCount,
                                     boolean reverseFill) {
        int[][] rows = new int[rowCount][];
        int buttonsPerRow = buttonCount / rowCount;
        int extraButtons = buttonCount % rowCount;
        int start = 0;
        for (int row = 0; row < rowCount; ++row) {
            boolean getsExtraButton = reverseFill
                    ? row < extraButtons
                    : row >= rowCount - extraButtons;
            int rowSize = buttonsPerRow + (getsExtraButton ? 1 : 0);
            rows[row] = range(start, start + rowSize);
            start += rowSize;
        }
        return rows;
    }

    private static boolean fitsRows(int[] buttonWidths, int[][] rows,
                                    int availableWidth, int horizontalMargin) {
        for (int[] row : rows) {
            if (!fits(buttonWidths, row[0], row.length + row[0],
                    availableWidth, horizontalMargin)) {
                return false;
            }
        }
        return true;
    }

    private static boolean fits(int[] buttonWidths, int start, int end,
                                int availableWidth, int horizontalMargin) {
        int count = end - start;
        int buttonSpace = availableWidth - 2 * horizontalMargin * count;
        if (buttonSpace < 0) return false;
        int equalButtonWidth = buttonSpace / count;
        for (int i = start; i < end; ++i) {
            if (buttonWidths[i] > equalButtonWidth) return false;
        }
        return true;
    }

    private static int[] range(int start, int end) {
        int[] result = new int[end - start];
        for (int i = 0; i < result.length; ++i) {
            result[i] = start + i;
        }
        return result;
    }
}
