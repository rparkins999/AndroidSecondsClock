/*
 * Copyright © 2026. Released under GPL V3 or later.
 */

package uk.co.yahoo.p1rpp.secondsclock;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextClock;

/** Measures a shared Checkbox TextClock size against the actual widget bounds. */
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
                TypedValue.COMPLEX_UNIT_SP, DEFAULT_AUTO_SIZE_MAX_SP, metrics));
        float heightFraction = analogCheckboxMode ? 1.5F / 4.0F : 1.0F;
        int minTextSizePx = analogCheckboxMode ? 12 : 16;

        TextClock sizingClock = new TextClock(context);
        sizingClock.setGravity(Gravity.CENTER);
        sizingClock.setFormat12Hour(format12);
        sizingClock.setFormat24Hour(format24);
        sizingClock.setLines(Math.max(1, lineCount));
        boolean is24Hour = DateFormat.is24HourFormat(context);
        CharSequence activeFormat = is24Hour ? format24 : format12;
        if (activeFormat == null) {
            activeFormat = is24Hour
                    ? sizingClock.getFormat24Hour()
                    : sizingClock.getFormat12Hour();
        }
        if (activeFormat != null) {
            sizingClock.setText(DateFormat.format(
                    activeFormat, System.currentTimeMillis()));
        }
        sizingClock.setAutoSizeTextTypeUniformWithConfiguration(
                minTextSizePx, maxTextSizePx, DEFAULT_AUTO_SIZE_STEP_PX,
                TypedValue.COMPLEX_UNIT_PX);

        int widthPx = Math.max(1, Math.round(minWidthDp * metrics.density));
        int heightPx = Math.max(1, Math.round(
                maxHeightDp * metrics.density * heightFraction));
        sizingClock.measure(
                View.MeasureSpec.makeMeasureSpec(
                        widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(
                        heightPx, View.MeasureSpec.EXACTLY));
        sizingClock.layout(0, 0, widthPx, heightPx);
        return sizingClock.getTextSize() / metrics.scaledDensity;
    }
}
