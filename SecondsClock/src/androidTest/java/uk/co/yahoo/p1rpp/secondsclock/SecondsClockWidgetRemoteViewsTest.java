package uk.co.yahoo.p1rpp.secondsclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SecondsClockWidgetRemoteViewsTest {
    private static final int TEST_WIDGET_ID = -19734123;
    private static final int WIDTH_DP = 300;
    private static final int HEIGHT_DP = 600;
    private static final String WIDGET_KEY = "W" + TEST_WIDGET_ID;

    private Context context;
    private SharedPreferences preferences;
    private String originalWidgetIds;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        preferences = context.getSharedPreferences(
                "SecondsClock", Context.MODE_PRIVATE);
        originalWidgetIds = preferences.getString("widgetIds", "");
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
                .commit();
    }

    @After
    public void tearDown() {
        preferences.edit()
                .remove(WIDGET_KEY + "bgcolour")
                .remove(WIDGET_KEY + "fgcolour")
                .remove(WIDGET_KEY + "touchaction")
                .remove(WIDGET_KEY + "analogMode")
                .remove(WIDGET_KEY + "advancedFormatMode")
                .remove(WIDGET_KEY + "formatSyncEnabled")
                .remove(WIDGET_KEY + "textStyleEmoji")
                .remove(WIDGET_KEY + "format")
                .putString("widgetIds", originalWidgetIds)
                .commit();
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
}
