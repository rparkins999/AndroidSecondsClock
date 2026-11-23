package uk.co.yahoo.p1rpp.secondsclock;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class CheckboxFormatLinesTest {
    @Test
    public void splitsQuotedSecondsNewlineAndAdditionalRows() {
        List<CheckboxFormatLines.Line> lines = CheckboxFormatLines.split(
                "h:mm'\n'ss a\nEEEE\nd",
                "HH:mm'\n'ss\nEEEE\nd");

        assertEquals(4, lines.size());
        assertEquals("h:mm", lines.get(0).format12);
        assertEquals("HH:mm", lines.get(0).format24);
        assertEquals("ss a", lines.get(1).format12);
        assertEquals("ss", lines.get(1).format24);
        assertEquals("EEEE", lines.get(2).format12);
        assertEquals("d", lines.get(3).format24);
    }

    @Test
    public void padsMismatchedFormatsAndKeepsBlankRows() {
        List<CheckboxFormatLines.Line> lines =
                CheckboxFormatLines.split("h:mm\n", "HH:mm");

        assertEquals(2, lines.size());
        assertEquals("", lines.get(1).format12);
        assertEquals("", lines.get(1).format24);
    }
}
