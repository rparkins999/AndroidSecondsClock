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
    public void selectorsChooseHandsAndDefaultToAllWhenNoneAreSpecified() {
        AnalogFormat.Hands defaults = AnalogFormat.handsFor("%@");
        assertTrue(defaults.hour);
        assertTrue(defaults.minute);
        assertTrue(defaults.second);

        AnalogFormat.Hands hourAndMinute = AnalogFormat.handsFor("%H%M%@");
        assertTrue(hourAndMinute.hour);
        assertTrue(hourAndMinute.minute);
        assertFalse(hourAndMinute.second);

        AnalogFormat.Hands secondOnly = AnalogFormat.handsFor("%S%@");
        assertFalse(secondOnly.hour);
        assertFalse(secondOnly.minute);
        assertTrue(secondOnly.second);

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
