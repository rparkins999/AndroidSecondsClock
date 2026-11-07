package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    @Test
    public void textStyleEmojiRequestsMonochromeAlarmSymbol() {
        assertEquals("⏰\uFE0E", TextStyleEmoji.preferTextPresentation("⏰"));
        assertEquals("'⏰\uFE0E'", DateCommandFormat.toTextClockPattern(
                "⏰", null, true));
        assertEquals("'⏰'", DateCommandFormat.toTextClockPattern(
                "⏰", null, false));
    }

    @Test
    public void textStyleEmojiLeavesJoinedAndKeycapSequencesIntact() {
        assertEquals("👨‍👩‍👧‍👦", TextStyleEmoji.preferTextPresentation("👨‍👩‍👧‍👦"));
        assertEquals("1️⃣", TextStyleEmoji.preferTextPresentation("1️⃣"));

        String subdivisionFlag = new String(Character.toChars(0x1F3F4))
                + new String(Character.toChars(0xE0067))
                + new String(Character.toChars(0xE0062))
                + new String(Character.toChars(0xE0065))
                + new String(Character.toChars(0xE006E))
                + new String(Character.toChars(0xE0067))
                + new String(Character.toChars(0xE007F));
        assertEquals(subdivisionFlag,
                TextStyleEmoji.preferTextPresentation(subdivisionFlag));
    }

    @Test
    public void textStyleEmojiDoesNotTreatTagsAfterOtherEmojiAsAFlag() {
        String tagLetter = new String(Character.toChars(0xE0067));

        assertEquals("😀\uFE0E" + tagLetter,
                TextStyleEmoji.preferTextPresentation("😀" + tagLetter));
    }
}
