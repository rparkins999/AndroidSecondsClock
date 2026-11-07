package uk.co.yahoo.p1rpp.secondsclock;

/** Calculates the delay until the next clock refresh boundary. */
final class ClockTickDelay {
    private ClockTickDelay() {}

    static long millisecondsUntilNextTick(long now, int secondsSize) {
        long interval = secondsSize > 0 ? 1000L : 60_000L;
        long remainder = Math.floorMod(now, interval);
        return remainder == 0 ? interval : interval - remainder;
    }
}
