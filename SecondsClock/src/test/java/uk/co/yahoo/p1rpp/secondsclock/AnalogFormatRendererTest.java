package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AnalogFormatRendererTest {
    @Test
    public void clockTokenCreatesAnIndependentFullRowBetweenFormattedLines() {
        AnalogFormatRenderer.Result rendered = AnalogFormatRenderer.render(
                "Before %x\n%C\nAfter %X", null, false);

        assertTrue(rendered.hasAnalog);
        assertTrue(rendered.hasSecondHand);
        assertEquals(3, rendered.lines.size());
        assertEquals(1, rendered.lines.get(0).parts.size());
        assertFalse(rendered.lines.get(0).parts.get(0).analog);
        assertEquals(1, rendered.lines.get(1).parts.size());
        assertTrue(rendered.lines.get(1).parts.get(0).analog);
        assertTrue(rendered.lines.get(1).parts.get(0).pattern.isEmpty());
        assertEquals(1, rendered.lines.get(2).parts.size());
        assertFalse(rendered.lines.get(2).parts.get(0).analog);

        String before = rendered.lines.get(0).parts.get(0).pattern;
        String after = rendered.lines.get(2).parts.get(0).pattern;
        String localDate = ((SimpleDateFormat) DateFormat.getDateInstance(
                DateFormat.SHORT, Locale.getDefault())).toPattern();
        String localTime = ((SimpleDateFormat) DateFormat.getTimeInstance(
                DateFormat.MEDIUM, Locale.getDefault())).toPattern();
        assertTrue(before.contains(localDate));
        assertTrue(after.contains(localTime));
    }

    @Test
    public void timeTokensStayDigitalOffClockLinesAndSuppressHandsOnClockLine() {
        AnalogFormatRenderer.Result rendered = AnalogFormatRenderer.render(
                "%H:%M:%S\n%C%H%S%nOther text", null, false);

        assertEquals(3, rendered.lines.size());
        String digitalTime = rendered.lines.get(0).parts.get(0).pattern;
        assertTrue(digitalTime.contains("HH"));
        assertTrue(digitalTime.contains("mm"));
        assertTrue(digitalTime.contains("ss"));

        AnalogFormatRenderer.Line clockLine = rendered.lines.get(1);
        assertEquals(1, clockLine.parts.size());
        assertTrue(clockLine.parts.get(0).analog);
        assertFalse(clockLine.hands.hour);
        assertTrue(clockLine.hands.minute);
        assertFalse(clockLine.hands.second);
        assertFalse(rendered.hasSecondHand);
        assertTrue(rendered.lines.get(2).parts.get(0).pattern.contains("Other text"));
    }

    @Test
    public void clockMarkerAloneShowsAllHandsAndEscapedMarkerRemainsText() {
        AnalogFormatRenderer.Result clock = AnalogFormatRenderer.render(
                "%C", null, false);
        assertTrue(clock.hasAnalog);
        assertTrue(clock.lines.get(0).hands.hour);
        assertTrue(clock.lines.get(0).hands.minute);
        assertTrue(clock.lines.get(0).hands.second);

        AnalogFormatRenderer.Result escaped = AnalogFormatRenderer.render(
                "%%C", null, false);
        assertFalse(escaped.hasAnalog);
        assertEquals(1, escaped.lines.get(0).parts.size());
        assertFalse(escaped.lines.get(0).parts.get(0).analog);
    }

    @Test
    public void legacyClockMarkerStillRendersSavedAnalogFormats() {
        AnalogFormatRenderer.Result rendered = AnalogFormatRenderer.render(
                "%@%M", null, false);

        assertTrue(rendered.hasAnalog);
        assertTrue(rendered.lines.get(0).hands.hour);
        assertFalse(rendered.lines.get(0).hands.minute);
        assertTrue(rendered.lines.get(0).hands.second);
    }

    @Test
    public void largeLineMarkerIsRemovedAndKeepsItsLineEmphasized() {
        AnalogFormatRenderer.Result rendered = AnalogFormatRenderer.render(
                "Before\n%LLarge line\n%C", null, true);

        assertEquals(3, rendered.lines.size());
        assertFalse(rendered.lines.get(0).emphasized);
        assertTrue(rendered.lines.get(1).emphasized);
        assertTrue(rendered.lines.get(1).parts.get(0).pattern
                .contains("Large line"));
        assertFalse(rendered.lines.get(1).parts.get(0).pattern
                .contains("%L"));
        assertFalse(rendered.lines.get(2).emphasized);
        assertTrue(rendered.lines.get(2).parts.get(0).analog);
    }

    @Test
    public void largeLineMarkerWorksWithoutAnAnalogClock() {
        AnalogFormatRenderer.Result rendered = AnalogFormatRenderer.render(
                "%H:%M\n%L%Y", null, true);

        assertFalse(rendered.hasAnalog);
        assertEquals(2, rendered.lines.size());
        assertFalse(rendered.lines.get(0).emphasized);
        assertTrue(rendered.lines.get(1).emphasized);
        assertFalse(rendered.lines.get(1).parts.get(0).pattern.contains("%L"));
    }

    @Test
    public void clockMarkerInAlarmBlockOnlyRendersForSelectedBranch() {
        String format = "%<%C%|No alarm%>";
        long alarmTime = 1_700_000_000_000L;

        AnalogFormatRenderer.Result withAlarm = AnalogFormatRenderer.render(
                format, alarmTime, false);
        assertTrue(withAlarm.hasAnalog);
        assertTrue(withAlarm.lines.get(0).hands.hour);

        AnalogFormatRenderer.Result withoutAlarm = AnalogFormatRenderer.render(
                format, null, false);
        assertFalse(withoutAlarm.hasAnalog);
        assertEquals(DateCommandFormat.literalPattern("No alarm"),
                withoutAlarm.lines.get(0).parts.get(0).pattern);
    }
}
