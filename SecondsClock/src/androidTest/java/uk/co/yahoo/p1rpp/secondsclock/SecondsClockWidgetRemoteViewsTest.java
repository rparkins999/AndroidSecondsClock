package uk.co.yahoo.p1rpp.secondsclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SecondsClockWidgetRemoteViewsTest {
    private static final int TEST_WIDGET_ID = -19734123;
    private static final int COMPACT_WIDTH_DP = 90;
    private static final int WIDTH_DP = 300;
    private static final int HEIGHT_DP = 600;
    private static final String WIDGET_KEY = "W" + TEST_WIDGET_ID;

    private Context context;
    private SharedPreferences preferences;
    private String originalWidgetIds;
    private Object originalLineColorsEnabled;
    private Object originalWidgetDefaultColorsEnabled;
    private Object originalLineColorProfile;
    private Object originalLineColorSeed;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        preferences = context.getSharedPreferences(
                "SecondsClock", Context.MODE_PRIVATE);
        originalWidgetIds = preferences.getString("widgetIds", "");
        originalLineColorsEnabled = preferences.getAll().get(
                LineColors.enabledKey(WIDGET_KEY));
        originalWidgetDefaultColorsEnabled = preferences.getAll().get(
                LineColors.enabledKey("W"));
        originalLineColorProfile = preferences.getAll().get(LineColors.PROFILE_KEY);
        originalLineColorSeed = preferences.getAll().get(LineColors.SEED_KEY);
        String widgetId = "[" + TEST_WIDGET_ID + "]";
        String widgetIds = originalWidgetIds.contains(widgetId)
                ? originalWidgetIds : originalWidgetIds + widgetId;
        preferences.edit()
                .putString("widgetIds", widgetIds)
                .putInt(WIDGET_KEY + "bgcolour", Color.TRANSPARENT)
                .putInt(WIDGET_KEY + "fgcolour", Color.WHITE)
                .putInt(WIDGET_KEY + "touchaction", 0)
                .putBoolean(WIDGET_KEY + "analogMode", false)
                .putBoolean(WIDGET_KEY + "advancedFormatMode", true)
                .putBoolean(WIDGET_KEY + "formatSyncEnabled", true)
                .putBoolean(WIDGET_KEY + "textStyleEmoji", false)
                .putBoolean(LineColors.enabledKey(WIDGET_KEY), false)
                .putInt(LineColors.PROFILE_KEY, LineColors.NORMAL)
                .putString(LineColors.SEED_KEY, "clock")
                .commit();
    }

    @After
    public void tearDown() {
        SharedPreferences.Editor editor = preferences.edit()
                .remove(WIDGET_KEY + "bgcolour")
                .remove(WIDGET_KEY + "fgcolour")
                .remove(WIDGET_KEY + "touchaction")
                .remove(WIDGET_KEY + "analogMode")
                .remove(WIDGET_KEY + "showSeconds")
                .remove(WIDGET_KEY + "showTime")
                .remove(WIDGET_KEY + "showWeekDay")
                .remove(WIDGET_KEY + "showShortDate")
                .remove(WIDGET_KEY + "showMonthDay")
                .remove(WIDGET_KEY + "showMonth")
                .remove(WIDGET_KEY + "showYear")
                .remove(WIDGET_KEY + "showNextAlarm")
                .remove(WIDGET_KEY + "advancedFormatMode")
                .remove(WIDGET_KEY + "formatSyncEnabled")
                .remove(WIDGET_KEY + "textStyleEmoji")
                .remove(WIDGET_KEY + "format")
                .putString("widgetIds", originalWidgetIds);
        restorePreference(editor, LineColors.enabledKey(WIDGET_KEY),
                originalLineColorsEnabled);
        restorePreference(editor, LineColors.enabledKey("W"),
                originalWidgetDefaultColorsEnabled);
        restorePreference(editor, LineColors.PROFILE_KEY,
                originalLineColorProfile);
        restorePreference(editor, LineColors.SEED_KEY, originalLineColorSeed);
        editor.commit();
    }

    @Test
    public void emphasizedYearIsLargerCenteredAndWithinWidgetBounds() {
        View root = renderFormat("%H:%M\n%L%Y\n%H:%M");
        LinearLayout container = root.findViewById(
                R.id.appwidget_format_container);

        assertEquals(View.VISIBLE, container.getVisibility());
        assertEquals(3, container.getChildCount());
        TextClock before = (TextClock) container.getChildAt(0);
        TextClock year = (TextClock) container.getChildAt(1);
        TextClock after = (TextClock) container.getChildAt(2);
        assertTrue(before.getFormat24Hour().toString().contains("HH"));
        assertTrue(before.getFormat24Hour().toString().contains("mm"));
        assertTrue(year.getFormat24Hour().toString().contains("yyyy"));
        assertEquals(Gravity.CENTER, year.getGravity());
        assertTrue(year.getHeight() > before.getHeight());
        assertTrue(year.getTextSize() > before.getTextSize());

        float surroundingCenter = (before.getY() + before.getHeight() / 2.0F
                + after.getY() + after.getHeight() / 2.0F) / 2.0F;
        assertEquals(surroundingCenter,
                year.getY() + year.getHeight() / 2.0F, 2.0F);
        assertEquals(root.getHeight() / 2.0F,
                (before.getY() + after.getY() + after.getHeight()) / 2.0F,
                2.0F);
        for (int index = 0; index < container.getChildCount(); ++index) {
            View row = container.getChildAt(index);
            assertTrue(row.getTop() >= container.getPaddingTop());
            assertTrue(row.getBottom() <= container.getHeight());
        }
        assertTrue(year.getLineHeight() <= year.getHeight());
    }

    @Test
    public void analogTokenAppliesAsAVisibleDialRowBetweenTextRows() {
        View root = renderFormat("%H:%M\n%C\n%Y");
        LinearLayout container = root.findViewById(
                R.id.appwidget_format_container);

        assertEquals(3, container.getChildCount());
        assertTrue(container.getChildAt(0) instanceof TextClock);
        View analogRow = container.getChildAt(1);
        assertTrue(analogRow instanceof LinearLayout);
        assertEquals(R.id.widget_format_line, analogRow.getId());
        View analog = analogRow.findViewById(R.id.widget_inline_analog);
        assertNotNull(analog);
        assertEquals(View.VISIBLE, analog.getVisibility());
        assertTrue(analogRow.getWidth() > 0);
        assertTrue(analogRow.getHeight() > container.getChildAt(0).getHeight());
        assertTrue(analogRow.getTop() >= 0);
        assertTrue(analogRow.getBottom() <= container.getHeight());
        assertTrue(container.getChildAt(2) instanceof TextClock);
    }

    @Test
    public void randomColorTokenChangesOnlyItsRemoteViewsLineColor() {
        View root = renderFormat("%H:%M\n%Q%Y\n%S");
        LinearLayout container = root.findViewById(
                R.id.appwidget_format_container);

        assertEquals(3, container.getChildCount());
        assertEquals(Color.WHITE,
                ((TextClock) container.getChildAt(0)).getCurrentTextColor());
        assertEquals(LineColors.colorForLine(
                        "clock", LineColors.NORMAL, 1),
                ((TextClock) container.getChildAt(1)).getCurrentTextColor());
        assertEquals(Color.WHITE,
                ((TextClock) container.getChildAt(2)).getCurrentTextColor());
    }

    @Test
    public void automaticColorsDoNotOverrideAdvancedLineMarkers() {
        preferences.edit().putBoolean(
                LineColors.enabledKey(WIDGET_KEY), true).commit();
        View root = renderFormat("%H:%M\n%Q%Y");
        LinearLayout container = root.findViewById(
                R.id.appwidget_format_container);

        assertEquals(Color.WHITE,
                ((TextClock) container.getChildAt(0)).getCurrentTextColor());
        assertEquals(LineColors.colorForLine(
                        "clock", LineColors.NORMAL, 1),
                ((TextClock) container.getChildAt(1)).getCurrentTextColor());
    }

    @Test
    public void automaticColorsApplyToCheckboxWidgetLines() {
        preferences.edit()
                .putBoolean(WIDGET_KEY + "advancedFormatMode", false)
                .putBoolean(WIDGET_KEY + "analogMode", false)
                .putInt(WIDGET_KEY + "showTime", 1)
                .putInt(WIDGET_KEY + "showWeekDay", 2)
                .putInt(WIDGET_KEY + "showShortDate", 0)
                .putInt(WIDGET_KEY + "showMonthDay", 1)
                .putInt(WIDGET_KEY + "showMonth", 0)
                .putInt(WIDGET_KEY + "showYear", 1)
                .putBoolean(WIDGET_KEY + "showNextAlarm", false)
                .putBoolean(LineColors.enabledKey(WIDGET_KEY), true)
                .commit();
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, WIDTH_DP);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, HEIGHT_DP);
        RemoteViews remoteViews = SecondsClockWidget.buildRemoteViews(
                context, TEST_WIDGET_ID, options);
        View root = remoteViews.apply(context, new FrameLayout(context));
        TextClock clock = root.findViewById(R.id.appwidget_textclock);
        LinearLayout lineContainer = root.findViewById(
                R.id.appwidget_checkbox_format_container);

        assertEquals(View.GONE, clock.getVisibility());
        assertEquals(View.VISIBLE, lineContainer.getVisibility());
        assertEquals(4, lineContainer.getChildCount());
        assertDisplayLineColors(lineContainer, 0);
    }

    @Test
    public void automaticColorsFollowQuotedNewlineInTimeFormat() {
        preferences.edit()
                .putBoolean(WIDGET_KEY + "advancedFormatMode", false)
                .putBoolean(WIDGET_KEY + "analogMode", false)
                .putInt(WIDGET_KEY + "showTime", 2)
                .putInt(WIDGET_KEY + "showWeekDay", 0)
                .putInt(WIDGET_KEY + "showShortDate", 0)
                .putInt(WIDGET_KEY + "showMonthDay", 0)
                .putInt(WIDGET_KEY + "showMonth", 0)
                .putInt(WIDGET_KEY + "showYear", 0)
                .putBoolean(WIDGET_KEY + "showNextAlarm", false)
                .putBoolean(LineColors.enabledKey(WIDGET_KEY), true)
                .commit();
        View root = renderCheckboxWidgetAtWidth(90, 150);
        LinearLayout lineContainer = root.findViewById(
                R.id.appwidget_checkbox_format_container);
        assertEquals(2, lineContainer.getChildCount());
        TextClock firstLine = (TextClock) lineContainer.getChildAt(0);
        TextClock secondLine = (TextClock) lineContainer.getChildAt(1);
        assertTrue(firstLine.getFormat24Hour().toString().contains("HH"));
        assertFalse(firstLine.getFormat24Hour().toString().contains("\n"));
        assertTrue(secondLine.getFormat24Hour().toString().contains("ss"));
        assertFalse(secondLine.getFormat24Hour().toString().contains("\n"));
        long renderTime = System.currentTimeMillis();
        setClockTextAt(firstLine, renderTime);
        setClockTextAt(secondLine, renderTime);
        measureCurrentClockText(firstLine);
        measureCurrentClockText(secondLine);
        assertCurrentTextFits(firstLine, "first time-only checkbox line");
        assertCurrentTextFits(secondLine, "seconds checkbox line");
        assertDisplayLineColors(lineContainer, 0);
    }

    @Test
    public void widgetColorSettingIsIndependentOfWidgetDefaults() {
        preferences.edit()
                .putBoolean(LineColors.enabledKey("W"), true)
                .putBoolean(LineColors.enabledKey(WIDGET_KEY), false)
                .putBoolean(WIDGET_KEY + "advancedFormatMode", false)
                .putBoolean(WIDGET_KEY + "analogMode", false)
                .putInt(WIDGET_KEY + "showTime", 1)
                .putInt(WIDGET_KEY + "showWeekDay", 0)
                .putBoolean(WIDGET_KEY + "showNextAlarm", false)
                .commit();
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, WIDTH_DP);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, HEIGHT_DP);
        RemoteViews remoteViews = SecondsClockWidget.buildRemoteViews(
                context, TEST_WIDGET_ID, options);
        View root = remoteViews.apply(context, new FrameLayout(context));

        TextClock clock = root.findViewById(R.id.appwidget_textclock);
        assertEquals(View.VISIBLE, clock.getVisibility());
        assertEquals(Color.WHITE, clock.getCurrentTextColor());
    }

    @Test
    public void checkboxColorsKeepPlainMultilinePlacementAndAutosizing() {
        preferences.edit()
                .putBoolean(WIDGET_KEY + "advancedFormatMode", false)
                .putBoolean(WIDGET_KEY + "analogMode", false)
                .putBoolean(LineColors.enabledKey(WIDGET_KEY), true)
                .putInt(WIDGET_KEY + "showTime", 2)
                .putInt(WIDGET_KEY + "showWeekDay", 2)
                .putInt(WIDGET_KEY + "showShortDate", 0)
                .putInt(WIDGET_KEY + "showMonthDay", 1)
                .putInt(WIDGET_KEY + "showMonth", 2)
                .putInt(WIDGET_KEY + "showYear", 1)
                .putBoolean(WIDGET_KEY + "showNextAlarm", false)
                .commit();
        View coloredRoot = renderCheckboxWidgetAtWidth(COMPACT_WIDTH_DP, 150);
        LinearLayout coloredContainer = coloredRoot.findViewById(
                R.id.appwidget_checkbox_format_container);
        TextClock coloredClock = coloredRoot.findViewById(R.id.appwidget_textclock);
        assertEquals(View.GONE, coloredClock.getVisibility());
        assertEquals(View.VISIBLE, coloredContainer.getVisibility());
        long renderTime = System.currentTimeMillis();
        setCheckboxLineTextAt(coloredContainer, renderTime);
        measureCheckboxLines(coloredContainer);
        assertCheckboxLinesFit(coloredContainer, "compact checkbox text");
        assertDisplayLineColors(coloredContainer, 0);
        float compactTextSize = ((TextClock) coloredContainer.getChildAt(0))
                .getTextSize();

        preferences.edit()
                .putBoolean(LineColors.enabledKey(WIDGET_KEY), false)
                .commit();
        View plainRoot = renderCheckboxWidgetAtWidth(COMPACT_WIDTH_DP, 150);
        TextClock plainClock = plainRoot.findViewById(R.id.appwidget_textclock);
        setClockTextAt(plainClock, renderTime);
        measureCurrentClockText(plainClock);
        float plainTextSize = plainClock.getTextSize();
        assertEquals("colored checkbox text should match plain autosizing; "
                        + "colored=" + compactTextSize
                        + ", plain=" + plainTextSize,
                plainTextSize, compactTextSize, 1.0F);
        assertSameTextPlacement(coloredContainer, plainClock);

        preferences.edit()
                .putBoolean(LineColors.enabledKey(WIDGET_KEY), true)
                .commit();
        View wideColoredRoot = renderCheckboxWidgetAtWidth(120, 150);
        LinearLayout wideColoredContainer = wideColoredRoot.findViewById(
                R.id.appwidget_checkbox_format_container);
        setCheckboxLineTextAt(wideColoredContainer, renderTime);
        measureCheckboxLines(wideColoredContainer);
        assertCheckboxLinesFit(wideColoredContainer, "wide checkbox text");
        TextClock wideColoredClock =
                (TextClock) wideColoredContainer.getChildAt(0);
        assertTrue("text should scale up for a wider widget",
                wideColoredClock.getTextSize() > compactTextSize);
        assertDisplayLineColors(wideColoredContainer, 0);

        preferences.edit()
                .putBoolean(LineColors.enabledKey(WIDGET_KEY), false)
                .commit();
        View plainWideRoot = renderCheckboxWidgetAtWidth(120, 150);
        TextClock plainWideClock = plainWideRoot.findViewById(
                R.id.appwidget_textclock);
        setClockTextAt(plainWideClock, renderTime);
        measureCurrentClockText(plainWideClock);
        float plainWideTextSize = plainWideClock.getTextSize();
        assertEquals("wide colored text should match plain checkbox autosizing",
                plainWideTextSize, wideColoredClock.getTextSize(), 1.0F);
        assertSameTextPlacement(wideColoredContainer, plainWideClock);
    }

    private View renderCheckboxWidgetAtWidth(int widthDp, int heightDp) {
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp);
        RemoteViews remoteViews = SecondsClockWidget.buildRemoteViews(
                context, TEST_WIDGET_ID, options);
        View root = remoteViews.apply(context, new FrameLayout(context));
        float density = context.getResources().getDisplayMetrics().density;
        int widthPx = Math.round(widthDp * density);
        int heightPx = Math.round(heightDp * density);
        root.measure(
                View.MeasureSpec.makeMeasureSpec(
                        widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(
                        heightPx, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, widthPx, heightPx);
        return root;
    }

    private void measureCurrentClockText(TextClock clock) {
        int width = clock.getWidth();
        int height = clock.getHeight();
        clock.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        clock.layout(clock.getLeft(), clock.getTop(),
                clock.getLeft() + width, clock.getTop() + height);
        assertNotNull(clock.getLayout());
    }

    private void setClockTextAt(TextClock clock, long timeMillis) {
        CharSequence format = DateFormat.is24HourFormat(context)
                ? clock.getFormat24Hour() : clock.getFormat12Hour();
        if (format != null) {
            clock.setText(DateFormat.format(format, timeMillis));
        }
    }

    private void setCheckboxLineTextAt(
            LinearLayout lineContainer, long timeMillis) {
        for (int index = 0; index < lineContainer.getChildCount(); ++index) {
            setClockTextAt((TextClock) lineContainer.getChildAt(index), timeMillis);
        }
    }

    private void measureCheckboxLines(LinearLayout lineContainer) {
        for (int index = 0; index < lineContainer.getChildCount(); ++index) {
            measureCurrentClockText((TextClock) lineContainer.getChildAt(index));
        }
    }

    private void assertCheckboxLinesFit(
            LinearLayout lineContainer, String description) {
        for (int index = 0; index < lineContainer.getChildCount(); ++index) {
            TextClock clock = (TextClock) lineContainer.getChildAt(index);
            assertCurrentTextFits(clock, description + " line " + index);
            assertEquals("each remote checkbox clock should render one line",
                    1, clock.getLayout().getLineCount());
        }
    }

    private void assertCurrentTextFits(TextClock clock, String description) {
        String displayText = clock.getText().toString().replace('\n', '|');
        int width = clock.getWidth();
        for (int line = 0; line < clock.getLayout().getLineCount(); ++line) {
            assertEquals("text should not be ellipsized: " + displayText
                            + " (line=" + line + ", " + description + ")",
                    0, clock.getLayout().getEllipsisCount(line));
            assertTrue("text should fit without clipping: " + displayText
                            + " (line=" + line
                            + ", lineWidth="
                            + clock.getLayout().getLineWidth(line)
                            + ", viewWidth=" + width
                            + ", textSize=" + clock.getTextSize()
                            + ", " + description + ")",
                    clock.getLayout().getLineWidth(line) <= width);
        }
    }

    private void assertSameTextPlacement(
            LinearLayout coloredContainer, TextClock plainClock) {
        StringBuilder coloredText = new StringBuilder();
        int plainLineCount = plainClock.getLayout().getLineCount();
        assertEquals(plainLineCount, coloredContainer.getChildCount());
        for (int line = 0; line < plainLineCount; ++line) {
            TextClock coloredLine = (TextClock) coloredContainer.getChildAt(line);
            if (line > 0) coloredText.append('\n');
            coloredText.append(coloredLine.getText());
            assertEquals(plainClock.getTextSize(), coloredLine.getTextSize(), 0.0F);
            assertEquals(plainClock.getWidth(), coloredLine.getWidth());
            assertEquals(plainClock.getLayout().getLineLeft(line),
                    coloredLine.getLayout().getLineLeft(0), 0.0F);
            int plainBaseline = plainClock.getTop() + plainClock.getBaseline()
                    + plainClock.getLayout().getLineBaseline(line)
                    - plainClock.getLayout().getLineBaseline(0);
            int coloredBaseline = coloredContainer.getTop()
                    + coloredLine.getTop() + coloredLine.getBaseline();
            assertEquals("colored line " + line + " baseline should match plain text: "
                            + "plainTop=" + plainClock.getTop()
                            + ", plainHeight=" + plainClock.getHeight()
                            + ", plainLines=" + plainLineCount
                            + ", coloredContainerTop=" + coloredContainer.getTop()
                            + ", coloredContainerHeight=" + coloredContainer.getHeight()
                            + ", coloredLineTop=" + coloredLine.getTop()
                            + ", coloredLineHeight=" + coloredLine.getHeight()
                            + ", coloredLineBaseline=" + coloredLine.getBaseline()
                            + ", plainBaseline=" + plainClock.getLayout()
                                    .getLineBaseline(line),
                    plainBaseline, coloredBaseline);
        }
        assertEquals(plainClock.getText().toString(), coloredText.toString());
    }

    private void assertDisplayLineColors(
            LinearLayout lineContainer, int firstLineIndex) {
        for (int lineIndex = 0;
                lineIndex < lineContainer.getChildCount(); ++lineIndex) {
            TextClock line = (TextClock) lineContainer.getChildAt(lineIndex);
            assertEquals("checkbox line " + lineIndex + " should use its palette color",
                    LineColors.checkboxLineColor(
                            preferences, WIDGET_KEY,
                            firstLineIndex + lineIndex, Color.WHITE),
                    line.getCurrentTextColor());
        }
    }

    @Test
    public void automaticColorsApplyAfterCheckboxAnalogDial() {
        preferences.edit()
                .putBoolean(WIDGET_KEY + "advancedFormatMode", false)
                .putBoolean(WIDGET_KEY + "analogMode", true)
                .putInt(WIDGET_KEY + "showWeekDay", 2)
                .putInt(WIDGET_KEY + "showShortDate", 0)
                .putInt(WIDGET_KEY + "showMonthDay", 1)
                .putInt(WIDGET_KEY + "showMonth", 0)
                .putInt(WIDGET_KEY + "showYear", 1)
                .putBoolean(WIDGET_KEY + "showNextAlarm", false)
                .putBoolean(LineColors.enabledKey(WIDGET_KEY), true)
                .commit();
        View root = renderCheckboxWidgetAtWidth(WIDTH_DP, HEIGHT_DP);
        TextClock coloredDateClock = root.findViewById(
                R.id.appwidget_analog_dateclock);
        LinearLayout coloredDateContainer = root.findViewById(
                R.id.appwidget_checkbox_analog_date_container);
        long renderTime = System.currentTimeMillis();

        assertEquals(View.VISIBLE,
                root.findViewById(R.id.appwidget_analogclock).getVisibility());
        assertEquals(View.GONE, coloredDateClock.getVisibility());
        assertEquals(View.VISIBLE, coloredDateContainer.getVisibility());
        setCheckboxLineTextAt(coloredDateContainer, renderTime);
        measureCheckboxLines(coloredDateContainer);
        assertCheckboxLinesFit(coloredDateContainer, "colored analog date text");
        assertDisplayLineColors(coloredDateContainer, 1);
        float coloredTextSize =
                ((TextClock) coloredDateContainer.getChildAt(0)).getTextSize();
        preferences.edit()
                .putBoolean(LineColors.enabledKey(WIDGET_KEY), false)
                .commit();
        View plainRoot = renderCheckboxWidgetAtWidth(WIDTH_DP, HEIGHT_DP);
        TextClock plainDateClock = plainRoot.findViewById(
                R.id.appwidget_analog_dateclock);
        setClockTextAt(plainDateClock, renderTime);
        measureCurrentClockText(plainDateClock);
        assertEquals("colored analog text should match plain checkbox autosizing",
                plainDateClock.getTextSize(), coloredTextSize, 1.0F);
        assertCurrentTextFits(plainDateClock, "plain analog date text");
        assertSameTextPlacement(coloredDateContainer, plainDateClock);
    }

    @Test
    public void emphasizedTextKeepsGravityWithAnalogRows() {
        View topAlignedRoot = renderFormat("%C\n%L%Y");
        LinearLayout topAlignedContainer = topAlignedRoot.findViewById(
                R.id.appwidget_format_container);
        TextClock topAlignedYear = (TextClock) topAlignedContainer.getChildAt(1);
        assertEquals(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                topAlignedYear.getGravity());

        View centeredRoot = renderFormat("%L%Y\n%C");
        LinearLayout centeredContainer = centeredRoot.findViewById(
                R.id.appwidget_format_container);
        TextClock centeredYear = (TextClock) centeredContainer.getChildAt(0);
        assertEquals(Gravity.CENTER, centeredYear.getGravity());
    }

    @Test
    public void singleLineTextRowsAutosizeWhenFormattedTextGrows() {
        assertAutosizesGrowingValue("%k:%M");
        assertAutosizesGrowingValue("%L%k:%M");
        assertAutosizesGrowingValue("%C\n%L%k:%M");
        assertAutosizesGrowingValue("%L%k:%M\n%C");
    }

    @Test
    public void reapplyingAnalogWidgetWithoutSecondsClearsNativeHand() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 37);
        preferences.edit()
                .putBoolean(WIDGET_KEY + "analogMode", true)
                .putBoolean(WIDGET_KEY + "advancedFormatMode", false)
                .putBoolean(WIDGET_KEY + "showSeconds", true)
                .commit();

        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, WIDTH_DP);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, HEIGHT_DP);
        RemoteViews withSeconds = SecondsClockWidget.buildRemoteViews(
                context, TEST_WIDGET_ID, options);
        View root = withSeconds.apply(context, new FrameLayout(context));
        assertNotNull(root.findViewById(R.id.appwidget_analog_hands));
        assertEquals(View.GONE,
                root.findViewById(R.id.appwidget_second_hand_flipper).getVisibility());

        preferences.edit().putBoolean(WIDGET_KEY + "showSeconds", false).commit();
        SecondsClockWidget.buildRemoteViews(context, TEST_WIDGET_ID, options)
                .reapply(context, root);

        assertEquals(View.GONE,
                root.findViewById(R.id.appwidget_second_hand_flipper).getVisibility());
    }

    private void assertAutosizesGrowingValue(String format) {
        View root = renderFormat(format);
        TextClock clock = root.findViewById(R.id.widget_format_text_block_clock);
        assertNotNull(clock);
        assertEquals(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM,
                clock.getAutoSizeTextType());

        int width = clock.getWidth();
        int height = clock.getHeight();
        clock.setText("9:59");
        measureTextClock(clock, width, height);
        float smallerValueSize = clock.getTextSize();

        clock.setText("10:00");
        measureTextClock(clock, width, height);

        assertTrue("growing clock text should fit without increasing font size: "
                        + format,
                clock.getTextSize() <= smallerValueSize);
        assertTrue("clock text should remain within its row: " + format,
                clock.getLayout().getLineWidth(0) <= width);
    }

    private static void measureTextClock(TextClock clock, int width, int height) {
        clock.measure(View.MeasureSpec.makeMeasureSpec(
                        width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        clock.layout(clock.getLeft(), clock.getTop(),
                clock.getLeft() + width, clock.getTop() + height);
    }

    private View renderFormat(String format) {
        preferences.edit().putString(WIDGET_KEY + "format", format).commit();
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, WIDTH_DP);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, HEIGHT_DP);
        RemoteViews remoteViews = SecondsClockWidget.buildRemoteViews(
                context, TEST_WIDGET_ID, options);
        View root = remoteViews.apply(context, new FrameLayout(context));
        float density = context.getResources().getDisplayMetrics().density;
        int widthPx = Math.round(WIDTH_DP * density);
        int heightPx = Math.round(HEIGHT_DP * density);
        root.measure(
                View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, widthPx, heightPx);
        return root;
    }

    private static void restorePreference(
            SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
        else if (value instanceof Integer) editor.putInt(key, (Integer) value);
        else if (value instanceof String) editor.putString(key, (String) value);
        else editor.remove(key);
    }
}
