/*
 * Copyright © 2026. SecondsClock contributors
 * Released under GPL V3 or later
 */

package uk.co.yahoo.p1rpp.secondsclock.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import uk.co.yahoo.p1rpp.secondsclock.R;
import uk.co.yahoo.p1rpp.secondsclock.SecondsClockWidget;

/** Verifies seconds on the actual RemoteViews delivered through Android's widget host service. */
@RunWith(AndroidJUnit4.class)
public final class SecondsClockWidgetCompatibilityTest {
    private static final String TAG = "SecondsClockWidgetTest";
    private static final long WIDGET_TIMEOUT_MS = 15_000;
    private static final long TICK_TIMEOUT_MS = 12_000;

    @Test
    public void visibleRemoteWidgetDisplaysChangingSeconds() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context targetContext = instrumentation.getTargetContext();
        SharedPreferences preferences = targetContext.getSharedPreferences(
                "SecondsClock", Context.MODE_PRIVATE);
        Map<String, ?> originalPreferences = preferences.getAll();
        ComponentName provider = new ComponentName(targetContext, SecondsClockWidget.class);
        WidgetHostActivity host = null;

        try {
            setSecondsOnlyDefault(preferences);

            Intent hostIntent = new Intent(targetContext, WidgetHostActivity.class)
                    .putExtra(WidgetHostActivity.EXTRA_PROVIDER, provider.flattenToString())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            targetContext.startActivity(hostIntent);
            host = waitForHostActivity();
            // The compatibility workflow grants this debug host bind authorization with
            // `appwidget grantbind` before instrumentation starts. This avoids depending on
            // Android Settings' version-specific consent UI while still using real binding.
            assertTrue("widget bind/RemoteViews delivery failed: " + host.getFailure(),
                    host.awaitWidget(WIDGET_TIMEOUT_MS, TimeUnit.MILLISECONDS));

            AppWidgetHostView hostView = host.getWidgetView();
            assertNotNull("AppWidgetHost did not create its RemoteViews host view", hostView);
            AtomicReference<ComponentName> deliveredProvider = new AtomicReference<>();
            instrumentation.runOnMainSync(() ->
                    deliveredProvider.set(hostView.getAppWidgetInfo().provider));
            assertEquals("host must receive the installed SecondsClock provider",
                    provider, deliveredProvider.get());

            TextClock clock = waitForVisibleSecondsClock(instrumentation, hostView);

            Set<String> displayedValues = new HashSet<>();
            long deadline = SystemClock.elapsedRealtime() + TICK_TIMEOUT_MS;
            while (SystemClock.elapsedRealtime() < deadline && displayedValues.size() < 3) {
                AtomicReference<String> currentText = new AtomicReference<>();
                instrumentation.runOnMainSync(() ->
                        currentText.set(clock.getText().toString().trim()));
                if (!currentText.get().isEmpty()) displayedValues.add(currentText.get());
                SystemClock.sleep(200);
            }

            assertTrue("visible RemoteViews TextClock did not advance through seconds; values="
                            + displayedValues,
                    displayedValues.size() >= 3);
            Log.i(TAG, "Observed changing seconds in AppWidgetHost RemoteViews: "
                    + displayedValues);
        } finally {
            cleanupHost(instrumentation, host);
            restorePreferences(preferences, originalPreferences);
        }
    }

    private static WidgetHostActivity waitForHostActivity() {
        long deadline = SystemClock.elapsedRealtime() + WIDGET_TIMEOUT_MS;
        while (SystemClock.elapsedRealtime() < deadline) {
            WidgetHostActivity host = WidgetHostActivity.active;
            if (host != null) return host;
            SystemClock.sleep(50);
        }
        throw new AssertionError("test widget-host Activity did not start");
    }

    private static TextClock waitForVisibleSecondsClock(
        Instrumentation instrumentation, AppWidgetHostView hostView) {
        long deadline = SystemClock.elapsedRealtime() + WIDGET_TIMEOUT_MS;
        AtomicReference<CharSequence> lastFormat = new AtomicReference<>();
        AtomicReference<Boolean> lastVisible = new AtomicReference<>(false);
        while (SystemClock.elapsedRealtime() < deadline) {
            AtomicReference<TextClock> found = new AtomicReference<>();
            instrumentation.runOnMainSync(() -> {
                View view = hostView.findViewById(R.id.appwidget_textclock);
                if (view instanceof TextClock) {
                    TextClock clock = (TextClock) view;
                    found.set(clock);
                    lastFormat.set(clock.getFormat24Hour());
                    lastVisible.set(clock.isAttachedToWindow()
                            && clock.isShown()
                            && clock.getWindowVisibility() == View.VISIBLE);
                }
            });
            CharSequence format = lastFormat.get();
            if (found.get() != null && Boolean.TRUE.equals(lastVisible.get())
                    && format != null && format.toString().contains("ss")) {
                return found.get();
            }
            SystemClock.sleep(100);
        }
        throw new AssertionError("AppWidgetHost did not receive visible seconds-formatted "
                + "RemoteViews; last24HourFormat=" + lastFormat.get()
                + ", visible=" + lastVisible.get());
    }

    private static void setSecondsOnlyDefault(SharedPreferences preferences) {
        preferences.edit()
                .clear()
                .putInt("WshowTime", 2)
                .putInt("WshowWeekDay", 0)
                .putInt("WshowShortDate", 0)
                .putInt("WshowMonthDay", 0)
                .putInt("WshowMonth", 0)
                .putInt("WshowYear", 0)
                .putBoolean("WadvancedFormatMode", false)
                .commit();
    }

    private static void cleanupHost(
            Instrumentation instrumentation, WidgetHostActivity host) {
        if (host == null) return;
        instrumentation.runOnMainSync(() -> {
            host.cleanup();
            host.finish();
        });
    }

    private static void restorePreferences(
            SharedPreferences preferences, Map<String, ?> original) {
        SharedPreferences.Editor editor = preferences.edit().clear();
        for (Map.Entry<String, ?> entry : original.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) editor.putString(entry.getKey(), (String) value);
            else if (value instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean) value);
            else if (value instanceof Integer) editor.putInt(entry.getKey(), (Integer) value);
            else if (value instanceof Long) editor.putLong(entry.getKey(), (Long) value);
            else if (value instanceof Float) editor.putFloat(entry.getKey(), (Float) value);
            else if (value instanceof Set<?>) {
                @SuppressWarnings("unchecked")
                Set<String> strings = (Set<String>) value;
                editor.putStringSet(entry.getKey(), strings);
            }
        }
        editor.commit();
    }
}
