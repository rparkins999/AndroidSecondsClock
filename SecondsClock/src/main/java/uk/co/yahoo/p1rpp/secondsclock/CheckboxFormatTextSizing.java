/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextClock;

/** Measures checkbox text with the same uniform autosizer as the plain clock. */
final class CheckboxFormatTextSizing {
    private static final int DEFAULT_AUTO_SIZE_MAX_SP = 112;
    private static final int DEFAULT_AUTO_SIZE_STEP_PX = 1;

    private CheckboxFormatTextSizing() {}

    static float widgetTextSizeSp(
            Context context, int minWidthDp, int maxHeightDp,
            String format12, String format24, int lineCount,
            boolean analogCheckboxMode) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int maxTextSizePx = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                DEFAULT_AUTO_SIZE_MAX_SP, metrics));
        return textSizeSp(context, minWidthDp, maxHeightDp,
                format12, format24, lineCount,
                analogCheckboxMode ? 12 : 16,
                maxTextSizePx, DEFAULT_AUTO_SIZE_STEP_PX,
                analogCheckboxMode);
    }

    static float previewTextSizeSp(
            Context context, int minWidthDp, int maxHeightDp,
            String format12, String format24, int lineCount,
            boolean analogCheckboxMode) {
        return textSizeSp(context, minWidthDp, maxHeightDp,
                format12, format24, lineCount,
                10, 3000, 3, analogCheckboxMode);
    }

    private static float textSizeSp(
            Context context, int minWidthDp, int maxHeightDp,
            String format12, String format24, int lineCount,
            int minTextSizePx, int maxTextSizePx, int stepPx,
            boolean analogCheckboxMode) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int widthPx = Math.max(1, Math.round(minWidthDp * metrics.density));
        float heightFraction = analogCheckboxMode ? 1.5F / 4.0F : 1.0F;
        int heightPx = Math.max(1, Math.round(
                maxHeightDp * metrics.density * heightFraction));

        TextClock sizingClock = new TextClock(context);
        sizingClock.setGravity(Gravity.CENTER);
        sizingClock.setFormat12Hour(format12);
        sizingClock.setFormat24Hour(format24);
        sizingClock.setLines(Math.max(1, lineCount));
        sizingClock.setAutoSizeTextTypeUniformWithConfiguration(
                minTextSizePx, maxTextSizePx, stepPx, TypedValue.COMPLEX_UNIT_PX);
        sizingClock.measure(
                View.MeasureSpec.makeMeasureSpec(
                        widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(
                        heightPx, View.MeasureSpec.EXACTLY));
        sizingClock.layout(0, 0, widthPx, heightPx);
        return sizingClock.getTextSize() / metrics.scaledDensity;
    }
}
