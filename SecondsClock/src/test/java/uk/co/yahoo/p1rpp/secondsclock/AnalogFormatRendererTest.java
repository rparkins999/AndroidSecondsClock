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
    public void dialKeepsSurroundingTextAndLocalizedDateAndTimePatterns() {
        AnalogFormatRenderer.Result rendered = AnalogFormatRenderer.render(
                "Before %@ on %x at %X", null, false);

        assertTrue(rendered.hasAnalog);
        assertEquals(1, rendered.lines.size());
        assertEquals(3, rendered.lines.get(0).parts.size());
        assertFalse(rendered.lines.get(0).parts.get(0).analog);
        assertTrue(rendered.lines.get(0).parts.get(1).analog);
        assertFalse(rendered.lines.get(0).parts.get(2).analog);

        String followingText = rendered.lines.get(0).parts.get(2).pattern;
        String localDate = ((SimpleDateFormat) DateFormat.getDateInstance(
                DateFormat.SHORT, Locale.getDefault())).toPattern();
        String localTime = ((SimpleDateFormat) DateFormat.getTimeInstance(
                DateFormat.MEDIUM, Locale.getDefault())).toPattern();
        assertTrue(followingText.contains(localDate));
        assertTrue(followingText.contains(localTime));
    }

    @Test
    public void suppressionTokensDoNotBecomeInlineTextAndHideMatchingHands() {
        AnalogFormatRenderer.Result rendered = AnalogFormatRenderer.render(
                "%H%@%S", null, false);

        assertTrue(rendered.hasAnalog);
        assertFalse(rendered.hands.hour);
        assertTrue(rendered.hands.minute);
        assertFalse(rendered.hands.second);
        assertEquals(1, rendered.lines.get(0).parts.size());
        assertTrue(rendered.lines.get(0).parts.get(0).analog);
    }

    @Test
    public void analogInsideAlarmBlockFollowsTheSelectedAlarmBranch() {
        long alarm = 1_700_000_000_000L;
        AnalogFormatRenderer.Result alarmSet = AnalogFormatRenderer.render(
                "%<%H%M%@%|No alarm%>", alarm, false);
        AnalogFormatRenderer.Result noAlarm = AnalogFormatRenderer.render(
                "%<%H%M%@%|No alarm%>", null, false);

        assertTrue(alarmSet.hasAnalog);
        assertFalse(noAlarm.hasAnalog);
        assertEquals("'No alarm'", noAlarm.lines.get(0).parts.get(0).pattern);
    }
}
