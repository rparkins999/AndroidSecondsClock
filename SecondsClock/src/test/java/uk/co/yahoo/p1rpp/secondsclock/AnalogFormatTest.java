package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AnalogFormatTest {
    @Test
    public void percentAtIsUnusedAndEscapedPercentDoesNotInsertADial() {
        assertFalse(AnalogFormat.containsPlaceholder("%H %M %S %x %X"));
        assertTrue(AnalogFormat.containsPlaceholder("Before %@ after"));
        assertFalse(AnalogFormat.containsPlaceholder("Literal %%@"));
    }

    @Test
    public void suppressionTokensRemoveOnlyTheirMatchingHands() {
        AnalogFormat.Hands defaults = AnalogFormat.handsFor("%@");
        assertTrue(defaults.hour);
        assertTrue(defaults.minute);
        assertTrue(defaults.second);

        AnalogFormat.Hands withoutHour = AnalogFormat.handsFor("%@%H");
        assertFalse(withoutHour.hour);
        assertTrue(withoutHour.minute);
        assertTrue(withoutHour.second);

        AnalogFormat.Hands withoutMinute = AnalogFormat.handsFor("%@%M");
        assertTrue(withoutMinute.hour);
        assertFalse(withoutMinute.minute);
        assertTrue(withoutMinute.second);

        AnalogFormat.Hands withoutSecond = AnalogFormat.handsFor("%@%S");
        assertTrue(withoutSecond.hour);
        assertTrue(withoutSecond.minute);
        assertFalse(withoutSecond.second);

        AnalogFormat.Hands withoutHourOrMinute =
                AnalogFormat.handsFor("%@%H%M");
        assertFalse(withoutHourOrMinute.hour);
        assertFalse(withoutHourOrMinute.minute);
        assertTrue(withoutHourOrMinute.second);

        AnalogFormat.Hands withoutAny = AnalogFormat.handsFor("%@%H%M%S");
        assertFalse(withoutAny.hour);
        assertFalse(withoutAny.minute);
        assertFalse(withoutAny.second);

        AnalogFormat.Hands escaped = AnalogFormat.handsFor("%%H %@");
        assertTrue(escaped.hour);
        assertTrue(escaped.minute);
        assertTrue(escaped.second);
    }

    @Test
    public void handSelectorsAreRemovedOnlyForAnalogFormats() {
        assertEquals("%H:%M:%S", AnalogFormat.removeHandSelectors("%H:%M:%S"));
        assertEquals("%@ %%H %x %X",
                AnalogFormat.removeHandSelectors("%H%M%S%@ %%H %x %X"));
    }
}
