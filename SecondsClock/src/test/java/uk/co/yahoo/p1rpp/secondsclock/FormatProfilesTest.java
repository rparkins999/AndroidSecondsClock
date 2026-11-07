package uk.co.yahoo.p1rpp.secondsclock;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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
    public void landscapeEmphasisCanAppearOnAnyLineAndKeepsSecondsToken() {
        List<FormatLineSplit.Line> lines = FormatLineSplit.splitLines(
                "header%n%L%H:%M:%S\n%L%S\n%%L literal %S");

        assertEquals(4, lines.size());
        assertEquals("header", lines.get(0).format);
        assertFalse(lines.get(0).emphasized);
        assertEquals("%H:%M:%S", lines.get(1).format);
        assertTrue(lines.get(1).emphasized);
        assertEquals("%S", lines.get(2).format);
        assertTrue(lines.get(2).emphasized);
        assertEquals("%%L literal %S", lines.get(3).format);
        assertFalse(lines.get(3).emphasized);
        assertEquals("", FormatLineSplit.stripEmphasisMarkers("%L"));
        assertEquals("%%L", FormatLineSplit.stripEmphasisMarkers("%%L"));
        assertEquals(DateCommandFormat.toTextClockPattern("%H:%M:%S", null),
                DateCommandFormat.toTextClockPattern(
                        FormatLineSplit.stripEmphasisMarkers("%L%H:%M:%S"), null));
    }

    @Test
    public void landscapeEmphasisKeepsAlarmBlocksValidAcrossLineBreaks() {
        List<FormatLineSplit.Line> lines = FormatLineSplit.splitLines(
                "%<Alarm:\n%L%H:%M%>");

        assertEquals(2, lines.size());
        assertEquals("%<Alarm:%>", lines.get(0).format);
        assertFalse(lines.get(0).emphasized);
        assertEquals("%<%H:%M%>", lines.get(1).format);
        assertTrue(lines.get(1).emphasized);
    }

    @Test
    public void legacyLandscapeEmphasisMovesToTheFirstNonEmptyLine() {
        List<FormatLineSplit.Line> lines =
                FormatLineSplit.applyLegacyFirstLineEmphasis(
                        FormatLineSplit.splitLines("\n%H:%M\n%A"), true);

        assertEquals(3, lines.size());
        assertFalse(lines.get(0).emphasized);
        assertTrue(lines.get(1).emphasized);
        assertFalse(lines.get(2).emphasized);
    }

    @Test
    public void explicitEmphasisOverridesLegacyFirstLineSetting() {
        List<FormatLineSplit.Line> lines =
                FormatLineSplit.applyLegacyFirstLineEmphasis(
                        FormatLineSplit.splitLines("header\n%L%H:%M"), true);

        assertFalse(lines.get(0).emphasized);
        assertTrue(lines.get(1).emphasized);
    }

    @Test
    public void legacyLandscapeEmphasisDoesNotChangeSingleLineFormats() {
        List<FormatLineSplit.Line> lines =
                FormatLineSplit.applyLegacyFirstLineEmphasis(
                        FormatLineSplit.splitLines("%H:%M"), true);

        assertFalse(lines.get(0).emphasized);
    }

    @Test
    public void shortEmphasizedLinesCanScaleBeyondFourTimes() {
        assertEquals(340.0F,
                FormatLineSizing.relativeScale(5.0F, 1_000.0F), 0.01F);
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
