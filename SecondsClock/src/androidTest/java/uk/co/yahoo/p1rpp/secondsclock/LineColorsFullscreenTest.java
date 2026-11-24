package uk.co.yahoo.p1rpp.secondsclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class LineColorsFullscreenTest {
    private static final int FOREGROUND = 0x55112233;
    private static final String SEED = "full-screen-color-test";
    private static final int PROFILE = LineColors.TRITANOPIA;

    private SharedPreferences preferences;
    private Map<String, ?> originalPreferences;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        preferences = context.getSharedPreferences(
                "SecondsClock", Context.MODE_PRIVATE);
        originalPreferences = new HashMap<>(preferences.getAll());
        setClockFormat("%H:%M\n%Q%Y", false);
    }

    @After
    public void tearDown() {
        SharedPreferences.Editor editor = preferences.edit().clear();
        for (Map.Entry<String, ?> entry : originalPreferences.entrySet()) {
            putPreference(editor, entry.getKey(), entry.getValue());
        }
        editor.commit();
    }

    @Test
    public void randomMarkerColorsOnlyItsFullscreenLineAndKeepsForegroundAlpha() {
        assertRenderedColors("%H:%M\n%Q%Y", false,
                new int[] {1}, new int[] {0});
    }

    @Test
    public void automaticSettingDoesNotColorUnmarkedAdvancedLines() {
        assertRenderedColors("%H:%M\n%Y", true,
                new int[0], new int[] {0, 1});
    }

    @Test
    public void explicitAdvancedMarkerStillWorksWhenAutomaticColorsAreEnabled() {
        assertRenderedColors("%Q%H:%M\n%Y", true,
                new int[] {0}, new int[] {1});
    }

    @Test
    public void automaticColorsApplyToCheckboxModeLines() {
        setCheckboxMode(true);
        try (ActivityScenario<ClockActivity> scenario =
                     ActivityScenario.launch(ClockActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                ClockView clock = findClockView(
                        activity.findViewById(R.id.genericlayout));
                assertNotNull(clock);
                List<TextView> lines = new ArrayList<>();
                collectTextViews(clock, lines);
                assertTrue("checkbox mode should display several separate lines",
                        lines.size() >= 6);
                for (int lineIndex = 0; lineIndex < lines.size(); ++lineIndex) {
                    assertEquals("checkbox line " + lineIndex,
                            LineColors.checkboxLineColor(
                                    preferences, "C", lineIndex, FOREGROUND),
                            lines.get(lineIndex).getCurrentTextColor());
                }
            });
        }
    }

    @Test
    public void fullscreenColorSettingIsIndependentOfWidgetDefaults() {
        preferences.edit().putBoolean(
                LineColors.enabledKey("W"), true).commit();
        setCheckboxMode(false);
        try (ActivityScenario<ClockActivity> scenario =
                     ActivityScenario.launch(ClockActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                ClockView clock = findClockView(
                        activity.findViewById(R.id.genericlayout));
                List<TextView> lines = new ArrayList<>();
                collectTextViews(clock, lines);
                assertTrue(lines.size() >= 6);
                for (TextView line : lines) {
                    assertEquals(FOREGROUND, line.getCurrentTextColor());
                }
            });
        }
    }

    private void setClockFormat(String format, boolean automaticColors) {
        preferences.edit()
                .putBoolean("CadvancedFormatMode", true)
                .putString("Cformat", format)
                .putBoolean("CformatSyncEnabled", true)
                .putBoolean("CadvancedLargeTime", false)
                .putBoolean("C7seg", false)
                .putBoolean("CtextStyleEmoji", false)
                .putBoolean("CshowNextAlarm", false)
                .putInt("Cfgcolour", FOREGROUND)
                .putBoolean(LineColors.enabledKey("C"), automaticColors)
                .putInt(LineColors.PROFILE_KEY, PROFILE)
                .putString(LineColors.SEED_KEY, SEED)
                .commit();
    }

    private void setCheckboxMode(boolean automaticColors) {
        preferences.edit()
                .putBoolean("CadvancedFormatMode", false)
                .putBoolean("CanalogMode", false)
                .putBoolean("Cforcevertical", true)
                .putInt("CsecondsSize", 0)
                .putInt("CshowWeekDay", 2)
                .putInt("CshowShortDate", 0)
                .putInt("CshowMonthDay", 1)
                .putInt("CshowMonth", 2)
                .putInt("CshowYear", 1)
                .putBoolean("CshowNextAlarm", false)
                .putInt("Cfgcolour", FOREGROUND)
                .putBoolean(LineColors.enabledKey("C"), automaticColors)
                .putInt(LineColors.PROFILE_KEY, PROFILE)
                .putString(LineColors.SEED_KEY, SEED)
                .commit();
    }

    private void assertRenderedColors(
            String format, boolean automaticColors,
            int[] coloredLines, int[] plainLines) {
        setClockFormat(format, automaticColors);
        try (ActivityScenario<ClockActivity> scenario =
                     ActivityScenario.launch(ClockActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                ViewGroup root = activity.findViewById(R.id.genericlayout);
                assertNotNull(root);
                TextClock clock = findTextClock(root);
                assertNotNull("full-screen advanced format should render a TextClock",
                        clock);
                CharSequence text = clock.getText();
                assertTrue("formatted text should retain color spans",
                        text instanceof Spanned);
                Spanned styledText = (Spanned) text;
                for (int lineIndex : coloredLines) {
                    int paletteColor = LineColors.colorForLine(
                            SEED, PROFILE, lineIndex);
                    int expectedColor = LineColors.withBaseAlpha(
                            paletteColor, FOREGROUND);
                    assertTrue("expected palette color on full-screen line "
                                    + lineIndex,
                            hasColorSpan(styledText, lineIndex, expectedColor,
                                    clock.getCurrentTextColor()));
                }
                for (int lineIndex : plainLines) {
                    assertFalse("unmarked full-screen line should keep base color",
                            hasColorOverride(styledText, lineIndex,
                                    clock.getCurrentTextColor()));
                }
            });
        }
    }

    private static boolean hasColorSpan(
            Spanned text, int expectedLine, int expectedColor, int baseColor) {
        CharacterStyle[] spans = text.getSpans(
                0, text.length(), CharacterStyle.class);
        for (CharacterStyle span : spans) {
            int start = text.getSpanStart(span);
            if (start < 0 || lineAt(text, start) != expectedLine) continue;
            TextPaint paint = new TextPaint();
            paint.setColor(baseColor);
            span.updateDrawState(paint);
            if (paint.getColor() == expectedColor) return true;
        }
        return false;
    }

    private static boolean hasColorOverride(
            Spanned text, int expectedLine, int baseColor) {
        CharacterStyle[] spans = text.getSpans(
                0, text.length(), CharacterStyle.class);
        for (CharacterStyle span : spans) {
            int start = text.getSpanStart(span);
            if (start < 0 || lineAt(text, start) != expectedLine) continue;
            TextPaint paint = new TextPaint();
            paint.setColor(baseColor);
            span.updateDrawState(paint);
            if (paint.getColor() != baseColor) return true;
        }
        return false;
    }

    private static int lineAt(CharSequence text, int offset) {
        int line = 0;
        for (int index = 0; index < offset; ++index) {
            if (text.charAt(index) == '\n') ++line;
        }
        return line;
    }

    private static TextClock findTextClock(View view) {
        if (view instanceof TextClock) return (TextClock) view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); ++index) {
            TextClock found = findTextClock(group.getChildAt(index));
            if (found != null) return found;
        }
        return null;
    }

    private static ClockView findClockView(View view) {
        if (view instanceof ClockView) return (ClockView) view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); ++index) {
            ClockView found = findClockView(group.getChildAt(index));
            if (found != null) return found;
        }
        return null;
    }

    private static void collectTextViews(View view, List<TextView> result) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.getText().length() > 0) result.add(text);
        }
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); ++index) {
            collectTextViews(group.getChildAt(index), result);
        }
    }

    private static void putPreference(
            SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof String) editor.putString(key, (String) value);
        else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
        else if (value instanceof Integer) editor.putInt(key, (Integer) value);
        else if (value instanceof Long) editor.putLong(key, (Long) value);
        else if (value instanceof Float) editor.putFloat(key, (Float) value);
        else if (value instanceof Set<?>) {
            @SuppressWarnings("unchecked")
            Set<String> strings = (Set<String>) value;
            editor.putStringSet(key, strings);
        }
    }
}
