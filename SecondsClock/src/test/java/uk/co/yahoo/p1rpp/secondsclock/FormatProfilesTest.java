package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FormatProfilesTest {
    @Test
    public void widgetShapeUsesReportedDimensions() {
        assertTrue(FormatProfiles.isHorizontalWidget(200, 100));
        assertTrue(FormatProfiles.isHorizontalWidget(100, 100));
        assertFalse(FormatProfiles.isHorizontalWidget(80, 120));
    }

    @Test
    public void widgetCanShareOrSelectIndependentFormats() {
        assertEquals("same", FormatProfiles.widgetFormat(
                "same", "wide", "tall", true, false));
        assertEquals("wide", FormatProfiles.widgetFormat(
                "same", "wide", "tall", false, true));
        assertEquals("tall", FormatProfiles.widgetFormat(
                "same", "wide", "tall", false, false));
        assertEquals("same", FormatProfiles.widgetFormat(
                "same", null, null, false, true));
    }

    @Test
    public void fullScreenCanShareOrSelectOrientationFormats() {
        assertEquals("same", FormatProfiles.clockFormat(
                "same", "portrait", "landscape", true, true));
        assertEquals("portrait", FormatProfiles.clockFormat(
                "same", "portrait", "landscape", false, false));
        assertEquals("landscape", FormatProfiles.clockFormat(
                "same", "portrait", "landscape", false, true));
    }

    @Test
    public void landscapeLargeTimeSplitsAtTheFirstExplicitLineBreak() {
        assertArrayEquals(new String[] {"%H:%M", "%A"},
                FormatLineSplit.splitAtFirstLine("%H:%M%n%A"));
        assertArrayEquals(new String[] {"time", "date"},
                FormatLineSplit.splitAtFirstLine("time\ndate"));
        assertArrayEquals(new String[] {"%H:%M%%n%A", null},
                FormatLineSplit.splitAtFirstLine("%H:%M%%n%A"));
    }

    @Test
    public void landscapeLargeTimeKeepsAlarmBlocksValidAcrossLineBreaks() {
        assertArrayEquals(new String[] {"%<Alarm:%>", "%<%H:%M%> after"},
                FormatLineSplit.splitAtFirstLine("%<Alarm:\n%H:%M%> after"));
    }

    @Test
    public void alarmBlockUsesReportedAlarmTimeAndDisappearsWithoutOne() {
        long alarmTime = 1_700_000_000_000L;
        String formattedTime = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(alarmTime));
        String expected = DateCommandFormat.literalPattern(
                "Alarm: " + formattedTime);
        assertEquals(expected, DateCommandFormat.toTextClockPattern(
                "%<Alarm: %H:%M%>", alarmTime));
        assertEquals("", DateCommandFormat.toTextClockPattern(
                "%<Alarm: %H:%M%>", null));
    }
}
