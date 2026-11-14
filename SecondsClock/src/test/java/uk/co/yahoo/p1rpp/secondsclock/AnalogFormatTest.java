package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AnalogFormatTest {
    @Test
    public void bothUnescapedClockTokensCreateAnalogClock() {
        assertFalse(AnalogFormat.containsClock("%H %M %S %x %X"));
        assertTrue(AnalogFormat.containsClock("%C"));
        assertTrue(AnalogFormat.containsClock("text %C %H"));
        assertTrue(AnalogFormat.containsClock("%@"));
        assertFalse(AnalogFormat.containsClock("Literal %%C"));
        assertFalse(AnalogFormat.containsClock("Literal %%@"));
    }

    @Test
    public void handSelectorsApplyOnlyWithinTheClockLine() {
        AnalogFormat.Hands defaults = AnalogFormat.handsForClockLine("%C");
        assertTrue(defaults.hour);
        assertTrue(defaults.minute);
        assertTrue(defaults.second);

        AnalogFormat.Hands selected =
                AnalogFormat.handsForClockLine("%H:%M:%S%C");
        assertFalse(selected.hour);
        assertFalse(selected.minute);
        assertFalse(selected.second);

        AnalogFormat.Hands escaped =
                AnalogFormat.handsForClockLine("%%H %C %%M");
        assertTrue(escaped.hour);
        assertTrue(escaped.minute);
        assertTrue(escaped.second);

        assertNull(AnalogFormat.handsForClockLine("%H:%M:%S"));
    }

    @Test
    public void legacyClockTokenRetainsHandSuppressionBehavior() {
        AnalogFormat.Hands selected = AnalogFormat.handsForClockLine("%@%M");
        assertTrue(selected.hour);
        assertFalse(selected.minute);
        assertTrue(selected.second);

        assertNull(AnalogFormat.handsForClockLine("%%@"));
    }
}
