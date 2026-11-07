package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClockTickDelayTest {
    @Test
    public void preEpochTimeSchedulesTheNextMinuteBoundary() {
        assertEquals(1_250L,
                ClockTickDelay.millisecondsUntilNextTick(-1_250L, 0));
    }

    @Test
    public void preEpochSecondsScheduleTheNextSecondBoundary() {
        assertEquals(250L,
                ClockTickDelay.millisecondsUntilNextTick(-1_250L, 255));
    }

    @Test
    public void exactBoundaryWaitsForTheFollowingTick() {
        assertEquals(60_000L,
                ClockTickDelay.millisecondsUntilNextTick(120_000L, 0));
        assertEquals(1_000L,
                ClockTickDelay.millisecondsUntilNextTick(120_000L, 255));
    }
}
