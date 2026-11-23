/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Splits checkbox-mode TextClock formats into independently colorable rows. */
final class CheckboxFormatLines {
    static final class Line {
        final String format12;
        final String format24;

        Line(String format12, String format24) {
            this.format12 = format12;
            this.format24 = format24;
        }
    }

    private CheckboxFormatLines() {}

    static List<Line> split(String format12, String format24) {
        List<String> lines12 = splitFormat(format12);
        List<String> lines24 = splitFormat(format24);
        int lineCount = Math.max(lines12.size(), lines24.size());
        if (lineCount == 0) return Collections.emptyList();

        List<Line> lines = new ArrayList<>(lineCount);
        for (int index = 0; index < lineCount; ++index) {
            lines.add(new Line(
                    lineAt(lines12, index), lineAt(lines24, index)));
        }
        return lines;
    }

    private static List<String> splitFormat(String format) {
        if (format == null || format.isEmpty()) return Collections.emptyList();
        // Formatter quotes the newline in compact time-only formats so that
        // TextClock renders seconds on a second row.
        String normalized = format.replace("'\n'", "\n");
        return Arrays.asList(normalized.split("\n", -1));
    }

    private static String lineAt(List<String> lines, int index) {
        return index < lines.size() ? lines.get(index) : "";
    }
}
